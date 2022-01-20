/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.graphics.Color;

public class MapCircle {

    public static final int DEFAULT_COLOR = Color.parseColor("#666666");
    public static final String DEFAULT_BORDER_WIDTH = "0px";

    public double latitude;
    public double longitude;
    public String coordType;
    public int radius;
    public int fillColor;
    public int borderWidth;
    public int borderColor;
    public int zIndex;
}
