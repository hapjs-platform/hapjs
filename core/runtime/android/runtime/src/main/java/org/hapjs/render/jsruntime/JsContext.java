/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.util.Log;
import com.eclipsesource.v8.ReferenceHandler;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsContext {
    private static final String TAG = "JsContext";
    private static final boolean DEBUG = false;
    private static final int ACTION_CREATE = 1;
    private static final int ACTION_DISPOSE = 2;
    private static final int TIMEOUT = 100;

    private V8 mV8;
    private JsThread mJsThread;
    private Map<V8Value, Throwable> mNativeObjects;
    private List<PendingAction> mPendingActions;
    private Runnable mPendingTimer;

    public JsContext(JsThread jsThread) {
        mJsThread = jsThread;
        mV8 =
                V8.createV8Runtime(
                        null,
                        null,
                        mJsThread.HAS_INFRASJS_SNAPSHOT ? mJsThread.INFRASJS_SNAPSHOT_SO_NAME :
                                null);
        if (DEBUG) {
            mNativeObjects = new HashMap<>();
            mPendingActions = new ArrayList<>();
            mPendingTimer =
                    new Runnable() {
                        @Override
                        public void run() {
                            for (PendingAction pendingAction : mPendingActions) {
                                if (pendingAction.action == ACTION_CREATE) {
                                    if (!pendingAction.value.isReleased()) {
                                        mNativeObjects
                                                .put(pendingAction.value, pendingAction.stack);
                                    }
                                } else if (pendingAction.action == ACTION_DISPOSE) {
                                    if (!pendingAction.value.isReleased()) {
                                        mNativeObjects.remove(pendingAction.value);
                                    }
                                }
                            }
                            mPendingActions.clear();
                            mJsThread.getHandler().postDelayed(this, TIMEOUT);
                        }
                    };
            mV8.addReferenceHandler(
                    new ReferenceHandler() {
                        @Override
                        public void v8HandleCreated(V8Value object) {
                            mPendingActions
                                    .add(new PendingAction(ACTION_CREATE, object, new Throwable()));
                        }

                        @Override
                        public void v8HandleDisposed(V8Value object) {
                            mPendingActions.add(new PendingAction(ACTION_DISPOSE, object, null));
                        }
                    });
            mJsThread.getHandler().postDelayed(mPendingTimer, TIMEOUT);
        }
    }

    public V8 getV8() {
        return mV8;
    }

    public JsThread getJsThread() {
        return mJsThread;
    }

    public void dispose() {
        mV8.shutdownExecutors(true);
        if (DEBUG) {
            mJsThread.getHandler().removeCallbacks(mPendingTimer);
            mPendingTimer.run();
            for (Map.Entry<V8Value, Throwable> entry : mNativeObjects.entrySet()) {
                V8Value object = entry.getKey();
                if (!object.isReleased()) {
                    Log.e(TAG, "Leak object: " + object, entry.getValue());
                }
            }
        }
        mV8.release(true);
        mV8 = null;
        mJsThread = null;
    }

    private static class PendingAction {
        int action;
        V8Value value;
        Throwable stack;

        PendingAction(int action, V8Value value, Throwable stack) {
            this.action = action;
            this.value = value;
            this.stack = stack;
        }
    }
}
