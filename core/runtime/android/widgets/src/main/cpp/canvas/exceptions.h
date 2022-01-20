/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef ANDROID_EXCEPTIONS_H
#define ANDROID_EXCEPTIONS_H

#include <jni.h>

namespace canvas {
    void throwIllegalException(JNIEnv *env, const char *msg);
}

#endif  // ANDROID_EXCEPTIONS_H
