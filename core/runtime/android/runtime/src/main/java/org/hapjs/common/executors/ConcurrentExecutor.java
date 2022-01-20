/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConcurrentExecutor implements Executor {

    private final WorkQueue<Runnable> mWorkQueue = new WorkQueue<>();
    private final RejectedExecutionHandler mRejectedPolicy = (r, e) -> mWorkQueue.superOffer(r);
    private ThreadPoolExecutor mExecutor;

    public ConcurrentExecutor(
            int corePoolSize, int maximumPoolSize, long keepAliveTime,
            ThreadFactory threadFactory) {
        mExecutor =
                new ThreadPoolExecutor(
                        corePoolSize,
                        maximumPoolSize,
                        keepAliveTime,
                        TimeUnit.MILLISECONDS,
                        mWorkQueue,
                        threadFactory,
                        mRejectedPolicy);
    }

    @Override
    public void execute(Runnable runnable) {
        mExecutor.execute(runnable);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureImpl<T> future = new FutureImpl<T>(mExecutor.submit(task));
        return future;
    }

    class WorkQueue<E> extends LinkedBlockingQueue<E> {
        @Override
        public boolean offer(E e) {
            if (mExecutor.getActiveCount() < mExecutor.getPoolSize()) {
                return super.offer(e);
            }
            return false;
        }

        public void superOffer(E e) {
            super.offer(e);
        }
    }
}
