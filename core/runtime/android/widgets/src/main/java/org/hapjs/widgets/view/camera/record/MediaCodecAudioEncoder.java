/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.hapjs.common.executors.Executors;
import org.hapjs.widgets.view.camera.CameraBaseMode;

public class MediaCodecAudioEncoder extends MediaEncoder {
    public static final int SAMPLES_PER_FRAME = 1024; // AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25; // AAC, frame/buffer/sec
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaCodecAudioEncoder";
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE =
            44100; // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    private static final int[] AUDIO_SOURCES =
            new int[] {
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.DEFAULT,
                    MediaRecorder.AudioSource.CAMCORDER,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
            };
    private static long mStartTime = -1;
    private AudioRunnable mAudioRunnable = null;

    public MediaCodecAudioEncoder(
            final MediaMuxerController muxer, final MediaEncoderListener listener) {
        super(muxer, listener);
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) {
            Log.v(TAG, "selectAudioCodec:");
        }
        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) { // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG) {
                    Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                }
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) {
            Log.v(TAG, "prepare:");
        }
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "prepare Unable to find an appropriate codec for "
                            + MIME_TYPE);
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "selected codec: " + audioCodecInfo.getName());
        }
        // 参数对应-> mime type、采样率、声道数
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        // KEY_CHANNEL_COUNT  通道数
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        if (DEBUG) {
            Log.i(TAG, "format: " + audioFormat);
        }
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        // 默认16位
        // audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING,16);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG) {
            Log.i(TAG, "prepare finishing");
        }
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "prepare:", e);
            }
        }
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mAudioRunnable == null) {
            mAudioRunnable = new AudioRunnable();
            Executors.io().execute(mAudioRunnable);
        }
    }

    @Override
    protected void release() {
        mAudioRunnable = null;
        super.release();
    }

    protected void signalEndOfInputStream() {
        if (DEBUG) {
            Log.d(TAG, "sending EOS to encoder");
        }
        long timestamp = (System.currentTimeMillis() - mStartTime) * 1000;
        encode(null, 0, timestamp);
        prevOutputPTSUs = 0;
        mStartTime = -1;
    }

    /**
     * 从mic 获取未压缩的16bit PCM data 并写入到 MediaCodec进行编码
     */
    private class AudioRunnable implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int minBufferSize =
                        AudioRecord.getMinBufferSize(
                                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT);
                int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (bufferSize < minBufferSize) {
                    bufferSize =
                            ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
                }

                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord =
                                new AudioRecord(
                                        source,
                                        SAMPLE_RATE,
                                        AudioFormat.CHANNEL_IN_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        bufferSize);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                            audioRecord = null;
                        }
                    } catch (final Exception e) {
                        audioRecord = null;
                    }
                    if (audioRecord != null) {
                        break;
                    }
                }
                if (audioRecord != null) {
                    try {
                        if (mIsCapturing) {
                            if (DEBUG) {
                                Log.v(TAG, "AudioThread:start audio recording");
                            }
                            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                            int readBytes;
                            audioRecord.startRecording();
                            try {
                                for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                    // read audio data from internal mic
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                    if (readBytes > 0) {
                                        // set audio data to encoder
                                        buf.position(readBytes);
                                        buf.flip();
                                        // video queue start
                                        if (mStartTime == -1) {
                                            mStartTime = System.currentTimeMillis();
                                        }
                                        long timestamp =
                                                (System.currentTimeMillis() - mStartTime) * 1000;
                                        if (MediaMuxerController.DEBUG) {
                                            Log.d(
                                                    TAG,
                                                    CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP
                                                            +
                                                            " start AudioRunnable audio_timestamp : "
                                                            + timestamp / 1000
                                                            + " mStartTime : "
                                                            + mStartTime);
                                        }
                                        encode(buf, readBytes, timestamp);
                                        // video queue end
                                        frameAvailableSoon();
                                    }
                                }
                                frameAvailableSoon();
                            } finally {
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    Log.e(TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG + "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "AudioThread#run", e);
            }
            if (DEBUG) {
                Log.v(TAG, "AudioThread:finished");
            }
        }
    }
}
