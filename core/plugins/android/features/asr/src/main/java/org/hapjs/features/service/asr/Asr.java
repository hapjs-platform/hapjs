/*
 * Copyright (c) 2023-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.features.service.asr;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;

import org.hapjs.bridge.Callback;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FeatureExtensionAnnotation(
        name = Asr.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Asr.ACTION_GET_PROVIDER, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Asr.ACTION_START, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Asr.EVENT_COMPLETE_RESULT, mode = FeatureExtension.Mode.CALLBACK, type = FeatureExtension.Type.EVENT, alias = Asr.EVENT_COMPLETE_RESULT_ALIAS),
        }
)
public class Asr extends FeatureExtension {
    private static final String TAG = "Asr";
    protected static final String FEATURE_NAME = "service.asr";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    protected static final String ACTION_START = "start";
    protected static final String EVENT_COMPLETE_RESULT = "__oncompleteresult";
    protected static final String EVENT_COMPLETE_RESULT_ALIAS = "oncompleteresult";
    private static final String KEY_RESULT = "result";

    private static final int REQUEST_CODE = 10000;
    private Map<String, Callback> mEventMap = new ConcurrentHashMap<>();

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_PROVIDER.equals(action)) {
            return getProvider();
        } else if (ACTION_START.equals(action)) {
            start(request);
        } else {
            handleEventRequest(request);
        }
        return Response.SUCCESS;
    }

    protected Response getProvider() {
        return new Response("");
    }

    protected Intent createIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        return intent;
    }

    private void start(Request request) {
        Intent intent = createIntent();
        final NativeInterface nativeInterface = request.getNativeInterface();
        Activity activity = nativeInterface.getActivity();
        final LifecycleListener lifecycleListener = new LifecycleListener() {
            @Override
            public void onDestroy() {
                super.onDestroy();
                nativeInterface.removeLifecycleListener(this);
            }

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent intent) {
                super.onActivityResult(requestCode, resultCode, intent);
                if (requestCode == REQUEST_CODE) {
                    nativeInterface.removeLifecycleListener(this);
                    if (intent != null) {
                        ArrayList<String> result = intent.getStringArrayListExtra("android.speech.extra.RESULTS");
                        if (result != null && result.size() > 0) {
                            callback(result.get(0));
                        } else {
                            callback("");
                        }
                    } else {
                        callback("");
                    }
                }
            }

            private void callback(String result) {
                Callback callback = mEventMap.get(EVENT_COMPLETE_RESULT);
                if (callback != null) {
                    try {
                        JSONObject data = new JSONObject();
                        data.put(KEY_RESULT, result);
                        callback.callback(new Response(data));
                    } catch (JSONException e) {
                        Log.e(TAG, "fail to callback", e);
                    }
                }
            }
        };

        try {
            activity.startActivityForResult(intent, REQUEST_CODE);
            nativeInterface.addLifecycleListener(lifecycleListener);
            request.getCallback().callback(Response.SUCCESS);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Speed recognizer not found", e);
            request.getCallback().callback(new Response(Response.CODE_SERVICE_UNAVAILABLE, "Speed recognizer not found"));
        }
    }

    private void handleEventRequest(Request request) {
        String action = request.getAction();
        Callback callback = request.getCallback();
        if (callback.isValid()) {
            mEventMap.put(action, callback);
        } else {
            mEventMap.remove(action);
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public void dispose(boolean force) {
        if (force) {
            mEventMap.clear();
        }
    }
}
