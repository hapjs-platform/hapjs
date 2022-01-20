/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public class InstallFlagImpl implements InstallFlag {
    private final int mTotalCount;
    private int mSuccessCount;
    private int mFinishCount;
    private boolean mInstallStarted;

    public InstallFlagImpl(int totalCount) {
        this(totalCount, totalCount);
    }

    public InstallFlagImpl(int totalCount, int needUpdateCount) {
        mTotalCount = totalCount;
        mSuccessCount = totalCount - needUpdateCount;
        mFinishCount = totalCount - needUpdateCount;
        mInstallStarted = mSuccessCount > 0;
    }

    @Override
    public synchronized boolean startInstall() {
        if (mInstallStarted) {
            return false;
        }
        mInstallStarted = true;
        return true;
    }

    @Override
    public synchronized boolean increaseFinishAndCheckAll(boolean succ) {
        mFinishCount++;
        if (succ) {
            mSuccessCount++;
        }
        return mFinishCount == mTotalCount;
    }

    @Override
    public synchronized boolean isAllFinished() {
        return mFinishCount == mTotalCount;
    }

    @Override
    public synchronized boolean isAllSuccess() {
        return mSuccessCount == mTotalCount;
    }

    @Override
    public synchronized boolean hasSuccess() {
        return mSuccessCount > 0;
    }

    @Override
    public synchronized void retryOne() {
        mFinishCount--;
    }
}
