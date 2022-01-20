/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.gesture;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.UiThread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapChoreographer;

public class GestureDispatcher {
    public static final int MIN_BUBBLE_PLATFORM_VERSION = 1040;
    private static final String TAG = "GestureDispatcher";
    private static final Map<RenderEventCallback, GestureDispatcher> INSTANCES = new HashMap<>();
    private RenderEventCallback mRenderEventCallback;
    private HapChoreographer mChoreographer;

    private QueuedGestureEvent mPendingGestureEventHead;
    private QueuedGestureEvent mPendingGestureEventTail;
    private int mTarget = -1;
    private int mMinPlatformVersion;

    private GestureDispatcher(RenderEventCallback callback) {
        mRenderEventCallback = callback;
        mChoreographer = HapChoreographer.createInstanceIfNecessary(callback);
    }

    /**
     * 后续卡片方案中，一个进程会存在多个引擎，单实例就不可用。 一个RootView可以代表唯一一个引擎，而renderEventCallback在RootView中也是唯一的。
     * 基于此，这里使用renderEventCallback作为GestureDispather的key。
     *
     * @param renderEventCallback
     * @return
     */
    @UiThread
    public static GestureDispatcher createInstanceIfNecessary(
            RenderEventCallback renderEventCallback) {
        if (INSTANCES.containsKey(renderEventCallback)) {
            return INSTANCES.get(renderEventCallback);
        }

        GestureDispatcher dispatcher = new GestureDispatcher(renderEventCallback);
        INSTANCES.put(renderEventCallback, dispatcher);
        return dispatcher;
    }

    public static GestureDispatcher getDispatcher(RenderEventCallback renderEventCallback) {
        if (INSTANCES.containsKey(renderEventCallback)) {
            return INSTANCES.get(renderEventCallback);
        }
        return null;
    }

    public static void remove(RenderEventCallback renderEventCallback) {
        if (INSTANCES.containsKey(renderEventCallback)) {
            GestureDispatcher dispatcher = INSTANCES.remove(renderEventCallback);
            if (dispatcher != null) {
                dispatcher.destroy();
            }
        }
    }

    public HapChoreographer getChoreographer() {
        return mChoreographer;
    }

    public void setMinPlatformVersion(int minPlatformVersion) {
        mMinPlatformVersion = minPlatformVersion;
    }

    @UiThread
    public void put(
            int pageId,
            int ref,
            String eventName,
            Map<String, Object> params,
            Map<String, Object> attributes) {
        if (mRenderEventCallback == null) {
            Log.e(TAG, "put() rendercallback is null");
            return;
        }
        if (TextUtils.isEmpty(eventName)) {
            Log.e(TAG, "put() invalidate event");
            return;
        }

        if (mMinPlatformVersion >= MIN_BUBBLE_PLATFORM_VERSION) {
            if (mTarget != -1 && mTarget != ref) {
                // 捕获冒泡，下发target的事件，其他的currentTarget就不用重复下发了。
                return;
            }
        }

        mTarget = ref;
        RenderEventCallback.EventData data =
                new RenderEventCallback.EventData(pageId, ref, eventName, params, attributes);

        QueuedGestureEvent event = new QueuedGestureEvent(data);

        QueuedGestureEvent last = mPendingGestureEventTail;
        if (last == null) {
            mPendingGestureEventHead = event;
            mPendingGestureEventTail = event;
        } else {
            if (mPendingGestureEventTail.mEvent.pageId != pageId) {
                // 只能同时下发一个页面的touch
                Log.e(TAG, "put() invalidate event, the pageId must be unique!");
                return;
            }
            mPendingGestureEventTail.mNext = event;
            mPendingGestureEventTail = event;
        }
    }

    public boolean contains(int pageId, int ref, String eventName) {
        if (mPendingGestureEventHead == null) {
            return false;
        }

        boolean ret = false;
        QueuedGestureEvent pointer = mPendingGestureEventHead;
        while (pointer != null) {
            QueuedGestureEvent event = pointer;
            pointer = event.mNext;
            RenderEventCallback.EventData eventData = event.mEvent;

            if (pageId == eventData.pageId
                    && ref == eventData.elementId
                    && TextUtils.equals(eventName, eventData.eventName)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    @UiThread
    public void flush() {
        mTarget = -1;
        if (mRenderEventCallback == null) {
            Log.e(TAG, "flush() rendercallback is null");
            return;
        }

        if (mPendingGestureEventHead == null) {
            return;
        }

        int pageId = -1;
        final List<RenderEventCallback.EventData> eventDatas = new ArrayList<>();
        while (mPendingGestureEventHead != null) {
            QueuedGestureEvent event = mPendingGestureEventHead;
            mPendingGestureEventHead = event.mNext;

            if (mPendingGestureEventHead == null) {
                mPendingGestureEventTail = null;
            }
            event.mNext = null;

            RenderEventCallback.EventData eventData = event.mEvent;

            if (pageId == -1) {
                pageId = eventData.pageId;
            }

            eventDatas.add(eventData);
        }
        mRenderEventCallback.onJsMultiEventCallback(pageId, eventDatas);
    }

    private void destroy() {
        HapChoreographer.remove(mRenderEventCallback);
        mRenderEventCallback = null;
    }

    public int getTarget() {
        return mTarget;
    }
}
