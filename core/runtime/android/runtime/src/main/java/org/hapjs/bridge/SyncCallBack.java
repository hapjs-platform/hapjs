/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public class SyncCallBack extends Callback {

    private Response mResponse;

    public SyncCallBack() {
        super(null, null, Extension.Mode.SYNC);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    protected void doCallback(Response response) {
        mResponse = response;
    }

    public Response getResponse() {
        return mResponse;
    }
}
