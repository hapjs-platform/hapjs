/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.resident;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.common.utils.NotificationChannelFactory;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.R;
import org.hapjs.runtime.RuntimeActivity;

public class ResidentService extends Service {

    private static final String CHANNEL_ID_SUFFIX = "channel.system.resident";
    private static final String BIND_SERVICE_ACTION = ".action.RESIDENT";

    private ResidentBinder mBinder = new ResidentBinder();

    public static Intent getIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction(getBindAction(context));
        intent.setPackage(context.getPackageName());
        return intent;
    }

    private static String getBindAction(Context context) {
        StringBuilder serviceName = new StringBuilder(context.getPackageName());
        serviceName.append(BIND_SERVICE_ACTION).append(getProcessIndexId(context));
        return serviceName.toString();
    }

    private static int getProcessIndexId(Context context) {
        String processName = ProcessUtils.getCurrentProcessName();
        String processPrefix = context.getPackageName() + ":Launcher";
        if (processName.startsWith(processPrefix)) {
            return Integer.parseInt(processName.substring(processPrefix.length()));
        } else {
            return -1;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static class ResidentService0 extends ResidentService {
    }

    public static class ResidentService1 extends ResidentService {
    }

    public static class ResidentService2 extends ResidentService {
    }

    public static class ResidentService3 extends ResidentService {
    }

    public static class ResidentService4 extends ResidentService {
    }

    protected class ResidentBinder extends Binder {

        private RemoteViews mRemoteViews;
        private Notification mNotification;

        public void notifyUser(AppInfo appInfo, String notiDesc) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotification = buildNotification(ResidentService.this, appInfo, notiDesc);
            NotificationChannelFactory.createResidentChannel(ResidentService.this);
            // The notification is the same as music notification.
            startForeground(getNotificationId(appInfo.getPackage()), mNotification);
        }

        public void removeNotify(AppInfo appInfo) {
            stopForeground(true);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(getNotificationId(appInfo.getPackage()));
            mRemoteViews = null;
            mNotification = null;
        }

        public boolean updateNotificationDesc(AppInfo appInfo, String desc) {
            if (null != mNotification && null != mRemoteViews && !TextUtils.isEmpty(desc)) {
                mRemoteViews.setTextViewText(R.id.tv_resident_notify_features, desc);
                mRemoteViews.setViewVisibility(R.id.tv_resident_notify_features, View.VISIBLE);
                mNotification.contentView = mRemoteViews;
                startForeground(getNotificationId(appInfo.getPackage()), mNotification);
                return true;
            }
            return false;
        }

        protected Notification buildNotification(Context context, AppInfo appInfo,
                                                 String notiDesc) {
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, getChannelId());
            } else {
                builder = new Notification.Builder(context);
            }
            builder
                    .setShowWhen(false)
                    .setSmallIcon(R.drawable.notification_small)
                    .setContentTitle(appInfo.getName())
                    .setContentIntent(createContentIntent(context, appInfo.getPackage()));

            mRemoteViews = buildRemoteViews(ResidentService.this, appInfo, notiDesc);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setCustomContentView(mRemoteViews);
            } else {
                builder.setContent(mRemoteViews);
            }
            return builder.build();
        }

        protected PendingIntent createContentIntent(Context context, String pkg) {
            Intent startIntent = new Intent(IntentUtils.getLaunchAction(context));
            startIntent.putExtra(RuntimeActivity.EXTRA_APP, pkg);
            startIntent.putExtra(RuntimeActivity.EXTRA_SOURCE, Source.currentSourceString());
            startIntent.setPackage(context.getPackageName());

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            context, getRequestCode(pkg), startIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }

        protected String getChannelId() {
            return NotificationChannelFactory.ID_CHANNEL_RESIDENT;
        }

        protected int getNotificationId(String pkg) {
            return (pkg + ResidentService.class.getSimpleName()).hashCode();
        }

        protected int getRequestCode(String pkg) {
            return pkg.hashCode();
        }

        protected RemoteViews buildRemoteViews(Context context, AppInfo appInfo, String notiDesc) {
            RemoteViews remoteViews =
                    new RemoteViews(context.getPackageName(), R.layout.resident_notify_remotes);

            remoteViews.setTextViewText(
                    R.id.tv_resident_notify_tips,
                    getString(R.string.resident_notification_desc, appInfo.getName()));
            if (!TextUtils.isEmpty(notiDesc)) {
                remoteViews.setTextViewText(R.id.tv_resident_notify_features, notiDesc);
                remoteViews.setViewVisibility(R.id.tv_resident_notify_features, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.tv_resident_notify_features, View.GONE);
            }

            Intent closeIntent =
                    new Intent(appInfo.getPackage() + "." + ResidentManager.ACTION_CLOSE);
            closeIntent.setPackage(context.getPackageName());
            remoteViews.setOnClickPendingIntent(
                    R.id.iv_resident_close,
                    PendingIntent.getBroadcast(
                            context,
                            getRequestCode(appInfo.getPackage()),
                            closeIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT));
            return remoteViews;
        }
    }
}
