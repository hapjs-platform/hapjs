/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.common;

import android.util.Log;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.record.MediaMuxerController;

public class VideoPacketPool {
    public static final String AUDIO_TAG = "  encode_audio  ";
    public static final String VIDEO_TAG = "  encode_video  ";
    public static final String AUDIO_VIDEO_WRITE_TAG = "  encode_writeSampleData  ";
    private static final String TAG = "VideoPacketPool";
    private final Object mAudioLock = new Object();
    private final Object mVideoLock = new Object();
    private volatile LinkedBlockingQueue<PacketData> mPCMAudioList = new LinkedBlockingQueue<>();
    private volatile LinkedBlockingQueue<PacketData> mEncoderVideoList =
            new LinkedBlockingQueue<>();
    private Object mLockTotalDuration = new Object();
    private PacketData mTempVideoPacket;
    private int mTotalDiscardVideoPacketDuration;
    private PacketData mFirstVideoData = null;
    private float mCurrentTimeMills = RecordUtils.NON_DROP_FRAME_FLAG;
    private volatile boolean mAbortAudioRequest;
    private volatile boolean mAbortVideoRequest;
    private boolean mDebugGopFrame = false;

    public static VideoPacketPool getInstance() {
        return Holder.INSTANCE;
    }

    public void initVideoPacketPool() {
        mAbortVideoRequest = false;
        mAbortAudioRequest = false;
        cleanAudioVideoDatas();
    }

    public void addAudioPacketData(PacketData data) {
        if (!MediaMuxerController.mIsAudioEnable) {
            return;
        }
        if (null == data) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "  addAudioPacketData data is null.");
            return;
        }
        try {
            if (!mAbortAudioRequest) {
                mPCMAudioList.put(data);
            }
        } catch (Exception e) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "addAudioPacketData Exception error : "
                            + e.getMessage());
        }
    }

    public void removeVideoPacketData(PacketData data) {
        if (null == data) {
            return;
        }
        synchronized (mAudioLock) {
            mPCMAudioList.remove(data);
        }
    }

    public PacketData getAudioPacketData() {
        PacketData data = null;
        if (mAbortAudioRequest) {
            return null;
        }
        if (MediaMuxerController.mIsVideoEnable) {
            while (precheckAudioPacket()) {
                if (!removeAndUpdateAudioPacket()) {
                    break;
                }
            }
        }
        try {
            data = mPCMAudioList.take();
        } catch (Exception e) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "getAudioPacketData Exception error : "
                            + e.getMessage());
        }
        return data;
    }

    public boolean precheckAudioPacket() {
        boolean ret = false;
        synchronized (mLockTotalDuration) {
            ret =
                    mTotalDiscardVideoPacketDuration
                            >= (RecordUtils.AUDIO_PACKET_DURATION_IN_SECS * 1000.0f);
        }

        return ret;
    }

    boolean removeAndUpdateAudioPacket() {
        boolean ret = false;
        ret = removeAudioHeader();
        if (ret) {
            synchronized (mLockTotalDuration) {
                mTotalDiscardVideoPacketDuration -=
                        (RecordUtils.AUDIO_PACKET_DURATION_IN_SECS * 1000.0f);
            }
        }
        return ret;
    }

    public boolean removeAudioHeader() {
        PacketData data = null;
        try {
            data = mPCMAudioList.poll();
        } catch (Exception e) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "removeAudioHeader Exception error : "
                            + e.getMessage());
        }
        return data != null;
    }

    public int getAudioSize() {
        synchronized (mAudioLock) {
            return mPCMAudioList.size();
        }
    }

    public int getVideoSize() {
        synchronized (mVideoLock) {
            return mEncoderVideoList.size();
        }
    }

    /**
     * ***********************decoder video packetdata******************************************
     */
    public PacketData getVideoPacket() {
        PacketData packetData = null;
        if (mAbortVideoRequest) {
            return null;
        }
        try {
            packetData = mEncoderVideoList.take();
        } catch (Exception e) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG + "getVideoPacket Exception error : "
                            + e.getMessage());
        }

        return packetData;
    }

    boolean checkVideoPacket() {
        boolean countLarge = false;
        synchronized (mVideoLock) {
            countLarge = mEncoderVideoList.size() > RecordUtils.VIDEO_PACKET_QUEUE_THRRESHOLD;
        }
        return countLarge;
    }

    public boolean pushRecordingVideoPacketToQueue(PacketData packetData) {
        boolean dropFrame = false;
        if (mDebugGopFrame) {
            Log.d(
                    TAG, VIDEO_TAG + "pushRecordingVideoPacketToQueue before : "
                            + mEncoderVideoList.size());
        }
        if (MediaMuxerController.mIsAudioEnable) {
            while (checkVideoPacket()) {
                dropFrame = true;
                int discardVideoFrameCnt = 0;
                int discardVideoFrameDuration = removeGOP();
                if (mDebugGopFrame) {
                    Log.d(
                            TAG,
                            VIDEO_TAG
                                    +
                                    " pushRecordingVideoPacketToQueue checktime  discardVideoFrameDuration : "
                                    + discardVideoFrameDuration);
                }
                if (discardVideoFrameDuration < 0) {
                    break;
                }
                recordDropVideoFrame(discardVideoFrameDuration);
            }
        }
        if (mDebugGopFrame) {
            Log.d(
                    TAG, VIDEO_TAG + "pushRecordingVideoPacketToQueue after  : "
                            + mEncoderVideoList.size());
        }
        // 计算当前帧的Duration, 延迟一帧放入Queue中
        if (null != mTempVideoPacket) {
            long packetDuration = packetData.timeMills - mTempVideoPacket.timeMills;
            mTempVideoPacket.duration = packetDuration;
            try {
                if (!mAbortVideoRequest) {
                    mEncoderVideoList.put(mTempVideoPacket);
                }
            } catch (Exception e) {
                Log.e(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "pushRecordingVideoPacketToQueue Exception error : "
                                + e.getMessage());
            }
        }
        mTempVideoPacket = packetData;
        if (mDebugGopFrame) {
            Log.d(TAG, VIDEO_TAG + "pushRecordingVideoPacketToQueue final : "
                    + mEncoderVideoList.size());
        }
        return dropFrame;
    }

    int getRecordingVideoPacketQueueSize() {
        synchronized (mEncoderVideoList) {
            return mEncoderVideoList.size();
        }
    }

    void clearVideoPacketData() {
        synchronized (mEncoderVideoList) {
            mEncoderVideoList.clear();
        }
    }

    int removeGOP() {
        synchronized (mVideoLock) {
            int discardVideoFrameDuration = 0;
            boolean isFirstFrameIDR = false;
            boolean tmpFirstFrameIDR = false;
            Iterator<PacketData> iterator = mEncoderVideoList.iterator();
            final Iterator<PacketData> removeIterator = mEncoderVideoList.iterator();
            if (iterator.hasNext()) {
                mFirstVideoData = iterator.next();
            }
            if (mFirstVideoData != null) {
                int naluType = mFirstVideoData.getNALUType();
                if (naluType == RecordUtils.H264_NALU_TYPE_IDR_PICTURE) {
                    isFirstFrameIDR = true;
                    tmpFirstFrameIDR = true;
                }
            }
            Iterator<PacketData> pktList = iterator;
            for (; ; ) {
                if (mAbortVideoRequest) {
                    discardVideoFrameDuration = 0;
                    break;
                }
                if (pktList != null) {
                    PacketData currentVideoData = mFirstVideoData;
                    if (currentVideoData != null) {
                        int naluType = currentVideoData.getNALUType();
                        if (RecordUtils.NON_DROP_FRAME_FLAG == mCurrentTimeMills) {
                            mCurrentTimeMills = currentVideoData.timeMills;
                        }
                        if (naluType == RecordUtils.H264_NALU_TYPE_IDR_PICTURE) {
                            if (isFirstFrameIDR) {
                                isFirstFrameIDR = false;
                                if (pktList.hasNext()) {
                                    mFirstVideoData = pktList.next();
                                }
                                discardVideoFrameDuration += currentVideoData.duration;
                                currentVideoData.isRemove = true;
                                continue;
                            } else {
                                break;
                            }
                        } else if (naluType == RecordUtils.H264_NALU_TYPE_NON_IDR_PICTURE) {
                            if (pktList.hasNext()) {
                                mFirstVideoData = pktList.next();
                            }
                            discardVideoFrameDuration += currentVideoData.duration;
                            currentVideoData.isRemove = true;
                            if (!tmpFirstFrameIDR) {
                                break;
                            }
                        } else {
                            // sps pps 的问题
                            discardVideoFrameDuration = -1;
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            PacketData tmpData = null;
            if (null != removeIterator) {
                while (removeIterator.hasNext()) {
                    tmpData = removeIterator.next();
                    if (tmpData.isRemove) {
                        removeIterator.remove();
                    } else {
                        break;
                    }
                }
            }
            return discardVideoFrameDuration;
        }
    }

    void recordDropVideoFrame(int discardVideoPacketDuration) {
        synchronized (mLockTotalDuration) {
            mTotalDiscardVideoPacketDuration += discardVideoPacketDuration;
        }
    }

    public void abortAudioPacketList() {
        mAbortAudioRequest = true;
    }

    public void abortVideoPacketList() {
        mAbortVideoRequest = true;
    }

    public void cleanAudioVideoDatas() {
        synchronized (mVideoLock) {
            mFirstVideoData = null;
            mTempVideoPacket = null;
            mEncoderVideoList.clear();
            mTotalDiscardVideoPacketDuration = 0;
            mCurrentTimeMills = RecordUtils.NON_DROP_FRAME_FLAG;
        }
        synchronized (mAudioLock) {
            mPCMAudioList.clear();
        }
    }

    private static class Holder {
        static final VideoPacketPool INSTANCE = new VideoPacketPool();
    }
}
