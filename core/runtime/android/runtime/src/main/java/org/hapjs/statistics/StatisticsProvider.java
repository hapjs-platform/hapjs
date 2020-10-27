/*
 * Copyright (C) 2017, hapjs.org. All rights reserved.
 */

package org.hapjs.statistics;

import java.util.Map;

public interface StatisticsProvider {
    String NAME = "statistics";
    String KEY_RPK_VERSION = "rpk_version";
    String KEY_RPK_PACKAGE = "rpk_package";
    void recordCountEvent(String appPackage, String category, String key);
    void recordCountEvent(String appPackage, String category, String key, Map<String, String> params);

    void recordCalculateEvent(String appPackage, String category, String key, long value);
    void recordCalculateEvent(String appPackage, String category, String key, long value, Map<String, String> params);

    void recordNumericPropertyEvent(String appPackage, String category, String key, long value);
    void recordNumericPropertyEvent(String appPackage, String category, String key, long value, Map<String, String> params);

    void recordStringPropertyEvent(String appPackage, String category, String key, String value);
    void recordStringPropertyEvent(String appPackage, String category, String key, String value, Map<String, String> params);
}
