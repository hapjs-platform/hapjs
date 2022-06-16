/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;

public interface StatusBarSizeProvider {
    String NAME = "StatusBarHeight";

    int getStatusBarHeight(Context context);
}
