/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
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
                        name = Keyguard.ACTION_GET_KEYGUARD_LOCKED_STATUS, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Keyguard.ACTION_REQUEST_DISMISS_KEYGUARD, mode = FeatureExtension.Mode.ASYNC)
        })
public class Keyguard extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.keyguard";
    protected static final String ACTION_GET_KEYGUARD_LOCKED_STATUS = "getKeyguardLockedStatus";
    protected static final String ACTION_REQUEST_DISMISS_KEYGUARD = "requestDismissKeyguard";
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
        } else if (ACTION_REQUEST_DISMISS_KEYGUARD.equals(action)) {
            requestDismissKeyguard(request);
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

    private void requestDismissKeyguard(Request request) {
        Activity activity = request.getNativeInterface().getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            request.getCallback().callback(Response.ERROR);
            return;
        }
        if (activity.getWindow() == null) {
            request.getCallback().callback(Response.ERROR);
            return;
        }

        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null) {
            request.getCallback().callback(Response.ERROR);
            return;
        }
        keyguardManager.requestDismissKeyguard(activity, new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissError() {
                super.onDismissError();
                request.getCallback().callback(Response.ERROR);
            }

            @Override
            public void onDismissSucceeded() {
                super.onDismissSucceeded();
                request.getCallback().callback(Response.SUCCESS);
            }

            @Override
            public void onDismissCancelled() {
                super.onDismissCancelled();
                request.getCallback().callback(Response.CANCEL);
            }
        });
    }
}
