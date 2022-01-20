/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.Property;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.hapjs.component.transition.utils.IntProperty;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;

public class BgColorTransition extends Transition {

    public static final Property<CSSBackgroundDrawable, Integer> BACKGROUND_COLOR;
    private static final String PROPNAME_BACKGROUND_COLOR = "bgColor";
    private static final String[] TRANSITION_PROPERTIES = {PROPNAME_BACKGROUND_COLOR};

    static {
        BACKGROUND_COLOR =
                new IntProperty<CSSBackgroundDrawable>() {
                    @Override
                    public void setValue(@NonNull CSSBackgroundDrawable object, int value) {
                        object.setColor(value);
                    }
                }.optimize();
    }

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(TransitionValues transitionValues) {
        Drawable background = transitionValues.view.getBackground();
        if (background instanceof CSSBackgroundDrawable) {
            transitionValues.values.put(
                    PROPNAME_BACKGROUND_COLOR, ((CSSBackgroundDrawable) background).getColor());
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

        Integer startBackgroundColor = (Integer) startValues.values.get(PROPNAME_BACKGROUND_COLOR);
        Integer endBackgroundColor = (Integer) endValues.values.get(PROPNAME_BACKGROUND_COLOR);
        ObjectAnimator bgAnimator = null;
        if (startBackgroundColor != null && endBackgroundColor != null) {
            final int startColor = startBackgroundColor;
            final int endColor = endBackgroundColor;
            if (startColor != endColor) {
                CSSBackgroundDrawable endDrawable =
                        (CSSBackgroundDrawable) endValues.view.getBackground();
                bgAnimator =
                        ObjectAnimator.ofInt(endDrawable, BACKGROUND_COLOR, startColor, endColor);
                bgAnimator.setEvaluator(new ArgbEvaluator());
            }
        }
        return bgAnimator;
    }
}
