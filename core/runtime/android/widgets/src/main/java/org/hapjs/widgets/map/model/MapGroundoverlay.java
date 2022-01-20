/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

public class MapGroundoverlay {

    public static final int DEFAULT_ID = -1;

    public int id;
    public HybridLatLng northEast;
    public HybridLatLng southWest;
    public String iconPath;
    public float opacity;
    public boolean visible;
    public int zIndex;
}
