/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "Log.h"

#include <cstdio>

LogLevel g_log_level = LOG_LEVEL_DEBUG;

LogLevel GetLogLevel() { return g_log_level; };

void SetLogLevel(LogLevel logLevel) {
    if (g_log_level == logLevel) return;
#ifdef DEBUG
    LogLevel oldLevel = g_log_level;
    LOG_I("[SetLogLevel] %d=>%d", oldLevel, logLevel);
#endif
    g_log_level = logLevel;
};

int TransLogLevel(LogLevel logLevel) {
    if (LOG_LEVEL_DEBUG == logLevel) return ANDROID_LOG_DEBUG;
    if (LOG_LEVEL_INFO == logLevel) return ANDROID_LOG_INFO;
    if (LOG_LEVEL_WARN == logLevel) return ANDROID_LOG_WARN;
    if (LOG_LEVEL_ERROR == logLevel) return ANDROID_LOG_ERROR;
    if (LOG_LEVEL_FATAL == logLevel) return ANDROID_LOG_FATAL;
    return ANDROID_LOG_DEFAULT;
}

void LogExt(LogLevel logLevel, const char *tag, const char *format, ...) {
    if (g_log_level > logLevel) return;

    va_list va;
    char buffer[1024];

    va_start(va, format);
    vsnprintf(buffer, 1024, format, va);
    va_end(va);
    __android_log_write(TransLogLevel(logLevel), tag, buffer);
}
