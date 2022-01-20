/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.app.Activity;
import android.content.Intent;

public class ActivityHookManager {

    private static ActivityHook sActivityHook;

    public static void init(ActivityHook activityHook) {
        sActivityHook = activityHook;
    }

    public static boolean onStartActivity(Activity activity, Intent intent) {
        if (sActivityHook != null) {
            return sActivityHook.onStartActivity(activity, intent);
        } else {
            return false;
        }
    }

    public interface ActivityHook {
        boolean onStartActivity(Activity activity, Intent intent);
    }
}
