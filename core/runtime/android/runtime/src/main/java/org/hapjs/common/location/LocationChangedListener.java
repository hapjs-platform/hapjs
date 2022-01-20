/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

public interface LocationChangedListener {
    int CODE_RESULT_RECEIVED = 1;
    int CODE_TIMEOUT = 2;
    int CODE_UNAVAILABLE = 3;
    int CODE_CLOSE = 4;

    void onLocationChanged(HapLocation location, int errorCode);
}
