/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.util.Log;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class MetaDataSet {
    private static final String TAG = "MetaDataSet";
    private static final String MetaDataSetImplClassname = "org.hapjs.bridge.MetaDataSetImpl";

    private static MetaDataSet sInstance;

    public static MetaDataSet getInstance() {
        if (sInstance == null) {
            sInstance = createMetaDataSetImpl();
            if (sInstance == null) {
                sInstance = new EmptyMetaDataSet();
            }
        }
        return sInstance;
    }

    private static MetaDataSet createMetaDataSetImpl() {
        try {
            Class klass = Class.forName(MetaDataSetImplClassname);
            return (MetaDataSet) klass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Fail to create MetaDataSetImpl");
            return null;
        }
    }

    public abstract ExtensionMetaData getFeatureMetaData(String name);

    public abstract Map<String, ExtensionMetaData> getFeatureMetaDataMap();

    public abstract String getFeatureMetaDataJSONString(boolean isCardMode);

    public abstract boolean isInResidentWhiteSet(String name);

    public abstract boolean isInResidentNormalSet(String name);

    public abstract boolean isInResidentImportantSet(String name);

    public abstract boolean isInMethodResidentWhiteSet(String name);

    public abstract ExtensionMetaData getModuleMetaData(String name);

    public abstract Map<String, ExtensionMetaData> getModuleMetaDataMap();

    public abstract String getModuleMetaDataJSONString(boolean isCardMode);

    public abstract ExtensionMetaData getWidgetMetaData(String name);

    public abstract String getWidgetMetaDataJSONString(boolean isCardMode);

    public abstract String getWidgetListJSONString(boolean isCardMode);

    public abstract Map<String, ExtensionMetaData> getWidgetMetaDataMap();

    public abstract List<Widget> getWidgetList();

    private static class EmptyMetaDataSet extends MetaDataSet {
        @Override
        public ExtensionMetaData getFeatureMetaData(String name) {
            return null;
        }

        @Override
        public Map<String, ExtensionMetaData> getFeatureMetaDataMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getFeatureMetaDataJSONString(boolean isCardMode) {
            return "";
        }

        @Override
        public boolean isInResidentWhiteSet(String name) {
            return false;
        }

        @Override
        public boolean isInResidentNormalSet(String name) {
            return false;
        }

        @Override
        public boolean isInResidentImportantSet(String name) {
            return false;
        }

        @Override
        public boolean isInMethodResidentWhiteSet(String name) {
            return false;
        }

        @Override
        public ExtensionMetaData getModuleMetaData(String name) {
            return null;
        }

        @Override
        public Map<String, ExtensionMetaData> getModuleMetaDataMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getModuleMetaDataJSONString(boolean isCardMode) {
            return "";
        }

        @Override
        public ExtensionMetaData getWidgetMetaData(String name) {
            return null;
        }

        @Override
        public String getWidgetMetaDataJSONString(boolean isCardMode) {
            return "";
        }

        @Override
        public String getWidgetListJSONString(boolean isCardMode) {
            return "";
        }

        @Override
        public Map<String, ExtensionMetaData> getWidgetMetaDataMap() {
            return Collections.emptyMap();
        }

        @Override
        public List<Widget> getWidgetList() {
            return Collections.emptyList();
        }
    }
}
