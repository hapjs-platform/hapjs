/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import org.hapjs.common.executors.Executors;
import org.hapjs.widgets.view.camera.CameraBaseMode;

public abstract class MediaEncoder implements Runnable {
    protected static final int TIMEOUT_USEC = 10000; // 10[msec]
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaEncoder";
    protected final Object mSync = new Object();
    /**
     * 标识正在录制
     */
    protected volatile boolean mIsCapturing;
    /**
     * 标识停止录制
     */
    protected volatile boolean mRequestStop;
    /**
     * 标识接收到 EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * 标识混合器muxer正在运行
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec; // API >= 16(Android4.1.2)
    protected WeakReference<MediaMuxerController> mWeakMuxer;
    protected MediaEncoderListener mListener;
    protected volatile long prevOutputPTSUs = 0;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    private int mRequestDrain;
    private MediaCodec.BufferInfo mBufferInfo; // API >= 16(Android4.1.2)

    public MediaEncoder(final MediaMuxerController muxer, final MediaEncoderListener listener) {
        if (listener == null) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "MediaEncoderListener is null");
            return;
        }
        if (muxer == null) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "MediaMuxerController is null");
            return;
        }
        mWeakMuxer = new WeakReference<MediaMuxerController>(muxer);
        muxer.addEncoder(this);
        mListener = listener;
        synchronized (mSync) {
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            Executors.io().execute(this);
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                Log.e(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + " MediaEncoder InterruptedException : "
                                + e.getMessage());
            }
        }
    }

    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
        synchronized (mSync) {
            mRequestStop = false;
            mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (mSync) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain) {
                    mRequestDrain--;
                }
            }
            if (localRequestStop) {
                drain();
                // request stop recording
                signalEndOfInputStream();
                // process output data again for EOS signale
                drain();
                // release all related objects
                release();
                break;
            }
            if (localRequestDrain) {
                drain();
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        if (DEBUG) {
            Log.d(TAG, "Encoder thread exiting");
        }
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    abstract void prepare() throws IOException;

    void startRecording() {
        if (DEBUG) {
            Log.v(TAG, "startRecording");
        }
        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            mSync.notifyAll();
        }
    }

    /**
     * the method to request stop encoding
     */
    void stopRecording() {
        if (DEBUG) {
            Log.v(TAG, "stopRecording");
        }
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true; // for rejecting newer frame
            mSync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

    /**
     * Release all releated objects
     */
    protected void release() {
        if (DEBUG) {
            Log.d(TAG, "release:");
        }
        try {
            mListener.onStopped(this);
        } catch (final Exception e) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "failed onStopped", e);
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "failed releasing MediaCodec", e);
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerController muxer = mWeakMuxer != null ? mWeakMuxer.get() : null;
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (final Exception e) {
                    Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "failed stopping muxer", e);
                }
            } else {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " release error  muxer is null");
            }
        } else {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " release error mMuxerStarted is false.");
        }
        mBufferInfo = null;
    }

    // ********************************************************************************
    // ********************************************************************************

    protected void signalEndOfInputStream() {
        if (DEBUG) {
            Log.d(TAG, "sending EOS to encoder");
        }
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
        // mMediaCodec.signalEndOfInputStream(); // API >= 18
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length,
                          final long presentationTimeUs) {
        if (!mIsCapturing) {
            return;
        }
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                    if (DEBUG) {
                        Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    }
                    mMediaCodec.queueInputBuffer(
                            inputBufferIndex, 0, 0, presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mMediaCodec
                            .queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    protected void drain() {
        if (mMediaCodec == null) {
            return;
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus = 0;
        int count = 0;
        final MediaMuxerController muxer = mWeakMuxer.get();
        if (muxer == null) {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "muxer is unexpectedly null");
            return;
        }
        LOOP:
        while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 5) {
                        break LOOP; // out of while
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (DEBUG) {
                    Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                }
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (DEBUG) {
                    Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                }
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) { // second time request is error
                    Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "drain format changed twice");
                    return;
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                try {
                    final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                    mTrackIndex = muxer.addTrack(format, false);
                    mMuxerStarted = true;
                } catch (IllegalStateException e) {
                    Log.e(TAG, "IllegalStateException error : " + e.getMessage());
                }

                if (!muxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (muxer) {
                        while (!muxer.isStarted()) {
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                        }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status
                if (DEBUG) {
                    Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: "
                            + encoderStatus);
                }
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    Log.e(
                            TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG
                                    + "drain encoderOutputBuffer "
                                    + encoderStatus
                                    + " was null");
                    return;
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED
                    // don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    if (DEBUG) {
                        Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    }
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will prrograming failure.
                        Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "drain :muxer hasn't started");
                        return;
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    // video queue start
                    byte[] tmpBytes = null;
                    if (null != encodedData && null != mBufferInfo && mBufferInfo.size > 4) {
                        tmpBytes = new byte[mBufferInfo.size];
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        encodedData.get(tmpBytes, 0, tmpBytes.length);
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    bufferInfo.set(
                            mBufferInfo.offset,
                            mBufferInfo.size,
                            mBufferInfo.presentationTimeUs,
                            mBufferInfo.flags);
                    if (MediaMuxerController.DEBUG) {
                        Log.d(
                                TAG,
                                CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP
                                        + " middle drain audio_timestamp : "
                                        + mBufferInfo.presentationTimeUs / 1000);
                    }
                    muxer.pushAudioDataToQueue(tmpBytes, bufferInfo);
                    // video queue end
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break; // out of while
                }
            }
        }
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs) {
            result = (prevOutputPTSUs - result) + result;
        }
        return result;
    }

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);
    }
}
