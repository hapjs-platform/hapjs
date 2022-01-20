/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.app.Activity;
import android.content.Intent;
import java.lang.ref.WeakReference;

public abstract class AbsShareApi {

    private ShareContent mContent;

    private WeakReference<Activity> mActivityWeakReference;

    private Platform mPlatform;

    public AbsShareApi(Activity activity, ShareContent content,
                       Platform media) {
        this.mActivityWeakReference = new WeakReference(activity);
        this.mPlatform = media;
        this.mContent = content;
    }

    public final void share(final ShareListener listener) {
        if (!isAvailable()) {
            notifyError(listener, "app isn't install or support");
            return;
        }
        if (listener != null) {
            listener.onStart(getPlatform());
        }

        //ActivityProxy constructor must be called in the ui thread
        getActicity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onShare(mContent, listener);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    protected abstract void onShare(ShareContent content,
                                    final ShareListener listener);

    protected boolean isConfig() {
        return true;
    }

    protected boolean isInstall() {
        return true;
    }

    protected boolean isSupport() {
        return true;
    }

    public final boolean isAvailable() {
        return isConfig() && isInstall() && isSupport();
    }

    public abstract void release();

    protected Activity getActicity() {
        return mActivityWeakReference.get();
    }

    protected Platform getPlatform() {
        return mPlatform;
    }

    protected ShareContent getContent() {
        return mContent;
    }

    protected void notifyError(ShareListener listener, String message) {
        if (listener != null) {
            listener.onError(getPlatform(), message);
        }
    }

    protected void notifyResult(ShareListener listener) {
        if (listener != null) {
            listener.onResult(getPlatform());
        }
    }
}