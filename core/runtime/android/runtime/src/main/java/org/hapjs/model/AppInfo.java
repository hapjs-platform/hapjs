/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hapjs.bridge.FitWidescreenProvider;
import org.hapjs.cache.Cache;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.LocaleResourcesParser;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.resource.ResourceManagerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AppInfo {
    protected static final String KEY_PACKAGE_INFO = "packageInfo";
    private static final String TAG = "AppInfo";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_NAME = "name";
    private static final String KEY_VERSION_NAME = "versionName";
    private static final String KEY_VERSION_CODE = "versionCode";
    private static final String KEY_MIN_PLATFORM_VERSION = "minPlatformVersion";
    private static final String KEY_ICON = "icon";
    private static final String KEY_FEATURES = "features";
    private static final String KEY_COMPONENTS = "components";
    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_ROUTER = "router";
    private static final String KEY_DISPLAY = "display";
    private static final String KEY_SUBPACKAGES = "subpackages";
    private static final String KEY_TRUSTED_SSL_DOMAINS = "trustedSslDomains";
    protected PackageInfo mPackageInfo;
    private JSONObject mRawData;
    private String mPackage;
    private String mName;
    private String mVersionName;
    private int mVersionCode;
    private int mMinPlatformVersion;
    private String mIcon;
    private String mSignature;
    private long mTimestamp;
    private List<FeatureInfo> mFeatureInfos;
    private List<ComponentInfo> mComponentInfos;
    private List<PermissionInfo> mPermissionInfos;
    private ConfigInfo mConfigInfo;
    private RouterInfo mRouterInfo;
    private DisplayInfo mDisplayInfo;
    private List<SubpackageInfo> mSubpackageInfos;
    private List<String> mTrustedSslDomains;
    private Map<String, String> mLocaledName = new HashMap<>();

    public static AppInfo fromFile(File manifest) {
        try {
            String manifestContent = FileUtils.readFileAsString(manifest.getPath());
            JSONObject manifestObject = new JSONObject(manifestContent);
            return parse(manifestObject);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "app info parse File fail. file path: " + manifest.getPath(), e);
        }
        return null;
    }

    public static AppInfo create(Context context, String pkg) {
        Uri uri =
                ResourceManagerFactory.getResourceManager(context, pkg)
                        .getResource(PackageUtils.FILENAME_MANIFEST);
        if (BuildPlatform.isTV()) {
            File file =
                    new File(Cache.getResourceDir(context, pkg), PackageUtils.FILENAME_MANIFEST_TV);
            boolean exists = file.exists();
            if (exists) {
                uri =
                        ResourceManagerFactory.getResourceManager(context, pkg)
                                .getResource(PackageUtils.FILENAME_MANIFEST_TV);
            }
        } else if (BuildPlatform.isPhone()) {
            File file =
                    new File(Cache.getResourceDir(context, pkg),
                            PackageUtils.FILENAME_MANIFEST_PHONE);
            boolean exists = file.exists();
            if (exists) {
                uri =
                        ResourceManagerFactory.getResourceManager(context, pkg)
                                .getResource(PackageUtils.FILENAME_MANIFEST_PHONE);
            }
        }
        return fromUri(context, uri);
    }

    private static AppInfo fromUri(Context context, Uri manifestUri) {
        if (manifestUri != null) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(manifestUri);
                String manifestContent = FileUtils.readStreamAsString(inputStream, true);
                JSONObject manifestObject = new JSONObject(manifestContent);
                return parse(manifestObject);
            } catch (IOException | JSONException e) {
                Log.w(TAG, "app info parse uri fail. uri: " + manifestUri.toString(), e);
            }
        }
        return null;
    }

    public static AppInfo parse(JSONObject manifestObject) {
        AppInfo appInfo = new AppInfo();
        appInfo.mRawData = manifestObject;
        appInfo.mPackage = manifestObject.optString(KEY_PACKAGE);
        appInfo.mName = manifestObject.optString(KEY_NAME, appInfo.mPackage);
        appInfo.mVersionName = manifestObject.optString(KEY_VERSION_NAME);
        appInfo.mVersionCode = manifestObject.optInt(KEY_VERSION_CODE);
        appInfo.mMinPlatformVersion = manifestObject.optInt(KEY_MIN_PLATFORM_VERSION, 1);
        appInfo.mIcon = manifestObject.optString(KEY_ICON);
        if (!appInfo.mIcon.startsWith("/")) {
            appInfo.mIcon = "/" + appInfo.mIcon;
        }
        JSONArray featureObjects = manifestObject.optJSONArray(KEY_FEATURES);
        appInfo.mFeatureInfos = FeatureInfo.parse(featureObjects);
        JSONArray componentObjects = manifestObject.optJSONArray(KEY_COMPONENTS);
        appInfo.mComponentInfos = ComponentInfo.parse(componentObjects);
        JSONArray permissionObjects = manifestObject.optJSONArray(KEY_PERMISSIONS);
        appInfo.mPermissionInfos = PermissionInfo.parse(permissionObjects);
        JSONObject configObject = manifestObject.optJSONObject(KEY_CONFIG);
        appInfo.mConfigInfo = ConfigInfo.parse(configObject);
        JSONObject routerObject = manifestObject.optJSONObject(KEY_ROUTER);
        appInfo.mRouterInfo = RouterInfo.parse(routerObject);
        JSONObject displayObject = manifestObject.optJSONObject(KEY_DISPLAY);
        if (displayObject != null) {
            appInfo.mDisplayInfo = DisplayInfo.parse(displayObject);
            String mode = appInfo.mDisplayInfo.getFitMode();
            FitWidescreenProvider provider = ProviderManager.getDefault().getProvider(FitWidescreenProvider.NAME);
            String fitMode = provider.getFitMode(appInfo.getPackage(), mode);
            appInfo.mDisplayInfo.setFitMode(fitMode);
        }
        JSONArray subpackageObject = manifestObject.optJSONArray(KEY_SUBPACKAGES);
        appInfo.mSubpackageInfos =
                SubpackageInfo.parseInfosFromManifest(
                        appInfo.mPackage,
                        subpackageObject,
                        appInfo.mRouterInfo.getPageInfos(),
                        appInfo.mRouterInfo.getEntry());

        // trusted ssl domains
        JSONArray domainsArray = manifestObject.optJSONArray(KEY_TRUSTED_SSL_DOMAINS);
        if (domainsArray != null) {
            List<String> domainsList = new ArrayList<>();
            for (int i = 0; i < domainsArray.length(); i++) {
                String domain = domainsArray.optString(i);
                if (!TextUtils.isEmpty(domain)) {
                    domainsList.add(domain);
                }
            }
            appInfo.mTrustedSslDomains = domainsList;
        }
        JSONObject packageObject = manifestObject.optJSONObject(KEY_PACKAGE_INFO);
        if (packageObject != null) {
            appInfo.mPackageInfo = PackageInfo.parse(packageObject);
        }
        return appInfo;
    }

    public JSONObject getRawData() {
        return mRawData;
    }

    public String getPackage() {
        return mPackage;
    }

    public String getOriginalName() {
        return mName;
    }

    public String getName() {
        if (mName.contains("$")) {
            Locale locale = ConfigurationManager.getInstance().getCurrentLocale();
            String language = locale.getLanguage();
            String countryOrRegion = locale.getCountry();
            String fullLocale = language + "-" + countryOrRegion;
            if (TextUtils.isEmpty(language)) {
                return mName;
            }
            String name = mLocaledName.get(fullLocale);
            if (TextUtils.isEmpty(name)) {
                name = LocaleResourcesParser.getInstance().getText(mPackage, locale, mName);
                mLocaledName.put(fullLocale, name);
            }
            return name;
        }
        return mName;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public PackageInfo getPackageInfo() {
        return mPackageInfo;
    }

    public int getMinPlatformVersion() {
        return mMinPlatformVersion;
    }

    public String getIcon() {
        return mIcon;
    }

    public String getSignature() {
        return mSignature;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public List<FeatureInfo> getFeatureInfos() {
        return mFeatureInfos;
    }

    public List<ComponentInfo> getComponentInfos() {
        return mComponentInfos;
    }

    public List<PermissionInfo> getPermissionInfos() {
        return mPermissionInfos;
    }

    public ConfigInfo getConfigInfo() {
        return mConfigInfo;
    }

    public RouterInfo getRouterInfo() {
        return mRouterInfo;
    }

    public DisplayInfo getDisplayInfo() {
        return mDisplayInfo;
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

    public List<SubpackageInfo> getSubpackageInfos() {
        return mSubpackageInfos;
    }

    public List<String> getTrustedSslDomains() {
        return mTrustedSslDomains;
    }

    public String getMetaInfo() {
        JSONObject metaInfoJSON = new JSONObject();
        try {
            metaInfoJSON.put(KEY_PACKAGE, mPackage);
            metaInfoJSON.put(KEY_NAME, getName());
            metaInfoJSON.put(KEY_ICON, mIcon);
            metaInfoJSON.put(KEY_VERSION_NAME, mVersionName);
            metaInfoJSON.put(KEY_VERSION_CODE, mVersionCode);
            metaInfoJSON.put(KEY_MIN_PLATFORM_VERSION, mMinPlatformVersion);
            JSONObject configJSON = null;
            if (null != mRawData) {
                configJSON = mRawData.optJSONObject(KEY_CONFIG);
            }
            metaInfoJSON.put(KEY_CONFIG, configJSON != null ? configJSON : new JSONObject());
        } catch (JSONException e) {
            Log.e(TAG, "getMetaInfo fail", e);
        }
        return metaInfoJSON.toString();
    }
}
