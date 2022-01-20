/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;

public class IntentUtils {
    private static final String LAUNCH_ACTION_EXTENSION = ".action.LAUNCH";

    private static String sLaunchAction;

    public static String getLaunchAction(Context context) {
        if (sLaunchAction == null) {
            sLaunchAction = context.getPackageName() + LAUNCH_ACTION_EXTENSION;
        }
        return sLaunchAction;
    }
}
