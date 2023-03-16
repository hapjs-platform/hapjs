/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import static org.hapjs.component.animation.AnimationParser.PROPERTY_ALPHA;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_BACKGROUND_COLOR;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_BACKGROUND_POSITION;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_HEIGHT;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_ROTATION;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_ROTATION_X;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_ROTATION_Y;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_SCALE_X;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_SCALE_Y;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_TRANSLATION_X;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_TRANSLATION_Y;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_TRANSLATION_Z;
import static org.hapjs.component.animation.AnimationParser.PROPERTY_WIDTH;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Objects;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.bridge.SimpleActivityStateListener;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;

public class CSSAnimatorSet {

    private Component mComponent;
    private Transform mTransform;
    private AnimatorSet mWrapped;
    private Animation mAnimation;
    // interpolator for each keyframe, not for the animation
    private TimeInterpolator mTimeInterpolator;

    private boolean mDirty;
    private boolean mIsReady = true;
    private boolean mIsPropertyUpdated = false;
    private int mRepeatCount;
    private String mFillMode = FillMode.NONE;
    private String mDirection = Direction.NORMAL;

    private String mAnimationAttr;
    private boolean mIsPercent = false;
    private boolean mIsCanceled = false;
    private boolean mIsFinished = false;
    private boolean mActivityListenerInstalled = false;

    private long mStartTime = 0;
    private long mDelay = 0;

    private HapEngine mHapEngine;
    private View.OnAttachStateChangeListener mOnAttachStateChangeListener =
            new View.OnAttachStateChangeListener() {
                private boolean mSuspended;

                @Override
                public void onViewAttachedToWindow(View v) {
                    if (mSuspended) {
                        mWrapped.resume();
                        v.addOnLayoutChangeListener(mLayoutChangeListener);
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    mSuspended = mWrapped.isStarted();
                    pause();
                    v.removeOnLayoutChangeListener(mLayoutChangeListener);
                }
            };
    private View.OnLayoutChangeListener mLayoutChangeListener =
            new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(
                        View v,
                        int left,
                        int top,
                        int right,
                        int bottom,
                        int oldLeft,
                        int oldTop,
                        int oldRight,
                        int oldBottom) {

                    if (!isRunning()) {
                        v.removeOnLayoutChangeListener(this);
                        return;
                    }

                    boolean sizeHasChange =
                            (left != oldLeft) || (right != oldRight) || (top != oldTop)
                                    || (bottom != oldBottom);

                    if (!sizeHasChange) {
                        return;
                    }
                    if (mComponent != null) {
                        CSSAnimatorSet temp = parseAndStart();
                        mComponent.setAnimatorSet(temp);
                    }
                }
            };

    private final SimpleActivityStateListener mActivityStateListener = new SimpleActivityStateListener() {
        private boolean mSuspended;

        @Override
        public void onActivityResume() {
            if (mSuspended) {
                mWrapped.resume();
                mSuspended = false;
            }
        }

        @Override
        public void onActivityPause() {
            boolean previouslyStarted = mWrapped.isStarted() && !mWrapped.isPaused();
            if (previouslyStarted && !mSuspended) {
                mWrapped.pause();
                mSuspended = mWrapped.isPaused();
            }
        }
    };

    public CSSAnimatorSet(HapEngine hapEngine, Component component) {
        mHapEngine = hapEngine;
        mComponent = component;
        mTransform = component.getTransform();
        if (mTransform == null) {
            mTransform = new Transform();
        }

        mWrapped = new AnimatorSet();
        mWrapped.setInterpolator(new LinearInterpolator());
        mWrapped.addListener(new CssAnimationListener(this));
    }

    public static CSSAnimatorSet createNewAnimator(
            HapEngine hapEngine, CSSAnimatorSet oldAnimatorSet, Component component) {
        if (hapEngine == null || component == null) {
            return null;
        }
        CSSAnimatorSet newAnimatorSet = new CSSAnimatorSet(hapEngine, component);
        if (oldAnimatorSet != null) {
            AnimatorSet wrapped = oldAnimatorSet.getWrappedAnimatorSet();
            if (wrapped.getDuration() > -1) {
                newAnimatorSet.setDuration(wrapped.getDuration());
            }
            newAnimatorSet.setStartDelay(wrapped.getStartDelay());
            if (wrapped.getInterpolator() != null) {
                newAnimatorSet.setInterpolator(wrapped.getInterpolator());
            }
            newAnimatorSet.setKeyFrameInterpolator(oldAnimatorSet.getKeyFrameInterpolator());
            newAnimatorSet.setRepeatCount(oldAnimatorSet.mRepeatCount);
            newAnimatorSet.setFillMode(oldAnimatorSet.mFillMode);
            newAnimatorSet.setDirection(oldAnimatorSet.mDirection);
            newAnimatorSet.setAttr(oldAnimatorSet.getAttr());
            if (oldAnimatorSet.getWrappedAnimatorSet().isRunning()) {
                oldAnimatorSet.getWrappedAnimatorSet().cancel();
            }
        }

        return newAnimatorSet;
    }

    public AnimatorSet getWrappedAnimatorSet() {
        return mWrapped;
    }

    public CSSAnimatorSet setDuration(long duration) {
        if (duration != mWrapped.getDuration()) {
            mWrapped.setDuration(duration);
            mIsPropertyUpdated = true;
        }
        return this;
    }

    public void setStartDelay(long startDelay) {
        mWrapped.setStartDelay(startDelay);
    }

    public void setDelay(long delay) {
        if (delay != mDelay) {
            mDelay = delay;
            mWrapped.setStartDelay(mDelay + mStartTime);
            mIsPropertyUpdated = true;
        }
    }

    public void setInterpolator(TimeInterpolator interpolator) {
        mWrapped.setInterpolator(interpolator);
    }

    public TimeInterpolator getKeyFrameInterpolator() {
        if (mTimeInterpolator == null) {
            mTimeInterpolator = new EaseInterpolator();
        }
        return mTimeInterpolator;
    }

    public void setKeyFrameInterpolator(TimeInterpolator interpolator) {
        if (!Objects.equals(interpolator, mTimeInterpolator)) {
            mTimeInterpolator = interpolator;
            mIsPropertyUpdated = true;
        }
    }

    public void setRepeatCount(int repeatCount) {
        if (repeatCount != mRepeatCount) {
            mRepeatCount = repeatCount;
            mIsPropertyUpdated = true;
        }
    }

    public void start() {
        if (mComponent == null) {
            return;
        }
        if (isRunning()) {
            if (!mDirty) {
                // already started and no change
                return;
            }
            cancel();
        }

        mDirty = false;
        mWrapped.start();
        View animatedView = mComponent.getHostView();
        if (animatedView != null) {
            // animated view may changed.
            animatedView.removeOnAttachStateChangeListener(mOnAttachStateChangeListener);
            animatedView.addOnAttachStateChangeListener(mOnAttachStateChangeListener);
        }

        // 百分比参数动画在自身尺寸发生变化时，需要进行自适应
        installLayoutChangeListener(animatedView);

        // 根据activity可见性暂停或重启动画
        installActivityListener(mComponent);
    }

    public void finish() {
        mWrapped.end();
    }

    public void pause() {
        mWrapped.pause();
    }

    public void reverse() {
        for (Animator animator : mWrapped.getChildAnimations()) {
            animator.end();
            ((ValueAnimator) animator).reverse();
        }
    }

    public void setAnimation(Animation animation) {
        mAnimation = animation;
    }

    private void installLayoutChangeListener(View view) {
        if (view != null && isPercent()) {
            view.removeOnLayoutChangeListener(mLayoutChangeListener);
            view.addOnLayoutChangeListener(mLayoutChangeListener);
        }
    }

    private void installActivityListener(Component component) {
        if (!mActivityListenerInstalled && component != null && component.getCallback() != null) {
            component.getCallback().addActivityStateListener(mActivityStateListener);
            mActivityListenerInstalled = true;
        }
    }

    private void uninstallActivityListener(Component component) {
        if (mActivityListenerInstalled && component != null && component.getCallback() != null) {
            component.getCallback().removeActivityStateListener(mActivityStateListener);
            mActivityListenerInstalled = false;
        }
    }

    public boolean isRunning() {
        return mWrapped.isRunning();
    }

    public boolean isFinished() {
        return mIsFinished;
    }

    public boolean isPending() {
        return !isRunning() && !mWrapped.isPaused() && !isFinished();
    }

    public String getPlayState() {
        if (mWrapped.isPaused()) {
            return "paused";
        }
        if (isFinished()) {
            return "finished";
        }
        if (isRunning() || mWrapped.isStarted()) {
            return "running";
        }
        return "idle";
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long startTime) {
        mStartTime = startTime;
        mWrapped.setStartDelay(mDelay + mStartTime);
    }

    public void cancel() {
        mWrapped.cancel();
    }

    public void playTogether(Animator... items) {
        mWrapped.playTogether(items);
    }

    public void playTogether(Collection<Animator> items) {
        mWrapped.playTogether(items);
    }

    public void setFillMode(String fillMode) {
        if (!Objects.equals(fillMode, mFillMode)) {
            mFillMode = fillMode;
            mIsPropertyUpdated = true;
        }
    }

    public void setDirection(String direction) {
        if (!Objects.equals(direction, mDirection)) {
            mDirection = direction;
            mIsPropertyUpdated = true;
        }
    }

    public boolean isFillForwards() {
        return (!TextUtils.isEmpty(mFillMode) && FillMode.FORWARDS.equals(mFillMode));
    }

    public boolean isDirty() {
        return mDirty;
    }

    public void setDirty(boolean dirty) {
        mDirty = dirty;
    }

    public void destroy() {
        cancel();
        Component component = mComponent;
        if (null != component) {
            View hostView = component.getHostView();
            if (hostView != null) {
                hostView.removeOnAttachStateChangeListener(mOnAttachStateChangeListener);
                hostView.removeOnLayoutChangeListener(mLayoutChangeListener);
            }
            uninstallActivityListener(component);
        }
        if (mWrapped != null) {
            mWrapped.end();
            mWrapped.removeAllListeners();
        }
        if (mAnimation != null) {
            // 先置空，避免互相调用 destroy，陷入循环
            Animation animation = mAnimation;
            mAnimation = null;
            animation.onDestroy();
        }
        mComponent = null;
        mTransform = null;
    }

    public void setIsReady(boolean isReady) {
        mIsReady = isReady;
    }

    public boolean isReady() {
        return mIsReady;
    }

    public void setIsPropertyUpdated(boolean isPropertyUpdated) {
        mIsPropertyUpdated = isPropertyUpdated;
    }

    public boolean isPropertyUpdated() {
        return mIsPropertyUpdated;
    }

    public void setIsPercent(boolean isPercent) {
        mIsPercent = isPercent;
    }

    public boolean isPercent() {
        return mIsPercent;
    }

    public String getAttr() {
        return mAnimationAttr;
    }

    public void setAttr(Object target) {
        String animationAttr = Attributes.getString(target, "");
        if (!Objects.equals(animationAttr, mAnimationAttr)) {
            mAnimationAttr = animationAttr;
            mIsPropertyUpdated = true;
        }
    }

    public CSSAnimatorSet parseAndStart() {
        if (mComponent == null || mHapEngine == null) {
            return null;
        }
        // 避免当前CssAnimatorSet被destroy
        CSSAnimatorSet oldAnimatorSet = createNewAnimator(mHapEngine, this, mComponent);
        // 创建新CssAnimatorSet时可能失败，此时返回的是oldAnimatorSet
        CSSAnimatorSet animatorSet =
                AnimationParser.parse(mHapEngine, oldAnimatorSet, getAttr(), mComponent);
        if (oldAnimatorSet != null && oldAnimatorSet != animatorSet) {
            oldAnimatorSet.destroy();
        }
        if (animatorSet == null) {
            if (isRunning()) {
                cancel();
                setDirty(false);
            }
            return null;
        }

        if (animatorSet.isDirty()) {
            if (isRunning()) {
                cancel();
            }
            animatorSet.start();
        }
        return animatorSet;
    }

    public interface FillMode {
        String NONE = "none";
        String FORWARDS = "forwards";
    }

    public interface Direction {
        String NORMAL = "normal";
        String REVERSE = "reverse";
        String ALTERNATE = "alternate";
        String ALTERNATE_REVERSE = "alternate-reverse";
    }

    private static class CssAnimationListener extends AnimatorListenerAdapter {
        private WeakReference<CSSAnimatorSet> mAnimationSetRef;

        public CssAnimationListener(CSSAnimatorSet cssAnimatorSet) {
            mAnimationSetRef = new WeakReference<>(cssAnimatorSet);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            final CSSAnimatorSet cssAnimatorSet = mAnimationSetRef.get();
            if (cssAnimatorSet == null
                    || cssAnimatorSet.mComponent == null
                    || cssAnimatorSet.mWrapped == null) {
                return;
            }
            cssAnimatorSet.mIsCanceled = false;
            cssAnimatorSet.mIsFinished = false;
            for (Animator anim : cssAnimatorSet.mWrapped.getChildAnimations()) {
                ValueAnimator valueAnimator = (ValueAnimator) anim;
                if (cssAnimatorSet.mRepeatCount > 0) {
                    valueAnimator.setRepeatCount(cssAnimatorSet.mRepeatCount - 1);
                } else {
                    valueAnimator.setRepeatCount(cssAnimatorSet.mRepeatCount);
                }
                switch (cssAnimatorSet.mDirection) {
                    case Direction.NORMAL:
                        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
                        break;
                    case Direction.REVERSE:
                        valueAnimator.setInterpolator(new ReverseInterpolator());
                        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
                        break;
                    case Direction.ALTERNATE:
                        valueAnimator.setRepeatMode(valueAnimator.REVERSE);
                        break;
                    case Direction.ALTERNATE_REVERSE:
                        valueAnimator.setInterpolator(new ReverseInterpolator());
                        valueAnimator.setRepeatMode(valueAnimator.REVERSE);
                        break;
                    default:
                        break;
                }
                PropertyValuesHolder[] values = valueAnimator.getValues();
                if (values == null || values.length < 1) {
                    continue;
                }
                PropertyValuesHolder value = values[0];

                switch (value.getPropertyName()) {
                    case PROPERTY_ALPHA:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            float opacity =
                                                    cssAnimatorSet.mComponent.getCurStateStyleFloat(
                                                            Attributes.Style.OPACITY, 1f);
                                            cssAnimatorSet.mComponent.setOpacity(opacity);
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_BACKGROUND_COLOR:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            String backgroundColorStr =
                                                    cssAnimatorSet.mComponent
                                                            .getCurStateStyleString(
                                                                    Attributes.Style.BACKGROUND_COLOR,
                                                                    "transparent");
                                            int backgroundColor =
                                                    ColorUtil.getColor(backgroundColorStr,
                                                            Color.TRANSPARENT);
                                            cssAnimatorSet.mComponent
                                                    .setBackgroundColor(backgroundColor);
                                            cssAnimatorSet.mComponent.applyBackground();
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_BACKGROUND_POSITION:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            String backgroundPositionStr =
                                                    cssAnimatorSet
                                                            .mComponent
                                                            .getOrCreateBackgroundComposer()
                                                            .getInitialPositionStr();
                                            cssAnimatorSet.mComponent
                                                    .setBackgroundPosition(backgroundPositionStr);
                                            cssAnimatorSet.mComponent.applyBackground();
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_WIDTH:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            String widthStr =
                                                    cssAnimatorSet.mComponent
                                                            .getCurStateStyleString(
                                                                    Attributes.Style.WIDTH, null);
                                            cssAnimatorSet.mComponent.setWidth(widthStr);
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_HEIGHT:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            String heightStr =
                                                    cssAnimatorSet.mComponent
                                                            .getCurStateStyleString(
                                                                    Attributes.Style.HEIGHT, null);
                                            cssAnimatorSet.mComponent.setHeight(heightStr);
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_ROTATION:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyRotate(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_ROTATION_X:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyRotateX(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_ROTATION_Y:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyRotateY(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_SCALE_X:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyScaleX(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_SCALE_Y:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyScaleY(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_TRANSLATION_X:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyTranslationX(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_TRANSLATION_Y:
                        valueAnimator.addListener(
                                new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (!cssAnimatorSet.isFillForwards()) {
                                            Transform.applyTranslationY(
                                                    cssAnimatorSet.mTransform,
                                                    cssAnimatorSet.mComponent.getHostView());
                                        }
                                    }
                                });

                        continue;
                    case PROPERTY_TRANSLATION_Z:
                        valueAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (!cssAnimatorSet.isFillForwards()) {
                                    Transform.applyTranslationZ(
                                            cssAnimatorSet.mTransform,
                                            cssAnimatorSet.mComponent.getHostView());
                                }
                            }
                        });

                        continue;
                    default:
                        break;
                }
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            final CSSAnimatorSet cssAnimatorSet = mAnimationSetRef.get();
            if (cssAnimatorSet == null) {
                return;
            }
            if (cssAnimatorSet.mAnimation != null) {
                cssAnimatorSet.mIsCanceled = true;
                cssAnimatorSet.mAnimation.onCancel();
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            final CSSAnimatorSet cssAnimatorSet = mAnimationSetRef.get();
            if (cssAnimatorSet == null || cssAnimatorSet.mWrapped == null) {
                return;
            }
            cssAnimatorSet.mIsFinished = true;
            for (Animator anim : cssAnimatorSet.mWrapped.getChildAnimations()) {
                ValueAnimator valueAnimator = (ValueAnimator) anim;
                valueAnimator.removeAllListeners();
            }
            if (cssAnimatorSet.mAnimation != null && !cssAnimatorSet.mIsCanceled) {
                cssAnimatorSet.mAnimation.onFinish();
            }
        }
    }

    private static class ReverseInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float pos) {
            return 1f - pos;
        }
    }
}
