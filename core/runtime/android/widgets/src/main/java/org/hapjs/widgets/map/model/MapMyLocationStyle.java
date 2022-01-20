/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

public class MapMyLocationStyle {
    public static final int DEFAULT_CIRCLE_FILL_COLOR = 0x2687CEEB;
    public static final int DEFAULT_CIRCLE_STROKE_COLOR = 0x2687CEEB;

    public String iconPath;
    public int accuracyCircleFillColor = DEFAULT_CIRCLE_FILL_COLOR;
    public int accuracyCircleStrokeColor = DEFAULT_CIRCLE_STROKE_COLOR;
}
