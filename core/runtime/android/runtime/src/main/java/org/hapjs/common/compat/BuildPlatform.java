/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.compat;

import android.text.TextUtils;
import org.hapjs.runtime.BuildConfig;

public class BuildPlatform {
    private static final String PHONE = "phone";
    private static final String TV = "tv";
    private static final String CAR = "car";

    public static boolean isPhone() {
        return TextUtils.equals(PHONE, BuildConfig.FLAVOR);
    }

    public static boolean isTV() {
        return TextUtils.equals(TV, BuildConfig.FLAVOR);
    }

    public static boolean isCar() {
        return TextUtils.equals(CAR, BuildConfig.FLAVOR);
    }
}
