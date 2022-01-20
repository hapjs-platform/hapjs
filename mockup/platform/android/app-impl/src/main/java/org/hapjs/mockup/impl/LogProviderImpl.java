/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.impl;

import android.util.Log;
import java.util.Map;
import org.hapjs.logging.LogProvider;

public class LogProviderImpl implements LogProvider {
    private static final String TAG = "LogProviderImpl";

    @Override
    public void logCountEvent(String appPackage, String category, String key) {
        logCountEvent(appPackage, category, key, null);
    }

    @Override
    public void logCountEvent(
            String appPackage, String category, String key, Map<String, String> params) {
        Log.i(
                TAG,
                "recordCountEvent: appPackage="
                        + appPackage
                        + ", category="
                        + category
                        + ", key="
                        + key
                        + ", params="
                        + params);
    }

    @Override
    public void logCalculateEvent(String appPackage, String category, String key, long value) {
        logCalculateEvent(appPackage, category, key, value, null);
    }

    @Override
    public void logCalculateEvent(
            String appPackage, String category, String key, long value,
            Map<String, String> params) {
        Log.i(
                TAG,
                "recordCalculateEvent: appPackage="
                        + appPackage
                        + ", category="
                        + category
                        + ", key="
                        + key
                        + ", value="
                        + value
                        + ", params="
                        + params);
    }

    @Override
    public void logNumericPropertyEvent(String appPackage, String category, String key,
                                        long value) {
        logNumericPropertyEvent(appPackage, category, key, value, null);
    }

    @Override
    public void logNumericPropertyEvent(
            String appPackage, String category, String key, long value,
            Map<String, String> params) {
        Log.i(
                TAG,
                "recordNumericPropertyEvent: appPackage="
                        + appPackage
                        + ", category="
                        + category
                        + ", key="
                        + key
                        + ", value="
                        + value
                        + ", params="
                        + params);
    }

    @Override
    public void logStringPropertyEvent(String appPackage, String category, String key,
                                       String value) {
        logStringPropertyEvent(appPackage, category, key, value, null);
    }

    @Override
    public void logStringPropertyEvent(
            String appPackage, String category, String key, String value,
            Map<String, String> params) {
        Log.i(
                TAG,
                "recordStringPropertyEvent: appPackage="
                        + appPackage
                        + ", category="
                        + category
                        + ", key="
                        + key
                        + ", value="
                        + value
                        + ", params="
                        + params);
    }
}
