/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.transition.TransitionValues;
import android.util.Property;
import android.view.View;
import androidx.annotation.NonNull;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.transition.utils.FloatProperty;
import org.hapjs.component.view.ComponentHost;

public class PositionTransition extends AbstractBoxTransition<Component, Float> {

    public PositionTransition(@NonNull String positionProperty) {
        super(positionProperty);
        mTransitionProperties =
                mDirection == Edge.ALL
                        ? new String[] {
                            Attributes.Style.LEFT,
                            Attributes.Style.TOP,
                            Attributes.Style.RIGHT,
                            Attributes.Style.BOTTOM
                        }
                        : new String[] {getPropertyName(mDirection)};
    }

    @Override
    protected int getDirection(@NonNull String position) {
        switch (position) {
            case Attributes.Style.LEFT:
                return Edge.LEFT;
            case Attributes.Style.TOP:
                return Edge.TOP;
            case Attributes.Style.RIGHT:
                return Edge.RIGHT;
            case Attributes.Style.BOTTOM:
                return Edge.BOTTOM;
            default:
                return Edge.ALL;
        }
    }

    @NonNull
    @Override
    protected Property<Component, Float> createProperty(int direction) {
        return new FloatProperty<Component>() {
            @Override
            public void setValue(@NonNull Component object, float value) {
                object.setPosition(getPropertyName(direction), value);
                View hostView = object.getHostView();
                if (hostView != null) {
                    hostView.requestLayout();
                }
            }
        };
    }

    @Override
    protected Component getTarget(@NonNull TransitionValues values) {
        return values.view instanceof ComponentHost
                ? ((ComponentHost) values.view).getComponent()
                : null;
    }

    @Override
    protected String getPropertyName(int direction) {
        switch (direction) {
            case Edge.LEFT:
                return Attributes.Style.LEFT;
            case Edge.TOP:
                return Attributes.Style.TOP;
            case Edge.RIGHT:
                return Attributes.Style.RIGHT;
            case Edge.BOTTOM:
                return Attributes.Style.BOTTOM;
            default:
                // should never get here
                return Attributes.Style.POSITION;
        }
    }

    @Override
    protected Float getPropertyValue(@NonNull Component target, int direction) {
        float position = target.getPosition(getPropertyName(direction));
        if (FloatUtil.floatsEqual(FloatUtil.UNDEFINED, position)) {
            return null;
        }
        return position;
    }
}
