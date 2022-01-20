/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation.rgc;

import org.hapjs.bridge.Request;

public interface GeocodeProvider {
    String NAME = "geolocation_rgc";

    // 地理编码
    void geocodeQuery(Request request);

    // 逆地理编码
    void reverseGeocodeQuery(Request request);

    void releaseGeocode();
}
