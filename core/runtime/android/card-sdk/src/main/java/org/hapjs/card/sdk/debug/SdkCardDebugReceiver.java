/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.debug;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;
import org.hapjs.card.api.CardService;
import org.hapjs.card.api.debug.CardDebugController;
import org.hapjs.card.api.debug.CardDebugHost;
import org.hapjs.card.sdk.CardClient;
import org.hapjs.card.sdk.utils.CardServiceLoader;

public final class SdkCardDebugReceiver extends BroadcastReceiver {

    private static final String TAG = "SdkCardDebugReceiver";

    private static final String ACTION_DEBUG = CardDebugController.ACTION_DEBUG_CARD;
    private static final String PERMISSION_DEBUG = CardDebugController.PERMISSION_DEBUG_CARD;
    private static final int MSG_ON_RECEIVE = 0;
    private static CardDebugHost sCardDebugHost;
    private static SdkCardDebugReceiver sSdkCardDebugReceiver;
    private static Set<MsgData> sPendingMsg = new HashSet<>();
    private static boolean sIsAsyncInitializing;
    private static Handler sHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_RECEIVE:
                    boolean msgHandled = false;
                    MsgData msgData = (MsgData) msg.obj;
                    synchronized (SdkCardDebugReceiver.class) {
                        msgHandled = !sPendingMsg.remove(msgData);
                    }
                    if (!msgHandled) {
                        handleReceive(msgData.context, msgData.intent);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public static void setCardDebugHost(CardDebugHost host) {
        SdkCardDebugReceiver.sCardDebugHost = host;
    }

    public static synchronized void register(Context context) {
        ActivityInfo activityInfo = getReceiverInfo(context);
        if (activityInfo != null) {
            setReceiverEnabled(context, true);
            return;
        }
        sSdkCardDebugReceiver = new SdkCardDebugReceiver();
        try {
            context.getApplicationContext().registerReceiver(sSdkCardDebugReceiver,
                    new IntentFilter(ACTION_DEBUG),
                    PERMISSION_DEBUG, null);
        } catch (Exception e) {
            Log.e(TAG, "registerReceiver error", e);
        }
    }

    public static synchronized void unregister(Context context) {
        if (sSdkCardDebugReceiver != null) {
            try {
                context.getApplicationContext().unregisterReceiver(sSdkCardDebugReceiver);
            } catch (Exception e) {
                Log.e(TAG, "unregisterReceiver error", e);
            }
            sSdkCardDebugReceiver = null;
        } else {
            ActivityInfo activityInfo = getReceiverInfo(context);
            if (activityInfo != null) {
                setReceiverEnabled(context, false);
            }
        }
    }

    private static ActivityInfo getReceiverInfo(Context context) {
        try {
            return context.getPackageManager().getReceiverInfo(
                    new ComponentName(context, SdkCardDebugReceiver.class), PackageManager.GET_DISABLED_COMPONENTS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "name not found exception", e);
        }
        return null;
    }

    private static void setReceiverEnabled(Context context, boolean enable) {
        try {
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context,
                            SdkCardDebugReceiver.class), enable ? PackageManager
                            .COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            Log.e(TAG, "set debug enable", e);
        }
    }

    public static synchronized void onAsyncInitStatus(boolean isAsyncInitializing) {
        sIsAsyncInitializing = isAsyncInitializing;
        if (!isAsyncInitializing && sHandler.hasMessages(MSG_ON_RECEIVE)) {
            sHandler.removeMessages(MSG_ON_RECEIVE);
            for (MsgData msgData : sPendingMsg) {
                sHandler.obtainMessage(MSG_ON_RECEIVE, msgData).sendToTarget();
            }
        }
    }

    private static void handleReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null || intent.getExtras().isEmpty()) {
            Log.e(TAG, "handleReceive intent is null");
            return;
        }

        if (sCardDebugHost == null) {
            Log.e(TAG, "debug is disable");
            return;
        }

        if (CardClient.getInstance() == null) {
            Log.e(TAG, "cardClient hasn't init");
            return;
        }
        CardService cardService = CardServiceLoader.load(context);
        if (cardService == null) {
            Log.e(TAG, "cardClient init fail");
            return;
        }

        CardDebugController debugManager = null;
        try {
            debugManager = cardService.getCardDebugController();
        } catch (AbstractMethodError e) {
            Log.e(TAG, "getCardDebugController error");
        }

        if (debugManager != null) {
            debugManager.setCardDebugHost(sCardDebugHost);
            debugManager.handleDebugMessage(context, intent);
            return;
        }
        Log.e(TAG, "the core platform unsupport debug");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        synchronized (SdkCardDebugReceiver.class) {
            if (sIsAsyncInitializing) {
                MsgData msgData = new MsgData(context, intent);
                sPendingMsg.add(msgData);
                Message msg = sHandler.obtainMessage(MSG_ON_RECEIVE, msgData);
                sHandler.sendMessageDelayed(msg, 5000);
            } else {
                handleReceive(context, intent);
            }
        }
    }

    private static class MsgData {
        Context context;
        Intent intent;

        MsgData(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }
    }
}
