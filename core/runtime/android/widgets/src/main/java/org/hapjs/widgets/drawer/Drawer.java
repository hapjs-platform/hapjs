/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.drawer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import androidx.drawerlayout.widget.DrawerLayout;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = Drawer.WIDGET_NAME,
        methods = {
                Drawer.METHOD_OPEN_DRAWER,
                Drawer.METHOD_CLOSE_DRAWER,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Drawer extends Container<FlexDrawerLayout> {
    protected static final String WIDGET_NAME = "drawer";
    protected static final String METHOD_OPEN_DRAWER = "openDrawer";
    protected static final String METHOD_CLOSE_DRAWER = "closeDrawer";
    private static final String TAG = "Drawer";
    private static final String METHOD_PARAM_DIRECTION = "direction";
    private static final int SHADOW_COLOR = 0x99000000;
    private final Set<Integer> mGravitySet = new HashSet<>();
    private DrawerSlideListener mDrawerSlideListener;
    private DrawerStateChangedListener mDrawerStateChangedListener;
    private DrawerLayout.SimpleDrawerListener mSimpleDrawerListener;
    private boolean mIsLeftOpen = false;
    private boolean mIsRightOpen = false;
    private int mDrawerState;
    private boolean mEnableSwipe = true;

    private static final int STATE_IDLE = 0;
    private static final int STATE_DRAGGING = 1;
    private static final int STATE_SETTLING = 2;

    private static final int SLIDE_END = 1;
    private static final int SLIDE_START = 0;

    private static final int STATE_OPEN = 1;
    private static final int STATE_CLOSE = 0;
    private View mDrawerView;
    private float mSlideOffset = 0f;

    public Drawer(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected FlexDrawerLayout createViewImpl() {
        FlexDrawerLayout drawerLayout = new FlexDrawerLayout(mContext);
        drawerLayout.setComponent(this);
        ViewGroup.LayoutParams lp = drawerLayout.getLayoutParams();
        if (lp == null) {
            lp =
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
        }
        drawerLayout.setScrimColor(SHADOW_COLOR);
        drawerLayout.setLayoutParams(lp);
        mSimpleDrawerListener = new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                mDrawerView = drawerView;
                mSlideOffset = slideOffset;
                if (mDrawerSlideListener != null) {
                    mDrawerSlideListener.onDrawerSlide(drawerView, slideOffset, mDrawerState);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (drawerView.getLayoutParams() != null) {
                    int gravity = ((DrawerLayout.LayoutParams) drawerView
                            .getLayoutParams()).gravity;
                    if (gravity == Gravity.START) {
                        mIsLeftOpen = true;
                    } else {
                        mIsRightOpen = true;
                    }
                    if (mDrawerStateChangedListener != null) {
                        mDrawerStateChangedListener.onDrawerStateChanged(
                                mIsLeftOpen ? mIsLeftOpen : mIsRightOpen, gravity);
                    }

                    // 打开状态再次调用onDrawerSlide，让state置为IDLE
                    if (mDrawerSlideListener != null) {
                        mDrawerSlideListener
                                .onDrawerSlide(drawerView, SLIDE_END, STATE_IDLE);
                    }
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (drawerView.getLayoutParams() != null) {
                    int gravity = ((DrawerLayout.LayoutParams) drawerView
                            .getLayoutParams()).gravity;
                    if (gravity == Gravity.START) {
                        mIsLeftOpen = false;
                    } else {
                        mIsRightOpen = false;
                    }
                    if (mDrawerStateChangedListener != null) {
                        mDrawerStateChangedListener.onDrawerStateChanged(
                                !mIsLeftOpen ? mIsLeftOpen : mIsRightOpen, gravity);
                    }

                    // 关闭状态再次调用onDrawerSlide，使得state置为IDLE
                    if (mDrawerSlideListener != null) {
                        mDrawerSlideListener
                                .onDrawerSlide(drawerView, SLIDE_START, STATE_IDLE);
                    }
                }
            }

            // state为IDLE时，onDrawerStateChanged会在onDrawerSlide调用完之后才调用
            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                if (newState == DrawerLayout.STATE_IDLE) {
                    mDrawerState = STATE_IDLE;
                } else if (newState == DrawerLayout.STATE_DRAGGING) {
                    mDrawerState = STATE_DRAGGING;
                } else if (newState == DrawerLayout.STATE_SETTLING) {
                    mDrawerState = STATE_SETTLING;
                }
                if (mDrawerSlideListener != null && mDrawerView != null) {
                    mDrawerSlideListener.onDrawerSlide(mDrawerView, mSlideOffset, mDrawerState);
                }
            }
        };
        drawerLayout.addDrawerListener(mSimpleDrawerListener);
        return drawerLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.ENABLE_SWIPE:
                boolean enableSwipe = Attributes.getBoolean(attribute, true);
                setEnableSwipe(enableSwipe);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public boolean isEnableSwipe() {
        return mEnableSwipe;
    }

    private void setEnableSwipe(boolean enableSwipe) {
        mEnableSwipe = enableSwipe;
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (Attributes.Event.CHANGE.equals(event)) {
            if (mDrawerStateChangedListener == null) {
                mDrawerStateChangedListener =
                        new DrawerStateChangedListener() {
                            @Override
                            public void onDrawerStateChanged(boolean isDrawerOpen, int gravity) {
                                Map<String, Object> params = new HashMap<>();
                                if (gravity == Gravity.START) {
                                    params.put("direction", "start");
                                } else if (gravity == Gravity.END) {
                                    params.put("direction", "end");
                                }
                                if (isDrawerOpen) {
                                    params.put("state", STATE_OPEN);
                                } else {
                                    params.put("state", STATE_CLOSE);
                                }
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, Attributes.Event.CHANGE, Drawer.this,
                                        params, null);
                            }
                        };
            }
            return true;
        } else if (Attributes.Event.SCROLL.equals(event)) {
            if (mDrawerSlideListener == null) {
                mDrawerSlideListener =
                        new DrawerSlideListener() {
                            @Override
                            public void onDrawerSlide(View drawerView, float slideOffset,
                                                      int state) {
                                Map<String, Object> params = new HashMap<>();
                                params.put("slideOffset", slideOffset);
                                params.put("state", state);
                                int gravity = ((DrawerLayout.LayoutParams) drawerView
                                        .getLayoutParams()).gravity;
                                if (gravity == Gravity.START) {
                                    params.put("direction", "start");
                                } else if (gravity == Gravity.END) {
                                    params.put("direction", "end");
                                }
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, Attributes.Event.SCROLL, Drawer.this,
                                        params, null);
                            }
                        };
            }
            return true;
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (Attributes.Event.SCROLL.equals(event)) {
            mDrawerSlideListener = null;
            return true;
        } else if (Attributes.Event.CHANGE.equals(event)) {
            mDrawerStateChangedListener = null;
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);
        if (METHOD_OPEN_DRAWER.equals(methodName) || METHOD_CLOSE_DRAWER.equals(methodName)) {
            int gravity = Gravity.START;
            // Drawer中只存在一个子组件 draw-navigation, 则获取此子组件的gravity
            if (mGravitySet != null && mGravitySet.size() == 1) {
                gravity = mGravitySet.iterator().next();
            } else {
                if (args != null) {
                    Object gravityObj = args.get(METHOD_PARAM_DIRECTION);
                    if (gravityObj instanceof String) {
                        if ("start".equals(gravityObj)) {
                            gravity = Gravity.START;
                        } else if ("end".equals(gravityObj)) {
                            gravity = Gravity.END;
                        }
                    }
                }
            }

            if (METHOD_OPEN_DRAWER.equals(methodName)) {
                openDrawer(gravity);
            } else {
                closeDrawer(gravity);
            }
        }
    }

    @Override
    public void addChild(Component child, int index) {
        if (child instanceof DrawerNavigation) {
            View childView = child.getHostView();
            if (childView.getLayoutParams() != null) {
                int childViewGravity =
                        ((DrawerLayout.LayoutParams) childView.getLayoutParams()).gravity;
                if (mGravitySet == null || mGravitySet.contains(childViewGravity)) {
                    return;
                }
                mGravitySet.add(childViewGravity);
            }
        }
        super.addChild(child, index);
    }

    @Override
    public void addView(View childView, int index) {
        if (mHost == null || childView == null) {
            return;
        }
        if ((childView instanceof ComponentHost)) {
            Component component = ((ComponentHost) childView).getComponent();
            if (component != null) {
                if (component instanceof DrawerNavigation) {
                    DrawerLayout.LayoutParams params =
                            (DrawerLayout.LayoutParams) childView.getLayoutParams();
                    if (params != null) {
                        mHost.addView(childView, index, params);
                        component.onHostViewAttached(mHost);
                    }
                } else {
                    ViewGroup.LayoutParams params = childView.getLayoutParams();
                    if (params == null) {
                        params = generateDefaultLayoutParams();
                    }
                    mHost.addView(childView, index, params);
                    component.onHostViewAttached(mHost);
                }
            }
        }
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        super.onHostViewAttached(parent);
        if (isParentYogaLayout()) {
            YogaNode yogaNode = ((YogaLayout) mHost.getParent()).getYogaNodeForView(mHost);
            if (yogaNode == null || yogaNode.getParent() == null) {
                Log.w(
                        TAG,
                        "onHostViewAttached: "
                                + (yogaNode == null ? "yogaNode == null" :
                                "yogaNode.getParent() == null"));
                return;
            }
            YogaFlexDirection parentDirection = yogaNode.getParent().getFlexDirection();
            if (!getStyleDomData().containsKey(Attributes.Style.FLEX_GROW)
                    && !getStyleDomData().containsKey(Attributes.Style.FLEX)
                    && ((parentDirection == YogaFlexDirection.ROW && !isWidthDefined())
                    || (parentDirection == YogaFlexDirection.COLUMN && !isHeightDefined()))) {
                yogaNode.setFlexGrow(1f);
            }
            if ((parentDirection == YogaFlexDirection.ROW && !isHeightDefined())
                    || (parentDirection == YogaFlexDirection.COLUMN && !isWidthDefined())) {
                yogaNode.setAlignSelf(YogaAlign.STRETCH);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            mHost.removeDrawerListener(mSimpleDrawerListener);
            mSimpleDrawerListener = null;
        }
    }

    public boolean isLeftOpen() {
        return mIsLeftOpen;
    }

    public boolean isRightOpen() {
        return mIsRightOpen;
    }

    public void openDrawer(int gravity) {
        if ((isLeftOpen() && gravity == Gravity.END)
                || (isRightOpen() && gravity == Gravity.START)) {
            mCallback.onJsException(
                    new IllegalAccessException(
                            "can't open two drawer-navigation at the same time"));
            return;
        }

        if (mHost != null && mGravitySet.contains(gravity)) {
            mHost.openDrawer(gravity);
        } else {
            mCallback.onJsException(
                    new IllegalAccessException(
                            "the gravity value of openDrawer function must equal DrawerNavigation's gravity"));
        }
    }

    public void closeDrawer(int gravity) {
        if (mHost != null && mGravitySet.contains(gravity)) {
            mHost.closeDrawer(gravity);
        } else {
            mCallback.onJsException(
                    new IllegalAccessException(
                            "the gravity value of closeDrawer function must equal DrawerNavigation's gravity"));
        }
    }

    private interface DrawerSlideListener {
        void onDrawerSlide(View drawerView, float slideOffset, int state);
    }

    private interface DrawerStateChangedListener {
        void onDrawerStateChanged(boolean isDrawerOpen, int gravity);
    }
}
