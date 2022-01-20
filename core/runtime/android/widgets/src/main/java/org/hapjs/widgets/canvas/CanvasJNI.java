/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.graphics.Bitmap;
import android.graphics.Rect;
import androidx.annotation.NonNull;

public class CanvasJNI {

    static {
        try {
            System.loadLibrary("canvas");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算bitmap中非空白区域范围
     *
     * @param bitmap 只支持bitmap的format是ARGB_8888
     * @param bounds
     */
    public static native void computeClipWhiteArea(@NonNull Bitmap bitmap, @NonNull Rect bounds);
}
