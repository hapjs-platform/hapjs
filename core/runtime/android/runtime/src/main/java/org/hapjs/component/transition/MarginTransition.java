/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.transition.TransitionValues;
import android.util.Property;
import android.view.View;
import androidx.annotation.NonNull;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.transition.utils.IntProperty;
import org.hapjs.component.view.ComponentHost;

public class MarginTransition extends AbstractBoxTransition<Component, Integer> {

    public MarginTransition(@NonNull String positionProperty) {
        super(positionProperty);
        mTransitionProperties =
                mDirection == Edge.ALL
                        ? new String[] {
                            Attributes.Style.MARGIN_LEFT,
                            Attributes.Style.MARGIN_TOP,
                            Attributes.Style.MARGIN_RIGHT,
                            Attributes.Style.MARGIN_BOTTOM
                        }
                        : new String[] {getPropertyName(mDirection)};
    }

    @Override
    protected int getDirection(@NonNull String position) {
        switch (position) {
            case Attributes.Style.MARGIN_LEFT:
                return Edge.LEFT;
            case Attributes.Style.MARGIN_TOP:
                return Edge.TOP;
            case Attributes.Style.MARGIN_RIGHT:
                return Edge.RIGHT;
            case Attributes.Style.MARGIN_BOTTOM:
                return Edge.BOTTOM;
            default:
                return Edge.ALL;
        }
    }

    @NonNull
    @Override
    protected Property<Component, Integer> createProperty(int direction) {
        return new IntProperty<Component>() {
            @Override
            public void setValue(@NonNull Component object, int value) {
                object.setMargin(getPropertyName(direction), value);
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
                return Attributes.Style.MARGIN_LEFT;
            case Edge.TOP:
                return Attributes.Style.MARGIN_TOP;
            case Edge.RIGHT:
                return Attributes.Style.MARGIN_RIGHT;
            case Edge.BOTTOM:
                return Attributes.Style.MARGIN_BOTTOM;
            default:
                // should never get here
                return Attributes.Style.MARGIN;
        }
    }

    @Override
    protected Integer getPropertyValue(@NonNull Component target, int direction) {
        return target.getMargin(getPropertyName(direction));
    }
}
