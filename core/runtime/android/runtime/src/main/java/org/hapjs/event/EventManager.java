/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.event;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {
    private static final String TAG = "EventManager";
    private ClassLoader mClassLoader;
    private ConcurrentHashMap<String, List<EventTarget>> mEventTargets = new ConcurrentHashMap<>();

    private EventManager() {
        mClassLoader = getClass().getClassLoader();
    }

    public static EventManager getInstance() {
        return Holder.INSTANCE;
    }

    public void invoke(Event event) {
        String eventName = event.getName();
        List<EventTarget> eventTargetList = getEventTargets(eventName);
        for (EventTarget target : eventTargetList) {
            if (target != null) {
                target.invoke(event);
            }
        }
    }

    private List<EventTarget> getEventTargets(String name) {
        List<EventTarget> result = mEventTargets.get(name);
        if (result == null) {
            result = new ArrayList<>();
            List<EventTargetMetaData> metaDataList =
                    EventTargetDataSet.getInstance().getEventTargetMetaDataList(name);
            if (metaDataList != null) {
                for (EventTargetMetaData metaData : metaDataList) {
                    EventTarget eventTarget = createEventTarget(mClassLoader, metaData);
                    if (eventTarget == null) {
                        throw new RuntimeException("Fail to init event target!");
                    }
                    result.add(eventTarget);
                }
            }
            List<EventTarget> old = mEventTargets.putIfAbsent(name, result);
            if (old != null) {
                result = old;
            }
            return result;
        }
        return result;
    }

    private EventTarget createEventTarget(
            ClassLoader classLoader, EventTargetMetaData eventTargetMetaData) {
        try {
            @SuppressWarnings("unchecked")
            Class<EventTarget> hfc =
                    (Class<EventTarget>) classLoader.loadClass(eventTargetMetaData.getModule());
            EventTarget eventTarget = hfc.newInstance();
            return eventTarget;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "event target not found: " + eventTargetMetaData.getModule(), e);
        } catch (InstantiationException e) {
            Log.e(TAG, "event target cannot be instantiated: " + eventTargetMetaData.getModule(),
                    e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "event target cannot be accessed: " + eventTargetMetaData.getModule(), e);
        }
        return null;
    }

    private static class Holder {
        static final EventManager INSTANCE = new EventManager();
    }
}
