/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

public class FloatUtil {
    public static final float UNDEFINED = Float.NaN;

    private static final float EPSILON = .00001f;

    public static boolean floatsEqual(float f1, float f2) {
        if (Float.isNaN(f1) || Float.isNaN(f2)) {
            return Float.isNaN(f1) && Float.isNaN(f2);
        }
        return Math.abs(f2 - f1) < EPSILON;
    }

    public static boolean floatListsEqual(float[] fl1, float[] fl2) {
        if (fl1 == null || fl2 == null) {
            return false;
        }
        if (fl1.length != fl2.length) {
            return false;
        }
        for (int i = 0; i < fl1.length; i++) {
            if (!floatsEqual(fl1[i], fl2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean doublesEqual(double d1, double d2) {
        if (Double.isNaN(d1) || Double.isNaN(d2)) {
            return Double.isNaN(d1) && Double.isNaN(d2);
        }
        return Math.abs(d2 - d1) < EPSILON;
    }

    public static boolean isUndefined(float value) {
        return Float.compare(value, UNDEFINED) == 0;
    }

    public static float parse(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return UNDEFINED;
    }
}
