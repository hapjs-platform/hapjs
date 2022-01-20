/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.annotation.SuppressLint;
import android.view.Choreographer;
import androidx.annotation.UiThread;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.component.bridge.RenderEventCallback;

public class HapChoreographer {

    public static final int CALLBACK_INPUT = 0;
    public static final int CALLBACK_ANIMATION = 1;
    public static final int CALLBACK_TRAVERSAL = 2;

    private static Map<RenderEventCallback, HapChoreographer> INSTANCES = new HashMap<>();

    private Choreographer mChoreographer;
    private Method mPostCallbackMethod;
    private Method mRemoveCallbackMethod;

    @SuppressLint("PrivateApi")
    @UiThread
    private HapChoreographer() {
        mChoreographer = Choreographer.getInstance();

        try {
            mPostCallbackMethod =
                    mChoreographer
                            .getClass()
                            .getDeclaredMethod("postCallback", int.class, Runnable.class,
                                    Object.class);
            mRemoveCallbackMethod =
                    mChoreographer
                            .getClass()
                            .getDeclaredMethod("removeCallbacks", int.class, Runnable.class,
                                    Object.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    public static HapChoreographer createInstanceIfNecessary(
            RenderEventCallback renderEventCallback) {
        if (INSTANCES.containsKey(renderEventCallback)) {
            return INSTANCES.get(renderEventCallback);
        }

        HapChoreographer instance = new HapChoreographer();
        INSTANCES.put(renderEventCallback, instance);
        return instance;
    }

    public static void remove(RenderEventCallback callback) {
        if (INSTANCES.containsKey(callback)) {
            INSTANCES.remove(callback);
        }
    }

    public void postFrameCallback(FrameCallback callback) {
        mChoreographer.postFrameCallback(callback.realCallback);
    }

    public void postCallback(int callbackType, Runnable action, Object token) {
        if (mPostCallbackMethod != null) {
            try {
                mPostCallbackMethod.invoke(mChoreographer, callbackType, action, token);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeCallbacks(int callbackType, Runnable action, Object token) {
        if (mRemoveCallbackMethod != null) {
            try {
                mRemoveCallbackMethod.invoke(mChoreographer, callbackType, action, token);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public abstract static class FrameCallback {

        private Choreographer.FrameCallback realCallback =
                new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long frameTimeNanos) {
                        FrameCallback.this.doFrame(frameTimeNanos);
                    }
                };

        public abstract void doFrame(long frameTimeNanos);
    }
}
