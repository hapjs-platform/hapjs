/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation;

import android.app.Activity;
import org.hapjs.bridge.Request;

public interface GeolocationProvider {

    String NAME = "geolocation";

    void onNavigateButtonClick(Activity activity, NavigationInfo navigationInfo, Request request);
}
