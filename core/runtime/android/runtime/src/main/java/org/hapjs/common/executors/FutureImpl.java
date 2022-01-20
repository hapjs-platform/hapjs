/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

import java.util.concurrent.ExecutionException;

class FutureImpl<V> implements Future<V> {

    private java.util.concurrent.Future<V> mFuture;

    public FutureImpl(java.util.concurrent.Future<V> future) {
        mFuture = future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mFuture.isCancelled();
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        return mFuture.get();
    }
}
