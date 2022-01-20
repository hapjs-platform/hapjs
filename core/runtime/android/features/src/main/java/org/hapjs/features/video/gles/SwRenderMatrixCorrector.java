/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.video.gles;

import android.graphics.Rect;
import android.media.MediaFormat;
import android.util.Log;
import java.util.Arrays;

/**
 * 存在解码时使用的是软件方式渲染,而又存在部分机型软件渲染时无法返回正确的矩阵,所以软渲染统一自行计算矩阵
 */
public class SwRenderMatrixCorrector {

    private static final int FLIP_H = 0x01;
    private static final int FLIP_V = 0x02;
    private static final int ROT_90 = 0x04;
    static float[] mtxIdentity = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
    };
    private float[] matrix;

    public SwRenderMatrixCorrector() {
    }

    public void createSwRenderMatrix(MediaFormat format, int rotate) {
        matrix = calculateMatrixByFormat(format, rotate);
        Log.d("render matrix", Arrays.toString(matrix));
    }

    private void mtxMul(float[] out, float[] first, float[] second) {
        out[0] =
                first[0] * second[0] + first[4] * second[1] + first[8] * second[2]
                        + first[12] * second[3];
        out[1] =
                first[1] * second[0] + first[5] * second[1] + first[9] * second[2]
                        + first[13] * second[3];
        out[2] =
                first[2] * second[0] + first[6] * second[1] + first[10] * second[2]
                        + first[14] * second[3];
        out[3] =
                first[3] * second[0] + first[7] * second[1] + first[11] * second[2]
                        + first[15] * second[3];

        out[4] =
                first[0] * second[4] + first[4] * second[5] + first[8] * second[6]
                        + first[12] * second[7];
        out[5] =
                first[1] * second[4] + first[5] * second[5] + first[9] * second[6]
                        + first[13] * second[7];
        out[6] =
                first[2] * second[4] + first[6] * second[5] + first[10] * second[6]
                        + first[14] * second[7];
        out[7] =
                first[3] * second[4] + first[7] * second[5] + first[11] * second[6]
                        + first[15] * second[7];

        out[8] =
                first[0] * second[8]
                        + first[4] * second[9]
                        + first[8] * second[10]
                        + first[12] * second[11];
        out[9] =
                first[1] * second[8]
                        + first[5] * second[9]
                        + first[9] * second[10]
                        + first[13] * second[11];
        out[10] =
                first[2] * second[8]
                        + first[6] * second[9]
                        + first[10] * second[10]
                        + first[14] * second[11];
        out[11] =
                first[3] * second[8]
                        + first[7] * second[9]
                        + first[11] * second[10]
                        + first[15] * second[11];

        out[12] =
                first[0] * second[12]
                        + first[4] * second[13]
                        + first[8] * second[14]
                        + first[12] * second[15];
        out[13] =
                first[1] * second[12]
                        + first[5] * second[13]
                        + first[9] * second[14]
                        + first[13] * second[15];
        out[14] =
                first[2] * second[12]
                        + first[6] * second[13]
                        + first[10] * second[14]
                        + first[14] * second[15];
        out[15] =
                first[3] * second[12]
                        + first[7] * second[13]
                        + first[11] * second[14]
                        + first[15] * second[15];
    }

    private int getValue(MediaFormat format, String key, int defVal) {
        if (format.containsKey(key)) {
            return format.getInteger(key);
        }
        return defVal;
    }

    private void computeTransformMatrix(
            float[] outTransform, int width, int height, Rect cropRect, int transform) {
        float[] xform = new float[16];
        for (int i = 0; i < 16; i++) {
            xform[i] = mtxIdentity[i];
        }
        float[] result = new float[16];
        float[] vFlipMat = {1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1};
        if ((transform & FLIP_H) != 0) {
            float[] hFlipMat = {-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1};
            mtxMul(result, xform, hFlipMat);
            System.arraycopy(result, 0, xform, 0, 16);
        }
        if ((transform & FLIP_V) != 0) {
            mtxMul(result, xform, vFlipMat);
            System.arraycopy(result, 0, xform, 0, 16);
        }
        if ((transform & ROT_90) != 0) {
            float[] mtxRot90 = {0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1};
            mtxMul(result, xform, mtxRot90);
            System.arraycopy(result, 0, xform, 0, 16);
        }

        float[] mtxBeforeFlip = new float[16];
        if (!cropRect.isEmpty()) {
            float tx = 0.0f;
            float ty = 0.0f;
            float sx = 1.0f;
            float sy = 1.0f;
            float shrinkAmount = 1.0f;

            // Only shrink the dimensions that are not the size of the buffer.
            if (cropRect.width() < width) {
                tx = (cropRect.left + shrinkAmount) / width;
                sx = (cropRect.width() - (2.0f * shrinkAmount)) / width;
            }
            if (cropRect.height() < height) {
                ty = (height - cropRect.bottom + shrinkAmount) / height;
                sy = (cropRect.height() - (2.0f * shrinkAmount)) / height;
            }
            float[] crop = {sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, 1, 0, tx, ty, 0, 1};
            mtxMul(mtxBeforeFlip, crop, xform);
        } else {
            System.arraycopy(xform, 0, mtxBeforeFlip, 0, 16);
        }
        mtxMul(outTransform, vFlipMat, mtxBeforeFlip);
    }

    private float[] calculateMatrixByFormat(MediaFormat format, int rotate) {
        String keyCropBottom = "crop-bottom";
        String keyCropRight = "crop-right";
        int width = getValue(format, MediaFormat.KEY_WIDTH, -1);
        int height = getValue(format, MediaFormat.KEY_HEIGHT, -1);
        int cropRight = getValue(format, keyCropRight, -1);
        int cropBottom = getValue(format, keyCropBottom, -1);

        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) {
            matrix[i] = mtxIdentity[i];
        }
        Rect rect = new Rect();
        if (cropRight != -1 && cropBottom != -1) {
            String keyCropTop = "crop-top";
            String keyCropLeft = "crop-left";
            int cropTop = getValue(format, keyCropTop, 0);
            int cropLeft = getValue(format, keyCropLeft, 0);
            rect.set(cropLeft, cropTop, cropRight, cropBottom);
        } else {
            String keyStride = "stride";
            String keySliceHeight = "slice-height";
            int stride = getValue(format, keyStride, 0);
            int sliceHeight = getValue(format, keySliceHeight, height);
            rect.set(0, 0, stride, sliceHeight);
        }
        int transform = 0;
        if (rotate == 90) {
            transform = ROT_90;
        } else if (rotate == 180) {
            transform = FLIP_H | FLIP_V;
        } else if (rotate == 270) {
            transform = FLIP_H | FLIP_V | ROT_90;
        }
        computeTransformMatrix(matrix, width, height, rect, transform);
        return matrix;
    }

    public void modifyMatrixIfNeeded(float[] mat) {
        System.arraycopy(matrix, 0, mat, 0, 16);
    }
}
