/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Keep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.ExtensionMetaData;
import org.hapjs.bridge.FeatureBridge;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Keep
public class CardInfo implements RoutableInfo {
    private static final String TAG = "CardInfo";

    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_COMPONENT = "component";
    private static final String KEY_PATH = "path";
    private static final String KEY_FEATURES = "features";
    private static final String KEY_MIN_PLATFORM_VERSION = "minPlatformVersion";
    private static final String KEY_ICON = "icon";
    private static final String KEY_TARGET_MANUFACTURERS = "targetManufacturers";
    private static final String KEY_TARGET_MANUFACTORYS_ALIAS = "targetManufactorys";
    private static final String KEY_DEPENDENCIES = "dependencies";
    private static final String KEY_QUICK_APPS = "quickApps";
    private static final String KEY_ANDROID_APPS = "androidApps";
    private static final String KEY_MIN_VERSION = "minVersion";
    private String mName;
    private String mTitle;
    private String mDescription;
    private String mPath;
    private String mUri;
    private String mComponent;
    private List<FeatureInfo> mFeatureInfos;
    private int mMinPlatformVersion;
    private String mIcon;
    private List<String> mTargetManufacturers;
    private Map<String, Map<String, AppDependency>> mDependencies;

    public static CardInfo parse(String name, JSONObject widgetJSON) {
        CardInfo cardInfo = new CardInfo();
        cardInfo.mName = name;
        cardInfo.mTitle = widgetJSON.optString(KEY_TITLE);
        cardInfo.mDescription = widgetJSON.optString(KEY_DESCRIPTION);
        cardInfo.mPath = widgetJSON.optString(KEY_PATH, "/" + name);
        String component;
        try {
            component = widgetJSON.getString(KEY_COMPONENT);
        } catch (JSONException e) {
            throw new IllegalStateException("Component can't be empty, name=" + name);
        }
        cardInfo.mComponent = component;
        cardInfo.mUri = name + "/" + component + ".js";
        JSONArray featureObjects = widgetJSON.optJSONArray(KEY_FEATURES);
        if (featureObjects != null) {
            cardInfo.mFeatureInfos = FeatureInfo.parse(featureObjects);
        }
        cardInfo.mMinPlatformVersion = widgetJSON.optInt(KEY_MIN_PLATFORM_VERSION, 1);
        cardInfo.mIcon = widgetJSON.optString(KEY_ICON);

        JSONArray targetManufacturerArray = widgetJSON.optJSONArray(KEY_TARGET_MANUFACTURERS);
        if (targetManufacturerArray == null) {
            targetManufacturerArray = widgetJSON.optJSONArray(KEY_TARGET_MANUFACTORYS_ALIAS);
        }
        if (targetManufacturerArray != null) {
            cardInfo.mTargetManufacturers = new ArrayList<>(targetManufacturerArray.length());
            for (int i = 0; i < targetManufacturerArray.length(); ++i) {
                String manufacturer = targetManufacturerArray.optString(i);
                if (!TextUtils.isEmpty(manufacturer)) {
                    cardInfo.mTargetManufacturers.add(manufacturer);
                }
            }
        }

        JSONObject dependenciesJson = widgetJSON.optJSONObject(KEY_DEPENDENCIES);
        if (dependenciesJson != null) {
            try {
                Map<String, Map<String, AppDependency>> dependencies = new HashMap<>();
                Iterator<String> platformItr = dependenciesJson.keys();
                while (platformItr.hasNext()) {
                    String platform = platformItr.next();
                    JSONObject appsJson = dependenciesJson.optJSONObject(platform);
                    if (appsJson != null) {
                        Map<String, AppDependency> apps = new HashMap<String, AppDependency>();
                        Iterator<String> appItr = appsJson.keys();
                        while (appItr.hasNext()) {
                            String pkg = appItr.next();
                            JSONObject appJson = appsJson.getJSONObject(pkg);
                            String minVersion = appJson.getString(KEY_MIN_VERSION);
                            AppDependency app =
                                    new AppDependency(pkg, Integer.parseInt(minVersion));
                            apps.put(pkg, app);
                        }
                        dependencies.put(platform, apps);
                    }
                }
                cardInfo.mDependencies = dependencies;
            } catch (JSONException e) {
                Log.e(TAG, "failed to parse dependencies", e);
            }
        }

        return cardInfo;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getPath() {
        return mPath;
    }

    @Override
    public String getUri() {
        return mUri;
    }

    @Override
    public String getComponent() {
        return mComponent;
    }

    @Override
    public String getLaunchMode() {
        return PageInfo.MODE_STANDARD;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public List<FeatureInfo> getFeatureInfos() {
        return mFeatureInfos;
    }

    public int getMinPlatformVersion() {
        return mMinPlatformVersion;
    }

    public List<String> getTargetManufacturers() {
        return mTargetManufacturers;
    }

    public String getIcon() {
        return mIcon;
    }

    public Collection<String> getPermissions() {
        if (mFeatureInfos == null) {
            return Collections.emptyList();
        }

        Set<String> permissions = new HashSet<>();
        for (FeatureInfo fi : mFeatureInfos) {
            ExtensionMetaData feature = FeatureBridge.getFeatureMap().get(fi.getName());
            if (feature != null) {
                for (String method : feature.getMethods()) {
                    String[] perms = feature.getPermissions(method);
                    if (perms != null) {
                        Collections.addAll(permissions, perms);
                    }
                }
            }
        }
        return permissions;
    }

    public boolean isFeatureAvailable(String feature) {
        if (mFeatureInfos != null) {
            for (FeatureInfo info : mFeatureInfos) {
                if (feature.equals(info.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, Map<String, AppDependency>> getDependencies() {
        return mDependencies;
    }

    public Map<String, AppDependency> getQuickAppDependencies() {
        if (mDependencies != null) {
            return mDependencies.get(KEY_QUICK_APPS);
        }
        return null;
    }

    public Map<String, AppDependency> getAndroidAppDependencies() {
        if (mDependencies != null) {
            return mDependencies.get(KEY_ANDROID_APPS);
        }
        return null;
    }
}
