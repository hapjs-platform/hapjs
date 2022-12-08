/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.tab;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.facebook.yoga.YogaNode;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.AbstractScrollable;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.HScrollable;
import org.hapjs.component.OnDomTreeChangeListener;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.GestureDispatcher;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.state.State;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;
import org.hapjs.widgets.view.PercentTabLayout;
import org.json.JSONArray;
import org.json.JSONException;

@WidgetAnnotation(
        name = TabBar.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class TabBar extends AbstractScrollable<PercentTabLayout>
        implements OnDomTreeChangeListener, HScrollable {
    protected static final String WIDGET_NAME = "tab-bar";
    private static final String TAG = "TabBar";
    private int mDomIndex;
    private static final String TALKBACK_TABS_LABELS = "arialabels";
    private List<String> mDescriptions = null;


    public TabBar(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);

        if (parent instanceof Tabs) {
            ((Tabs) parent)
                    .addOnTabChangeListener(
                            new Tabs.OnTabChangeListener() {
                                @Override
                                public void onTabActive(int index) {
                                    for (int i = 0; i < mChildren.size(); i++) {
                                        boolean active = false;
                                        if (i == index) {
                                            active = true;
                                        }
                                        processStateChanged(mChildren.get(i), State.ACTIVE, active);
                                    }
                                }
                            });
        }
    }

    private void processStateChanged(Component component, String state, boolean stateValue) {
        StateHelper.onActiveStateChanged(component, stateValue);
        if (component instanceof Container) {
            for (int i = 0; i < ((Container) component).getChildCount(); i++) {
                processStateChanged(((Container) component).getChildAt(i), state, stateValue);
            }
        }
    }

    @Override
    protected PercentTabLayout createViewImpl() {
        PercentTabLayout tabLayout = new PercentTabLayout(mContext);
        tabLayout.setComponent(this);
        // fake yoga node.
        mNode = new YogaNode();
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setLayoutParams(params);
        tabLayout.setSelectedTabIndicatorHeight(0);
        tabLayout.setScrollListener(
                new PercentTabLayout.ScrollListener() {
                    @Override
                    public void onScrollChanged(
                            PercentTabLayout percentTabLayout, int x, int y, int oldx, int oldy) {
                        processAppearanceEvent();
                    }
                });
        return tabLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.MODE:
                String mode = Attributes.getString(attribute, Attributes.Mode.FIXED);
                setMode(mode);
                return true;
            case TALKBACK_TABS_LABELS:
                initTabsDesp(attribute);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    private void initTabsDesp(Object attribute) {
        boolean isTalkBackEnable = isEnableTalkBack();
        if (isTalkBackEnable && attribute instanceof JSONArray) {
            mDescriptions = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) attribute;
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    Object description = jsonArray.get(i);
                    if (description instanceof String) {
                        mDescriptions.add((String) description);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "initTabsDesp failed ", e);
                }
            }
        }
    }

    private void setMode(String modeStr) {
        if (mHost == null) {
            return;
        }

        switch (modeStr) {
            case Attributes.Mode.SCROLLABLE:
                mHost.setTabMode(TabLayout.MODE_SCROLLABLE);
                break;
            case Attributes.Mode.FIXED:
                mHost.setTabMode(TabLayout.MODE_FIXED);
                mHost.setTabGravity(TabLayout.GRAVITY_FILL);
                break;
            default:
                break;
        }
    }

    @Override
    public void setFlex(float flex) {
        if (mHost == null) {
            return;
        }

        if (mHost.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) mHost.getLayoutParams()).weight = flex;
        }
    }

    @Override
    public void addChild(Component child, int index) {
        super.addChild(child, index);

        if (mDomIndex == mChildren.indexOf(child)) {
            child.addOnDomTreeChangeListener(this);
            final TabLayout tabLayout = mHost;
            tabLayout.addOnLayoutChangeListener(
                    new View.OnLayoutChangeListener() {
                        @Override
                        public void onLayoutChange(
                                View view, int l, int t, int r, int b, int oldL, int oldT, int oldR,
                                int oldB) {
                            setSelectTab(mDomIndex);
                            tabLayout.removeOnLayoutChangeListener(this);
                        }
                    });
        }
    }

    @Override
    public void addView(View childView, final int index) {
        if (mHost == null) {
            return;
        }
        final TabLayout.Tab tab = mHost.newTab();
        final ViewGroup tabLayoutView = tab.view;
        tabLayoutView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        TabBarLayoutWrapper wrapper = new TabBarLayoutWrapper(mContext);
        wrapper.setGravity(Gravity.CENTER);
        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        wrapper.setLayoutParams(layoutParams);
        wrapper.addView(childView);
        if (isEnableTalkBack()) {
            String description = null;
            if (null != mDescriptions && index < mDescriptions.size()) {
                description = mDescriptions.get(index);
            }
            wrapper.initTalkBack(childView, index, description);
        }
        tab.setCustomView(wrapper);
        mHost.addTab(tab, index, false);

        final ViewGroup tabView = (ViewGroup) wrapper.getParent();
        tabView.setPadding(0, 0, 0, 0);
        tabView.setMinimumWidth(0);
        final Component child = mChildren.get(index);
        wrapper.setTouchEventCallback(
                new TabBarLayoutWrapper.TouchEventCallback() {
                    @Override
                    public void touchEvent(MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_UP:
                                tab.select();

                                // wrapper match_parent,防止没有点击child，而是其他空白地方无法处理tab的点击
                                if (child.getDomEvents().contains(Attributes.Event.CLICK)) {
                                    if (mMinPlatformVersion < 1040) {
                                        // 1030开始事件统一通过GestureDelegate处理。
                                        GestureDispatcher dispatcher =
                                                GestureDispatcher
                                                        .createInstanceIfNecessary(getCallback());
                                        // 防止重复事件
                                        if (dispatcher != null
                                                && !dispatcher.contains(
                                                child.getPageId(), child.getRef(),
                                                Attributes.Event.CLICK)) {
                                            if (child.getHostView() instanceof GestureHost) {
                                                IGesture gesture =
                                                        ((GestureHost) child.getHostView())
                                                                .getGesture();
                                                if (gesture instanceof GestureDelegate) {
                                                    // 强制触发click事件
                                                    // !!!bug 这种方式计算的touches，changed_touches不正确，实际click并没有发生在child上
                                                    // 只是为了在点击tab的时候能够正常将click的操作传给framework
                                                    ((GestureDelegate) gesture)
                                                            .onSingleTapUp(event);
                                                }
                                            }
                                        }
                                    } else {
                                        if (child.getHostView() instanceof GestureHost) {
                                            // 1040开始dispatcher中只保留target事件，这里无需进行重复判断
                                            IGesture gesture = ((GestureHost) child.getHostView())
                                                    .getGesture();
                                            if (gesture instanceof GestureDelegate) {
                                                // 强制触发click事件
                                                // bug 这种方式计算的touches，changed_touches不正确，实际click并没有发生在child上
                                                // 只是为了在点击tab的时候能够正常将click的操作传给framework
                                                ((GestureDelegate) gesture).onSingleTapUp(event);
                                            }
                                        }
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    @Override
    public void removeChild(Component child) {
        int index = removeChildInternal(child);
        if (mHost != null) {
            int lastIndex = mHost.getTabCount() - 1;
            if (index < 0 || index > lastIndex) {
                Log.e(
                        TAG,
                        "removeChild: remove child at index "
                                + index
                                + " Outside the scope of 0 ~ "
                                + lastIndex);
                return;
            }
            mHost.removeTabAt(index);
        }
    }

    @Override
    public void removeView(View child) {
        // not use
    }

    void addOnTabSelectedListener(TabLayout.OnTabSelectedListener listener) {
        if (mHost == null) {
            return;
        }
        mHost.addOnTabSelectedListener(listener);
    }

    void setDomIndex(int index) {
        mDomIndex = index;
        setSelectTab(mDomIndex);
    }

    void setSelectTab(int position) {
        if (mHost == null) {
            return;
        }

        TabLayout.Tab tab = mHost.getTabAt(position);
        if (tab != null) {
            tab.select();
        }
    }

    @Override
    public void onDomTreeChange(Component component, boolean add) {
        if (add) {
            component.addOnDomTreeChangeListener(this);
            StateHelper.onActiveStateChanged(component, true);
        }
    }

    private static class TabBarLayoutWrapper extends LinearLayout {

        private TouchEventCallback mTouchEventCallback;

        public TabBarLayoutWrapper(Context context) {
            super(context);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        public TabBarLayoutWrapper(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TabBarLayoutWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public void initTalkBack(View childView, int index, String description) {
            if (null != childView && index >= 0) {
                if (TextUtils.isEmpty(description)) {
                    setContentDescription(childView.getResources().getString(R.string.talkback_notitle_defaultstr));
                } else {
                    setContentDescription(description);
                }
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            super.dispatchTouchEvent(ev);
            if (mTouchEventCallback != null) {
                // 最后处理wrapper，优先child处理事件
                mTouchEventCallback.touchEvent(ev);
            }
            // 只要点击了wrapper就处理事件
            return true;
        }

        public void setTouchEventCallback(TouchEventCallback touchEventCallback) {
            mTouchEventCallback = touchEventCallback;
        }

        interface TouchEventCallback {
            void touchEvent(MotionEvent e);
        }
    }
}
