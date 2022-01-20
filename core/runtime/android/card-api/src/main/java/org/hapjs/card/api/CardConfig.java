/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

import java.util.HashMap;
import java.util.Map;

public class CardConfig {
    public static final String KEY_TEXT_SIZE_AUTO = "textSizeAuto";
    public static final String KEY_HOST_SOURCE = "hostSource";
    public static final String KEY_DARK_MODE = "darkMode";
    public static final String KEY_CLOSE_GLOBAL_DEFAULT_NIGHT_MODE = "closeGlobalDefaultNightMode";

    private Map<String, Object> mConfigs;

    public void set(String key, Object value) {
        if (mConfigs == null) {
            mConfigs = new HashMap<>();
        }

        mConfigs.put(key, value);
    }

    public Object get(String key) {
        if (mConfigs == null) {
            return null;
        }

        return mConfigs.get(key);
    }
}
