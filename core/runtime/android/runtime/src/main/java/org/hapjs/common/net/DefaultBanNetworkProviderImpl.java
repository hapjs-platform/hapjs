/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DefaultBanNetworkProviderImpl implements BanNetworkProvider {

    @Override
    public boolean shouldBanNetwork() {
        return false;
    }

    @Override
    public Response getBanNetworkResponse(Request request) {
        return new Response.Builder()
                .code(403)
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("not support network access")
                .body(
                        ResponseBody.create(
                                MediaType.get("application/text; charset=utf-8"),
                                "not support network access"))
                .build();
    }
}
