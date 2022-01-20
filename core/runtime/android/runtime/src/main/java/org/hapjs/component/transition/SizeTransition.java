/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.transition.utils.IntProperty;
import org.hapjs.component.view.ComponentHost;

public class SizeTransition extends Transition {

    /**
     * 宽度或高度，true 为宽度，默认值 false 为高度.
     */
    private boolean isWidthOrHeight;

    private String[] mTransitionProperties;

    private IntProperty<Component> mSizeProperty;

    public SizeTransition(boolean widthOrHeight) {
        super();
        isWidthOrHeight = widthOrHeight;
        if (widthOrHeight) {
            mSizeProperty =
                    new IntProperty<Component>() {
                        @Override
                        public void setValue(@NonNull Component object, int value) {
                            object.setWidth(String.valueOf(value));
                        }
                    };
        } else {
            mSizeProperty =
                    new IntProperty<Component>() {
                        @Override
                        public void setValue(@NonNull Component object, int value) {
                            object.setHeight(String.valueOf(value));
                        }
                    };
        }
        mTransitionProperties = new String[] {getPropertyName()};
    }

    @Override
    public String[] getTransitionProperties() {
        return mTransitionProperties;
    }

    private String getPropertyName() {
        return isWidthOrHeight ? Attributes.Style.WIDTH : Attributes.Style.HEIGHT;
    }

    private void captureValues(TransitionValues transitionValues) {
        View target = transitionValues.view;
        if (target instanceof ComponentHost) {
            Component component = ((ComponentHost) target).getComponent();
            if (component != null) {
                transitionValues.values.put(
                        getPropertyName(),
                        isWidthOrHeight ? component.getWidth() : component.getHeight());
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
        String propertyName = getPropertyName();
        Integer startSize = (Integer) startValues.values.get(propertyName);
        Integer endSize = (Integer) endValues.values.get(propertyName);

        if (startSize != null && endSize != null && !startSize.equals(endSize)) {
            View view = endValues.view;
            if (view instanceof ComponentHost) {
                Component component = ((ComponentHost) view).getComponent();
                if (component != null && mSizeProperty != null) {
                    return ObjectAnimator
                            .ofObject(component, mSizeProperty, null, startSize, endSize);
                }
            }
        }
        return null;
    }
}
