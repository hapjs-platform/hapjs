/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation;

public class NavigationInfo {
    public double latitude;
    public double longitude;
    public double scale = 18;
    public String name = "";
    public String address = "";

    public NavigationInfo(
            double latitude, double longitude, double scale, String name, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.scale = scale;
        this.name = name;
        this.address = address;
    }
}
