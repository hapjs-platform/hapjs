/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map;

public class CoordType {
    public static final String WGS84 = "wgs84";
    public static final String GPS = WGS84;
    public static final String GCJ02 = "gcj02";

    public static boolean isLegal(String coordType) {
        return GCJ02.equals(coordType) || GPS.equals(coordType);
    }

    public static boolean isLegalCovertFrom(String coordType) {
        return GPS.equals(coordType);
    }

    public static boolean isLegalCovertTo(String coordType) {
        return GCJ02.equals(coordType);
    }
}
