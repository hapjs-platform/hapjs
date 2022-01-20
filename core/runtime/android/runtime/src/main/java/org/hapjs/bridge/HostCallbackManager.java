/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.util.Log;
import android.util.SparseArray;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.hapjs.bridge.HostConnectionMessageCache.HostMessage;
import org.hapjs.bridge.HostConnectionMessageCache.JsMessage;
import org.hapjs.bridge.HostConnectionMessageCache.Message;
import org.hapjs.card.api.CardLifecycleCallback;
import org.hapjs.card.api.CardMessageCallback;
import org.hapjs.common.executors.Executors;

public class HostCallbackManager {
    public static final String ACTION_REGISTER_CALLBACK = "__onregistercallback";
    private static final String TAG = "HostCallbackManager";
    private WeakHashMap<HybridManager, HostEntity> mCallbacks = new WeakHashMap<>();
    private ConcurrentHashMap<Integer, CardLifecycleCallback> mLifecycleCallbacks =
            new ConcurrentHashMap<>();

    private HostConnectionMessageCache mHostMessageCache = new HostConnectionMessageCache();
    private HostConnectionMessageCache mJsMessageCache = new HostConnectionMessageCache();

    private HostCallbackManager() {
    }

    public static HostCallbackManager getInstance() {
        return HostCallbackManagerHolder.INSTANCE;
    }

    public synchronized void addHostCallback(HybridManager manager, CardMessageCallback callback) {
        HostEntity entity;
        if (mCallbacks.containsKey(manager)) {
            entity = mCallbacks.get(manager);
        } else {
            entity = new HostEntity();
            mCallbacks.put(manager, entity);
        }
        if (entity != null) {
            entity.mHostCallback = callback;
            entity.counter = new AtomicInteger(1);
            entity.array = new SparseArray<>();
        } else {
            Log.w(TAG, "addHostCallback: entity is null");
        }

        List<Message> pendingMessages = mHostMessageCache.retriveMessage(manager);
        if (pendingMessages != null) {
            for (Message message : pendingMessages) {
                doHostCallback(manager, message.content, ((HostMessage) message).callback);
            }
        }
    }

    public synchronized void removeHostCallback(HybridManager manager) {
        if (mCallbacks.containsKey(manager)) {
            HostEntity hostEntity = mCallbacks.get(manager);
            if (hostEntity != null) {
                hostEntity.mHostCallback = null;
            } else {
                Log.w(TAG, "removeHostCallback: hostEntity is null");
            }
        }

        mHostMessageCache.retriveMessage(manager);
    }

    public synchronized void removeCallback(HybridManager manager) {
        mCallbacks.remove(manager);
        mHostMessageCache.retriveMessage(manager);
        mJsMessageCache.retriveMessage(manager);
    }

    public synchronized void addJsCallback(
            HybridManager manager, CallbackContextHolder jsCallbackHolder) {
        HostEntity entity;
        if (!mCallbacks.containsKey(manager)) {
            entity = new HostEntity();
            mCallbacks.put(manager, entity);
        } else {
            entity = mCallbacks.get(manager);
        }
        if (entity != null) {
            entity.jsCallbackHolder = jsCallbackHolder;
        } else {
            Log.w(TAG, "addJsCallback: entity is null");
        }

        List<Message> pendingMessages = mJsMessageCache.retriveMessage(manager);
        if (pendingMessages != null) {
            for (Message message : pendingMessages) {
                doJsCallback(manager, ((JsMessage) message).code, message.content);
            }
        }
    }

    public synchronized void removeJsCallback(HybridManager manager) {
        if (mCallbacks.containsKey(manager)) {
            HostEntity hostEntity = mCallbacks.get(manager);
            if (hostEntity != null) {
                hostEntity.jsCallbackHolder = null;
            } else {
                Log.w(TAG, "removeJsCallback: hostEntity is null");
            }
        }

        mJsMessageCache.retriveMessage(manager);
    }

    public synchronized void clearAll() {
        mCallbacks.clear();
        mHostMessageCache.clear();
        mJsMessageCache.clear();
    }

    public synchronized void doHostCallback(
            HybridManager manager, String content, Callback jsCallback) {
        HostEntity entity = mCallbacks.get(manager);
        if (entity == null || entity.mHostCallback == null) {
            mHostMessageCache.addMessage(manager, new HostMessage(content, jsCallback));
            Log.d(TAG, "cache host message");
            return;
        }
        CardMessageCallback callback = entity.mHostCallback;
        int code = entity.counter.getAndIncrement();
        SparseArray<Callback> array = entity.array;
        array.put(code, jsCallback);
        Executors.io().execute(() -> callback.onMessage(code, content));
    }

    public synchronized void doJsCallback(HybridManager manager, int code, String data) {
        HostEntity entity = mCallbacks.get(manager);
        if (entity == null) {
            if (code == 0) {
                mJsMessageCache.addMessage(manager, new JsMessage(code, data));
                Log.d(TAG, "cache js message");
            } else {
                Log.w(TAG, "no js callback for code=" + code);
            }
            return;
        }
        if (code == 0) {
            CallbackContextHolder jsCallbackHolder = entity.jsCallbackHolder;
            if (jsCallbackHolder != null) {
                Executors.io()
                        .execute(() -> jsCallbackHolder
                                .runCallbackContext(ACTION_REGISTER_CALLBACK, 0, data));
            } else {
                mJsMessageCache.addMessage(manager, new JsMessage(code, data));
                Log.d(TAG, "cache js message");
            }
        } else {
            if (entity.array != null) {
                Callback callback = entity.array.get(code);
                if (callback != null) {
                    callback.callback(new Response(data));
                    entity.array.remove(code);
                    return;
                }
            }
            Log.w(TAG, "no js callback for code=" + code);
        }
    }

    public void addMessageCallback(HybridManager manager, final HostMessageCallback callback) {
        addHostCallback(
                manager,
                new CardMessageCallback() {
                    @Override
                    public void onMessage(int code, String data) {
                        callback.onCallback(code, data);
                    }
                });
    }

    public void addLifecycleCallback(int id, CardLifecycleCallback callBack) {
        mLifecycleCallbacks.put(id, callBack);
    }

    public CardLifecycleCallback removeLifecyleCallback(int id) {
        return mLifecycleCallbacks.remove(id);
    }

    public void onCardCreate(int id) {
        CardLifecycleCallback cardLifecycleCallback = mLifecycleCallbacks.get(id);
        if (cardLifecycleCallback != null) {
            cardLifecycleCallback.onCreateFinish();
        }
    }

    public void onCardDestroy(int id) {
        CardLifecycleCallback cardLifecycleCallback = mLifecycleCallbacks.get(id);
        if (cardLifecycleCallback != null) {
            try {
                cardLifecycleCallback.onDestroy();
            } catch (Throwable t) {
                Log.w(TAG, "failed to callback onDestroy", t);
            }
            if (removeLifecyleCallback(id) != null) {
                Log.w(TAG, "CardLifecycleCallback is expected to be removed in onDestroy!");
            }
        }
    }

    private static class HostCallbackManagerHolder {
        private static final HostCallbackManager INSTANCE = new HostCallbackManager();
    }

    private static class HostEntity {
        CardMessageCallback mHostCallback;
        AtomicInteger counter;
        SparseArray<Callback> array;
        CallbackContextHolder jsCallbackHolder;
    }
}
