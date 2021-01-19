/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.constants;

import android.content.Context;

public interface FontSizeProvider {
    String NAME = "FontSize";

    float getBestFontSize(Context context, float originalSize);
}
