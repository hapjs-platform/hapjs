/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.transition.utils.FloatProperty;
import org.hapjs.component.view.ComponentHost;

public class FlexTransition extends Transition {

    public static final int FLEX = 0;
    public static final int FLEX_GROW = 1;
    public static final int FLEX_SHRINK = 2;
    public static final int FLEX_BASIS = 3;

    protected String[] mTransitionProperties;

    protected String mFlexTypeStr;
    protected int mFlexType;
    protected Property<Component, Float> mFlexGrow;
    protected Property<Component, Float> mFlexShrink;
    protected Property<Component, Float> mFlexBasis;

    public FlexTransition(@NonNull String flexTypeStr) {
        super();
        mFlexTypeStr = flexTypeStr;
        switch (flexTypeStr) {
            case Attributes.Style.FLEX_GROW:
                mFlexType = FLEX_GROW;
                mFlexGrow = createProperty(FLEX_GROW);
                break;
            case Attributes.Style.FLEX_SHRINK:
                mFlexType = FLEX_SHRINK;
                mFlexShrink = createProperty(FLEX_SHRINK);
                break;
            case Attributes.Style.FLEX_BASIS:
                mFlexType = FLEX_BASIS;
                mFlexBasis = createProperty(FLEX_BASIS);
                break;
            default:
                mFlexType = FLEX;
                setAllProperty();
                break;
        }
        mTransitionProperties =
                mFlexType == FLEX
                        ? new String[] {
                            Attributes.Style.FLEX_GROW, Attributes.Style.FLEX_SHRINK,
                            Attributes.Style.FLEX_BASIS
                        }
                        : new String[] {getPropertyName(mFlexType)};
    }

    private Property<Component, Float> createProperty(@FlexType int flexType) {
        switch (flexType) {
            case FLEX_GROW:
                return new FloatProperty<Component>() {
                    @Override
                    public void setValue(@NonNull Component object, float value) {
                        object.setFlexGrow(value);
                        View hostView = object.getHostView();
                        if (hostView != null) {
                            hostView.requestLayout();
                        }
                    }
                };
            case FLEX_SHRINK:
                return new FloatProperty<Component>() {
                    @Override
                    public void setValue(@NonNull Component object, float value) {
                        object.setFlexShrink(value);
                        View hostView = object.getHostView();
                        if (hostView != null) {
                            hostView.requestLayout();
                        }
                    }
                };
            case FLEX_BASIS:
                return new FloatProperty<Component>() {
                    @Override
                    public void setValue(@NonNull Component object, float value) {
                        object.setFlexBasis(value);
                        View hostView = object.getHostView();
                        if (hostView != null) {
                            hostView.requestLayout();
                        }
                    }
                };
            default:
                // should never go here
                return new FloatProperty<Component>() {
                    @Override
                    public void setValue(@NonNull Component object, float value) {
                        object.setFlex(value);
                        View hostView = object.getHostView();
                        if (hostView != null) {
                            hostView.requestLayout();
                        }
                    }
                };
        }
    }

    private void setAllProperty() {
        mFlexGrow = createProperty(FLEX_GROW);
        mFlexShrink = createProperty(FLEX_SHRINK);
        mFlexBasis = createProperty(FLEX_BASIS);
    }

    @Override
    public String[] getTransitionProperties() {
        return mTransitionProperties;
    }

    protected String getPropertyName(@FlexType int flexType) {
        switch (flexType) {
            case FLEX_GROW:
                return Attributes.Style.FLEX_GROW;
            case FLEX_SHRINK:
                return Attributes.Style.FLEX_SHRINK;
            case FLEX_BASIS:
                return Attributes.Style.FLEX_BASIS;
            default:
                // should never get here
                return Attributes.Style.FLEX;
        }
    }

    private void captureValues(@NonNull TransitionValues transitionValues) {
        View target = transitionValues.view;
        if (target instanceof ComponentHost) {
            Component component = ((ComponentHost) target).getComponent();
            if (component != null) {
                if (mFlexType == FLEX) {
                    transitionValues.values
                            .put(Attributes.Style.FLEX_GROW, component.getFlexGrow());
                    transitionValues.values
                            .put(Attributes.Style.FLEX_SHRINK, component.getFlexShrink());
                    transitionValues.values
                            .put(Attributes.Style.FLEX_BASIS, component.getFlexBasis());
                } else if (mFlexType == FLEX_GROW) {
                    transitionValues.values.put(mFlexTypeStr, component.getFlexGrow());
                } else if (mFlexType == FLEX_SHRINK) {
                    transitionValues.values.put(mFlexTypeStr, component.getFlexShrink());
                } else if (mFlexType == FLEX_BASIS) {
                    transitionValues.values.put(mFlexTypeStr, component.getFlexBasis());
                }
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
        if (mFlexType != FLEX) {
            return createAnimator(mFlexType, startValues, endValues);
        } else {
            return createAllAnimators(startValues, endValues);
        }
    }

    protected Animator createAnimator(
            @FlexType int flexType,
            @NonNull TransitionValues startValues,
            @NonNull TransitionValues endValues) {
        String flexTypeStr = getPropertyName(flexType);
        Float startValue = (Float) startValues.values.get(flexTypeStr);
        Float endValue = (Float) endValues.values.get(flexTypeStr);
        if (startValue != null && endValue != null && !startValue.equals(endValue)) {
            View target = endValues.view;
            if (target instanceof ComponentHost) {
                Component component = ((ComponentHost) target).getComponent();
                if (component != null) {
                    Property<Component, Float> flexProperty = chooseProperty(flexType);
                    return ObjectAnimator
                            .ofObject(component, flexProperty, null, startValue, endValue);
                }
            }
        }
        return null;
    }

    protected Property<Component, Float> chooseProperty(@FlexType int flexType) {
        switch (flexType) {
            case FLEX_GROW:
                return mFlexGrow;
            case FLEX_SHRINK:
                return mFlexShrink;
            case FLEX_BASIS:
                return mFlexBasis;
            default:
                // should never get here
                return null;
        }
    }

    protected Animator createAllAnimators(
            @NonNull TransitionValues startValues, @NonNull TransitionValues endValues) {
        Animator flexGrowthAnim = createAnimator(FLEX_GROW, startValues, endValues);
        Animator flexShrinkAnim = createAnimator(FLEX_SHRINK, startValues, endValues);
        Animator flexBasisAnim = createAnimator(FLEX_BASIS, startValues, endValues);
        List<Animator> animatorList = new ArrayList<>(3);
        if (flexGrowthAnim != null) {
            animatorList.add(flexGrowthAnim);
        }
        if (flexShrinkAnim != null) {
            animatorList.add(flexShrinkAnim);
        }
        if (flexBasisAnim != null) {
            animatorList.add(flexBasisAnim);
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

    @IntDef({FLEX_GROW, FLEX_SHRINK, FLEX_BASIS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FlexType {
    }
}
