/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share.impl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

public class ActivityProxy extends Activity {

    private final String appName;
    private final String packageName;
    private final Activity base;

    public ActivityProxy(Activity activity, String packageName, String appName) {
        super();
        this.packageName = packageName;
        this.appName = appName;
        this.base = activity;
        attachBaseContext(activity);
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        ApplicationInfo applicationInfo = new ApplicationInfo(base.getApplicationInfo());
        applicationInfo.nonLocalizedLabel = appName;
        return applicationInfo;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode,
                                       Bundle options) {
        base.startActivityForResult(intent, requestCode, options);
    }
}