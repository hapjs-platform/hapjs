/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.video;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.viewpager.widget.ViewPager;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.model.videodata.VideoCacheData;
import org.hapjs.model.videodata.VideoCacheManager;
import org.hapjs.widgets.R;
import org.hapjs.widgets.video.ExoPlayer;
import org.hapjs.widgets.video.IMediaPlayer;
import org.hapjs.widgets.video.MediaUtils;
import org.hapjs.widgets.video.Player;

/**
 * Helper for implementing media controls in an application. Use instead of the very useful
 * android.widget.MediaController. This version is embedded inside of an application's layout.
 */
public class MediaController extends RelativeLayout {

    private static final int sDefaultTimeout = 3000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private final Context mContext;
    private IMediaPlayer mPlayer;
    private View mRoot;
    private ProgressBar mProgress;
    private TextView mEndTime;
    private TextView mCurrentTime;
    private boolean mDragging;
    private ImageButton mPauseButton;
    private ImageButton mFullButton;
    private FlexVideoView mVideoView;
    private final Handler mHandler =
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    long pos;
                    switch (msg.what) {
                        case FADE_OUT:
                            hide();
                            break;
                        case SHOW_PROGRESS:
                            if (null == mPlayer) {
                                Log.w(TAG, "handleMessage SHOW_PROGRESS mPlayer is null.");
                                return;
                            }
                            pos = setProgress();
                            if (mPlayer != null && mPlayer.isPlaying() && !mDragging
                                    && isShowing()) {
                                msg = obtainMessage(SHOW_PROGRESS);
                                sendMessageDelayed(msg, 1000 - (pos % 1000));
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
    private final OnTouchListener mTouchListener =
            new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (isShowing()) {
                            hide();
                        }
                    }
                    return false;
                }
            };
    private final View.OnClickListener mPauseListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doPauseResume();
                    if (null == mPlayer) {
                        Log.w(TAG, "mPauseListener onClick mPlayer is null.");
                        return;
                    }
                    if (mPlayer.isPlaying()) {
                        show(sDefaultTimeout);
                    } else {
                        show(0);
                    }
                }
            };
    private View mTitleContainer;
    private View mTitleBarContainer;
    private TextView mTitle;
    private int mTitleHeight;
    private final String TAG = "MediaController";
    private FullscreenChangeListener mFullScreenChangeListener;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private final SeekBar.OnSeekBarChangeListener mSeekListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar bar) {
                    show(0);

                    mDragging = true;

                    // By removing these pending progress messages we make sure
                    // that a) we won't update the progress while the user adjusts
                    // the seekbar and b) once the user is done dragging the thumb
                    // we will post one of these messages to the queue again and
                    // this ensures that there will be exactly one message queued up.
                    mHandler.removeMessages(SHOW_PROGRESS);
                }

                @Override
                public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                    if (!fromuser) {
                        // We're not interested in programmatically generated changes to
                        // the progress bar's position.
                        return;
                    }
                    if (null == mPlayer) {
                        Log.w(TAG, "onProgressChanged  mPlayer is null.");
                        return;
                    }
                    long duration = mPlayer.getDuration();
                    // avoid overflow
                    long newPosition = (duration / 1000) * progress;
                    if (mCurrentTime != null) {
                        mCurrentTime.setText(MediaUtils.stringForTime(newPosition));
                    }
                    if (mOnSeekBarChangeListener != null) {
                        mOnSeekBarChangeListener.onSeeking(newPosition);
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar bar) {
                    if (null == mPlayer) {
                        Log.w(TAG, "onStopTrackingTouch  mPlayer is null.");
                        return;
                    }
                    mDragging = false;
                    // avoid overflow
                    long duration = mPlayer.getDuration();
                    long newPosition = (duration / 1000) * bar.getProgress();
                    mPlayer.seek(newPosition);
                    if (!mPlayer.isPlaying()) {
                        mPlayer.start();
                    }
                    updatePausePlay();
                    setProgress();
                    if (mPlayer.isPlaying()) {
                        show(sDefaultTimeout);
                    } else {
                        show(0);
                    }
                    // Ensure that progress is properly updated in the future,
                    // the call to show() does not guarantee this because it is a
                    // no-op if we are already showing.
                    mHandler.sendEmptyMessage(SHOW_PROGRESS);

                    if (mOnSeekBarChangeListener != null) {
                        mOnSeekBarChangeListener.onSeeked(newPosition);
                    }
                }
            };

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = this;
        mContext = context;
        makeControllerView();
    }

    public MediaController(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        makeControllerView();
    }

    public MediaController(Context context) {
        this(context, true);
    }

    public void setMediaPlayer(IMediaPlayer player) {
        mPlayer = player;
        if (player != null) {
            updatePausePlay();
        }
    }

    public void clearMediaController() {
        mPlayer = null;
    }

    /**
     * Create the view that holds the widgets that control playback. Derived classes can override this
     * to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, this, true);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }
        mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);

        mFullButton = (ImageButton) v.findViewById(R.id.full_screen);
        // set TV UI
        if (BuildPlatform.isTV()) {
            mFullButton.setVisibility(GONE);
            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) mEndTime.getLayoutParams();
            layoutParams.rightMargin += DisplayUtil.dip2Pixel(getContext(), 20);
            mEndTime.setLayoutParams(layoutParams);
        }
        mFullButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mFullScreenChangeListener != null) {
                            mFullScreenChangeListener.onChange();
                        }
                    }
                });

        mTitleContainer = v.findViewById(R.id.title_container);
        mTitleBarContainer = v.findViewById(R.id.title_bar_container);
        v.findViewById(R.id.back_arrow).setOnClickListener(view -> mVideoView.exitFullscreen());
        mTitle = v.findViewById(R.id.title);
    }

    private void applyButtonVisibility() {
        if (null == mPlayer) {
            Log.w(TAG, "applyButtonVisibility  mPlayer is null.");
            return;
        }
        boolean canSeek = mPlayer.isSeekable();
        int visibility = canSeek ? VISIBLE : INVISIBLE;
        if (mProgress != null) {
            mProgress.setVisibility(visibility);
        }
        if (mCurrentTime != null) {
            mCurrentTime.setVisibility(visibility);
        }
        if (mEndTime != null) {
            mEndTime.setVisibility(visibility);
        }
    }

    /**
     * Show the controller on screen. It will go away automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Show the controller on screen. It will go away automatically after 'timeout' milliseconds of
     * inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show the controller until hide() is
     *                called.
     */
    public void show(int timeout) {
        if (mVideoView != null) {
            mVideoView.enableStatusBar(true);
        }

        if (!isShowing()) {
            setVisibility(VISIBLE);
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            applyButtonVisibility();
        }
        updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        mHandler.removeMessages(FADE_OUT);
        if (timeout != 0) {
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return getVisibility() == View.VISIBLE;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mVideoView != null) {
            mVideoView.enableStatusBar(false);
        }

        if (isShowing()) {
            try {
                mHandler.removeMessages(SHOW_PROGRESS);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            setVisibility(GONE);
        }
    }

    public void refresh() {
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    public void setVideoView(FlexVideoView videoView) {
        mVideoView = videoView;
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setTitleBarEnabled(boolean titleBarEnabled) {
        mTitleContainer.setVisibility(titleBarEnabled ? VISIBLE : GONE);
    }

    private long setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
        VideoCacheData videoCacheData = null;
        if (position <= 0 || duration == ExoPlayer.DURATION_NONE) {
            if (null != mVideoView) {
                Component tmpComponent = mVideoView.getComponent();
                String videoUriStr = "";
                int pageId = -1;
                if (null != tmpComponent) {
                    pageId = tmpComponent.getPageId();
                }
                if (null != mVideoView.mUri) {
                    videoUriStr = mVideoView.mUri.toString();
                }
                if (pageId != -1 && !TextUtils.isEmpty(videoUriStr)) {
                    videoCacheData = VideoCacheManager.getInstance().getVideoData(pageId, videoUriStr);
                }
            }
            if (null != videoCacheData) {
                if (videoCacheData.lastPosition >= 0) {
                    position = videoCacheData.lastPosition;
                }
                if (videoCacheData.duration > 0) {
                    duration = videoCacheData.duration;
                }
            }
        }
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null) {
            mEndTime.setText(MediaUtils.stringForTime(duration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(MediaUtils.stringForTime(position));
        }

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());
        int childCount = getChildCount();
        Log.d("MediaController", "child Count:" + childCount);
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (x >= child.getLeft()
                    && x <= child.getRight()
                    && y >= child.getTop()
                    && y <= child.getBottom()) {
                Log.d("MediaController", "touched:" + child);
                return true;
            }
        }
        Log.d("MediaController", "touched outside.");
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                ViewParent parent = getHScrollParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE: {
                ViewParent parent = getHScrollParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                break;
            }
            default:
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    private ViewParent getHScrollParent() {
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof ViewPager) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown =
                event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (null == mPlayer) {
                Log.w(TAG, "dispatchKeyEvent KEYCODE_MEDIA_PLAY mPlayer is null.");
                return true;
            }
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (null == mPlayer) {
                Log.w(TAG, "dispatchKeyEvent KEYCODE_MEDIA_STOP or  KEYCODE_MEDIA_PAUSE mPlayer is null.");
                return true;
            }
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_MINUS
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_PLUS
                || keyCode == KeyEvent.KEYCODE_EQUALS) {
            if (null == mPlayer) {
                Log.w("MediaController", "dispatchKeyEvent while mPlayer is null");
                return true;
            }
            long increment = mPlayer.getDuration() / 20;
            long position = mPlayer.getCurrentPosition();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MINUS:
                    if (position == mPlayer.getDuration()) {
                        mPlayer.setCurrentState(Player.STATE_PREPARED);
                    }
                    position = Math.max(position - increment, 0);
                    mPlayer.seek(position);
                    if (!mPlayer.isPlaying() && position != 0) {
                        mPlayer.start();
                    }
                    show(sDefaultTimeout);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_PLUS:
                case KeyEvent.KEYCODE_EQUALS:
                    position = Math.min(position + increment, mPlayer.getDuration());
                    mPlayer.seek(position);
                    if (!mPlayer.isPlaying() && position != mPlayer.getDuration()) {
                        mPlayer.start();
                    }
                    show(sDefaultTimeout);
                    return true;
                default:
                    return false;
            }
        }
        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.ic_media_pause);
        } else {
            mPauseButton.setImageResource(R.drawable.ic_media_play);
        }
    }

    private void doPauseResume() {
        if (null == mPlayer) {
            Log.w(TAG, "doPauseResume  mPlayer is null.");
            return;
        }
        if (mPlayer.isPlaying()) {
            if (mVideoView != null) {
                mVideoView.setPauseButtonPress(true);
            }
            mPlayer.pause();
            if (mVideoView != null) {
                mVideoView.setPauseButtonPress(false);
            }
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        applyButtonVisibility();
        super.setEnabled(enabled);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.MediaController.class.getName();
    }

    public void setFullscreenChangeListener(FullscreenChangeListener l) {
        mFullScreenChangeListener = l;
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    public void enterFullscreen() {
        mFullButton.setImageResource(R.drawable.ic_media_exit_fullscreen);

        mTitleBarContainer.setVisibility(VISIBLE);
        View statusBarPlaceholder = mTitleBarContainer.findViewById(R.id.status_bar_bg);
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) statusBarPlaceholder.getLayoutParams();
        int statusBarHeight = DisplayUtil.getStatusBarHeight(getContext());
        if (mTitleHeight == 0) {
            mTitleHeight = params.height = statusBarHeight;
        }
        // refresh status bar need delay time
        mVideoView.getComponent().getRootComponent().setLightStatusBar(false);
        show(mPlayer != null && mPlayer.isPlaying() ? sDefaultTimeout : 0);
    }

    public void exitFullscreen() {
        mFullButton.setImageResource(R.drawable.ic_media_enter_fullscreen);

        mTitleBarContainer.setVisibility(GONE);
        mVideoView.getComponent().getRootComponent().resetStatusBar();
    }

    public interface FullscreenChangeListener {
        void onChange();
    }

    public interface OnSeekBarChangeListener {
        void onSeeking(long position);

        void onSeeked(long position);
    }
}