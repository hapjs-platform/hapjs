/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

public interface MediaPropertyInfo {
    int getScreenHeight();

    int getScreenWidth();

    int getViewPortHeight();

    int getViewPortWidth();

    int getResolution();

    int getOrientation();

    int getPrefersColorScheme();
}
