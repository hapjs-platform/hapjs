/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

import java.util.concurrent.TimeUnit;

public interface ScheduledExecutor extends DelayedExecutor {

    Future scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    Future scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
