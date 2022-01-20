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

public class PaddingTransition extends AbstractBoxTransition<Component, Float> {

    public PaddingTransition(@NonNull String positionProperty) {
        super(positionProperty);
        mTransitionProperties =
                mDirection == Edge.ALL
                        ? new String[] {
                            Attributes.Style.PADDING_LEFT,
                            Attributes.Style.PADDING_TOP,
                            Attributes.Style.PADDING_RIGHT,
                            Attributes.Style.PADDING_BOTTOM
                        }
                        : new String[] {getPropertyName(mDirection)};
    }

    @Override
    protected int getDirection(@NonNull String positionProperty) {
        switch (positionProperty) {
            case Attributes.Style.PADDING_LEFT:
                return Edge.LEFT;
            case Attributes.Style.PADDING_TOP:
                return Edge.TOP;
            case Attributes.Style.PADDING_RIGHT:
                return Edge.RIGHT;
            case Attributes.Style.PADDING_BOTTOM:
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
                if (!FloatUtil.floatsEqual(object.getPadding(direction), value)) {
                    object.setPadding(direction, value);
                    mDirty = true;
                }
                if (mDirty) {
                    object.setRealPadding();
                    View hostView = object.getHostView();
                    if (hostView != null) {
                        hostView.requestLayout();
                    }
                    mDirty = false;
                }
            }
        };
    }

    @Override
    protected Component getTarget(@NonNull TransitionValues values) {
        View target = values.view;
        return target instanceof ComponentHost ? ((ComponentHost) target).getComponent() : null;
    }

    @Override
    protected String getPropertyName(int direction) {
        switch (direction) {
            case Edge.LEFT:
                return Attributes.Style.PADDING_LEFT;
            case Edge.TOP:
                return Attributes.Style.PADDING_TOP;
            case Edge.RIGHT:
                return Attributes.Style.PADDING_RIGHT;
            case Edge.BOTTOM:
                return Attributes.Style.PADDING_BOTTOM;
            default:
                // should never get here
                return Attributes.Style.PADDING;
        }
    }

    @Override
    protected Float getPropertyValue(@NonNull Component target, int direction) {
        return target.getPadding(direction);
    }
}
