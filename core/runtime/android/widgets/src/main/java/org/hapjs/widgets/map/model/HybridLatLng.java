/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

public final class HybridLatLng {

    public static final double FACTOR = 1000000.0D;

    public final double latitude;
    public final double latitudeE6;
    public final double longitude;
    public final double longitudeE6;
    public String coordType;

    public HybridLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.latitudeE6 = latitude * FACTOR;
        this.longitude = longitude;
        this.longitudeE6 = longitude * FACTOR;
    }

    public HybridLatLng(double latitude, double longitude, String coordType) {
        this.latitude = latitude;
        this.latitudeE6 = latitude * FACTOR;
        this.longitude = longitude;
        this.longitudeE6 = longitude * FACTOR;
        this.coordType = coordType;
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }

    /**
     * @return 生成标识String，用于判断LatLng是同一个
     */
    public String getGenerateSymbol() {
        return coordType + "(" + latitude + ", " + longitude + ")";
    }
}
