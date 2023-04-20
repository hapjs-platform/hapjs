/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import org.hapjs.component.constants.Attributes;

public class ExoPlayer extends Player
        implements SimpleExoPlayer.VideoListener,
        com.google.android.exoplayer2.Player.EventListener {

    private static final String TAG = "ExoPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private final DataSource.Factory mManifestDataSourceFactory;
    private final DataSource.Factory mMediaDataSourceFactory;
    private SimpleExoPlayer mPlayer;
    private DefaultTrackSelector trackSelector;
    private MediaSource mMediaSource = null;
    private Timeline.Period mPeriod = null;
    private int mSourceType = C.TYPE_OTHER;
    private boolean isBuffering;
    private SuspendLoadControl mSuspendLoadControl;

    public ExoPlayer(@NonNull Context context) {
        super(context);
        mManifestDataSourceFactory =
                new DefaultDataSourceFactory(context, Util.getUserAgent(context, "default"));
        mMediaDataSourceFactory =
                new DefaultDataSourceFactory(
                        context, Util.getUserAgent(context, "default"),
                        new DefaultBandwidthMeter());
    }

    @Override
    protected void onPrepare(boolean readyToPlay) {
        if (mUri == null) {
            return;
        }
        if (mPlayer == null) {
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            DefaultRenderersFactory renderersFactory =
                    new DefaultRenderersFactory(mApplicationContext);
            mSuspendLoadControl = new SuspendLoadControl();
            mSuspendLoadControl.setSuspendBuffering(isSuspendBuffer());
            mPlayer =
                    ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector,
                            mSuspendLoadControl);
            mPlayer.setVideoListener(this);
            mPlayer.addListener(this);
            if (mSurface != null) {
                mPlayer.setVideoSurface(mSurface);
            }
            setMuted(isMuted());
            setSpeed(mSpeed);
        }
        if (mPlayCount == 1) {
            mMediaSource = createMediaSource(mUri, null, null);
        } else {
            mMediaSource = new LoopingMediaSource(createMediaSource(mUri, null, null), mPlayCount);
        }
        mPlayCountChanged = false;
        mPlayer.prepare(mMediaSource);
        if (readyToPlay) {
            mPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onStart() {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onStop() {
        if (mPlayer != null) {
            mPlayer.stop(true);
        }
    }

    @Override
    protected void onRelease() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.clearVideoSurface();
            mPlayer.release();
            mPlayer = null;
            mSuspendLoadControl = null;
        }
    }

    @Override
    public long getDuration() {
        if (mPlayer != null && hasDuration()) {
            return mPlayer.getDuration();
        }
        return DURATION_NONE;
    }

    @Override
    protected void onSuspendBufferChanged(boolean suspendBuffer) {
        if (mSuspendLoadControl != null) {
            mSuspendLoadControl.setSuspendBuffering(suspendBuffer);
        }
    }

    private boolean hasDuration() {
        boolean result = false;
        switch (mSourceType) {
            case C.TYPE_HLS: {
                // situation: m3u8 has end tag.
                Object currentManifest = mPlayer.getCurrentManifest();
                if (currentManifest instanceof HlsManifest) {
                    HlsManifest hlsManifest = (HlsManifest) currentManifest;
                    if (hlsManifest.mediaPlaylist != null && hlsManifest.mediaPlaylist.hasEndTag) {
                        result = true;
                    }
                }

                break;
            }
            case C.TYPE_DASH: {
                // situation: mpd type is static.
                Object currentManifest = mPlayer.getCurrentManifest();
                if (currentManifest instanceof DashManifest) {
                    DashManifest dashManifest = (DashManifest) currentManifest;
                    if (!dashManifest.dynamic) {
                        result = true;
                    }
                }

                break;
            }
            case C.TYPE_SS: {
                // Nothing to do.
                break;
            }
            case C.TYPE_OTHER: {
                result = true;
                break;
            }
            default:
                break;
        }
        return result;
    }

    @Override
    public long getCurrentPosition() {
        if (mPlayer != null && isInPlaybackState()) {
            long position = mPlayer.getCurrentPosition();
            // Adjust position to be relative to start of period rather than window
            // when source type is m3u8.
            Timeline currentTimeline = mPlayer.getCurrentTimeline();
            if (!currentTimeline.isEmpty()) {
                if (mPeriod == null) {
                    mPeriod = new Timeline.Period();
                }
                position -=
                        currentTimeline
                                .getPeriod(mPlayer.getCurrentPeriodIndex(), mPeriod)
                                .getPositionInWindowMs();
            }
            return position;
        }
        return 0;
    }

    @Override
    protected void onSeek(long timeMs) {
        if (mPlayer == null || getDuration() <= 0) {
            return;
        }
        if (mMediaSource instanceof LoopingMediaSource
                && getCurrentState() == STATE_PLAYBACK_COMPLETED
                && !mPlayer.getCurrentTimeline().isEmpty()) {
            mPlayer.seekTo(0, timeMs);
        } else {
            mPlayer.seekTo(timeMs);
        }
    }

    @Override
    public int getBufferPercentage() {
        if (mPlayer != null) {
            return mPlayer.getBufferedPercentage();
        }
        return 0;
    }

    @Override
    public void setVolume(float volume) {
        if (mPlayer != null) {
            mPlayer.setVolume(volume);
        }
    }

    @Override
    public void onMuted(boolean muted) {
        if (mPlayer != null) {
            mPlayer.setVolume(muted ? 0f : 1f);
        }
    }

    @Override
    public void setPlayCount(String playCount) {
        if (TextUtils.isEmpty(playCount)) {
            return;
        }
        playCount = playCount.trim();
        int parsedCount;
        if (Attributes.PlayCount.INFINITE.equals(playCount)) {
            parsedCount = Integer.MAX_VALUE;
        } else {
            try {
                long count = Long.parseLong(playCount);
                parsedCount = count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
                if (parsedCount <= 0) {
                    Log.w(TAG, "setPlayCount: illegal playcount property " + parsedCount);
                    parsedCount = 1;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "setPlayCount: " + e);
                parsedCount = 1;
            }
        }
        if (mPlayCount != parsedCount) {
            mPlayCount = parsedCount;
            mPlayCountChanged = true;
        }
    }

    @Override
    public void setSpeed(float speed) {
        mSpeed = speed;
        if (mPlayer == null) {
            return;
        }
        PlaybackParameters origin = mPlayer.getPlaybackParameters();
        if (origin.speed == mSpeed) {
            Log.w(TAG, "the same speed,so  cancel setSpeed");
            return;
        }
        PlaybackParameters parameters;
        if (origin != null) {
            parameters = new PlaybackParameters(speed, origin.pitch, origin.skipSilence);
        } else {
            parameters = new PlaybackParameters(speed);
        }
        mPlayer.setPlaybackParameters(parameters);
    }

    @Override
    protected void onAttachSurface(@Nullable Surface surface) {
        if (mPlayer != null) {
            mPlayer.setVideoSurface(surface);
        }
    }

    private MediaSource createMediaSource(
            Uri uri, Handler handler, @Nullable MediaSourceEventListener listener) {
        mSourceType = Util.inferContentType(uri);
        switch (mSourceType) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mMediaDataSourceFactory),
                        mManifestDataSourceFactory)
                        .createMediaSource(uri, handler, listener);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mMediaDataSourceFactory)
                        .createMediaSource(uri, handler, listener);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(mMediaDataSourceFactory)
                        .createMediaSource(uri, handler, listener);
            case C.TYPE_SS:
            default:
                throw new IllegalStateException("Unsupported type: " + mSourceType);
        }
    }

    @Override
    public void onVideoSizeChanged(
            int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        onVideoSizeChanged(width, height);
    }

    @Override
    public void onRenderedFirstFrame() {
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object o, int i) {
        // do nothing.
    }

    @Override
    public void onTracksChanged(
            TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        // do nothing.
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int plackbackState) {
        switch (plackbackState) {
            case com.google.android.exoplayer2.Player.STATE_IDLE: {
                if (getCurrentState() != STATE_ERROR && getCurrentState() != STATE_STOP) {
                    onPlayerStateChanged(Player.STATE_IDLE);
                }
                break;
            }
            case com.google.android.exoplayer2.Player.STATE_BUFFERING: {
                isBuffering = true;
                notifyLoadingChange(true);
                break;
            }
            case com.google.android.exoplayer2.Player.STATE_READY: {
                if (isBuffering) {
                    isBuffering = false;
                    notifyLoadingChange(false);
                }
                if (IMediaPlayer.STATE_PREPARING == getCurrentState()) {
                    onPlayerStateChanged(IMediaPlayer.STATE_PREPARED);
                }

                if (playWhenReady && getCurrentState() == IMediaPlayer.STATE_PREPARED) {
                    // playWhenPrepared
                    onPlayerStateChanged(IMediaPlayer.STATE_PLAYING);
                }

                break;
            }
            case com.google.android.exoplayer2.Player.STATE_ENDED: {
                onPlayerStateChanged(IMediaPlayer.STATE_PLAYBACK_COMPLETED);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int i) {
        // do nothing.
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean b) {
        // do nothing.
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        onPlayerStateChanged(Player.STATE_ERROR);

        int what = -1;
        if (e != null) {
            what = e.type;
        }
        notifyError(what, -1);
    }

    @Override
    public void onPositionDiscontinuity(int i) {
        // do nothing.
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // do nothing.
    }

    @Override
    public void onSeekProcessed() {
        // do nothing.
    }
}
