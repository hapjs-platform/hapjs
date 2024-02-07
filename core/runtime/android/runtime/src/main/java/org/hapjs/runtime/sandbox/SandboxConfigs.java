/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import java.util.HashMap;
import java.util.Map;

public class SandboxConfigs {
    private static Map<String, String> sConfigs;
    private static final String KEY_DEBUG_LOG_ENABLED = "debugLogEnabled";
    private static final String KEY_PROFILER_ENABLED = "profilerEnabled";

    public static void setConfigs(Map<String, String> config) {
        sConfigs = config;
    }

    public static boolean isDebugLogEnabled() {
        Map<String, String> config = sConfigs;
        return config != null && config.get(KEY_DEBUG_LOG_ENABLED) != null
                && Boolean.valueOf(config.get(KEY_DEBUG_LOG_ENABLED));
    }

    public static boolean isProfilerEnabled() {
        Map<String, String> config = sConfigs;
        return config != null && config.get(KEY_PROFILER_ENABLED) != null
                && Boolean.valueOf(config.get(KEY_PROFILER_ENABLED));
    }

    public static class Builder {
        private Map<String, String> mConfigs = new HashMap<>();

        public Builder() {}

        public Builder setDebugLogEnabled(boolean enabled) {
            mConfigs.put(KEY_DEBUG_LOG_ENABLED, String.valueOf(enabled));
            return this;
        }

        public Builder setProfilerEnabled(boolean enabled) {
            mConfigs.put(KEY_PROFILER_ENABLED, String.valueOf(enabled));
            return this;
        }

        public Map<String, String> build() {
            return mConfigs;
        }
    }
}
