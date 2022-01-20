/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef INSPECTOR_LOG_H
#define INSPECTOR_LOG_H

#include <android/log.h>
#include <dlfcn.h>

#define PROP_VALUE_MAX 92
#define LOG_TAG "Inspector"
#define ALOG(TAG, LEVEL, ...)                                   \
  do {                                                          \
    __android_log_print(ANDROID_LOG_##LEVEL, TAG, __VA_ARGS__); \
  } while (0)
#define LOGD(...) ALOG(LOG_TAG, DEBUG, __VA_ARGS__)

#define ALOGI(TAG, ...) ALOG(TAG, INFO, __VA_ARGS__)
#define ALOGD(TAG, ...) ALOG(TAG, DEBUG, __VA_ARGS__)
#define ALOGE(TAG, ...) ALOG(TAG, ERROR, __VA_ARGS__)
#define ALOGV(TAG, ...) ALOG(TAG, VERBOSE, __VA_ARGS__)

#endif
