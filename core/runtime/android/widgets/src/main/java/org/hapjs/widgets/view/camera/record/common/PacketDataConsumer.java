/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.common;

import android.media.MediaCodec;
import android.util.Log;
import java.nio.ByteBuffer;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.record.MediaMuxerController;

public class PacketDataConsumer implements Runnable {
    private static final String TAG = "PacketDataConsumer";
    private boolean mRunning = true;
    private volatile long mLastVideoStamp = -1;
    private volatile long mLastAudioStamp = 0;

    public PacketDataConsumer() {
        mLastVideoStamp = -1;
        mLastAudioStamp = 0;
    }

    @Override
    public void run() {
        while (mRunning) {
            int ret = encode();
            if (ret < 0) {
                break;
            }
        }
    }

    int encode() {
        int ret = 0;
        /* Compute current audio and video time. 时间戳来源上一帧的时间戳*/
        double videoTime = getVideoStreamTimeInSecs();
        double audioTime = getAudioStreamTimeInSecs();
        /// * write interleaved audio and video frames */
        boolean isAudioWrite = false;
        if (audioTime < videoTime) {
            isAudioWrite = true;
            ret = writeAudioFrameData();
        } else {
            ret = writeVideoFrameData();
        }
        if (MediaMuxerController.DEBUG) {
            Log.d(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + VideoPacketPool.AUDIO_VIDEO_WRITE_TAG
                            + (isAudioWrite ? "audio" : "video")
                            + " timeStamp   audio_time : "
                            + audioTime
                            + " video_time : "
                            + videoTime);
        }
        ret = 0;
        return ret;
    }

    public long getVideoStreamTimeInSecs() {
        return mLastVideoStamp;
    }

    public long getAudioStreamTimeInSecs() {
        return mLastAudioStamp;
    }

    public int writeVideoFrameData() {
        int ret = -1;
        MediaMuxerController mediaMuxerWrapper = MediaMuxerController.getInstance("");
        if (null == mediaMuxerWrapper) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "writeVideoFrameData error mediaMuxerWrapper is null");
            return ret;
        }
        PacketData packetData = VideoPacketPool.getInstance().getVideoPacket();
        if (null != packetData && null != mediaMuxerWrapper) {
            MediaCodec.BufferInfo bufferInfo = packetData.bufferInfo;
            ByteBuffer byteBuffer = ByteBuffer.wrap(packetData.buffer);
            ret = 0;
            if (bufferInfo.size != 0 && null != byteBuffer) {
                mLastVideoStamp = bufferInfo.presentationTimeUs;
                mediaMuxerWrapper.writeSampleData(true, byteBuffer, bufferInfo);
            }
        }
        return ret;
    }

    public int writeAudioFrameData() {
        int ret = -1;
        MediaMuxerController mediaMuxerWrapper = MediaMuxerController.getInstance("");
        if (null == mediaMuxerWrapper) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "writeAudioFrameData error mediaMuxerWrapper is null");
            return ret;
        }
        PacketData packetData = VideoPacketPool.getInstance().getAudioPacketData();
        if (null != packetData && null != mediaMuxerWrapper) {
            MediaCodec.BufferInfo bufferInfo = packetData.bufferInfo;
            ByteBuffer byteBuffer = ByteBuffer.wrap(packetData.buffer);
            ret = 0;
            if (bufferInfo.size != 0 && null != byteBuffer) {
                mLastAudioStamp = bufferInfo.presentationTimeUs;
                mediaMuxerWrapper.writeSampleData(false, byteBuffer, bufferInfo);
            }
        }
        return ret;
    }

    public void setRunning(boolean isRun) {
        mRunning = isRun;
    }
}
