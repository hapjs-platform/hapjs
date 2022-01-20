/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.Property;
import android.view.ViewGroup;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.component.constants.Edge;

/**
 * 定义前端 box 模型组件的某个样式属性在四个方向上的过渡，属性包括 padding，border 等.
 *
 * @param <T> 带有该属性的对象类型
 * @param <V> 属性值类型
 */
public abstract class AbstractBoxTransition<T, V> extends Transition {

    protected String[] mTransitionProperties;
    /**
     * 字符串方位属性名.
     */
    protected String mPositionProperty;
    /**
     * 整型方位，与字符串方位对应.
     */
    protected int mDirection;

    /**
     * 左方位属性.
     */
    protected Property<T, V> mLeftProperty;
    /**
     * 上方位属性.
     */
    protected Property<T, V> mTopProperty;
    /**
     * 右方位属性.
     */
    protected Property<T, V> mRightProperty;
    /**
     * 下方位属性.
     */
    protected Property<T, V> mBottomProperty;

    protected boolean mDirty = false;

    public AbstractBoxTransition(@NonNull String positionProperty) {
        super();
        mPositionProperty = positionProperty;
        mDirection = getDirection(positionProperty);
        switch (mDirection) {
            case Edge.LEFT:
                mLeftProperty = createProperty(Edge.LEFT);
                break;
            case Edge.TOP:
                mTopProperty = createProperty(Edge.TOP);
                break;
            case Edge.RIGHT:
                mRightProperty = createProperty(Edge.RIGHT);
                break;
            case Edge.BOTTOM:
                mBottomProperty = createProperty(Edge.BOTTOM);
                break;
            default:
                setAllProperty();
                break;
        }
    }

    @Override
    public String[] getTransitionProperties() {
        return mTransitionProperties;
    }

    /**
     * 获取与方位参数对应的整型方位.
     *
     * @param position 传入的方位参数
     * @return 整型方位参数，应是与参数对应的 Edge.LEFT, Edge.TOP, Edge.RIGHT, Edge.BOTTOM, Edge.ALL 中的一种
     */
    protected abstract int getDirection(@NonNull String position);

    /**
     * 创建当个方位上的属性.
     *
     * @param direction 方位
     * @return 属性
     */
    protected abstract @NonNull Property<T, V> createProperty(@DirectionType int direction);

    /**
     * 设置所有四个方位属性.
     */
    protected void setAllProperty() {
        mLeftProperty = createProperty(Edge.LEFT);
        mTopProperty = createProperty(Edge.TOP);
        mRightProperty = createProperty(Edge.RIGHT);
        mBottomProperty = createProperty(Edge.BOTTOM);
    }

    /**
     * 获取带有该属性的目标对象.
     *
     * @param values 保存属性值的对象
     * @return 带有该属性的目标对象
     */
    protected abstract T getTarget(@NonNull TransitionValues values);

    /**
     * 获取方位对应属性名.
     *
     * @param direction 方位
     * @return 属性名
     */
    protected abstract String getPropertyName(@DirectionType int direction);

    /**
     * 获取方位对应属性值.
     *
     * @param target    带有属性的目标对象
     * @param direction 方位
     * @return 属性值
     */
    protected abstract V getPropertyValue(@NonNull T target, @DirectionType int direction);

    /**
     * 捕获动画开始和结束时的属性值.
     *
     * @param transitionValues 保存属性值的对象
     */
    protected void captureValues(@NonNull TransitionValues transitionValues) {
        T target = getTarget(transitionValues);
        if (target != null) {
            if (mDirection == Edge.ALL) {
                transitionValues.values.put(
                        getPropertyName(Edge.LEFT), getPropertyValue(target, Edge.LEFT));
                transitionValues.values
                        .put(getPropertyName(Edge.TOP), getPropertyValue(target, Edge.TOP));
                transitionValues.values.put(
                        getPropertyName(Edge.RIGHT), getPropertyValue(target, Edge.RIGHT));
                transitionValues.values.put(
                        getPropertyName(Edge.BOTTOM), getPropertyValue(target, Edge.BOTTOM));
            } else {
                transitionValues.values
                        .put(mPositionProperty, getPropertyValue(target, mDirection));
            }
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public Animator createAnimator(
            ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        if (mDirection != Edge.ALL) {
            return createAnimator(mDirection, startValues, endValues);
        } else {
            return createAllAnimators(startValues, endValues);
        }
    }

    /**
     * 创建单个方位的属性动画.
     *
     * @param direction   方位
     * @param startValues 动画开始属性值
     * @param endValues   动画结束属性值
     * @return 属性动画
     */
    protected Animator createAnimator(
            @DirectionType int direction,
            @NonNull TransitionValues startValues,
            @NonNull TransitionValues endValues) {
        String directionStr = getPropertyName(direction);
        V startValue = (V) startValues.values.get(directionStr);
        V endValue = (V) endValues.values.get(directionStr);
        if (startValue != null && endValue != null && !startValue.equals(endValue)) {
            T target = getTarget(endValues);
            if (target != null) {
                Property<T, V> borderProperty = chooseProperty(direction);
                return ObjectAnimator.ofObject(
                        target, borderProperty, getEvaluator(), startValue, endValue);
            }
        }
        return null;
    }

    /**
     * 构造属性动画时，采用的估值器.覆盖此方法以给出特定估值器.
     *
     * @return 估值器，缺省为 null 即线性估值
     */
    protected TypeEvaluator<V> getEvaluator() {
        return null;
    }

    /**
     * 从四个不同方位中选出与参数方位对应的属性.
     *
     * @param direction 整型方位
     * @return 对应的方位属性
     */
    protected Property<T, V> chooseProperty(@DirectionType int direction) {
        switch (direction) {
            case Edge.LEFT:
                return mLeftProperty;
            case Edge.TOP:
                return mTopProperty;
            case Edge.RIGHT:
                return mRightProperty;
            case Edge.BOTTOM:
                return mBottomProperty;
            default:
                // should never get here
                return null;
        }
    }

    /**
     * 创建左、上、右、下四个方位的属性动画.
     *
     * @param startValues 动画开始属性值
     * @param endValues   动画结束属性值
     * @return 属性动画
     */
    protected Animator createAllAnimators(
            @NonNull TransitionValues startValues, @NonNull TransitionValues endValues) {
        Animator leftAnimator = createAnimator(Edge.LEFT, startValues, endValues);
        Animator topAnimator = createAnimator(Edge.TOP, startValues, endValues);
        Animator rightAnimator = createAnimator(Edge.RIGHT, startValues, endValues);
        Animator bottomAnimator = createAnimator(Edge.BOTTOM, startValues, endValues);
        List<Animator> animatorList = new ArrayList<>(4);
        if (leftAnimator != null) {
            animatorList.add(leftAnimator);
        }
        if (topAnimator != null) {
            animatorList.add(topAnimator);
        }
        if (rightAnimator != null) {
            animatorList.add(rightAnimator);
        }
        if (bottomAnimator != null) {
            animatorList.add(bottomAnimator);
        }
        int size = animatorList.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return animatorList.get(0);
        } else {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animatorList);
            return animatorSet;
        }
    }

    @IntDef({Edge.LEFT, Edge.TOP, Edge.RIGHT, Edge.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DirectionType {
    }
}
