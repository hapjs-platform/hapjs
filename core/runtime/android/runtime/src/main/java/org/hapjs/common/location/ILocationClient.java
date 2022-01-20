/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

public interface ILocationClient {

    int CACHE_EXPIRE = 2000;

    void subscribe(boolean useCache, LocationChangedListener listener);

    void unsubscribe();
}
