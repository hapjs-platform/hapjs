/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import org.hapjs.common.executors.Executors;

public abstract class Player implements IMediaPlayer {

    public static final int DURATION_NONE = -1;
    private static final String TAG = "Player";
    protected final Context mApplicationContext;
    private final AudioManager mAudioManager;
    // settable by the client
    protected Uri mUri;
    protected TextureView mVideoView = null;
    // recording the seek position while preparing
    protected long mSeekWhenPrepared;
    protected int mPlayCount = 1;
    protected float mSpeed = 1;
    protected Surface mSurface;
    protected boolean mDataSourceChanged;
    protected boolean mPlayCountChanged;
    private Map<String, String> mHeaders;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    private int mVideoWidth;
    private int mVideoHeight;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private IMediaPlayer.EventListener mEventListener;
    private boolean mPlayWhenPrepared;
    private boolean mMuted;
    private boolean mAutoPlay;
    private boolean mSuspendBuffer = false;

    protected Player(@NonNull Context context) {
        mApplicationContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mApplicationContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public final void prepare() {
        if (mUri == null) {
            return;
        }
        prepare(false);
    }

    @Override
    public final void prepare(boolean playWhenReady) {
        if (mUri == null) {
            return;
        }
        if (mCurrentState == STATE_IDLE
                || mCurrentState == STATE_ERROR
                || mCurrentState == STATE_STOP
                || mDataSourceChanged
                || mPlayCountChanged) {

            mDataSourceChanged = false;
            onPlayerStateChanged(STATE_PREPARING);
            onPrepare(playWhenReady);
            if (playWhenReady) {
                requestAudioFocus();
            }
            if (mVideoView != null) {
                mVideoView.requestLayout();
                mVideoView.invalidate();
            }
        } else {
            Log.w(TAG, "prepare ignore");
        }

        if (playWhenReady) {
            mTargetState = STATE_PLAYING;
        } else {
            mTargetState = STATE_PREPARED;
        }
    }

    @Override
    public final void start() {
        if (mUri == null) {
            Log.w(TAG, "start play fail,the datasource is null");
            return;
        }

        if (mCurrentState == STATE_IDLE
                || mCurrentState == STATE_ERROR
                || mCurrentState == STATE_STOP
                || mDataSourceChanged
                || mPlayCountChanged) {

            prepare(true);
        } else if (isInPlaybackState() && mCurrentState != STATE_PLAYING) {
            if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
                seek(0);
            }
            requestAudioFocus();
            onStart();

            if (mCurrentState != STATE_PLAYING) {
                onPlayerStateChanged(STATE_PLAYING);
            }
        } else {
            mPlayWhenPrepared = true;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public final void pause() {
        if (mUri == null) {
            Log.w(TAG, "pause play fail,the datasource is null");
            return;
        }
        mPlayWhenPrepared = false;
        if (isPlaying() || mCurrentState == STATE_PREPARED) {
            abandonAudioFocus();
            onPause();
            onPlayerStateChanged(STATE_PAUSED);
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public final void stop() {
        abandonAudioFocus();
        onPlayerStateChanged(STATE_STOP);
        onStop();
        mTargetState = STATE_STOP;
        mDataSourceChanged = false;
    }

    @Override
    public void release() {
        abandonAudioFocus();
        onRelease();
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }

        if (mVideoView != null) {
            TextureView.SurfaceTextureListener listener = mVideoView.getSurfaceTextureListener();
            if (listener instanceof VideoTextureListener) {
                listener = ((VideoTextureListener) listener).mTextureListener;
            }
            mVideoView.setSurfaceTextureListener(listener);
            mVideoView = null;
        }

        mEventListener = null;
        mTargetState = STATE_IDLE;
        onPlayerStateChanged(STATE_IDLE);
        mDataSourceChanged = false;
    }

    @Override
    public void setDataSource(String path) {
        setDataSource(Uri.parse(path));
    }

    @Override
    public void setDataSource(Uri uri) {
        setDataSource(uri, null);
    }

    @Override
    public void setDataSource(Uri uri, Map<String, String> headers) {
        mDataSourceChanged = mUri != uri;
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;

        if (mUri != null && mDataSourceChanged) {
            stop();
        }

        if (mUri != null && mAutoPlay) {
            prepare(true);
        }
    }

    @Override
    public void autoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
        if (mUri != null && mAutoPlay && mCurrentState == STATE_IDLE
                || mCurrentState == STATE_ERROR) {
            prepare(true);
        }
    }

    @Override
    public Uri getDataSource() {
        return mUri;
    }

    @Override
    public @PlayerState
    int getCurrentState() {
        return mCurrentState;
    }

    @Override
    public void setCurrentState(int currentState) {
        this.mCurrentState = currentState;
    }

    @Override
    public @PlayerState
    int getTargetState() {
        return mTargetState;
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && (mCurrentState == STATE_PLAYING);
    }

    @Override
    public final void seek(long timeMs) {
        if (isInPlaybackState()) {
            onSeek(timeMs);
        } else {
            mSeekWhenPrepared = timeMs;
        }
    }

    @Override
    public boolean isSuspendBuffer() {
        return mSuspendBuffer;
    }

    @Override
    public void setSuspendBuffer(boolean suspendBuffer) {
        if (mSuspendBuffer == suspendBuffer) {
            return;
        }
        mSuspendBuffer = suspendBuffer;
        onSuspendBufferChanged(suspendBuffer);
    }

    protected abstract void onMuted(boolean muted);

    public boolean isMuted() {
        return mMuted;
    }

    public void setMuted(boolean muted) {
        mMuted = muted;
        onMuted(muted);
    }

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    protected abstract void onSuspendBufferChanged(boolean suspendBuffer);

    @Override
    public boolean isSeekable() {
        return isInPlaybackState() && getDuration() > 0;
    }

    @Override
    public void setVideoTextureView(TextureView videoView) {
        if (mVideoView == videoView) {
            return;
        }
        mVideoView = videoView;
        if (videoView == null) {
            setSurface(null);
        } else {
            TextureView.SurfaceTextureListener listener = videoView.getSurfaceTextureListener();
            if (listener instanceof VideoTextureListener) {
                listener = ((VideoTextureListener) listener).mTextureListener;
            }
            videoView.setSurfaceTextureListener(new VideoTextureListener(videoView, listener));

            SurfaceTexture surfaceTexture =
                    videoView.isAvailable() ? videoView.getSurfaceTexture() : null;
            if (surfaceTexture != null) {
                Surface surface = new Surface(surfaceTexture);
                setSurface(surface);
            }
        }
    }

    void setSurface(Surface surface) {
        if (mSurface != null && Objects.equals(surface, mSurface)) {
            // unsupport change surface
            return;
        }
        onAttachSurface(surface);
        mSurface = surface;
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public void setEventListener(IMediaPlayer.EventListener l) {
        mEventListener = l;
    }

    boolean requestAudioFocus() {
        if (isMuted()) {
            return false;
        }
        if (mOnAudioFocusChangeListener == null) {
            mOnAudioFocusChangeListener =
                    focusChange ->
                            Executors.ui()
                                    .execute(
                                            () -> {
                                                switch (focusChange) {
                                                    case AudioManager.AUDIOFOCUS_GAIN:
                                                        if (mEventListener != null) {
                                                            mEventListener.onAudioFocusChange(
                                                                    AudioManager.AUDIOFOCUS_GAIN);
                                                        }
                                                        break;
                                                    case AudioManager.AUDIOFOCUS_LOSS:
                                                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                                                        if (mEventListener != null) {
                                                            mEventListener.onAudioFocusChange(
                                                                    AudioManager.AUDIOFOCUS_LOSS);
                                                        }
                                                        break;
                                                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            });
        }
        final int result =
                mAudioManager.requestAudioFocus(
                        mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    void abandonAudioFocus() {
        if (mOnAudioFocusChangeListener != null) {
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }
    }

    protected void onPlayerStateChanged(@PlayerState int state) {
        boolean needNotify = mCurrentState != state;
        mCurrentState = state;

        if (state == STATE_PREPARED) {
            if (mVideoWidth != 0
                    && mVideoHeight != 0
                    && mVideoView != null
                    && mVideoView.getSurfaceTexture() != null) {
                mVideoView.getSurfaceTexture().setDefaultBufferSize(mVideoWidth, mVideoHeight);
                mVideoView.requestLayout();
            }

            if (mSeekWhenPrepared > 0) {
                onSeek(mSeekWhenPrepared);
                mSeekWhenPrepared = 0;
            }

            if (mPlayWhenPrepared) {
                requestAudioFocus();
                onStart();
                mPlayWhenPrepared = false;
            }
        }

        if (state == STATE_ERROR || state == STATE_PLAYBACK_COMPLETED) {
            mTargetState = state;
        }

        if (needNotify) {
            notifyStateChanged(state);
        }
    }

    private void notifyStateChanged(@PlayerState int state) {
        if (mEventListener != null) {
            mEventListener.onPlayerStateChanged(this, state);
        }
    }

    protected void notifyError(int what, int extra) {
        if (mEventListener != null) {
            mEventListener.onError(this, what, extra);
        }
    }

    protected void onVideoSizeChanged(int width, int height) {
        if (mVideoView != null && (mVideoWidth != width || mVideoHeight != height)) {
            SurfaceTexture surfaceTexture = mVideoView.getSurfaceTexture();
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(width, height);
            }
            mVideoView.requestLayout();
        }
        mVideoWidth = width;
        mVideoHeight = height;

        notifyVideoSizeChanged(width, height);
    }

    private void notifyVideoSizeChanged(int width, int height) {
        if (mEventListener != null) {
            mEventListener.onVideoSizeChanged(this, width, height);
        }
    }

    protected void notifyLoadingChange(boolean isLoading) {
        if (mEventListener != null) {
            mEventListener.onLoadingChanged(this, isLoading);
        }
    }

    public boolean isInPlaybackState() {
        return (mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE
                && mCurrentState != STATE_PREPARING
                && mCurrentState != STATE_STOP);
    }

    public abstract void setVolume(float volume);

    protected abstract void onSeek(long pos);

    protected abstract void onPrepare(boolean readyToPlay);

    protected abstract void onStart();

    protected abstract void onPause();

    protected abstract void onStop();

    protected abstract void onAttachSurface(@Nullable Surface surface);

    protected abstract void onRelease();

    @Override
    public abstract void setPlayCount(String playCount);

    private class VideoTextureListener implements TextureView.SurfaceTextureListener {

        private TextureView mTextureView;
        private TextureView.SurfaceTextureListener mTextureListener;

        VideoTextureListener(TextureView textureView, TextureView.SurfaceTextureListener listener) {
            mTextureView = textureView;
            mTextureListener = listener;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (mTextureListener != null) {
                mTextureListener.onSurfaceTextureAvailable(surface, width, height);
            }
            if (mSurface == null) {
                setSurface(new Surface(surface));
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if (mTextureListener != null) {
                mTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (mTextureListener != null) {
                boolean destroy = mTextureListener.onSurfaceTextureDestroyed(surface);
                if (destroy) {
                    setSurface(null);
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        if (mTextureView != null) {
                            mTextureView.setSurfaceTextureListener(mTextureListener);
                        }
                        mTextureView = null;
                        mTextureListener = null;
                    }
                }
                return destroy;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if (mTextureListener != null) {
                mTextureListener.onSurfaceTextureUpdated(surface);
            }
        }
    }
}
