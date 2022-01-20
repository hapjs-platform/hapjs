/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.util.LongSparseArray;

public class CanvasWaitChannel {

    private static final int MAX_WAIT_TIME = 1000; // 1000ms

    private final Object mLock = new Object();

    private LongSparseArray<Boolean> mDone = new LongSparseArray<>();
    private long mWaitId = -1;

    public final void doRun(long id) {
        synchronized (mLock) {
            mDone.put(id, false);
        }
    }

    public final void done(long id) {
        synchronized (mLock) {
            if (mWaitId == id) {
                mLock.notifyAll();
                mWaitId = -1;
            }
            mDone.remove(id);
        }
    }

    public final void waitFinish(long id) {
        synchronized (mLock) {
            if (mDone.get(id, true)) {
                return;
            }

            try {
                mWaitId = id;
                mLock.wait(MAX_WAIT_TIME);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    public final boolean isDone(long id) {
        synchronized (mLock) {
            return mDone.get(id, true);
        }
    }
}
