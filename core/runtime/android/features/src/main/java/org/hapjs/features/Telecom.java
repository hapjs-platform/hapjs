/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.ReflectUtils;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Telecom.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = Telecom.ACTION_GET_TELECOM_INFO, mode = FeatureExtension.Mode.ASYNC),
        })
public class Telecom extends FeatureExtension {

    protected static final String FEATURE_NAME = "system.telecom";
    protected static final String ACTION_GET_TELECOM_INFO = "getTelecomInfo";
    protected static final String PARAMS_IS_5G_DEVICE = "is5GDevice";
    protected static final String PARAMS_IS_5G_SWITCH_OPENED = "is5GSwitchOpened";
    protected static final String METHOD_PREFERRED_NETWORK_TYPE = "getPreferredNetworkType";
    /**
     * TelephonyManager$PrefNetworkMode: NR 5G only mode
     */
    protected static final int NETWORK_MODE_NR_ONLY = 23;
    /**
     * TelephonyManager$PrefNetworkMode: NR 5G, LTE, TD-SCDMA, CDMA, EVDO, GSM and WCDMA
     */
    protected static final int NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 33;
    private static final String TAG = "Telecom";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (TextUtils.equals(ACTION_GET_TELECOM_INFO, action)) {
            getTelecomInfo(request);
        }
        return null;
    }

    protected void getTelecomInfo(Request request) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(PARAMS_IS_5G_DEVICE, is5GDevice(request));
        jsonObject.put(PARAMS_IS_5G_SWITCH_OPENED, is5GSwitchOpened(request));

        Response response = new Response(jsonObject);
        request.getCallback().callback(response);
    }

    protected boolean is5GDevice(Request request) {
        // TODO 各厂商各自实现
        return false;
    }

    protected boolean is5GSwitchOpened(Request request) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            return is5GSwitchOpenedOnP(request);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Context context = request.getNativeInterface().getActivity();
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            Object obj =
                    ReflectUtils.invokeMethod(
                            TelephonyManager.class.getName(),
                            tm,
                            METHOD_PREFERRED_NETWORK_TYPE,
                            new Class[] {int.class},
                            new Object[] {defaultDataSubId});

            if (null != obj) {
                int preferredNetworkType = (int) obj;
                if (preferredNetworkType >= NETWORK_MODE_NR_ONLY
                        &&
                        preferredNetworkType <= NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA) {
                    return true;
                } else {
                    Log.w(TAG, "preferredNetworkType: " + preferredNetworkType);
                }
            } else {
                Log.w(TAG, "null of reflect");
            }
        }
        return false;
    }

    protected boolean is5GSwitchOpenedOnP(Request request) {
        // TODO Android 9.0 需各厂商各自实现
        return false;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
