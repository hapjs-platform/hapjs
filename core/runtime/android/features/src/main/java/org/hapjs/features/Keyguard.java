/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.KeyguardManager;
import android.content.Context;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Keyguard.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = Keyguard.ACTION_GET_KEYGUARD_LOCKED_STATUS,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class Keyguard extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.keyguard";
    protected static final String ACTION_GET_KEYGUARD_LOCKED_STATUS = "getKeyguardLockedStatus";
    protected static final String RESULT_KEY_IS_KEYGUARD_LOCKED = "isKeyguardLocked";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_KEYGUARD_LOCKED_STATUS.equals(action)) {
            getKeyguardLockedStatus(request);
        }
        return null;
    }

    private void getKeyguardLockedStatus(Request request) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        if (context != null) {
            KeyguardManager keyguardManager =
                    (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                JSONObject info = new JSONObject();
                info.put(RESULT_KEY_IS_KEYGUARD_LOCKED, keyguardManager.isKeyguardLocked());
                request.getCallback().callback(new Response(info));
            } else {
                request.getCallback().callback(Response.ERROR);
            }
        } else {
            request.getCallback().callback(Response.ERROR);
        }
    }
}
