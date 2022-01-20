/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "exceptions.h"

void canvas::throwIllegalException(JNIEnv *env, const char *msg) {
    jclass clazz = env->FindClass("java/lang/IllegalAccessException");
    if (clazz == nullptr) {
        return;
    }

    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
    env->ThrowNew(clazz, msg);
}