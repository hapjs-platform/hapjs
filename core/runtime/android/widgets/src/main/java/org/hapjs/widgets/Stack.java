/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.PercentFrameLayout;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = Stack.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_REQUEST_FULLSCREEN,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Stack extends Container<PercentFrameLayout> {

    protected static final String WIDGET_NAME = "stack";

    private YogaFlexDirection mFlexDirection = YogaFlexDirection.ROW;
    private YogaJustify mJustifyContent = YogaJustify.FLEX_START;
    private YogaAlign mAlignItems = YogaAlign.STRETCH;

    public Stack(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected PercentFrameLayout createViewImpl() {
        PercentFrameLayout percentFrameLayout = new PercentFrameLayout(mContext);
        percentFrameLayout.setComponent(this);
        return percentFrameLayout;
    }

    @Override
    public void addView(View childView, int index) {
        if (mHost == null || childView == null) {
            return;
        }

        PercentFlexboxLayout flexboxLayout = (PercentFlexboxLayout) getInnerView();
        flexboxLayout.setComponent(this);
        flexboxLayout.getYogaNode().setFlexDirection(mFlexDirection);
        flexboxLayout.getYogaNode().setJustifyContent(mJustifyContent);
        flexboxLayout.getYogaNode().setAlignItems(mAlignItems);
        FrameLayout.LayoutParams frameLp =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        mHost.addView(flexboxLayout, index, frameLp);

        ViewGroup.LayoutParams params = childView.getLayoutParams();
        if (params == null) {
            params =
                    new YogaLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        flexboxLayout.addView(childView, params);

        if (childView instanceof ComponentHost) {
            ((ComponentHost) childView).getComponent().onHostViewAttached(flexboxLayout);
        }
    }

    @Override
    public void removeView(View child) {
        if (mHost != null) {
            for (int i = 0; i < mHost.getChildCount(); i++) {
                ViewGroup childContainer = (ViewGroup) mHost.getChildAt(i);
                if (childContainer.getChildCount() > 0
                        && childContainer.getChildAt(0).equals(child)) {
                    childContainer.removeView(child);
                    mHost.removeView(childContainer);
                    break;
                }
            }
        }
    }

    @Override
    public View getChildViewAt(int index) {
        if (index < 0 || index >= getChildCount() || mHost == null) {
            return null;
        }

        ViewGroup childContainer = (ViewGroup) mHost.getChildAt(index);
        if (childContainer != null && childContainer.getChildCount() == 1) {
            return childContainer.getChildAt(0);
        }

        return null;
    }

    @Override
    public void setFlexDirection(String flexDirectionStr) {
        if (TextUtils.isEmpty(flexDirectionStr)) {
            return;
        }
        YogaFlexDirection flexDirection = YogaFlexDirection.ROW;
        if ("column".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.COLUMN;
        } else if ("row-reverse".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.ROW_REVERSE;
        } else if ("column-reverse".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.COLUMN_REVERSE;
        }
        mFlexDirection = flexDirection;

        if (mHost != null) {
            for (int i = 0; i < mHost.getChildCount(); i++) {
                YogaLayout childContainer = (YogaLayout) mHost.getChildAt(i);
                childContainer.getYogaNode().setFlexDirection(flexDirection);
                if (childContainer.getYogaNode().isDirty()) {
                    childContainer.requestLayout();
                }
            }
        }
    }

    @Override
    public void setJustifyContent(String justifyContentStr) {
        if (TextUtils.isEmpty(justifyContentStr)) {
            return;
        }

        YogaJustify justifyContent = YogaJustify.FLEX_START;
        if ("flex-end".equals(justifyContentStr)) {
            justifyContent = YogaJustify.FLEX_END;
        } else if ("center".equals(justifyContentStr)) {
            justifyContent = YogaJustify.CENTER;
        } else if ("space-between".equals(justifyContentStr)) {
            justifyContent = YogaJustify.SPACE_BETWEEN;
        } else if ("space-around".equals(justifyContentStr)) {
            justifyContent = YogaJustify.SPACE_AROUND;
        }
        mJustifyContent = justifyContent;

        if (mHost != null) {
            for (int i = 0; i < mHost.getChildCount(); i++) {
                YogaLayout childContainer = (YogaLayout) mHost.getChildAt(i);
                childContainer.getYogaNode().setJustifyContent(justifyContent);
                if (childContainer.getYogaNode().isDirty()) {
                    childContainer.requestLayout();
                }
            }
        }
    }

    @Override
    public void setAlignItems(String alignItemsStr) {
        if (TextUtils.isEmpty(alignItemsStr)) {
            return;
        }

        YogaAlign alignItems = YogaAlign.STRETCH;
        if ("flex-start".equals(alignItemsStr)) {
            alignItems = YogaAlign.FLEX_START;
        } else if ("flex-end".equals(alignItemsStr)) {
            alignItems = YogaAlign.FLEX_END;
        } else if ("center".equals(alignItemsStr)) {
            alignItems = YogaAlign.CENTER;
        }
        mAlignItems = alignItems;

        if (mHost != null) {
            for (int i = 0; i < mHost.getChildCount(); i++) {
                YogaLayout flexboxContainer = (YogaLayout) mHost.getChildAt(i);
                flexboxContainer.getYogaNode().setAlignItems(alignItems);
                if (flexboxContainer.getYogaNode().isDirty()) {
                    flexboxContainer.requestLayout();
                }
            }
        }
    }

    @Override
    public ViewGroup getInnerView() {
        return new PercentFlexboxLayout(mContext);
    }
}
