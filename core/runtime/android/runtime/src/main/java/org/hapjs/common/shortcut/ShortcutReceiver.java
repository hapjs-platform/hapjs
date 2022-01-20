/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.shortcut;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class ShortcutReceiver extends BroadcastReceiver {
    private static final String TAG = "ShortcutReceiver";
    private static final String EXTRA_REQUEST_ID = "request_id";

    static PendingIntent getPendingIntent(Context context, String requestId) {
        Intent resultIntent = new Intent(context, ShortcutReceiver.class);
        resultIntent.putExtra(EXTRA_REQUEST_ID, requestId);
        return PendingIntent.getBroadcast(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
            if (!TextUtils.isEmpty(requestId)) {
                ShortcutInstaller.getInstance().onInstallSuccess(requestId);
            }
        } catch (Exception e) {
            Log.e(TAG, "onReceive get error", e);
        }
    }
}
