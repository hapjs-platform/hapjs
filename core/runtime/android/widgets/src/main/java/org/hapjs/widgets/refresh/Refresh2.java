/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.refresh;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.widgets.custom.WidgetProvider;
import org.hapjs.widgets.view.refresh.DefaultFooterView;
import org.hapjs.widgets.view.refresh.DefaultHeaderView;
import org.hapjs.widgets.view.refresh.Footer;
import org.hapjs.widgets.view.refresh.HapRefreshLayout;
import org.hapjs.widgets.view.refresh.Header;
import org.hapjs.widgets.view.refresh.RefreshContent;
import org.hapjs.widgets.view.refresh.RefreshExtension;
import org.hapjs.widgets.view.refresh.RefreshLayout;

@WidgetAnnotation(
        name = Refresh2.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Refresh2.METHOD_START_PULLDOWN_REFRESH,
                Refresh2.METHOD_START_PULLUP_REFRESH,
                Refresh2.METHOD_STOP_PULLDOWN_REFRESH,
                Refresh2.METHOD_STOP_PULLUP_REFRESH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Refresh2 extends Container<HapRefreshLayout> {

    protected static final String WIDGET_NAME = "refresh2";
    protected static final String METHOD_START_PULLDOWN_REFRESH = "startPullDownRefresh";
    protected static final String METHOD_START_PULLUP_REFRESH = "startPullUpRefresh";
    protected static final String METHOD_STOP_PULLDOWN_REFRESH = "stopPullDownRefresh";
    protected static final String METHOD_STOP_PULLUP_REFRESH = "stopPullUpRefresh";
    /**
     * ************************************兼容refresh组件****************************************
     */
    private static final String DEFAULT_OFFSET = "132px";
    private static final String DEFAULT_BACKGROUND_COLOR = "white";
    private static final String DEFAULT_PROGRESS_COLOR = "black";
    private static final String REFRESH_TYPE_DEFAULT = "auto";
    private static final String REFRESH_TYPE_PULLDOWN = "pulldown";
    /**
     * ******************************************************************************************
     */
    private static final String EVENT_TYPE_PULL_DOWN = "pulldownrefresh";
    private static final String EVENT_TYPE_PULL_UP = "pulluprefresh";
    private static final String ATTR_REFRESH_TYPE = "type";
    private static final String ATTR_PULLDOWN_REFRESHING = "pulldownrefreshing";
    private static final String ATTR_PULLUP_REFRESHING = "pulluprefreshing";
    private static final String ATTR_ANIMATION_DURATION = "animationduration";
    private static final String ATTR_ENABLE_PULL_DOWN = "enablepulldown";
    private static final String ATTR_ENABLE_PULL_UP = "enablepullup";
    private static final String ATTR_REBOUNDABLE = "reboundable";
    private static final String ATTR_GESTURE = "gesture";
    private YogaLayout mContentLayout;
    private Set<String> mRegisterEvents = new HashSet<>();
    private Header mDefaultHeader;
    private Footer mDefaultFooter;

    public Refresh2(
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
        mContentLayout = new PercentFlexboxLayout(mContext);
        ((PercentFlexboxLayout) mContentLayout).setComponent(this);
        mContentLayout.getYogaNode().setFlexDirection(YogaFlexDirection.ROW);
        mContentLayout.getYogaNode().setFlexGrow(1);
        RefreshLayout.MarginLayoutParams lp =
                new RefreshLayout.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mContentLayout.setLayoutParams(lp);
        RefreshContent content = new RefreshContent(mContentLayout);

        HapRefreshLayout refreshLayout = new HapRefreshLayout(mContext);
        refreshLayout.setContent(content);
        refreshLayout.setComponent(this);

        ViewGroup.LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        refreshLayout.setLayoutParams(layoutParams);

        refreshLayout.addPullDownRefreshListener(
                () -> {
                    if (mRegisterEvents.contains(EVENT_TYPE_PULL_DOWN)) {
                        mCallback.onJsEventCallback(
                                getPageId(), getRef(), EVENT_TYPE_PULL_DOWN, Refresh2.this, null,
                                null);
                    }

                    if (mRegisterEvents.contains(Attributes.Event.REFRESH)) {
                        Map<String, Object> params = new HashMap<>();
                        params.put(Attributes.Style.REFRESHING, true);
                        mCallback.onJsEventCallback(
                                getPageId(), getRef(), Attributes.Event.REFRESH, Refresh2.this,
                                params, null);
                    }
                });

        refreshLayout.addPullUpListener(
                () -> {
                    if (mRegisterEvents.contains(EVENT_TYPE_PULL_UP)) {
                        mCallback.onJsEventCallback(
                                getPageId(), getRef(), EVENT_TYPE_PULL_UP, Refresh2.this, null,
                                null);
                    }
                });

        mDefaultHeader = createDefaultHeader(mContext);
        refreshLayout.setHeader(mDefaultHeader);
        refreshLayout.enablePullDown(true);

        mDefaultFooter = createDefaultFooter(mContext);
        refreshLayout.setFooter(mDefaultFooter);

        refreshLayout.enablePullDown(true);
        refreshLayout.enablePullUp(false);
        refreshLayout.enableRebound(false);
        return refreshLayout;
    }

    private Header createDefaultHeader(Context context) {
        WidgetProvider provider = ProviderManager.getDefault().getProvider(WidgetProvider.NAME);
        if (provider != null) {
            Header header = provider.createRefreshHeader(context);
            if (header != null) {
                return header;
            }
        }
        DefaultHeaderView defaultHeaderView = new DefaultHeaderView(context);
        Header header = new Header(defaultHeaderView);
        header.setStyle(RefreshExtension.STYLE_FIXED_FRONT);
        int size = Attributes.getInt(mHapEngine, DEFAULT_OFFSET);
        header.setDisplayHeight(size);
        header.setTriggerRefreshHeight(size);
        header.setMaxDragHeight(
                (int) (size * Header.DEFAULT_MAX_DRAG_RATIO
                        / Header.DEFAULT_TRIGGER_REFRESH_RATIO));
        return header;
    }

    private Footer createDefaultFooter(Context context) {
        WidgetProvider provider = ProviderManager.getDefault().getProvider(WidgetProvider.NAME);
        if (provider != null) {
            Footer footer = provider.createRefreshFooter(context);
            if (footer != null) {
                return footer;
            }
        }
        DefaultFooterView footerView = new DefaultFooterView(context);
        footerView.setLayoutParams(
                new RefreshLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, DisplayUtil.dip2Pixel(context, 60)));
        Footer footer = new Footer(footerView);
        footer.setStyle(RefreshExtension.STYLE_TRANSLATION);
        return footer;
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
                setRefreshing(Attributes.getBoolean(attribute, false));
                return true;
            case ATTR_REFRESH_TYPE:
                String refreshType = Attributes.getString(attribute, REFRESH_TYPE_DEFAULT);
                setRefreshType(refreshType);
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                String colorStr = Attributes.getString(attribute, DEFAULT_BACKGROUND_COLOR);
                setBackgroundColor(colorStr);
                return true;
            case Attributes.Style.FLEX_DIRECTION:
                return true;
            case ATTR_PULLDOWN_REFRESHING:
                boolean pullDownRefreshing = Attributes.getBoolean(attribute, false);
                setPullDownRefreshing(pullDownRefreshing);
                return true;
            case ATTR_PULLUP_REFRESHING:
                boolean pullUpRefreshing = Attributes.getBoolean(attribute, false);
                setPullUpRefreshing(pullUpRefreshing);
                return true;
            case ATTR_ANIMATION_DURATION:
                int animationDuration =
                        Attributes.getInt(mHapEngine, attribute,
                                RefreshLayout.DEFAULT_ANIMATION_DURATION);
                setAnimationDuration(animationDuration);
                return true;
            case ATTR_ENABLE_PULL_DOWN:
                boolean enablePullDown = Attributes.getBoolean(attribute, true);
                enablePullDown(enablePullDown);
                return true;
            case ATTR_ENABLE_PULL_UP:
                boolean enablePullUp = Attributes.getBoolean(attribute, false);
                enablePullUp(enablePullUp);
                return true;
            case ATTR_REBOUNDABLE:
                boolean rebound = Attributes.getBoolean(attribute, false);
                enableReboundable(rebound);
                return true;
            case ATTR_GESTURE:
                boolean gestureEnable = Attributes.getBoolean(attribute, true);
                enableGestureTouch(gestureEnable);
                return true;
            default:
                break;
        }

        // 对自定义的header设置attr
        if (mDefaultHeader != null) {
            View headerView = mDefaultHeader.getView();
            if (headerView instanceof WidgetProvider.AttributeApplier) {
                if (((WidgetProvider.AttributeApplier) headerView).apply(key, attribute)) {
                    return true;
                }
            }
        }

        // 对自定义的footer设置attr
        if (mDefaultFooter != null) {
            View footerView = mDefaultFooter.getView();
            if (footerView instanceof WidgetProvider.AttributeApplier) {
                if (((WidgetProvider.AttributeApplier) footerView).apply(key, attribute)) {
                    return true;
                }
            }
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    public void setBackgroundColor(String colorStr) {
        super.setBackgroundColor(colorStr);
        if (TextUtils.isEmpty(colorStr) || mDefaultHeader == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        View view = mDefaultHeader.getView();
        if (view instanceof DefaultHeaderView) {
            ((DefaultHeaderView) view).setSpinnerColor(color);
        }
    }

    public void setProgressColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || (mDefaultHeader == null && mDefaultFooter == null)) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        if (mDefaultHeader != null) {
            View view = mDefaultHeader.getView();
            if (view instanceof DefaultHeaderView) {
                ((DefaultHeaderView) view).setProgressColor(color);
            }
        }

        if (mDefaultFooter != null) {
            View view = mDefaultFooter.getView();
            if (view instanceof DefaultFooterView) {
                ((DefaultFooterView) view).setLoadingColor(color);
            }
        }
    }

    public void setOffset(int offset) {
        if (offset <= 0) {
            return;
        }

        if (mDefaultHeader == null) {
            return;
        }

        mDefaultHeader.setTriggerRefreshHeight(offset);
        mDefaultHeader.setDisplayHeight(offset);
        mDefaultHeader.setMaxDragHeight(
                (int) (offset * Header.DEFAULT_MAX_DRAG_RATIO
                        / Header.DEFAULT_TRIGGER_REFRESH_RATIO));
        if (mHost != null && mHost.isPullDownRefreshing()) {
            mHost.finishPullDownRefresh();
            mHost.setPullDownRefresh();
        }
    }

    public void setRefreshing(boolean refreshing) {
        setPullDownRefreshing(refreshing);
    }

    public void setRefreshType(String refreshType) {
        if (mDefaultHeader == null) {
            return;
        }
        if (TextUtils.equals(refreshType, REFRESH_TYPE_PULLDOWN)) {
            mDefaultHeader.setStyle(RefreshExtension.STYLE_TRANSLATION);
        } else {
            mDefaultHeader.setStyle(RefreshExtension.STYLE_FIXED_FRONT);
        }
    }

    private void setPullDownRefreshing(boolean refreshing) {
        if (mHost == null) {
            return;
        }
        if (refreshing) {
            mHost.setPullDownRefresh();
        } else {
            mHost.finishPullDownRefresh();
        }
    }

    private void setPullUpRefreshing(boolean refreshing) {
        if (mHost == null) {
            return;
        }
        if (refreshing) {
            mHost.setPullUpRefresh();
        } else {
            mHost.finishPullUpRefresh();
        }
    }

    private void setAnimationDuration(int duration) {
        if (mHost == null) {
            return;
        }
        mHost.setAnimationDuration(duration);
    }

    private void enablePullDown(boolean enable) {
        if (mHost == null) {
            return;
        }
        mHost.enablePullDown(enable);
    }

    private void enablePullUp(boolean enable) {
        if (mHost == null) {
            return;
        }
        mHost.enablePullUp(enable);
    }

    private void enableReboundable(boolean enable) {
        if (mHost == null) {
            return;
        }
        mHost.enableRebound(enable);
    }

    private void enableGestureTouch(boolean enable) {
        if (mHost == null) {
            return;
        }
        mHost.enableGesture(enable);
    }

    @Override
    public void addView(View childView, int index) {
        if (childView instanceof ComponentHost) {
            Component component = ((ComponentHost) childView).getComponent();
            if (component instanceof RefreshHeader) {
                Header header = new Header(((RefreshHeader) component).getHostView());
                mHost.setHeader(header);
                mDefaultHeader = null;
                return;
            } else if (component instanceof RefreshFooter) {
                Footer footer = new Footer(((RefreshFooter) component).getHostView());
                mHost.setFooter(footer);
                return;
            }
        }
        super.addView(childView, index);
    }

    @Override
    public void removeView(View childView) {
        if (childView instanceof ComponentHost) {
            Component component = ((ComponentHost) childView).getComponent();
            if (component instanceof RefreshHeader) {
                mHost.setHeader(null);
                return;
            } else if (component instanceof RefreshFooter) {
                mHost.setFooter(null);
                return;
            }
        }
        super.removeView(childView);
    }

    @Override
    public int offsetIndex(int index) {
        index = super.offsetIndex(index);
        int skip = 0;
        for (Component child : mChildren) {
            if ((child instanceof RefreshHeader) || child instanceof RefreshFooter) {
                skip++;
            }
        }
        index -= skip;
        return index;
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.equals(EVENT_TYPE_PULL_DOWN, event)
                || TextUtils.equals(EVENT_TYPE_PULL_UP, event)
                || TextUtils.equals(Attributes.Event.REFRESH, event)) {

            mRegisterEvents.add(event);
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (mRegisterEvents.contains(event)) {
            mRegisterEvents.remove(event);
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {

        if (mHost != null) {
            if (METHOD_START_PULLDOWN_REFRESH.equals(methodName)) {
                mHost.setPullDownRefresh();
                return;
            } else if (METHOD_START_PULLUP_REFRESH.equals(methodName)) {
                mHost.setPullUpRefresh();
                return;
            } else if (METHOD_STOP_PULLDOWN_REFRESH.equals(methodName)) {
                mHost.finishPullDownRefresh();
                return;
            } else if (METHOD_STOP_PULLUP_REFRESH.equals(methodName)) {
                mHost.finishPullUpRefresh();
                return;
            }
        }
        super.invokeMethod(methodName, args);
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
        mHost.requestLayout();
    }

    private void setParentFlexGrowRecursive() {
        ViewGroup parentView = (ViewGroup) mHost.getParent();
        Container parentComponent = getParent();
        while (parentView instanceof YogaLayout
                && !parentComponent.getStyleDomData().containsKey(Attributes.Style.FLEX_GROW)
                && !parentComponent.getStyleDomData().containsKey(Attributes.Style.FLEX)) {
            YogaNode yogaNode = ((YogaLayout) parentView).getYogaNode();
            yogaNode.setFlexGrow(1);

            parentView = (ViewGroup) parentView.getParent();
            parentComponent = parentComponent.getParent();
        }
    }

    @Override
    public ViewGroup getInnerView() {
        return mContentLayout;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            mHost.removeAllPullDownRefreshListener();
            mHost.removeAllPullUpRefreshListener();
        }
    }
}
