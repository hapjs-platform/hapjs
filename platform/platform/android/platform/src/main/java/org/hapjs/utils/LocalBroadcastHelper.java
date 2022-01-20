/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.hapjs.runtime.Runtime;

public class LocalBroadcastHelper {
    public static final String ACTION_BROADCAST_RUNNING_APP = "local.action.BROADCAST_RUNNING_APP";
    public static final String EXTRA_APP_ID = "app";
    public static final String EXTRA_COMPONENT = "component";
    private Context mContext;
    private LocalBroadcastManager mLocalBroadcastManager;

    private LocalBroadcastHelper(Context context) {
        mContext = context;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public static LocalBroadcastHelper getInstance() {
        return Holder.INSTANCE;
    }

    public void broadcastRunningApp(String pkg, Class<? extends Activity> activityClass) {
        Intent intent = new Intent(ACTION_BROADCAST_RUNNING_APP);
        intent.putExtra(EXTRA_APP_ID, pkg);
        intent.putExtra(EXTRA_COMPONENT, new ComponentName(mContext, activityClass));
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public BroadcastReceiver registerRunningAppReceiver(final RunningAppListener listener) {
        IntentFilter filter = new IntentFilter(LocalBroadcastHelper.ACTION_BROADCAST_RUNNING_APP);
        BroadcastReceiver receiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String pkg = intent.getStringExtra(LocalBroadcastHelper.EXTRA_APP_ID);
                        ComponentName component =
                                intent.getParcelableExtra(LocalBroadcastHelper.EXTRA_COMPONENT);
                        listener.onRunningAppReceive(pkg, component);
                    }
                };
        registerReceiver(receiver, filter);
        return receiver;
    }

    public void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    public interface RunningAppListener {
        void onRunningAppReceive(String pkg, ComponentName component);
    }

    private static class Holder {
        static LocalBroadcastHelper INSTANCE =
                new LocalBroadcastHelper(Runtime.getInstance().getContext());
    }
}
