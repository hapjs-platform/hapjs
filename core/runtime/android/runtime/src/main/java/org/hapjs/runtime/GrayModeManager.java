/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.hapjs.cache.CacheStorage;
import org.hapjs.model.AppInfo;
import org.hapjs.model.GrayModeConfig;
import org.hapjs.model.GrayModeInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GrayModeManager {

    private static final String TAG = "GrayModeManager";

    private static final String GRAY_MODE_SHARED_PREFS_NAME = "grayMode_prefs";
    private static final String KEY_GRAY_MODE_INFOS = "gray_mode_infos";

    private Map<String, GrayModeInfo> mGrayModeInfoMap;

    private String mCurrentPkg;
    private ColorMatrix mGrayModeCm;
    private Paint mGrayModePaint;

    private volatile boolean mInitialized = false;
    private volatile boolean mNeedRecreate = false;

    private GrayModeManager() {
    }

    private static class ManagerHolder {
        private static GrayModeManager INSTANCE = new GrayModeManager();
    }

    public static GrayModeManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void setCurrentPkg(String pkg) {
        mCurrentPkg = pkg;
    }

    public synchronized void init(Context context) {
        if (!mInitialized) {
            mGrayModeInfoMap = loadInfosFromSharedPrefs(context);
            initGrayModePaint();
            mInitialized = true;
        }
    }

    private void initGrayModePaint() {
        if (mGrayModePaint == null) {
            mGrayModePaint = new Paint();
            mGrayModeCm = new ColorMatrix();
            mGrayModeCm.setSaturation(0);
            mGrayModePaint.setColorFilter(new ColorMatrixColorFilter(mGrayModeCm));
        }
    }

    private Map<String, GrayModeInfo> loadInfosFromSharedPrefs(Context context) {
        Map<String, GrayModeInfo> grayModeInfos = new HashMap<>();
        try {
            SharedPreferences sp = context.getSharedPreferences(GRAY_MODE_SHARED_PREFS_NAME, Context.MODE_MULTI_PROCESS);
            String grayModeInfosStr = sp.getString(KEY_GRAY_MODE_INFOS, "");
            if (!TextUtils.isEmpty(grayModeInfosStr)) {
                JSONArray grayModeInfosJsonArray = new JSONArray(grayModeInfosStr);
                for (int i = 0; i < grayModeInfosJsonArray.length(); i++) {
                    JSONObject grayModeInfoJsonObject = grayModeInfosJsonArray.getJSONObject(i);
                    GrayModeInfo grayModeInfo = GrayModeInfo.fromJSONObject(grayModeInfoJsonObject);
                    grayModeInfos.put(grayModeInfo.getPkgName(), grayModeInfo);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "loadConfigsFromSharedPref: JSONException.", e);
        }
        return grayModeInfos;
    }

    private void saveInfosToSharedPrefs(Context context) {
        JSONArray configsJsonArray = new JSONArray();
        if (mGrayModeInfoMap != null && !mGrayModeInfoMap.isEmpty()) {
            for (GrayModeInfo grayModeConfig : mGrayModeInfoMap.values()) {
                configsJsonArray.put(grayModeConfig.toJsonObject());
            }
        }
        SharedPreferences sp = context.getSharedPreferences(GRAY_MODE_SHARED_PREFS_NAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_GRAY_MODE_INFOS, configsJsonArray.toString());
        editor.apply();
    }

    public void recordGrayModeConfig(Context context, JSONObject configJson, int versionCode) {
        if (TextUtils.isEmpty(mCurrentPkg)) {
            return;
        }
        boolean isInGrayMode = shouldApplyGrayMode();
        GrayModeInfo oldInfo = mGrayModeInfoMap.get(mCurrentPkg);
        GrayModeInfo newInfo = GrayModeInfo.fromJSONObject(configJson, mCurrentPkg, versionCode);
        if (oldInfo == null) {
            mGrayModeInfoMap.put(mCurrentPkg, newInfo);
        } else {
            if (oldInfo.equals(newInfo)) {
                return;
            }
            mGrayModeInfoMap.put(mCurrentPkg, newInfo);
        }
        if (shouldApplyGrayMode() != isInGrayMode) {
            mNeedRecreate = true;
        }
        saveInfosToSharedPrefs(context);
    }

    public boolean isNeedRecreate() {
        return mNeedRecreate;
    }

    public void setNeedRecreate(boolean needRecreate) {
        mNeedRecreate = needRecreate;
    }

    private boolean isBelongGrayModeDuration() {
        if (TextUtils.isEmpty(mCurrentPkg)) {
            return false;
        }
        GrayModeInfo info = mGrayModeInfoMap.get(mCurrentPkg);
        // Feature和manifest中配置的灰色模式信息生效的优先级为：
        // 1、同一版本Feature中的动态配置信息优先于manifest中的静态配置信息
        // 2、版本号高的manifest中的静态信息优先于低版本中缓存在本地的Feature中动态配置的信息
        if (info != null && info.getVersionCode() >= getVersionCodeInManifest()) {
            return info.isBelongGrayModeDuration();
        } else {
            return isBelongGrayModeDurationInManifest();
        }
    }

    private boolean isBelongGrayModeDurationInManifest() {
        GrayModeConfig grayModeConfig = getGrayModeConfigInManifest();
        if (grayModeConfig != null) {
            return grayModeConfig.isBelongGrayModeDuration();
        }
        return false;
    }

    private boolean isEnableGrayMode() {
        if (TextUtils.isEmpty(mCurrentPkg)) {
            return false;
        }
        GrayModeInfo info = mGrayModeInfoMap.get(mCurrentPkg);
        if (info != null && info.getVersionCode() >= getVersionCodeInManifest()) {
            return info.isEnable();
        } else {
            return isEnableGrayModeInManifest();
        }
    }

    private boolean isEnableGrayModeInManifest() {
        GrayModeConfig grayModeConfig = getGrayModeConfigInManifest();
        if (grayModeConfig != null) {
            return grayModeConfig.isEnable();
        }
        return false;
    }

    public boolean isPageExclude(String pagePath) {
        if (TextUtils.isEmpty(mCurrentPkg)) {
            return false;
        }
        GrayModeInfo info = mGrayModeInfoMap.get(mCurrentPkg);
        if (info != null && info.getVersionCode() >= getVersionCodeInManifest()) {
            return info.isPagePathExcluded(pagePath);
        } else {
            return isPageExcludeInManifest(pagePath);
        }
    }

    private boolean isPageExcludeInManifest(String pagePath) {
        GrayModeConfig grayModeConfig = getGrayModeConfigInManifest();
        if (grayModeConfig != null) {
            return grayModeConfig.isPagePathExcluded(pagePath);
        }
        return false;
    }

    private GrayModeConfig getGrayModeConfigInManifest() {
        if (TextUtils.isEmpty(mCurrentPkg)) {
            return null;
        }
        if (CacheStorage.getInstance(Runtime.getInstance().getContext()).hasCache(mCurrentPkg)) {
            AppInfo appInfo = CacheStorage.getInstance(Runtime.getInstance().getContext()).getCache(mCurrentPkg).getAppInfo();
            if (appInfo != null && appInfo.getConfigInfo() != null) {
                return appInfo.getConfigInfo().getGrayModeConfig();
            }
        } else {
            Log.d(TAG, "packages " + mCurrentPkg + " no cache");
        }
        return null;
    }

    private int getVersionCodeInManifest() {
        if (TextUtils.isEmpty(mCurrentPkg)) {
            return -1;
        }
        if (CacheStorage.getInstance(Runtime.getInstance().getContext()).hasCache(mCurrentPkg)) {
            AppInfo appInfo = CacheStorage.getInstance(Runtime.getInstance().getContext()).getCache(mCurrentPkg).getAppInfo();
            if (appInfo != null) {
                return appInfo.getVersionCode();
            }
        } else {
            Log.d(TAG, "packages " + mCurrentPkg + " no cache");
        }
        return -1;
    }

    public boolean shouldApplyGrayMode() {
        boolean enableGrayMode = isEnableGrayMode();
        boolean belongGrayModeDate = isBelongGrayModeDuration();
        return enableGrayMode && belongGrayModeDate;
    }

    public boolean applyGrayMode(View view, boolean shouldApply) {
        if (mGrayModePaint == null || view == null) {
            Log.e(TAG, "apply view in gray mode fail.");
            return false;
        }
        if (shouldApply) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, mGrayModePaint);
            return true;
        } else {
            view.setLayerType(View.LAYER_TYPE_NONE, null);
            return false;
        }
    }

    public void applyGrayMode(Drawable drawable) {
        if (mGrayModePaint == null || mGrayModeCm == null) {
            Log.e(TAG, "apply drawable in gray mode fail.");
            return;
        }
        drawable.setColorFilter(new ColorMatrixColorFilter(mGrayModeCm));
    }
}
