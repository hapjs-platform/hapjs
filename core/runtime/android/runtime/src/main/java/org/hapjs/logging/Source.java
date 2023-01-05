/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.logging;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.hapjs.runtime.RuntimeActivity;
import org.json.JSONObject;

public class Source {

    // type values
    public static final String TYPE_SHORTCUT = "shortcut";
    public static final String TYPE_PUSH = "push";
    public static final String TYPE_URL = "url";
    public static final String TYPE_BAR_CODE = "barcode";
    public static final String TYPE_NFC = "nfc";
    public static final String TYPE_BLUETOOTH = "bluetooth";
    public static final String TYPE_OTHER = "other";
    public static final String TYPE_UNKNOWN = "unknown";
    // extra keys
    public static final String EXTRA_ORIGINAL = "original"; // 上级来源 source
    public static final String EXTRA_SCENE = "scene";
    // internal keys
    public static final String INTERNAL_SUB_SCENE = "subScene";
    public static final String INTERNAL_CHANNEL = "channel";
    public static final String INTERNAL_ENTRY = "entry"; // 入口来源 source
    // shortcut scene values
    public static final String SHORTCUT_SCENE_DIALOG = "dialog";
    public static final String SHORTCUT_SCENE_API = "api";
    public static final String SHORTCUT_SCENE_WEB = "web";
    public static final String SHORTCUT_SCENE_MENU = "menu";
    public static final String SHORTCUT_SCENE_SMART_PROGRAM = "smartProgram";
    public static final String SHORTCUT_SCENE_EASY_TRANSFER = "easyTransfer";
    public static final String SHORTCUT_SCENE_OTHER = "other";
    // shortcut dialog subscene values
    public static final String DIALOG_SCENE_EXIT_APP = "exitApp";
    public static final String DIALOG_SCENE_USE_TIMES = "useTimes";
    public static final String DIALOG_SCENE_USE_DURATION = "useDuration";
    // channel values
    public static final String CHANNEL_DEEPLINK = "deeplink";
    public static final String CHANNEL_INTENT = "intent";
    private static final String TAG = "Source";
    private static final String KEY_PACKAGE_NAME = "packageName";
    private static final String KEY_TYPE = "type";
    private static final String KEY_EXTRA = "extra";
    private static final String KEY_INTERNAL = "internal";
    private static final String KEY_HOST_PACKAGE_NAME = "hostPackageName";
    private String mPackageName = "";
    private String mType = "";
    private Map<String, String> mExtra = new HashMap<>();
    private Map<String, String> mInternal = new HashMap<>();
    private String mHostPackageName;
    private Source mEntry;

    public static Source currentSource() {
        return fromJson(currentSourceString());
    }

    public static String currentSourceString() {
        return System.getProperty(RuntimeActivity.PROP_SOURCE);
    }

    public static Source fromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            JSONObject jsonObj = new JSONObject(json);
            Source source = new Source();
            source.setPackageName(jsonObj.optString(KEY_PACKAGE_NAME));
            source.setType(jsonObj.optString(KEY_TYPE));
            source.setHostPackageName(jsonObj.optString(KEY_HOST_PACKAGE_NAME, null));

            JSONObject jsonExtra = jsonObj.optJSONObject(KEY_EXTRA);
            if (jsonExtra != null) {
                Iterator<String> iterator = jsonExtra.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    source.putExtra(key, jsonExtra.optString(key));
                }
            }

            JSONObject jsonInternal = jsonObj.optJSONObject(KEY_INTERNAL);
            if (jsonInternal != null) {
                Iterator<String> iterator = jsonInternal.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    source.putInternal(key, jsonInternal.optString(key));
                }
            }
            return source;
        } catch (Exception e) {
            Log.e(TAG, "Fail from Json to Source", e);
        }
        return null;
    }

    public static Source fromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String launchFrom = extras == null ? null : extras.getString(RuntimeActivity.EXTRA_SOURCE);
        if (TextUtils.isEmpty(launchFrom)) {
            Source source = new Source();
            source.setType(Source.TYPE_UNKNOWN);
            launchFrom = source.toJson().toString();
        }
        return fromJson(launchFrom);
    }

    public String getPackageName() {
        return mPackageName;
    }

    public Source setPackageName(String pkgName) {
        this.mPackageName = pkgName;
        return this;
    }

    public String getHostPackageName() {
        if (TextUtils.isEmpty(mHostPackageName)) {
            return mPackageName;
        }
        return mHostPackageName;
    }

    public Source setHostPackageName(String pkgName) {
        this.mHostPackageName = pkgName;
        return this;
    }

    public String getOriginalHostPackageName() {
        return mHostPackageName;
    }

    public String getType() {
        return mType;
    }

    public Source setType(String type) {
        this.mType = type;
        return this;
    }

    public Map<String, String> getExtra() {
        return mExtra;
    }

    public Map<String, String> getInternal() {
        return mInternal;
    }

    public Source putExtra(Map<String, String> extra) {
        if (extra != null) {
            this.mExtra.putAll(extra);
        }
        return this;
    }

    public Source putExtra(String key, String value) {
        mExtra.put(key, value);
        return this;
    }

    public Source putInternal(Map<String, String> internal) {
        if (internal != null) {
            this.mInternal.putAll(internal);
        }
        return this;
    }

    public Source putInternal(String key, String value) {
        mInternal.put(key, value);
        return this;
    }

    public Source getEntry() {
        if (mEntry == null) {
            // 先从当前 source 中获取是否存在 entry 入口来源
            mEntry = fromJson(mInternal.get(INTERNAL_ENTRY));
        }
        if (mEntry == null) {
            // 再检查是否存在 original（桌面）
            mEntry = fromJson(mExtra.get(EXTRA_ORIGINAL));
        }
        if (mEntry == null) {
            // 如果都不存在，当前 source 即为入口来源
            mEntry = this;
        }
        return mEntry;
    }

    public JSONObject toJson() {
        return toJsonObject(false);
    }

    public JSONObject toSafeJson() {
        return toJsonObject(true);
    }

    private JSONObject toJsonObject(boolean safe) {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_PACKAGE_NAME, mPackageName);
            json.put(KEY_TYPE, mType);

            JSONObject jsonExtra = new JSONObject();
            for (Map.Entry<String, String> entry : mExtra.entrySet()) {
                if (EXTRA_ORIGINAL.equals(entry.getKey())) {
                    Source original = Source.fromJson(entry.getValue());
                    if (original != null) {
                        JSONObject jsonOriginal = safe ? original.toSafeJson() : original.toJson();
                        jsonExtra.put(entry.getKey(), jsonOriginal);
                    }
                } else {
                    jsonExtra.put(entry.getKey(), entry.getValue());
                }
            }
            json.put(KEY_EXTRA, jsonExtra);

            if (!safe) {
                json.put(KEY_HOST_PACKAGE_NAME, mHostPackageName);
                JSONObject jsonInternal = new JSONObject();
                for (Map.Entry<String, String> entry : mInternal.entrySet()) {
                    if (INTERNAL_ENTRY.equals(entry.getKey())) {
                        Source entrySource = Source.fromJson(entry.getValue());
                        if (entrySource != null) {
                            jsonInternal.put(entry.getKey(), entrySource.toJson());
                        }
                    } else {
                        jsonInternal.put(entry.getKey(), entry.getValue());
                    }
                }
                json.put(KEY_INTERNAL, jsonInternal);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail from Source to Json", e);
        }
        return json;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
