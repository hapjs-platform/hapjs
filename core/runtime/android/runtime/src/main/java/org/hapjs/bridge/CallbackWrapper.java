/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import androidx.annotation.NonNull;

import org.hapjs.logging.RuntimeLogManager;

public class CallbackWrapper extends Callback {
    private static final String TAG = "CallbackWrapper";

    private String mPkg;
    private String mName;
    private String mAction;
    private Callback mCallback;

    public CallbackWrapper(String pkg, String name, String action, @NonNull Callback realCallback) {
        super(null, realCallback.mJsCallback, realCallback.mMode);
        mPkg = pkg;
        mName = name;
        mAction = action;
        mCallback = realCallback;
    }

    @Override
    protected void doCallback(Response response) {
        RuntimeLogManager.getDefault().logFeatureResult(mPkg, mName, mAction, response);
        mCallback.callback(response);
    }

}
