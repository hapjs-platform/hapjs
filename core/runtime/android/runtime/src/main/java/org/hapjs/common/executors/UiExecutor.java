/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

class UiExecutor implements DelayedExecutor {

    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            sMainHandler.post(runnable);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTaskImpl<T> futureTask = new FutureTaskImpl(task);
        if (isMainThread()) {
            futureTask.run();
        } else {
            sMainHandler.post(futureTask);
        }
        return new UiFutureImpl(futureTask);
    }

    @Override
    public Future executeWithDelay(@NonNull Runnable runnable, long delayMillis) {
        FutureTaskImpl futureTask = new FutureTaskImpl(runnable, null);
        sMainHandler.postDelayed(futureTask, delayMillis);
        return new UiFutureImpl(futureTask);
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private static class FutureTaskImpl<V> extends FutureTask<V> {

        AtomicBoolean mIsRun = new AtomicBoolean(false);

        public FutureTaskImpl(Callable<V> callable) {
            super(callable);
        }

        public FutureTaskImpl(Runnable runnable, V result) {
            super(runnable, result);
        }

        @Override
        public void run() {
            if (mIsRun.compareAndSet(false, true)) {
                super.run();
            }
        }
    }

    private static class UiFutureImpl<V> implements Future<V> {

        private FutureTaskImpl<V> mFutureTaskImpl;
        private boolean mIsCancelled;

        UiFutureImpl(FutureTaskImpl futureTaskImpl) {
            mFutureTaskImpl = futureTaskImpl;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mFutureTaskImpl.mIsRun.compareAndSet(false, true)) {
                sMainHandler.removeCallbacks(mFutureTaskImpl);
                mIsCancelled = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return mIsCancelled;
        }

        @Override
        public V get() throws ExecutionException, InterruptedException {
            return mFutureTaskImpl.get();
        }
    }
}
