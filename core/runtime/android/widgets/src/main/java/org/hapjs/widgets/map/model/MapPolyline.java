/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.graphics.Color;
import java.util.List;

public class MapPolyline {

    public static final int DEFAULT_COLOR = Color.parseColor("#666666");
    public static final String DEFAULT_WIDTH = "10px";

    public List<HybridLatLng> points;
    public int color;
    public int width;
    public boolean dotted;
    public boolean arrowLine;
    public String arrowIconPath;
    public int zIndex;
}
