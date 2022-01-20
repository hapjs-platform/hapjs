/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import java.util.Objects;
import org.hapjs.widgets.map.CoordType;

public class LocationInfo {
    public double latitude = 701;
    public double longitude = 701;
    public String coordType = CoordType.GCJ02;
    public String name = "";
    public String address = "";
    public String city;

    public LocationInfo(double latitude, double longitude, String name, String address,
                        String city) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public LocationInfo() {
    }

    public boolean isValid() {
        if (latitude > 700 || longitude > 700) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocationInfo that = (LocationInfo) o;
        return Double.compare(that.latitude, latitude) == 0
                && Double.compare(that.longitude, longitude) == 0
                && Objects.equals(coordType, that.coordType)
                && Objects.equals(name, that.name)
                && Objects.equals(address, that.address)
                && Objects.equals(city, that.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, coordType, name, address, city);
    }

    @Override
    public String toString() {
        return "LocationInfo{"
                + "latitude="
                + latitude
                + ", longitude="
                + longitude
                + ", coordType="
                + coordType
                + ", name='"
                + name
                + '\''
                + ", address='"
                + address
                + '\''
                + ", city='"
                + city
                + '\''
                + '}';
    }
}
