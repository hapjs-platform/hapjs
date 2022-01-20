/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

public class HybridLatLngBounds {

    public final HybridLatLng southwest;
    public final HybridLatLng northeast;

    public HybridLatLngBounds(HybridLatLng southwest, HybridLatLng northeast) {
        this.southwest = southwest;
        this.northeast = northeast;
    }

    @Override
    public String toString() {
        String str1 = "southwest: " + southwest;
        String str2 = "northeast: " + northeast;
        return "LatLngBounds——" + str1 + " " + str2;
    }
}
