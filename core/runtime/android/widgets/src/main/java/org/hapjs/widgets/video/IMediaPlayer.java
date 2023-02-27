/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.net.Uri;
import android.view.TextureView;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public interface IMediaPlayer {

    int STATE_ERROR = -1;
    int STATE_IDLE = 0;
    int STATE_PREPARING = 1;
    int STATE_PREPARED = 2;
    int STATE_PLAYING = 3;
    int STATE_PAUSED = 4;
    int STATE_STOP = 5;
    int STATE_PLAYBACK_COMPLETED = 6;
    int MEDIA_SOURCE_ERROR = 200000;
    int MEDIA_RENDER_ERROR = 300000;
    int MEDIA_UNEXCEPTED_ERROR = 400000;

    void prepare();

    void prepare(boolean playWhenReady);

    void start();

    void pause();

    void stop();

    void release();

    void setDataSource(String path);

    void setDataSource(Uri uri);

    void setDataSource(Uri uri, Map<String, String> headers);

    Uri getDataSource();

    void autoPlay(boolean autoPlay);

    @PlayerState
    int getCurrentState();

    void setCurrentState(int currentState);

    @PlayerState
    int getTargetState();

    boolean isPlaying();

    void seek(long timeMs);

    void setMuted(boolean muted);

    long getCurrentPosition();

    long getDuration();

    boolean isSeekable();

    void setVideoTextureView(TextureView textureView);

    int getVideoWidth();

    int getVideoHeight();

    int getBufferPercentage();

    void setEventListener(IMediaPlayer.EventListener listener);

    void setPlayCount(String playCount);

    /**
     * 是否暂停缓冲
     *
     * @return
     */
    boolean isSuspendBuffer();

    /**
     * 暂停缓冲控制
     *
     * @param suspendBuffer true:暂停缓冲，false：恢复缓冲
     */
    void setSuspendBuffer(boolean suspendBuffer);

    @IntDef({
            STATE_ERROR,
            STATE_IDLE,
            STATE_PREPARING,
            STATE_PREPARED,
            STATE_PLAYING,
            STATE_PAUSED,
            STATE_STOP,
            STATE_PLAYBACK_COMPLETED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface PlayerState {
    }

    @IntDef({MEDIA_SOURCE_ERROR, MEDIA_RENDER_ERROR, MEDIA_UNEXCEPTED_ERROR})
    @interface MediaErrorType {
    }

    /**
     * 设置播放速度
     * @param speed 倍速
     */
    void setSpeed(float speed);

    interface EventListener {
        void onLoadingChanged(IMediaPlayer player, boolean isLoading);

        boolean onError(IMediaPlayer player, @MediaErrorType int what, int extra);

        void onPlayerStateChanged(IMediaPlayer player, @PlayerState int playbackState);

        void onVideoSizeChanged(IMediaPlayer player, int width, int height);

        void onAudioFocusChange(int focusChange);
    }
}
