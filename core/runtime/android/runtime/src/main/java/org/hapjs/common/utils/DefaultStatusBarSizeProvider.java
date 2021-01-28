/*
 * Copyright (C) 2021, hapjs.org. All rights reserved.
 */

package org.hapjs.common.utils;

import android.content.Context;

public class DefaultStatusBarSizeProvider implements StatusBarSizeProvider {

    @Override
    public int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }

        return 0;
    }
}