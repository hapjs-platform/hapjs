/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.hapjs.widgets.view.camera.CameraBaseMode;

public class VideoEncoderCore {
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = false;

    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25; // 25fps
    private static final int FRAME_COMPRESS_RATE = 20;
    private static final int IFRAME_INTERVAL = 1; // 1 seconds between I-frames
    // 视频比特数 比如一个像素3字节 , 比特率 = 宽 * 高 * 帧率 * 像素位数
    private static final float BPP = 0.25f;
    private Surface mInputSurface;
    private MediaMuxerController mMuxer = null;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    /**
     * 上一次进行写入编码数据的时间戳
     */
    private volatile long prevOutputPTSUs = 0;

    public VideoEncoderCore(int width, int height, boolean isCompress, File outputFile, int bitRate)
            throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        if (isCompress) {
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_COMPRESS_RATE);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        } else {
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate(width, height));
        }
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        mMuxer = MediaMuxerController.getInstance(outputFile.toString());
        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    private int calcBitRate(int width, int height) {
        final int bitrate = (int) (BPP * FRAME_RATE * width * height);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    /**
     * 大于720*1280则以 720*1280为基准， 720/realHeight = width / height 小于720*1280则以 width *height *3为基准码率
     *
     * @param width
     * @param height
     * @return
     */
    private int calcComppressBitRate(int width, int height) {
        final int bitrate = (3 * width * height);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * 释放编码资源
     */
    public void release() {
        if (VERBOSE) {
            Log.d(TAG, "releasing encoder objects");
        }
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer = null;
        }
    }

    public void drainEncoder(boolean endOfStream) {
        final int timeoutUsec = 10000;
        if (VERBOSE) {
            Log.d(TAG, "drainEncoder(" + endOfStream + ")");
        }

        if (endOfStream) {
            if (VERBOSE) {
                Log.d(TAG, "sending EOS to encoder");
            }
            mEncoder.signalEndOfInputStream();
            prevOutputPTSUs = 0;
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        LOOP:
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, timeoutUsec);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break; // out of while
                } else {
                    if (VERBOSE) {
                        Log.d(TAG, "no output available, spinning to await EOS");
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    Log.e(TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG + "drainEncoder format changed twice.");
                    return;
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                // now that we have the Magic Goodies, start the muxer
                try {
                    if (null != mMuxer) {
                        mTrackIndex = mMuxer.addTrack(newFormat, true);
                        mMuxerStarted = true;
                    } else {
                        Log.e(TAG,
                                CameraBaseMode.VIDEO_RECORD_TAG + "drainEncoder mMuxer is null.");
                    }
                } catch (IllegalStateException e) {
                    Log.e(
                            TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG
                                    + "drainEncoder IllegalStateException error : "
                                    + e.getMessage());
                }

                if (null != mMuxer && !mMuxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (mMuxer) {
                        while (!mMuxer.isStarted()) {
                            try {
                                mMuxer.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                        }
                    }
                }
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    Log.e(
                            TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG
                                    + "drainEncoder encoderOutputBuffer "
                                    + encoderStatus
                                    + " was null");
                    return;
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) {
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    }
                    mBufferInfo.size = 0;
                }
                // video queue start
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        // throw new RuntimeException("muxer hasn't started");
                        Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                + " drainEncoder muxer hasn't started");
                    }
                    byte[] tmpBytes = null;
                    if (null != encodedData && null != mBufferInfo && mBufferInfo.size > 4) {
                        tmpBytes = new byte[mBufferInfo.size];
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        encodedData.get(tmpBytes, 0, tmpBytes.length);
                    }
                    boolean keyFrame = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                    if (mBufferInfo.presentationTimeUs > prevOutputPTSUs || prevOutputPTSUs == 0) {
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
                                            + " middle drainEncoder video_timestamp : "
                                            + mBufferInfo.presentationTimeUs / 1000);
                        }
                        mMuxer.pushVideoDataToQueue(tmpBytes, bufferInfo, keyFrame);
                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    } else {
                        Log.w(
                                TAG,
                                CameraBaseMode.VIDEO_RECORD_TAG
                                        +
                                        " drainEncoder prevOutputPTSUs is not valid  mBufferInfo.presentationTimeUs : "
                                        + mBufferInfo.presentationTimeUs
                                        + " prevOutputPTSUs : "
                                        + prevOutputPTSUs);
                    }
                }
                // video queue end
                mEncoder.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                + "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) {
                            Log.d(TAG, "end of stream reached");
                        }
                    }
                    break; // out of while
                }
            }
        }
    }
}
