/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.hapjs.component.Component;
import org.hapjs.component.animation.BgPositionEvaluator;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;
import org.hapjs.component.view.drawable.SizeBackgroundDrawable.Position;

public class BgPositionTransition extends Transition {

    public static final Property<CSSBackgroundDrawable, Position> BACKGROUND_POSITION;
    private static final String PROPNAME_BACKGROUND_POSITION = "bgPosition";
    private static final String[] TRANSITION_PROPERTIES = {PROPNAME_BACKGROUND_POSITION};

    static {
        BACKGROUND_POSITION =
                new Property<CSSBackgroundDrawable, Position>(Position.class, null) {
                    @Override
                    public void set(CSSBackgroundDrawable object, Position value) {
                        String positionStr = value.getParseStr();
                        object.setPosition(positionStr);
                    }

                    @Override
                    public Position get(CSSBackgroundDrawable object) {
                        return object.getPosition();
                    }
                };
    }

    private int[] mRelativeWidthHeight = new int[2];

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(TransitionValues transitionValues, boolean start) {
        View target = transitionValues.view;
        if (target instanceof ComponentHost) {
            Component component = ((ComponentHost) target).getComponent();
            if (component != null) {
                Position position =
                        component.getOrCreateBackgroundComposer().getBackgroundDrawable()
                                .getPosition();
                if (position != null) {
                    transitionValues.values.put(PROPNAME_BACKGROUND_POSITION, position);
                    if (start) {
                        mRelativeWidthHeight[0] = position.getRelativeWidth();
                        mRelativeWidthHeight[1] = position.getRelativeHeight();
                    } else {
                        position.setRelativeSize(mRelativeWidthHeight[0], mRelativeWidthHeight[1]);
                        position.calculatePx(component.getHapEngine());
                    }
                }
            }
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues, true);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues, false);
    }

    @Override
    public Animator createAnimator(
            ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        Position startPosition = (Position) startValues.values.get(PROPNAME_BACKGROUND_POSITION);
        Position endPosition = (Position) endValues.values.get(PROPNAME_BACKGROUND_POSITION);

        if (startPosition != null
                && endPosition != null
                && isDiffPosition(startPosition, endPosition)) {
            View view = endValues.view;
            if (view instanceof ComponentHost) {
                Component component = ((ComponentHost) view).getComponent();
                if (component != null) {
                    CSSBackgroundDrawable drawable =
                            component.getOrCreateBackgroundComposer().getBackgroundDrawable();
                    return ObjectAnimator.ofObject(
                            drawable,
                            BACKGROUND_POSITION,
                            BgPositionEvaluator.getInstance(),
                            startPosition,
                            endPosition);
                }
            }
        }
        return null;
    }

    private boolean isDiffPosition(@NonNull Position pos1, @NonNull Position pos2) {
        return pos1.getPositionX() != pos2.getPositionX()
                || pos1.getPositionY() != pos2.getPositionY();
    }
}
