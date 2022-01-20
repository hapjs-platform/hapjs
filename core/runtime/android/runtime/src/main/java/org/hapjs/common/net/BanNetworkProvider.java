/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import okhttp3.Request;
import okhttp3.Response;

public interface BanNetworkProvider {
    String NAME = "banNetwork";

    /**
     * whether to ban network requests
     *
     * @return true if we should ban network access, false otherwise
     */
    boolean shouldBanNetwork();

    Response getBanNetworkResponse(Request request);
}
