/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Arrays;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.Runtime;

public class BallLoadingView extends View {

    public static final int DEFAULT_SIZE = 45;
    public static final int DEFAULT_INDICATORS = 3;
    public static final int DEFAULT_BALL_COLOR = 0xFFE75764;
    private BallIndicator mIndicator;
    private Paint mPaint;

    public BallLoadingView(Context context) {
        this(context, null);
    }

    public BallLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BallLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(DEFAULT_BALL_COLOR);
        mPaint.setStyle(Paint.Style.FILL);
        setIndicators(DEFAULT_INDICATORS);
    }

    public void setIndicators(int size) {
        if (size <= 0) {
            return;
        }
        if (mIndicator != null && mIndicator.getIndicatorSize() == size) {
            return;
        }

        if (mIndicator != null) {
            mIndicator.cancelAnimation();
            mIndicator = null;
        }
        mIndicator = new BallIndicator(this, size);
        postInvalidate();
    }

    public void setIndicatorColor(int color) {
        mPaint.setColor(color);
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureDimension(dp2px(DEFAULT_SIZE), widthMeasureSpec);
        int height = measureDimension(dp2px(DEFAULT_SIZE), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureDimension(int defaultSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(defaultSize, specSize);
        } else {
            result = defaultSize;
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mIndicator.applyAnimations();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIndicator != null) {
            mIndicator.draw(canvas, mPaint);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (mIndicator != null) {
            if (visibility == VISIBLE) {
                mIndicator.applyAnimations();
            } else {
                mIndicator.stopAnimation();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mIndicator != null) {
            mIndicator.cancelAnimation();
        }
    }

    private int dp2px(int dpValue) {
        return (int) getContext().getResources().getDisplayMetrics().density * dpValue;
    }

    static class BallIndicator {
        public static final int DEFAULT_ANIMATION_DURATION = 1000; // 1000ms

        private View mTarget;
        private float[] mScales;
        private int mAnimationDuration = DEFAULT_ANIMATION_DURATION;
        private int mBallSpace;
        private Animator[] mAnimations;

        public BallIndicator(View target, int indicatorNum) {
            mTarget = target;
            mScales = new float[indicatorNum];
            Arrays.fill(mScales, 1f);
            mBallSpace = DisplayUtil.dip2Pixel(Runtime.getInstance().getContext(), 4);
        }

        public int getIndicatorSize() {
            return mScales.length;
        }

        public void applyAnimations() {
            if (mAnimations == null) {
                mAnimations = createAnimation();
            }

            for (Animator animation : mAnimations) {
                if (!animation.isRunning()) {
                    animation.start();
                }
            }
        }

        public void stopAnimation() {
            if (mAnimations == null) {
                return;
            }
            for (Animator animation : mAnimations) {
                animation.end();
            }
        }

        public void cancelAnimation() {
            if (mAnimations == null) {
                return;
            }
            for (Animator animation : mAnimations) {
                animation.cancel();
            }
        }

        private Animator[] createAnimation() {
            int length = mScales.length;
            int[] delays = new int[length];
            int interval = Math.round(360f / length);
            for (int i = 0; i < length; i++) {
                delays[i] = i * interval;
            }
            Animator[] animators = new Animator[length];
            for (int i = 0; i < length; i++) {
                ValueAnimator animator = ValueAnimator.ofFloat(1f, 0.3f, 1f);
                animator.setDuration(mAnimationDuration);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setStartDelay(delays[i]);

                final int index = i;
                animator.addUpdateListener(
                        animation -> {
                            mScales[index] = (float) animation.getAnimatedValue();
                            mTarget.postInvalidate();
                        });
                animators[i] = animator;
            }
            return animators;
        }

        public void draw(Canvas canvas, Paint paint) {
            int length = mScales.length;
            float space = mBallSpace;
            float radius =
                    (Math.min(mTarget.getWidth(), mTarget.getHeight()) - space * (length - 1))
                            / (length * 2);

            int x = 0;
            int y = mTarget.getHeight() / 2;
            for (int i = 0; i < length; i++) {
                canvas.save();
                x = (int) (radius + i * (radius * 2 + space));
                canvas.drawCircle(x, y, radius * mScales[i], paint);
                canvas.restore();
            }
        }
    }
}
