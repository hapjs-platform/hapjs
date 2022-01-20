/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Callback interface for hybrid invocation with {@link Extension.Mode#CALLBACK}.
 */
public class Callback {

    protected String mJsCallback;
    protected Extension.Mode mMode;
    protected AtomicBoolean mIsCalled = new AtomicBoolean(false);
    private ExtensionManager mExtensionManager;

    /**
     * Construct a new instance.
     *
     * @param extensionManager bridge manager.
     * @param jsCallback       callback.
     * @param mode             Invocation mode
     */
    public Callback(ExtensionManager extensionManager, String jsCallback, Extension.Mode mode) {
        mExtensionManager = extensionManager;
        mJsCallback = jsCallback;
        mMode = mode;
    }

    public boolean isValid() {
        return ExtensionManager.isValidCallback(mJsCallback);
    }

    /**
     * Invoke callback with specified response.
     *
     * @param response invocation response.
     */
    public void callback(Response response) {
        if (!isValid()) {
            return;
        }
        if (mMode == Extension.Mode.CALLBACK
                || mIsCalled.compareAndSet(false, true)
                || response.getCode() == Response.CODE_CUSTOM_CALLBACK) {
            doCallback(response);
        }
    }

    protected void doCallback(Response response) {
        mExtensionManager.callback(response, mJsCallback);
    }
}
