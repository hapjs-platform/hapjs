/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Battery.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = Battery.ACTION_GET_STATUS, mode = FeatureExtension.Mode.ASYNC)
        })
public class Battery extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.battery";
    protected static final String ACTION_GET_STATUS = "getStatus";

    protected static final String RESULT_KEY_CHARGING = "charging";
    protected static final String RESULT_KEY_LEVEL = "level";

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_STATUS.equals(action)) {
            getStatus(request);
        }
        return null;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private void getStatus(Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        Intent batteryInfoIntent =
                activity
                        .getApplicationContext()
                        .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryInfoIntent != null) {
            int status = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int level = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
            float batteryPct = (float) level / scale;

            JSONObject info = new JSONObject();
            info.put(RESULT_KEY_CHARGING, isCharging);
            info.put(RESULT_KEY_LEVEL, batteryPct);
            request.getCallback().callback(new Response(info));
        } else {
            request.getCallback().callback(Response.ERROR);
        }
    }
}
