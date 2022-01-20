/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils;

import org.hapjs.card.api.CardConfig;
import org.hapjs.logging.Source;
import org.json.JSONObject;

public class CardConfigUtils {
    private static CardConfig sCardConfig;
    private static CardConfigChangeListener sCardConfigChangeListener;

    public static void registerCardConfigChangeListener(
            CardConfigChangeListener cardConfigChangeListener) {
        sCardConfigChangeListener = cardConfigChangeListener;
    }

    public static void unRegisterCardConfigChangeListener() {
        sCardConfigChangeListener = null;
    }

    public static void setCardConfig(CardConfig cardConfig) {
        sCardConfig = cardConfig;
        if (sCardConfigChangeListener != null) {
            sCardConfigChangeListener.onCardConfigChanged(cardConfig);
        }
    }

    public static Object get(String key) {
        if (sCardConfig == null) {
            return null;
        }

        return sCardConfig.get(key);
    }

    public static Object getTextSizeAdjustAuto() {
        return get(CardConfig.KEY_TEXT_SIZE_AUTO);
    }

    public static Source getHostSource() {
        Object value = get(CardConfig.KEY_HOST_SOURCE);
        if (value instanceof String) {
            return Source.fromJson((String) value);
        } else if (value instanceof JSONObject) {
            return Source.fromJson(value.toString());
        }
        return null;
    }

    public static boolean isCloseGlobalDefaultNightMode() {
        Object value = get(CardConfig.KEY_CLOSE_GLOBAL_DEFAULT_NIGHT_MODE);
        if (value instanceof Integer && (Integer) value == 1) {
            return true;
        }
        return false;
    }

    public interface CardConfigChangeListener {
        void onCardConfigChanged(CardConfig config);
    }
}
