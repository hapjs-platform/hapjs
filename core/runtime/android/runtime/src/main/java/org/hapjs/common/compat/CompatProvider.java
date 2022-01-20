/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.compat;

import org.hapjs.common.location.LocationProvider;

public interface CompatProvider {
    String NAME = "CompatProvider";

    LocationProvider createLocationProvider();
}
