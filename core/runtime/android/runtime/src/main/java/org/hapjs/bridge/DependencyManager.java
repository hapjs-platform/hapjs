/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.util.Log;

public abstract class DependencyManager {
    private static final String TAG = "DependencyManager";
    private static final String PropertyManagerImplClassname =
            "org.hapjs.bridge.DependencyManagerImpl";

    private static volatile DependencyManager sInstance;

    public static DependencyManager getInstance() {
        if (sInstance == null) {
            sInstance = createImpl();
            if (sInstance == null) {
                sInstance = new DependencyManager.EmptyDependencyManager();
            }
        }
        return sInstance;
    }

    private static DependencyManager createImpl() {
        try {
            Class klass = Class.forName(PropertyManagerImplClassname);
            return (DependencyManager) klass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Fail to create MetaDataSetImpl");
            return null;
        }
    }

    public abstract Dependency getDependency(String key);

    public static class Dependency {
        private String iClass;

        public Dependency(String klass) {
            iClass = klass;
        }

        public String getClassName() {
            return iClass;
        }
    }

    private static class EmptyDependencyManager extends DependencyManager {

        @Override
        public Dependency getDependency(String key) {
            return null;
        }
    }
}
