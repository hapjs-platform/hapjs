/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public interface InstallFlag {
    boolean startInstall();

    boolean increaseFinishAndCheckAll(boolean succ);

    boolean isAllFinished();

    boolean isAllSuccess();

    boolean hasSuccess();

    void retryOne();
}
