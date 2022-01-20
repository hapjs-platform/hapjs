/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef ANDROID_UTILS_H
#define ANDROID_UTILS_H

#include "jni.h"

namespace canvas {
    void computeClipWhiteArea(JNIEnv *env, jobject bitmap, jobject bounds);
}

#endif  // ANDROID_UTILS_H
