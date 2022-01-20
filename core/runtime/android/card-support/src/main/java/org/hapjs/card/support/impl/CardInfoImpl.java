/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.permission.RuntimePermissionManager;
import org.hapjs.card.api.AppDependency;
import org.hapjs.card.api.CardInfo;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.Runtime;

class CardInfoImpl implements CardInfo {
    private String mTitle;
    private String mDescription;
    private int mMinPlatformVersion;
    private Collection<String> mPermissions;
    private Uri mIcon;
    private Map<String, Map<String, AppDependency>> mDependencies;

    private CardInfoImpl(
            String title,
            String description,
            int minPlatformVersion,
            Collection<String> permissions,
            Uri icon,
            Map<String, Map<String, AppDependency>> dependencies) {
        mTitle = title;
        mDescription = description;
        mMinPlatformVersion = minPlatformVersion;
        mPermissions = permissions;
        mIcon = icon;
        mDependencies = dependencies;
    }

    static CardInfoImpl retrieveCardInfo(String uri) {
        org.hapjs.model.CardInfo cardInfo = findCardInfo(uri);
        if (cardInfo != null) {
            Uri iconUri = null;
            AppInfo appInfo = getAppInfo(uri);
            String iconPath = cardInfo.getIcon();
            if (TextUtils.isEmpty(iconPath)) {
                iconPath = appInfo.getIcon();
            }
            if (!TextUtils.isEmpty(iconPath)) {
                iconUri =
                        HapEngine.getInstance(appInfo.getPackage()).getResourceManager()
                                .getResource(iconPath);
            }

            Map<String, Map<String, org.hapjs.model.AppDependency>> originalDependencies =
                    cardInfo.getDependencies();
            Map<String, Map<String, AppDependency>> dependApps =
                    transformDependApps(originalDependencies);

            return new CardInfoImpl(
                    cardInfo.getTitle(),
                    cardInfo.getDescription(),
                    cardInfo.getMinPlatformVersion(),
                    cardInfo.getPermissions(),
                    iconUri,
                    dependApps);
        }
        return null;
    }

    private static Map<String, Map<String, AppDependency>> transformDependApps(
            Map<String, Map<String, org.hapjs.model.AppDependency>> originalDependencies) {
        Map<String, Map<String, AppDependency>> dependencies = new HashMap<>();
        if (originalDependencies != null) {
            for (Map.Entry<String, Map<String, org.hapjs.model.AppDependency>> entry :
                    originalDependencies.entrySet()) {
                String platform = entry.getKey();
                Map<String, org.hapjs.model.AppDependency> originalPlatformDependencies =
                        entry.getValue();
                Map<String, AppDependency> platformDepedencies = new HashMap<>();
                if (originalPlatformDependencies != null) {
                    for (Map.Entry<String, org.hapjs.model.AppDependency> entry2 :
                            originalPlatformDependencies.entrySet()) {
                        String pkg = entry2.getKey();
                        org.hapjs.model.AppDependency appDependency = entry2.getValue();
                        platformDepedencies.put(
                                pkg,
                                new AppDependency(appDependency.pkg, appDependency.minVersion));
                    }
                }
                dependencies.put(platform, platformDepedencies);
            }
        }
        return dependencies;
    }

    private static org.hapjs.model.CardInfo findCardInfo(String uri) {
        HybridRequest request = new HybridRequest.Builder().uri(uri).build();
        if (request instanceof HybridRequest.HapRequest) {
            HybridRequest.HapRequest hapRequest = (HybridRequest.HapRequest) request;
            HapEngine engine = HapEngine.getInstance(hapRequest.getPackage());
            engine.setMode(HapEngine.Mode.CARD);
            AppInfo appInfo = engine.getApplicationContext().getAppInfo();
            return appInfo == null
                    ? null
                    : appInfo.getRouterInfo().getCardInfoByPath(hapRequest.getPagePath());
        }
        return null;
    }

    private static AppInfo getAppInfo(String uri) {
        HybridRequest request = new HybridRequest.Builder().uri(uri).build();
        if (request instanceof HybridRequest.HapRequest) {
            HybridRequest.HapRequest hapRequest = (HybridRequest.HapRequest) request;
            HapEngine engine = HapEngine.getInstance(hapRequest.getPackage());
            engine.setMode(HapEngine.Mode.CARD);
            return engine.getApplicationContext().getAppInfo();
        }
        return null;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public int getMinPlatformVersion() {
        return mMinPlatformVersion;
    }

    @Override
    public Collection<String> getPermissionDescriptions() {
        if (mPermissions == null) {
            return null;
        }

        List<String> permissionDescriptions = new ArrayList<>(mPermissions.size());
        RuntimePermissionManager manager = RuntimePermissionManager.getDefault();
        Context context = Runtime.getInstance().getContext();
        for (String permission : mPermissions) {
            permissionDescriptions.add(manager.getPermissionDescription(context, permission));
        }
        return permissionDescriptions;
    }

    @Override
    public Uri getIcon() {
        return mIcon;
    }

    @Override
    public Map<String, Map<String, AppDependency>> getDependencies() {
        return mDependencies;
    }

    Collection<String> getPermissions() {
        return mPermissions;
    }
}
