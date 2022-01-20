/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.view.animation.Interpolator;
import java.util.Objects;

/**
 * 步进插值器
 *
 * @author xiaopengfei
 * @date 2020/4/8
 */
public class StepInterpolator implements Interpolator {

    /**
     * Jump at the start step position.
     */
    public static final String JUMP_START = "jump-start";
    /**
     * Jump at the end step position.
     */
    public static final String JUMP_END = "jump-end";
    /**
     * Jump at neither start or end step position.
     */
    public static final String JUMP_NONE = "jump-none";

    /* steps(int, step-position) 方法的第二个参数 */
    /**
     * Jump at both start and end step position.
     */
    public static final String JUMP_BOTH = "jump-both";
    /**
     * same with {@link #JUMP_START}.
     */
    public static final String START = "start";
    /**
     * same with {@link #JUMP_END}.
     */
    public static final String END = "end";
    private int mSteps;
    private String mStepPosition;
    /**
     * 用于标记动画处于动画开始前的 delay 延迟时期 标记为 {@code true} 会引起 {@link #steps(float)} 方法在整点处的输出降到比原值低一步。例如，可以 纠正
     * {@link #START} 型步进插值器在延迟期间输入进度为 0 的情况下输出的进度值
     */
    private boolean mBefore = false;

    public StepInterpolator(int steps) {
        this(steps, END);
    }

    /**
     * Instantiates a new Step interpolator.
     *
     * @param steps        the number of steps
     * @param stepPosition the step position
     * @throws IllegalArgumentException the illegal argument exception
     */
    public StepInterpolator(int steps, String stepPosition) throws IllegalArgumentException {
        if (steps <= 0) {
            throw new IllegalArgumentException("steps must be a positive integer greater than 0");
        } else if (JUMP_NONE.equals(stepPosition) && steps <= 1) {
            throw new IllegalArgumentException(
                    "steps must be a positive integer greater than 1 "
                            + "when step position is jump-none");
        }
        mSteps = steps;
        switch (stepPosition) {
            case JUMP_START:
            case JUMP_END:
            case START:
            case END:
            case JUMP_NONE:
            case JUMP_BOTH:
                mStepPosition = stepPosition;
                return;
            default:
                throw new IllegalArgumentException("unsupported StepPosition: " + stepPosition);
        }
    }

    @Override
    public float getInterpolation(float input) {
        return steps(input);
    }

    /**
     * @see <a href="https://drafts.csswg.org/css-animations-2/#animation-timing-function">
     * timing-function 算法参考标准</a>
     */
    private float steps(float input) {
        /* 此判断语句非原算法本身部分。
         * 为适应android 29级别 {@link android.animation.FloatKeyframeSet #getFloatValue(float fraction)}
         * 属性值计算的顺序BUG，避开 input=0 的情况（应该先算 intervalFraction 再用 intervalFraction 计算插值）
         * */
        if (input == 0) {
            return 0;
        }
        double multiValue = input * mSteps;
        double currentStep = Math.floor(multiValue);
        switch (mStepPosition) {
            case JUMP_START:
            case START:
            case JUMP_BOTH:
                currentStep += 1;
                break;
            default:
                break;
        }
        if (mBefore && Math.abs(multiValue % 1) < 1e-5) {
            currentStep -= 1;
        }
        if (input >= 0 && currentStep < 0) {
            currentStep = 0;
        }
        int jumps;
        switch (mStepPosition) {
            case JUMP_START:
            case JUMP_END:
            case START:
            case END:
                jumps = mSteps;
                break;
            case JUMP_NONE:
                jumps = mSteps - 1;
                break;
            case JUMP_BOTH:
                jumps = mSteps + 1;
                break;
            default:
                throw new IllegalStateException("unsupported StepPosition: " + mStepPosition);
        }
        if (input <= 1 && currentStep > jumps) {
            currentStep = jumps;
        }
        return (float) currentStep / jumps;
    }

    public void setBefore(boolean before) {
        mBefore = before;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (!(obj instanceof StepInterpolator)) {
            return false;
        }
        StepInterpolator interpolator = (StepInterpolator) obj;
        if (!Objects.equals(mStepPosition, interpolator.mStepPosition)) {
            return false;
        } else {
            return mSteps == interpolator.mSteps && mBefore == interpolator.mBefore;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        return 31 * (31 * hashCode + mStepPosition.hashCode()) + mSteps + (mBefore ? 1 : 0);
    }
}
