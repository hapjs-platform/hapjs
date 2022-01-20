/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share.impl;

import android.content.Context;
import android.content.ContextWrapper;

public class ContextProxy extends ContextWrapper {

    private final String proxyPackageName;

    public ContextProxy(Context base, String packageName) {
        super(base);
        this.proxyPackageName = packageName;
    }

    @Override
    public String getPackageName() {
        return proxyPackageName;
    }
}