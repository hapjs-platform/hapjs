/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef ANDROID_LOG_H
#define ANDROID_LOG_H

#include <android/log.h>

typedef enum {
    LOG_LEVEL_DEBUG = 0,
    LOG_LEVEL_INFO,
    LOG_LEVEL_WARN,
    LOG_LEVEL_ERROR,
    LOG_LEVEL_FATAL
} LogLevel;

int TransLogLevel(LogLevel logLevel);

void LogExt(LogLevel logLevel, const char *tag, const char *format, ...);

void SetLogLevel(LogLevel logLevel);

LogLevel GetLogLevel();

#define LOG_TAG_NAME "Canvas.Native"
#define LOG_D(...) LogExt(LOG_LEVEL_DEBUG, LOG_TAG_NAME, __VA_ARGS__)
#define LOG_I(...) LogExt(LOG_LEVEL_INFO, LOG_TAG_NAME, __VA_ARGS__)
#define LOG_W(...) LogExt(LOG_LEVEL_WARN, LOG_TAG_NAME, __VA_ARGS__)
#define LOG_E(...) LogExt(LOG_LEVEL_ERROR, LOG_TAG_NAME, __VA_ARGS__)
#define LOG_F(...) LogExt(LOG_LEVEL_FATAL, LOG_TAG_NAME, __VA_ARGS__)
#define LOG_EXT LogExt

#endif
