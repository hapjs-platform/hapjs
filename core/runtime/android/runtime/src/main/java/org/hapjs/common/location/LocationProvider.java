/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

import android.content.Context;
import java.util.Set;

public interface LocationProvider {

    String NAME = "location";

    String COORTYPE_WGS84 = "wgs84";
    String COORTYPE_GCJ02 = "gcj02";

    ILocationClient createLocationClient(Context context, String type);

    Set<String> getSupportedCoordTypes();
}
