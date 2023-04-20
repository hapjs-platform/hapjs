/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.model.AppInfo;
import org.hapjs.render.Display;
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
                @ActionAnnotation(name = Record.ACTION_STOP_RECORD, mode = FeatureExtension.Mode.ASYNC)
        })
public class Record extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "system.record";
    protected static final String ACTION_START_RECORD = "start";
    protected static final String ACTION_STOP_RECORD = "stop";
    protected static final String KEY_DURATION = "duration";
    protected static final String KEY_SAMPLERATE = "sampleRate";
    protected static final String KEY_NUMBER_OF_CHANNELS = "numberOfChannels";
    protected static final String KEY_ENCODE_BIT_RATE = "encodeBitRate";
    protected static final String KEY_FORMAT = "format";
    protected static final int DEF_DURATION_MAX = -1;
    protected static final int DEF_SAMPLE_RATE = 8000;
    protected static final int DEF_ENCODE_BIT_RATE = 16000;
    protected static final int DEF_CHANNELS = 2;
    protected static final String VALUE_3GPP = "3gpp";
    protected static final String VALUE_AMR_NB = "amr_nb";
    protected static final String VALUE_AAC = "aac";
    protected static final String RESULT_URI = "uri";
    private static final String TAG = "Record";
    private static final int RECORD_CODE_ERROR = 1;
    private static final int RECORD_CODE_REACH_LIMIT = 2;
    private static final int RECORD_CODE_STOP = 3;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_START_RECORD.equals(action)) {
            stopRecord(request);
            return startRecord(request);
        } else {
            return stopRecord(request);
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
                format = params.optString(KEY_FORMAT, VALUE_3GPP);
                if (!VALUE_3GPP.equals(format)
                        && !VALUE_AAC.equals(format)
                        && !VALUE_AMR_NB.equals(format)) {
                    Response response =
                            new Response(Response.CODE_ILLEGAL_ARGUMENT,
                                    "illegal format:" + format);
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

            RecordCallbackContext recordCallbackContext =
                    new RecordCallbackContext(
                            request, duration, sampleRate, numberOfChannels, encodeBitRate, format);
            putCallbackContext(recordCallbackContext);
        } catch (Exception ex) {
            Response response = getExceptionResponse(request, ex);
            request.getCallback().callback(response);
        }
        return null;
    }

    private Response stopRecord(Request request) {
        runCallbackContext(ACTION_START_RECORD, RECORD_CODE_STOP, null);
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
        MediaRecorder recorder;
        File file;
        int duration;
        int sampleRate;
        int numberOfChannels;
        int encodeBitRate;
        String format;

        public RecordCallbackContext(
                Request request,
                int duration,
                int sampleRate,
                int numberOfChannels,
                int encodeBitRate,
                String format) {
            super(Record.this, ACTION_START_RECORD, request, true);
            this.duration = duration;
            this.sampleRate = sampleRate;
            this.numberOfChannels = numberOfChannels;
            this.encodeBitRate = encodeBitRate;
            this.format = format;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            String suffix = ".3gp";
            switch (format) {
                case VALUE_AAC:
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    suffix = ".aac";
                    break;
                case VALUE_AMR_NB:
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    suffix = ".amr";
                    break;
                case VALUE_3GPP:
                default:
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    break;
            }
            try {
                file = getScrapFile(mRequest, "audio", suffix);
            } catch (IOException e) {
                callback(RECORD_CODE_ERROR, recorder);
                return;
            }
            recorder.setOutputFile(file.getPath());
            recorder.setMaxDuration(duration);
            recorder.setAudioChannels(numberOfChannels);
            recorder.setAudioSamplingRate(sampleRate);
            recorder.setAudioEncodingBitRate(encodeBitRate);
            recorder.setOnErrorListener(
                    new MediaRecorder.OnErrorListener() {
                        @Override
                        public void onError(MediaRecorder mr, int what, int extra) {
                            Log.e(TAG,
                                    "Error occurs when record, what=" + what + ", extra=" + extra);
                            callback(RECORD_CODE_ERROR, mr);
                        }
                    });
            recorder.setOnInfoListener(
                    new MediaRecorder.OnInfoListener() {
                        @Override
                        public void onInfo(MediaRecorder mr, int what, int extra) {
                            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                                callback(RECORD_CODE_REACH_LIMIT, mr);
                                notifyFeatureStatus(null, getRequest(),
                                        Display.DISPLAY_STATUS_FINISH);
                            }
                        }
                    });
            try {
                recorder.prepare();
            } catch (IOException e) {
                callback(RECORD_CODE_ERROR, recorder);
                return;
            }
            getRequest().getNativeInterface().getResidentManager().postRegisterFeature(Record.this);
            recorder.start();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getRequest().getNativeInterface().getResidentManager()
                    .postUnregisterFeature(Record.this);
            if (null != recorder) {
                try {
                    recorder.setOnErrorListener(null);
                    recorder.setOnInfoListener(null);
                    recorder.setPreviewDisplay(null);
                    recorder.stop();
                } catch (IllegalStateException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                } catch (RuntimeException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                recorder.release();
            }
        }

        @Override
        public void callback(int what, Object obj) {
            MediaRecorder recorder = (MediaRecorder) obj;
            if (this.recorder == recorder) {
                if (what == RECORD_CODE_ERROR) {
                    mRequest.getCallback().callback(Response.ERROR);
                    removeCallbackContext(ACTION_START_RECORD);
                } else if (what == RECORD_CODE_REACH_LIMIT) {
                    stopAndCallback();
                }
            } else {
                if (what == RECORD_CODE_STOP) {
                    stopAndCallback();
                }
            }
        }

        private void stopAndCallback() {
            String internalPath = mRequest.getApplicationContext().getInternalUri(file);
            JSONObject result = makeResult(internalPath);
            Response response = new Response(result);
            Callback callback = mRequest.getCallback();
            removeCallbackContext(ACTION_START_RECORD);
            callback.callback(response);
        }
    }
}
