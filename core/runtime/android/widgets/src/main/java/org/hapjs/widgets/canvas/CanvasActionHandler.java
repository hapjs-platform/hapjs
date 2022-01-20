/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hapjs.common.executors.Executors;
import org.hapjs.widgets.canvas.canvas2d.CanvasContextRendering2D;

public class CanvasActionHandler {

    private static volatile CanvasActionHandler INSTANCE = null;
    private final CanvasRenderActionProcessor mActionProcessor;
    private final ActionDispatcher mActionDispatcher;
    private CanvasWaitChannel mWaitChannel;
    private List<OnActionHandleCallback> mActionHandleCallbacks = new CopyOnWriteArrayList<>();

    private CanvasActionHandler() {
        mActionProcessor = new CanvasRenderActionProcessor();
        mWaitChannel = new CanvasWaitChannel();
        mActionDispatcher = new ActionDispatcher();
        mActionDispatcher.start();
    }

    public static CanvasActionHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (CanvasActionHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CanvasActionHandler();
                }
            }
        }
        return INSTANCE;
    }

    public Map<String, Object> processSyncAction(int pageId, int ref, String command)
            throws Exception {
        // getImageData这种同步处理，需要依赖前面的绘制内容，需要等待命令处理完成
        long id = makeId(pageId, ref);
        if (!mWaitChannel.isDone(id)) {
            mWaitChannel.waitFinish(id);
        }
        Map<String, Object> result = new HashMap<>();
        ArrayList<Action> actions = mActionProcessor.process(pageId, ref, command);
        if (actions != null && actions.size() > 0) {
            Action action = actions.get(0);
            if (action instanceof CanvasSyncRenderAction) {
                CanvasContext context = CanvasManager.getInstance().getContext(pageId, ref);
                if (context != null && context.is2d()) {
                    ((CanvasSyncRenderAction) action)
                            .render((CanvasContextRendering2D) context, result);
                }
            }
        }
        return result;
    }

    public void processAsyncActions(int pageId, int ref, String actionsCommand) {
        final long id = makeId(pageId, ref);
        mWaitChannel.doRun(id);
        ActionWork actionWorker = new ActionWork(pageId, ref, actionsCommand, mActionProcessor);
        mActionDispatcher.dispatch(actionWorker);
    }

    public synchronized void addActionHandleCallback(OnActionHandleCallback callback) {
        mActionHandleCallbacks.add(callback);
    }

    public synchronized void addActionHandleCallback(
            int pageId, int canvasId, OnActionHandleCallback callback) {
        mActionHandleCallbacks.add(callback);
        if (isHandleCommandCompleted(pageId, canvasId)) {
            callback.actionHandleComplete(pageId, canvasId);
        }
    }

    public synchronized void removeActionHandleCallback(OnActionHandleCallback callback) {
        mActionHandleCallbacks.remove(callback);
    }

    private synchronized void notifyActionHandleComplete(long id) {
        mWaitChannel.done(id);
        int pageId = (int) (id >> 32);
        int canvasId = (int) (id & 0xFFFFFFFF);
        List<OnActionHandleCallback> callbacks = new ArrayList<>(mActionHandleCallbacks);
        for (OnActionHandleCallback callback : callbacks) {
            callback.actionHandleComplete(pageId, canvasId);
        }
    }

    public synchronized boolean isHandleCommandCompleted(int pageId, int ref) {
        long id = makeId(pageId, ref);
        return mActionDispatcher.isActionCompleted(id);
    }

    public void exist() {
        synchronized (CanvasActionHandler.class) {
            mActionDispatcher.exist();
        }
    }

    private long makeId(int pageId, int ref) {
        return (long) pageId << 32 | ref;
    }

    public interface OnActionHandleCallback {
        void actionHandleComplete(int pageId, int canvasId);
    }

    private static class ActionWork implements Runnable {

        private int mPageId;
        private int mCanvasId;
        private long mId;
        private String mCommand;
        private CanvasRenderActionProcessor mActionProcessor;

        ActionWork(
                int pageId, int canvasId, String actionsCommand,
                CanvasRenderActionProcessor processor) {
            mPageId = pageId;
            mCanvasId = canvasId;
            mId = makeId(pageId, canvasId);
            mCommand = actionsCommand;
            mActionProcessor = processor;
        }

        private long makeId(int pageId, int ref) {
            return (long) pageId << 32 | ref;
        }

        public long getId() {
            return mId;
        }

        @Override
        public void run() {
            ArrayList<Action> renderActions =
                    mActionProcessor.process(mPageId, mCanvasId, mCommand);
            if (renderActions != null && !renderActions.isEmpty()) {
                ArrayList<CanvasRenderAction> actions = new ArrayList<>();
                for (Action renderAction : renderActions) {
                    if (renderAction instanceof CanvasRenderAction) {
                        actions.add((CanvasRenderAction) renderAction);
                    }
                }
                CanvasManager canvasManager = CanvasManager.getInstance();
                canvasManager.addRenderActions(mPageId, mCanvasId, actions);
                canvasManager.triggerRender(mPageId, mCanvasId);
            }
        }
    }

    private class ActionDispatcher {
        private final Object mBlockLock = new Object();
        private Map<Long, ConcurrentLinkedQueue<ActionWork>> mActions;
        private ConcurrentLinkedQueue<ActionWork> mActionLists;
        private boolean mExist = false;

        ActionDispatcher() {
            mActions = new ConcurrentHashMap<>(1);
            mActionLists = new ConcurrentLinkedQueue<>();
        }

        public boolean isActionCompleted(long id) {
            ConcurrentLinkedQueue<ActionWork> queue = mActions.get(id);
            return queue == null || queue.isEmpty();
        }

        public void dispatch(ActionWork worker) {
            if (worker == null) {
                return;
            }

            if (mExist) {
                start();
            }

            ConcurrentLinkedQueue<ActionWork> queue = mActions.get(worker.getId());
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
                mActions.put(worker.getId(), queue);
            }
            queue.add(worker);
            mActionLists.add(worker);
            wakeUp();
        }

        public void start() {
            mExist = false;
            mActionLists.clear();
            mActions.clear();
            Executors.io()
                    .execute(
                            () -> {
                                for (; !mExist; ) {
                                    if (mActionLists.isEmpty()) {
                                        block();
                                    }
                                    ActionWork worker = mActionLists.poll();
                                    if (worker == null) {
                                        continue;
                                    }
                                    worker.run();
                                    ConcurrentLinkedQueue<ActionWork> queue =
                                            mActions.get(worker.getId());
                                    if (queue != null) {
                                        Iterator<ActionWork> iterator = queue.iterator();
                                        while (iterator.hasNext()) {
                                            ActionWork cacheAction = iterator.next();
                                            if (Objects.equals(cacheAction, worker)) {
                                                iterator.remove();
                                                break;
                                            }
                                        }
                                        if (queue.isEmpty()) {
                                            notifyActionHandleComplete(worker.getId());
                                        }
                                    }
                                }
                            });
        }

        public void exist() {
            mExist = true;
            wakeUp();
        }

        private void block() {
            synchronized (mBlockLock) {
                try {
                    mBlockLock.wait();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        private void wakeUp() {
            synchronized (mBlockLock) {
                try {
                    mBlockLock.notifyAll();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}
