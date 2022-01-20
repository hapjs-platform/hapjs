/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.OneShotInstallFlag;
import org.hapjs.cache.PackageInstaller;
import org.hapjs.distribution.AppDistributionMeta;
import org.hapjs.distribution.DistributionService.InstallStatus;
import org.hapjs.distribution.InstallSemaphore;

public abstract class Task implements Runnable {
    private volatile Type mType;
    private AppDistributionMeta mDistributionMeta;
    private boolean mIsUpdate;
    private boolean mApplyUpdateOnly;
    private OneShotInstallFlag mInstallFlag;
    private InstallSemaphore mInstallSemaphore;
    private RunnableFuture<Void> mFuture;
    private volatile boolean mIsFailed = false;

    public Task(
            AppDistributionMeta distributionMeta,
            Type type,
            boolean isUpdate,
            boolean applyUpdateOnly,
            OneShotInstallFlag installFlag,
            InstallSemaphore installSemaphore) {
        mType = type;
        mDistributionMeta = distributionMeta;
        mIsUpdate = isUpdate;
        mApplyUpdateOnly = applyUpdateOnly;
        mInstallFlag = installFlag;
        mInstallSemaphore = installSemaphore;
        mFuture = new FutureTask<>(this, null);
    }

    public int getVersion() {
        return mDistributionMeta.getVersion();
    }

    public AppDistributionMeta getDistributionMeta() {
        return mDistributionMeta;
    }

    public boolean isApplyUpdateOnly() {
        return mApplyUpdateOnly;
    }

    public OneShotInstallFlag getInstallFlag() {
        return mInstallFlag;
    }

    public InstallSemaphore getInstallSemaphore() {
        return mInstallSemaphore;
    }

    public boolean isDone() {
        return mFuture.isDone();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    public void resetInstallFlag() {
        mInstallFlag.retryOne();
    }

    public RunnableFuture<Void> getFuture() {
        return mFuture;
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }

    public boolean isUpdate() {
        return mIsUpdate;
    }

    public String getPackage() {
        return mDistributionMeta.getPackage();
    }

    public void saveAndNotifyLoadStatus(InstallStatus status) {
        throw new UnsupportedOperationException(
                "Type: " + getClass() + " does NOT support saveAndNotifyLoadStatus");
    }

    public boolean isPackageReady() {
        throw new UnsupportedOperationException(
                "Type: " + getClass() + " does NOT support isPackageReady");
    }

    public PackageInstaller createInstaller(InputStream stream) throws CacheException {
        throw new UnsupportedOperationException(
                "Type: " + getClass() + " does NOT support createIntaller(InputStream)");
    }

    public PackageInstaller createInstaller(File archiveFile) throws IOException, CacheException {
        throw new UnsupportedOperationException(
                "Type: " + getClass() + " does NOT support createIntaller(File)");
    }

    public boolean isFailed() {
        return mIsFailed;
    }

    public void setFailed(boolean failed) {
        mIsFailed = failed;
    }

    @Override
    public String toString() {
        return "pkg: "
                + getPackage()
                + ", type="
                + mType
                + ", isUpdate="
                + mIsUpdate
                + ", isDone: "
                + isDone();
    }

    /**
     * 优先级由高到低
     */
    public enum Type {
        FOREGROUND,
        FOREGROUND_PRELOAD,
        BACKGROUND
    }
}
