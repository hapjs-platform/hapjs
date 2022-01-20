/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.impl.android;

import org.hapjs.model.AppInfo;
import org.hapjs.render.RootView;

public class AndroidViewClient {
    /**
     * Run in js core thread
     */
    public void onRuntimeCreate(RootView view) {
    }

    /**
     * Run in js core thread
     */
    public void onRuntimeDestroy(RootView view) {
    }

    public void onApplicationCreate(RootView view, AppInfo appInfo) {
    }

    public boolean shouldOverrideUrlLoading(RootView view, String url) {
        return false;
    }

    public void onPageStarted(RootView view, String url) {
    }
}
