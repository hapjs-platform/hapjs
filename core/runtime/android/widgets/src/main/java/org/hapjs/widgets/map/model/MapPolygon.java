/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.graphics.Color;
import java.util.List;

public class MapPolygon {
    public static final int DEFAULT_COLOR = Color.parseColor("#666666");
    public static final String DEFAULT_WIDTH = "5px";

    public List<HybridLatLng> points;
    public int fillColor;
    public int strokeWidth;
    public int strokeColor;
    public int zIndex;
}
