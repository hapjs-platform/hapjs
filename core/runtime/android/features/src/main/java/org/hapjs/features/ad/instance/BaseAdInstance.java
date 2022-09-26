/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import android.text.TextUtils;
import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.ad.BannerAd;
import org.hapjs.features.ad.BaseAd;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseAdInstance implements InstanceManager.IInstance {
    protected Map<String, Map<String, Callback>> mCallbackMap = new ConcurrentHashMap<>();
    private UnConsumeResponseWrapper mUnConsumeResponseWrapper;

    public void addListener(Request request) {
        String jsCallback = request.getJsCallback();
        if (ExtensionManager.isValidCallback(jsCallback)) {
            String action = request.getAction();
            Callback callback = request.getCallback();
            Map<String, Callback> actionCallbacks = mCallbackMap.get(action);
            if (actionCallbacks == null) {
                actionCallbacks = new ConcurrentHashMap<>();
                actionCallbacks.put(jsCallback, callback);
                mCallbackMap.put(action, actionCallbacks);
            } else {
                actionCallbacks.put(jsCallback, callback);
            }
            callbackUnConsumeResponse(action, callback);
        }
    }

    public void removeListener(Request request) {
        String action = request.getAction();
        String removeAction = "";
        switch (action) {
            case BaseAd.ACTION_OFF_LOAD:
                removeAction = BaseAd.ACTION_ON_LOAD;
                break;
            case BaseAd.ACTION_OFF_CLOSE:
                removeAction = BaseAd.ACTION_ON_CLOSE;
                break;
            case BaseAd.ACTION_OFF_ERROR:
                removeAction = BaseAd.ACTION_ON_ERROR;
                break;
            case BannerAd.ACTION_OFF_RESIZE:
                removeAction = BannerAd.ACTION_ON_RESIZE;
                break;
            default:
                break;
        }

        String jsCallback = request.getJsCallback();
        // When it is invalid, This type of event needs to be removed
        if (ExtensionManager.isValidCallback(jsCallback)) {
            Map<String, Callback> actionCallbacks = mCallbackMap.get(removeAction);
            if (actionCallbacks != null) {
                actionCallbacks.remove(jsCallback);
            }
        } else {
            mCallbackMap.remove(removeAction);
        }
    }

    protected void onLoad() {
        Map<String, Callback> actionCallbacks = mCallbackMap.get(BaseAd.ACTION_ON_LOAD);
        Response response = Response.SUCCESS;
        if (actionCallbacks == null) {
            cacheUnConsumeResponse(BaseAd.ACTION_ON_LOAD, response);
            return;
        }
        for (Map.Entry<String, Callback> entry : actionCallbacks.entrySet()) {
            entry.getValue().callback(response);
        }
    }

    protected void onClose() {
        Map<String, Callback> actionCallbacks = mCallbackMap.get(BaseAd.ACTION_ON_CLOSE);
        Response response = Response.SUCCESS;
        if (actionCallbacks == null) {
            cacheUnConsumeResponse(BaseAd.ACTION_ON_CLOSE, response);
            return;
        }
        for (Map.Entry<String, Callback> entry : actionCallbacks.entrySet()) {
            entry.getValue().callback(response);
        }
    }

    protected void onClose(boolean isEnded) {
        JSONObject jsonObject = new JSONObject();
        Response response = null;
        try {
            jsonObject.put(AdConstants.KEY_IS_ENDED, isEnded);
            response = new Response(jsonObject);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "onClose fail,JSONException occurred", e);
            response = new Response(Response.CODE_GENERIC_ERROR,
                    "onClose fail,JSONException occurred");
        }
        Map<String, Callback> actionCallbacks = mCallbackMap.get(BaseAd.ACTION_ON_CLOSE);
        if (actionCallbacks == null) {
            cacheUnConsumeResponse(BaseAd.ACTION_ON_CLOSE, response);
            return;
        }
        for (Map.Entry<String, Callback> entry : actionCallbacks.entrySet()) {
            entry.getValue().callback(response);
        }
    }

    protected void onError(int code, String msg) {
        JSONObject jsonObject = new JSONObject();
        Response response = null;
        try {
            jsonObject.put(AdConstants.ERROR_CODE, code);
            jsonObject.put(AdConstants.ERROR_MSG, msg);
            response = new Response(jsonObject);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "onError fail,JSONException occurred", e);
            response = new Response(Response.CODE_GENERIC_ERROR,
                    "onError fail,JSONException occurred");
        }
        Map<String, Callback> actionCallbacks = mCallbackMap.get(BaseAd.ACTION_ON_ERROR);
        if (actionCallbacks == null) {
            cacheUnConsumeResponse(BaseAd.ACTION_ON_ERROR, response);
            return;
        }
        for (Map.Entry<String, Callback> entry : actionCallbacks.entrySet()) {
            entry.getValue().callback(response);
        }
    }

    private void callbackUnConsumeResponse(String action, Callback callback) {
        if (mUnConsumeResponseWrapper == null || action == null || callback == null) {
            return;
        }
        if (action.equals(mUnConsumeResponseWrapper.mAction)
                && mUnConsumeResponseWrapper.mResponse != null) {
            callback.callback(mUnConsumeResponseWrapper.mResponse);
            mUnConsumeResponseWrapper = null;
        }
    }

    protected void cacheUnConsumeResponse(String action, Response response) {
        if (TextUtils.isEmpty(action) || response == null) {
            return;
        }
        mUnConsumeResponseWrapper = new UnConsumeResponseWrapper(action, response);
    }

    private class UnConsumeResponseWrapper {
        String mAction;
        Response mResponse;

        UnConsumeResponseWrapper(String action, Response response) {
            mAction = action;
            mResponse = response;
        }
    }

    /**
     * 不支持广告相关功能提示
     */
    protected void callbackDefaultMockupErrorResponse() {
        onError(AdConstants.ERROR_UNKNOWN, "Mockup does not support ad-related features");
    }

}
