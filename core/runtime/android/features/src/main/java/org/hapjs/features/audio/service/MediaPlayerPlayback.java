/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.io.IOException;
import org.hapjs.common.executors.Executors;

public final class MediaPlayerPlayback extends Playback {
    private static final String TAG = "MediaPlayerPlayback";

    private static final String KEY_ERROR_WHAT = "what";
    private static final String KEY_ERROR_EXT = "ext";
    private final Context mContext;
    private MediaPlayer mMediaPlayer;
    private PlaybackInfoListener mPlaybackInfoListener;
    private boolean mCurrentMediaPlayedToCompletion;
    private int mCurrentBufferPercentage;
    private int mAudioSession;
    private int mSeekWhileNotPlaying = -1;
    private int mStreamType = AudioManager.STREAM_MUSIC;
    private int mCurrentStreamType;
    private boolean mShouldNotify = true;
    private MediaPlayer.OnPreparedListener mPreparedListener =
            new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    if (getTargetState() == STATE_PLAYING) {
                        setNewState(STATE_PREPARED);
                        mMediaPlayer.start();
                        if (mSeekWhileNotPlaying != -1) {
                            mMediaPlayer.seekTo(mSeekWhileNotPlaying);
                            mSeekWhileNotPlaying = -1;
                        }
                        MediaMetadataCompat metadata =
                                new MediaMetadataCompat.Builder()
                                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                                mMediaPlayer.getDuration())
                                        .putLong(KEY_META_NOTIFY, mShouldNotify ? 1 : 0)
                                        .build();
                        mPlaybackInfoListener.onMetadataChanged(metadata);
                        setNewState(STATE_PLAYING);
                        mShouldNotify = true;
                    }
                }
            };
    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    setNewState(STATE_PLAYBACK_COMPLETED);
                }
            };
    private MediaPlayer.OnErrorListener mErrorListener =
            new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int ext) {
                    Log.e(TAG, "onError" + " what:" + what + " ext:" + ext);
                    release(true);
                    Bundle bundle = new Bundle();
                    bundle.putInt(KEY_ERROR_WHAT, what);
                    bundle.putInt(KEY_ERROR_EXT, ext);
                    setNewState(STATE_ERROR, bundle);
                    return true;
                }
            };
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new MediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                    if (STATE_PREPARING != getCurrentState()
                            && mMediaPlayer != null
                            && mMediaPlayer.getDuration() != 0) {
                        if (percent
                                < (mMediaPlayer.getCurrentPosition()
                                / (float) mMediaPlayer.getDuration())) {
                            setNewState(STATE_BUFFERING);
                        }
                    }
                }
            };

    public MediaPlayerPlayback(Context context, PlaybackInfoListener listener) {
        super(context);
        mContext = context.getApplicationContext();
        mPlaybackInfoListener = listener;
    }

    @Override
    public void playFromMediaUri(Uri uri) {
        super.playFromMediaUri(uri);
        if (uri == null) {
            return;
        }
        boolean mediaChanged =
                (mCurrentUri == null
                        || !mCurrentUri.equals(uri)
                        || (getCurrentState() == STATE_PLAYBACK_COMPLETED
                        && mCurrentStreamType != mStreamType));
        if (mCurrentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media uri for playback.
            mediaChanged = true;
            mCurrentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play();
            }
            return;
        } else {
            release(true);
        }

        mCurrentUri = uri;

        try {
            mMediaPlayer = new MediaPlayer();
            if (mAudioSession != 0) {
                mMediaPlayer.setAudioSessionId(mAudioSession);
            } else {
                mAudioSession = mMediaPlayer.getAudioSessionId();
            }
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mContext.getApplicationContext(), mCurrentUri, null);
            mMediaPlayer.setAudioStreamType(mStreamType);
            mCurrentStreamType = mStreamType;
            mMediaPlayer
                    .setWakeMode(mContext.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.prepareAsync();
            prepare();
            setNewState(STATE_PREPARING);
        } catch (IOException ex) {
            Log.e(TAG, "playFromMediaUri IOException", ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "playFromMediaUri IllegalArgumentException", ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalStateException ex) {
            Log.e(TAG, "playFromMediaUri IllegalStateException", ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    @Override
    public void release(boolean releasePlayer) {
        super.release(releasePlayer);
        if (releasePlayer && mMediaPlayer != null) {
            mMediaPlayer.setOnPreparedListener(null);
            mMediaPlayer.setOnCompletionListener(null);
            mMediaPlayer.setOnErrorListener(null);
            mMediaPlayer.setOnBufferingUpdateListener(null);
            mMediaPlayer.setOnSeekCompleteListener(null);
            final MediaPlayer lastMediaPlayer = mMediaPlayer;
            Executors.io()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    lastMediaPlayer.reset();
                                    lastMediaPlayer.release();
                                }
                            });
            mMediaPlayer = null;
        }
    }

    @Override
    public void onStop() {
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        setNewState(STATE_IDLE);
        release(true);
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    @Override
    protected void onPlay() {
        if (mSeekWhileNotPlaying != -1) {
            final int position = mSeekWhileNotPlaying;
            mMediaPlayer.seekTo(position);
            mMediaPlayer.setOnSeekCompleteListener(
                    new MediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            mMediaPlayer.start();
                            mSeekWhileNotPlaying = -1;
                            setNewState(STATE_PLAYING);
                        }
                    });
        } else {
            mMediaPlayer.start();
            setNewState(STATE_PLAYING);
        }
    }

    @Override
    protected void onPause() {
        mMediaPlayer.pause();
        Bundle extra = new Bundle();
        extra.putBoolean(KEY_EXTRA_NOTIFY, mShouldNotify);
        setNewState(STATE_PAUSED, extra);
        release(false);
    }

    private void setNewState(int newPlayerState) {
        setNewState(newPlayerState, null);
    }

    private void setNewState(int newPlayerState, Bundle extras) {
        setCurrentState(newPlayerState);

        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        if (newPlayerState == STATE_IDLE) {
            mCurrentMediaPlayedToCompletion = true;
        }

        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        final long reportPosition;
        if (mSeekWhileNotPlaying >= 0) {
            reportPosition = mSeekWhileNotPlaying;
            if (newPlayerState == STATE_PLAYING) {
                mSeekWhileNotPlaying = -1;
            }
        } else {
            reportPosition = mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
        }
        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());
        stateBuilder.setBufferedPosition(mCurrentBufferPercentage);
        stateBuilder.setState(newPlayerState, reportPosition, 1.0f, SystemClock.elapsedRealtime());

        if (extras != null) {
            stateBuilder.setExtras(extras);
        }

        mPlaybackInfoListener.onPlaybackStateChange(stateBuilder.build());
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not listed in
     * the bitmask of capabilities then the MediaSession will not handle it. For example, if you don't
     * want ACTION_STOP to be handled by the MediaSession, then don't included it in the bitmask
     * that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        int state = getCurrentState();
        switch (state) {
            case STATE_IDLE:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case STATE_PLAYING:
                actions |=
                        PlaybackStateCompat.ACTION_STOP
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |=
                        PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                | PlaybackStateCompat.ACTION_STOP
                                | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    @Override
    public void seekTo(long position) {
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mSeekWhileNotPlaying = (int) position;
            } else {
                mSeekWhileNotPlaying = -1;
                mMediaPlayer.seekTo((int) position);
                // Set the state (to the current state) because the position changed and should
                // be reported to clients.
                setNewState(getCurrentState());
            }
        }
    }

    @Override
    public void setVolume(float volume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    /**
     * MediaPlayer 不支持动态设置这个参数，暂时通过重新prepare和seek实现。
     */
    public void setStreamType(int streamType) {
        if (streamType == mStreamType) {
            return;
        }
        mStreamType = streamType;
        mShouldNotify = false;
        if (mMediaPlayer != null) {
            mSeekWhileNotPlaying = mMediaPlayer.getCurrentPosition();
            pause();
            mCurrentMediaPlayedToCompletion = true;
            playFromMediaUri(mCurrentUri);
        }
    }
}
