/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

public class IntegerUtil {
    public static final int UNDEFINED = Integer.MAX_VALUE;

    public static boolean isUndefined(int value) {
        return value == UNDEFINED;
    }

    public static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return UNDEFINED;
    }
}
