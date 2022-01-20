/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;
import android.view.Window;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import org.hapjs.card.api.AppInfo;
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
import org.hapjs.card.api.debug.CardDebugHost;
import org.hapjs.card.common.utils.CardClassLoaderHelper;
import org.hapjs.card.common.utils.CardConfigHelper;
import org.hapjs.card.sdk.debug.SdkCardDebugReceiver;
import org.hapjs.card.sdk.utils.CardServiceLoader;
import org.hapjs.card.sdk.utils.ResourceInjector;

public class CardClient {
    private static final String TAG = "CardClient";
    private static volatile CardClient sInstance;
    private Set<String> mUnsupportedMethodSet = Collections.synchronizedSet(new HashSet<String>());
    private CardService mService;
    private volatile InitStatus mInitStatus = InitStatus.NONE;
    private Vector<Card> mCards = new Vector<>();

    private CardClient(Context context) {
        initInternal(context);
    }

    /**
     * Init CardClient blocked
     *
     * @param context the context
     * @return true if success, false otherwise
     */
    public static boolean init(Context context) {
        if (sInstance == null) {
            synchronized (CardClient.class) {
                if (sInstance == null) {
                    if (isUserUnlocked(context)) {
                        CardClient instance = new CardClient(context);
                        if (instance.mInitStatus == InitStatus.SUCCESS) {
                            sInstance = instance;
                        }
                    } else {
                        Log.w(TAG, "user is not unlocked. refuse to init.");
                    }
                }
            }
        }
        return sInstance != null;
    }

    /**
     * Init CardClient asynchronously
     *
     * @param context  the context
     * @param callback the callback to receive init result
     */
    public static void initAsync(final Context context, final InitCallback callback) {
        if (isUserUnlocked(context)) {
            initAsyncInternal(context, callback);
        } else {
            registerUserUnlockedReceiver(context, callback);
        }
    }

    private static boolean isUserUnlocked(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            return userManager.isUserUnlocked();
        } else {
            return true;
        }
    }

    private static void initAsyncInternal(final Context context, final InitCallback callback) {
        SdkCardDebugReceiver.onAsyncInitStatus(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    init(context);
                    CardClient instance = getInstance();
                    if (callback != null) {
                        if (instance == null) {
                            callback.onInitFail();
                        } else {
                            callback.onInitSuccess(instance);
                        }
                    }
                } finally {
                    SdkCardDebugReceiver.onAsyncInitStatus(false);
                }
            }
        }).start();
    }

    private static void registerUserUnlockedReceiver(final Context context, final InitCallback callback) {
        Log.i(TAG, "wait for ACTION_USER_UNLOCKED");
        BroadcastReceiver userUnlockedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) {
                    Log.i(TAG, "receive ACTION_USER_UNLOCKED. do initialization");
                    try {
                        context.unregisterReceiver(this);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "failed to unregister userUnlockedReceiver");
                    }
                    initAsyncInternal(context, callback);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_UNLOCKED);
        context.registerReceiver(userUnlockedReceiver, filter);
    }

    /**
     * Gets CardClient instance
     *
     * @return null if {@link #init(Context)} or {@link #initAsync(Context, InitCallback)} has not finished
     */
    public static CardClient getInstance() {
        return sInstance;
    }

    /**
     * Reset instance state. This should be called after quick app platform upgrade
     */
    public static void reset() {
        synchronized (CardClient.class) {
            sInstance = null;
        }
        CardClassLoaderHelper.clearClassLoader();
        CardServiceLoader.clearCardServiceClass();
    }

    private void initInternal(Context context) {
        if (mService != null) {
            Log.w(TAG, "client has init");
            return;
        }

        context = context.getApplicationContext();
        // must load service first
        mService = CardServiceLoader.load(context);
        if (mService == null) {
            mInitStatus = InitStatus.FAIL;
            return;
        }

        if (!CardConfigHelper.isLoadFromLocal(context)) {
            boolean result = ResourceInjector.inject(context);
            if (!result) {
                mInitStatus = InitStatus.FAIL;
                return;
            }
        }

        try {
            mService.init(context, CardConfigHelper.getPlatform(context));
            mInitStatus = InitStatus.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Fail to init service", e);
            mInitStatus = InitStatus.FAIL;
        }
    }

    public int getPlatformVersion() {
        return mService.getPlatformVersion();
    }

    public CardInfo getCardInfo(String uri) {
        return mService.getCardInfo(uri);
    }

    public AppInfo getAppInfo(String pkg) {
        return mService.getAppInfo(pkg);
    }

    public Card createCardOnActivity(Activity activity) {
        return createCardOnActivity(activity, null);
    }

    public Card createCardOnActivity(Activity activity, String uri) {
        ResourceInjector.inject(activity);
        Card card = mService.createCard(activity, uri);
        if (card == null) {
            return null;
        }
        mCards.add(card);
        return card;
    }

    @Deprecated
    public Card createCard(Activity activity, String uri) {
        return createCardOnActivity(activity, uri);
    }

    @Deprecated
    public Card createCard(Activity activity) {
        return createCardOnActivity(activity);
    }

    public Card createCardOnWindow(Context context) {
        return createCardOnWindow(context, null, null);
    }

    public Card createCardOnWindow(Context context, String uri, Window window) {
        return createCardOnActivity(new MockActivity(context, window), uri);
    }

    public Inset createInsetOnActivity(Activity activity, String uri) {
        ResourceInjector.inject(activity);
        Inset inset = mService.createInset(activity, uri);
        if (inset == null) {
            return null;
        }
        mCards.add(inset);
        return inset;
    }

    public Inset createInsetOnActivity(Activity activity) {
        return createInsetOnActivity(activity, null);
    }

    public boolean grantPermissions(String uri) {
        return mService.grantPermissions(uri);
    }

    public void download(String pkg, int versionCode, DownloadListener listener) {
        try {
            downloadOrThrow(pkg, versionCode, listener);
        } catch (IncompatibleException e) {
            Log.w(TAG, "download: ", e);
        }
    }

    public void downloadOrThrow(String pkg, int versionCode, DownloadListener listener) throws IncompatibleException {
        Throwable cause = null;
        if (!mUnsupportedMethodSet.contains("download")) {
            try {
                mService.download(pkg, versionCode, listener);
                return;
            } catch (IncompatibleClassChangeError e) {
                mUnsupportedMethodSet.add("download");
                cause = e;
            }
        }
        throw new IncompatibleException("download not supported", cause);
    }

    public void install(String pkg, String fileUri, InstallListener listener) {
        mService.install(pkg, fileUri, listener);
    }

    public void install(String pkg, int versionCode, InstallListener listener) {
        try {
            installOrThrow(pkg, versionCode, listener);
        } catch (IncompatibleException e) {
            Log.w(TAG, "install: ", e);
        }
    }

    public void installOrThrow(String pkg, int versionCode, InstallListener listener) throws IncompatibleException {
        Throwable cause = null;
        if (!mUnsupportedMethodSet.contains("install(String, int, InstallListener)")) {
            try {
                mService.install(pkg, versionCode, listener);
                return;
            } catch (IncompatibleClassChangeError e) {
                mUnsupportedMethodSet.add("install(String, int, InstallListener)");
                cause = e;
            }
        }
        throw new IncompatibleException("install(String, int, InstallListener) not supported", cause);
    }

    public void setCardDebugHost(CardDebugHost host) {
        SdkCardDebugReceiver.setCardDebugHost(host);
    }

    //give a interface to host app to destroy cards
    public void destroy() {
        if (mService != null) {
            removeAllCard();
        }
    }

    /*if host app add view in listView or recycleView , when list scroll,
     * cardView onDetachedFromWindow/onAttachedToWindow will call, but card must reused,
     * if list reuse, RootView will throw IllegalStateException("Can't reuse a RootView");
     * so need add destroy method*/
    public void removeAllCard() {
        for (Card card : mCards) {
            card.destroy();
        }
        mCards.clear();
    }

    /*set card default theme, Attributes will used it*/
    public void setTheme(Context context, String theme) {
        mService.setTheme(context, theme);
    }

    // set card default config
    public void setConfig(CardConfig config) {
        try {
            mService.setConfig(config);
        } catch (IncompatibleClassChangeError e) {
            Log.w(TAG, "setConfig: " + e);
        }
    }

    public void startDebug(Context context) {
        SdkCardDebugReceiver.register(context);
    }

    public void stopDebug(Context context) {
        SdkCardDebugReceiver.unregister(context);
    }

    /**
     * 设置日志listener
     */
    public void setLogListener(LogListener listener) {
        try {
            setLogListenerOrThrow(listener);
        } catch (IncompatibleException e) {
            Log.w(TAG, "setLogListener: ", e);
        }
    }

    public void setLogListenerOrThrow(LogListener listener) throws IncompatibleException {
        Throwable cause = null;
        if (!mUnsupportedMethodSet.contains("setLogListener")) {
            try {
                mService.setLogListener(listener);
                return;
            } catch (IncompatibleClassChangeError e) {
                mUnsupportedMethodSet.add("setLogListener");
                cause = e;
            }
        }
        throw new IncompatibleException("setLogListener not supported", cause);
    }

    public void setRuntimeErrorListener(RuntimeErrorListener listener) {
        try {
            setRuntimeErrorListenerOrThrow(listener);
        } catch (IncompatibleException e) {
            Log.w(TAG, "setRuntimeErrorListener: ", e);
        }
    }

    public void setRuntimeErrorListenerOrThrow(RuntimeErrorListener listener) throws IncompatibleException {
        Throwable cause = null;
        if (!mUnsupportedMethodSet.contains("setRuntimeErrorListener")) {
            try {
                mService.setRuntimeErrorListener(listener);
                return;
            } catch (IncompatibleClassChangeError e) {
                mUnsupportedMethodSet.add("setRuntimeErrorListener");
                cause = e;
            }
        }
        throw new IncompatibleException("setRuntimeErrorListener not supported", cause);
    }

    public void uninstall(String pkg, UninstallListener listener) {
        try {
            uninstallOrThrow(pkg, listener);
        } catch (IncompatibleException e) {
            Log.w(TAG, "uninstall: ", e);
        }
    }

    public void uninstallOrThrow(String pkg, UninstallListener listener) throws IncompatibleException {
        Throwable cause = null;
        if (!mUnsupportedMethodSet.contains("uninstall")) {
            try {
                mService.uninstall(pkg, listener);
                return;
            } catch (IncompatibleClassChangeError e) {
                mUnsupportedMethodSet.add("uninstall");
                cause = e;
            }
        }
        throw new IncompatibleException("uninstall not supported", cause);
    }

    public void getAllApps(GetAllAppsListener listener) {
        try {
            getAllAppsOrThrow(listener);
        } catch (IncompatibleException e) {
            Log.w(TAG, "getAllApps: ", e);
        }
    }

    public void getAllAppsOrThrow(GetAllAppsListener listener) throws IncompatibleException {
        Throwable cause = null;
        if (!mUnsupportedMethodSet.contains("getAllApps")) {
            try {
                mService.getAllApps(listener);
            } catch (IncompatibleClassChangeError e) {
                mUnsupportedMethodSet.add("getAllApps");
                cause = e;
            }
        }
        throw new IncompatibleException("getAllApps not supported", cause);
    }

    private enum InitStatus {
        NONE, SUCCESS, FAIL
    }

    public interface InitCallback {
        void onInitSuccess(CardClient client);

        void onInitFail();
    }
}
