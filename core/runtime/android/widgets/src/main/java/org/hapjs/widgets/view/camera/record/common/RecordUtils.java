/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.common;

public class RecordUtils {
    public static final int H264_NALU_TYPE_NON_IDR_PICTURE = 1;
    public static final int H264_NALU_TYPE_IDR_PICTURE = 5;
    public static final int H264_NALU_TYPE_SEQUENCE_PARAMETER_SET = 7;
    public static final int H264_NALU_TYPE_PICTURE_PARAMETER_SET = 8;
    public static final int H264_NALU_TYPE_SEI = 6;

    public static final int VIDEO_PACKET_QUEUE_THRRESHOLD = 60;
    public static final float AUDIO_PACKET_DURATION_IN_SECS = 0.04f;
    public static final float NON_DROP_FRAME_FLAG = -1.0f;
    public static final float DTS_PARAM_UN_SETTIED_FLAG = -1;
    public static final float DTS_PARAM_NOT_A_NUM_FLAG = -2;
    public static final float PTS_PARAM_UN_SETTIED_FLAG = -1;
}
