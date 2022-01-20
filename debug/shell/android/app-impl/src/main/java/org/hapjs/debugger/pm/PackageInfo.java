/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.pm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;

public class PackageInfo {
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_NAME = "name";
    private static final String KEY_ICON = "icon";
    private static final String KEY_VERSION_NAME = "versionName";
    private static final String KEY_VERSION_CODE = "versionCode";
    private static final String KEY_MIN_PLATFORM_VERSION = "minPlatformVersion";
    private static final String KEY_ROUTER = "router";
    private static final String KEY_CARDS = "widgets";
    private static final String KEY_CARD_PATH = "path";
    private String mPackage;
    private String mName;
    private String mIconPath;
    private String mVersionName;
    private int mVersionCode;
    private int mMinPlatformVersion;
    private Map<String, String> mCardInfos;
    private byte[] mSignature;

    private PackageInfo() {
    }

    public static PackageInfo parse(JSONObject manifestJSON) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.mPackage = manifestJSON.optString(KEY_PACKAGE);
        packageInfo.mName = manifestJSON.optString(KEY_NAME);
        packageInfo.mIconPath = manifestJSON.optString(KEY_ICON);
        packageInfo.mVersionName = manifestJSON.optString(KEY_VERSION_NAME);
        packageInfo.mVersionCode = manifestJSON.optInt(KEY_VERSION_CODE);
        packageInfo.mMinPlatformVersion = manifestJSON.optInt(KEY_MIN_PLATFORM_VERSION, 1);
        packageInfo.mCardInfos = parseCardInfos(manifestJSON);
        return packageInfo;
    }

    private static Map<String, String> parseCardInfos(JSONObject manifestJSON) {
        JSONObject routerObject = manifestJSON.optJSONObject(KEY_ROUTER);
        if (routerObject == null) {
            return Collections.emptyMap();
        }
        JSONObject cardObject = routerObject.optJSONObject(KEY_CARDS);
        if (cardObject == null) {
            return Collections.emptyMap();
        }
        Iterator<String> keys = cardObject.keys();
        Map<String, String> cardInfos = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            String path = cardObject.optJSONObject(key).optString(KEY_CARD_PATH, "/" + key);
            cardInfos.put(key, path);
        }
        return cardInfos;
    }

    public String getPackage() {
        return mPackage;
    }

    public String getName() {
        return mName;
    }

    public String getIconPath() {
        return mIconPath;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public int getMinPlatformVersion() {
        return mMinPlatformVersion;
    }

    public Map<String, String> getCardInfos() {
        return mCardInfos;
    }

    public byte[] getSignature() {
        return mSignature;
    }

    public void setSignature(byte[] signature) {
        this.mSignature = signature;
    }
}
