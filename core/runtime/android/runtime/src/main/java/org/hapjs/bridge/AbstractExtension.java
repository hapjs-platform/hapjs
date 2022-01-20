/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.text.TextUtils;
import android.util.Log;
import org.hapjs.common.executors.Executor;

public abstract class AbstractExtension implements Extension {
    private static final String TAG = "AbstractExtension";

    public static Response getExceptionResponse(Request request, Exception e) {
        return getExceptionResponse(request.getAction(), e);
    }

    public static Response getExceptionResponse(String action, Exception e) {
        return getExceptionResponse(action, e, Response.CODE_GENERIC_ERROR);
    }

    public static Response getExceptionResponse(String action, Exception e, int code) {
        Log.e(TAG, "Fail to invoke: " + action, e);
        return new Response(code, e.getMessage());
    }

    public static Response getErrorResponse(String action, Error e, int code) {
        Log.e(TAG, "Fail to invoke: " + action, e);
        return new Response(code, e.getMessage());
    }

    /**
     * Invoke specified action in this feature.
     *
     * @param request invocation request.
     * @return invocation response. <code>null</code> if current {@link Mode} is {@link Mode#CALLBACK}
     */
    public Response invoke(Request request) {
        Mode mode = getInvocationMode(request);
        if (mode == null) {
            return new Response(Response.CODE_NO_ACTION, "no such action: " + request.getAction());
        } else {
            String proxyAction =
                    ExtensionProxyConfig.getInstance()
                            .getProxyAction(getName(), request.getAction());
            Mode proxyMode = null;
            if (!TextUtils.isEmpty(proxyAction)) { // has proxy action
                request.setAction(proxyAction);
                proxyMode = getInvocationMode(request);
                if (proxyMode == null) {
                    return new Response(Response.CODE_NO_ACTION,
                            "no such action: " + request.getAction());
                }
                if (mode != proxyMode && mode == Mode.SYNC) { // async to sync
                    request.setCallback(new SyncCallBack());
                }
            }

            try {
                Response response = invokeInner(request);
                if (proxyMode != null && proxyMode != mode) {
                    if (mode == Mode.SYNC) { // async to sync
                        return ((SyncCallBack) request.getCallback()).getResponse();
                    } else if (proxyMode == Mode.SYNC) { // sync to async
                        request.getCallback().callback(response);
                    }
                }
                return response;
            } catch (Exception e) {
                if (mode == Mode.SYNC) {
                    return getExceptionResponse(request, e);
                }
                request.getCallback().callback(getExceptionResponse(request, e));
                return null;
            }
        }
    }

    /**
     * Get the invocation mode of specified action.
     *
     * @param request invocation request.
     * @return invocation mode.
     * @see Mode#SYNC
     * @see Mode#ASYNC
     * @see Mode#CALLBACK
     */
    public Mode getInvocationMode(Request request) {
        ExtensionMetaData metaData = getMetaData();
        if (metaData != null) {
            return metaData.getInvocationMode(request.getAction());
        } else {
            Log.w(TAG, "getInvocationMode: metaData is null");
        }
        return null;
    }

    /**
     * Get the custom executor invoke the request .
     *
     * @param request invocation request.
     * @return executor
     */
    public Executor getExecutor(Request request) {
        return null;
    }

    public String[] getPermissions(Request request) {
        ExtensionMetaData metaData = getMetaData();
        if (metaData != null) {
            return metaData.getPermissions(request.getAction());
        } else {
            Log.w(TAG, "getPermissions: metaData is null");
        }
        return null;
    }

    public PermissionPromptStrategy getPermissionPromptStrategy(Request request) {
        return PermissionPromptStrategy.FIRST_TIME;
    }

    protected abstract Response invokeInner(Request request) throws Exception;

    public abstract String getName();

    public abstract ExtensionMetaData getMetaData();

    public boolean isBuiltInExtension() {
        return false;
    }

    public enum PermissionPromptStrategy {
        FIRST_TIME,
        EVERY_TIME
    }
}
