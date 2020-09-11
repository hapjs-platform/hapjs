/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class GrayModeInfo {
    private static final String TAG = "GrayModeInfo";

    private static final String KEY_GRAY_MODE_PACKAGE = "package";
    private static final String KEY_GRAY_MODE_CONFIG = "config";
    private static final String KEY_GRAY_MODE_VERSION_CODE = "versionCode";

    private String mPkgName;
    private int mVersionCode;
    private GrayModeConfig mGrayModeConfig;

    private GrayModeInfo() {
    }

    public static GrayModeInfo fromJSONObject(JSONObject infoJsonObject) {
        if (infoJsonObject == null) {
            return null;
        }
        GrayModeInfo grayModeInfo = new GrayModeInfo();
        grayModeInfo.mPkgName = infoJsonObject.optString(KEY_GRAY_MODE_PACKAGE);
        grayModeInfo.mVersionCode = infoJsonObject.optInt(KEY_GRAY_MODE_VERSION_CODE, -1);
        grayModeInfo.mGrayModeConfig = GrayModeConfig.fromJSONObject(infoJsonObject.optJSONObject(KEY_GRAY_MODE_CONFIG));
        return grayModeInfo;
    }

    public static GrayModeInfo fromJSONObject(JSONObject configJsonObject, String pkgName, int versionCode) {
        if (configJsonObject == null) {
            return null;
        }
        GrayModeInfo grayModeInfo = new GrayModeInfo();
        grayModeInfo.mPkgName = pkgName;
        grayModeInfo.mVersionCode = versionCode;
        grayModeInfo.mGrayModeConfig = GrayModeConfig.fromJSONObject(configJsonObject);
        return grayModeInfo;
    }

    public JSONObject toJsonObject() {
        try {
            JSONObject grayModeInfoObj = new JSONObject();
            grayModeInfoObj.put(KEY_GRAY_MODE_PACKAGE, mPkgName);
            grayModeInfoObj.put(KEY_GRAY_MODE_VERSION_CODE, mVersionCode);
            grayModeInfoObj.put(KEY_GRAY_MODE_CONFIG, mGrayModeConfig.toJsonObject());
            return grayModeInfoObj;
        } catch (JSONException e) {
            Log.e(TAG, "toJsonObject: JSONException.", e);
            return new JSONObject();
        }
    }

    public void setPkgName(String pkgName) {
        this.mPkgName = pkgName;
    }

    public String getPkgName() {
        return this.mPkgName;
    }

    public void setVersionCode(int versionCode) {
        this.mVersionCode = versionCode;
    }

    public int getVersionCode() {
        return this.mVersionCode;
    }

    public GrayModeConfig getGrayModeConfig() {
        return this.mGrayModeConfig;
    }

    public boolean isEnable() {
        if (mGrayModeConfig != null) {
            return mGrayModeConfig.isEnable();
        }
        return false;
    }

    public boolean isBelongGrayModeDuration() {
        if (mGrayModeConfig != null) {
            return mGrayModeConfig.isBelongGrayModeDuration();
        }
        return false;
    }

    public boolean isPagePathExcluded(String pathPath) {
        if (mGrayModeConfig != null) {
            return mGrayModeConfig.isPagePathExcluded(pathPath);
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GrayModeInfo other = (GrayModeInfo) obj;
        return TextUtils.equals(this.mPkgName, other.mPkgName)
                && this.mVersionCode == other.mVersionCode
                && this.mGrayModeConfig.equals(other.mGrayModeConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPkgName, mVersionCode, mGrayModeConfig);
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }
}
