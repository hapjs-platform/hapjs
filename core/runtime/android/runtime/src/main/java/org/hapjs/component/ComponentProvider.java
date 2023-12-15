/*
 * Copyright (c) 2023-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;

public interface ComponentProvider {
    String NAME = "component";

    /**
     * Android O以下版本Bitmap是否默认RGB565
     *
     * @param context
     * @return true: Android O以下版本Bitmap默认RGB565 false:Android O以下版本Bitmap默认ARGB8888
     */
    boolean isDefaultRgb565EnableBelowAndroidO(Context context);

    boolean isSysShowSizeChange();
}