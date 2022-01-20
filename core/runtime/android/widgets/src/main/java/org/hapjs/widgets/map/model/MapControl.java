/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.graphics.Bitmap;

public class MapControl {

    public static final int DEFAULT_ID = -1;
    public static final int DEFAULT_POSITION = 0;
    public static final int INVALID_POSITION = -1;
    public static final boolean DEFAULT_CLICKABLE = true;

    public int id;
    public Position position = new Position();
    public String iconPath;
    public Boolean clickable;
    public Bitmap mIcon;

    public class Position {
        public int left;
        public int right;
        public int top;
        public int bottom;
        public int width = Integer.MAX_VALUE;
        public int height = Integer.MAX_VALUE;
    }
}
