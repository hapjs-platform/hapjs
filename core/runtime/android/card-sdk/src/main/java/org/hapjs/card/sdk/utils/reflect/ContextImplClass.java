/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils.reflect;

import android.app.Application;
import android.content.Context;
import java.lang.reflect.Field;

public class ContextImplClass {
    private static Field packageInfo;

    public static Object getPackageInfo(Context context)
            throws IllegalAccessException, NoSuchFieldException {
        Context base = ((Application) context.getApplicationContext()).getBaseContext();
        if (packageInfo == null) {
            packageInfo = base.getClass().getDeclaredField("mPackageInfo");
            packageInfo.setAccessible(true);
        }
        return packageInfo.get(base);
    }
}
