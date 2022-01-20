/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.NonNull;
import org.hapjs.common.utils.UriUtils;

public abstract class Playback {

    // all possible internal states
    public static final int STATE_ERROR = PlaybackStateCompat.STATE_ERROR;
    public static final int STATE_IDLE = PlaybackStateCompat.STATE_NONE;
    public static final int STATE_PREPARING = PlaybackStateCompat.STATE_CONNECTING << 2;
    public static final int STATE_PREPARED = PlaybackStateCompat.STATE_CONNECTING << 3;
    public static final int STATE_BUFFERING = PlaybackStateCompat.STATE_BUFFERING;
    public static final int STATE_PLAYING = PlaybackStateCompat.STATE_PLAYING;
    public static final int STATE_PAUSED = PlaybackStateCompat.STATE_PAUSED;
    public static final int STATE_PLAYBACK_COMPLETED = PlaybackStateCompat.STATE_STOPPED;
    public static final String KEY_EXTRA_NOTIFY = "extra_notify";
    public static final String KEY_META_NOTIFY = "meta_notify";
    private static final float MEDIA_VOLUME_DEFAULT = 1.0f;
    private static final float MEDIA_VOLUME_DUCK = 0.2f;
    private static final IntentFilter AUDIO_NOISY_INTENT_FILTER =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final Context mApplicationContext;
    private final AudioManager mAudioManager;
    private final AudioFocusHelper mAudioFocusHelper;
    private final WifiManager.WifiLock mWifiLock;
    protected Uri mCurrentUri;
    private int mCurrentState;
    private int mTargetState;
    private boolean mAudioNoisyReceiverRegistered = false;
    private boolean mPlayOnAudioFocus = false;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private final BroadcastReceiver mAudioNoisyReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                        if (isPlaying()) {
                            pause();
                        }
                    }
                }
            };

    public Playback(@NonNull Context context) {
        mApplicationContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mApplicationContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusHelper = new AudioFocusHelper();
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.mWifiLock =
                ((WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "hybrid_audio_service_lock");
    }

    public final void prepare() {
        if (mAudioFocusHelper.requestAudioFocus()) {
            registerAudioNoisyReceiver();
        }
    }

    public final void play() {
        if (isInPlaybackState()
                && mCurrentState != STATE_PLAYING
                && mAudioFocusHelper.requestAudioFocus()) {
            registerAudioNoisyReceiver();
            onPlay();
            Uri uri = mCurrentUri;
            if (uri != null && UriUtils.isWebSchema(uri.getScheme())) {
                // If we are streaming from the internet, we want to hold a
                // Wifi lock, which prevents the Wifi radio from going to
                // sleep while the song is playing.
                mWifiLock.acquire();
            }
        }
        mTargetState = Playback.STATE_PLAYING;
    }

    public final void pause() {
        if (isPlaying()) {
            if (!mPlayOnAudioFocus) {
                mAudioFocusHelper.abandonAudioFocus();
            }
            unregisterAudioNoisyReceiver();
            onPause();
        }
        mTargetState = Playback.STATE_PAUSED;
    }

    public final void stop() {
        mAudioFocusHelper.abandonAudioFocus();
        unregisterAudioNoisyReceiver();
        onStop();
        mTargetState = Playback.STATE_IDLE;
    }

    public void release(boolean releasePlayer) {
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    public int getTargetState() {
        return mTargetState;
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(int state) {
        mCurrentState = state;
    }

    public boolean isInPlaybackState() {
        return (mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE
                && mCurrentState != STATE_PREPARING);
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mApplicationContext.registerReceiver(mAudioNoisyReceiver, AUDIO_NOISY_INTENT_FILTER);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mApplicationContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    public void playFromMediaUri(Uri uri) {
        mTargetState = Playback.STATE_PLAYING;
    }

    public abstract boolean isPlaying();

    protected abstract void onPlay();

    protected abstract void onPause();

    protected abstract void onStop();

    public abstract void seekTo(long position);

    public abstract void setVolume(float volume);

    public abstract void setStreamType(int streamType);

    public interface PlaybackInfoListener {
        void onPlaybackStateChange(PlaybackStateCompat state);

        void onMetadataChanged(MediaMetadataCompat metadata);
    }

    private final class AudioFocusHelper {

        private boolean requestAudioFocus() {
            if (mOnAudioFocusChangeListener == null) {
                mOnAudioFocusChangeListener =
                        new AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {
                                switch (focusChange) {
                                    case AudioManager.AUDIOFOCUS_GAIN:
                                        if (mPlayOnAudioFocus && !isPlaying()) {
                                            play();
                                        } else if (isPlaying()) {
                                            setVolume(MEDIA_VOLUME_DEFAULT);
                                        }
                                        mPlayOnAudioFocus = false;
                                        break;
                                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                                        setVolume(MEDIA_VOLUME_DUCK);
                                        break;
                                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                                        if (isPlaying()) {
                                            mPlayOnAudioFocus = true;
                                            pause();
                                        }
                                        break;
                                    case AudioManager.AUDIOFOCUS_LOSS:
                                        mAudioManager.abandonAudioFocus(this);
                                        mPlayOnAudioFocus = false;
                                        pause();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        };
            }
            final int result =
                    mAudioManager.requestAudioFocus(
                            mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        private void abandonAudioFocus() {
            if (mOnAudioFocusChangeListener != null) {
                mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
            }
        }
    }
}
