/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.util.Log;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = ResidentModule.NAME,
        actions = {
                @ActionAnnotation(name = ResidentModule.ACTION_START, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ResidentModule.ACTION_STOP, mode = Extension.Mode.SYNC)
        })
public class ResidentModule extends ModuleExtension {

    protected static final String NAME = "system.resident";
    protected static final String ACTION_START = "start";
    protected static final String ACTION_STOP = "stop";
    private static final String TAG = "ResidentModule";
    private static final String PARAMS_NOTIFICATION_DESC = "desc";

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
    }

    @Override
    public Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_START.equals(action)) {
            return startResident(request);

        } else if (ACTION_STOP.equals(action)) {
            request.getNativeInterface().getResidentManager().postStopResident();
            return Response.SUCCESS;
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    private Response startResident(Request request) throws Exception {
        JSONObject jsonParams = null;
        try {
            jsonParams = request.getJSONParams();
        } catch (JSONException e) {
            Log.e(TAG, "error of json params.");
        }
        if (jsonParams == null) {
            Log.w(TAG, "startResident: jsonParams is null");
            return Response.ERROR;
        }
        String notiDesc = jsonParams.optString(PARAMS_NOTIFICATION_DESC);
        request.getNativeInterface().getResidentManager().postStartResident(notiDesc);
        return Response.SUCCESS;
    }
}
