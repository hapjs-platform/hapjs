/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.common.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class CardConfigHelper {
    private static Map<String, String> sPlatform = new HashMap<>();

    public static String getPlatform(Context context) {
        String platform = sPlatform.get(context.getPackageName());
        if (platform == null) {
            platform = CardConfig.getPlatform(context);
            sPlatform.put(context.getPackageName(), platform);
        }
        return platform;
    }

    public static boolean isLoadFromLocal(Context context) {
        return TextUtils.equals(getPlatform(context), context.getPackageName());
    }

    private static class CardConfig {
        private static final String TAG = "CardConfig";

        private static final String CARD_CONFIG_NAME = "hap/card.json";
        private static final String KEY_PLATFORM = "platform";

        private static String getPlatform(Context context) {
            String result = null;
            String cardConfig = getCardConfig(context);
            if (cardConfig != null) {
                try {
                    result = new JSONObject(cardConfig).optString(KEY_PLATFORM, null);
                } catch (JSONException e) {
                    Log.e(TAG, "Fail to get platform", e);
                }
            }
            return TextUtils.isEmpty(result) ? context.getPackageName() : result;
        }

        private static String getCardConfig(Context context) {
            InputStream in = null;
            try {
                in = context.getResources().getAssets().open(CARD_CONFIG_NAME);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                while (true) {
                    int byteCount = in.read(buffer);
                    if (byteCount == -1) {
                        break;
                    }
                    stream.write(buffer, 0, byteCount);
                }
                return new String(stream.toByteArray(), "UTF-8");
            } catch (Exception e) {
                Log.e(TAG, "Fail to get card config", e);
                return null;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore this exception
                    }
                }
            }
        }
    }
}
