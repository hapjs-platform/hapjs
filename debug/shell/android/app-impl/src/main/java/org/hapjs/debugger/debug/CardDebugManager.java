/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.debug;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hapjs.card.api.debug.CardDebugController;
import org.hapjs.card.common.utils.CardConfigHelper;
import org.hapjs.debugger.pm.PackageInfo;
import org.hapjs.debugger.pm.PackageManager;
import org.hapjs.debugger.utils.AppUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

public class CardDebugManager {
    public static final int SERVICE_CODE_UNKNOWN_ERROR = CardDebugController.CODE_UNKNOWN_ERROR;
    public static final int SERVICE_CODE_UNKNOWN_MESSAGE = CardDebugController.CODE_UNKNOWN_MESSAGE;
    public static final int SERVICE_CODE_UNSUPPORT_DEBUG = CardDebugController.CODE_UNSUPPORT_DEBUG;
    public static final int SERVICE_CODE_UNSUPPORT_CARD = CardDebugController.CODE_UNSUPPORT_CARD;
    public static final int ERROR_CODE_UNKNOWN_ERROR = 100;
    public static final int ERROR_CODE_SEND_ERROR = 101;
    public static final int ERROR_CODE_HOST_NOT_CHOOSE = 102;
    private static final String TAG = "SdkCardDebugManager";
    private static final String ACTION_DEBUG_CARD = CardDebugController.ACTION_DEBUG_CARD;
    private static final int MSG_SERVICE_IS_SUPPORT = CardDebugController.MSG_IS_SUPPORT;
    private static final int MSG_SERVICE_LAUNCH_CARD = CardDebugController.MSG_LAUNCH_CARD;
    private static final int MSG_SERVICE_DEBUG_CARD = CardDebugController.MSG_DEBUG_CARD;
    private static final String EXTRA_CARD_URL = CardDebugController.EXTRA_CARD_URL;
    private static final String EXTRA_SERVER = CardDebugController.EXTRA_SERVER;
    private static final String EXTRA_RESULT = CardDebugController.EXTRA_RESULT;
    private static final String EXTRA_ERROR_CODE = CardDebugController.EXTRA_ERROR_CODE;
    private static final String EXTRA_SHOULD_RELOAD = CardDebugController.EXTRA_SHOULD_RELOAD;
    private static final String EXTRA_USE_ADB = CardDebugController.EXTRA_USE_ADB;
    private static final String EXTRA_SERIAL_NUMBER = CardDebugController.EXTRA_SERIAL_NUMBER;
    private static final String EXTRA_PLATFORM_VERSION_CODE = CardDebugController.EXTRA_PLATFORM_VERSION_CODE;
    private static final String EXTRA_WAIT_DEVTOOLS = CardDebugController.EXTRA_WAIT_DEVTOOLS;
    private static final String EXTRA_MESSAGE_CODE = CardDebugController.EXTRA_MESSAGE_CODE;
    private static final String EXTRA_IS_SUPPORTED = CardDebugController.EXTRA_IS_SUPPORTED;
    private static final String EXTRA_FROM = CardDebugController.EXTRA_FROM;
    private static final String EXTRA_ARCHIVE_HOST = CardDebugController.EXTRA_ARCHIVE_HOST;
    private static final String EXTRA_RUNTIME_HOST = CardDebugController.EXTRA_RUNTIME_HOST;
    private static final Object sLocker = new Object();
    private static volatile CardDebugManager sInstance;
    private Context mContext;
    private CardDebugListener mListener;
    private Set<CardHostInfo> mCardHosts = new HashSet<>();
    private int mUsedPlatformVersionCode;

    private CardDebugManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static CardDebugManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (sLocker) {
                CardDebugManager instance = new CardDebugManager(context);
                sInstance = instance;
            }
        }
        return sInstance;
    }

    public void setDebugListener(CardDebugListener listener) {
        mListener = listener;
    }

    public void setUsedPlatformVersionCode(int platformVersionCode) {
        mUsedPlatformVersionCode = platformVersionCode;
    }

    public void startDebugging(String pkg, String path, String server) {
        String url = getCardUrl(pkg, path);
        boolean isUseADB = PreferenceUtils.isUseADB(mContext);

        Bundle data = new Bundle();
        data.putString(EXTRA_CARD_URL, url);
        data.putString(EXTRA_SERVER, server);
        data.putBoolean(EXTRA_USE_ADB, isUseADB);
        data.putString(EXTRA_SERIAL_NUMBER, isUseADB ? AppUtils.getSerialNumber() : "");
        data.putInt(EXTRA_PLATFORM_VERSION_CODE, mUsedPlatformVersionCode);
        data.putBoolean(EXTRA_WAIT_DEVTOOLS, PreferenceUtils.isWaitDevTools(mContext));
        data.putInt(EXTRA_MESSAGE_CODE, MSG_SERVICE_DEBUG_CARD);
        sendMessageToTargetPlatform(data);
    }

    public void launchCard(String pkg, String path) {
        String url = getCardUrl(pkg, path);
        Bundle data = new Bundle();
        data.putString(EXTRA_CARD_URL, url);
        data.putBoolean(EXTRA_SHOULD_RELOAD, PreferenceUtils.shouldReloadPackage(mContext));
        data.putInt(EXTRA_MESSAGE_CODE, MSG_SERVICE_LAUNCH_CARD);
        sendMessageToTargetPlatform(data);
    }

    private void sendMessageToTargetPlatform(Bundle data) {
        CardHostInfo cardHostInfo = CardHostInfo.fromString(PreferenceUtils.getCardHostPlatform(mContext));
        if (cardHostInfo == null) {
            onError(ERROR_CODE_HOST_NOT_CHOOSE);
            return;
        }
        sendMessageToTargetPlatform(cardHostInfo.runtimeHost, data);
    }

    private void sendMessageToTargetPlatform(String platform, Bundle data) {
        if (data == null) {
            onError(ERROR_CODE_UNKNOWN_ERROR);
            return;
        }

        Intent intent = new Intent(ACTION_DEBUG_CARD);
        intent.setPackage(platform);
        data.putString(EXTRA_FROM, mContext.getPackageName());
        intent.putExtras(data);
        try {
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
            onError(ERROR_CODE_SEND_ERROR);
        }
    }

    private String getCardUrl(String pkg, String path) {
        String url = "hap://card/" + pkg + path;
        String params = PreferenceUtils.getLaunchParams(mContext);
        if (!TextUtils.isEmpty(params)) {
            url += "?" + params;
        }
        return url;
    }

    public void querySupportedHostPlatforms() {
        mCardHosts.clear();
        android.content.pm.PackageManager pm = mContext.getPackageManager();
        Intent receiverIntent = new Intent(ACTION_DEBUG_CARD);

        Bundle data = new Bundle();
        data.putInt(EXTRA_MESSAGE_CODE, MSG_SERVICE_IS_SUPPORT);

        //query manifest-declared receivers with explicit broadcast
        List<ResolveInfo> ris = pm.queryBroadcastReceivers(receiverIntent, android.content.pm
                .PackageManager.GET_RESOLVED_FILTER);
        if (ris != null && ris.size() > 0) {
            for (ResolveInfo ri : ris) {
                String pkg = ri.activityInfo.applicationInfo.packageName;
                sendMessageToTargetPlatform(pkg, data);
            }
        }

        //query context-registered receiver with implicit broadcast
        sendMessageToTargetPlatform(null, data);
    }

    public String getCorePlatform(String hostPlatform) {
        Context context;
        try {
            context = getInstalledContext(mContext, hostPlatform);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
        return CardConfigHelper.getPlatform(context);
    }

    private Context getInstalledContext(Context context, String pkg) throws android.content.pm
            .PackageManager.NameNotFoundException {
        return context.createPackageContext(pkg, 0);
    }

    public PackageInfo getPackageInfo(String pkg) {
        String path = PreferenceUtils.getDebugRpkPath(mContext);
        PackageInfo info = PackageManager.getPackageInfo(path);
        return info != null && TextUtils.equals(pkg, info.getPackage()) ? info : null;
    }

    public void close() {
        sInstance = null;
    }

    public void handleServerMessage(Intent intent) {
        if (intent == null || intent.getExtras() == null || intent.getExtras().isEmpty()) {
            Log.e(TAG, "Received null data");
            return;
        }

        int code = intent.getIntExtra(EXTRA_MESSAGE_CODE, -1);
        String callingPkg = intent.getStringExtra(EXTRA_FROM);
        if (TextUtils.isEmpty(callingPkg) || code == -1) {
            Log.e(TAG, "unknow message");
            return;
        }
        Log.d(TAG, "handleServerMessage " + intent.getExtras());

        String archiveHost = intent.getStringExtra(EXTRA_ARCHIVE_HOST);
        String runtimeHost = intent.getStringExtra(EXTRA_RUNTIME_HOST);
        runtimeHost = TextUtils.isEmpty(runtimeHost) ? callingPkg : runtimeHost;
        archiveHost = TextUtils.isEmpty(archiveHost) ? callingPkg : archiveHost;
        CardHostInfo newHost = new CardHostInfo(runtimeHost, archiveHost);
        if (MSG_SERVICE_IS_SUPPORT == code) {
            boolean isSupported = intent.getBooleanExtra(EXTRA_IS_SUPPORTED, false);
            updateCardHosts(newHost, isSupported);
            return;
        }

        CardHostInfo currentHost = CardHostInfo.fromString(PreferenceUtils.getCardHostPlatform(mContext));
        if (!newHost.equals(currentHost)) {
            Log.d(TAG, "ignore message from " + callingPkg);
            return;
        }

        boolean result = intent.getBooleanExtra(EXTRA_RESULT, true);
        if (!result) {
            int error = intent.getIntExtra(EXTRA_ERROR_CODE, SERVICE_CODE_UNKNOWN_ERROR);
            onError(error);
            return;
        }

        switch (code) {
            case MSG_SERVICE_LAUNCH_CARD:
                onLaunchResult();
                break;
            case MSG_SERVICE_DEBUG_CARD:
                onDebugResult();
                break;
            default:
                Log.d(TAG, "ignore unknow message from " + callingPkg);
        }
    }

    private void updateCardHosts(CardHostInfo cardHost, boolean isSupport) {
        boolean changed;
        if (isSupport) {
            changed = mCardHosts.add(cardHost);
        } else {
            changed = mCardHosts.remove(cardHost);
        }
        if (mListener != null && changed) {
            ArrayList<CardHostInfo> list = new ArrayList<>();
            list.addAll(mCardHosts);
            mListener.updateCardHosts(list);
        }
    }

    private void onLaunchResult() {
        if (mListener != null) {
            mListener.onLaunchResult();
        }
    }

    private void onDebugResult() {
        if (mListener != null) {
            mListener.onDebugResult();
        }
    }

    private void onError(int code) {
        if (mListener != null) {
            mListener.onError(code);
        }
    }

    public interface CardDebugListener {
        void onLaunchResult();

        void onDebugResult();

        void onError(int code);

        void updateCardHosts(List<CardHostInfo> cardHosts);
    }

}
