/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ReflectUtils;
import org.hapjs.common.utils.SystemPropertiesUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implement FeatureExtension for getting NetWork status
 *
 * <table>
 * <tr>
 * <th>Action name</th> <th>Action description</th> <th>Invocation mode</th>
 * <th>Request param name</th> <th>Request param description</th>
 * <th>Response param name</th> <th>Response param description</th>
 * </tr>
 * <tr>
 * <td>getType</td> <td>check whether currently active data network is metered</td> <td>SYNC</td>
 * <td>N/A</td> <td>N/A</td>
 * <td>metered</td> <td>boolean represents currently active data network is metered</td>
 * </tr>
 * <tr>
 * <td>enableNotification</td> <td>register a notification for network connection changing</td> <td>CALLBACK</td>
 * <td>N/A</td> <td>N/A</td>
 * <td>connected</td> <td>boolean represents currently network is connected</td>
 * </tr>
 *
 * <tr>
 * <td>disableNotification</td> <td>unregister network notification</td> <td>SYNC</td>
 * <td>N/A</td> <td>N/A</td>
 * <td>N/A</td> <td>CODE_CANCEL=100 represent success to unregister it</td>
 * </tr>
 * </table>
 */
@FeatureExtensionAnnotation(
        name = Network.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = Network.ACTION_GET_TYPE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Network.ACTION_SUBSCRIBE, mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(name = Network.ACTION_UNSUBSCRIBE, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Network.ACTION_GET_SIM_OPERATORS, mode = FeatureExtension.Mode.ASYNC)
        }
)
public class Network extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "system.network";
    protected static final String ACTION_GET_TYPE = "getType";
    protected static final String ACTION_SUBSCRIBE = "subscribe";
    protected static final String ACTION_UNSUBSCRIBE = "unsubscribe";
    protected static final String ACTION_GET_SIM_OPERATORS = "getSimOperators";
    protected static final String KEY_METERED = "metered";
    protected static final String KEY_TYPE = "type";
    protected static final String KEY_SIGNAL_STRENGTH = "signalStrength";
    protected static final String KEY_OPERATORS = "operators";
    protected static final String KEY_SIM_SIZE = "size";
    protected static final String KEY_SLOT_INDEX = "slotIndex";
    protected static final String KEY_OPERATOR = "operator";
    protected static final String KEY_IS_DEFAULT_DATA_OPERATOR = "isDefaultDataOperator";
    protected static final String PROPERTY_OPERATORS = "gsm.sim.operator.numeric";
    protected static final int TYPE_NONE = 0;
    protected static final int TYPE_2G = 1;
    protected static final int TYPE_3G = 2;
    protected static final int TYPE_4G = 3;
    protected static final int TYPE_5G = 4;
    protected static final int TYPE_WIFI = 5;
    protected static final int TYPE_BLUETOOTH = 6;
    protected static final int TYPE_OTHERS = 7;
    protected static final String[] TYPE_TEXTS =
            new String[] {"none", "2g", "3g", "4g", "5g", "wifi", "bluetooth", "others"};
    private static final String TAG = "Network";
    private static final int NR_STATE_NONE = 0;
    private static final int NR_STATE_RESTRICTED = 1;
    private static final int NR_STATE_NOT_RESTRICTED = 2;
    private static final int NR_STATE_CONNECTED = 3;
    private static final String METHOD_GET_NR_STATE = "getNrState";
    private static final String METHOD_GET_DBM = "getDbm";
    private static final int CODE_NO_SIM_ERROR = Response.CODE_FEATURE_ERROR + 1;
    private static final int CODE_TYPE_ERROR = Response.CODE_FEATURE_ERROR + 2;
    protected static final int ERROR_SIGNAL_STRENGTH = -1000;
    protected static final int ERROR_SIGNAL_STRENGTH_WITHOUT_INIT = -1001;
    protected static final int ERROR_SIGNAL_STRENGTH_OTHER_NETWORK = -1002;
    protected static final int ERROR_SIGNAL_STRENGTH_NO_CELL_INFO = -1003;

    private IntentFilter mFilter;

    public Network() {
        mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_TYPE.equals(action)) {
            return getNetworkType(request);
        } else if (ACTION_GET_SIM_OPERATORS.equals(action)) {
            return getSimOperators(request);
        } else if (ACTION_SUBSCRIBE.equals(action)) {
            return subscribe(request);
        } else {
            return unsubscribe(request);
        }
    }

    private Response getNetworkType(Request request) throws JSONException {
        Response response = doGetNetworkType(request.getNativeInterface().getActivity());
        request.getCallback().callback(response);
        return Response.SUCCESS;
    }

    protected Response doGetNetworkType(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                return makeResponse(false, TYPE_NONE, ERROR_SIGNAL_STRENGTH);
            }
            boolean metered = connectivityManager.isActiveNetworkMetered();
            int networkType = networkInfo.getType();
            if (networkType == ConnectivityManager.TYPE_MOBILE) {
                int mobileType = getMobileType(context, networkInfo);
                int mobileSignalStrength = getMobileSignalStrength(context);
                return makeResponse(metered, mobileType, mobileSignalStrength);
            } else if (networkType == ConnectivityManager.TYPE_WIFI) {
                int wifiSignalStrength = getWifiSignalStrength(context);
                return makeResponse(metered, TYPE_WIFI, wifiSignalStrength);
            } else if (networkType == ConnectivityManager.TYPE_BLUETOOTH) {
                return makeResponse(metered, TYPE_BLUETOOTH, ERROR_SIGNAL_STRENGTH_OTHER_NETWORK);
            } else {
                Log.e(TAG, "Unknown network type: " + networkType);
                return makeResponse(metered, TYPE_OTHERS, ERROR_SIGNAL_STRENGTH_OTHER_NETWORK);
            }
        } catch (SecurityException e) {
            return getExceptionResponse(ACTION_GET_TYPE, e, Response.CODE_GENERIC_ERROR);
        } catch (JSONException e) {
            return getExceptionResponse(ACTION_GET_TYPE, e, Response.CODE_GENERIC_ERROR);
        }
    }

    private int getMobileSignalStrength(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                SignalStrength signalStrength = telephonyManager.getSignalStrength();
                return getMobileSignalStrengthFromSignalStrength(signalStrength);
            }
            else {
                return getMobileSignalStrengthFromCellInfo(context, telephonyManager);
            }
        }
        return ERROR_SIGNAL_STRENGTH;
    }

    private int getMobileSignalStrengthFromCellInfo(Context context, TelephonyManager telephonyManager) {
        int signalStrength = ERROR_SIGNAL_STRENGTH;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "return default signal strength because no permission.");
            return ERROR_SIGNAL_STRENGTH_WITHOUT_INIT;
        }
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        if (cellInfoList != null && cellInfoList.size() > 0) {
            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo.isRegistered()) {
                    if (cellInfo instanceof CellInfoGsm) {
                        CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
                        signalStrength = cellSignalStrengthGsm.getDbm();
                    } else if (cellInfo instanceof CellInfoCdma) {
                        CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma) cellInfo).getCellSignalStrength();
                        signalStrength = cellSignalStrengthCdma.getDbm();
                    } else if (cellInfo instanceof CellInfoLte) {
                        CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
                        signalStrength = cellSignalStrengthLte.getDbm();
                    } else if (cellInfo instanceof CellInfoWcdma) {
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
                        signalStrength = cellSignalStrengthWcdma.getDbm();
                    }
                    break;
                }
            }
        }else {
            Log.e(TAG, "return default signal strength because cellInfoList is empty.");
            signalStrength = ERROR_SIGNAL_STRENGTH_NO_CELL_INFO;
        }
        return signalStrength;
    }

    private int getMobileSignalStrengthFromSignalStrength(SignalStrength signalStrength) {
        try {
            Object value = ReflectUtils.invokeDeclaredMethod(
                    SignalStrength.class.getName(), signalStrength, METHOD_GET_DBM, new Class[]{}, new Object[]{});
            if (value != null){
                return (int)value;
            }
        }catch (Exception e){
            Log.e(TAG,"return default signal strength because reflection getDbm failed.",e);
        }
        return ERROR_SIGNAL_STRENGTH;
    }

    private int getWifiSignalStrength(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return wifiInfo.getRssi();
        }
        return ERROR_SIGNAL_STRENGTH;
    }

    protected Response doSubsrcibeResponse(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                return makeResponse(false, TYPE_NONE);
            }
            boolean metered = connectivityManager.isActiveNetworkMetered();
            int networkType = networkInfo.getType();
            if (networkType == ConnectivityManager.TYPE_MOBILE) {
                int mobileType = getMobileType(context, networkInfo);
                return makeResponse(metered, mobileType);
            } else if (networkType == ConnectivityManager.TYPE_WIFI) {
                return makeResponse(metered, TYPE_WIFI);
            } else if (networkType == ConnectivityManager.TYPE_BLUETOOTH) {
                return makeResponse(metered, TYPE_BLUETOOTH);
            } else {
                Log.e(TAG, "Unknown network type: " + networkType);
                return makeResponse(metered, TYPE_OTHERS);
            }
        } catch (SecurityException e) {
            return getExceptionResponse(ACTION_GET_TYPE, e, Response.CODE_GENERIC_ERROR);
        } catch (JSONException e) {
            return getExceptionResponse(ACTION_GET_TYPE, e, Response.CODE_GENERIC_ERROR);
        }
    }

    private Response makeResponse(boolean metered, int type, int signalStrength) throws JSONException {
        JSONObject result = new JSONObject();
        result.put(KEY_METERED, metered);
        result.put(KEY_TYPE, TYPE_TEXTS[type]);
        result.put(KEY_SIGNAL_STRENGTH, signalStrength);
        return new Response(result);
    }

    private Response makeResponse(boolean metered, int type) throws JSONException {
        JSONObject result = new JSONObject();
        result.put(KEY_METERED, metered);
        result.put(KEY_TYPE, TYPE_TEXTS[type]);
        return new Response(result);
    }

    protected int getMobileType(Context context, NetworkInfo networkInfo) {
        if (is5GOnNSA(context)) {
            return TYPE_5G;
        }
        switch (networkInfo.getSubtype()) {
            // 如果是2g类型
            case TelephonyManager.NETWORK_TYPE_GPRS: // 联通2g
            case TelephonyManager.NETWORK_TYPE_CDMA: // 电信2g
            case TelephonyManager.NETWORK_TYPE_EDGE: // 移动2g
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return TYPE_2G;
            // 如果是3g类型
            case TelephonyManager.NETWORK_TYPE_EVDO_A: // 电信3g
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return TYPE_3G;
            // 如果是4g类型
            case TelephonyManager.NETWORK_TYPE_LTE:
                return TYPE_4G;
            // 5g类型：默认Android 10.0才支持5G，如果有9.0的5G项目，各厂商需要各自适配
            case TelephonyManager.NETWORK_TYPE_NR:
                return TYPE_5G;
            default:
                String subTypeName = networkInfo.getSubtypeName();
                // 中国移动 联通 电信 三种3G制式
                if ("TD-SCDMA".equalsIgnoreCase(subTypeName)
                        || "WCDMA".equalsIgnoreCase(subTypeName)
                        || "CDMA2000".equalsIgnoreCase(subTypeName)) {
                    return TYPE_3G;
                } else {
                    Log.e(
                            TAG,
                            "Unknown network type"
                                    + ", subType: "
                                    + networkInfo.getSubtype()
                                    + ", subTypeName: "
                                    + subTypeName);
                    return TYPE_2G;
                }
        }
    }

    protected boolean is5GOnNSA(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                TelephonyManager telephonyManager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                ServiceState serviceState = telephonyManager.getServiceState();
                Method method = ServiceState.class.getDeclaredMethod(METHOD_GET_NR_STATE);
                method.setAccessible(true);
                int nrState = (int) method.invoke(serviceState);
                if (NR_STATE_CONNECTED == nrState) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "No such method.", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Illegal access.", e);
            } catch (InvocationTargetException e) {
                Log.e(TAG, "Invocation target exception.", e);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check 5G on NSA.", e);
            }
        }
        return false;
    }

    protected Response getSimOperators(Request request) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        if (!hasSim(context)) {
            request.getCallback().callback(new Response(CODE_NO_SIM_ERROR, "No sim card."));
            return Response.SUCCESS;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String operatorProterties = SystemPropertiesUtils.get(PROPERTY_OPERATORS);
        if (!TextUtils.isEmpty(operatorProterties)) {
            String[] operators = operatorProterties.split(",");
            JSONArray jsonArray = new JSONArray();
            String simOperator = telephonyManager.getSimOperator();
            for (int i = 0; i < operators.length; i++) {
                String operator = operators[i];
                if (!TextUtils.isEmpty(operator)) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KEY_SLOT_INDEX, i);
                    jsonObject.put(KEY_OPERATOR, operator);
                    jsonObject.put(KEY_IS_DEFAULT_DATA_OPERATOR, operator.equals(simOperator));
                    jsonArray.put(jsonObject);
                }
            }
            JSONObject result = new JSONObject();
            result.put(KEY_OPERATORS, jsonArray);
            result.put(KEY_SIM_SIZE, jsonArray.length());
            request.getCallback().callback(new Response(result));
        } else {
            request.getCallback().callback(new Response(CODE_TYPE_ERROR, "Get operation failed."));
        }
        return Response.SUCCESS;
    }

    protected boolean hasSim(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return tm.getSimState() == TelephonyManager.SIM_STATE_READY;
        } else {
            String operator = tm.getSimOperator();
            return !TextUtils.isEmpty(operator);
        }
    }

    private Response subscribe(Request request) {
        SubscribeCallbackContext subscribeCallbackContext =
                new SubscribeCallbackContext(request, isReserved(request));
        putCallbackContext(subscribeCallbackContext);
        return Response.SUCCESS;
    }

    private Response unsubscribe(Request request) {
        removeCallbackContext(ACTION_SUBSCRIBE);
        return Response.SUCCESS;
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                Executors.io().execute(() -> runCallbackContext(ACTION_SUBSCRIBE, 0, null));
            }
        }
    }

    private class SubscribeCallbackContext extends CallbackContext {
        NetworkStateReceiver receiver;

        public SubscribeCallbackContext(Request request, boolean reserved) {
            super(Network.this, ACTION_SUBSCRIBE, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            receiver = new NetworkStateReceiver();
            Context applicationContext =
                    mRequest.getNativeInterface().getActivity().getApplicationContext();
            applicationContext.registerReceiver(receiver, mFilter);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            NativeInterface nativeInterface = mRequest.getNativeInterface();
            Context applicationContext = nativeInterface.getActivity().getApplicationContext();
            applicationContext.unregisterReceiver(receiver);
        }

        @Override
        public void callback(int what, Object obj) {
            Response response = doSubsrcibeResponse(mRequest.getNativeInterface().getActivity());
            mRequest.getCallback().callback(response);
        }
    }
}
