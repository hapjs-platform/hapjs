/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.net.Uri;
import android.view.TextureView;
import java.util.Map;
import org.hapjs.component.constants.Attributes;

public class PlayerProxy<P extends IMediaPlayer> implements IMediaPlayer {

    protected P mPlayer;

    private Uri mDataSource;
    private TextureView mTextureView;
    private EventListener mEventListener;
    private boolean mMuted;
    private boolean mAutoPlay;
    private String mPlayCount = Attributes.PlayCount.ONCE;
    private float mSpeed = 1.0f;
    public long mCachedPosition = -1;
    private boolean mSuspendBuffer = false;

    @Override
    public void prepare() {
        prepare(false);
    }

    @Override
    public void prepare(boolean playWhenReady) {
        if (mDataSource == null) {
            return;
        }

        if (mPlayer == null) {
            bind();
        }
        mPlayer.prepare(playWhenReady);
        PlayerInstanceManager.getInstance().startUsePlayer(this);
    }

    @Override
    public void start() {
        if (mDataSource == null) {
            return;
        }
        if (mPlayer == null) {
            bind();
        }
        mPlayer.start();
        PlayerInstanceManager.getInstance().startUsePlayer(this);
    }

    @Override
    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Override
    public void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    @Override
    public void release() {
        PlayerInstanceManager.getInstance().release(mPlayer);
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
        if (mDataSource != null && mDataSource != uri) {
            mCachedPosition = -1;
        }
        mDataSource = uri;
        if (mPlayer != null) {
            mPlayer.setDataSource(uri, headers);
        }
    }

    @Override
    public Uri getDataSource() {
        return mDataSource;
    }

    @Override
    public void autoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;

        if (mPlayer != null) {
            mPlayer.autoPlay(autoPlay);
        }
    }

    @Override
    public int getCurrentState() {
        if (mPlayer != null) {
            return mPlayer.getCurrentState();
        }
        return IMediaPlayer.STATE_IDLE;
    }

    @Override
    public void setCurrentState(int currentState) {
        mPlayer.setCurrentState(currentState);
    }

    @Override
    public int getTargetState() {
        if (mPlayer != null) {
            return mPlayer.getTargetState();
        }
        return STATE_IDLE;
    }

    @Override
    public boolean isPlaying() {
        if (mPlayer != null) {
            return mPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void seek(long timeMs) {
        if (mPlayer != null) {
            mPlayer.seek(timeMs);
        } else {
            mCachedPosition = timeMs;
        }
    }

    @Override
    public void setMuted(boolean muted) {
        mMuted = muted;
        if (mPlayer != null) {
            mPlayer.setMuted(muted);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (mPlayer != null) {
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mPlayer != null) {
            return mPlayer.getDuration();
        }
        return Player.DURATION_NONE;
    }

    @Override
    public boolean isSeekable() {
        if (mPlayer != null) {
            return mPlayer.isSeekable();
        }
        return false;
    }

    @Override
    public void setVideoTextureView(TextureView textureView) {
        mTextureView = textureView;
        if (mPlayer != null) {
            mPlayer.setVideoTextureView(textureView);
        }
    }

    @Override
    public int getVideoWidth() {
        if (mPlayer != null) {
            return mPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (mPlayer != null) {
            return mPlayer.getVideoHeight();
        }
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        if (mPlayer != null) {
            return mPlayer.getBufferPercentage();
        }
        return 0;
    }

    @Override
    public void setPlayCount(String playCount) {
        mPlayCount = playCount;
        if (mPlayer != null) {
            mPlayer.setPlayCount(playCount);
        }
    }

    @Override
    public boolean isSuspendBuffer() {
        return mSuspendBuffer;
    }

    @Override
    public void setSuspendBuffer(boolean suspendBuffer) {
        mSuspendBuffer = suspendBuffer;
        if (mPlayer != null) {
            mPlayer.setSuspendBuffer(mSuspendBuffer);
        }
    }

    @Override
    public void setSpeed(float speed) {
        mSpeed = speed;
        if (mPlayer != null) {
            mPlayer.setSpeed(speed);
        }
    }

    @Override
    public void setEventListener(EventListener listener) {
        mEventListener = listener;
        if (mPlayer != null) {
            mPlayer.setEventListener(listener);
        }
    }

    protected void bind() {
        if (mPlayer == null) {
            mPlayer = PlayerInstanceManager.getInstance().obtainPlayer(this);
            onAttach();
        }
    }

    protected void onAttach() {
        mPlayer.setDataSource(mDataSource);
        if (mTextureView != null) {
            mPlayer.setVideoTextureView(mTextureView);
        }

        if (mEventListener != null) {
            mPlayer.setEventListener(mEventListener);
        }

        if (mCachedPosition > 0) {
            mPlayer.seek(mCachedPosition);
            mCachedPosition = -1;
        }
        mPlayer.setMuted(mMuted);
        mPlayer.autoPlay(mAutoPlay);
        mPlayer.setPlayCount(mPlayCount);
        mPlayer.setSpeed(mSpeed);
        mPlayer.setSuspendBuffer(mSuspendBuffer);
    }

    public void unbind() {
        if (mPlayer != null) {
            long currentPosition = mPlayer.getCurrentPosition();
            if (currentPosition > 0) {
                mCachedPosition = currentPosition;
            }
            mPlayer.pause();
            mPlayer.stop();
            onDettach();
        }
        mPlayer = null;
    }

    protected void onDettach() {
        mPlayer.setEventListener(null);
        mPlayer.setVideoTextureView(null);
        mPlayer.setMuted(false);
        mPlayer.autoPlay(false);
        mPlayer.setPlayCount(Attributes.PlayCount.ONCE);
        mPlayer.setSuspendBuffer(false);
    }
}
