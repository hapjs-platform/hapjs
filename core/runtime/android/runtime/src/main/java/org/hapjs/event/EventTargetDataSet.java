/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.event;

import android.util.Log;
import java.util.List;
import java.util.Map;

public abstract class EventTargetDataSet {

    private static final String TAG = "EventTargetDataSet";
    private static final String EventDataSetImplClassname =
            "org.hapjs.event.EventTargetDataSetImpl";

    private static EventTargetDataSet sInstance;

    public static EventTargetDataSet getInstance() {
        if (sInstance == null) {
            sInstance = createMetaDataSetImpl();
            if (sInstance == null) {
                sInstance = new EmptyEventTargetDataSet();
            }
        }
        return sInstance;
    }

    private static EventTargetDataSet createMetaDataSetImpl() {
        try {
            Class klass = Class.forName(EventDataSetImplClassname);
            return (EventTargetDataSet) klass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Fail to create MetaDataSetImpl");
            return null;
        }
    }

    public abstract List<EventTargetMetaData> getEventTargetMetaDataList(String name);

    public abstract Map<String, List<EventTargetMetaData>> getEventTargetMetaDataMap();

    private static class EmptyEventTargetDataSet extends EventTargetDataSet {

        @Override
        public List<EventTargetMetaData> getEventTargetMetaDataList(String name) {
            return null;
        }

        @Override
        public Map<String, List<EventTargetMetaData>> getEventTargetMetaDataMap() {
            return null;
        }
    }
}
