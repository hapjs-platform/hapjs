/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;
import org.hapjs.bridge.MetaDataSet;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.Runtime;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConfigInfo {

    public static final int DEFAULT_DESIGN_WIDTH = 750;
    private static final String KEY_DESIGN_WIDTH = "designWidth";
    private static final String KEY_DEBUG = "debug";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_FEATURES = "features";
    private static final String KEY_NETWORK = "network";
    private static final String DEVICE_WIDTH = "device-width";
    private static final int CODE_DEVICE_WIDTH = -1;
    private static final String KEY_GRAY_MODE = "grayMode";

    private JSONObject mData;
    private int mDesignWidth = DEFAULT_DESIGN_WIDTH;
    private boolean mDebug = false;
    private Set<String> mBackgroundFeatures = new HashSet<>();
    private NetworkConfig mNetworkConfig;
    private GrayModeConfig mGrayModeConfig;
    private boolean mDynamicDesignWidth = false;

    public ConfigInfo(JSONObject data) {
        mData = data;
    }

    public static ConfigInfo parse(JSONObject configObject) {
        ConfigInfo configInfo = new ConfigInfo(configObject);
        if (configObject != null) {
            configInfo.mDesignWidth = parseDesignWidth(configObject.opt(KEY_DESIGN_WIDTH));
            if (configInfo.mDesignWidth == CODE_DEVICE_WIDTH) {
                configInfo.mDynamicDesignWidth = true;
            }
            configInfo.mDebug = configObject.optBoolean(KEY_DEBUG, false);
            JSONObject jsonBackground = configObject.optJSONObject(KEY_BACKGROUND);
            if (null != jsonBackground && jsonBackground.has(KEY_FEATURES)) {
                JSONArray jsonArray = jsonBackground.optJSONArray(KEY_FEATURES);
                if (null != jsonArray && jsonArray.length() > 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String featureName = jsonArray.optString(i);
                        if (!TextUtils.isEmpty(featureName)
                                && (MetaDataSet.getInstance().isInResidentNormalSet(featureName)
                                ||
                                MetaDataSet.getInstance().isInResidentImportantSet(featureName))) {
                            configInfo.mBackgroundFeatures.add(featureName);
                        }
                    }
                }
            }
            JSONObject networkObject = configObject.optJSONObject(KEY_NETWORK);
            configInfo.mNetworkConfig = NetworkConfig.parse(networkObject);
            JSONObject jsonGrayMode = configObject.optJSONObject(KEY_GRAY_MODE);
            configInfo.mGrayModeConfig = GrayModeConfig.fromJSONObject(jsonGrayMode);
        }
        return configInfo;
    }

    private static int parseDesignWidth(Object designWidth) {
        if (designWidth == null) {
            return DEFAULT_DESIGN_WIDTH;
        }
        String designWidthStr = designWidth.toString().trim();
        if (DEVICE_WIDTH.equals(designWidthStr)) {
            return CODE_DEVICE_WIDTH;
        }
        try {
            int designWidthInt = Math.round(Float.parseFloat(designWidthStr));
            if (designWidthInt > 0) {
                return designWidthInt;
            }
        } catch (NumberFormatException e) {
            Log.e("ConfigInfo", e.getMessage());
        }
        return DEFAULT_DESIGN_WIDTH;
    }

    public String getString(String key) {
        if (mData != null) {
            return mData.optString(key);
        }
        return null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (mData != null) {
            return mData.optBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public int getDesignWidth() {
        if (mDynamicDesignWidth) {
            Context context = Runtime.getInstance().getContext();
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenWidth = DisplayUtil.getScreenWidth(context);
            mDesignWidth = Math.round(screenWidth / displayMetrics.density);
        }
        return mDesignWidth;
    }

    public boolean isDebug() {
        return mDebug;
    }

    public Set<String> getBackgroundFeatures() {
        return mBackgroundFeatures;
    }

    public boolean isBackgroundFeature(String featureName) {
        return mBackgroundFeatures.contains(featureName);
    }

    public NetworkConfig getNetworkConfig() {
        return mNetworkConfig;
    }

    public GrayModeConfig getGrayModeConfig() {
        return mGrayModeConfig;
    }

    public JSONObject getData() {
        return mData;
    }

    public String getDslName() {
        JSONObject dsl = mData.optJSONObject("dsl");
        if (dsl != null) {
            return dsl.optString("name", "xvm");
        }
        return "xvm";
    }
}
