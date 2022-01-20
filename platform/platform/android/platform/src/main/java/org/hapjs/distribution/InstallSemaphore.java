/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

public class InstallSemaphore {
    private int mStatus = 0;

    public InstallSemaphore() {
        this(false);
    }

    public InstallSemaphore(boolean installDelayed) {
        mStatus = installDelayed ? -1 : 0;
    }

    public synchronized boolean requireDelay() {
        if (mStatus == 0) {
            mStatus = -1;
        }
        return mStatus == -1;
    }

    public synchronized boolean requireInstall() {
        if (mStatus >= 0) {
            mStatus++;
            return true;
        }
        return false;
    }

    public synchronized boolean isDelayed() {
        return mStatus == -1;
    }
}
