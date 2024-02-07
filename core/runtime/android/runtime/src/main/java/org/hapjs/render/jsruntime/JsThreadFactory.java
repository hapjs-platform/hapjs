/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.content.Context;

public class JsThreadFactory {

    private final Object mLock;
    private JsThread mPreloadThread;

    private JsThreadFactory() {
        mLock = new Object();
    }

    public static JsThreadFactory getInstance() {
        return Holder.INSTANCE;
    }

    public void preload(Context context) {
        synchronized (mLock) {
            if (mPreloadThread == null) {
                mPreloadThread = load(context);
            }
        }
    }

    public JsThread create(Context context) {
        JsThread thread;
        synchronized (mLock) {
            if (mPreloadThread == null) {
                thread = load(context);
            } else {
                thread = mPreloadThread;
                mPreloadThread = null;
            }
        }
        return thread;
    }

    private JsThread load(Context context) {
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) {
            return new AppJsThread(applicationContext);
        } else {
            return new AppJsThread(context);
        }
    }

    private static class Holder {
        static final JsThreadFactory INSTANCE = new JsThreadFactory();
    }
}
