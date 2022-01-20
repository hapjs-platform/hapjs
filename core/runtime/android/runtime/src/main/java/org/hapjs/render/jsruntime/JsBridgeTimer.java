/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.os.Handler;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.Choreographer;
import androidx.annotation.UiThread;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8RuntimeException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.common.executors.Executors;

public class JsBridgeTimer extends V8Object {
    private JsContext mJsContext;
    private Handler mJsThreadHandler;
    private Map<Integer, CallbackData> mCallbackDatas;
    private SparseArray<SparseArray<CallbackType>> mCallbackMap;

    public JsBridgeTimer(JsContext jsContext, Handler jsThreadHandler) {
        super(jsContext.getV8());
        mJsContext = jsContext;
        mJsThreadHandler = jsThreadHandler;
        mCallbackDatas = new ConcurrentHashMap<>();
        mCallbackMap = new SparseArray<>();
    }

    public void requestAnimationFrameNative(int pageId, int id) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                CallbackData callbackData = new CallbackData(id, false, 0);
                                Choreographer choreographer = Choreographer.getInstance();
                                choreographer.postFrameCallback(callbackData);
                                mCallbackDatas.put(id, callbackData);
                                addCallback(pageId, id, CallbackType.Animation);
                            }
                        });
    }

    public void cancelAnimationFrameNative(int id) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                CallbackData callbackData = mCallbackDatas.remove(id);
                                if (callbackData == null) {
                                    return;
                                }
                                Choreographer choreographer = Choreographer.getInstance();
                                choreographer.removeFrameCallback(callbackData);
                                removeCallback(id);
                            }
                        });
    }

    public void setTimeoutNative(int pageId, int id, int time) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                CallbackData callbackData = new CallbackData(id, false, time);
                                mJsThreadHandler.postDelayed(callbackData, time);
                                mCallbackDatas.put(id, callbackData);
                                addCallback(pageId, id, CallbackType.Timer);
                            }
                        });
    }

    public void clearTimeoutNative(int id) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                CallbackData callbackData = mCallbackDatas.remove(id);
                                if (callbackData == null) {
                                    return;
                                }
                                mJsThreadHandler.removeCallbacks(callbackData);
                                removeCallback(id);
                            }
                        });
    }

    public void setIntervalNative(int pageId, int id, int time) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                CallbackData callbackData = new CallbackData(id, true, time);
                                mJsThreadHandler.postDelayed(callbackData, time);
                                mCallbackDatas.put(id, callbackData);
                                addCallback(pageId, id, CallbackType.Timer);
                            }
                        });
    }

    public void clearIntervalNative(int id) {
        clearTimeoutNative(id);
    }

    void clearTimers(int pageId) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                SparseArray<CallbackType> callbackIds = mCallbackMap.get(pageId);
                                if (callbackIds != null) {
                                    mCallbackMap.remove(pageId);

                                    final int N = callbackIds.size();
                                    for (int index = 0; index < N; ++index) {
                                        int callbackId = callbackIds.keyAt(index);
                                        CallbackType type = callbackIds.valueAt(index);
                                        if (type == CallbackType.Timer) {
                                            clearTimeoutNative(callbackId);
                                        } else if (type == CallbackType.Animation) {
                                            cancelAnimationFrameNative(callbackId);
                                        }
                                    }
                                }
                            }
                        });
    }

    private void removeDataInner(int id) {
        Executors.ui()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                mCallbackDatas.remove(id);
                                removeCallback(id);
                            }
                        });
    }

    @UiThread
    private void addCallback(int pageId, int callbackId, CallbackType type) {
        SparseArray<CallbackType> callbackIds = mCallbackMap.get(pageId);
        if (callbackIds == null) {
            callbackIds = new SparseArray<>();
            mCallbackMap.append(pageId, callbackIds);
        }
        callbackIds.append(callbackId, type);
    }

    @UiThread
    private void removeCallback(int callbackId) {
        final int N = mCallbackMap.size();
        for (int index = 0; index < N; ++index) {
            SparseArray<CallbackType> callbackIds = mCallbackMap.valueAt(index);
            callbackIds.remove(callbackId);
        }
    }

    private enum CallbackType {
        Timer,
        Animation
    }

    class CallbackData implements Runnable, Choreographer.FrameCallback {
        public final int id;
        public final boolean isRepeat;
        public final int time;
        private long mEstimatedFireTime;

        public CallbackData(int id, boolean isRepeat, int time) {
            this.id = id;
            this.isRepeat = isRepeat;
            this.time = time;
            if (this.isRepeat) {
                mEstimatedFireTime = SystemClock.uptimeMillis() + time;
            }
        }

        @Override
        public void run() {
            if (isRepeat) {
                // 0 is illegal , could not be executed
                if (time == 0) {
                    return;
                }
                long now = SystemClock.uptimeMillis();
                long nextFireTime = mEstimatedFireTime + time;
                if (now >= nextFireTime) {
                    nextFireTime += ((now - nextFireTime) / time + 1) * time;
                }
                mJsThreadHandler.postAtTime(this, nextFireTime);
                mEstimatedFireTime = nextFireTime;
            } else {
                removeDataInner(id);
            }

            V8 v8 = mJsContext.getV8();
            V8Array arr = new V8Array(v8);
            arr.push(id);
            try {
                if (!isRepeat) {
                    v8.executeFunction("setTimeoutCallback", arr);
                } else {
                    v8.executeFunction("setIntervalCallback", arr);
                }
            } catch (V8RuntimeException ex) {
                mJsContext.getJsThread().processV8Exception(ex);
            } finally {
                JsUtils.release(arr);
            }
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            mJsThreadHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            V8 v8 = mJsContext.getV8();
                            V8Array arr = new V8Array(v8);
                            arr.push(id);
                            arr.push(((double) frameTimeNanos) / 1000000.0D);
                            try {
                                v8.executeFunction("requestAnimationFrameCallback", arr);
                            } catch (V8RuntimeException ex) {
                                mJsContext.getJsThread().processV8Exception(ex);
                            } finally {
                                JsUtils.release(arr);
                            }
                        }
                    });

            removeDataInner(id);
        }
    }
}
