/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

public class ServerSettings {
    private String[] mNativePackages;

    public String[] getNativePackages() {
        return mNativePackages;
    }

    public void setNativePackages(String[] nativePackages) {
        mNativePackages = nativePackages;
    }
}
