/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import androidx.swiperefreshlayout.widget.HapRefreshLayout;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = Refresh.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Refresh extends Container<HapRefreshLayout> {

    protected static final String WIDGET_NAME = "refresh";
    private static final String DEFAULT_OFFSET = "132px";
    private static final String DEFAULT_BACKGROUND_COLOR = "white";
    private static final String DEFAULT_PROGRESS_COLOR = "black";
    private static final String REFRESH_TYPE_DEFAULT = "auto";
    private static final String REFRESH_TYPE_PULL_DOWN = "pulldown";
    private static final String STYLE_REFRESH_TYPE = "type";
    private YogaLayout mYogaLayout;

    public Refresh(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected HapRefreshLayout createViewImpl() {
        HapRefreshLayout refreshLayout = new HapRefreshLayout(mContext);
        refreshLayout.setComponent(this);
        mYogaLayout = new PercentFlexboxLayout(mContext);
        ((PercentFlexboxLayout) mYogaLayout).setComponent(this);
        mYogaLayout.getYogaNode().setFlexDirection(YogaFlexDirection.ROW);
        ViewGroup.MarginLayoutParams lp =
                new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        refreshLayout.addView(mYogaLayout, lp);
        ViewGroup.LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        refreshLayout.setLayoutParams(layoutParams);
        return refreshLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.PROGRESS_COLOR:
                String progressColor = Attributes.getString(attribute, DEFAULT_PROGRESS_COLOR);
                setProgressColor(progressColor);
                return true;
            case Attributes.Style.OFFSET:
                int offset =
                        Attributes.getInt(mHapEngine, attribute,
                                Attributes.getInt(mHapEngine, DEFAULT_OFFSET));
                setOffset(offset);
                return true;
            case Attributes.Style.REFRESHING:
                boolean refreshing = Attributes.getBoolean(attribute, false);
                setRefreshing(refreshing);
                return true;
            case STYLE_REFRESH_TYPE:
                String refreshType = Attributes.getString(attribute, REFRESH_TYPE_DEFAULT);
                setRefreshType(refreshType);
                return true;
            case Attributes.Style.ENABLE_REFRESH:
                boolean enableRefresh = Attributes.getBoolean(attribute, true);
                setEnableRefresh(enableRefresh);
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                String colorStr = Attributes.getString(attribute, DEFAULT_BACKGROUND_COLOR);
                setBackgroundColor(colorStr);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.REFRESH.equals(event)) {
            mHost.setOnRefreshListener(
                    new HapRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            Map<String, Object> params = new HashMap<>();
                            params.put(Attributes.Style.REFRESHING, true);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.REFRESH, Refresh.this,
                                    params, null);
                        }
                    });
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.REFRESH.equals(event)) {
            mHost.setOnRefreshListener(null);
            return true;
        }

        return super.removeEvent(event);
    }

    @Override
    public void setBackgroundColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setProgressBackgroundColorSchemeColor(color);
    }

    @Override
    public YogaLayout getInnerView() {
        if (mHost == null) {
            return null;
        }
        return mYogaLayout;
    }

    public void setProgressColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setColorSchemeColors(color);
    }

    public void setOffset(int offset) {
        if (offset == 0 || mHost == null) {
            return;
        }

        mHost.setProgressViewEndTarget(false, offset);
        if (mHost.isRefreshing()) {
            mHost.setRefreshing(false);
            mHost.setRefreshing(true);
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (mHost == null) {
            return;
        }

        mHost.setRefreshing(refreshing);
    }

    public void setRefreshType(String refreshType) {
        if (mHost == null) {
            return;
        }

        mHost.setPullDownRefresh(REFRESH_TYPE_PULL_DOWN.equals(refreshType));
    }

    public void setEnableRefresh(boolean enableRefresh) {
        if (mHost == null) {
            return;
        }

        mHost.setEnableRefresh(enableRefresh);
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        super.onHostViewAttached(parent);
        if (isParentYogaLayout()
                && !getStyleDomData().containsKey(Attributes.Style.FLEX_GROW)
                && !getStyleDomData().containsKey(Attributes.Style.FLEX)) {
            YogaNode yogaNode = ((YogaLayout) mHost.getParent()).getYogaNodeForView(mHost);
            yogaNode.setFlexGrow(1f);
        }
        setParentFlexGrowRecursive();
    }

    private void setParentFlexGrowRecursive() {
        ViewGroup parentView = (ViewGroup) mHost.getParent();
        Container parentComponent = (Container) getParent();
        while (parentView instanceof YogaLayout
                && !parentComponent.getStyleDomData().containsKey(Attributes.Style.FLEX_GROW)
                && !parentComponent.getStyleDomData().containsKey(Attributes.Style.FLEX)) {
            YogaNode yogaNode = ((YogaLayout) parentView).getYogaNode();
            yogaNode.setFlexGrow(1);

            parentView = (ViewGroup) parentView.getParent();
            parentComponent = (Container) parentComponent.getParent();
        }
    }
}
