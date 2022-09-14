/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.hapjs.common.executors.Executors;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.record.common.PacketData;
import org.hapjs.widgets.view.camera.record.common.PacketDataConsumer;
import org.hapjs.widgets.view.camera.record.common.RecordUtils;
import org.hapjs.widgets.view.camera.record.common.VideoPacketPool;

public class MediaMuxerController {
    public static final boolean DEBUG = false;
    private static final String TAG = "MediaMuxerController";
    public static boolean mIsVideoEnable = true;
    public static boolean mIsAudioEnable = true;
    private static volatile MediaMuxerController mediaMuxerController = null;
    private final MediaMuxer mMediaMuxer; // API >= 18
    public CameraSurfaceRender.OnVideoStatusListener mOnVideoStatusListener;
    PacketDataConsumer packetDataConsumer = null;
    private int mEncoderCount;
    private int mStatredCount;
    private boolean mIsStarted;
    private TextureMovieEncoder mVideoEncoder;
    private MediaEncoder mAudioEncoder;
    private int mVideoTrack = -1;
    private int mAudioTrack = -1;

    /**
     * Constructor
     *
     * @param ext extension of output file
     * @throws IOException
     */
    private MediaMuxerController(String ext) throws IOException {
        mMediaMuxer = new MediaMuxer(ext, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }

    public static MediaMuxerController getInstance(String ext) {
        if (null == mediaMuxerController) {
            synchronized (MediaMuxerController.class) {
                if (null == mediaMuxerController) {
                    try {
                        if (TextUtils.isEmpty(ext)) {
                            return mediaMuxerController;
                        }
                        mediaMuxerController = new MediaMuxerController(ext);
                    } catch (IOException e) {
                        Log.e(
                                TAG,
                                CameraBaseMode.VIDEO_RECORD_TAG + " getInstance  IOException : "
                                        + e.getMessage());
                    }
                }
            }
        }
        return mediaMuxerController;
    }

    public void setOnVideoStartedListener(
            CameraSurfaceRender.OnVideoStatusListener onVideoStatusListener) {
        this.mOnVideoStatusListener = onVideoStatusListener;
    }

    public void initStartStatus() {
        if (mEncoderCount > 0 || mStatredCount > 0 || mIsStarted) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + " initStartStatus mEncoderCount : "
                            + mEncoderCount
                            + " mStatredCount : "
                            + mStatredCount
                            + " mIsStarted : "
                            + mIsStarted);
        }
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }

    public void prepare() throws IOException {
        if (mAudioEncoder != null) {
            mAudioEncoder.prepare();
        }
    }

    public void startRecording() {
        VideoPacketPool.getInstance().initVideoPacketPool();
        if (mVideoEncoder != null) {
            mVideoEncoder.startVideoRecording();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.startRecording();
        }
    }

    public void stopRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.stopRecording();
        }
        mAudioEncoder = null;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaCodecAudioEncoder) {
            if (mAudioEncoder != null) {
                Log.e(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "addEncoder MediaAudioEncoder encoder already added.");
                return;
            }
            mAudioEncoder = encoder;
        } else {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "addEncoder unsupported encoder");
            return;
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    public void addVideoEncoder(final TextureMovieEncoder encoder) {
        if (encoder instanceof TextureMovieEncoder) {
            if (mVideoEncoder != null) {
                Log.e(
                        TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                + "addVideoEncoder Video encoder already added.");
                return;
            }
            mVideoEncoder = encoder;
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * 获取编码后数据,开始录制
     */
    public synchronized boolean start() {
        if (DEBUG) {
            Log.v(TAG, "start:");
        }
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            try {
                mMediaMuxer.start();
            } catch (Exception e) {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " mMediaMuxer.start error : " + e.getMessage());
            }
            mIsStarted = true;
            notifyAll();
            if (null != mOnVideoStatusListener) {
                mOnVideoStatusListener.onVideoStarted();
            }
            // video queue start
            startConsumer();
            // video queue end
            if (DEBUG) {
                Log.v(TAG, "MediaMuxer started:");
            }
        }
        return mIsStarted;
    }

    public void startConsumer() {
        packetDataConsumer = new PacketDataConsumer();
        Executors.io().execute(packetDataConsumer);
    }

    /**
     * 接收到EOS后，停止录制
     */
    /*package*/
    public synchronized void stop() {
        if (DEBUG) {
            Log.v(TAG, "stop:mStatredCount=" + mStatredCount);
        }
        Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "mEncoderCount : " + mEncoderCount
                + " mStatredCount : " + mStatredCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            // java.lang.IllegalStateException: Failed to stop the muxer
            // video queue start
            if (null != packetDataConsumer) {
                packetDataConsumer.setRunning(false);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "packetDataConsumer is null.");
            }
            // 清空缓存队列数据
            VideoPacketPool.getInstance().abortVideoPacketList();
            VideoPacketPool.getInstance().abortAudioPacketList();
            VideoPacketPool.getInstance().cleanAudioVideoDatas();
            try {
                if (mIsStarted) {
                    mMediaMuxer.stop();
                    mMediaMuxer.release();
                } else {
                    Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " mMediaMuxer.stop mIsStarted false.");
                }
            } catch (Exception e) {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " mMediaMuxer.stop error : "
                        + e.getMessage());
            }
            mIsStarted = false;
            // video queue end
            if (DEBUG) {
                Log.v(TAG, "MediaMuxer stopped:");
            }
            mediaMuxerController = null;
            packetDataConsumer = null;
            if (null != mOnVideoStatusListener) {
                mOnVideoStatusListener.onVideoStoped();
            } else {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                        + " stop error mOnVideoStatusListener is null.");
            }
            mOnVideoStatusListener = null;
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " stop success.");
        } else {
            Log.w(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + " stop error mEncoderCount : "
                            + mEncoderCount
                            + " mStatredCount : "
                            + mStatredCount);
        }
    }

    public synchronized void stopMuxer() {
        if (mIsStarted && null != mMediaMuxer) {
            try {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (Exception e) {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " stopMuxer error : " + e.getMessage());
            }
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " stopMuxer warning  mIsStarted false or mMediaMuxer null. ");
        }
    }

    /**
     * 编码数据添加到muxer
     */
    /*package*/
    public synchronized int addTrack(final MediaFormat format, boolean isVideo) {
        if (mIsStarted) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "addTrack  muxer already started mIsStarted : "
                            + mIsStarted);
            return -1;
        }
        int trackIx = -1;
        try {
            trackIx = mMediaMuxer.addTrack(format);
        } catch (Exception e) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " mMediaMuxer.addTrack error : " + e.getMessage());
        }
        if (DEBUG) {
            Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format="
                    + format);
        }
        // video queue start
        if (isVideo) {
            mVideoTrack = trackIx;
        } else {
            mAudioTrack = trackIx;
        }
        // video queue end
        return trackIx;
    }

    /**
     * ******************************video audio data
     * store***********************************************************
     */
    public void pushVideoDataToQueue(
            byte[] encodedData, MediaCodec.BufferInfo bufferInfo, boolean isKeyFrame) {
        PacketData packetData = new PacketData();
        if (isKeyFrame) {
            packetData.mNaluType = RecordUtils.H264_NALU_TYPE_IDR_PICTURE;
        }
        packetData.buffer = encodedData;
        packetData.bufferInfo = bufferInfo;
        packetData.timeMills = bufferInfo.presentationTimeUs / 1000;
        VideoPacketPool.getInstance().pushRecordingVideoPacketToQueue(packetData);
    }

    public void pushAudioDataToQueue(byte[] encodedData, MediaCodec.BufferInfo bufferInfo) {
        PacketData packetData = new PacketData();
        packetData.buffer = encodedData;
        packetData.bufferInfo = bufferInfo;
        // um ---> ms
        packetData.timeMills = bufferInfo.presentationTimeUs / 1000;
        packetData.duration = bufferInfo.size;
        VideoPacketPool.getInstance().addAudioPacketData(packetData);
    }

    public synchronized void writeSampleData(
            boolean isVideo, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStatredCount > 0) {
            if (null != byteBuf && null != bufferInfo) {
                try {
                    if (isVideo) {
                        if (bufferInfo.size != 0) {
                            bufferInfo.offset = 0;
                            byteBuf.position(bufferInfo.offset);
                            byteBuf.limit(bufferInfo.offset + bufferInfo.size);
                            if (MediaMuxerController.DEBUG) {
                                Log.d(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP
                                                + " end writeSampleData video_timestamp : "
                                                + bufferInfo.presentationTimeUs / 1000);
                            }
                            mMediaMuxer.writeSampleData(mVideoTrack, byteBuf, bufferInfo);
                        }
                    } else {
                        if (bufferInfo.size != 0) {
                            bufferInfo.offset = 0;
                            byteBuf.position(bufferInfo.offset);
                            byteBuf.limit(bufferInfo.offset + bufferInfo.size);
                            if (MediaMuxerController.DEBUG) {
                                Log.d(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP
                                                + " end writeAudioFrameData audio_timestamp : "
                                                + bufferInfo.presentationTimeUs / 1000);
                            }
                            mMediaMuxer.writeSampleData(mAudioTrack, byteBuf, bufferInfo);
                        }
                    }
                } catch (Exception e) {
                    // if (bufferInfo.size < 0 || bufferInfo.offset < 0 || (bufferInfo.offset +
                    // bufferInfo.size) > byteBuf.capacity()|| bufferInfo.presentationTimeUs < 0)
                    Log.e(
                            TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG
                                    + " writeSampleData isVideo "
                                    + isVideo
                                    + " bufferInfo.size : "
                                    + bufferInfo.size
                                    + "bufferInfo.offset : "
                                    + bufferInfo.offset
                                    + "  byteBuf.capacity() : "
                                    + byteBuf.capacity()
                                    + " bufferInfo.presentationTimeUs: "
                                    + bufferInfo.presentationTimeUs);
                    Log.e(
                            TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG
                                    + " writeSampleData isVideo "
                                    + isVideo
                                    + " error : "
                                    + e.getMessage());
                }
            }
        }
    }
}
