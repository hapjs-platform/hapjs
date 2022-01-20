/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum Placement {
    BOTTOM(0),
    TOP(1),
    LEFT(2),
    RIGHT(3),
    BOTTOM_LEFT(4),
    BOTTOM_RIGHT(5),
    TOP_LEFT(6),
    TOP_RIGHT(7);

    private static Placement[] sValues = values();
    private int mIntValue;

    Placement(int intValue) {
        mIntValue = intValue;
    }

    public static Placement fromString(@NonNull String value) {
        switch (value) {
            case "left":
                return LEFT;
            case "right":
                return RIGHT;
            case "top":
                return TOP;
            case "bottom":
                return BOTTOM;
            case "topLeft":
                return TOP_LEFT;
            case "topRight":
                return TOP_RIGHT;
            case "bottomLeft":
                return BOTTOM_LEFT;
            case "bottomRight":
                return BOTTOM_RIGHT;
            default:
                return BOTTOM;
        }
    }

    @Nullable
    public Placement next() {
        int index = ordinal() + 1;
        if (index >= sValues.length) {
            return null;
        }
        return sValues[index];
    }
}
