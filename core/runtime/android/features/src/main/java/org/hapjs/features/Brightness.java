/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.WindowManager;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.BrightnessUtils;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Brightness.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = Brightness.ACTION_GET_VALUE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Brightness.ACTION_SET_VALUE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Brightness.ACTION_GET_MODE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Brightness.ACTION_SET_MODE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Brightness.ACTION_SET_KEEP_SCREEN_ON,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class Brightness extends FeatureExtension {

    protected static final String FEATURE_NAME = "system.brightness";
    protected static final String ACTION_SET_VALUE = "setValue";
    protected static final String ACTION_GET_VALUE = "getValue";
    protected static final String ACTION_GET_MODE = "getMode";
    protected static final String ACTION_SET_MODE = "setMode";
    protected static final String ACTION_SET_KEEP_SCREEN_ON = "setKeepScreenOn";
    protected static final String PARAM_KEY_VALUE = "value";
    protected static final String PARAM_KEY_MODE = "mode";
    protected static final String PARAM_KEY_KEEP_SCREEN_ON = "keepScreenOn";
    protected static final String RESULT_KEY_VALUE = "value";
    protected static final String RESULT_KEY_MODE = "mode";
    private static final int BRIGHTNESS_VALUE_MIN = 0;
    private static final int BRIGHTNESS_VALUE_MAX = 255;
    private static final int BRIGHTNESS_MODE_MANUAL = 0;
    private static final int BRIGHTNESS_MODE_AUTOMATIC = 1;
    private Activity mActivity;

    @Override
    public Response invokeInner(Request request) throws JSONException, SerializeException {
        mActivity = request.getNativeInterface().getActivity();
        String action = request.getAction();
        if (ACTION_SET_VALUE.equals(action)) {
            setValue(request);
        } else if (ACTION_GET_VALUE.equals(action)) {
            getValue(request);
        } else if (ACTION_GET_MODE.equals(action)) {
            getMode(request);
        } else if (ACTION_SET_MODE.equals(action)) {
            setMode(request);
        } else if (ACTION_SET_KEEP_SCREEN_ON.equals(action)) {
            setKeepScreenOn(request);
        }
        return null;
    }

    private void setValue(final Request request) throws JSONException {
        JSONObject params = new JSONObject(request.getRawParams());
        int value = params.getInt(PARAM_KEY_VALUE);
        value = Math.min(BRIGHTNESS_VALUE_MAX, Math.max(BRIGHTNESS_VALUE_MIN, value));

        final float brightness = (float) value / BRIGHTNESS_VALUE_MAX;
        final Activity activity = request.getNativeInterface().getActivity();
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        BrightnessUtils.setWindowBrightness(activity, brightness);
                        request.getCallback().callback(Response.SUCCESS);
                    }
                });
    }

    private void getValue(final Request request) {
        final Activity activity = request.getNativeInterface().getActivity();
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            float brightness = BrightnessUtils.getWindowBrightness(activity);
                            int value;
                            if (brightness >= 0) {
                                value = (int) (brightness * BRIGHTNESS_VALUE_MAX);
                            } else {
                                ContentResolver resolver = activity.getContentResolver();
                                int realValue = Settings.System
                                        .getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
                                value = (realValue * BRIGHTNESS_VALUE_MAX)
                                        / getMaxBrightnessValue();
                            }

                            JSONObject result = new JSONObject();
                            result.put(RESULT_KEY_VALUE, value);
                            request.getCallback().callback(new Response(result));
                        } catch (Exception e) {
                            request.getCallback().callback(getExceptionResponse(request, e));
                        }
                    }
                });
    }

    protected int getMaxBrightnessValue() {
        return BRIGHTNESS_VALUE_MAX;
    }

    private void getMode(final Request request) {
        final Activity activity = request.getNativeInterface().getActivity();
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        float brightness = BrightnessUtils.getWindowBrightness(activity);
                        int value =
                                brightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                                        ? BRIGHTNESS_MODE_AUTOMATIC
                                        : BRIGHTNESS_MODE_MANUAL;
                        try {
                            JSONObject result = new JSONObject();
                            result.put(RESULT_KEY_MODE, value);
                            request.getCallback().callback(new Response(result));
                        } catch (JSONException e) {
                            request.getCallback().callback(getExceptionResponse(request, e));
                        }
                    }
                });
    }

    private void setMode(final Request request) throws JSONException {
        final JSONObject params = new JSONObject(request.getRawParams());
        final int mode = params.getInt(PARAM_KEY_MODE);
        if (mode != BRIGHTNESS_MODE_AUTOMATIC && mode != BRIGHTNESS_MODE_MANUAL) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Unsupported mode"));
            return;
        }
        final Activity activity = request.getNativeInterface().getActivity();
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        float brightness = BrightnessUtils.getWindowBrightness(activity);
                        if (mode == BRIGHTNESS_MODE_AUTOMATIC
                                &&
                                brightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                            BrightnessUtils.resetWindowBrightness(activity);
                        } else if (mode == BRIGHTNESS_MODE_MANUAL
                                &&
                                brightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                            ContentResolver resolver = activity.getContentResolver();
                            try {
                                float sysBrightness =
                                        Settings.System.getInt(resolver,
                                                Settings.System.SCREEN_BRIGHTNESS);
                                BrightnessUtils.setWindowBrightness(activity,
                                        sysBrightness / BRIGHTNESS_VALUE_MAX);
                            } catch (Settings.SettingNotFoundException e) {
                                request.getCallback().callback(getExceptionResponse(request, e));
                            }
                        }
                        request.getCallback().callback(Response.SUCCESS);
                    }
                });
    }

    private void setKeepScreenOn(final Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        if (params == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param"));
            return;
        }
        final boolean keepScreenOn = params.getBoolean(PARAM_KEY_KEEP_SCREEN_ON);

        final Activity activity = request.getNativeInterface().getActivity();
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        BrightnessUtils.setKeepScreenOn(activity, keepScreenOn);
                        request.getCallback().callback(Response.SUCCESS);
                    }
                });
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);

        if (mActivity != null) {
            mActivity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            BrightnessUtils.resetWindowBrightness(mActivity);
                            BrightnessUtils.setKeepScreenOn(mActivity, false);
                        }
                    });
        }
    }
}
