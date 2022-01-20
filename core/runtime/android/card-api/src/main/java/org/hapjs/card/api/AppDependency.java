/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public class AppDependency {
    private final String mPkg;
    private final int mMinVersion;

    public AppDependency(String pkg, int minVersion) {
        this.mPkg = pkg;
        this.mMinVersion = minVersion;
    }

    public String getPackage() {
        return mPkg;
    }

    public int getMinVersion() {
        return mMinVersion;
    }

    @Override
    public String toString() {
        return "[mPkg: " + mPkg + ", mMinVersion: " + mMinVersion + "]";
    }
}
