/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public class HybridException extends Exception {

    private static final long serialVersionUID = 1L;

    private Response mResponse;

    public HybridException(int errorCode, String detailMessage) {
        super(new Response(errorCode, detailMessage).toString());
        mResponse = new Response(errorCode, detailMessage);
    }

    public Response getResponse() {
        return mResponse;
    }
}
