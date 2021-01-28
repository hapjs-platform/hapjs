/*
 * Copyright (C) 2021, hapjs.org. All rights reserved.
 */

package org.hapjs.common.utils;

import android.content.Context;

public interface StatusBarSizeProvider {
    String NAME = "StatusBarHeight";

    int getStatusBarHeight(Context context);
}