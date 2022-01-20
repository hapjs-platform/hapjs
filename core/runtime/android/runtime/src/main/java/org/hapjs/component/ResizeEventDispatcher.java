/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.util.Log;
import android.util.SparseArray;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.render.RootView;

public class ResizeEventDispatcher {

    private static final String TAG = "ResizeEventDispatcher";
    private static Map<RenderEventCallback, ResizeEventDispatcher> INSTANCES;

    private RenderEventCallback mRenderCallback;
    private RootView mRootView;
    private ViewTreeObserver.OnPreDrawListener mPreDrawListener;
    private SparseArray<List<RenderEventCallback.EventData>> mEventBuffer;
    private boolean mHasPreDrawListener = false;

    private ResizeEventDispatcher(Component component) {
        mRenderCallback = component.getCallback();
        mRootView = (RootView) component.getRootComponent().getHostView();
    }

    public static ResizeEventDispatcher getInstance(Component component) {
        if (INSTANCES == null) {
            INSTANCES = new HashMap<>();
        }
        RenderEventCallback callback = component.getCallback();
        if (INSTANCES.containsKey(callback)) {
            return INSTANCES.get(callback);
        }
        ResizeEventDispatcher dispatcher = new ResizeEventDispatcher(component);
        INSTANCES.put(callback, dispatcher);
        return dispatcher;
    }

    public static void destroyInstance(RenderEventCallback callback) {
        if (INSTANCES == null) {
            return;
        }
        if (INSTANCES.containsKey(callback)) {
            ResizeEventDispatcher dispatcher = INSTANCES.remove(callback);
            if (dispatcher != null) {
                dispatcher.destroy();
            }
        }
    }

    public void destroy() {
        mEventBuffer.clear();
        mEventBuffer = null;
        mRenderCallback = null;
        mRootView = null;
    }

    public void put(RenderEventCallback.EventData eventData) {
        if (mEventBuffer == null) {
            // 为了扩展，考虑不同page的事件put到同一个buffer中，并且大多数情况下是在同一个page中，
            // 因此采用一个初始容量为1的SparseArray的数据结构
            mEventBuffer = new SparseArray<>(1);
        }
        if (mEventBuffer.get(eventData.pageId) == null) {
            mEventBuffer.put(eventData.pageId, new ArrayList<RenderEventCallback.EventData>());
        }
        mEventBuffer.get(eventData.pageId).add(eventData);

        if (mPreDrawListener == null) {
            mPreDrawListener =
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            flush();
                            mRootView.getViewTreeObserver()
                                    .removeOnPreDrawListener(mPreDrawListener);
                            mHasPreDrawListener = false;
                            return true;
                        }
                    };
        }
        if (!mHasPreDrawListener) {
            mRootView.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
            mHasPreDrawListener = true;
        }
    }

    private void flush() {
        if (mEventBuffer == null || mEventBuffer.size() == 0) {
            return;
        }
        Log.d(
                TAG,
                "flush event: page size: "
                        + mEventBuffer.size()
                        + " first page event: "
                        + mEventBuffer.valueAt(0).size());

        for (int i = 0; i < mEventBuffer.size(); i++) {
            int pageId = mEventBuffer.keyAt(i);
            List<RenderEventCallback.EventData> events = mEventBuffer.valueAt(i);
            if (events != null && !events.isEmpty()) {
                mRenderCallback.onJsMultiEventCallback(pageId, events);
                events.clear();
            }
        }
    }
}
