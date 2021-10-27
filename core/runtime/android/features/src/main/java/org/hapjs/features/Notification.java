/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.IconUtils;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.common.utils.NotificationChannelFactory;
import org.hapjs.logging.Source;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.system.SysOpProvider;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Notification.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = Notification.ACTION_SHOW, mode = FeatureExtension.Mode.SYNC)
        })
public class Notification extends FeatureExtension {
    protected static final String TAG = "Notification";
    protected static final String FEATURE_NAME = "system.notification";
    protected static final String ACTION_SHOW = "show";

    protected static final String PARAM_CONTENT_TITLE = "contentTitle";
    protected static final String PARAM_CONTENT_TEXT = "contentText";
    protected static final String PARAM_CLICK_ACTION = "clickAction";
    protected static final String PARAM_URI = "uri";

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        if (ACTION_SHOW.equals(request.getAction())) {
            Executors.io()
                    .execute(
                            () -> {
                                try {
                                    show(request);
                                } catch (JSONException e) {
                                    Log.e(TAG, "Fail to show notification", e);
                                }
                            });
        }
        return Response.SUCCESS;
    }

    private void show(Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        String pkg = request.getApplicationContext().getPackage();
        // 检查是否允许通知
        SysOpProvider provider =
                (SysOpProvider) ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        if (!provider.isNotificationEnabled(activity, pkg)) {
            Log.i(TAG, "notification is not allowed by user");
            return;
        }
        showNotification(request);
    }

    private void showNotification(Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        ApplicationContext application = request.getApplicationContext();

        JSONObject params = new JSONObject(request.getRawParams());
        String contentTitle = params.optString(PARAM_CONTENT_TITLE);
        String contentText = params.optString(PARAM_CONTENT_TEXT);
        String actionUri = null;
        JSONObject jsonAction = params.optJSONObject(PARAM_CLICK_ACTION);
        if (jsonAction != null) {
            actionUri = jsonAction.optString(PARAM_URI);
        }
        Uri iconUri = application.getIcon();
        Bitmap icon = IconUtils.getRoundIconBitmap(activity, iconUri);
        PendingIntent pendingIntent =
                getClickPendingIntent(activity, application.getPackage(), actionUri);

        android.app.Notification notification =
                createNotification(activity, application, contentTitle, contentText, icon,
                        pendingIntent);
        NotificationManager nm =
                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannelFactory.createAppChannel(activity);
        nm.notify(application.getPackage(), 0, notification);
    }

    protected android.app.Notification createNotification(
            Activity activity,
            ApplicationContext application,
            String contentTitle,
            String contentText,
            Bitmap icon,
            PendingIntent pendingIntent) {
        android.app.Notification.Builder b = new android.app.Notification.Builder(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b.setChannelId(NotificationChannelFactory.ID_CHANNEL_APP);
        }
        if (!TextUtils.isEmpty(contentTitle)) {
            b.setContentTitle(contentTitle);
        }
        if (!TextUtils.isEmpty(contentText)) {
            b.setContentText(contentText);
        }
        if (pendingIntent != null) {
            b.setContentIntent(pendingIntent);
        }
        if (icon != null) {
            b.setLargeIcon(icon);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && icon != null) {
            b.setSmallIcon(Icon.createWithBitmap(icon));
        } else {
            b.setSmallIcon(activity.getApplicationInfo().icon);
        }
        b.setAutoCancel(true);
        return b.build();
    }

    private PendingIntent getClickPendingIntent(Activity activity, String pkg, String actionUri) {
        Intent intent = new Intent(IntentUtils.getLaunchAction(activity));
        intent.putExtra(RuntimeActivity.EXTRA_APP, pkg);
        if (!TextUtils.isEmpty(actionUri)) {
            intent.putExtra(RuntimeActivity.EXTRA_PATH, actionUri);
        }
        String source = Source.currentSourceString();
        intent.putExtra(RuntimeActivity.EXTRA_SOURCE, source);
        intent.setPackage(activity.getPackageName());
        return PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
