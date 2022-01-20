/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Application;
import android.content.Context;

public class RuntimeApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Runtime.getInstance().onPreCreate(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Runtime.getInstance().onCreate(this);
    }
}
