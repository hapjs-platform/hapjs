/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

public interface LocationListener {

    void onLocationChanged(double latitude, double longitude, float accuracy);
}
