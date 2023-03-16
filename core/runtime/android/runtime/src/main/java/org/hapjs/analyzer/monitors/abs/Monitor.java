/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.monitors.abs;

public interface Monitor {
    String getName();

    void setEnable(boolean enable);

    boolean isEnabled();

    void start();

    void stop();

    boolean isRunning();

    interface Pipeline<T> {
        void output(T data);
    }
}
