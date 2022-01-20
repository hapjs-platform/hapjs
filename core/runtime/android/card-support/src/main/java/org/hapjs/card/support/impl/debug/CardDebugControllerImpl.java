/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl.debug;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.card.api.debug.CardDebugController;
import org.hapjs.card.api.debug.CardDebugHost;
import org.hapjs.debug.DebugUtils;
import org.hapjs.runtime.PermissionChecker;

public class CardDebugControllerImpl implements CardDebugController {
    private static final String TAG = "CardDebugControllerImpl";

    private CardDebugHost mCardDebugHost;

    private HandlerThread mThread;
    private HandlerImpl mHandler;
    private Context mContext;

    public CardDebugControllerImpl(Context context) {
        mContext = context;
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new HandlerImpl(mThread.getLooper());
    }

    private void onReceiveUnknownMessage(Message msg) {
        Log.e(TAG, "Invalid message: " + msg.what);

        Bundle data = new Bundle();
        data.putBoolean(EXTRA_RESULT, false);
        data.putInt(EXTRA_ERROR_CODE, CODE_UNKNOWN_MESSAGE);

        sendMessageToTargetPlatform(msg.getData().getString(EXTRA_FROM), data);
    }

    private void onLaunchCard(Message msg) {
        Bundle params = msg.getData();
        String url = params.getString(EXTRA_CARD_URL);

        Bundle data = new Bundle();
        data.putString(EXTRA_CARD_URL, url);

        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "Invalid  card url: " + url);
            data.putBoolean(EXTRA_RESULT, false);
            data.putInt(EXTRA_ERROR_CODE, CODE_UNKNOWN_ERROR);
        } else if (mCardDebugHost == null) {
            Log.e(TAG, "No CardDebugCallback");
            data.putBoolean(EXTRA_RESULT, false);
            data.putInt(EXTRA_ERROR_CODE, CODE_UNSUPPORT_DEBUG);
        } else {
            boolean result = mCardDebugHost.launch(mContext, url);
            data.putBoolean(EXTRA_RESULT, result);
        }

        data.putInt(EXTRA_MESSAGE_CODE, params.getInt(EXTRA_MESSAGE_CODE));
        sendMessageToTargetPlatform(params.getString(EXTRA_FROM), data);
    }

    private void onDebugCard(Message msg) {
        Bundle params = msg.getData();
        String url = params.getString(EXTRA_CARD_URL);
        Bundle data = new Bundle();
        data.putString(EXTRA_CARD_URL, url);
        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "Invalid  card url: " + url);
            data.putBoolean(EXTRA_RESULT, false);
            data.putInt(EXTRA_ERROR_CODE, CODE_UNKNOWN_ERROR);
        } else if (mCardDebugHost == null) {
            Log.e(TAG, "No CardDebugCallback");
            data.putBoolean(EXTRA_RESULT, false);
            data.putInt(EXTRA_ERROR_CODE, CODE_UNSUPPORT_DEBUG);
        } else {
            String server = params.getString(EXTRA_SERVER);
            String pkg = new HybridRequest.Builder().uri(url).build().getPackage();
            boolean useADB = params.getBoolean(EXTRA_USE_ADB);
            String serialNumber = params.getString(EXTRA_SERIAL_NUMBER);
            boolean waitDevTools = params.getBoolean(EXTRA_WAIT_DEVTOOLS);
            int platformVersionCode = params.getInt(EXTRA_PLATFORM_VERSION_CODE);
            String debugUrl =
                    DebugUtils.appendDebugParams(
                            url, server, pkg, serialNumber, useADB, platformVersionCode,
                            waitDevTools, null, "");
            boolean result = mCardDebugHost.launch(mContext, debugUrl);
            data.putBoolean(EXTRA_RESULT, result);
            DebugUtils.resetDebugger();
        }

        data.putInt(EXTRA_MESSAGE_CODE, params.getInt(EXTRA_MESSAGE_CODE));
        sendMessageToTargetPlatform(params.getString(EXTRA_FROM), data);
    }

    private void onIsSupported(Message msg) {
        Bundle params = msg.getData();
        Bundle data = new Bundle();
        data.putBoolean(EXTRA_RESULT, true);
        data.putBoolean(EXTRA_IS_SUPPORTED, mCardDebugHost != null);
        data.putInt(EXTRA_MESSAGE_CODE, params.getInt(EXTRA_MESSAGE_CODE));
        sendMessageToTargetPlatform(params.getString(EXTRA_FROM), data);
    }

    @Override
    public void handleDebugMessage(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null || intent.getExtras().isEmpty()) {
            Log.e(TAG, "Received null data");
            return;
        }

        String callingPkg = intent.getStringExtra(EXTRA_FROM);
        if (TextUtils.isEmpty(callingPkg)
                || !checkPermission(context, callingPkg)
                || !PermissionChecker.verify(context, callingPkg)) {
            Log.e(TAG, "Received ungranted request");
            return;
        }

        Message message = new Message();
        message.setData(intent.getExtras());
        message.what = intent.getExtras().getInt(EXTRA_MESSAGE_CODE);
        mHandler.sendMessage(message);
    }

    /*check pkg owner the permission and permission level is signature*/
    private boolean checkPermission(Context context, String callingPkg) {
        try {
            PermissionInfo info =
                    context.getPackageManager().getPermissionInfo(PERMISSION_DEBUG_CARD, 0);
            return info.packageName.equals(callingPkg)
                    && info.protectionLevel == PermissionInfo.PROTECTION_SIGNATURE;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "check permission error", e);
        }
        return false;
    }

    @Override
    public void setCardDebugHost(CardDebugHost host) {
        mCardDebugHost = host;
    }

    private void sendMessageToTargetPlatform(String targetPackage, Bundle data) {
        Log.d(TAG, "sendMessageToTargetPlatform: " + targetPackage);

        String archiveHost = null;
        String runtimeHost = null;
        try {
            archiveHost = mCardDebugHost.getArchiveHost();
            runtimeHost = mCardDebugHost.getRuntimeHost();
        } catch (Throwable t) {
            Log.w(TAG, "failed to get host", t);
        }

        data.putString(EXTRA_FROM, mContext.getPackageName());
        data.putString(EXTRA_ARCHIVE_HOST, archiveHost);
        data.putString(EXTRA_RUNTIME_HOST, runtimeHost);
        Intent intent = new Intent(ACTION_CARD_DEBUG_RESULT);
        intent.setPackage(targetPackage);
        intent.putExtras(data);
        try {
            mContext.sendBroadcast(intent, CardDebugController.PERMISSION_DEBUG_CARD);
        } catch (Exception e) {
            Log.e(TAG, "Fail to send reply message", e);
        }
    }

    private class HandlerImpl extends Handler {

        public HandlerImpl(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LAUNCH_CARD:
                    onLaunchCard(msg);
                    break;
                case MSG_DEBUG_CARD:
                    onDebugCard(msg);
                    break;
                case MSG_IS_SUPPORT:
                    onIsSupported(msg);
                    break;
                default:
                    onReceiveUnknownMessage(msg);
                    break;
            }
        }
    }
}
