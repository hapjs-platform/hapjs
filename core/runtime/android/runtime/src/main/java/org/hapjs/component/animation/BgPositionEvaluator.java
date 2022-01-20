/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.animation.TypeEvaluator;
import org.hapjs.component.view.drawable.SizeBackgroundDrawable.Position;

/**
 * Evaluator class for background-position property animation
 */
public class BgPositionEvaluator implements TypeEvaluator<Position> {
    private static final BgPositionEvaluator INSTANCE = new BgPositionEvaluator();

    public static BgPositionEvaluator getInstance() {
        return INSTANCE;
    }

    @Override
    public Position evaluate(float fraction, Position startValue, Position endValue) {
        int startPositionX = startValue.getPositionX();
        int startPositionY = startValue.getPositionY();
        int endPositionX = endValue.getPositionX();
        int endPositionY = endValue.getPositionY();
        int relativeWidth = endValue.getRelativeWidth();
        int relativeHeight = endValue.getRelativeHeight();
        float evaluateX = startPositionX + fraction * (endPositionX - startPositionX);
        float evaluateY = startPositionY + fraction * (endPositionY - startPositionY);
        float percentX = relativeWidth == 0 ? 0 : evaluateX * 100 / relativeWidth;
        float percentY = relativeHeight == 0 ? 0 : evaluateY * 100 / relativeHeight;
        String positionStr = "left " + percentX + "% top " + percentY + "%";
        return Position.parse(positionStr);
    }
}
