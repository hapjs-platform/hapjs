/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.graphics.drawable.Drawable;
import android.transition.TransitionValues;
import android.util.Property;
import androidx.annotation.NonNull;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.constants.Spacing;
import org.hapjs.component.transition.utils.FloatProperty;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;

public class BorderWidthTransition extends AbstractBoxTransition<CSSBackgroundDrawable, Float> {

    public BorderWidthTransition(@NonNull String positionProperty) {
        super(positionProperty);
        mTransitionProperties =
                mDirection == Edge.ALL
                        ? new String[] {
                            Attributes.Style.BORDER_LEFT_WIDTH,
                            Attributes.Style.BORDER_TOP_WIDTH,
                            Attributes.Style.BORDER_RIGHT_WIDTH,
                            Attributes.Style.BORDER_BOTTOM_WIDTH
                        }
                        : new String[] {getPropertyName(mDirection)};
    }

    @Override
    protected int getDirection(@NonNull String positionProperty) {
        switch (positionProperty) {
            case Attributes.Style.BORDER_LEFT_WIDTH:
                return Edge.LEFT;
            case Attributes.Style.BORDER_TOP_WIDTH:
                return Edge.TOP;
            case Attributes.Style.BORDER_RIGHT_WIDTH:
                return Edge.RIGHT;
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
                return Edge.BOTTOM;
            default:
                return Edge.ALL;
        }
    }

    @NonNull
    @Override
    protected Property<CSSBackgroundDrawable, Float> createProperty(int direction) {
        return new FloatProperty<CSSBackgroundDrawable>() {
            @Override
            public void setValue(@NonNull CSSBackgroundDrawable object, float value) {
                Spacing borderWidth = object.getBorderWidth();
                if (borderWidth != null) {
                    value = value > 0 && value < 1 ? 1 : value;
                    mDirty = borderWidth.set(direction, value);
                }
                if (mDirty) {
                    object.invalidateSelf();
                    mDirty = false;
                }
            }
        };
    }

    @Override
    protected CSSBackgroundDrawable getTarget(@NonNull TransitionValues values) {
        Drawable background = values.view.getBackground();
        return background instanceof CSSBackgroundDrawable ? (CSSBackgroundDrawable) background :
                null;
    }

    @Override
    protected String getPropertyName(int direction) {
        switch (direction) {
            case Edge.LEFT:
                return Attributes.Style.BORDER_LEFT_WIDTH;
            case Edge.TOP:
                return Attributes.Style.BORDER_TOP_WIDTH;
            case Edge.RIGHT:
                return Attributes.Style.BORDER_RIGHT_WIDTH;
            case Edge.BOTTOM:
                return Attributes.Style.BORDER_BOTTOM_WIDTH;
            default:
                // should never get here
                return Attributes.Style.BORDER_WIDTH;
        }
    }

    @Override
    protected Float getPropertyValue(@NonNull CSSBackgroundDrawable target, int direction) {
        return target.getDrawBorderWidth(direction);
    }
}
