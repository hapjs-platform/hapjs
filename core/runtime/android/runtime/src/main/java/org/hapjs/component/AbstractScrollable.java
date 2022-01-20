/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hapjs.component.appearance.AppearanceHelper;
import org.hapjs.component.appearance.AppearanceManager;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;

public abstract class AbstractScrollable<T extends View> extends Container<T>
        implements Scrollable {

    private static final String TAG = "AbstractScrollable";
    protected AppearanceManager mAppearanceManager;
    private List<Scrollable> mSubScrollableList;
    private boolean mFirstIn = true;
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    private View.OnAttachStateChangeListener mOnAttachStateChangeListener;

    public AbstractScrollable(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    public void bindAppearEvent(Component component) {
        setAppearanceWatch(component, AppearanceHelper.APPEAR, true);
    }

    @Override
    public void bindDisappearEvent(Component component) {
        setAppearanceWatch(component, AppearanceHelper.DISAPPEAR, true);
    }

    @Override
    public void unbindAppearEvent(Component component) {
        setAppearanceWatch(component, AppearanceHelper.APPEAR, false);
    }

    @Override
    public void unbindDisappearEvent(Component component) {
        setAppearanceWatch(component, AppearanceHelper.DISAPPEAR, false);
    }

    @Override
    public void processAppearanceEvent() {
        if (mAppearanceManager != null && mFirstIn) {
            // ensure appear event can be triggered when first enter
            checkOnPreDraw();
            mFirstIn = false;
        } else if (mAppearanceManager != null) {
            mAppearanceManager.checkAppearanceEvent();
        }
        if (mSubScrollableList != null && !mSubScrollableList.isEmpty()) {
            for (Scrollable scrollable : mSubScrollableList) {
                scrollable.processAppearanceEvent();
            }
        }
    }

    @Override
    public void processAppearanceOneEvent(Component component) {
        if (mAppearanceManager != null) {
            mAppearanceManager.checkAppearanceOneEvent(component);
        }
    }

    private void checkOnPreDraw() {
        if (mHost == null) {
            return;
        }
        if (mOnPreDrawListener == null) {
            mOnPreDrawListener =
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            mAppearanceManager.checkAppearanceEvent();
                            if (mHost != null) {
                                mHost.getViewTreeObserver().removeOnPreDrawListener(this);
                                mOnPreDrawListener = null;
                                Log.i(TAG, "remove pre draw listener when onPreDraw() is called.");
                            }
                            Log.i(TAG, "check appearance in pre draw listener");
                            return true;
                        }
                    };
            mHost.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
        }
        if (mOnAttachStateChangeListener == null) {
            mOnAttachStateChangeListener =
                    new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            // do nothing
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            if (mHost != null && mOnPreDrawListener != null) {
                                mHost.getViewTreeObserver()
                                        .removeOnPreDrawListener(mOnPreDrawListener);
                                mHost.removeOnAttachStateChangeListener(this);
                                mOnAttachStateChangeListener = null;
                                Log.i(TAG, "remove pre draw listener when detached from window.");
                            }
                        }
                    };
            mHost.addOnAttachStateChangeListener(mOnAttachStateChangeListener);
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        Scrollable scrollable = getParentScroller();
        if (scrollable != null) {
            scrollable.removeSubScrollable(this);
        }
    }

    @Override
    public void removeSubScrollable(Scrollable subScrollable) {
        if (mSubScrollableList != null) {
            mSubScrollableList.remove(subScrollable);
        }
    }

    @Override
    public void addSubScrollable(Scrollable subScrollable) {
        if (mSubScrollableList == null) {
            mSubScrollableList = new ArrayList<>();
        }
        if (!mSubScrollableList.contains(subScrollable)) {
            mSubScrollableList.add(subScrollable);
        }
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        super.onHostViewAttached(parent);
        Scrollable scrollable = getParentScroller();
        if (scrollable != null) {
            scrollable.addSubScrollable(this);
        }
    }

    public void setAppearanceWatch(Component component, int event, boolean isWatch) {
        component.setWatchAppearance(event, isWatch);
        if (isWatch) {
            ensureAppearanceManager();
            mAppearanceManager.bindAppearanceEvent(component);
            if (mOnPreDrawListener == null) {
                checkOnPreDraw();
            }
        } else if (mAppearanceManager != null) {
            mAppearanceManager.unbindAppearanceEvent(component);
        }
        processAppearanceEvent();
    }

    public void lazySetAppearanceWatch(Component component) {
        lazySetAppearanceWatch(component, AppearanceHelper.APPEAR);
        lazySetAppearanceWatch(component, AppearanceHelper.DISAPPEAR);
        processAppearanceEvent();
    }

    private void lazySetAppearanceWatch(Component component, int event) {
        if (component.isWatchAppearance(event)) {
            ensureAppearanceManager();
            mAppearanceManager.bindAppearanceEvent(component);
        } else if (mAppearanceManager != null) {
            mAppearanceManager.unbindAppearanceEvent(component);
        }

        if (component instanceof Container<?>) {
            java.util.List<Component> children = ((Container<?>) component).getChildren();
            for (Component child : children) {
                lazySetAppearanceWatch(child, event);
            }
        }
    }

    public void ensureAppearanceManager() {
        if (mAppearanceManager == null) {
            mAppearanceManager = new AppearanceManager();
        }
    }
}
