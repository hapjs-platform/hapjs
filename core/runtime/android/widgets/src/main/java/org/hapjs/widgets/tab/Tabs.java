/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.tab;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.viewpager.widget.ViewPager;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.PercentLinearLayout;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = Tabs.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Tabs extends Container<PercentLinearLayout> {

    protected static final String WIDGET_NAME = "tabs";
    private static final String TAG = "Tabs";

    private TabBar mTabBar;
    private TabContent mTabContent;

    private OnTabChangeListener mOnTabChangeListener;
    private List<OnTabChangeListener> mOnTabChangeListeners;

    private int mCurrentIndex = -1;
    private int mDomIndex;

    public Tabs(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    @Override
    protected PercentLinearLayout createViewImpl() {
        PercentLinearLayout linearLayout = new PercentLinearLayout(mContext);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setComponent(this);
        return linearLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.INDEX:
                int index = Attributes.getInt(mHapEngine, attribute, 0);
                setDomIndex(index);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    @Override
    protected Object getAttribute(String key) {
        switch (key) {
            case Attributes.Style.INDEX:
                return mDomIndex;
            default:
                break;
        }

        return super.getAttribute(key);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            if (mOnTabChangeListener == null) {
                mOnTabChangeListener =
                        new OnTabChangeListener() {
                            @Override
                            public void onTabActive(int index) {
                                Map<String, Object> params = new HashMap();
                                params.put("index", index);
                                Map<String, Object> attrs = new HashMap();
                                attrs.put("index", index);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, Attributes.Event.CHANGE, Tabs.this,
                                        params, attrs);
                            }
                        };
            }
            addOnTabChangeListener(mOnTabChangeListener);
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            removeOnTabActiveListener(mOnTabChangeListener);
            return true;
        }

        return super.removeEvent(event);
    }

    private void setDomIndex(int index) {
        mDomIndex = index;

        if (mTabBar != null) {
            mTabBar.setDomIndex(mDomIndex);
        }
        if (mTabContent != null) {
            mTabContent.setDomIndex(mDomIndex);
        }
    }

    void addOnTabChangeListener(OnTabChangeListener onTabChangeListener) {
        if (mOnTabChangeListeners == null) {
            mOnTabChangeListeners = new ArrayList<>();
        }

        mOnTabChangeListeners.add(onTabChangeListener);
    }

    void removeOnTabActiveListener(OnTabChangeListener onTabActiveListener) {
        if (mOnTabChangeListeners == null) {
            return;
        }

        mOnTabChangeListeners.remove(onTabActiveListener);
    }

    @Override
    public void addChild(Component child, int index) {
        super.addChild(child, index);

        if (child instanceof TabBar) {
            mTabBar = (TabBar) child;
            mTabBar.setDomIndex(mDomIndex);

            mTabBar.addOnTabSelectedListener(
                    new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(TabLayout.Tab tab) {
                            updateCurrentIndex(tab.getPosition());
                            if (mTabContent == null) {
                                return;
                            }
                            mTabContent.setCurrentItem(tab.getPosition());
                        }

                        @Override
                        public void onTabUnselected(TabLayout.Tab tab) {
                        }

                        @Override
                        public void onTabReselected(TabLayout.Tab tab) {
                        }
                    });
        }

        if (child instanceof TabContent) {
            mTabContent = (TabContent) child;
            mTabContent.setDomIndex(mDomIndex);

            mTabContent.addOnPageChangeListener(
                    new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(
                                int position, float positionOffset, int positionOffsetPixels) {
                        }

                        @Override
                        public void onPageSelected(int position) {
                            updateCurrentIndex(position);
                            if (mTabBar == null) {
                                return;
                            }
                            mTabBar.setSelectTab(position);
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {
                        }
                    });
        }
    }

    private void updateCurrentIndex(int index) {
        if (mCurrentIndex == index) {
            return;
        }

        mCurrentIndex = index;

        if (mOnTabChangeListeners == null) {
            return;
        }

        for (OnTabChangeListener listener : mOnTabChangeListeners) {
            listener.onTabActive(index);
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

    interface OnTabChangeListener {
        void onTabActive(int index);
    }
}
