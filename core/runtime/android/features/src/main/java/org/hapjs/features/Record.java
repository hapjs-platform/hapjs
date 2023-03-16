/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.UInt8Array;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.hapjs.bridge.Callback;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.model.AppInfo;
import org.hapjs.render.Display;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Record.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.RESIDENT_IMPORTANT,
        actions = {
                @ActionAnnotation(
                        name = Record.ACTION_START_RECORD,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.RECORD_AUDIO}),
                @ActionAnnotation(name = Record.ACTION_STOP_RECORD, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Record.EVENT_ON_FRAME_RECORDED,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = Extension.Type.EVENT,
                        alias = Record.EVENT_ON_FRAME_RECORDED_ALIAS)
        })
public class Record extends CallbackHybridFeature {
    private static final String TAG = "Record";

    protected static final String FEATURE_NAME = "system.record";
    protected static final String ACTION_START_RECORD = "start";
    protected static final String ACTION_STOP_RECORD = "stop";
    protected static final String EVENT_ON_FRAME_RECORDED_ALIAS = "onframerecorded";
    protected static final String EVENT_ON_FRAME_RECORDED = "__onframerecorded";

    protected static final String KEY_DURATION = "duration";
    protected static final String KEY_SAMPLERATE = "sampleRate";
    protected static final String KEY_NUMBER_OF_CHANNELS = "numberOfChannels";
    protected static final String KEY_ENCODE_BIT_RATE = "encodeBitRate";
    protected static final String KEY_FORMAT = "format";
    protected static final String KEY_FRAME_SIZE = "bufferSize";

    protected static final int DEF_DURATION_MAX = -1;
    protected static final int DEF_SAMPLE_RATE = 8000;
    protected static final int DEF_ENCODE_BIT_RATE = 16000;
    protected static final int DEF_CHANNELS = 2;
    protected static final int DEF_FRAME_SIZE = 0;

    protected static final String VALUE_3GPP = "3gpp";
    protected static final String VALUE_AMR_NB = "amr_nb";
    protected static final String VALUE_AAC = "aac";
    protected static final String VALUE_PCM = "pcm";

    protected static final String RESULT_URI = "uri";
    private static final String RESULT_IS_LAST_FRAME = "isLastFrame";
    private static final String RESULT_FRAME_BUFFER = "frameBuffer";

    private static final int RECORD_CODE_ERROR = 1;
    private static final int RECORD_CODE_REACH_LIMIT = 2;
    private static final int RECORD_CODE_STOP = 3;
    private static final int RECORD_CODE_FRAME_BUFFER = 4;

    private RecordDelegate mRecorderDelegate;
    private Handler mHandler;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_START_RECORD.equals(action)) {
            stopRecord(request);
            return startRecord(request);
        } else if (EVENT_ON_FRAME_RECORDED.equals(action)) {
            handleEventRequest(request, action);
            return Response.SUCCESS;
        } else {
            return stopRecord(request);
        }
    }

    private void handleEventRequest(Request request, String action) {
        if (request.getCallback().isValid()) {
            putCallbackContext(new FrameCallbackContext(request));
        } else {
            removeCallbackContext(action);
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Response startRecord(final Request request) throws IOException {
        notifyFeatureStatus(null, request, Display.DISPLAY_RECORD_START);
        try {
            int duration = DEF_DURATION_MAX;
            int numberOfChannels = DEF_CHANNELS;
            int sampleRate = DEF_SAMPLE_RATE;
            int encodeBitRate = DEF_ENCODE_BIT_RATE;
            int bufferSize = DEF_FRAME_SIZE;
            String format = VALUE_3GPP;
            JSONObject params = request.getJSONParams();
            if (null != params) {
                duration = params.optInt(KEY_DURATION, DEF_DURATION_MAX);
                numberOfChannels = params.optInt(KEY_NUMBER_OF_CHANNELS, DEF_CHANNELS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    sampleRate = params.optInt(KEY_SAMPLERATE, DEF_SAMPLE_RATE);
                } else {
                    Log.w(TAG, "Below Android 6.0, the input sampling rate is forced to 8000");
                }
                encodeBitRate = params.optInt(KEY_ENCODE_BIT_RATE, DEF_ENCODE_BIT_RATE);
                bufferSize = params.optInt(KEY_FRAME_SIZE, DEF_FRAME_SIZE);
                format = params.optString(KEY_FORMAT, VALUE_3GPP);

                if (!isValidFormat(bufferSize, format)) {
                    Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "illegal format:" + format);
                    request.getCallback().callback(response);
                    return null;
                }
            }

            /*
             * If do not need to run in background, stop recording when enter the onStop life cycle.
             */
            AppInfo appInfo = request.getApplicationContext().getAppInfo();
            if (appInfo == null
                    || !appInfo.getConfigInfo().isBackgroundFeature(Record.this.getName())) {
                request
                        .getNativeInterface()
                        .addLifecycleListener(
                                new LifecycleListener() {

                                    @Override
                                    public void onStop() {
                                        super.onStop();
                                        stopRecord(request);
                                        request.getNativeInterface().removeLifecycleListener(this);
                                    }
                                });
            }

            File file = getAudioFile(request, format);
            if (file == null) {
                request.getCallback().callback(new Response(RECORD_CODE_ERROR));
                return null;
            }

            RecordCallbackContext fileCallbackContext = new RecordCallbackContext(request, file);
            putCallbackContext(fileCallbackContext);

            if (VALUE_PCM.equals(format)) {
                mRecorderDelegate = new AudioRecordImpl(request, duration, sampleRate, numberOfChannels, bufferSize, file);
            } else {
                mRecorderDelegate = new MediaRecorderImpl(request, duration, sampleRate, numberOfChannels, encodeBitRate, format, file);
            }
            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper());
            }
            mRecorderDelegate.start();
        } catch (Exception ex) {
            Response response = getExceptionResponse(request, ex);
            request.getCallback().callback(response);
        }
        return null;
    }

    private boolean isValidFormat(int bufferSize, String format) {
        if (!isFormatSupported(format)) {
            return false;
        }
        return bufferSize <= 0 || VALUE_PCM.equals(format);
    }

    private boolean isFormatSupported(String format) {
        return VALUE_3GPP.equals(format) || VALUE_AAC.equals(format) || VALUE_AMR_NB.equals(format) || VALUE_PCM.equals(format);
    }

    private File getAudioFile(Request request, String format) {
        String suffix = getFileSuffix(format);
        try {
            return getScrapFile(request, "audio", suffix);
        } catch (IOException e) {
            return null;
        }
    }

    private String getFileSuffix(String format) {
        String suffix;
        switch (format) {
            case VALUE_AAC:
                suffix = ".aac";
                break;
            case VALUE_AMR_NB:
                suffix = ".amr";
                break;
            case VALUE_PCM:
                suffix = ".pcm";
                break;
            case VALUE_3GPP:
            default:
                suffix = ".3gp";
                break;
        }
        return suffix;
    }

    private Response stopRecord(Request request) {
        if (mRecorderDelegate != null) {
            mRecorderDelegate.stop();
        }
        notifyFeatureStatus(null, request, Display.DISPLAY_STATUS_FINISH);
        return Response.SUCCESS;
    }

    private File getScrapFile(Request request, String prefix, String suffix) throws IOException {
        File cacheDir = request.getApplicationContext().getCacheDir();
        return File.createTempFile(prefix, suffix, cacheDir);
    }

    private JSONObject makeResult(String uri) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_URI, uri);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void onStopRunningInBackground() {
        stopRecord(null);
    }

    private class RecordCallbackContext extends CallbackContext {
        private final File file;

        public RecordCallbackContext(Request request, File file) {
            super(Record.this, ACTION_START_RECORD, request, true);
            this.file = file;
        }

        @Override
        public void callback(int what, Object obj) {
            switch (what) {
                case RECORD_CODE_ERROR:
                    mRequest.getCallback().callback(Response.ERROR);
                    removeCallbackContext(ACTION_START_RECORD);
                    break;
                case RECORD_CODE_STOP:
                case RECORD_CODE_REACH_LIMIT:
                    callbackFilePath(file);
                    removeCallbackContext(ACTION_START_RECORD);
                    break;
            }
        }

        private void callbackFilePath(File file) {
            String internalPath = getRequest().getApplicationContext().getInternalUri(file);
            JSONObject result = makeResult(internalPath);
            Response response = new Response(result);
            Callback callback = getRequest().getCallback();
            callback.callback(response);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mRecorderDelegate.onDestroy();
        }
    }

    private class FrameCallbackContext extends CallbackContext {

        public FrameCallbackContext(Request request) {
            super(Record.this, request.getAction(), request, true);
        }

        @Override
        public void callback(int what, Object obj) {
            if (what == RECORD_CODE_FRAME_BUFFER) {
                SerializeObject result = (SerializeObject) obj;
                getRequest().getCallback().callback(new Response(result));
            }
        }
    }

    private class MediaRecorderImpl extends RecordDelegate {
        private final MediaRecorder recorder;

        public MediaRecorderImpl(Request request, int duration, int sampleRate,
                                 int numberOfChannels, int encodeBitRate, String format, File file) {
            super(request);
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            switch (format) {
                case VALUE_AAC:
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    break;
                case VALUE_AMR_NB:
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    break;
                case VALUE_3GPP:
                default:
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    break;
            }

            recorder.setOutputFile(file.getPath());
            recorder.setMaxDuration(duration);
            recorder.setAudioChannels(numberOfChannels);
            recorder.setAudioSamplingRate(sampleRate);
            recorder.setAudioEncodingBitRate(encodeBitRate);
            recorder.setOnErrorListener((mr, what, extra) -> {
                Log.e(TAG, "Error occurs when record, what=" + what + ", extra=" + extra);
                runCallbackContext(ACTION_START_RECORD, RECORD_CODE_ERROR, null);
            });
            recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        runCallbackContext(ACTION_START_RECORD, RECORD_CODE_REACH_LIMIT, null);
                        notifyFeatureStatus(null, getRequest(), Display.DISPLAY_STATUS_FINISH);
                    }
                }
            });
            try {
                recorder.prepare();
            } catch (IOException e) {
                runCallbackContext(ACTION_START_RECORD, RECORD_CODE_ERROR, null);
            }

        }

        @Override
        public void startInternal() {
            recorder.start();
        }

        @Override
        public void stopInternal() {
            runCallbackContext(ACTION_START_RECORD, RECORD_CODE_STOP, null);
        }

        @Override
        void onDestroy() {
            if (null != recorder) {
                try {
                    recorder.setOnErrorListener(null);
                    recorder.setOnInfoListener(null);
                    recorder.setPreviewDisplay(null);
                    recorder.stop();
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                recorder.release();
            }
        }
    }

    private class AudioRecordImpl extends RecordDelegate {

        private final AudioRecord audioRecord;
        private int bufferSize = 0;
        private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        private static final int MAX_DURATION = 10 * 60 * 1000; // max record duration is 10 minutes.
        private File file;
        private int duration;
        private boolean callbackFrameBuffer = true;

        public AudioRecordImpl(Request request, int duration, int sampleRate, int numberOfChannels, int bufferSize, File file) {
            super(request);
            this.duration = duration;
            if (this.duration > MAX_DURATION || this.duration < 0) {
                this.duration = MAX_DURATION;
            }
            int audioChannelConfig;
            if (numberOfChannels == 1) {
                audioChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            } else {
                audioChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
            }

            callbackFrameBuffer = bufferSize > 0;
            this.bufferSize = Math.max(AudioRecord.getMinBufferSize(sampleRate, audioChannelConfig, audioFormat), bufferSize);
            this.file = file;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, audioChannelConfig, audioFormat, this.bufferSize);
        }

        @Override
        public void startInternal() {
            countdown();
            if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.e(TAG, "AudioRecord uninitialized.");
                runCallbackContext(ACTION_START_RECORD, RECORD_CODE_ERROR, audioRecord);
                return;
            }
            Executors.io().execute(() -> {
                try {
                    //获取到文件的数据流
                    FileOutputStream fos = new FileOutputStream(file, true);
                    byte[] buffer = new byte[this.bufferSize];
                    audioRecord.startRecording();
                    while (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                        int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                        if (bufferReadResult > 0) {
                            fos.write(buffer, 0, bufferReadResult);
                            if (callbackFrameBuffer) {
                                if (bufferReadResult == bufferSize) {
                                    runCallbackContext(EVENT_ON_FRAME_RECORDED, RECORD_CODE_FRAME_BUFFER, makeResult(false, buffer));
                                } else {
                                    byte[] realData = new byte[bufferReadResult];
                                    System.arraycopy(buffer, 0, realData, 0, bufferReadResult);
                                    runCallbackContext(EVENT_ON_FRAME_RECORDED, RECORD_CODE_FRAME_BUFFER, makeResult(false, realData));
                                }
                            }
                        }
                    }
                    fos.close();
                    if (callbackFrameBuffer) {
                        runCallbackContext(EVENT_ON_FRAME_RECORDED, RECORD_CODE_FRAME_BUFFER, makeResult(true, new byte[0]));
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Recording Failed.", t);
                    runCallbackContext(ACTION_START_RECORD, RECORD_CODE_ERROR, audioRecord);
                }
            });
        }

        private void countdown() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopRecord(getRequest());
                }
            }, duration);
        }

        private SerializeObject makeResult(boolean isLastFrame, byte[] bytes) {
            SerializeObject result = new JavaSerializeObject();
            result.put(RESULT_IS_LAST_FRAME, isLastFrame);
            UInt8Array array = new UInt8Array(new ArrayBuffer(bytes));
            result.put(RESULT_FRAME_BUFFER, array);
            return result;
        }

        @Override
        public void stopInternal() {
            runCallbackContext(ACTION_START_RECORD, RECORD_CODE_STOP, null);
        }

        @Override
        void onDestroy() {
            cancelCountdown();
            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
                audioRecord.release();
            }
            file = null;
        }

        private void cancelCountdown() {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    abstract class RecordDelegate {
        private final Request request;

        RecordDelegate(Request request) {
            this.request = request;
        }

        final void start() {
            registerFeature();
            startInternal();
        }

        public Request getRequest() {
            return request;
        }

        final void stop() {
            stopInternal();
            unRegisterFeature();
        }

        abstract void startInternal();

        abstract void stopInternal();

        abstract void onDestroy();

        private void registerFeature() {
            request.getNativeInterface().getResidentManager().postRegisterFeature(Record.this);
        }

        private void unRegisterFeature() {
            request.getNativeInterface().getResidentManager().postUnregisterFeature(Record.this);
        }
    }
}
