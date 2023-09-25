/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.util.Log;
import java.util.concurrent.CountDownLatch;

public class SyncWaiter<T> {
    private static final String TAG = "SyncWaiter";

    private CountDownLatch mLatch = new CountDownLatch(1);
    private T mResult;
    private T mDefault;

    public SyncWaiter(T def) {
        mDefault = def;
    }

    public void setResult(T result) {
        mResult = result;
        mLatch.countDown();
    }

    public T waitAndGetResult() {
        try {
            mLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted while waiting for result", e);
            return mDefault;
        }
        return mResult;
    }
}
