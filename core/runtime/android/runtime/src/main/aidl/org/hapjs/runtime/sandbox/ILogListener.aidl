/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.runtime.sandbox;

import org.hapjs.analyzer.model.LogData;

interface ILogListener {
    void onLog(in List<LogData> logs);
}