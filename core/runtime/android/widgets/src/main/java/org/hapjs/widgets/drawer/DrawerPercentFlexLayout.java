/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.drawer;

import android.content.Context;
import android.view.ViewGroup;
import com.facebook.yoga.YogaNode;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;

public class DrawerPercentFlexLayout extends PercentFlexboxLayout {

    public DrawerPercentFlexLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float mMinWidthRatio = 0.2f;
        float mMaxWidthRatio = 0.8f;
        if (!(getParent() instanceof YogaLayout)) {
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            YogaNode node = getYogaNode();

            if (getParent() != null) {
                int parentWidth = ((ViewGroup) getParent()).getMeasuredWidth();
                int maxWidth = (int) (parentWidth * mMaxWidthRatio);
                int minWidth = (int) (parentWidth * mMinWidthRatio);
                if (getComponent() != null) {
                    if (getComponent().isWidthDefined()) {
                        if (widthMode == MeasureSpec.EXACTLY) {
                            float percentWidth = getComponent().getPercentWidth();
                            if (!FloatUtil.isUndefined(percentWidth)
                                    && !FloatUtil.floatsEqual(percentWidth, -1)
                                    && percentWidth >= 0
                                    && percentWidth <= 1) {
                                node.setWidth(percentWidth * parentWidth);
                                widthMeasureSpec =
                                        MeasureSpec.makeMeasureSpec(
                                                (int) (percentWidth * parentWidth),
                                                MeasureSpec.EXACTLY);
                            } else {
                                if (widthSize > maxWidth || widthSize < minWidth) {
                                    node.setWidth(maxWidth);
                                    widthMeasureSpec = MeasureSpec
                                            .makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
                                }
                            }
                        }
                    } else {
                        if (widthSize > maxWidth || widthSize < minWidth) {
                            node.setWidth(maxWidth);
                            widthMeasureSpec =
                                    MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
                        }
                    }
                }
            }

            if (heightMode == MeasureSpec.EXACTLY) {
                if (getComponent() != null) {
                    if ((getParent() instanceof FlexDrawerLayout)
                            && getComponent().isHeightDefined()) {
                        float percentHeight = getComponent().getPercentHeight();
                        if (!FloatUtil.isUndefined(percentHeight)
                                && !FloatUtil.floatsEqual(percentHeight, -1)
                                && percentHeight >= 0
                                && percentHeight <= 1) {
                            int parentHeight = ((ViewGroup) getParent()).getMeasuredHeight();
                            node.setHeight(parentHeight * percentHeight);
                            heightMeasureSpec =
                                    MeasureSpec.makeMeasureSpec(
                                            (int) (parentHeight * percentHeight),
                                            MeasureSpec.EXACTLY);
                        }
                    }
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
