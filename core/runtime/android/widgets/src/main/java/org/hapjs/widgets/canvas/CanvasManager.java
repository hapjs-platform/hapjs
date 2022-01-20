/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.bridge.SimpleActivityStateListener;
import org.hapjs.render.IPage;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.canvas.canvas2d.CanvasContextRendering2D;
import org.hapjs.widgets.canvas.image.CanvasImageHelper;
import org.hapjs.widgets.canvas.webgl.WebGLRenderingContext;

public class CanvasManager extends SimpleActivityStateListener
        implements ApplicationContext.PageLifecycleCallbacks {

    private static final String TAG = "CanvasManager";

    private static final int MAX_CACHED_COMMAND_LIMIT = 1000;

    private static final Object LOCK = new Object();

    private ArrayMap</*pageId*/ Integer, ArrayMap</*ref*/ Integer, Canvas>> mCanvasHolders =
            new ArrayMap<>();
    private ArrayMap</*pageId*/ Integer, ArrayMap</*ref*/ Integer, CanvasContext>>
            mContextArrayMap =
            new ArrayMap<>();
    private ConcurrentHashMap<
            /*pageId*/ Integer,
            ConcurrentHashMap</*ref*/ Integer, ConcurrentLinkedQueue<CanvasRenderAction>>>
            mCanvasRenderingCommandQueue = new ConcurrentHashMap<>();

    private String mPackageName;

    private boolean mHasRegisterPageLifecycle = false;
    private boolean mHasRegisterActivityLifecycle = false;

    private CanvasManager() {
    }

    public static CanvasManager getInstance() {
        return Holder.instance;
    }

    @Override
    public void onPageStart(@NonNull IPage page) {
    }

    @Override
    public void onPageStop(@NonNull IPage page) {
    }

    @Override
    public void onPageDestroy(@NonNull IPage page) {
        mCanvasHolders.remove(page.getPageId());
        ArrayMap<Integer, CanvasContext> contexts = mContextArrayMap.remove(page.getPageId());
        if (contexts != null && contexts.size() > 0) {
            Collection<CanvasContext> values = contexts.values();
            for (CanvasContext canvasContext : values) {
                canvasContext.destroy();
            }
        }
        mCanvasRenderingCommandQueue.remove(page.getPageId());
    }

    @Override
    public void onActivityDestroy() {
        destroy();
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;

        if (!TextUtils.isEmpty(packageName)) {
            if (!mHasRegisterPageLifecycle) {
                ApplicationContext context =
                        HapEngine.getInstance(packageName).getApplicationContext();
                context.registerPageLifecycleCallbacks(this);
                mHasRegisterPageLifecycle = true;
            }
        }
    }

    public void registerActivityLifecycle(RenderEventCallback callback) {
        if (!mHasRegisterActivityLifecycle) {
            callback.addActivityStateListener(this);
            mHasRegisterActivityLifecycle = true;
        }
    }

    public void destroy() {
        mCanvasRenderingCommandQueue.clear();
        mContextArrayMap.clear();
        mCanvasHolders.clear();
        if (mHasRegisterPageLifecycle) {
            ApplicationContext context =
                    HapEngine.getInstance(mPackageName).getApplicationContext();
            context.unregisterPageLifecycleCallbacks(this);
            mHasRegisterPageLifecycle = false;
        }

        mHasRegisterActivityLifecycle = false;
        CanvasImageHelper.getInstance().clear();
    }

    public boolean addCanvas(Canvas canvas) {
        if (canvas == null) {
            return false;
        }

        int pageId = canvas.getPageId();
        if (pageId == Component.INVALID_PAGE_ID) {
            return false;
        }

        int ref = canvas.getRef();
        ArrayMap<Integer, Canvas> map = mCanvasHolders.get(pageId);
        if (map == null) {
            map = new ArrayMap<>();
            mCanvasHolders.put(pageId, map);
        }
        map.put(ref, canvas);
        return true;
    }

    public boolean removeCanvas(Canvas canvas) {
        if (canvas == null) {
            return false;
        }

        int pageId = canvas.getPageId();
        if (pageId == Component.INVALID_PAGE_ID) {
            return false;
        }

        int ref = canvas.getRef();
        ArrayMap<Integer, Canvas> map = mCanvasHolders.get(pageId);
        if (map != null) {
            map.remove(ref);
            return true;
        }
        return false;
    }

    public Canvas getCanvas(int pageId, int ref) {
        if (!mCanvasHolders.containsKey(pageId)) {
            return null;
        }

        ArrayMap<Integer, Canvas> map = mCanvasHolders.get(pageId);
        if (map == null || !map.containsKey(ref)) {
            return null;
        }
        return map.get(ref);
    }

    public CanvasContext getContext(int pageId, int ref) {
        if (pageId == Component.INVALID_PAGE_ID) {
            return null;
        }
        synchronized (LOCK) {
            return getContextInner(pageId, ref);
        }
    }

    public CanvasContext getContext(int pageId, int ref, String type) {
        if (pageId == Component.INVALID_PAGE_ID) {
            return null;
        }

        if (CanvasImageHelper.getInstance().isDestroyed()) {
            CanvasImageHelper.getInstance().reset();
        }

        if (is2dType(type)) {
            return getContext2d(pageId, ref);
        } else if (isWebGLType(type)) {
            return getWebGLContext(pageId, ref);
        }
        return null;
    }

    private CanvasContext getContextInner(int pageId, int ref) {
        ArrayMap<Integer, CanvasContext> map = mContextArrayMap.get(pageId);
        if (map != null) {
            return map.get(ref);
        }
        return null;
    }

    private CanvasContextRendering2D getContext2d(int pageId, int ref) {

        synchronized (LOCK) {
            CanvasContext context = getContextInner(pageId, ref);
            if (context == null) {
                context =
                        new CanvasContextRendering2D(
                                pageId, ref, HapEngine.getInstance(mPackageName).getDesignWidth());
                setContext(pageId, ref, context);
                prepareCanvasView(pageId, ref);
                return (CanvasContextRendering2D) context;
            }

            if (!context.is2d()) {
                return null;
            }
            return (CanvasContextRendering2D) context;
        }
    }

    private void prepareCanvasView(int pageId, int ref) {
        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Canvas canvas = getCanvas(pageId, ref);
                        if (canvas == null) {
                            return;
                        }
                        canvas.prepareCanvasView();
                        if (canvas.getHostView() != null && canvas.getCanvasView() != null) {
                            canvas.addCanvasView(canvas.getHostView());
                        }
                    }
                });
    }

    private WebGLRenderingContext getWebGLContext(int pageId, int ref) {
        synchronized (LOCK) {
            CanvasContext context = getContextInner(pageId, ref);
            if (context == null) {
                context =
                        new WebGLRenderingContext(
                                pageId, ref, HapEngine.getInstance(mPackageName).getDesignWidth());
                setContext(pageId, ref, context);
                return (WebGLRenderingContext) context;
            }

            if (!context.isWebGL()) {
                return null;
            }
            return (WebGLRenderingContext) context;
        }
    }

    private boolean is2dType(String type) {
        return TextUtils.equals(type, "2d");
    }

    private boolean isWebGLType(String type) {
        return TextUtils.equals(type, "webgl");
    }

    private void setContext(int pageId, int ref, CanvasContext context) {
        if (pageId == Component.INVALID_PAGE_ID) {
            return;
        }
        ArrayMap<Integer, CanvasContext> map = mContextArrayMap.get(pageId);
        if (map == null) {
            map = new ArrayMap<>();
            mContextArrayMap.put(pageId, map);
        }
        map.put(ref, context);
    }

    public void triggerRender(int pageId, int ref) {
        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        CanvasContext context = getContext(pageId, ref);
                        if (context != null && context.is2d()) {
                            ((CanvasContextRendering2D) context).setDirty(true);
                        }

                        Canvas canvas = getCanvas(pageId, ref);
                        if (canvas == null || canvas.getCanvasView() == null) {
                            Log.w(TAG, "triggerRender,canvas or canvasView is null!");
                            return;
                        }
                        canvas.getCanvasView().draw();
                    }
                });
    }

    public void addRenderActions(int pageId, int ref, ArrayList<CanvasRenderAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        ConcurrentHashMap<Integer, ConcurrentLinkedQueue<CanvasRenderAction>> allCachedInPage =
                mCanvasRenderingCommandQueue.get(pageId);
        if (allCachedInPage == null) {
            allCachedInPage = new ConcurrentHashMap<>();
            mCanvasRenderingCommandQueue.put(pageId, allCachedInPage);
        }

        ConcurrentLinkedQueue<CanvasRenderAction> renderActionsQueue = allCachedInPage.get(ref);
        if (renderActionsQueue == null) {
            renderActionsQueue = new ConcurrentLinkedQueue<>();
            allCachedInPage.put(ref, renderActionsQueue);
        }

        if (renderActionsQueue.size() >= actions.size()) {
            ArrayList<CanvasRenderAction> localActions = new ArrayList<>(renderActionsQueue);
            int localSize = localActions.size();
            int addSize = actions.size();
            int start = localSize - addSize;
            boolean same = true;
            for (int i = 0; start < localSize && i < addSize; start++, i++) {
                if (localActions.get(start).hashCode() != actions.get(i).hashCode()) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return;
            }
        }

        CanvasContextRendering2D context = getOrCreateContext2D(pageId, ref);
        if (context == null) {
            Log.e(TAG, "CanvasRenderingContext2D is NULL!");
            return;
        }

        for (CanvasRenderAction action : actions) {
            // todo 限制action数量
            //            while (renderActionsQueue.size() >= MAX_CACHED_COMMAND_LIMIT) {
            //                renderActionsQueue.poll();
            //            }

            if (action.canClear(context)) {
                renderActionsQueue.clear();
                continue;
            }
            renderActionsQueue.add(action);
        }
    }

    private CanvasContextRendering2D getOrCreateContext2D(int pageId, int ref) {
        if (pageId == Component.INVALID_PAGE_ID) {
            return null;
        }
        synchronized (this) {
            return (CanvasContextRendering2D) getContext(pageId, ref, "2d");
        }
    }

    public ArrayList<CanvasRenderAction> getRenderActions(int pageId, int ref) {
        ConcurrentHashMap<Integer, ConcurrentLinkedQueue<CanvasRenderAction>> allCachedInPage =
                mCanvasRenderingCommandQueue.get(pageId);
        if (allCachedInPage == null) {
            return null;
        }

        ConcurrentLinkedQueue<CanvasRenderAction> queue = allCachedInPage.get(ref);
        if (queue == null) {
            return null;
        }

        return new ArrayList<>(queue);
    }

    private static class Holder {
        static CanvasManager instance = new CanvasManager();
    }
}
