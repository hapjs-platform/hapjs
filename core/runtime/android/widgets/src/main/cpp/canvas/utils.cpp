/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "utils.h"

#include <random>

#include "Log.h"
#include "android/bitmap.h"
#include "exceptions.h"

bool inline isClipColor(int alpha, int red, int green, int blue) {
    return alpha == 0 || (red == 255 && green == 255 && blue == 255);
}

void writeBounds(JNIEnv *env, jobject bounds, int left, int top, int right,
                 int bottom) {
    jclass rectClass = env->FindClass("android/graphics/Rect");
    if (rectClass == nullptr) {
        return;
    }

    if (!env->IsInstanceOf(bounds, rectClass)) {
        canvas::throwIllegalException(env, "bounds is not instance of Rect!");
        return;
    }

    jfieldID leftHandle = env->GetFieldID(rectClass, "left", "I");
    jfieldID topHandle = env->GetFieldID(rectClass, "top", "I");
    jfieldID righttHandle = env->GetFieldID(rectClass, "right", "I");
    jfieldID bottomHandle = env->GetFieldID(rectClass, "bottom", "I");

    env->SetIntField(bounds, leftHandle, left);
    env->SetIntField(bounds, topHandle, top);
    env->SetIntField(bounds, righttHandle, right);
    env->SetIntField(bounds, bottomHandle, bottom);
}

void canvas::computeClipWhiteArea(JNIEnv *env, jobject bitmap, jobject bounds) {
    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) !=
        ANDROID_BITMAP_RESUT_SUCCESS) {
        LOG_D("get bitmap info fail!");
        return;
    }

    // only support argb_8888 format
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOG_D("the bitmap format is not RGBA_8888");
        return;
    }

    int width = bitmapInfo.width;
    int height = bitmapInfo.height;

    if (width <= 0 || height <= 0) {
        LOG_D("bitmap's width or height is zero!");
        return;
    }

    int *bitmapData = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, (void **) &bitmapData) !=
        ANDROID_BITMAP_RESULT_SUCCESS) {
        writeBounds(env, bounds, 0, 0, width, height);
        return;
    }

    int left = -1, top = -1, right = -1, bottom = -1;
    int red, green, blue, alpha;
    int color;

    // calculate left edge
    for (int col = 0; col < width; ++col) {
        left = col;
        for (int row = 0; row < height; ++row) {
            color = bitmapData[row * width + col];
            alpha = color & 0xff000000;
            red = (color & 0x00ff0000) >> 16;
            green = (color & 0x0000ff00) >> 8;
            blue = color & 0x000000ff;
            if (!isClipColor(alpha, red, green, blue)) {
                goto leftEnd;
            }
        }
    }
    leftEnd:
    if (left == (width - 1)) {
        writeBounds(env, bounds, 0, 0, width, height);
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    }

    // calculate right edge
    for (int col = width - 1; col >= left; col--) {
        right = col;
        for (int row = 0; row < height; ++row) {
            color = bitmapData[row * width + col];
            alpha = color & 0xff000000;
            red = (color & 0x00ff0000) >> 16;
            green = (color & 0x0000ff00) >> 8;
            blue = color & 0x000000ff;
            if (!isClipColor(alpha, red, green, blue)) {
                goto rightEnd;
            }
        }
    }
    rightEnd:

    // calculate top edge
    for (int row = 0; row < height; ++row) {
        top = row;
        for (int col = left; col < right; col++) {
            color = bitmapData[row * width + col];
            alpha = color & 0xff000000;
            red = (color & 0x00ff0000) >> 16;
            green = (color & 0x0000ff00) >> 8;
            blue = color & 0x000000ff;
            if (!isClipColor(alpha, red, green, blue)) {
                goto topEnd;
            }
        }
    }
    topEnd:
    if (top == height - 1) {
        writeBounds(env, bounds, 0, 0, width, height);
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    }

    // calculate bottom edge
    for (int row = height - 1; row > top; row--) {
        bottom = row;
        for (int col = left; col < right; col++) {
            color = bitmapData[row * width + col];
            alpha = color & 0xff000000;
            red = (color & 0x00ff0000) >> 16;
            green = (color & 0x0000ff00) >> 8;
            blue = color & 0x000000ff;
            if (!isClipColor(alpha, red, green, blue)) {
                goto bottomEnd;
            }
        }
    }
    bottomEnd:

    writeBounds(env, bounds, left, top, right, bottom);
    AndroidBitmap_unlockPixels(env, bitmap);
}
