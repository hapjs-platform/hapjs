/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.card.api.Card;
import org.hapjs.card.api.CardConfig;
import org.hapjs.card.api.CardInfo;
import org.hapjs.card.api.CardService;
import org.hapjs.card.api.DownloadListener;
import org.hapjs.card.api.GetAllAppsListener;
import org.hapjs.card.api.Inset;
import org.hapjs.card.api.InstallListener;
import org.hapjs.card.api.LogListener;
import org.hapjs.card.api.RuntimeErrorListener;
import org.hapjs.card.api.UninstallListener;
import org.hapjs.card.api.debug.CardDebugController;
import org.hapjs.card.api.debug.CardDebugService;
import org.hapjs.card.common.utils.CardClassLoader;
import org.hapjs.card.common.utils.CardClassLoaderHelper;
import org.hapjs.card.common.utils.CardConfigHelper;
import org.hapjs.card.support.utils.InternalConfig;

/**
 * a wrapper class
 */
public class CardServiceImpl implements CardService {
    private static final String TAG = "CardServiceImpl";

    private static final String CARD_SERVICE_WORKER_CLASS =
            "org.hapjs.card.support.impl.CardServiceWorker";

    private CardService mCardService;

    private static String getCardServiceName(Context context, String platform) {
        String cardServiceName = null;
        if (TextUtils.isEmpty(platform)) {
            cardServiceName = InternalConfig.getInstance(context).getCardServiceName();
        } else {
            Context platformContext = getPlatformContext(context, platform);
            if (platformContext != null) {
                cardServiceName = InternalConfig.getInstance(platformContext).getCardServiceName();
            }
        }
        return cardServiceName;
    }

    private static Context getPlatformContext(Context context, String platform) {
        try {
            return context.createPackageContext(platform, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "failed to createPackageContext", e);
        }
        return null;
    }

    @Override
    public void init(Context context, String platform) {
        ClassLoader classLoader = CardServiceImpl.class.getClassLoader();
        if (!CardConfigHelper.isLoadFromLocal(context)
                && classLoader.getClass() != CardClassLoader.class) {
            classLoader =
                    CardClassLoaderHelper
                            .getClassLoader(context, classLoader.getParent(), platform);
        }

        String cardServiceName = getCardServiceName(context, platform);
        if (TextUtils.isEmpty(cardServiceName)) {
            cardServiceName = CARD_SERVICE_WORKER_CLASS;
        }

        try {
            Class serviceClass = Class.forName(cardServiceName, true, classLoader);
            mCardService = (CardService) serviceClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Fail to create CardService", e);
        }

        mCardService.init(context, platform);
    }

    @Override
    public int getPlatformVersion() {
        return mCardService.getPlatformVersion();
    }

    @Override
    public CardInfo getCardInfo(String uri) {
        return mCardService.getCardInfo(uri);
    }

    @Override
    public org.hapjs.card.api.AppInfo getAppInfo(String pkg) {
        return mCardService.getAppInfo(pkg);
    }

    @Override
    public Card createCard(Activity activity) {
        return mCardService.createCard(activity);
    }

    @Override
    public Card createCard(Activity activity, String uri) {
        return mCardService.createCard(activity, uri);
    }

    @Override
    public Inset createInset(Activity activity) {
        return mCardService.createInset(activity);
    }

    @Override
    public Inset createInset(Activity activity, String uri) {
        return mCardService.createInset(activity, uri);
    }

    @Override
    public boolean grantPermissions(String uri) {
        return mCardService.grantPermissions(uri);
    }

    @Override
    public void download(String pkg, int versionCode, DownloadListener listener) {
        mCardService.download(pkg, versionCode, listener);
    }

    @Override
    public void install(String pkg, String fileUri, InstallListener listener) {
        mCardService.install(pkg, fileUri, listener);
    }

    @Override
    public void install(String pkg, int versionCode, InstallListener listener) {
        mCardService.install(pkg, versionCode, listener);
    }

    @Override
    public CardDebugService getCardDebugService() {
        return mCardService.getCardDebugService();
    }

    @Override
    public CardDebugController getCardDebugController() {
        return mCardService.getCardDebugController();
    }

    @Override
    public void setTheme(Context context, String theme) {
        mCardService.setTheme(context, theme);
    }

    @Override
    public void setLogListener(LogListener listener) {
        mCardService.setLogListener(listener);
    }

    @Override
    public void setConfig(CardConfig config) {
        mCardService.setConfig(config);
    }

    @Override
    public void setRuntimeErrorListener(RuntimeErrorListener listener) {
        mCardService.setRuntimeErrorListener(listener);
    }

    @Override
    public void uninstall(String pkg, UninstallListener listener) {
        mCardService.uninstall(pkg, listener);
    }

    @Override
    public void getAllApps(GetAllAppsListener listener) {
        mCardService.getAllApps(listener);
    }
}
