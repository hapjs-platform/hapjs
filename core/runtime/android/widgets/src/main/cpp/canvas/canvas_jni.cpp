/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>

#include "utils.h"

#define CANVAS_JNI_CLASS "org/hapjs/widgets/canvas/CanvasJNI"

void nativeComputeClipWhiteArea(JNIEnv *env, jclass clazz, jobject bitmap,
                                jobject bounds) {
    canvas::computeClipWhiteArea(env, bitmap, bounds);
}

JNINativeMethod gMethods[] = {
        {"computeClipWhiteArea",
                "(Landroid/graphics/Bitmap;Landroid/graphics/Rect;)V",
                (void *) nativeComputeClipWhiteArea},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    jclass clazz = env->FindClass(CANVAS_JNI_CLASS);
    if (clazz == nullptr) {
        return -1;
    }
    if (env->RegisterNatives(clazz, gMethods,
                             sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        return -1;
    }

    return JNI_VERSION_1_4;
}