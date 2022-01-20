/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

public class BaseMapMarker {
    public static final int DEFAULT_ID = -1;
    public static final String DEFAULT_ANCHOR = "0";

    public int id;
    public double latitude = Double.MAX_VALUE;
    public double longitude = Double.MAX_VALUE;
    public int offsetX = Integer.MAX_VALUE;
    public int offsetY = Integer.MAX_VALUE;
    public String coordType;

    public boolean isInvalid() {
        return (latitude == Double.MAX_VALUE) || (longitude == Double.MAX_VALUE);
    }
}
