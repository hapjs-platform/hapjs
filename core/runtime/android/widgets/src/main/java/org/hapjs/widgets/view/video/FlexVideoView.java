/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.video;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.FlexRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.yoga.YogaNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.BrightnessUtils;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.appearance.AppearanceHelper;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.ScrollView;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.model.videodata.VideoCacheData;
import org.hapjs.model.videodata.VideoCacheManager;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.Display;
import org.hapjs.render.Page;
import org.hapjs.widgets.Div;
import org.hapjs.widgets.R;
import org.hapjs.widgets.list.List;
import org.hapjs.widgets.video.IMediaPlayer;
import org.hapjs.widgets.video.Player;
import org.hapjs.widgets.video.PlayerInstanceManager;
import org.hapjs.widgets.video.PlayerProxy;
import org.hapjs.widgets.video.Video;
import org.hapjs.widgets.view.image.FlexImageView;
import org.hapjs.widgets.view.list.FlexGridLayoutManager;

public class FlexVideoView extends FrameLayout
        implements ComponentHost,
        TextureVideoView.SurfaceTextureListener,
        MediaGestureHelper.MediaGestureChangeListener,
        IMediaPlayer.EventListener,
        GestureHost {

    private static final String TAG = "FlexVideoView";

    private static final Set<Integer> sKeepScreenOnFlag = new HashSet<>();
    private static final int MSG_TIME_UPDATE = 0;
    private static final int TIME_UPDATE_INTERVAL = 250;

    private static final int STATE_FULLSCREEN = 0;
    private static final int STATE_NOT_FULLSCREEN = 1;
    private static final int STATE_ENTERING_FULLSCREEN = 2;
    private static final int STATE_EXITING_FULLSCREEN = 3;
    private final ControlsManager mControlsManager;
    private boolean mControlsVisible = true;
    @Nullable
    private IMediaPlayer mPlayer;
    public Uri mUri;
    private Uri mPosterUri;
    private Boolean mMuted;
    private String mPlayCount;
    private float mSpeed = Video.SPEED_DEFAULT;
    private OnErrorListener mOnErrorListener;
    private OnIdleListener mOnIdleListener;
    private OnStartListener mOnStartListener;
    private OnPreparingListener mOnPreparingListener;
    private OnPreparedListener mOnPreparedListener;
    private OnPlayingListener mOnPlayingListener;
    private OnPauseListener mOnPauseListener;
    private OnCompletionListener mOnCompletionListener;
    private OnTimeUpdateListener mOnTimeUpdateListener;
    private final Handler mTimeUpdateHandler =
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (mOnTimeUpdateListener != null) {
                        mOnTimeUpdateListener.onTimeUpdate();
                    }
                    removeMessages(MSG_TIME_UPDATE);
                    if (null != mPlayer && mPlayer.getCurrentState() == Player.STATE_PLAYING) {
                        msg = obtainMessage(MSG_TIME_UPDATE);
                        sendMessageDelayed(msg, TIME_UPDATE_INTERVAL);
                    }
                }
            };
    private OnSeekingListener mOnSeekingListener;
    private OnSeekedListener mOnSeekedListener;
    private OnFullscreenChangeListener mOnFullscreenChangeListener;
    private Video mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private ViewGroup mParent;
    private View mPlaceHolderView;
    private int mVideoIndex;
    private int mFullscreenState = STATE_NOT_FULLSCREEN;
    private boolean mExitingFullscreen = false;
    private int mScreenOrientaion = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

    private Path mClipPath;
    private float[] mRadiusArray;
    private RectF mRoundRectF;
    private int mBorderRadiusFlags;

    private boolean mAutoPlay = false;
    private MediaGestureHelper mMediaGestureHelper;
    private boolean mIsLazyCreate;
    private YogaNode mCurrentNode;
    private boolean mRegisterBrightnessObserver;
    private IGesture mGesture;
    private boolean mVisible;
    private boolean mIsFirstExitFullScreenAttach = false;
    public static Uri mCacheFullScreenUri = null;
    public static boolean mIsFullScreen = false;
    public boolean mIsEverCacheVideo = false;
    // 是否按下 video 控制栏左下角暂停按钮
    private boolean mIsPauseButtonPress;

    public FlexVideoView(final Context context) {
        this(context, true);
    }

    public FlexVideoView(final Context context, boolean controlsVisibility) {
        super(context);
        mControlsVisible = controlsVisibility;
        mControlsManager = new ControlsManager(controlsVisibility);
        mVisible = true;
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    private void initBorderRadiusParams() {
        if (mClipPath == null) {
            mClipPath = new Path();
        }
        if (mRadiusArray == null) {
            mRadiusArray = new float[8];
        }
        if (mRoundRectF == null) {
            mRoundRectF = new RectF();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isRoundedBorders()) {
            mRoundRectF.set(0, 0, getWidth(), getHeight());
            mClipPath.reset();
            mClipPath.addRoundRect(mRoundRectF, mRadiusArray, Path.Direction.CW);
            canvas.clipPath(mClipPath);
        }
        super.onDraw(canvas);
    }

    /**
     * mBorderRadiusFlags 是取int前8位标记mRadiusArray的8个坐标值是否大于0，八个1二进制中等于255
     *
     * @param radius
     */
    public void setBorderRadius(float radius) {
        initBorderRadiusParams();
        Arrays.fill(mRadiusArray, radius);
        if (radius > 0f) {
            mBorderRadiusFlags |= 255;
        } else {
            mBorderRadiusFlags &= ~255;
        }
    }

    public void setBorderCornerRadii(int position, float radius) {
        initBorderRadiusParams();
        if (!FloatUtil.floatsEqual(mRadiusArray[position * 2], radius)) {
            mRadiusArray[position * 2] = radius;
            mRadiusArray[position * 2 + 1] = radius;
        }
        if (radius > 0f) {
            mBorderRadiusFlags |= (1 << position);
            mBorderRadiusFlags |= (1 << (position + 1));
        } else {
            mBorderRadiusFlags &= ~(1 << position);
            mBorderRadiusFlags &= ~(1 << (position + 1));
        }
    }

    private boolean isRoundedBorders() {
        return mBorderRadiusFlags > 0f;
    }

    public void setPauseButtonPress(boolean mIsPauseButtonPress) {
        this.mIsPauseButtonPress = mIsPauseButtonPress;
    }

    public boolean isPauseButtonPress() {
        return mIsPauseButtonPress;
    }

    public void start() {
        if (mUri == null) {
            return;
        }
        if (mIsFullScreen && null != mCacheFullScreenUri && !mUri.equals(mCacheFullScreenUri)) {
            Log.w(TAG, "start mUri is not fullscreen Uri ,start invalid");
            return;
        }
        if (null != mPlayer && mUri.equals(mPlayer.getDataSource())) {
            if (mPlayer.isPlaying()) {
                Log.w(TAG, "start mPlayer  isPlaying  getCurrentState : " + mPlayer.getCurrentState());
                return;
            } else if (mPlayer.getCurrentState() == IMediaPlayer.STATE_PREPARED) {
                Log.w(TAG, "start mPlayer  STATE_PREPARED  getCurrentState : " + mPlayer.getCurrentState());
                mPlayer.start();
                return;
            }
        }
        initPlayer();
        if (mPlayer.getTargetState() != IMediaPlayer.STATE_PLAYING && mOnStartListener != null) {
            mOnStartListener.onStart();
        }
        makeEffectVideoURI(true);
        mPlayer.start();
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (null != mControlsManager) {
            boolean isControlsVisible = mControlsManager.isControlsLayoutVisible();
            if (!isControlsVisible) {
                FlexImageView postView = mControlsManager.createPosterView();
                if (mPosterUri == null) {
                    postView.setVisibility(GONE);
                } else {
                    postView.setSource(mPosterUri);
                    if (mPlayer == null || !mPlayer.isPlaying()) {
                        postView.setVisibility(VISIBLE);
                    }
                }
            }
            if (null != mPlayer) {
                mControlsManager.attachPlayer(mPlayer);
            }
        } else {
            Log.w(TAG, "onAttachedToWindow mControlsManager is null.");
        }
        if (mFullscreenState == STATE_ENTERING_FULLSCREEN ||
                mIsFirstExitFullScreenAttach) {
            if (mAutoPlay && null != mUri && null == mPlayer) {
                initPlayer();
                makeEffectVideoURI(false);
            }
            if (null != mPlayer && (mPlayer.getCurrentState() == Player.STATE_PREPARED
                    || mPlayer.getCurrentState() == Player.STATE_PLAYING) && mAutoPlay && mUri != null) {
                mPlayer.start();
            }
        }
        mIsFirstExitFullScreenAttach = false;
        onAttach();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onDetach();
    }

    private void onAttach() {
        if (mOnTimeUpdateListener != null
                && mPlayer != null
                && mPlayer.getCurrentState() == Player.STATE_PLAYING) {
            mTimeUpdateHandler.sendEmptyMessage(MSG_TIME_UPDATE);
        }
    }

    private void onDetach() {
        if (isFullscreen()) {
            ((Activity) getContext())
                    .setRequestedOrientation(
                            BuildPlatform.isTV()
                                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        if (mFullscreenState != STATE_ENTERING_FULLSCREEN
                && mFullscreenState != STATE_EXITING_FULLSCREEN) {
            if (mPlayer != null) {
                cacheCurrentVideoData();
                releasePlayer();
                mControlsVisible = true;
            } else {
                cacheCurrentVideoData();
            }
        }
        if (mOnTimeUpdateListener != null) {
            mTimeUpdateHandler.removeMessages(MSG_TIME_UPDATE);
        }
    }

    private void cacheCurrentVideoData() {
        if (mPlayer != null) {
            long position = mPlayer.getCurrentPosition();
            long duration = mPlayer.getDuration();
            if (position <= 0) {
                if (mPlayer instanceof PlayerProxy) {
                    position = ((PlayerProxy) mPlayer).mCachedPosition;
                }
            }
            if (position > 0 && null != mUri && null != mComponent && !mComponent.mIsDestroy) {
                mIsEverCacheVideo = true;
                VideoCacheData videoCacheData = new VideoCacheData();
                videoCacheData.lastPosition = position;
                videoCacheData.uri = mUri.toString();
                videoCacheData.duration = duration;
                VideoCacheManager.getInstance().putVideoData(mComponent.getPageId(), videoCacheData.uri, videoCacheData);
            }
        } else {
            Log.w(TAG, "cacheCurrentVideoData mPlayer is null.");
        }
    }

    protected void releasePlayer() {
        if (null != mPlayer) {
            mPlayer.pause();
            mPlayer.stop();
            mPlayer.setEventListener(null);
            mPlayer.setVideoTextureView(null);
            mPlayer.setMuted(false);
            mPlayer.autoPlay(false);
            mPlayer.setPlayCount(Attributes.PlayCount.ONCE);
            mPlayer.setSuspendBuffer(false);
            mPlayer.release();
            if (null != mControlsManager) {
                mControlsManager.detachPlayer();
            }
            mPlayer = null;
        } else {
            Log.w(TAG, "releasePlayer mPlayer is null.");
        }

    }

    protected void onIdle() {
        if (mOnIdleListener != null) {
            mOnIdleListener.onIdle();
        }
        if (mOnTimeUpdateListener != null) {
            mTimeUpdateHandler.removeMessages(MSG_TIME_UPDATE);
        }
        mControlsManager.initializeControlsView();
        switchKeepScreenOnFlagsByState(Player.STATE_IDLE);
    }

    protected void onPreparing() {

        if (mOnPreparingListener != null) {
            mOnPreparingListener.onPreparing();
        }
        switchKeepScreenOnFlagsByState(Player.STATE_PREPARING);
        mControlsManager.showLoading();
    }

    protected void onPrepared() {

        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(mPlayer);
        }
        switchKeepScreenOnFlagsByState(Player.STATE_PREPARED);
    }

    protected void onPlaying() {
        if (mOnPlayingListener != null) {
            mOnPlayingListener.onPlaying();
        }
        if (mOnTimeUpdateListener != null) {
            mTimeUpdateHandler.sendEmptyMessage(MSG_TIME_UPDATE);
        }

        mControlsManager.hiddenLoading();

        switchKeepScreenOnFlagsByState(Player.STATE_PLAYING);
        // refresh progress.
        mControlsManager.showMediaController();
        // when user call start,will be in playing state,
        // must set preIsInPlayState flag to false.
        mComponent.setPreIsInPlayingState(false);
    }

    protected void onPause() {
        if (mOnPauseListener != null) {
            mOnPauseListener.onPause();
        }
        if (mOnTimeUpdateListener != null) {
            mTimeUpdateHandler.removeMessages(MSG_TIME_UPDATE);
        }

        switchKeepScreenOnFlagsByState(Player.STATE_PAUSED);
        // when user call pause,controller always show.
        mControlsManager.showMediaControllerWidthTimeout(0);

        mComponent.setLastPosition(getCurrentPosition());
    }

    protected void onCompletion() {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion();
        }
        if (mOnTimeUpdateListener != null) {
            mTimeUpdateHandler.removeMessages(MSG_TIME_UPDATE);
        }
        switchKeepScreenOnFlagsByState(Player.STATE_PLAYBACK_COMPLETED);
        mControlsManager.showCompletion();
    }

    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    public void setOnIdleListener(OnIdleListener l) {
        mOnIdleListener = l;
    }

    public void setOnStartListener(OnStartListener l) {
        mOnStartListener = l;
    }

    public void setOnPreparingListener(OnPreparingListener l) {
        mOnPreparingListener = l;
    }

    public void setOnPreparedListener(OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    public void setOnPlayingListener(OnPlayingListener l) {
        mOnPlayingListener = l;
    }

    public void setOnPauseListener(OnPauseListener l) {
        mOnPauseListener = l;
    }

    public void setOnCompletionListener(OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    public void setOnTimeUpdateListener(OnTimeUpdateListener l) {
        mOnTimeUpdateListener = l;
    }

    @Override
    public void onLoadingChanged(IMediaPlayer player, boolean isLoading) {
        if (isLoading && mPlayer != null && mPlayer.getCurrentState() != Player.STATE_ERROR) {
            if (mOnTimeUpdateListener != null) {
                mTimeUpdateHandler.removeMessages(MSG_TIME_UPDATE);
            }
            mControlsManager.showLoading();
        } else {
            if (mOnTimeUpdateListener != null) {
                mTimeUpdateHandler.sendEmptyMessage(MSG_TIME_UPDATE);
            }
            mControlsManager.hiddenLoading();
        }
    }

    @Override
    public void onPlayerStateChanged(IMediaPlayer player, int playbackState) {
        switch (playbackState) {
            case Player.STATE_ERROR: {
                // do nothing, deal with onPlayerErrorEvent
                break;
            }
            case Player.STATE_IDLE: {
                onIdle();
                break;
            }
            case Player.STATE_PREPARING: {
                onPreparing();
                break;
            }
            case Player.STATE_PREPARED: {
                onPrepared();
                break;
            }
            case Player.STATE_PLAYING: {
                onPlaying();
                break;
            }
            case Player.STATE_PAUSED: {
                onPause();
                break;
            }
            case Player.STATE_PLAYBACK_COMPLETED: {
                onCompletion();
                break;
            }
            default:
                break;
        }
    }

    protected boolean onError(int what, int extra) {
        if (mOnTimeUpdateListener != null) {
            mTimeUpdateHandler.removeMessages(MSG_TIME_UPDATE);
        }

        if (mOnErrorListener != null) {
            mOnErrorListener.onError(what, extra);
        }

        switchKeepScreenOnFlagsByState(Player.STATE_ERROR);
        mControlsManager.showErrorMsg(what, extra);

        // save position at error state,for restore position while retry.
        if (mPlayer != null) {
            long currentPos = mPlayer.getCurrentPosition();
            if (currentPos > 0) {
                mComponent.setLastPosition(currentPos);
            }
        }
        return false;
    }

    @Override
    public boolean onError(IMediaPlayer player, int what, int extra) {
        return onError(what, extra);
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer player, int width, int height) {
        // nothing to do.
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // Play
                if (mPlayer != null
                        && getPreIsInPlayingState()
                        && mPlayer.getCurrentState() == Player.STATE_PAUSED
                        && !mComponent.isPaused()) {
                    start();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Pause
                if (mPlayer != null
                        && (mPlayer.getCurrentState() == Player.STATE_PLAYING
                        || mPlayer.getCurrentState() == Player.STATE_PREPARING
                        || mPlayer.getCurrentState() == Player.STATE_PREPARED)) {
                    setPreIsInPlayingState(true);
                    pause();
                }
                break;
            default:
                break;
        }
    }

    public void switchControlsVisibility(boolean visible) {
        mControlsVisible = visible;
        mControlsManager.switchControlsVisibility(visible);
    }

    public void enableStatusBar(boolean show) {
        if (isFullscreen()) {

            Display display =
                    ((DecorLayout) (mComponent.getRootComponent().getInnerView()))
                            .getDecorLayoutDisPlay();
            View statusBarView = display.getStatusBarView();

            if (show) {
                setSystemUiVisibility(
                        getSystemUiVisibility()
                                & (~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                                & (~View.SYSTEM_UI_FLAG_FULLSCREEN)
                                & (~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY));
                if (statusBarView != null) {
                    statusBarView.setVisibility(VISIBLE);
                }
            } else {
                setSystemUiVisibility(
                        getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                if (statusBarView != null) {
                    statusBarView.setVisibility(GONE);
                }
            }
        }
    }

    public void setVideoURI(Uri uri) {
        if ((uri == null && mUri == null) || (uri != null && uri.equals(mUri))) {
            return;
        }

        if (uri == null) {
            if (mPlayer != null && mControlsManager.mVideoView != null) {
                mPlayer.stop();
            }
        }
        mUri = uri;

        if (mIsFullScreen && null != mCacheFullScreenUri && !mCacheFullScreenUri.equals(mUri)) {
            Log.w(TAG, "setVideoURI mUri is not fullscreen Uri ,start invalid");
            return;
        }
        if (mAutoPlay && mUri != null) {
            initPlayer();
            makeEffectVideoURI(false);
            mPlayer.start();
        }
    }

    private void makeEffectVideoURI(boolean isNeedStart) {
        if (mUri == null || mPlayer == null) {
            Log.w(TAG, "makeEffectVideoURI mUri  or mPlayer is null.");
            return;
        }
        mControlsManager.createVideoLayout();
        if (mUri.equals(mPlayer.getDataSource())) {
            //when at error state or idle state,can start again.
            if (mPlayer.getCurrentState() == Player.STATE_ERROR
                    || mPlayer.getCurrentState() == Player.STATE_IDLE) {
                mPlayer.prepare();

            } else if (mPlayer.getCurrentState() == Player.STATE_PAUSED) {
                if (isNeedStart) {
                    mPlayer.prepare();
                } else {
                    Log.w(TAG, "makeEffectVideoURI same uri, mPlayer.getCurrentState()  : " + (null != mPlayer ? mPlayer.getCurrentState() : " null mPlayer")
                            + " isNeedStart false ");
                }
            } else {
                Log.w(TAG, "makeEffectVideoURI same uri, mPlayer.getCurrentState()  : " + (null != mPlayer ? mPlayer.getCurrentState() : " null mPlayer"));
            }
            //Avoid open video repeat,return
            return;
        }
        Uri uri = mUri;
        if (!mComponent.isPaused()) {
            mPlayer.setDataSource(uri);
            mPlayer.prepare();
        }

        // surface not ready yet,so set flag preIsInPlayingState true,
        // onAvailable will be call resume to reopen video view for start.
        if (mPlayer.getCurrentState() == Player.STATE_IDLE) {
            mComponent.setPreIsInPlayingState(true);
        }
    }

    @Nullable
    public View getPosterView() {
        return mControlsManager.mPosterView;
    }

    public Uri getPoster() {
        return mPosterUri;
    }

    public void setPoster(Uri posterUri) {
        if ((posterUri == null && mPosterUri == null)) {
            return;
        }

        if (posterUri != null && posterUri.equals(mPosterUri)) {
            return;
        }

        mPosterUri = posterUri;

        FlexImageView postView = mControlsManager.createPosterView();
        if (mPosterUri == null) {
            postView.setVisibility(GONE);
        } else {
            postView.setSource(mPosterUri);
            if (mPlayer == null || !mPlayer.isPlaying()) {
                postView.setVisibility(VISIBLE);
            }
        }
    }

    @Nullable
    public ProgressBar getProgressBar() {
        return mControlsManager.mProgressBar;
    }

    @Nullable
    public MediaController getMediaController() {
        return mControlsManager.mMediaController;
    }

    public void setMediaController(MediaController mediaController) {
        mControlsManager.setMediaController(mediaController);
    }

    @Nullable
    public TextureView getVideoView() {
        return mControlsManager.mVideoView;
    }

    @Nullable
    public ImageView getStartPauseView() {
        return mControlsManager.mBtnPlay;
    }

    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void onActivityResume() {
        if (mPlayer != null) {
            mPlayer.setSuspendBuffer(false);
            if (mComponent.getPreIsInPlayingState()) {
                start();
            }
        }
    }

    public void onActivityPaused() {
        if (mPlayer != null) {
            mPlayer.setSuspendBuffer(true);
            mComponent.setLastPosition(getCurrentPosition());
            if (mPlayer.isPlaying() || mPlayer.getCurrentState() == IMediaPlayer.STATE_PREPARING) {
                mComponent.setPreIsInPlayingState(true);
                if (mPlayer.getCurrentState() == IMediaPlayer.STATE_PREPARING) {
                    mPlayer.stop();
                }
            }
            mPlayer.pause();
        }
    }

    public void setCurrentTime(int position) {
        if (mPlayer == null || !mPlayer.isPlaying()) {
            mComponent.setLastPosition(position);
        } else {
            mPlayer.seek(position);
        }
    }

    public void setMuted(boolean muted) {
        mMuted = muted;
        if (mPlayer != null) {
            mPlayer.setMuted(muted);
        }
    }

    public long getCurrentPosition() {
        return (mPlayer != null) ? mPlayer.getCurrentPosition() : 0;
    }

    public IMediaPlayer getPlayer() {
        return mPlayer;
    }

    public boolean getPreIsInPlayingState() {
        return mComponent.getPreIsInPlayingState();
    }

    public void setPreIsInPlayingState(boolean isPreIsInPlayingState) {
        mComponent.setPreIsInPlayingState(isPreIsInPlayingState);
    }

    public void setOnSeekingListener(OnSeekingListener l) {
        mOnSeekingListener = l;
    }

    public void setOnSeekedListener(OnSeekedListener l) {
        mOnSeekedListener = l;
    }

    public void setOnFullscreenChangeListener(OnFullscreenChangeListener l) {
        mOnFullscreenChangeListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mPlayer != null) {
            int currentState = mPlayer.getCurrentState();
            if (currentState != Player.STATE_ERROR && currentState != Player.STATE_PREPARING) {

                if (!isParentScrollable() || isFullscreen()) {
                    if (mMediaGestureHelper == null) {
                        mMediaGestureHelper = new MediaGestureHelper(this, mPlayer);
                        mMediaGestureHelper.setGestureChangeListener(this);
                    }
                    result = mMediaGestureHelper.onTouch(event);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (currentState != Player.STATE_PAUSED
                            && currentState != Player.STATE_IDLE
                            && currentState != Player.STATE_PLAYBACK_COMPLETED) {
                        mControlsManager.toggleMediaControlsVisibility();
                    }
                }
            }
        }
        return result;
    }

    private boolean isInPlaybackState() {
        if (mPlayer != null) {
            int playerState = mPlayer.getCurrentState();
            return (playerState != IMediaPlayer.STATE_ERROR
                    && playerState != IMediaPlayer.STATE_IDLE
                    && playerState != IMediaPlayer.STATE_PREPARING);
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (mPlayer != null && isInPlaybackState()) {
            mControlsManager.toggleMediaControlsVisibility();
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mControlsManager.mMediaController != null
                && mControlsManager.mMediaController.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isFullscreen()
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.isTracking()
                && !event.isCanceled()) {
            exitFullscreen(getContext());
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isFullscreen() && keyCode == KeyEvent.KEYCODE_BACK && !event.isCanceled()) {
            event.startTracking();
            return true;
        }

        boolean isKeyCodeSupported =
                keyCode != KeyEvent.KEYCODE_BACK
                        && keyCode != KeyEvent.KEYCODE_VOLUME_UP
                        && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
                        && keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
                        && keyCode != KeyEvent.KEYCODE_MENU
                        && keyCode != KeyEvent.KEYCODE_CALL
                        && keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (BuildPlatform.isTV()
                && mControlsManager.mBtnPlay.getVisibility() == View.VISIBLE
                && mPlayer != null
                && isKeyCodeSupported
                && mControlsManager.mMediaController != null) {
            mControlsManager.mBtnPlay.performClick();
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        }

        if (mPlayer != null
                && isInPlaybackState()
                && isKeyCodeSupported
                && mControlsManager.mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                    || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                    mControlsManager.showMediaController();
                } else {
                    mPlayer.start();
                    mControlsManager.hideMediaController();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mPlayer.isPlaying()) {
                    mPlayer.start();
                    mControlsManager.hideMediaController();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                    mControlsManager.showMediaController();
                }
                return true;
            } else {
                mControlsManager.toggleMediaControlsVisibility();
            }
        }

        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    @Override
    public void onBrightnessChange(float newValue, float oldValue) {
        if (mRegisterBrightnessObserver) {
            return;
        }
        BrightnessUtils.BrightnessObserver observer =
                new BrightnessUtils.BrightnessObserver() {
                    @Override
                    public void onChange(boolean selfChange) {
                        WindowManager.LayoutParams params =
                                ((Activity) getContext()).getWindow().getAttributes();
                        if (params.screenBrightness
                                != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                            params.screenBrightness =
                                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                            ((Activity) getContext()).getWindow().setAttributes(params);
                            BrightnessUtils.unregisterOberver(getContext(), this);
                            mRegisterBrightnessObserver = false;
                        }
                    }
                };
        BrightnessUtils.registerOberver(getContext(), observer);
        mRegisterBrightnessObserver = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mExitingFullscreen) {
            AppearanceHelper.mWatchEnabled = true;
            mExitingFullscreen = false;
        }
    }

    private boolean isParentScrollable() {
        if (mComponent == null) {
            return true;
        }
        if (mIsLazyCreate) {
            return true;
        }
        Component parentComponent = mComponent.getParent();
        ViewGroup parentView = (ViewGroup) parentComponent.getHostView();
        while (!(parentView instanceof ScrollView)
                && (parentView.getParent() instanceof ViewGroup)) {
            parentView = (ViewGroup) parentView.getParent();
        }
        if (parentView instanceof ScrollView && parentView.getChildCount() > 0) {
            View childView = parentView.getChildAt(0);
            if (childView.getMeasuredHeight() > parentView.getMeasuredHeight()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFullscreen() {
        return mFullscreenState == STATE_FULLSCREEN;
    }

    /**
     * getParentList components
     */
    public Component getParentList() {
        Component component = mComponent;
        if (component == null) {
            return null;
        }
        Component container;
        for (; ; ) {
            container = component.getParent();
            if (container == null) {
                return null;
            }
            if (container instanceof List) {
                return container;
            }
            component = container;
        }
    }

    private void initUseCacheItem(boolean isSelfReset) {
        Component tmpList = getParentList();
        if (tmpList instanceof List) {
            View tmpHostView = tmpList.getHostView();
            FlexRecyclerView flexRecyclerView = null;
            RecyclerView.LayoutManager layoutManager = null;
            FlexGridLayoutManager flexGridLayoutManager = null;
            if (tmpHostView instanceof FlexRecyclerView) {
                flexRecyclerView = ((FlexRecyclerView) tmpHostView);
                layoutManager = flexRecyclerView.getLayoutManager();
            }
            if (layoutManager instanceof FlexGridLayoutManager) {
                flexGridLayoutManager = ((FlexGridLayoutManager) layoutManager);
            }
            if (null != flexGridLayoutManager) {
                flexGridLayoutManager.setIsUseCacheItem(true, isSelfReset);
            } else {
                Log.w(TAG, "initUseCacheItem flexGridLayoutManager is null.");
            }
        } else {
            Log.w(TAG, "initUseCacheItem tmpList is not List.");
        }
    }

    private void enterFullscreen(int screenOrientation) {
        if (isFullscreen()) {
            return;
        }
        initUseCacheItem(false);
        mFullscreenState = STATE_ENTERING_FULLSCREEN;
        final TextureVideoView video = mControlsManager.createVideoLayout();
        video.setShouldReleaseSurface(false);

        AppearanceHelper.mWatchEnabled = false;

        // 隐藏虚拟按键
        View decorView;
        if (getScreenOrientaion() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            decorView = ((Activity) getContext()).getWindow().getDecorView();
            int uiOptions =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        } else if (getScreenOrientaion() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            (((Activity) getContext()).getWindow())
                    .clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            decorView = ((Activity) getContext()).getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        boolean enableFullScreenContainer = false;
        if (mComponent.getParent() instanceof Div) {
            enableFullScreenContainer =
                    ((Div) mComponent.getParent()).enableVideoFullscreenContainer();
        }

        getComponent()
                .getRootComponent()
                .enterFullscreen(mComponent, screenOrientation, enableFullScreenContainer);

        onEnterFullscreen();

        setFocusableInTouchMode(true);
        requestFocus();
        if (mOnFullscreenChangeListener != null) {
            mOnFullscreenChangeListener.onFullscreenChange(true);
        }
        mFullscreenState = STATE_FULLSCREEN;
        mCacheFullScreenUri = mUri;
        mIsFullScreen = true;
    }

    public void requestFullscreen(int screenOrientation) {
        if (mControlsManager.mVideoView != null) {
            enterFullscreen(screenOrientation);
        }
    }

    private void exitFullscreen(Context context) {
        if (!isFullscreen()) {
            return;
        }
        mCacheFullScreenUri = null;
        mIsFullScreen = false;
        mFullscreenState = STATE_EXITING_FULLSCREEN;
        mIsFirstExitFullScreenAttach = true;
        initUseCacheItem(true);
        final TextureVideoView video = mControlsManager.createVideoLayout();
        video.setShouldReleaseSurface(false);

        getComponent().getRootComponent().exitFullscreen();

        onExitFullscreen();

        if (mOnFullscreenChangeListener != null) {
            mOnFullscreenChangeListener.onFullscreenChange(false);
        }
        mFullscreenState = STATE_NOT_FULLSCREEN;
        mExitingFullscreen = true;
    }

    public void exitFullscreen() {
        if (mControlsManager.mVideoView != null) {
            exitFullscreen(getContext());
        }
    }

    public void switchKeepScreenOnFlagsByState(int state) {
        switch (state) {
            case Player.STATE_IDLE:
            case Player.STATE_ERROR:
            case Player.STATE_PAUSED:
            case Player.STATE_PREPARING:
            case Player.STATE_PLAYBACK_COMPLETED: {
                final Set<Integer> screenOnFlags = sKeepScreenOnFlag;
                final int ref = mComponent.getRef();
                if (!screenOnFlags.contains(ref)) {
                    return;
                }
                if (screenOnFlags.size() == 1) {
                    ((Activity) getContext())
                            .getWindow()
                            .clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                screenOnFlags.remove(ref);
                break;
            }
            case Player.STATE_PREPARED:
            case Player.STATE_PLAYING: {
                final Set<Integer> screenOnFlags = sKeepScreenOnFlag;
                screenOnFlags.add(mComponent.getRef());
                ((Activity) getContext())
                        .getWindow()
                        .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            }
            default:
                break;
        }
    }

    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
        if (mAutoPlay && null != mUri && null == mPlayer) {
            initPlayer();
            if (mIsFullScreen && null != mCacheFullScreenUri && !mUri.equals(mCacheFullScreenUri)) {
                Log.w(TAG, "setAutoPlay mUri is not fullscreen Uri ,setAutoPlay invalid");
                return;
            }
            makeEffectVideoURI(false);
        }
    }

    public void setPlayCount(String playCount) {
        mPlayCount = playCount;
        if (mPlayer != null) {
            mPlayer.setPlayCount(playCount);
        }
    }

    public void setIsLazyCreate(boolean lazyCreate) {
        mIsLazyCreate = lazyCreate;
    }

    @Override
    public void onSurfaceTextureAvailable() {
        if (mComponent.getPreIsInPlayingState() && !mComponent.isPaused()) {
            initPlayer();
            mPlayer.start();
            mComponent.setPreIsInPlayingState(false);
        } else {
            Log.w(TAG, "onSurfaceTextureAvailable  mPlayer.getCurrentState()  : " + (null != mPlayer ? mPlayer.getCurrentState() : " null mPlayer"));
        }
    }

    @Override
    public void onSurfaceTextureDestroyed() {
        if (mPlayer != null
                && (mPlayer.isPlaying()
                || mPlayer.getCurrentState() == IMediaPlayer.STATE_PREPARING)) {
            mComponent.setPreIsInPlayingState(true);
            mComponent.setLastPosition(getCurrentPosition());
        }

        if (mPlayer != null) {
            mPlayer.release();
        }
    }

    private void onEnterFullscreen() {
        if (mControlsManager.mMediaController != null) {
            mControlsManager.mMediaController.enterFullscreen();
        }
    }

    private void onExitFullscreen() {
        if (mControlsManager.mMediaController != null) {
            mControlsManager.mMediaController.exitFullscreen();
        }
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = (Video) component;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mGesture != null) {
            mGesture.onTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setObjectFit(String objectFit) {
        if (getVideoView() instanceof TextureVideoView) {
            ((TextureVideoView) getVideoView()).setObjectFit(objectFit);
        }
        if (getPosterView() instanceof FlexImageView) {
            ((FlexImageView) getPosterView()).setObjectFit(objectFit);
        }
    }

    public void setScreenOrientation(String orientation) {
        if (Page.ORIENTATION_PORTRAIT.equals(orientation)) {
            mScreenOrientaion = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else if (Page.ORIENTATION_LANDSCAPE.equals(orientation)) {
            mScreenOrientaion = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        }
    }

    public int getScreenOrientaion() {
        return mScreenOrientaion;
    }

    public void setTitle(String title) {
        mControlsManager.createMediaControllerView();
        if (mControlsManager.mMediaController != null) {
            mControlsManager.mMediaController.setTitle(title);
        }
    }

    public void setTitleBarEnabled(boolean titleBarEnabled) {
        mControlsManager.createMediaControllerView();
        if (mControlsManager.mMediaController != null) {
            mControlsManager.mMediaController.setTitleBarEnabled(titleBarEnabled);
        }
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
        }
    }

    public void initPlayer() {
        if (mPlayer == null) {
            mPlayer = PlayerInstanceManager.getInstance().createMediaPlayer();
            if (mPlayCount != null) {
                mPlayer.setPlayCount(mPlayCount);
            }
            if (mMuted != null) {
                mPlayer.setMuted(mMuted);
            }
            mPlayer.setSpeed(mSpeed);
            mPlayer.setDataSource(mUri);
            mPlayer.setEventListener(this);
            mControlsManager.attachPlayer(mPlayer);
        }
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
        if (mPlayer != null) {
            mPlayer.setSpeed(speed);
        }
    }

    public interface OnErrorListener {
        boolean onError(int what, int extra);
    }

    public interface OnIdleListener {
        void onIdle();
    }

    public interface OnStartListener {
        void onStart();
    }

    public interface OnPreparingListener {
        void onPreparing();
    }

    public interface OnPreparedListener {
        void onPrepared(IMediaPlayer mp);
    }

    public interface OnPlayingListener {
        void onPlaying();
    }

    public interface OnPauseListener {
        void onPause();
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public interface OnTimeUpdateListener {
        void onTimeUpdate();
    }

    public interface OnSeekingListener {
        void onSeeking(long position);
    }

    public interface OnSeekedListener {
        void onSeeked(long position);
    }

    public interface OnFullscreenChangeListener {
        void onFullscreenChange(boolean fullscreen);
    }

    private class ControlsManager {

        @Nonnull
        private final FrameLayout mControlsLayout;
        @Nullable
        private TextureVideoView mVideoView;
        @Nullable
        private FlexImageView mPosterView;
        @Nullable
        private ProgressBar mProgressBar;
        @Nullable
        private MediaController mMediaController;
        @Nullable
        private TextView mTvErrorMsg;
        @Nullable
        private LinearLayout mErrorLayout;
        @Nullable
        private Button mBtnRetry;
        @Nullable
        private ImageView mBtnPlay;
        @Nonnull
        private ViewGroup mRootView;
        private boolean mVisible;

        ControlsManager(boolean visible) {
            mVisible = visible;
            mRootView =
                    (ViewGroup)
                            LayoutInflater.from(getContext())
                                    .inflate(R.layout.video_layout, FlexVideoView.this, true);
            mControlsLayout = mRootView.findViewById(R.id.controls_layout);
            if (visible) {
                createPlayView();
                createLoadingProgressView();
            }
            createVideoLayout();
        }

        public boolean isControlsLayoutVisible() {
            boolean isVisible = false;
            if (null != mControlsLayout && mControlsLayout.getVisibility() == View.VISIBLE) {
                isVisible = true;
            }
            return isVisible;
        }

        void attachPlayer(IMediaPlayer player) {
            if (mVideoView != null) {
                mVideoView.attachPlayer(player);
            }

            if (mMediaController != null) {
                mMediaController.setMediaPlayer(player);
            }
        }

        private void switchControlsVisibility(boolean visible) {
            mVisible = visible;
            if (visible) {
                mControlsLayout.setVisibility(VISIBLE);
            } else {
                mControlsLayout.setVisibility(GONE);
            }
            if (isMediaControllerShowing() && mMediaController != null) {
                mMediaController.refresh();
            }
        }

        private void initializeControlsView() {
            if (mBtnPlay != null) {
                mBtnPlay.setImageResource(R.drawable.ic_media_star_video);
                mBtnPlay.setVisibility(VISIBLE);
            } else {
                Log.w(TAG, "initializeControlsView mBtnPlay is null.");
            }
            if (mProgressBar != null) {
                mProgressBar.setVisibility(GONE);
            }
            if (mTvErrorMsg != null) {
                mTvErrorMsg.setVisibility(GONE);
            }
            if (mBtnRetry != null) {
                mBtnRetry.setVisibility(GONE);
            }
            if (mPosterView != null) {
                mPosterView.setVisibility(VISIBLE);
            }
            hideMediaController();
        }

        void detachPlayer() {
            if (mVideoView != null) {
                mVideoView.detachPlayer();
            }

            if (mMediaController != null) {
                mMediaController.clearMediaController();
            }
            onIdle();
        }

        private boolean isMediaControllerShowing() {
            return mMediaController != null && mMediaController.isShowing();
        }

        private void showMediaController() {
            if (!mVisible) {
                return;
            }
            createMediaControllerView();
            if (mMediaController != null) {
                mMediaController.show();
            }
        }

        private void showMediaControllerWidthTimeout(int timeout) {
            if (!mVisible) {
                return;
            }
            createMediaControllerView();
            if (mMediaController != null) {
                mMediaController.show(timeout);
            }
        }

        private void hideMediaController() {
            if (mMediaController != null) {
                mMediaController.hide();
            }
        }

        private void toggleMediaControlsVisibility() {
            if (isMediaControllerShowing()) {
                hideMediaController();
            } else {
                showMediaController();
            }
        }

        private void showCompletion() {
            if (!mVisible) {
                return;
            }

            if (mBtnPlay != null) {
                mBtnPlay.setVisibility(GONE);
            }
            if (mProgressBar != null) {
                mProgressBar.setVisibility(GONE);
            }
            // mPlayer.seek(0);
            // always show controller.
            showMediaControllerWidthTimeout(0);

            if (mPosterView != null) {
                mPosterView.setVisibility(VISIBLE);
            }
        }

        private void showLoading() {
            if (!mVisible) {
                return;
            }
            if (mTvErrorMsg != null) {
                mTvErrorMsg.setVisibility(GONE);
            }
            if (mBtnRetry != null) {
                mBtnRetry.setVisibility(GONE);
            }
            if (mBtnPlay != null) {
                mBtnPlay.setVisibility(GONE);
            }
            createLoadingProgressView().setVisibility(VISIBLE);
        }

        private void hiddenLoading() {
            if (mProgressBar != null) {
                mProgressBar.setVisibility(GONE);
            }
            if (mBtnPlay != null) {
                mBtnPlay.setVisibility(GONE);
            }
            if (mPosterView != null) {
                mPosterView.setVisibility(GONE);
            }
        }

        private void showErrorMsg(int what, int extra) {
            if (!mVisible) {
                return;
            }
            createPlayView().setVisibility(VISIBLE);
            Resources r = getContext().getResources();
            int messageId;
            if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                messageId = android.R.string.VideoView_error_text_invalid_progressive_playback;
            } else {
                messageId = android.R.string.VideoView_error_text_unknown;
            }
            String message = r.getString(messageId);

            TextView tvErrMsg = createErrorMsgView();
            tvErrMsg.setText(message);
            tvErrMsg.setVisibility(VISIBLE);

            Button retry = createRetryView();
            retry.setText(r.getString(R.string.media_contorls_retry));
            retry.setVisibility(VISIBLE);

            if (mProgressBar != null) {
                mProgressBar.setVisibility(GONE);
            }
            if (mBtnPlay != null) {
                mBtnPlay.setVisibility(GONE);
            }
            if (mPosterView != null) {
                mPosterView.setVisibility(VISIBLE);
            }
        }

        private void setMediaController(MediaController mediaController) {
            mMediaController = mediaController;
        }

        private TextureVideoView createVideoLayout() {
            if (mVideoView == null) {
                mVideoView = new TextureVideoView(getContext());
                ViewStub stub = mRootView.findViewById(R.id.stub_texture_view_layout);
                replaceViewStubWithView(stub, mVideoView, mRootView);
            }
            mVideoView.setSurfaceTextureListener(FlexVideoView.this);
            mVideoView.attachPlayer(mPlayer);
            return mVideoView;
        }

        private MediaController createMediaControllerView() {
            if (mMediaController == null) {
                mMediaController = new MediaController(getContext());
                mMediaController.setVideoView(FlexVideoView.this);
                ViewStub stub = mControlsLayout.findViewById(R.id.stub_controller_layout);
                replaceViewStubWithView(stub, mMediaController, mControlsLayout);

                if (isFullscreen()) {
                    mMediaController.enterFullscreen();
                } else {
                    mMediaController.exitFullscreen();
                }
                mMediaController.setFullscreenChangeListener(
                        new MediaController.FullscreenChangeListener() {
                            @Override
                            public void onChange() {
                                if (isFullscreen()) {
                                    exitFullscreen(getContext());
                                } else {
                                    enterFullscreen(mScreenOrientaion);
                                }
                            }
                        });
                mMediaController.setOnSeekBarChangeListener(
                        new MediaController.OnSeekBarChangeListener() {
                            @Override
                            public void onSeeking(long position) {
                                if (mOnSeekingListener != null) {
                                    mOnSeekingListener.onSeeking(position);
                                }
                            }

                            @Override
                            public void onSeeked(long position) {
                                if (mOnSeekedListener != null) {
                                    mOnSeekedListener.onSeeked(position);
                                }
                            }
                        });
                mMediaController.setMediaPlayer(mPlayer);
                mMediaController.setVisibility(GONE);
            }

            return mMediaController;
        }

        private FlexImageView createPosterView() {
            if (mPosterView == null) {
                mPosterView = new FlexImageView(getContext());
                mPosterView.setObjectFit(
                        Attributes.ObjectFit.CONTAIN); // 与 video 组件 object-fit 默认值保持一致
                ViewStub stub = mRootView.findViewById(R.id.stub_poster_layout);
                replaceViewStubWithView(stub, mPosterView, mRootView);
            }
            return mPosterView;
        }

        private ImageView createPlayView() {
            if (mBtnPlay == null) {
                mBtnPlay = new FlexImageView(getContext());
                mBtnPlay.setImageResource(R.drawable.ic_media_star_video);
                mBtnPlay.setScaleType(ImageView.ScaleType.CENTER);
                mBtnPlay.setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                start();
                            }
                        });
                ViewStub stub = mControlsLayout.findViewById(R.id.stub_play_layout);
                replaceViewStubWithView(stub, mBtnPlay, mControlsLayout);
            }
            return mBtnPlay;
        }

        private ProgressBar createLoadingProgressView() {
            if (mProgressBar == null) {
                mProgressBar = new ProgressBar(getContext());
                mProgressBar.setIndeterminateDrawable(
                        getContext().getResources().getDrawable(R.drawable.pg_media_loading));
                mProgressBar.setVisibility(GONE);
                ViewStub stub = mControlsLayout.findViewById(R.id.stub_loading_layout);
                replaceViewStubWithView(stub, mProgressBar, mControlsLayout);
            }
            return mProgressBar;
        }

        private TextView createErrorMsgView() {
            if (mTvErrorMsg == null) {
                mTvErrorMsg = createErrorMsgLayout().findViewById(R.id.error_msg);
            }
            return mTvErrorMsg;
        }

        private Button createRetryView() {
            if (mBtnRetry == null) {
                mBtnRetry = createErrorMsgLayout().findViewById(R.id.btn_retry);
                if (mBtnRetry != null) {
                    mBtnRetry.setOnClickListener(
                            new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    start();
                                }
                            });
                }
            }
            return mBtnRetry;
        }

        private LinearLayout createErrorMsgLayout() {
            if ((mErrorLayout == null) && inflateViewStub(R.id.stub_error_layout)) {
                mErrorLayout = mRootView.findViewById(R.id.error_layout);
            }
            return mErrorLayout;
        }

        private boolean inflateViewStub(int id) {
            ViewStub stub = mRootView.findViewById(id);
            if (stub != null) {
                stub.inflate();
                return true;
            }
            return false;
        }

        private void replaceViewStubWithView(ViewStub stub, View view, ViewGroup parent) {
            final int index = parent.indexOfChild(stub);
            parent.removeViewInLayout(stub);
            view.setId(stub.getInflatedId());
            final ViewGroup.LayoutParams layoutParams = stub.getLayoutParams();
            if (layoutParams != null) {
                parent.addView(view, index, layoutParams);
            } else {
                parent.addView(view, index);
            }
        }
    }
}