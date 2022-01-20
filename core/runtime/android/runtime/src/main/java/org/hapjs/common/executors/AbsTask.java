/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.executors;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

public abstract class AbsTask<T> implements Runnable {

    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    @WorkerThread
    protected abstract T doInBackground();

    @UiThread
    protected void onPostExecute(T object) {
    }

    @Override
    public final void run() {
        T result = doInBackground();
        sMainHandler.post(() -> onPostExecute(result));
    }
}
