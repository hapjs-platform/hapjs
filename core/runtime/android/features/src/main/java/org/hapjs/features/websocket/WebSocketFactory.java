/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.websocket;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = WebSocketFactory.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = WebSocketFactory.ACTION_CREATE, mode = FeatureExtension.Mode.SYNC),
        })
public class WebSocketFactory extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.websocketfactory";

    protected static final String ACTION_CREATE = "create";

    private static final String PARAMS_KEY_URL = "url";
    private static final String PARAMS_KEY_HEADER = "header";
    private static final String PARAMS_KEY_PROTOCOLS = "protocols";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws JSONException {
        String action = request.getAction();
        if (ACTION_CREATE.equals(action)) {
            return createWebSocket(request);
        }
        return Response.NO_ACTION;
    }

    private Response createWebSocket(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String url = jsonParams.getString(PARAMS_KEY_URL);
        JSONObject jsonHeader = jsonParams.optJSONObject(PARAMS_KEY_HEADER);
        JSONArray protocols = jsonParams.optJSONArray(PARAMS_KEY_PROTOCOLS);
        SocketTask socketTask = new SocketTask(url, jsonHeader, protocols);
        socketTask.connectSocket();
        HybridManager hybridManager = request.getView().getHybridManager();
        return new Response(
                InstanceManager.getInstance().createInstance(hybridManager, socketTask));
    }
}
