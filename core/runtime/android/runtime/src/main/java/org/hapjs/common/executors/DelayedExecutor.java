/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

public interface DelayedExecutor extends Executor {
    Future executeWithDelay(Runnable runnable, long delay);
}
