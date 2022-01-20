/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

public class AppDependency {
    public final String pkg;
    public final int minVersion;

    public AppDependency(String pkg, int minVersion) {
        this.pkg = pkg;
        this.minVersion = minVersion;
    }
}
