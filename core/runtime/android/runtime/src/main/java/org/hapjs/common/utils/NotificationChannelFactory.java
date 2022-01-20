/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import org.hapjs.runtime.R;

/**
 * we should put all notification channels here in order to manage them conveniently
 */
public class NotificationChannelFactory {

    public static final String ID_CHANNEL_APP =
            "channel.system.notification"; // quickapp notification channel
    public static final String ID_CHANNEL_AUDIO = "channel.system.audio"; // quickapp audio channel
    public static final String ID_CHANNEL_RESIDENT =
            "channel.system.resident"; // quickapp resident channel
    public static final String ID_CHANNEL_SERVICE =
            "channel.platform.service"; // quickapp platform channel

    public static void createAppChannel(Context context) {
        create(
                context,
                ID_CHANNEL_APP,
                context.getString(R.string.features_notification_channel_default),
                NotificationManager.IMPORTANCE_DEFAULT);
    }

    public static void createAudioChannel(Context context) {
        create(
                context,
                ID_CHANNEL_AUDIO,
                context.getString(R.string.features_notification_channel_audio),
                NotificationManager.IMPORTANCE_LOW);
    }

    public static void createServiceChannel(Context context) {
        create(
                context,
                ID_CHANNEL_SERVICE,
                context.getString(R.string.platform_notification_channel_service),
                NotificationManager.IMPORTANCE_DEFAULT);
    }

    public static void createResidentChannel(Context context) {
        create(
                context,
                ID_CHANNEL_RESIDENT,
                context.getString(R.string.resident_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
    }

    protected static NotificationChannel create(
            Context context, String id, String name, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context == null) {
                return null;
            }
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(id);
            if (channel == null) {
                channel = new NotificationChannel(id, name, importance);
                manager.createNotificationChannel(channel);
            }
            return channel;
        }
        return null;
    }
}
