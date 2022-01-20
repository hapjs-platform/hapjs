/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.shortcut;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import org.hapjs.common.utils.NotificationChannelFactory;
import org.hapjs.common.utils.ShortcutManager;
import org.hapjs.logging.Source;
import org.hapjs.runtime.R;

public class ShortcutService extends Service {
    private static final String TAG = "ShortcutService";
    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_APP_ID = "app_id";
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_PARAMS = "params";
    private static final String EXTRA_APP_NAME = "app_name";
    private static final String EXTRA_APP_ICON = "app_icon";
    private static final String EXTRA_SOURCE = "source";

    static void install(
            Context context,
            String requestId,
            String appId,
            String path,
            String params,
            String appName,
            Bitmap icon,
            Source source) {
        Intent intent = new Intent(context, ShortcutService.class);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        intent.putExtra(EXTRA_APP_ID, appId);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_PARAMS, params);
        intent.putExtra(EXTRA_APP_NAME, appName);
        intent.putExtra(EXTRA_APP_ICON, icon);
        intent.putExtra(EXTRA_SOURCE, source.toJson().toString());
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= 26) {
            if (intent != null) {
                String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
                String appId = intent.getStringExtra(EXTRA_APP_ID);
                String path = intent.getStringExtra(EXTRA_PATH);
                String params = intent.getStringExtra(EXTRA_PARAMS);
                String appName = intent.getStringExtra(EXTRA_APP_NAME);
                Bitmap icon = intent.getParcelableExtra(EXTRA_APP_ICON);
                Source source = Source.fromJson(intent.getStringExtra(EXTRA_SOURCE));

                Notification n =
                        new Notification.Builder(getApplicationContext())
                                .setChannelId(NotificationChannelFactory.ID_CHANNEL_SERVICE)
                                .setContentTitle(
                                        getString(R.string.notification_creating_shortcut_for,
                                                appName))
                                .setSmallIcon(getApplicationInfo().icon)
                                .build();
                NotificationChannelFactory.createServiceChannel(this);
                startForeground(1, n);

                PendingIntent pendingIntent = ShortcutReceiver.getPendingIntent(this, requestId);
                boolean success =
                        ShortcutManager.installAboveOreo(
                                getApplicationContext(),
                                appId,
                                path,
                                params,
                                appName,
                                icon,
                                source,
                                pendingIntent.getIntentSender());
                stopForeground(true);
                Log.d(TAG, "create shortcut for " + appId + (success ? " success" : " failed"));
            } else {
                Log.w(TAG, "onStartCommand: intent is null");
            }
        } else {
            Log.w(TAG, "Should not start ShortcutService on android sdk level "
                    + Build.VERSION.SDK_INT);
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }
}
