/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution.task;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.hapjs.common.executors.Executors;
import org.hapjs.distribution.task.Task.Type;
import org.hapjs.logging.RuntimeLogManager;

public class TaskDispatcher {
    private static final String TAG = "TaskDispatcher";

    private static final int MAXIMUM_RUNNING_SIZE = 5;
    private static final int MAXIMUN_RUNNING_FOREGROUND = MAXIMUM_RUNNING_SIZE;
    private static final int MAXIMUN_RUNNING_FOREGROUND_PRELOAD = 2;
    private static final int MAXIMUN_RUNNING_BACKGROUND = 1;

    private static final int MSG_DISPATCH_TASKS = 0;
    private static final int MSG_ON_TASK_FINISH = 1;
    private final List<Task> mReadyTasks = new LinkedList<>();
    private final List<Task> mRunningTasks = new LinkedList<>();
    private Handler mHandler =
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_DISPATCH_TASKS:
                            dispatchTasksInner((List) msg.obj);
                            break;
                        case MSG_ON_TASK_FINISH:
                            onTaskFinished((Task) msg.obj);
                            break;
                        default:
                            break;
                    }
                }
            };

    private TaskDispatcher() {
    }

    public static TaskDispatcher getInstance() {
        return InstanceHolder.sInstance;
    }

    public void dispatch(Task task) {
        List<Task> tasks = new LinkedList<>();
        tasks.add(task);
        dispatchAll(tasks);
    }

    public void dispatchAll(List<Task> tasks) {
        mHandler.obtainMessage(MSG_DISPATCH_TASKS, tasks).sendToTarget();
    }

    private void dispatchTasksInner(List<Task> tasks) {
        Log.d(TAG, "dispatch task=" + tasks);
        List<Task> tmpTasks = new LinkedList<>();
        for (Task task : tasks) {
            if (!task.isDone() && !mRunningTasks.contains(task)) {
                tmpTasks.add(task);
            }
        }
        mReadyTasks.removeAll(tasks);
        mReadyTasks.addAll(0, tmpTasks);
        schedule();
    }

    private void onTaskFinished(Task task) {
        if (!mRunningTasks.remove(task)) {
            Log.w(TAG, "remove task failed");
        }
        schedule();
    }

    private void schedule() {
        if (mRunningTasks.size() >= MAXIMUM_RUNNING_SIZE) {
            return;
        }
        if (mReadyTasks.isEmpty()) {
            return;
        }

        for (Type type : Type.values()) {
            // no other type can run when FOREGROUND task is running
            if (type != Type.FOREGROUND && runningTaskCount(Type.FOREGROUND) > 0) {
                return;
            }
            for (Iterator<Task> i = mReadyTasks.iterator(); i.hasNext(); ) {
                Task task = i.next();

                if (task.isDone()) {
                    i.remove();
                    continue;
                }

                if (task.getType() == type && runningTaskCount(type) < maxTaskCount(type)) {
                    i.remove();
                    mRunningTasks.add(task);
                    Log.d(TAG, "execute " + task);
                    Executors.io().execute(new TaskWrapper(task));

                    if (mRunningTasks.size() >= MAXIMUM_RUNNING_SIZE) {
                        return;
                    }
                }
            }
        }
    }

    private int runningTaskCount(Type type) {
        int result = 0;
        for (Task task : mRunningTasks) {
            if (task.getType() == type) {
                result++;
            }
        }
        return result;
    }

    private int maxTaskCount(Type type) {
        switch (type) {
            case FOREGROUND:
                return MAXIMUN_RUNNING_FOREGROUND;
            case FOREGROUND_PRELOAD:
                return MAXIMUN_RUNNING_FOREGROUND_PRELOAD;
            case BACKGROUND:
                return MAXIMUN_RUNNING_BACKGROUND;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    private static class InstanceHolder {
        private static final TaskDispatcher sInstance = new TaskDispatcher();
    }

    private class TaskWrapper implements Runnable {
        private Task mTask;

        TaskWrapper(Task task) {
            mTask = task;
        }

        @Override
        public void run() {
            RuntimeLogManager.getDefault()
                    .logAsyncThreadTaskStart(mTask.getPackage(), "taskDispatcher");
            try {
                mTask.getFuture().run();
            } finally {
                mHandler.obtainMessage(MSG_ON_TASK_FINISH, mTask).sendToTarget();
            }
            RuntimeLogManager.getDefault()
                    .logAsyncThreadTaskEnd(mTask.getPackage(), "taskDispatcher");
        }
    }
}
