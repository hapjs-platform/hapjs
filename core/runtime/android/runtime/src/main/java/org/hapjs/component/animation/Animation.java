/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import java.util.HashMap;
import java.util.Map;

import org.hapjs.component.Component;

public class Animation {
    private static final String TAG = "Animation";

    private int mRef;
    private CSSAnimatorSet mAnimatorSet;
    private Component mComponent;

    private OnCancelListener mCancelListener;
    private OnFinishListener mFinishListener;
    private Map<String, AnimationLifecycle> mLifecycleList = new HashMap<>();

    public Animation(CSSAnimatorSet animatorSet) {
        mAnimatorSet = animatorSet;
        mAnimatorSet.setAnimation(this);
    }

    public CSSAnimatorSet getAnimatorSet() {
        return mAnimatorSet;
    }

    public void setAnimatorSet(CSSAnimatorSet animatorSet) {
        mAnimatorSet = animatorSet;
    }

    public Component getComponent() {
        return mComponent;
    }

    public void setComponent(Component component) {
        mComponent = component;
    }

    public int getRef() {
        return mRef;
    }

    public void setRef(int ref) {
        mRef = ref;
    }

    public long getStartTime() {
        if (mAnimatorSet != null) {
            return mAnimatorSet.getStartTime();
        } else {
            return 0;
        }
    }

    public boolean isReady() {
        if (mAnimatorSet != null) {
            return mAnimatorSet.isReady();
        } else {
            return false;
        }
    }

    public boolean isFinished() {
        if (mAnimatorSet != null) {
            return mAnimatorSet.isFinished();
        } else {
            return true;
        }
    }

    public boolean isPending() {
        if (mAnimatorSet != null) {
            return mAnimatorSet.isPending();
        } else {
            return false;
        }
    }

    public String getPlayState() {
        if (mAnimatorSet != null) {
            return mAnimatorSet.getPlayState();
        } else {
            return "idle";
        }
    }

    public void play() {
        if (mAnimatorSet != null) {
            mAnimatorSet.start();
        }
    }

    public void pause() {
        if (mAnimatorSet != null) {
            mAnimatorSet.pause();
        }
    }

    public void resume() {
        if (mAnimatorSet != null) {
            mAnimatorSet.resume();
        }
    }

    public void finish() {
        if (mAnimatorSet != null) {
            mAnimatorSet.finish();
        }
    }

    public void cancel() {
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }
    }

    public void reverse() {
        if (mAnimatorSet != null) {
            mAnimatorSet.reverse();
        }
    }

    public void onCancel() {
        if (mCancelListener != null) {
            mCancelListener.onCancel();
        }
    }

    public void onFinish() {
        if (mFinishListener != null) {
            mFinishListener.onFinish();
        }
    }

    public void onDestroy() {
        for (String key : mLifecycleList.keySet()) {
            AnimationLifecycle lifecycle = mLifecycleList.get(key);
            if (lifecycle != null) {
                lifecycle.onDestroy(key);
            }
        }
        if (mAnimatorSet != null) {
            // 先置空，避免互相调用 destroy，陷入循环
            CSSAnimatorSet animatorSet = mAnimatorSet;
            mAnimatorSet = null;
            animatorSet.destroy();
        }
    }

    public void setAnimationLifecycle(AnimationLifecycle lifecycle, String key) {
        mLifecycleList.put(key, lifecycle);
    }

    public void setCancelListener(OnCancelListener listener) {
        mCancelListener = listener;
    }

    public void setFinishListener(OnFinishListener listener) {
        mFinishListener = listener;
    }

    public interface OnCancelListener {
        void onCancel();
    }

    public interface OnFinishListener {
        void onFinish();
    }

    public interface AnimationLifecycle {
        void onDestroy(String key);
    }
}
