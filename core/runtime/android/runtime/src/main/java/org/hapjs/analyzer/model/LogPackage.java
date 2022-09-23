/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.model;

import android.util.Log;

import androidx.annotation.IntDef;

import java.util.List;

public class LogPackage {
    public int position;
    public List<LogData> datas;
    public static final int LOG_LEVEL_DEFAULT = 0;
    public static final int LOG_TYPE_DEFAULT = 0;
    public static final int LOG_TYPE_NATIVE = 1;
    public static final int LOG_TYPE_JS = 2;

    public LogPackage(List<LogData> datas) {
        this(-1, datas);
    }

    public LogPackage(int position, List<LogData> datas) {
        this.position = position;
        this.datas = datas;
    }

    public static class LogData {
        public @LogLevel int mLevel;
        public @LogType int mType;
        public String mContent;

        public LogData(@LogLevel int level, @LogType int type, String content) {
            mLevel = level;
            mType = type;
            mContent = content;
        }
    }

    @IntDef({LOG_LEVEL_DEFAULT, Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR})
    public @interface LogLevel {
    }

    @IntDef({LOG_TYPE_DEFAULT, LOG_TYPE_NATIVE, LOG_TYPE_JS})
    public @interface LogType {
    }
}
