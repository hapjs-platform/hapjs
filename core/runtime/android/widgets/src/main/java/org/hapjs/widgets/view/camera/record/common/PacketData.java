/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.common;

import android.media.MediaCodec;

public class PacketData {
    public MediaCodec.BufferInfo bufferInfo;
    public int size;
    public long timeMills;
    public byte[] buffer;
    public long duration;
    public long pts;
    public long dts;
    public boolean isRemove;
    public int mNaluType = RecordUtils.H264_NALU_TYPE_NON_IDR_PICTURE;

    int getNALUType() {
        int naluType = RecordUtils.H264_NALU_TYPE_NON_IDR_PICTURE;
        if (null != buffer && buffer.length > 4) {
            naluType = (buffer[4] & 0x1F);
        }
        return naluType;
    }
}
