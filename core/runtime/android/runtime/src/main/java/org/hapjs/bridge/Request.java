/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.text.TextUtils;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Hybrid invocation request. Hold all necessary information needed by feature.
 *
 * @see FeatureExtension
 */
public class Request {

    private String action;
    private Object rawParams;
    private Callback callback;
    private ApplicationContext applicationContext;
    private HapEngine mHapEngine;
    private NativeInterface nativeInterface;
    private HybridView view;
    private int instanceId;
    private String jsCallback;

    /**
     * Get action.
     *
     * @return action.
     */
    public String getAction() {
        return action;
    }

    /**
     * Set action.
     *
     * @param action action.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Get raw parameters. <br>
     * <br>
     * <b>Note:</b> This method is deprecated for params now support both json text and {@link
     * JavaSerializeObject}
     *
     * @return raw parameters.
     */
    @Deprecated
    public String getRawParams() {
        if (rawParams instanceof String) {
            return (String) rawParams;
        }
        return null;
    }

    /**
     * Set raw parameters.
     *
     * @param rawParams raw parameters.
     */
    public void setRawParams(Object rawParams) {
        this.rawParams = rawParams;
    }

    /**
     * Get params as {@link JSONObject}
     *
     * @return the params. null if the params is {@link JavaSerializeObject}
     * @throws JSONException
     */
    public JSONObject getJSONParams() throws JSONException {
        if (rawParams instanceof String) {
            String rawParamsText = (String) rawParams;
            if (!TextUtils.isEmpty(rawParamsText)) {
                return new JSONObject(rawParamsText);
            }
        }
        return null;
    }

    /**
     * Get callback.
     *
     * @return callback.
     */
    public Callback getCallback() {
        return callback;
    }

    /**
     * Set callback.
     *
     * @param callback callback.
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * Get application context.
     *
     * @return application context.
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Set application context.
     *
     * @param applicationContext application context.
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Get HapEngine.
     *
     * @return HapEngine.
     */
    public HapEngine getHapEngine() {
        return mHapEngine;
    }

    /**
     * Set HapEngine.
     *
     * @param hapEngine HapEngine.
     */
    public void setHapEngine(HapEngine hapEngine) {
        mHapEngine = hapEngine;
    }

    /**
     * Get native interface.
     *
     * @return native interface.
     */
    public NativeInterface getNativeInterface() {
        return nativeInterface;
    }

    /**
     * Set native interface.
     *
     * @param nativeInterface native interface.
     */
    public void setNativeInterface(NativeInterface nativeInterface) {
        this.nativeInterface = nativeInterface;
    }

    /**
     * Get view.
     *
     * @return view.
     */
    public HybridView getView() {
        return view;
    }

    /**
     * Set view.
     *
     * @param view view.
     */
    public void setView(HybridView view) {
        this.view = view;
    }

    /**
     * 获取对象id
     *
     * @return
     */
    public int getInstanceId() {
        return instanceId;
    }

    /**
     * @param instanceId
     */
    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public String getJsCallback() {
        return jsCallback;
    }

    public void setJsCallback(String jsCallback) {
        this.jsCallback = jsCallback;
    }

    /**
     * Get reader as {@link JavaSerializeObject}
     *
     * @return
     * @throws JSONException
     */
    public SerializeObject getSerializeParams() throws SerializeException {
        if (rawParams instanceof String) {
            try {
                return new JavaSerializeObject(new JSONObject((String) rawParams));
            } catch (JSONException e) {
                // ignore
            }
        } else if (rawParams instanceof SerializeObject) {
            return (SerializeObject) rawParams;
        }
        return null;
    }
}
