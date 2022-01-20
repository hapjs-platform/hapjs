/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;

public class CardThemeUtils {

    public static final String KEY_THEME = "theme";
    private static final String TAG = "CardThemeUtils";
    private static Map<String, ?> sThemes = new ConcurrentHashMap<>();

    public static void initTheme(Context context) {
        sThemes.clear();
        String name = KEY_THEME + "_" + context.getApplicationInfo().packageName;
        Map<String, ?> all = context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
        if (!all.isEmpty()) {
            sThemes = all;
        } else {
            Map<String, Object> map = new ConcurrentHashMap<>();
            map.put("theme.titleTextColor", "#000000");
            map.put("theme.textColor", "#000000");
            map.put("theme.buttonTextColor", "#0971D4");
            map.put("theme.buttonClickTextColor", "#4D0971D4");
            map.put("theme.backgroundColor", "#00FFFFFF");
            map.put("theme.borderTopRadius", "12px");
            map.put("theme.borderBottomRadius", "12px");
            map.put("theme.activeColor", "#4D000000");
            sThemes = map;
        }
    }

    public static String getThemeValue(String key) {
        Object obj = sThemes.get(key);
        if (obj != null && obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    public static void setTheme(Context context, String theme) {
        if (TextUtils.isEmpty(theme)) {
            return;
        }
        try {
            String name = KEY_THEME + "_" + context.getApplicationInfo().packageName;
            SharedPreferences.Editor editor =
                    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
            Map<String, Object> map = new HashMap<>();
            JSONObject jsonObject = new JSONObject(theme);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.getString(key);
                editor.putString(key, value);
                map.put(key, value);
            }
            editor.apply();
            sThemes.clear();
            sThemes = map;
        } catch (JSONException e) {
            Log.e(TAG, "Fail to setTheme", e);
        }
    }
}
