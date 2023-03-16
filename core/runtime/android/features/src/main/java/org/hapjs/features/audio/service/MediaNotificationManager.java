/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import java.lang.ref.WeakReference;
import org.hapjs.bridge.permission.HapCustomPermissions;
import org.hapjs.common.utils.IconUtils;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.common.utils.NotificationChannelFactory;
import org.hapjs.features.R;
import org.hapjs.logging.Source;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RuntimeActivity;

public class MediaNotificationManager extends BroadcastReceiver {

    public static final String ACTION_PAUSE = "audio.pause";
    public static final String ACTION_PLAY = "audio.play";
    public static final String ACTION_PREVIOUS = "audio.previous";
    public static final String ACTION_NEXT = "audio.next";
    public static final String ACTION_STOP = "audio.stop";
    private static final String TAG = "NotificationManager";
    protected final AudioService mService;
    protected final int mRequestCode;
    protected final String mPkg;
    private final NotificationManager mNotificationManager;
    protected String mTitle;
    protected String mArtist;
    protected String mCover;
    private RemoteViews mRemoteViews;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;
    private PlaybackStateCompat mPlaybackState;
    private MediaNotificationProvider mediaNotificationProvider;

    private PendingIntent mPlayIntent;
    private PendingIntent mPreviousIntent;
    private PendingIntent mNextIntent;
    private PendingIntent mPauseIntent;
    private PendingIntent mStopIntent;
    private int mNotificationColor;
    private boolean mStarted = false;
    private WeakReference<Bitmap> mIcon;
    private WeakReference<Bitmap> mAppIcon;
    private final MediaControllerCompat.Callback mCb =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    mPlaybackState = state;
                    if (mStarted
                            && state.getState() != Playback.STATE_IDLE
                            && mService.isNotificationEnabled()) {
                        Notification notification = buildNotification();
                        if (notification != null) {
                            mNotificationManager.notify(mRequestCode, notification);
                        }
                    }
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    if (mService.isNotificationEnabled()) {
                        Notification notification = buildNotification();
                        if (notification != null) {
                            mNotificationManager.notify(mRequestCode, notification);
                        }
                    }
                }

                @Override
                public void onSessionDestroyed() {
                    super.onSessionDestroyed();
                    try {
                        updateSessionToken();
                    } catch (RemoteException e) {
                        Log.e(TAG, "update session token error when session destroy!");
                    }
                }
            };

    public MediaNotificationManager(String pkg, AudioService service) throws RemoteException {
        mPkg = pkg;
        mService = service;
        mNotificationColor = Color.TRANSPARENT;
        mNotificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mRequestCode = mPkg.hashCode();
        updateSessionToken();
        String pkgName = mService.getPackageName();
        mediaNotificationProvider =
                (MediaNotificationProvider)
                        ProviderManager.getDefault().getProvider(MediaNotificationProvider.NAME);
        mPauseIntent =
                PendingIntent.getBroadcast(
                        mService,
                        mRequestCode,
                        new Intent(buildActionWithPkg(ACTION_PAUSE)).setPackage(pkgName),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent =
                PendingIntent.getBroadcast(
                        mService,
                        mRequestCode,
                        new Intent(buildActionWithPkg(ACTION_PLAY)).setPackage(pkgName),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent =
                PendingIntent.getBroadcast(
                        mService,
                        mRequestCode,
                        new Intent(buildActionWithPkg(ACTION_PREVIOUS)).setPackage(pkgName),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent =
                PendingIntent.getBroadcast(
                        mService,
                        mRequestCode,
                        new Intent(buildActionWithPkg(ACTION_NEXT)).setPackage(pkgName),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        mStopIntent =
                PendingIntent.getBroadcast(
                        mService,
                        mRequestCode,
                        new Intent(buildActionWithPkg(ACTION_STOP)).setPackage(pkgName),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        // Cancel current notification to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancel(mRequestCode);
    }

    public final void startNotification() {
        if (show()) {
            final Service context = mService;
            IntentFilter filter = new IntentFilter();
            filter.addAction(buildActionWithPkg(ACTION_PAUSE));
            filter.addAction(buildActionWithPkg(ACTION_PLAY));
            filter.addAction(buildActionWithPkg(ACTION_PREVIOUS));
            filter.addAction(buildActionWithPkg(ACTION_NEXT));
            filter.addAction(buildActionWithPkg(ACTION_STOP));
            configIntentFilterAction(filter);
            context.registerReceiver(
                    this, filter, HapCustomPermissions.getHapPermissionReceiveBroadcast(context),
                    null);
        }
    }

    public final void stopNotification() {
        if (hidden()) {
            try {
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "receiver is not registered,ignore!");
            }
        }
    }

    public boolean show() {
        if (!mStarted) {
            mPlaybackState = mController.getPlaybackState();
            final Notification notification = buildNotification();
            if (notification != null) {
                mController.registerCallback(mCb);
                boolean isShowed = false;
                if (mediaNotificationProvider != null) {
                    isShowed = mediaNotificationProvider.show(notification);
                }
                if (!isShowed && mService.isForeground()) {
                    mService.startForeground(mRequestCode, notification);
                }
                mStarted = true;
                return true;
            }
        }
        return false;
    }

    public boolean hidden() {
        if (mStarted) {
            mStarted = false;
            mController.unregisterCallback(mCb);
            boolean isHidden = false;
            if (mediaNotificationProvider != null) {
                isHidden = mediaNotificationProvider.hidden();
            }
            if (!isHidden) {
                if (mService.isForeground()) {
                    mService.stopForeground(true);
                }
                mNotificationManager.cancel(mRequestCode);
            }
            return true;
        }
        return false;
    }

    public boolean isShowing() {
        return mStarted;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        final String actionPrefix = mPkg + ".";
        if (action != null && action.startsWith(actionPrefix)) {
            action = action.substring(actionPrefix.length());
            switch (action) {
                case ACTION_PAUSE:
                    mTransportControls.pause();
                    break;
                case ACTION_PLAY:
                    mTransportControls.play();
                    break;
                case ACTION_PREVIOUS:
                    mTransportControls.sendCustomAction(AudioService.ACTION_PREVIOUS_ITEM, null);
                    break;
                case ACTION_NEXT:
                    mTransportControls.sendCustomAction(AudioService.ACTION_NEXT_ITEM, null);
                    break;
                case ACTION_STOP:
                    boolean needStop =
                            mediaNotificationProvider != null
                                    ? mediaNotificationProvider.isStopWhenRemoveNotification()
                                    : true;
                    Bundle params = new Bundle();
                    params.putBoolean(AudioService.ACTION_IS_STOP_WHEN_REMOVE_NOTIFICATION,
                            needStop);
                    mTransportControls
                            .sendCustomAction(AudioService.ACTION_STOP_FROM_NOTIFICATION, params);
                    break;
                default:
                    Log.e(TAG, "Unknown intent ignored. Action=" + action);
            }
        }
    }

    protected void configIntentFilterAction(IntentFilter filter) {
    }

    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null && freshToken != null
                || mSessionToken != null && !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            if (mSessionToken != null) {
                mController = new MediaControllerCompat(mService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (mStarted) {
                    mController.registerCallback(mCb);
                }
            }
        }
    }

    private Notification buildNotification() {
        boolean isViewConfiged = false;
        Notification notification = null;
        mRemoteViews = new RemoteViews(mService.getPackageName(), R.layout.audio_notification);

        if (mPlaybackState == null) {
            return null;
        }
        // Notification channels are only supported on Android O+.
        NotificationChannelFactory.createAudioChannel(mService);

        final String pkg = mPkg;
        final AudioService service = mService;

        if (mediaNotificationProvider != null) {
            mediaNotificationProvider.initNotification(
                    mNotificationManager,
                    mPkg,
                    mService,
                    mTransportControls,
                    mRemoteViews,
                    mRequestCode,
                    mStopIntent,
                    mPauseIntent,
                    mPlayIntent,
                    mPreviousIntent,
                    mNextIntent,
                    mTitle,
                    mArtist,
                    mCover);
            isViewConfiged = mediaNotificationProvider.configView(mPlaybackState);
            notification = mediaNotificationProvider.buildNotification();
        }

        if (!isViewConfiged) {
            configView();
        }

        if (notification == null) {
            final Notification.Builder notificationBuilder = new Notification.Builder(service);
            Bundle extras = new Bundle();
            extras.putString(AudioService.KEY_PACKAGE, pkg);

            notificationBuilder
                    .setExtras(extras)
                    .setShowWhen(false)
                    .setContent(mRemoteViews)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(createContentIntent(service, pkg, mRequestCode))
                    .setDeleteIntent(mStopIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder.setChannelId(NotificationChannelFactory.ID_CHANNEL_AUDIO);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setColor(mNotificationColor);
            }
            Bitmap icon = mIcon != null ? mIcon.get() : null;
            if (icon != null) {
                notificationBuilder.setLargeIcon(icon);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && icon != null) {
                notificationBuilder.setSmallIcon(Icon.createWithBitmap(icon));
            } else {
                notificationBuilder.setSmallIcon(service.getApplicationInfo().icon);
            }
            setNotificationPlaybackState(notificationBuilder);
            notification = notificationBuilder.build();
        }
        configExtraNotification(service, notification);
        return notification;
    }

    protected void configExtraNotification(Context context, Notification notification) {
    }

    protected PendingIntent createContentIntent(Context context, String pkg, int requestCode) {
        Intent startIntent = new Intent(IntentUtils.getLaunchAction(context));
        startIntent.putExtra(RuntimeActivity.EXTRA_APP, pkg);
        String source = Source.currentSourceString();
        startIntent.putExtra(RuntimeActivity.EXTRA_SOURCE, source);
        startIntent.setPackage(context.getPackageName());
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context, requestCode, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    private void configView() {
        // Play or pause button, depending on the current state.
        final String label;
        final int playIcon;
        final PendingIntent intent;
        if (isPlayingState()) {
            label = mService.getString(R.string.audio_playing_label);
            playIcon = R.drawable.ic_media_notification_pause;
            intent = mPauseIntent;
        } else {
            label = mService.getString(R.string.audio_paused_label);
            playIcon = R.drawable.ic_media_notification_play;
            intent = mPlayIntent;
        }

        if (!TextUtils.isEmpty(mCover)) {
            Uri iconUri = Uri.parse(mCover);
            if (iconUri != null) {
                Bitmap iconBitmap = IconUtils.getRoundIconBitmap(mService, iconUri);
                mIcon = new WeakReference<>(iconBitmap);
                if (mIcon != null && mIcon.get() != null) {
                    mRemoteViews.setImageViewBitmap(R.id.icon, mIcon.get());
                } else {
                    setAppIconToCover();
                }
            }
        } else {
            setAppIconToCover();
        }
        mRemoteViews.setImageViewResource(R.id.play, playIcon);

        if (TextUtils.isEmpty(mTitle) || TextUtils.isEmpty(mArtist)) {
            mRemoteViews.removeAllViews(R.id.textLinearLayout);
            RemoteViews titleRemoteViews =
                    new RemoteViews(mService.getPackageName(),
                            R.layout.audio_notification_text_play_state);
            titleRemoteViews.setTextViewText(R.id.play_state, label);
            mRemoteViews.addView(R.id.textLinearLayout, titleRemoteViews);
        } else {
            mRemoteViews.removeAllViews(R.id.textLinearLayout);
            RemoteViews titleRemoteViews =
                    new RemoteViews(mService.getPackageName(),
                            R.layout.audio_notification_text_title);
            RemoteViews artistRemoteViews =
                    new RemoteViews(mService.getPackageName(),
                            R.layout.audio_notification_text_artist);
            titleRemoteViews.setTextViewText(R.id.audio_title, mTitle);
            artistRemoteViews.setTextViewText(R.id.audio_artist, mArtist);
            mRemoteViews.addView(R.id.textLinearLayout, titleRemoteViews);
            mRemoteViews.addView(R.id.textLinearLayout, artistRemoteViews);
        }
        mRemoteViews.setOnClickPendingIntent(R.id.play, intent);
        mRemoteViews.setOnClickPendingIntent(R.id.previous, mPreviousIntent);
        mRemoteViews.setOnClickPendingIntent(R.id.next, mNextIntent);
    }

    private String buildActionWithPkg(String action) {
        StringBuilder builder = new StringBuilder(mPkg).append(".").append(action);
        return builder.toString();
    }

    private void setNotificationPlaybackState(Notification.Builder builder) {
        if (mPlaybackState == null || !mStarted) {
            mService.stopForeground(true);
            return;
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState.getState() == Playback.STATE_PLAYING);
    }

    protected void setTitle(String title) {
        this.mTitle = title;
    }

    protected void setArtist(String artist) {
        this.mArtist = artist;
    }

    protected void setCover(String cover) {
        this.mCover = cover;
    }

    private boolean isPlayingState() {
        return mPlaybackState != null
                && (mPlaybackState.getState() == Playback.STATE_PREPARED
                || mPlaybackState.getState() == Playback.STATE_PREPARING
                || mPlaybackState.getState() == Playback.STATE_PLAYING
                || mPlaybackState.getState() == Playback.STATE_BUFFERING);
    }

    private void setAppIconToCover() {
        if (mPkg != null && (mAppIcon == null || mAppIcon.get() == null)) {
            Uri mAppIconUri = HapEngine.getInstance(mPkg).getApplicationContext().getIcon();
            if (mAppIconUri != null) {
                Bitmap appIconBitmap = IconUtils.getIconBitmap(mService, mAppIconUri);
                mAppIcon = new WeakReference<>(appIconBitmap);
            }
        }
        if (mAppIcon != null && mAppIcon.get() != null) {
            mRemoteViews.setImageViewBitmap(R.id.icon, mAppIcon.get());
        }
    }
}
