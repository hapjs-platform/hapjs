/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer;

import java.util.Map;

public interface AnalyzerStatisticsProvider {
    String NAME = "analyzer_statistics";

    void report(String pkgName, String event, Map<String, String> params);
}
