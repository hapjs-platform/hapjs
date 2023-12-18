/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.drawer;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.metrics.Metrics;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = DrawerNavigation.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class DrawerNavigation extends Container<DrawerPercentFlexLayout> {
    protected static final String WIDGET_NAME = "drawer-navigation";
    private static final float MIN_WIDTH_RATIO = 0.2f;
    private static final float MAX_WIDTH_RATIO = 0.8f;
    private static final String DEFAULT_WIDTH = "80%";

    public DrawerNavigation(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected DrawerPercentFlexLayout createViewImpl() {
        DrawerPercentFlexLayout percentFlexboxLayout = new DrawerPercentFlexLayout(mContext);
        percentFlexboxLayout.setComponent(this);
        DrawerLayout.LayoutParams layoutParams =
                new DrawerLayout.LayoutParams(
                        DrawerLayout.LayoutParams.MATCH_PARENT,
                        DrawerLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.START;
        percentFlexboxLayout.setLayoutParams(layoutParams);
        return percentFlexboxLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.DIRECTION:
                String drawerPosition = Attributes.getString(attribute, "start");
                setDrawerPosition(drawerPosition);
                return true;
            case Attributes.Style.WIDTH:
                String widthStr = Attributes.getString(attribute, DEFAULT_WIDTH);
                if (widthStr.endsWith("%")) {
                    if (!parseWidthPercent(widthStr)) {
                        widthStr = DEFAULT_WIDTH;
                    }
                }
                setWidth(widthStr);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    private void setDrawerPosition(String drawerPosition) {
        if (mHost == null) {
            return;
        }
        int gravity = Gravity.START;
        ViewGroup.LayoutParams layoutParams = mHost.getLayoutParams();
        if (!(layoutParams instanceof DrawerLayout.LayoutParams)) {
            return;
        }

        if ("start".equals(drawerPosition)) {
            gravity = Gravity.START;
        } else if ("end".equals(drawerPosition)) {
            gravity = Gravity.END;
        }

        // 如果左右都存在抽屉，改变direction的值时，需要保证每一边只有一个抽屉
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) layoutParams;
        if (mHost.getParent() instanceof FlexDrawerLayout) {
            FlexDrawerLayout parent = (FlexDrawerLayout) mHost.getParent();
            if (parent != null) {
                int childCount = parent.getChildCount();
                int drawerCount = 0;
                for (int i = 0; i < childCount; i++) {
                    if (parent.getChildAt(i) instanceof DrawerPercentFlexLayout) {
                        drawerCount++;
                    }
                }
                if (drawerCount >= 1) {
                    if (params.gravity != gravity) {
                        mCallback.onJsException(
                                new Exception(
                                        "the drawer just only have one DrawerNavigation on the direction value of start or end"));
                        return;
                    }
                }
            }
        }

        params.gravity = gravity;
        mHost.requestLayout();
    }

    private boolean parseWidthPercent(String widthStr) {
        String temp = widthStr.trim();
        temp = temp.substring(0, temp.indexOf(Metrics.PERCENT));
        if (!TextUtils.isEmpty(temp)) {
            float percentValue = FloatUtil.parse(temp) / 100f;
            if (FloatUtil.floatsEqual(percentValue, MIN_WIDTH_RATIO)
                    || FloatUtil.floatsEqual(percentValue, MAX_WIDTH_RATIO)
                    || (percentValue > MIN_WIDTH_RATIO && percentValue < MAX_WIDTH_RATIO)) {
                return true;
            }
        }
        return false;
    }
}
