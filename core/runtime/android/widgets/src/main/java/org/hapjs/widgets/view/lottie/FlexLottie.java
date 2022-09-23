/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.lottie;

import android.animation.ValueAnimator;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.airbnb.lottie.LottieAnimationView;

import org.hapjs.common.executors.Executors;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FlexLottie extends LottieAnimationView implements ComponentHost, GestureHost {
    private static final String TAG = "FlexLottie";
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private OnChangeListener mOnChangeListener;
    private OnErrorListener mOnErrorListener;
    private boolean mHasErrorListener = false;
    private IGesture mGesture;
    private int mUpdateTime = 0;
    private int mTotalFrame = 0;
    private boolean isFirstTotalFrame = true;

    public FlexLottie(Context context) {
        super(context);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
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
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    public void setSource(Uri source) {
        String scheme = source.getScheme();
        if (scheme != null && (scheme.startsWith("http") || scheme.startsWith("https"))) {
            setAnimationFromUrl(source.toString());
        } else {
            Executors.io().execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "source.getPath: " + source.getPath());
                    InputStream inputStream = readFile(source.getPath());
                    if (inputStream != null) {
                        setAnimation(inputStream, source.getPath());
                    }
                }
            });

        }
    }

    public void setMaxFrame() {
        if (isFirstTotalFrame) {
            mTotalFrame = (int) getMaxFrame();
            if (mTotalFrame > 0) {
                isFirstTotalFrame = false;
            }
            Log.d(TAG, "mTotalFrame: " + mTotalFrame);
        }
    }


    public void setMethSpeed(float speed) {
        setSpeed(speed);
    }

    public void setMethProgress(float progress) {
        setProgress(progress);
    }

    public void setLoop(boolean loop) {
        loop(loop);
    }

    public void release() {
        cancelAnimation();
        mOnChangeListener = null;
        mOnErrorListener = null;
    }

    public void setAutoPlay(boolean autoplay) {
        if (autoplay) {
            playAnimation();
        }
    }

    public void start(int startFrame, int endFrame) {
        cancelAnimation();
        setMinFrame(startFrame);
        setMaxFrame(endFrame);
        playAnimation();
    }

    public void stop() {
        pauseAnimation();
    }

    public void start() {
        cancelAnimation();
        if (getMinFrame() != 0) {
            setMinFrame(0);
            setProgress(0);
        }
        if (getFrame() != mTotalFrame) {
            setMaxFrame(mTotalFrame);
        }
        playAnimation();
    }

    public void resume() {
        resumeAnimation();
    }

    public void reset() {
        cancelAnimation();
        setProgress(0);
    }

    private InputStream readFile(String filePath) {
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        InputStream instr = null;
        if (file.isDirectory()) {
            Log.d(TAG, filePath + " is directory");
            return null;
        } else {
            try {
                instr = new FileInputStream(file);
                return instr;
            } catch (Exception e) {
                if (mHasErrorListener) {
                    mOnErrorListener.onError(e.toString());
                }
                Log.e(TAG, " Exception: " + e);
            }
        }
        return null;
    }

    public void onActivityResume() {
        resumeAnimation();
    }

    public void onActivityPaused() {
        pauseAnimation();
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        mOnChangeListener = onChangeListener;
        addAnimatorUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mUpdateTime++;
                if (mUpdateTime == 10) {
                    mUpdateTime = 0;
                    mOnChangeListener.onChange(Float.parseFloat(valueAnimator.getAnimatedValue().toString()));
                }
            }
        });
    }

    public void setOnErrorListener(OnErrorListener onErrorListener, boolean hasErrorListener) {
        mOnErrorListener = onErrorListener;
        mHasErrorListener = hasErrorListener;
    }

    //change progress
    public interface OnChangeListener {
        void onChange(float progress);
    }

    public interface OnErrorListener {
        void onError(String error);
    }
}
