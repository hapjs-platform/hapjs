/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.video;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.hapjs.widgets.R;
import org.hapjs.widgets.video.IMediaPlayer;
import org.hapjs.widgets.video.MediaUtils;
import org.hapjs.widgets.video.Player;

public class MediaGestureHelper {

    public static final int THRESHOLD = 80;
    public static final int START_Y = 15;
    private static final String TAG = "MediaGestureHelper";
    private static final int MAX_SEEK_POSITION = 300000;
    private static final float CHANGE_FACTOR = 0.8f;
    private final View mHostView;
    private final IMediaPlayer mPlayer;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;
    protected AudioManager mAudioManager;
    // progres dialog
    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;
    // volume dialog
    protected Dialog mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;
    protected TextView mDialogVolumeTextView;
    protected ImageView mDialogVolumeImageView;
    // brightness dilaog
    protected Dialog mBrightnessDialog;
    protected ProgressBar mDialogBrightnessProgressBar;
    protected TextView mDialogBrightnessTextView;
    private float mVideoViewWidth;
    private float mVideoViewHeight;
    private MediaGestureChangeListener mGestureChangeListener;

    public MediaGestureHelper(View hostView, IMediaPlayer player) {
        mHostView = hostView;
        mAudioManager =
                (AudioManager) hostView.getContext().getSystemService(Context.AUDIO_SERVICE);
        mPlayer = player;
    }

    public void setGestureChangeListener(MediaGestureChangeListener l) {
        mGestureChangeListener = l;
    }

    public boolean onTouch(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();
        float rawY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchingProgressBar = true;
                mDownX = x;
                mDownY = y;
                if (rawY < START_Y) {
                    return false;
                }
                mChangeVolume = false;
                mChangePosition = false;
                mChangeBrightness = false;
                mVideoViewWidth = mHostView.getMeasuredWidth();
                mVideoViewHeight = mHostView.getMeasuredHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - mDownX;
                float deltaY = y - mDownY;
                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);
                if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                    if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                        if (absDeltaX >= THRESHOLD) {
                            if (canChangePosition()) {
                                mChangePosition = true;
                                mGestureDownPosition = mPlayer.getCurrentPosition();
                            }
                        } else {
                            if (mDownX < mVideoViewWidth * 0.5f) {
                                mChangeBrightness = true;
                                WindowManager.LayoutParams lp =
                                        ((Activity) mHostView.getContext()).getWindow()
                                                .getAttributes();
                                if (lp.screenBrightness < 0) {
                                    try {
                                        mGestureDownBrightness =
                                                Settings.System.getInt(
                                                        mHostView.getContext().getContentResolver(),
                                                        Settings.System.SCREEN_BRIGHTNESS);
                                    } catch (Settings.SettingNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    mGestureDownBrightness = lp.screenBrightness * 255;
                                }
                            } else {
                                mChangeVolume = true;
                                mGestureDownVolume =
                                        mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            }
                        }
                    }
                    break;
                }
                if (mChangePosition) {
                    long totalTimeDuration = mPlayer.getDuration();
                    long maxSeekPosition = Math.min(MAX_SEEK_POSITION, totalTimeDuration);
                    mSeekTimePosition =
                            (int) (mGestureDownPosition
                                    + deltaX * maxSeekPosition / mVideoViewWidth);
                    if (mSeekTimePosition > totalTimeDuration) {
                        mSeekTimePosition = totalTimeDuration;
                    } else if (mSeekTimePosition < 0) {
                        mSeekTimePosition = 0;
                    }
                    String seekTime = MediaUtils.stringForTime(mSeekTimePosition);
                    String totalTime = MediaUtils.stringForTime(totalTimeDuration);
                    showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime,
                            totalTimeDuration);
                }
                if (mChangeVolume) {
                    deltaY = -deltaY;
                    int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int deltaV = (int) (max * deltaY * CHANGE_FACTOR / mVideoViewHeight);
                    mAudioManager
                            .setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV,
                                    0);
                    int volumePercent =
                            (int)
                                    (mGestureDownVolume * 100 / max
                                            + deltaY * CHANGE_FACTOR * 100 / mVideoViewHeight);
                    showVolumeDialog(volumePercent);
                }
                if (mChangeBrightness) {
                    deltaY = -deltaY;
                    int deltaV = (int) (255 * deltaY * CHANGE_FACTOR / mVideoViewHeight);
                    WindowManager.LayoutParams params =
                            ((Activity) mHostView.getContext()).getWindow().getAttributes();
                    if (((mGestureDownBrightness + deltaV) / 255) >= 1) {
                        params.screenBrightness = 1;
                    } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                        params.screenBrightness = 0.01f;
                    } else {
                        params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                    }
                    if (mGestureChangeListener != null) {
                        mGestureChangeListener.onBrightnessChange(
                                params.screenBrightness, mGestureDownBrightness);
                    }
                    ((Activity) mHostView.getContext()).getWindow().setAttributes(params);
                    int brightnessPercent =
                            (int)
                                    (mGestureDownBrightness * 100 / 255
                                            + deltaY * CHANGE_FACTOR * 100 / mVideoViewHeight);
                    showBrightnessDialog(brightnessPercent);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchingProgressBar = false;
                dismissProgressDialog();
                dismissVolumeDialog();
                dismissBrightnessDialog();
                if (mChangePosition) {
                    mPlayer.seek(mSeekTimePosition);
                    if (!mPlayer.isPlaying()) {
                        mPlayer.start();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    private boolean canChangePosition() {
        return mPlayer.getCurrentState() != Player.STATE_ERROR
                && mPlayer.getCurrentState() != Player.STATE_PREPARING
                && mPlayer.isSeekable();
    }

    public void showProgressDialog(
            float deltaX,
            String seekTime,
            long seekTimePosition,
            String totalTime,
            long totalTimeDuration) {
        if (mProgressDialog == null) {
            View localView =
                    LayoutInflater.from(mHostView.getContext())
                            .inflate(R.layout.media_dialog_progress, null);
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mProgressDialog = createDialogWithView(localView);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        mDialogProgressBar.setProgress(
                totalTimeDuration <= 0 ? 0 : (int) (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.ic_media_dialog_forward);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.ic_media_dialog_backward);
        }
    }

    public void showVolumeDialog(int volumePercent) {
        if (mVolumeDialog == null) {
            View localView =
                    LayoutInflater.from(mHostView.getContext())
                            .inflate(R.layout.media_dialog_volume, null);
            mDialogVolumeImageView = ((ImageView) localView.findViewById(R.id.volume_image_tip));
            mDialogVolumeTextView = ((TextView) localView.findViewById(R.id.tv_volume));
            mDialogVolumeProgressBar =
                    ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
            mVolumeDialog = createDialogWithView(localView);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (volumePercent <= 0) {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.ic_media_dialog_close_volume);
        } else {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.ic_media_dialog_add_volume);
        }
        if (volumePercent > 100) {
            volumePercent = 100;
        } else if (volumePercent < 0) {
            volumePercent = 0;
        }
        mDialogVolumeTextView.setText(volumePercent + "%");
        mDialogVolumeProgressBar.setProgress(volumePercent);
    }

    public void showBrightnessDialog(int brightnessPercent) {
        if (mBrightnessDialog == null) {
            View localView =
                    LayoutInflater.from(mHostView.getContext())
                            .inflate(R.layout.media_dialog_brightness, null);
            mDialogBrightnessTextView = ((TextView) localView.findViewById(R.id.tv_brightness));
            mDialogBrightnessProgressBar =
                    ((ProgressBar) localView.findViewById(R.id.brightness_progressbar));
            mBrightnessDialog = createDialogWithView(localView);
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        if (brightnessPercent > 100) {
            brightnessPercent = 100;
        } else if (brightnessPercent < 0) {
            brightnessPercent = 0;
        }
        mDialogBrightnessTextView.setText(brightnessPercent + "%");
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);
    }

    public Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(mHostView.getContext(), R.style.Media_Dialog_Progress);
        dialog.setContentView(localView);
        Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(Window.FEATURE_ACTION_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams localLayoutParams = window.getAttributes();
            localLayoutParams.gravity = Gravity.CENTER;
            window.setAttributes(localLayoutParams);
        }
        return dialog;
    }

    private void dismissBrightnessDialog() {
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
        }
    }

    private void dismissVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    public interface MediaGestureChangeListener {
        void onBrightnessChange(float newValue, float oldValue);
    }
}
