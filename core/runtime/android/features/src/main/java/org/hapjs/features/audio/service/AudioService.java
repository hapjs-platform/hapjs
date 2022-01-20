/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio.service;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import java.lang.ref.WeakReference;
import java.util.List;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;

public class AudioService extends MediaBrowserServiceCompat
        implements Playback.PlaybackInfoListener {

    public static final String ACTION_ARGUMENT_CURRENT_ITEM = "ACTION_ARGUMENT_CURRENT_ITEM";
    public static final String ACTION_ARGUMENT_IS_REMOVE_NOTIFICATION =
            "ACTION_ARGUMENT_IS_REMOVE_NOTIFICATION";
    public static final String ACTION_ARGUMENT_SET_VOLUME = "ACTION_ARGUMENT_SET_VOLUME";
    public static final String ACTION_ARGUMENT_SET_NOTIFICATION_ENABLED =
            "ACTION_ARGUMENT_SET_NOTIFICATION_ENABLED";
    public static final String ACTION_ARGUMENT_SET_STREAM_TYPE = "ACTION_ARGUMENT_SET_STREAM_TYPE";
    public static final String ACTION_ARGUMENT_SET_TITLE = "ACTION_ARGUMENT_SET_TITLE";
    public static final String ACTION_ARGUMENT_SET_ARTIST = "ACTION_ARGUMENT_SET_ARTIST";
    public static final String ACTION_ARGUMENT_SET_COVER = "ACTION_ARGUMENT_SET_COVER";
    public static final String ACTION_STOP_ITEM = "ACTION_STOP_ITEM";
    public static final String ACTION_PAUSE_ITEM = "ACTION_PAUSE_ITEM";
    public static final String ACTION_STOP_FROM_NOTIFICATION = "ACTION_STOP_FROM_NOTIFICATION";
    public static final String ACTION_SET_VOLUME = "ACTION_SET_VOLUME";
    public static final String ACTION_SET_NOTIFICATION_ENABLED = "ACTION_SET_NOTIFICATION_ENABLED";
    public static final String ACTION_SET_STREAM_TYPE = "ACTION_SET_STREAM_TYPE";
    public static final String ACTION_SET_TITLE = "ACTION_SET_TITLE";
    public static final String ACTION_SET_ARTIST = "ACTION_SET_ARTIST";
    public static final String ACTION_SET_COVER = "ACTION_SET_COVER";
    public static final String ACTION_PREVIOUS_ITEM = "ACTION_PREVIOUS_ITEM";
    public static final String ACTION_NEXT_ITEM = "ACTION_NEXT_ITEM";
    public static final String ACTION_IS_STOP_WHEN_REMOVE_NOTIFICATION =
            "ACTION_IS_STOP_WHEN_REMOVE_NOTIFICATION";
    public static final String ACTION_RELOAD_NOTIFICATION = "ACTION_RELOAD_NOTIFICATION";
    public static final String KEY_PACKAGE = "PACKAGE";
    public static final String KEY_MUTED = "MUTED";
    public static final String KEY_STREAM_TYPE = "STREAM_TYPE";
    public static final String KEY_NOTIFICATION_ENABLE = "NOTIFICATION_ENABLE";
    public static final String KEY_TITLE = "TITLE";
    public static final String KEY_ARTIST = "ARTIST";
    public static final String KEY_COVER_URI = "COVER_URI";
    private static final String TAG = "AudioService";
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;
    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    protected String mPkg;
    protected String mTitle;
    protected String mArtist;
    protected String mCover;
    private MediaSessionCompat mSession;
    private Playback mPlayback;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaSessionCallback mCallback;
    private boolean mNotificationEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();
        // MediaButtonReceiver component can't be null in Android KK
        if (Build.VERSION.SDK_INT < 21) {
            mSession =
                    new MediaSessionCompat(
                            this,
                            "AudioService",
                            new ComponentName(this, MediaButtonReceiver.class.getName()),
                            null);
        } else {
            mSession = new MediaSessionCompat(this, "AudioService");
        }
        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());
        mPlayback = new MediaPlayerPlayback(getApplicationContext(), this);
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mMediaNotificationManager != null) {
            mMediaNotificationManager.stopNotification();
        }
        mPlayback.stop();
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mSession.release();
        super.onDestroy();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 Bundle rootHints) {
        if (rootHints != null) {
            mPkg = rootHints.getString(KEY_PACKAGE);

            mPlayback.setVolume(rootHints.getBoolean(KEY_MUTED) ? 0f : 1f);
            mPlayback.setStreamType(rootHints.getInt(KEY_STREAM_TYPE));
            setNotificationEnabled(rootHints.getBoolean(KEY_NOTIFICATION_ENABLE));
            if (mMediaNotificationManager != null) {
                mMediaNotificationManager.setTitle(rootHints.getString(KEY_TITLE));
                mMediaNotificationManager.setArtist(rootHints.getString(KEY_ARTIST));
                mMediaNotificationManager.setCover(rootHints.getString(KEY_COVER_URI));
            }
        }
        return new BrowserRoot(mPkg, null);
    }

    @Override
    public void onLoadChildren(
            @NonNull final String parentMediaId,
            @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
    }

    @Override
    public void onPlaybackStateChange(PlaybackStateCompat state) {
        mSession.setPlaybackState(state);
        switch (state.getState()) {
            case Playback.STATE_PLAYING:
                onNotificationRequired();
                onPlaybackPlaying();
                break;
            case Playback.STATE_IDLE:
                onPlaybackStop();
                break;
            default:
                break;
        }
    }

    private void onPlaybackPlaying() {
        mSession.setActive(true);
        mDelayedStopHandler.removeCallbacksAndMessages(null);
    }

    private void onPlaybackStop() {
        mSession.setActive(false);
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    private void onNotificationRequired() {
        if (isNotificationEnabled()) {
            if (mPkg != null && mMediaNotificationManager == null) {
                mMediaNotificationManager = createNotificationManager(mPkg, AudioService.this);
            }
            if (mMediaNotificationManager != null) {
                mMediaNotificationManager.startNotification();
            }
        }
    }

    protected boolean isNotificationEnabled() {
        SysOpProvider provider =
                (SysOpProvider) ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        return provider.isNotificationEnabled(this, mPkg) && mNotificationEnabled;
    }

    private void setNotificationEnabled(boolean enabled) {
        if (enabled == mNotificationEnabled
                && (mMediaNotificationManager != null
                && mMediaNotificationManager.isShowing() == enabled)) {
            return;
        }

        mNotificationEnabled = enabled;
        if (enabled) {
            if (mMediaNotificationManager == null || !mMediaNotificationManager.isShowing()) {
                onNotificationRequired();
            }
        } else {
            if (mMediaNotificationManager != null && mMediaNotificationManager.isShowing()) {
                mMediaNotificationManager.stopNotification();
            }
        }
    }

    protected boolean isForeground() {
        return true;
    }

    protected MediaNotificationManager createNotificationManager(String pkg, AudioService service) {
        try {
            return new MediaNotificationManager(pkg, service);
        } catch (RemoteException e) {
            Log.e(TAG, "create audio notification error!" + e);
        }
        return null;
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        mSession.setMetadata(metadata);
    }

    public Playback getPlayback() {
        return mPlayback;
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<AudioService> mWeakReference;

        private DelayedStopHandler(AudioService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioService service = mWeakReference.get();
            if (service != null && service.getPlayback() != null) {
                if (service.getPlayback().isPlaying()) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                Log.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
            }
        }
    }

    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        private Uri mCurrentUri;

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (extras == null) {
                return;
            }
            switch (action) {
                case ACTION_STOP_ITEM: {
                    Uri uri = extras.getParcelable(ACTION_ARGUMENT_CURRENT_ITEM);
                    boolean isRemoveNotification =
                            extras.getBoolean(ACTION_ARGUMENT_IS_REMOVE_NOTIFICATION);
                    if (uri != null && uri.equals(mCurrentUri)) {
                        onStop();
                    }
                    if (isRemoveNotification && mMediaNotificationManager != null) {
                        mMediaNotificationManager.stopNotification();
                    }
                    break;
                }
                case ACTION_PAUSE_ITEM: {
                    Uri uri = extras.getParcelable(ACTION_ARGUMENT_CURRENT_ITEM);
                    if (uri != null && uri.equals(mCurrentUri)) {
                        onPause();
                    }
                    break;
                }
                case ACTION_RELOAD_NOTIFICATION: {
                    if (mNotificationEnabled == true) {
                        setNotificationEnabled(true);
                    }
                    break;
                }
                case ACTION_STOP_FROM_NOTIFICATION: {
                    boolean isStop = extras.getBoolean(ACTION_IS_STOP_WHEN_REMOVE_NOTIFICATION);
                    if (mMediaNotificationManager != null) {
                        mMediaNotificationManager.stopNotification();
                    }
                    if (isStop) {
                        onPause();
                        onStop();
                    }
                    break;
                }
                case ACTION_SET_VOLUME: {
                    float volume = extras.getFloat(ACTION_ARGUMENT_SET_VOLUME);
                    mPlayback.setVolume(volume);
                    break;
                }
                case ACTION_SET_NOTIFICATION_ENABLED: {
                    boolean enabled = extras.getBoolean(ACTION_ARGUMENT_SET_NOTIFICATION_ENABLED);
                    setNotificationEnabled(enabled);
                    break;
                }
                case ACTION_SET_STREAM_TYPE: {
                    int streamType = extras.getInt(ACTION_ARGUMENT_SET_STREAM_TYPE);
                    mPlayback.setStreamType(streamType);
                    break;
                }
                case ACTION_SET_TITLE: {
                    mTitle = extras.getString(ACTION_ARGUMENT_SET_TITLE);
                    if (mMediaNotificationManager != null) {
                        mMediaNotificationManager.setTitle(mTitle);
                    }
                    break;
                }
                case ACTION_SET_ARTIST: {
                    mArtist = extras.getString(ACTION_ARGUMENT_SET_ARTIST);
                    if (mMediaNotificationManager != null) {
                        mMediaNotificationManager.setArtist(mArtist);
                    }
                    break;
                }
                case ACTION_SET_COVER: {
                    mCover = extras.getString(ACTION_ARGUMENT_SET_COVER);
                    if (mMediaNotificationManager != null) {
                        mMediaNotificationManager.setCover(mCover);
                    }
                    break;
                }
                case ACTION_PREVIOUS_ITEM: {
                    if (mSession != null) {
                        mSession.sendSessionEvent(ACTION_PREVIOUS_ITEM, null);
                    }
                    break;
                }
                case ACTION_NEXT_ITEM: {
                    if (mSession != null) {
                        mSession.sendSessionEvent(ACTION_NEXT_ITEM, null);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            mCurrentUri = uri;
            mSession.setActive(true);
            mPlayback.playFromMediaUri(uri);
        }

        @Override
        public void onPlay() {
            if (mCurrentUri != null) {
                Uri uri = mCurrentUri;
                mPlayback.playFromMediaUri(uri);
            }
        }

        @Override
        public void onPause() {
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            mPlayback.stop();
        }

        @Override
        public void onSeekTo(long pos) {
            mPlayback.seekTo(pos);
        }
    }
}
