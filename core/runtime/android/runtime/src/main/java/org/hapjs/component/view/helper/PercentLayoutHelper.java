/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.helper;

import android.view.View;
import android.view.ViewGroup;
import com.facebook.yoga.YogaNode;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.view.YogaLayout;

public class PercentLayoutHelper {
    private static final String TAG = "PercentLayoutHelper";

    public static void adjustChildren(
            int widthMeasureSpec, int heightMeasureSpec, Container container) {
        if (container == null) {
            return;
        }

        int widthHint = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightHint = View.MeasureSpec.getSize(heightMeasureSpec);
        int n = container.getChildCount();
        for (int i = 0; i < n; i++) {
            Component c = container.getChildAt(i);
            if (c == null) {
                continue;
            }
            View view = c.getHostView();
            if (view == null || view.getLayoutParams() == null) {
                continue;
            }

            YogaNode childNode = null;
            if (view instanceof YogaLayout) {
                childNode = ((YogaLayout) view).getYogaNode();
            }
            ViewGroup.LayoutParams lp = view.getLayoutParams();

            float percentWidth = c.getPercentWidth();
            if (percentWidth >= 0) {
                lp.width = (int) (widthHint * percentWidth);
                if (childNode != null) {
                    childNode.setWidth(lp.width);
                }
            }

            float percentHeight = c.getPercentHeight();
            if (percentHeight >= 0) {
                lp.height = (int) (heightHint * percentHeight);
                if (childNode != null) {
                    childNode.setHeight(lp.height);
                }
            }
        }
    }
}
