/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.app.Activity;
import android.content.Intent;
import org.hapjs.common.resident.ResidentManager;
import org.hapjs.render.RootView;

/**
 * The interface exposed to feature to access internal methods.
 */
public class NativeInterface {

    private HybridManager mManager;

    /**
     * Construct a new instance.
     *
     * @param manager hybrid manager.
     */
    public NativeInterface(HybridManager manager) {
        mManager = manager;
    }

    /**
     * Get current activity.
     *
     * @return current activity.
     */
    public Activity getActivity() {
        return mManager.getActivity();
    }

    /**
     * add activity lifecycle listener.
     *
     * @param listener listener.
     */
    public void addLifecycleListener(LifecycleListener listener) {
        mManager.addLifecycleListener(listener);
    }

    /**
     * remove activity lifecycle listener.
     *
     * @param listener listener.
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        mManager.removeLifecycleListener(listener);
    }

    /**
     * @see Activity#startActivityForResult
     */
    public void startActivityForResult(Intent intent, int requestCode) {
        mManager.startActivityForResult(intent, requestCode);
    }

    /**
     * @see Activity#requestPermissions
     */
    public void requestPermissions(String[] permissions, int requestCode) {
        mManager.requestPermissions(permissions, requestCode);
    }

    public RootView getRootView() {
        if (mManager.getHybridView() == null) {
            return null;
        }
        return ((RootView) mManager.getHybridView().getWebView());
    }

    public ResidentManager getResidentManager() {
        return mManager.getResidentManager();
    }
}
