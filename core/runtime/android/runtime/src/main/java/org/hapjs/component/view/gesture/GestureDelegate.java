/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.gesture;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Floating;
import org.hapjs.component.FloatingHelper;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ScrollView;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RouterManageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GestureDelegate implements IGesture, GestureDetector.OnGestureListener {

    private final String TAG ="GestureDelegate";
    private final GestureDetector mGestureDetector;
    private Component mComponent;
    private Context mContext;
    private HapEngine mHapEngine;
    private Rect mClientDisplayRect = new Rect();
    private Point mClientDisplayOffset = new Point();
    private PointF mClientLocation = new PointF();
    private Set<String> mGestureTypes;
    private GestureDispatcher mGestureDispatcher;
    private Set<String> mFrozenEvents;
    private int[] mLocationTmp = new int[2];
    private int mMinAppPlatformVersion;
    private Component mHandedTargetComponent;
    private boolean mLongPressed = false;
    private int mTouchupPositionX = 0;
    private int mTouchupPositionY = 0;

    private boolean mIsWatchingLongPress;
    private boolean mIsWatchingClick;
    private RouterManageProvider mRouterManageProvider;

    public GestureDelegate(HapEngine hapEngine, Component component, Context context) {
        mHapEngine = hapEngine;
        mComponent = component;
        mContext = context;
        mGestureDetector = new GestureDetector(context, this);
        mGestureTypes = new HashSet<>();
        mFrozenEvents = new HashSet<>();

        mGestureDispatcher = GestureDispatcher.getDispatcher(mComponent.getCallback());
        if (mGestureDispatcher == null) {
            mGestureDispatcher =
                    GestureDispatcher.createInstanceIfNecessary(mComponent.getCallback());
        }

        final View hostView = mComponent.getHostView();
        if (hostView != null) {
            hostView.setAccessibilityDelegate(
                    new View.AccessibilityDelegate() {
                        public boolean performAccessibilityAction(View host, int action,
                                                                  Bundle args) {
                            boolean res = super.performAccessibilityAction(host, action, args);
                            switch (action) {
                                case AccessibilityNodeInfo.ACTION_CLICK:
                                    hostView.getLocationOnScreen(mLocationTmp);
                                    MotionEvent clickEvent =
                                            MotionEvent.obtain(
                                                    SystemClock.uptimeMillis(),
                                                    SystemClock.uptimeMillis(),
                                                    MotionEvent.ACTION_UP,
                                                    mLocationTmp[0]
                                                            + hostView.getMeasuredWidth() / 2,
                                                    mLocationTmp[1]
                                                            + hostView.getMeasuredHeight() / 2,
                                                    0);
                                    handleEvent(Attributes.Event.TOUCH_CLICK, clickEvent, true);
                                    clickEvent.recycle();
                                    break;

                                case AccessibilityNodeInfo.ACTION_LONG_CLICK:
                                    hostView.getLocationOnScreen(mLocationTmp);
                                    MotionEvent clickLongEvent =
                                            MotionEvent.obtain(
                                                    SystemClock.uptimeMillis(),
                                                    SystemClock.uptimeMillis(),
                                                    MotionEvent.ACTION_UP,
                                                    mLocationTmp[0]
                                                            + hostView.getMeasuredWidth() / 2,
                                                    mLocationTmp[1]
                                                            + hostView.getMeasuredHeight() / 2,
                                                    0);
                                    handleEvent(Attributes.Event.TOUCH_LONG_PRESS, clickLongEvent,
                                            true);
                                    clickLongEvent.recycle();
                                    break;
                                default:
                                    break;
                            }
                            return res;
                        }
                    });
        }
        mMinAppPlatformVersion = mHapEngine.getMinPlatformVersion();
        mGestureDispatcher.setMinPlatformVersion(mMinAppPlatformVersion);
        mRouterManageProvider = ProviderManager.getDefault().getProvider(RouterManageProvider.NAME);
    }

    /**
     * 监听手势事件
     *
     * @param eventType
     */
    @Override
    public void registerEvent(String eventType) {
        if (TextUtils.isEmpty(eventType)) {
            return;
        }

        if (!BuildPlatform.isTV()) {
            if (Attributes.Event.CLICK.equals(eventType)) {
                View hostView = mComponent.getHostView();
                if (hostView != null) {
                    hostView.setClickable(true);
                }
            }
        }

        mGestureTypes.add(eventType);
    }

    /**
     * 移除手势事件
     *
     * @param eventType
     */
    @Override
    public void removeEvent(String eventType) {
        if (mGestureTypes.contains(eventType)) {
            mGestureTypes.remove(eventType);
        }
    }

    @Override
    public void addFrozenEvent(String eventType) {
        if (TextUtils.isEmpty(eventType)) {
            return;
        }

        // option等一些组件frozen了click事件，需要将hostview的clickbale设置为false，否则parent无法处理事件
        if (TextUtils.equals(eventType, Attributes.Event.CLICK)) {
            View hostView = mComponent.getHostView();
            if (hostView != null) {
                hostView.setClickable(false);
            }
        }
        mFrozenEvents.add(eventType);
    }

    @Override
    public void removeFrozenEvent(String eventType) {
        if (mFrozenEvents.contains(eventType)) {
            mFrozenEvents.remove(eventType);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Set<String> temp = new HashSet<>(mGestureTypes);
        temp.removeAll(mFrozenEvents);
        if (temp.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    /**
     * 单击事件
     *
     * @param e
     * @return
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {

        if (mComponent == null || mComponent.getHostView() == null) {
            return false;
        }

        if (mGestureTypes.contains(Attributes.Event.TOUCH_CLICK)
                && !mFrozenEvents.contains(Attributes.Event.TOUCH_CLICK)) {
            if (mComponent.isRegisterClickEventComponent()) {
                // performClick中，调用onclick前先调用playSoundEffect()
                mComponent.getHostView().playSoundEffect(SoundEffectConstants.CLICK);
            }
            handleEvent(Attributes.Event.TOUCH_CLICK, e);
            DocComponent docComponent = mComponent.getRootComponent();
            if (docComponent != null) {
                FloatingHelper helper = docComponent.getFloatingHelper();
                Floating floating = helper.getFloating(mComponent);
                if (floating != null) {
                    floating.dismiss();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    /**
     * 长按事件
     *
     * @param e
     */
    @Override
    public void onLongPress(MotionEvent e) {
        if (mGestureTypes.contains(Attributes.Event.TOUCH_LONG_PRESS)) {
            handleEvent(Attributes.Event.TOUCH_LONG_PRESS, e, true);
            mLongPressed = true;
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void updateComponent(@NonNull Component component) {
        mComponent = component;
        mGestureDispatcher = GestureDispatcher.getDispatcher(component.getCallback());
        mGestureDispatcher = GestureDispatcher.createInstanceIfNecessary(component.getCallback());
    }

    /**
     * 手势处理
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(MotionEvent event) {

        if (mComponent == null || mComponent.getHostView() == null) {
            return false;
        }

        if (mMinAppPlatformVersion >= GestureDispatcher.MIN_BUBBLE_PLATFORM_VERSION) {
            // 1040+，所有组件都会设置TouchEvent，child处理了事件，parent无法收到
            Component c = mComponent;
            c.triggerActiveState(event);
            // 过滤掉非target节点的事件处理
            int target = mGestureDispatcher.getTarget();
            if (target != -1 && target != mComponent.getRef()) {
                return false;
            }
            boolean touchDown = false;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                touchDown = true;
            }
            MotionEvent transformEvent = MotionEvent.obtain(event);
            while (c != null) {
                View hostView = c.getHostView();
                if (hostView == null) {
                    return false;
                }
                boolean ret = c.onTouch(hostView, transformEvent);
                if (ret && touchDown) {
                    mHandedTargetComponent = c;
                    break;
                }
                if (mHandedTargetComponent == c) {
                    break;
                }
                c = c.getParent();
                if (c != null) {
                    View parent = c.getHostView();
                    if (parent != null) {
                        final float offsetX = hostView.getLeft() - parent.getScrollX();
                        final float offsetY = hostView.getTop() - parent.getScrollY();
                        transformEvent.offsetLocation(offsetX, offsetY);
                    }
                }
            }
            transformEvent.recycle();
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                mHandedTargetComponent = null;
            }
        }

        if (mComponent.isDisabled()) {
            // 允许parent处理event
            return false;
        }
        boolean result = mGestureDetector.onTouchEvent(event);

        String eventType = "";
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mLongPressed = false;
                eventType = Attributes.Event.TOUCH_START;
                break;
            case MotionEvent.ACTION_MOVE:
                eventType = Attributes.Event.TOUCH_MOVE;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mTouchupPositionX = (int) event.getX();
                mTouchupPositionY = (int) event.getY();
                eventType = Attributes.Event.TOUCH_END;
                break;

            case MotionEvent.ACTION_CANCEL:
                eventType = Attributes.Event.TOUCH_CANCEL;
                mLongPressed = false;
                break;
            default:
                break;
        }

        if (!TextUtils.isEmpty(eventType)) {

            result |= handleEvent(eventType, event);
        }

        return result;
    }

    public void onKeyEventClick(int keyCode, KeyEvent event) {
        if (mComponent.isDisabled()) {
            return;
        }

        if (mGestureTypes.contains(Attributes.Event.TOUCH_CLICK)
                && !mFrozenEvents.contains(Attributes.Event.TOUCH_CLICK)) {
            // performClick中，调用onclick前先调用playSoundEffect()
            mComponent.getHostView().playSoundEffect(SoundEffectConstants.CLICK);
            mComponent.onHostKey(keyCode, event.getAction(), event, Attributes.Event.TOUCH_CLICK);
            DocComponent docComponent = mComponent.getRootComponent();
            if (docComponent != null) {
                FloatingHelper helper = docComponent.getFloatingHelper();
                Floating floating = helper.getFloating(mComponent);
                if (floating != null) {
                    floating.dismiss();
                }
            }
        }
    }

    private void requestDisallowInterceptTouchEvent(boolean allow) {
        View hostView = mComponent.getHostView();
        if (hostView != null) {
            ViewGroup parent = (ViewGroup) hostView.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(allow);
            }
        }
    }

    /**
     * 处理事件，并下发到js。默认先缓存事件数据，当事件回到RootView的时候，再一次性将所有的事件数据下发到js
     *
     * @param eventType
     * @param event
     * @return
     */
    private boolean handleEvent(String eventType, MotionEvent event) {
        return handleEvent(eventType, event, false);
    }

    /**
     * 处理事件，并下发到js
     *
     * @param eventType
     * @param event
     * @param immediately true：立即下发到js。false：先缓存，等事件回到rootview时再下发到js
     * @return
     */
    private boolean handleEvent(String eventType, MotionEvent event, boolean immediately) {
        if (!mGestureTypes.contains(eventType) || mFrozenEvents.contains(eventType)) {
            return false;
        }

        Map<String, Object> object = new HashMap<>();
        // jsframework会组装type和timestamp数据，所以这里不用再下发了

        // touches
        if (!(Attributes.Event.TOUCH_END.equals(eventType))
                && !(Attributes.Event.TOUCH_CANCEL.equals(eventType))) {

            object.put("touches", buildTouches(event));
        }

        // changedTouches
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            object.put("changedTouches", buildTouches(event));
        } else if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            int pointerIndex = event.getActionIndex();
            object.put("changedTouches", buildTouches(event, pointerIndex));
        }
        // Floating上的事件需要立即下发，事件的源头不再是RootView了
        if (!immediately && isFloating(mComponent)) {
            immediately = true;
        }

        if (mLongPressed
                && Attributes.Event.TOUCH_END.equals(eventType)
                && mTouchupPositionX >= 0
                && mTouchupPositionX < mComponent.getHostView().getWidth()
                && mTouchupPositionY >= 0
                && mTouchupPositionY < mComponent.getHostView().getHeight()
                && mIsWatchingClick
                && !mIsWatchingLongPress) {
            mLongPressed = false;
            onSingleTapUp(event);
        }

        // click and longpress event
        if (TextUtils.equals(Attributes.Event.CLICK, eventType)
                || TextUtils.equals(Attributes.Event.LONGPRESS, eventType)
                || TextUtils.equals(Attributes.Event.TOUCH_CLICK, eventType)
                || TextUtils.equals(Attributes.Event.TOUCH_LONG_PRESS, eventType)) {
            List<Map<String, Object>> touchList = (List<Map<String, Object>>) object.get("touches");
            if (touchList != null && !touchList.isEmpty()) {
                Map<String, Object> mouseEvent = touchList.get(0);
                mouseEvent.remove("identifier");
                object.putAll(mouseEvent);
            }
        }
        boolean isConsume = false;
        if (null != mComponent) {
            isConsume = mComponent.preConsumeEvent(eventType, object, immediately);
        }
        if (!isConsume) {
            fireEvent(eventType, object, immediately);
        }
        return true;
    }

    private boolean isFloating(Component component) {
        if (component == null) {
            return false;
        }

        Component c = component.getParent();
        while (c != null) {
            if (c instanceof Floating) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    private List<Map<String, Object>> buildTouches(MotionEvent event) {
        List<Map<String, Object>> touches = new ArrayList<>();
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = event.getPointerId(i);
            PointF pageLocation = getEventPageLoc(event, i);
            PointF screenLocation = getEventClientLoc(event, i);
            PointF offsetLoc = getEventOffsetLoc(event, i);
            Map<String, Object> jsonObject =
                    buildPointerInfo(pointerId, pageLocation, screenLocation, offsetLoc);
            touches.add(jsonObject);
        }
        return touches;
    }

    private List<Map<String, Object>> buildTouches(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        PointF pageLocation = getEventPageLoc(event, pointerIndex);
        PointF screenLocation = getEventClientLoc(event, pointerIndex);
        PointF offsetLoc = getEventOffsetLoc(event, pointerIndex);
        List<Map<String, Object>> touches = new ArrayList<>();
        Map<String, Object> obj =
                buildPointerInfo(pointerId, pageLocation, screenLocation, offsetLoc);
        touches.add(obj);
        return touches;
    }

    /**
     * 获取touch事件点到顶层Document{@link DecorLayout}距离
     *
     * @param event
     * @param pointerIndex
     * @return
     */
    private PointF getEventPageLoc(MotionEvent event, int pointerIndex) {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        mClientLocation.set(x, y);

        mClientDisplayRect.set(0, 0, 0, 0);
        mClientDisplayOffset.set(0, 0);
        View host = mComponent.getHostView();
        if (host != null) {
            host.getGlobalVisibleRect(mClientDisplayRect, mClientDisplayOffset);
            mClientLocation.offset(mClientDisplayOffset.x, mClientDisplayOffset.y);
            mClientLocation.offset(host.getScrollX(), host.getScrollY());
        }
        float pageX = mClientLocation.x;
        float pageY = mClientLocation.y;

        DocComponent rootComponent = mComponent.getRootComponent();
        if (rootComponent != null) {
            DecorLayout decorLayout = (DecorLayout) rootComponent.getInnerView();
            Rect contentInsets = decorLayout.getContentInsets();
            // 目前page和client计算出来的都是一样的值
            pageX = mClientLocation.x - contentInsets.left;
            pageY = mClientLocation.y - contentInsets.top;

            // 非fixed状态下，需要加上scrollview的scroll
            if (!mComponent.isFixed()) {
                RootView rootView = (RootView) rootComponent.getHostView();
                VDocument document = rootView.getDocument();
                if (document != null) {
                    VElement scroller = document.getElementById(VElement.ID_BODY);
                    if (scroller != null) {
                        ScrollView scrollView = (ScrollView) scroller.getComponent().getHostView();
                        pageX += scrollView.getScrollX();
                        pageY += scrollView.getScrollY();
                    }
                }
            }
        }

        return new PointF(
                DisplayUtil.getDesignPxByWidth(pageX, mHapEngine.getDesignWidth()),
                DisplayUtil.getDesignPxByWidth(pageY, mHapEngine.getDesignWidth()));
    }

    /**
     * 获取touch事件点到当前component边界的距离
     *
     * @param event
     * @param pointerIndex
     * @return
     */
    private PointF getEventOffsetLoc(MotionEvent event, int pointerIndex) {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        return new PointF(
                DisplayUtil.getDesignPxByWidth(x, mHapEngine.getDesignWidth()),
                DisplayUtil.getDesignPxByWidth(y, mHapEngine.getDesignWidth()));
    }

    /**
     * 获取touch点到屏幕的距离(除去titlebar和statusbar)
     *
     * @param event
     * @param pointerIndex
     * @return
     */
    private PointF getEventClientLoc(MotionEvent event, int pointerIndex) {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        mClientLocation.set(x, y);

        mClientDisplayRect.set(0, 0, 0, 0);
        mClientDisplayOffset.set(0, 0);
        View host = mComponent.getHostView();
        if (host != null) {
            host.getGlobalVisibleRect(mClientDisplayRect, mClientDisplayOffset);
            mClientLocation.offset(mClientDisplayOffset.x, mClientDisplayOffset.y);
            mClientLocation.offset(host.getScrollX(), host.getScrollY());
        }
        DocComponent rootComponent = mComponent.getRootComponent();
        if (rootComponent != null) {
            DecorLayout decorLayout = (DecorLayout) rootComponent.getInnerView();
            Rect contentInsets = decorLayout.getContentInsets();
            // 去除titlebar和statusbar的高度
            mClientLocation.offset(0, -contentInsets.top);
        }

        return new PointF(
                DisplayUtil.getDesignPxByWidth(mClientLocation.x, mHapEngine.getDesignWidth()),
                DisplayUtil.getDesignPxByWidth(mClientLocation.y, mHapEngine.getDesignWidth()));
    }

    private Map<String, Object> buildPointerInfo(
            int pointerId, PointF pageXY, PointF screenXY, PointF offset) {
        Map<String, Object> object = new HashMap<>();
        object.put("identifier", pointerId);
        object.put("pageX", pageXY.x);
        object.put("pageY", pageXY.y);
        object.put("clientX", screenXY.x);
        object.put("clientY", screenXY.y);
        object.put("offsetX", offset.x);
        object.put("offsetY", offset.y);

        return object;
    }

    public void fireEvent(String eventName, Map<String, Object> data, boolean immediately) {
        if (mRouterManageProvider != null) {
            mRouterManageProvider.recordFireEvent(eventName);
        }
        // longpress通过handler的delaytime检查判断，不会向上传递，这里需要立即传递
        if (immediately) {
            mGestureDispatcher
                    .put(mComponent.getPageId(), mComponent.getRef(), eventName, data, null);
            mGestureDispatcher.flush();
        } else {
            mGestureDispatcher
                    .put(mComponent.getPageId(), mComponent.getRef(), eventName, data, null);
        }
    }

    public boolean fireClickEvent(MotionEvent motionEvent,boolean immediately) {
        boolean isTrigger = false;
        if(null != motionEvent){
            Map<String, Object> object = new HashMap<>();
            object.put("touches", buildTouches(motionEvent));
            List<Map<String, Object>> touchList = (List<Map<String, Object>>) object.get("touches");
            if (touchList != null && !touchList.isEmpty()) {
                Map<String, Object> mouseEvent = touchList.get(0);
                mouseEvent.remove("identifier");
                object.putAll(mouseEvent);
                fireEvent(Attributes.Event.CLICK,object,immediately);
                isTrigger = true;
            }else {
                Log.w(TAG,"fireClickEvent motionEvent touchList is null or empty.");
            }
        }else {
            Log.w(TAG,"fireClickEvent motionEvent is null.");
        }
        return isTrigger;
    }

    @Override
    public void setIsWatchingLongPress(boolean isWatchingLongPress) {
        mIsWatchingLongPress = isWatchingLongPress;
    }

    @Override
    public void setIsWatchingClick(boolean isWatchingClick) {
        mIsWatchingClick = isWatchingClick;
    }
}
