/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.transition.utils.TransitionUtils;

public class TransformOriginTransition extends Transition {

    private static final String PROPNAME_PIVOT_X = "pivotX";
    private static final String PROPNAME_PIVOT_Y = "pivotY";
    private static final String[] TRANSITION_PROPERTIES = {PROPNAME_PIVOT_X, PROPNAME_PIVOT_Y};

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(@NonNull TransitionValues transitionValues) {
        transitionValues.values.put(PROPNAME_PIVOT_X, transitionValues.view.getPivotX());
        transitionValues.values.put(PROPNAME_PIVOT_Y, transitionValues.view.getPivotY());
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

        Float startX = (Float) startValues.values.get(PROPNAME_PIVOT_X);
        Float startY = (Float) startValues.values.get(PROPNAME_PIVOT_Y);
        Float endX = (Float) endValues.values.get(PROPNAME_PIVOT_X);
        Float endY = (Float) endValues.values.get(PROPNAME_PIVOT_Y);

        Animator pxAnimator = null;
        Animator pyAnimator = null;
        if (startX != null && endX != null && !FloatUtil.floatsEqual(startX, endX)) {
            pxAnimator = ObjectAnimator.ofFloat(endValues.view, PROPNAME_PIVOT_X, startX, endX);
        }
        if (startY != null && endY != null && !FloatUtil.floatsEqual(startY, endY)) {
            pyAnimator = ObjectAnimator.ofFloat(endValues.view, PROPNAME_PIVOT_Y, startY, endY);
        }
        return TransitionUtils.mergeAnimators(pxAnimator, pyAnimator);
    }
}
