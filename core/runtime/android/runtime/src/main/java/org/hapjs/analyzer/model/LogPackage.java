/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.model;

import java.util.List;

public class LogPackage {
    public int position;
    public List<LogData> datas;

    public LogPackage(List<LogData> datas) {
        this(-1, datas);
    }

    public LogPackage(int position, List<LogData> datas) {
        this.position = position;
        this.datas = datas;
    }

    public static class LogData {
        public int mLevel;
        public boolean mIsJsLog;
        public String mContent;

        public LogData(int level, boolean jsLog, String content) {
            mLevel = level;
            mIsJsLog = jsLog;
            mContent = content;
        }
    }
}
