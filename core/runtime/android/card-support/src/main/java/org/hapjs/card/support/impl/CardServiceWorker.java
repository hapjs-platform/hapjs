/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.imagepipeline.nativecode.ImagePipelineNativeLoader;
import com.facebook.soloader.SoLoader;
import java.util.Collection;
import org.hapjs.bridge.FeatureBridge;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.permission.RuntimePermissionManager;
import org.hapjs.cache.CacheConstants;
import org.hapjs.card.api.Card;
import org.hapjs.card.api.CardConfig;
import org.hapjs.card.api.CardService;
import org.hapjs.card.api.DownloadListener;
import org.hapjs.card.api.GetAllAppsListener;
import org.hapjs.card.api.Inset;
import org.hapjs.card.api.InstallListener;
import org.hapjs.card.api.LogListener;
import org.hapjs.card.api.RuntimeErrorListener;
import org.hapjs.card.api.UninstallListener;
import org.hapjs.card.api.debug.CardDebugService;
import org.hapjs.card.sdk.utils.CardConfigUtils;
import org.hapjs.card.sdk.utils.CardThemeUtils;
import org.hapjs.card.support.CardInstaller;
import org.hapjs.card.support.impl.debug.CardDebugControllerImpl;
import org.hapjs.card.support.utils.CardRuntimeErrorManager;
import org.hapjs.common.utils.FrescoUtils;
import org.hapjs.component.ComponentManager;
import org.hapjs.component.view.ScrollView;
import org.hapjs.logging.CardLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.render.jsruntime.JsThreadFactory;
import org.hapjs.render.jsruntime.module.ModuleBridge;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.Runtime;

public class CardServiceWorker implements CardService {
    private static final String TAG = "CardServiceWorker";

    @Override
    public void init(Context context, String platform) {
        Log.i(TAG, "CardServiceWorker init, platform=" + platform);
        ResourceConfig.getInstance().init(context, platform);
        Runtime.setPlatform(platform);
        Runtime.getInstance().onCreate(context);
        configBlacklist();
        JsThreadFactory.getInstance().preload(context);
        preloadSoLibrary();
        preloadScrollView(context);
        registerPackageChangedListener(context);
    }

    private void registerPackageChangedListener(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CacheConstants.ACTION_PACKAGE_PACKAGE_REMOVED);
        filter.addAction(CacheConstants.ACTION_PACKAGE_PACKAGE_UPDATED);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        context
                .getApplicationContext()
                .registerReceiver(
                        new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String pkg = intent.getStringExtra(CacheConstants.EXTRA_PACKAGE);
                                String platform =
                                        intent.getStringExtra(CacheConstants.EXTRA_PLATFORM);
                                if (!TextUtils.isEmpty(pkg)
                                        && TextUtils.equals(platform,
                                        ResourceConfig.getInstance().getPlatform())) {
                                    HapEngine.getInstance(pkg).getApplicationContext().reset();
                                }
                                Log.d(
                                        TAG,
                                        "onReceive: action="
                                                + intent.getAction()
                                                + ", pkg="
                                                + pkg
                                                + ", platform="
                                                + platform);
                            }
                        },
                        filter);
    }

    private void configBlacklist() {
        FeatureBridge.configCardBlacklist();
        ModuleBridge.configCardBlacklist();
        ComponentManager.configCardBlacklist();
    }

    private void preloadSoLibrary() {
        FrescoUtils.addInitializedCallback(() -> {
            try {
                SoLoader.loadLibrary("yoga");
                ImagePipelineNativeLoader.load();
            } catch (Exception e) {
                Log.e(TAG, "init: ", e);
            }
        });
    }

    private void preloadScrollView(Context context) {
        try {
            new ScrollView(context);
        } catch (Exception e) {
            Log.w(TAG, "preloadScrollView: ", e);
        }
    }

    @Override
    public int getPlatformVersion() {
        return org.hapjs.runtime.BuildConfig.platformVersion;
    }

    @Override
    public CardInfoImpl getCardInfo(String uri) {
        return CardInfoImpl.retrieveCardInfo(uri);
    }

    @Override
    public org.hapjs.card.api.AppInfo getAppInfo(String pkg) {
        AppInfo appInfo = HapEngine.getInstance(pkg).getApplicationContext().getAppInfo();
        if (appInfo != null) {
            return new org.hapjs.card.api.AppInfo(
                    appInfo.getPackage(),
                    appInfo.getName(),
                    appInfo.getVersionName(),
                    appInfo.getVersionCode(),
                    appInfo.getMinPlatformVersion());
        }
        return null;
    }

    @Override
    public Card createCard(Activity activity) {
        return new CardImpl(activity, HapEngine.Mode.CARD);
    }

    @Override
    public Card createCard(Activity activity, String uri) {
        return new CardImpl(activity, uri, HapEngine.Mode.CARD);
    }

    @Override
    public Inset createInset(Activity activity) {
        return new InsetImpl(activity, HapEngine.Mode.INSET);
    }

    @Override
    public Inset createInset(Activity activity, String uri) {
        return new InsetImpl(activity, uri, HapEngine.Mode.INSET);
    }

    @Override
    public boolean grantPermissions(String uri) {
        CardInfoImpl cardInfo = getCardInfo(uri);
        if (cardInfo == null) {
            return false;
        }
        Collection<String> permissions = cardInfo.getPermissions();
        if (permissions == null) {
            return false;
        }

        HybridRequest request = new HybridRequest.Builder().uri(uri).build();
        String pkg = request.getPackage();
        RuntimePermissionManager.getDefault()
                .grantPermissions(pkg, permissions.toArray(new String[permissions.size()]));
        return true;
    }

    @Override
    public void download(String pkg, int versionCode, DownloadListener listener) {
        CardInstaller.getInstance().download(pkg, versionCode, listener);
    }

    @Override
    public void install(String pkg, String fileUri, InstallListener listener) {
        CardInstaller.getInstance().install(pkg, fileUri, listener);
    }

    @Override
    public void install(String pkg, int versionCode, InstallListener listener) {
        CardInstaller.getInstance().install(pkg, versionCode, listener);
    }

    @Override
    public CardDebugService getCardDebugService() {
        return null;
    }

    @Override
    public CardDebugControllerImpl getCardDebugController() {
        return new CardDebugControllerImpl(Runtime.getInstance().getContext());
    }

    @Override
    public void setTheme(Context context, String theme) {
        CardThemeUtils.setTheme(context, theme);
    }

    @Override
    public void setLogListener(LogListener listener) {
        CardLogManager.setListener(listener);
    }

    @Override
    public void setConfig(CardConfig config) {
        CardConfigUtils.setCardConfig(config);
    }

    @Override
    public void setRuntimeErrorListener(RuntimeErrorListener listener) {
        CardRuntimeErrorManager.setListener(listener);
    }

    @Override
    public void uninstall(String pkg, UninstallListener listener) {
        CardInstaller.getInstance().uninstall(pkg, listener);
    }

    @Override
    public void getAllApps(GetAllAppsListener listener) {
        CardInstaller.getInstance().getAllApps(listener);
    }
}
