/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;
import org.hapjs.common.utils.FloatUtil;

public class OpacityTransition extends Transition {

    static final String PROPNAME_ALPHA = "alpha";
    private static final String[] TRANSITION_PROPERTIES = {PROPNAME_ALPHA};

    private static float getAlpha(TransitionValues values) {
        float alpha = 1f;
        if (values != null) {
            Float alphaFloat = (Float) values.values.get(PROPNAME_ALPHA);
            if (alphaFloat != null) {
                alpha = alphaFloat;
            }
        }
        return alpha;
    }

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROPNAME_ALPHA, transitionValues.view.getAlpha());
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
        return createAnimation(startValues.view, getAlpha(startValues), getAlpha(endValues));
    }

    private Animator createAnimation(final View view, float startAlpha, final float endAlpha) {
        if (FloatUtil.floatsEqual(startAlpha, endAlpha)) {
            return null;
        }

        final ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, endAlpha);

        final OpacityAnimatorListener listener = new OpacityAnimatorListener(view);
        anim.addListener(listener);
        return anim;
    }

    private static class OpacityAnimatorListener extends AnimatorListenerAdapter {
        private final View mView;
        private boolean mLayerTypeChanged = false;

        public OpacityAnimatorListener(View view) {
            mView = view;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (mView.hasOverlappingRendering() && mView.getLayerType() == View.LAYER_TYPE_NONE) {
                mLayerTypeChanged = true;
                mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (mLayerTypeChanged) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }
}
