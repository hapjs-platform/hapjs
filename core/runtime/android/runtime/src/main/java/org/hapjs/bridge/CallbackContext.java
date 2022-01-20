/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public abstract class CallbackContext {
    protected final Request mRequest;
    private final CallbackContextHolder mHolder;
    private final String mAction;
    private final boolean mReserved;
    private final LifecycleListener mLifecycleListener;

    public CallbackContext(
            CallbackContextHolder holder, String action, Request request, boolean reserved) {
        mHolder = holder;
        mAction = action;
        mRequest = request;
        mReserved = reserved;
        mLifecycleListener =
                new LifecycleListener() {
                    @Override
                    public void onDestroy() {
                        mHolder.removeCallbackContext(mAction);
                    }
                };
    }

    public String getAction() {
        return mAction;
    }

    public Request getRequest() {
        return mRequest;
    }

    public boolean isReserved() {
        return mReserved;
    }

    public abstract void callback(int what, Object obj);

    public void onCreate() {
        if (mLifecycleListener != null) {
            mRequest.getNativeInterface().addLifecycleListener(mLifecycleListener);
        }
    }

    public void onDestroy() {
        if (mLifecycleListener != null) {
            mRequest.getNativeInterface().removeLifecycleListener(mLifecycleListener);
        }
    }
}
