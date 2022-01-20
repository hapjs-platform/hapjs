/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.util.concurrent.TimeUnit;

public abstract class MainThreadFrameWorker implements Runnable {
    private static int mFrameRate = 60;
    private static long mFrameIntervalNs = (long) (1000000000 / mFrameRate);
    private boolean isRunning = false;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private View mView;

    protected MainThreadFrameWorker(View view) {
        mView = view;
    }

    private static void checkMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return;
        }
        throw new IllegalStateException();
    }

    public void start() {
        checkMainThread();

        if (isRunning) {
            return;
        }

        isRunning = true;
        run();
    }

    @Override
    public void run() {
        long lastFrameVSyncNs =
                mView.getDrawingTime() == 0
                        ? System.nanoTime()
                        : TimeUnit.MILLISECONDS.toNanos(mView.getDrawingTime());

        long nextFrameNs = lastFrameVSyncNs + mFrameIntervalNs;

        boolean isContinue = doTasksWithDeadLine(nextFrameNs);
        if (isContinue) {
            mHandler.post(this);
        } else {
            isRunning = false;
        }
    }

    /**
     * 在当前帧内循环执行任务
     *
     * @return 是否需要在下一帧继续执行
     */
    private boolean doTasksWithDeadLine(long deadlineNs) {
        boolean isContinue = doMiniTask();
        if (!isContinue) {
            return false;
        }

        // 如果没到截至时间, 继续执行
        if (System.nanoTime() < deadlineNs) {
            doTasksWithDeadLine(deadlineNs);
        }

        return true;
    }

    /**
     * 重写需要执行的任务, 会不断调用直到返回 false
     *
     * @return 是否需要继续执行
     */
    public abstract boolean doMiniTask();
}
