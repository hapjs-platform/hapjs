/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.Future;
import org.hapjs.common.executors.ScheduledExecutor;
import org.hapjs.common.resident.ResidentManager;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.features.audio.service.AudioService;
import org.hapjs.features.audio.service.Playback;

public class AudioProxyImpl implements AudioProxy {

    private static final String TAG = "AudioProxyImpl";

    private static final long UPDATE_INTERNAL = 250;
    private static final long UPDATE_INITIAL_INTERVAL = 0;
    private final String mPkg;
    private final Context mContext;
    private final ServiceInfoCallback mServiceInfoCallback;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;
    private MediaControllerCompat.Callback mControllerCallback;
    private MediaBrowserCompat.ConnectionCallback mConnectionCallback;
    private PlaybackStateCompat mLastPlaybackState;
    private int mTargetPlaybackState = Playback.STATE_IDLE;
    private Uri mCurrentUri;
    private Uri mTargetUri;
    private boolean mPlayWhenServiceStart = false;
    private boolean mSeekWhilePlaying = false;
    private Future mScheduleFuture;
    private OnPlayListener mOnPlayListener;
    private OnPauseListener mOnPauseListener;
    private OnLoadedDataListener mOnLoadedDataListener;
    private OnEndedListener mOnEndedListener;
    private OnDurationChangeListener mOnDurationChangeListener;
    private OnErrorListener mOnErrorListener;
    private OnTimeUpdateListener mOnTimeUpdateListener;
    private OnStopListener mOnStopListener;
    private OnPreviousListener mOnPreviousListener;
    private OnNextListener mOnNextListener;
    private int mDuration = -1;
    private boolean mLoop;
    private boolean mMuted;
    private float mVolume = -1;
    private int mMaxVolume = -1;
    private boolean mAutoPlay;
    private boolean mNotificationEnabled = true;
    private int mStreamType = AudioManager.STREAM_MUSIC;
    private float mCurrentTime;
    private final Runnable mTimeUpdateTask =
            new Runnable() {
                @Override
                public void run() {
                    updateCurrentTimeIfNeeded();
                    updateTimeListenerIfNeeded();
                }
            };
    private boolean mTimeUpdating = false;
    private String mTitle;
    private String mArtist;
    private Uri mCoverUri;
    private ResidentManager mResidentManager;
    private Audio mAudio;

    public AudioProxyImpl(Context context, String pkg, Audio audio,
                          ResidentManager residentManager) {
        this(context, pkg, null, audio, residentManager);
    }

    public AudioProxyImpl(
            Context context,
            String pkg,
            ServiceInfoCallback serviceInfoCallback,
            Audio audio,
            ResidentManager residentManager) {
        mContext = context;
        mPkg = pkg;
        ServiceInfoCallback l = serviceInfoCallback;
        if (l == null) {
            l = new SimpleServiceInfoCallback();
        }
        mServiceInfoCallback = l;
        this.mAudio = audio;
        this.mResidentManager = residentManager;
    }

    @Override
    public void play() {
        final Uri src = mTargetUri;
        if (src == null) {
            return;
        }
        if (mTargetPlaybackState != Playback.STATE_PLAYING) {
            mTargetPlaybackState = Playback.STATE_PLAYING;
            boolean connected = isAudioServiceConnected();
            if (!connected) {
                mPlayWhenServiceStart = true;
                connectAudioService();
            } else {
                playInternal();
            }
        }
    }

    private void playInternal() {
        final MediaControllerCompat controller = mMediaController;
        final Uri src = mCurrentUri = mTargetUri;
        if (controller == null || src == null) {
            return;
        }
        resetCurrentTime();
        MediaControllerCompat.TransportControls transportControls = getTransportControls();
        if (transportControls == null) {
            Log.e(TAG, "playInternal: transportControls is null");
            return;
        }
        transportControls.playFromUri(src, null);
        if (mOnPlayListener != null) {
            mOnPlayListener.onPlay();
        }
    }

    @Override
    public void pause() {
        final Uri src = mCurrentUri;
        if (src == null) {
            return;
        }
        if (mTargetPlaybackState != Playback.STATE_PAUSED) {
            mTargetPlaybackState = Playback.STATE_PAUSED;
            Bundle params = new Bundle();
            params.putParcelable(AudioService.ACTION_ARGUMENT_CURRENT_ITEM, src);
            sendCustomAction(AudioService.ACTION_PAUSE_ITEM, params);
            if (mOnPauseListener != null) {
                mOnPauseListener.onPause();
            }
        }
    }

    @Override
    public void stop(boolean isRemoveNotification) {
        final Uri src = mCurrentUri;
        stopTimeUpdate();
        // reset
        mCurrentTime = 0;
        if (src == null || mMediaController == null) {
            return;
        }
        if (mTargetPlaybackState != Playback.STATE_IDLE) {
            mTargetPlaybackState = Playback.STATE_IDLE;
            Bundle params = new Bundle();
            params.putParcelable(AudioService.ACTION_ARGUMENT_CURRENT_ITEM, src);
            params.putBoolean(AudioService.ACTION_ARGUMENT_IS_REMOVE_NOTIFICATION,
                    isRemoveNotification);
            sendCustomAction(AudioService.ACTION_STOP_ITEM, params);
            if (mOnStopListener != null) {
                mOnStopListener.onStop();
            }
        }
    }

    @Override
    public float getVolume() {
        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (mMaxVolume == -1) {
            mMaxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        float systemVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        float volume;
        // avoid precision issue
        if (FloatUtil.floatsEqual(Math.round(mVolume * mMaxVolume), systemVolume)) {
            volume = mVolume;
        } else {
            volume = systemVolume / mMaxVolume;
        }
        return volume;
    }

    @Override
    public void setVolume(float volume) {
        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (mMaxVolume == -1) {
            mMaxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        mVolume = volume;
        int mappedVolume = Math.round(volume * mMaxVolume);
        mappedVolume = Math.min(mMaxVolume, mappedVolume);
        mappedVolume = Math.max(0, mappedVolume);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, mappedVolume,
                AudioManager.FLAG_PLAY_SOUND);
    }

    @Override
    public boolean getLoop() {
        return mLoop;
    }

    @Override
    public void setLoop(boolean loop) {
        mLoop = loop;
    }

    private void setMutedInternal(boolean muted, boolean forceUpdate) {
        if (muted == mMuted && !forceUpdate) {
            return;
        }
        float volume = muted ? 0f : 1.0f;
        Bundle params = new Bundle();
        params.putFloat(AudioService.ACTION_ARGUMENT_SET_VOLUME, volume);
        sendCustomAction(AudioService.ACTION_SET_VOLUME, params);
        mMuted = muted;
    }

    @Override
    public boolean getMuted() {
        return mMuted;
    }

    @Override
    public void setMuted(boolean muted) {
        setMutedInternal(muted, false);
    }

    @Override
    public boolean getAutoPlay() {
        return mAutoPlay;
    }

    @Override
    public void setAutoPlay(boolean autoplay) {
        if (!mAutoPlay && autoplay) {
            play();
        }
        mAutoPlay = autoplay;
    }

    private void setNotificationEnabledInternal(boolean enabled, boolean forceUpdate) {
        if (enabled == mNotificationEnabled && !forceUpdate) {
            return;
        }
        Bundle params = new Bundle();
        params.putBoolean(AudioService.ACTION_ARGUMENT_SET_NOTIFICATION_ENABLED, enabled);
        sendCustomAction(AudioService.ACTION_SET_NOTIFICATION_ENABLED, params);
        mNotificationEnabled = enabled;
    }

    private void setStreamTypeInternal(int streamType, boolean forceUpdate) {
        if (streamType == mStreamType && !forceUpdate) {
            return;
        }

        Bundle params = new Bundle();
        params.putInt(AudioService.ACTION_ARGUMENT_SET_STREAM_TYPE, streamType);
        sendCustomAction(AudioService.ACTION_SET_STREAM_TYPE, params);

        mStreamType = streamType;
    }

    @Override
    public float getCurrentTime() {
        if (!mTimeUpdating) {
            updateCurrentTimeIfNeeded();
        }
        if (mLastPlaybackState != null) {
            Log.d(
                    TAG,
                    "getCurrentTime="
                            + mCurrentTime
                            + " Duration="
                            + mDuration
                            + " State="
                            + mLastPlaybackState.getState());
        }
        return mCurrentTime;
    }

    @Override
    public void setCurrentTime(float millisecond) {
        stopTimeUpdate();
        mCurrentTime = millisecond;
        if (mMediaController != null) {
            MediaControllerCompat.TransportControls transportControls = getTransportControls();
            if (transportControls != null) {
                transportControls.seekTo((long) millisecond);
            } else {
                Log.e(TAG, "setCurrentTime: transportControls is null");
            }
        } else {
            mSeekWhilePlaying = true;
        }
    }

    @Override
    public float getDuration() {
        return mDuration;
    }

    @Override
    public boolean isNotificationEnabled() {
        return mNotificationEnabled;
    }

    @Override
    public void setNotificationEnabled(boolean enabled) {
        setNotificationEnabledInternal(enabled, false);
    }

    @Override
    public int getStreamType() {
        return mStreamType;
    }

    @Override
    public void setStreamType(int streamType) {
        setStreamTypeInternal(streamType, false);
    }

    @Override
    public String getPackage() {
        return mPkg;
    }

    public void setTitleInternal(String title, boolean forceUpdate) {
        if (title != null && title.equals(mTitle) && !forceUpdate) {
            return;
        }
        mTitle = title;
        Bundle params = new Bundle();
        params.putString(AudioService.ACTION_ARGUMENT_SET_TITLE, title);
        sendCustomAction(AudioService.ACTION_SET_TITLE, params);
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public void setTitle(String title) {
        setTitleInternal(title, false);
    }

    public void setArtistInternal(String artist, boolean forceUpdate) {
        if (artist != null && artist.equals(mArtist) && !forceUpdate) {
            return;
        }
        mArtist = artist;
        Bundle params = new Bundle();
        params.putString(AudioService.ACTION_ARGUMENT_SET_ARTIST, artist);
        sendCustomAction(AudioService.ACTION_SET_ARTIST, params);
    }

    @Override
    public String getArtist() {
        return mArtist;
    }

    @Override
    public void setArtist(String artist) {
        setArtistInternal(artist, false);
    }

    public void setCoverInternal(Uri coverUri, boolean forceUpdate) {
        if (coverUri != null && coverUri.equals(mCoverUri) && !forceUpdate) {
            return;
        }
        mCoverUri = coverUri;
        Bundle params = new Bundle();
        params.putString(
                AudioService.ACTION_ARGUMENT_SET_COVER,
                (coverUri == null) ? null : coverUri.toString());
        sendCustomAction(AudioService.ACTION_SET_COVER, params);
    }

    @Override
    public String getCover() {
        return mCoverUri != null ? mCoverUri.toString() : "";
    }

    @Override
    public void setCover(Uri coverUri) {
        setCoverInternal(coverUri, false);
    }

    public String getTargetPlaybackState() {
        String state = null;
        if (Playback.STATE_PLAYING == mTargetPlaybackState) {
            state = "play";
        } else if (Playback.STATE_PAUSED == mTargetPlaybackState) {
            state = "pause";
        } else if (Playback.STATE_IDLE == mTargetPlaybackState) {
            state = "stop";
        }
        return state;
    }

    @Override
    public void reloadNotification() {
        sendCustomAction(AudioService.ACTION_RELOAD_NOTIFICATION, null);
    }

    private void connectAudioService() {
        if (mMediaBrowser == null) {
            if (mControllerCallback == null) {
                mControllerCallback = new SimpleControllerCallback();
            }
            if (mConnectionCallback == null) {
                mConnectionCallback = new SimpleConnectionCallback();
            }
            Bundle rootHits = new Bundle();
            rootHits.putString(AudioService.KEY_PACKAGE, mPkg);

            // must refresh.
            rootHits.putBoolean(AudioService.KEY_MUTED, mMuted);
            rootHits.putInt(AudioService.KEY_STREAM_TYPE, mStreamType);
            rootHits.putBoolean(AudioService.KEY_NOTIFICATION_ENABLE, mNotificationEnabled);
            rootHits.putString(AudioService.KEY_TITLE, mTitle);
            rootHits.putString(AudioService.KEY_ARTIST, mArtist);
            rootHits.putString(
                    AudioService.KEY_COVER_URI, (mCoverUri == null) ? null : mCoverUri.toString());

            mMediaBrowser =
                    new MediaBrowserCompat(
                            mContext,
                            mServiceInfoCallback.getServiceComponentName(mContext),
                            mConnectionCallback,
                            rootHits);
        }
        if (!mMediaBrowser.isConnected()) {
            try {
                mMediaBrowser.connect();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Connect Fail!");
            }
        }
    }

    private void disconnect() {
        stopTimeUpdate();
        if (mMediaController != null) {
            mMediaController = null;
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
            mMediaBrowser = null;
        }
    }

    private boolean isAudioServiceConnected() {
        return mMediaBrowser != null && mMediaBrowser.isConnected();
    }

    private boolean isSrcChange() {
        return mCurrentUri == null
                || (mTargetUri != null && !mTargetUri.equals(mCurrentUri))
                || (mTargetUri == null && mCurrentUri != null);
    }

    @Override
    public Uri getSrc() {
        return mTargetUri;
    }

    @Override
    public void setSrc(Uri uri) {
        // reset
        mDuration = -1;

        if (uri == null) {
            stop(true);
            mCurrentUri = null;
            mTargetUri = null;
            return;
        }
        stop(false);

        mTargetUri = uri;
        if (mAutoPlay) {
            play();
        }
    }

    private MediaControllerCompat.TransportControls getTransportControls() {
        if (mMediaController == null) {
            throw new IllegalStateException();
        }
        return mMediaController.getTransportControls();
    }

    private boolean sendCustomAction(String action, Bundle params) {
        if (mMediaController != null) {
            MediaControllerCompat.TransportControls transportControls = getTransportControls();
            if (transportControls != null) {
                transportControls.sendCustomAction(action, params);
                return true;
            } else {
                Log.e(TAG, "sendCustomAction: transportControls is null");
                return false;
            }
        }
        return false;
    }

    private void resetCurrentTime() {
        if (isSrcChange()) {
            stopTimeUpdate();
            mCurrentTime = 0;
        } else if (mLastPlaybackState != null
                && mLastPlaybackState.getState() == Playback.STATE_PLAYBACK_COMPLETED) {
            setCurrentTime(0);
        }
    }

    private void updatePlaybackState(PlaybackStateCompat currentState) {
        if (currentState == null) {
            return;
        }

        Bundle extras = currentState.getExtras();

        // update current time if current at paused state.
        if (currentState.getState() == Playback.STATE_PAUSED) {
            updateCurrentTimeIfNeeded();
        }

        // align currentTime when completed
        if (currentState.getState() == Playback.STATE_PLAYBACK_COMPLETED) {
            updateTimeListenerIfNeeded();
        }

        boolean hasTargetStateChanged = false;
        if (mTargetPlaybackState == currentState.getState()) {
            hasTargetStateChanged = true;
        }

        mLastPlaybackState = currentState;
        switch (currentState.getState()) {
            case Playback.STATE_ERROR: {
                mResidentManager.postUnregisterFeature(mAudio);
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError();
                }
                stopTimeUpdate();
                resetTargetState();
                break;
            }
            case Playback.STATE_IDLE: {
                mResidentManager.postUnregisterFeature(mAudio);
                if (!hasTargetStateChanged && mOnStopListener != null) {
                    mOnStopListener.onStop();
                }
                stopTimeUpdate();
                resetTargetState();
                break;
            }
            case Playback.STATE_PREPARING: {
                // do nothing.
                break;
            }
            case Playback.STATE_PREPARED: {
                // do nothing.
                break;
            }
            case Playback.STATE_PLAYING: {
                mResidentManager.postRegisterFeature(mAudio);
                if (!hasTargetStateChanged && mOnPlayListener != null) {
                    mOnPlayListener.onPlay();
                }
                if (mOnTimeUpdateListener != null) {
                    scheduleTimeUpdate();
                }
                mTargetPlaybackState = Playback.STATE_PLAYING;
                break;
            }
            case Playback.STATE_BUFFERING: {
                stopTimeUpdate();
                break;
            }
            case Playback.STATE_PAUSED: {
                mResidentManager.postUnregisterFeature(mAudio);
                if (!hasTargetStateChanged
                        && mOnPauseListener != null
                        && extras != null
                        && extras.getBoolean(Playback.KEY_EXTRA_NOTIFY, true)) {
                    mOnPauseListener.onPause();
                }
                stopTimeUpdate();
                mTargetPlaybackState = Playback.STATE_PAUSED;
                break;
            }
            case Playback.STATE_PLAYBACK_COMPLETED: {
                mResidentManager.postUnregisterFeature(mAudio);
                if (mOnEndedListener != null) {
                    mOnEndedListener.onEnded();
                }
                stopTimeUpdate();
                resetTargetState();
                if (mLoop) {
                    play();
                }
                break;
            }
            default: {
                Log.d(TAG, "Unhandled state " + currentState);
            }
        }
    }

    private void resetTargetState() {
        mTargetPlaybackState = Playback.STATE_IDLE;
    }

    private void scheduleTimeUpdate() {
        stopTimeUpdate();
        mTimeUpdating = true;
        mScheduleFuture =
                ExecutorHolder.INSTANCE.scheduleAtFixedRate(
                        mTimeUpdateTask, UPDATE_INITIAL_INTERVAL, UPDATE_INTERNAL,
                        TimeUnit.MILLISECONDS);
    }

    private void updateCurrentTimeIfNeeded() {
        if (mLastPlaybackState == null) {
            return;
        }
        float currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() == Playback.STATE_PLAYING) {
            long timeDelta =
                    SystemClock.elapsedRealtime() - mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
            currentPosition = mDuration != -1 ? Math.min(currentPosition, mDuration) : mDuration;
            mCurrentTime = currentPosition;
        }
        if (mLastPlaybackState.getState() == Playback.STATE_PLAYBACK_COMPLETED) {
            currentPosition = mDuration;
            mCurrentTime = currentPosition;
        }
    }

    private void updateTimeListenerIfNeeded() {
        if (mLastPlaybackState != null && mLastPlaybackState.getState() == Playback.STATE_PLAYING) {
            if (mOnTimeUpdateListener != null) {
                mOnTimeUpdateListener.onTimeUpdateListener();
            }
        }
    }

    private void stopTimeUpdate() {
        mTimeUpdating = false;
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(true);
        }
    }

    @Override
    public void setOnPlayListener(OnPlayListener l) {
        mOnPlayListener = l;
    }

    @Override
    public void setOnPauseListener(OnPauseListener l) {
        mOnPauseListener = l;
    }

    @Override
    public void setOnLoadedDataListener(OnLoadedDataListener l) {
        mOnLoadedDataListener = l;
    }

    @Override
    public void setOnEndedListener(OnEndedListener l) {
        mOnEndedListener = l;
    }

    @Override
    public void setOnDurationChangeListener(OnDurationChangeListener l) {
        mOnDurationChangeListener = l;
    }

    @Override
    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    @Override
    public void setOnTimeUpdateListener(OnTimeUpdateListener l) {
        mOnTimeUpdateListener = l;
        if (l == null) {
            stopTimeUpdate();
        } else if (mLastPlaybackState != null
                && mLastPlaybackState.getState() == Playback.STATE_PLAYING) {
            scheduleTimeUpdate();
        }
    }

    @Override
    public void setOnStopListener(OnStopListener l) {
        mOnStopListener = l;
    }

    @Override
    public void setOnPreviousListener(OnPreviousListener l) {
        mOnPreviousListener = l;
    }

    @Override
    public void setOnNextListener(OnNextListener l) {
        mOnNextListener = l;
    }

    private void resetNotification() {
        this.mTitle = "";
        this.mArtist = "";
        this.mCoverUri = null;

        setCoverInternal(null, true);
        setTitleInternal(null, true);
        setArtistInternal(null, true);
    }

    private static class ExecutorHolder {
        private static final ScheduledExecutor INSTANCE = Executors.createSingleThreadExecutor();
    }

    public class SimpleConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        @Override
        public void onConnected() {
            try {
                mMediaController =
                        new MediaControllerCompat(mContext, mMediaBrowser.getSessionToken());
                mMediaController
                        .registerCallback(mControllerCallback, new Handler(Looper.getMainLooper()));

                if (mPlayWhenServiceStart) {
                    playInternal();
                    mPlayWhenServiceStart = false;
                }
                if (mSeekWhilePlaying) {
                    setCurrentTime(mCurrentTime);
                    mSeekWhilePlaying = false;
                }
            } catch (RemoteException e) {
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onConnectionSuspended() {
            disconnect();
        }

        @Override
        public void onConnectionFailed() {
            resetTargetState();
            if (mOnErrorListener != null) {
                mOnErrorListener.onError();
            }
        }
    }

    public class SimpleControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            int duration = 0;
            boolean notify = true;
            if (metadata != null) {
                duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                notify = (int) metadata.getLong(Playback.KEY_META_NOTIFY) == 1;
            }
            mDuration = duration;
            if (mOnDurationChangeListener != null && notify) {
                mOnDurationChangeListener.onDurationChange(duration);
            }
            if (mOnLoadedDataListener != null && notify) {
                mOnLoadedDataListener.onLoadedData();
            }
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            switch (event) {
                case AudioService.ACTION_PREVIOUS_ITEM: {
                    if (mOnPreviousListener != null) {
                        resetNotification();
                        mOnPreviousListener.onPrevious();
                    }
                    break;
                }
                case AudioService.ACTION_NEXT_ITEM: {
                    if (mOnNextListener != null) {
                        resetNotification();
                        mOnNextListener.onNext();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    public class SimpleServiceInfoCallback implements ServiceInfoCallback {

        @Override
        public ComponentName getServiceComponentName(Context context) {
            return new ComponentName(context, AudioService.class);
        }
    }
}
