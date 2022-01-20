/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public class OneShotInstallFlag implements InstallFlag {
    private InstallFlag mImpl;
    private boolean mHasIncreaseFinish;

    public OneShotInstallFlag(InstallFlag impl) {
        mImpl = impl;
    }

    @Override
    public synchronized boolean startInstall() {
        return mImpl.startInstall();
    }

    @Override
    public synchronized boolean increaseFinishAndCheckAll(boolean succ) {
        if (mHasIncreaseFinish) {
            return mImpl.isAllFinished();
        }
        mHasIncreaseFinish = true;
        return mImpl.increaseFinishAndCheckAll(succ);
    }

    @Override
    public synchronized boolean isAllFinished() {
        return mImpl.isAllFinished();
    }

    @Override
    public synchronized boolean isAllSuccess() {
        return mImpl.isAllSuccess();
    }

    @Override
    public synchronized boolean hasSuccess() {
        return mImpl.hasSuccess();
    }

    @Override
    public synchronized void retryOne() {
        if (mHasIncreaseFinish) {
            mHasIncreaseFinish = false;
            mImpl.retryOne();
        }
    }
}
