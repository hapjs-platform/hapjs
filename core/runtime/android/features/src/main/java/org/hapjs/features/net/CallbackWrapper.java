/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net;

import org.hapjs.bridge.Response;

public class CallbackWrapper {
    private final org.hapjs.bridge.Callback mCallback;
    private final String mJsCallbackId;

    public CallbackWrapper(org.hapjs.bridge.Callback callback, String jsCallbackId) {
        this.mCallback = callback;
        this.mJsCallbackId = jsCallbackId;
    }

    public void callback(Response response){
        mCallback.callback(response);
    }

    public String getJsCallbackId() {
        return mJsCallbackId;
    }
}
