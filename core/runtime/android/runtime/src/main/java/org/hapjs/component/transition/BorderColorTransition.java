/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition;

import android.animation.ArgbEvaluator;
import android.animation.TypeEvaluator;
import android.graphics.drawable.Drawable;
import android.transition.TransitionValues;
import android.util.Property;
import androidx.annotation.NonNull;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.transition.utils.IntProperty;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;

public class BorderColorTransition extends AbstractBoxTransition<CSSBackgroundDrawable, Integer> {

    private static ArgbEvaluator sArgbEvaluator = new ArgbEvaluator();

    public BorderColorTransition(@NonNull String positionProperty) {
        super(positionProperty);
        mTransitionProperties =
                mDirection == Edge.ALL
                        ? new String[] {
                            Attributes.Style.BORDER_LEFT_COLOR,
                            Attributes.Style.BORDER_TOP_COLOR,
                            Attributes.Style.BORDER_RIGHT_COLOR,
                            Attributes.Style.BORDER_BOTTOM_COLOR
                        }
                        : new String[] {getPropertyName(mDirection)};
    }

    @Override
    protected int getDirection(@NonNull String positionProperty) {
        switch (positionProperty) {
            case Attributes.Style.BORDER_LEFT_COLOR:
                return Edge.LEFT;
            case Attributes.Style.BORDER_TOP_COLOR:
                return Edge.TOP;
            case Attributes.Style.BORDER_RIGHT_COLOR:
                return Edge.RIGHT;
            case Attributes.Style.BORDER_BOTTOM_COLOR:
                return Edge.BOTTOM;
            default:
                return Edge.ALL;
        }
    }

    @NonNull
    @Override
    protected Property<CSSBackgroundDrawable, Integer> createProperty(int direction) {
        return new IntProperty<CSSBackgroundDrawable>() {
            @Override
            public void setValue(@NonNull CSSBackgroundDrawable object, int value) {
                int[] borderColor = object.getBorderColor();
                if (borderColor != null && borderColor[direction] != value) {
                    borderColor[direction] = value;
                    mDirty = true;
                }
                if (mDirty) {
                    object.invalidateSelf();
                    mDirty = false;
                }
            }
        }.optimize();
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
                return Attributes.Style.BORDER_LEFT_COLOR;
            case Edge.TOP:
                return Attributes.Style.BORDER_TOP_COLOR;
            case Edge.RIGHT:
                return Attributes.Style.BORDER_RIGHT_COLOR;
            case Edge.BOTTOM:
                return Attributes.Style.BORDER_BOTTOM_COLOR;
            default:
                // should never get here
                return Attributes.Style.BORDER_COLOR;
        }
    }

    @Override
    protected Integer getPropertyValue(@NonNull CSSBackgroundDrawable target, int direction) {
        return target.getDrawBorderColor(direction);
    }

    @Override
    protected TypeEvaluator<Integer> getEvaluator() {
        return sArgbEvaluator;
    }
}
