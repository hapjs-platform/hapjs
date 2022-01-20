/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

class ScheduledExecutorImpl implements ScheduledExecutor {

    private ScheduledThreadPoolExecutor mExecutor;

    public ScheduledExecutorImpl(int corePoolSize, ThreadFactory threadFactory) {
        mExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, new AbortPolicy());
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

    @Override
    public Future executeWithDelay(Runnable runnable, long delay) {
        return new FutureImpl(mExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS));
    }

    @Override
    public Future scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        return new FutureImpl(mExecutor.scheduleAtFixedRate(command, initialDelay, period, unit));
    }

    @Override
    public Future scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return new FutureImpl(mExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit));
    }
}
