/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.app.Application;
import android.os.Handler;
import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.android.HandlerUtil;
import com.facebook.stetho.inspector.elements.DocumentProvider;
import com.facebook.stetho.inspector.elements.DocumentProviderFactory;

public class VDocumentProviderFactory implements DocumentProviderFactory, ThreadBound {

    private final Handler mHandler;
    private Application mApplication;

    public VDocumentProviderFactory(Application application) {
        mApplication = application;
        // Use special send handle
        // 1. stetho要求发送和接收的线程必须是同一个
        // 2. android不允许UI线程访问网络接口
        mHandler = new Handler(V8Inspector.getInstance().getSendThread().getLooper());
    }

    @Override
    public DocumentProvider create() {
        return new VDocumentProvider(mApplication, this);
    }

    // ThreadBound implementation
    @Override
    public boolean checkThreadAccess() {
        return HandlerUtil.checkThreadAccess(mHandler);
    }

    @Override
    public void verifyThreadAccess() {
        HandlerUtil.verifyThreadAccess(mHandler);
    }

    @Override
    public <V> V postAndWait(UncheckedCallable<V> c) {
        return HandlerUtil.postAndWait(mHandler, c);
    }

    @Override
    public void postAndWait(Runnable r) {
        HandlerUtil.postAndWait(mHandler, r);
    }

    @Override
    public void postDelayed(Runnable r, long delayMillis) {
        if (!mHandler.postDelayed(r, delayMillis)) {
            throw new RuntimeException("Handler.postDelayed() returned false");
        }
    }

    @Override
    public void removeCallbacks(Runnable r) {
        mHandler.removeCallbacks(r);
    }
}
