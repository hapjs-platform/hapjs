/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.List;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Wifi.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = Wifi.ACTION_CONNECT,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = Manifest.permission.ACCESS_COARSE_LOCATION),
                @ActionAnnotation(
                        name = Wifi.ACTION_SCAN,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = Manifest.permission.ACCESS_COARSE_LOCATION),
                @ActionAnnotation(
                        name = Wifi.EVENT_ON_SCANNED,
                        alias = Wifi.EVENT_ON_SCANNED_ALIAS,
                        type = FeatureExtension.Type.EVENT,
                        mode = FeatureExtension.Mode.CALLBACK,
                        permissions = Manifest.permission.ACCESS_COARSE_LOCATION),
                @ActionAnnotation(
                        name = Wifi.EVENT_ON_STATE_CHANGED,
                        alias = Wifi.EVENT_ON_STATE_CHANGED_ALIAS,
                        type = FeatureExtension.Type.EVENT,
                        mode = FeatureExtension.Mode.CALLBACK,
                        permissions = Manifest.permission.ACCESS_COARSE_LOCATION),
                @ActionAnnotation(
                        name = Wifi.ACTION_GET_CONNECTED_WIFI,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = Manifest.permission.ACCESS_COARSE_LOCATION)
        })
public class Wifi extends CallbackHybridFeature {

    protected static final String FEATURE_NAME = "system.wifi";
    // function
    protected static final String ACTION_CONNECT = "connect";
    protected static final String ACTION_SCAN = "scan";
    protected static final String ACTION_GET_CONNECTED_WIFI = "getConnectedWifi";
    // event
    protected static final String EVENT_ON_SCANNED_ALIAS = "onscanned";
    protected static final String EVENT_ON_SCANNED = "__onscanned";
    protected static final String EVENT_ON_STATE_CHANGED_ALIAS = "onstatechanged";
    protected static final String EVENT_ON_STATE_CHANGED = "__onstatechanged";
    private static final String TAG = "Wifi";
    // security types
    private static final String WEP = "WEP";
    private static final String WPA = "WPA";
    private static final String WPA2 = "WPA2";
    private static final String WPA_EAP = "WPA-EAP";
    private static final String IEEE8021X = "IEEE8021X";
    private static final String OPEN = "Open";
    private static final int TIME_OUT_DURATION = 15 * 1000;
    private static final int SSID_MAX_LENGTH = 32;
    private static final String SSID_NONE = "<unknown ssid>";
    private static final String BSSID_ANY = "any";
    private static final int CODE_SCANNED = 1;
    private static final int CODE_CONNECTED = 2;
    private static final int CODE_DISCONNECTED = 3;

    private static final String PARAM_SSID = "SSID";
    private static final String PARAM_BSSID = "BSSID";
    private static final String PARAM_PASSWORD = "password";

    private static final String RESULT_WIFI_LIST = "wifiList";
    private static final String RESULT_SSID = "SSID";
    private static final String RESULT_BSSID = "BSSID";
    private static final String RESULT_SECURE = "secure";
    private static final String RESULT_SIGNAL_STRENGTH = "signalStrength";
    private static final String RESULT_STATE = "state";

    private static final int ERROR_CODE_BASE = Response.CODE_FEATURE_ERROR;
    private static final int ERROR_CODE_PASSWD_ERROR = ERROR_CODE_BASE;
    private static final int ERROR_CODE_TIMEOUT = ERROR_CODE_BASE + 1;
    private static final int ERROR_CODE_DUP_REQUEST = ERROR_CODE_BASE + 2;
    private static final int ERROR_CODE_WIFI_NOT_ENABLE = ERROR_CODE_BASE + 3;
    private static final int ERROR_CODE_LOCATION_SERVICE_NOT_ENABLE = ERROR_CODE_BASE + 4;
    private static final int ERROR_CODE_INVALID_SSID = ERROR_CODE_BASE + 5;

    private WifiManager mWifiManager;
    private LocationManager mLocationManager;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        Context appContext = request.getNativeInterface().getActivity().getApplicationContext();
        if (mWifiManager == null) {
            mWifiManager =
                    ((WifiManager) appContext.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE));
        }
        if (mLocationManager == null) {
            mLocationManager =
                    (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        }
        String action = request.getAction();

        switch (action) {
            case ACTION_CONNECT:
                connect(request);
                break;
            case EVENT_ON_STATE_CHANGED:
                onStateChanged(request);
                break;
            case ACTION_SCAN:
                scan(request);
                break;
            case EVENT_ON_SCANNED:
                onScanned(request);
                break;
            case ACTION_GET_CONNECTED_WIFI:
                getConnectedWifi(request);
                break;
            default:
                // do nothing
                break;
        }
        return Response.SUCCESS;
    }

    @SuppressWarnings("MissingPermission")
    private void connect(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid params!"));
            return;
        }
        String ssid = jsonParams.optString(PARAM_SSID);
        String bssid = jsonParams.optString(PARAM_BSSID);
        String password = jsonParams.optString(PARAM_PASSWORD);

        if (TextUtils.isEmpty(ssid)) {
            request
                    .getCallback()
                    .callback(
                            new Response(Response.CODE_ILLEGAL_ARGUMENT, "SSID must not be null!"));
            return;
        }

        if (ssid.length() >= SSID_MAX_LENGTH) {
            request.getCallback()
                    .callback(new Response(ERROR_CODE_INVALID_SSID, "invalid wifi SSID!"));
            return;
        }

        if (TextUtils.isEmpty(bssid)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            "BSSID must not be null!"));
            return;
        }

        if (!mWifiManager.isWifiEnabled()) {
            request.getCallback()
                    .callback(new Response(ERROR_CODE_WIFI_NOT_ENABLE, "wifi not enabled!"));
            return;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getSSID().equals(convertToQuotedString(ssid))) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_CODE_DUP_REQUEST,
                            "duplicate request for same SSID!"));
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        ConnectionBroadcastReceiver receiver = new ConnectionBroadcastReceiver(request);

        String security = "";
        // use security type of latest scan results
        List<ScanResult> latestScanResults = getScanResults();
        if (latestScanResults != null && latestScanResults.size() > 0) {
            security = getSecurityForSsid(latestScanResults, ssid);
        }

        // no scan result, use security type of saved wifi configuration
        if (TextUtils.isEmpty(security)) {
            WifiConfiguration configuredNetwork = getConfiguredNetwork(ssid);
            if (configuredNetwork != null) {
                security = getSecurity(configuredNetwork);
            }
        }

        // the first time this wifi connect
        if (TextUtils.isEmpty(security)) {
            security = TextUtils.isEmpty(password) ? OPEN : WPA2;
        }

        int networkId = -1;
        for (WifiConfiguration configuration : mWifiManager.getConfiguredNetworks()) {
            if (configuration.SSID.equals(convertToQuotedString(ssid))
                    && security.equals(getSecurity(configuration))
                    && (bssid.equals(configuration.BSSID)
                    || BSSID_ANY.equals(configuration.BSSID)
                    || TextUtils.isEmpty(configuration.BSSID))) {
                networkId = configuration.networkId;
                break;
            }
        }

        if (networkId == -1) {
            WifiConfiguration config = createWifiConfiguration(ssid, bssid, password, security);
            if (config == null) {
                request
                        .getCallback()
                        .callback(new Response(Response.CODE_GENERIC_ERROR,
                                "create wifi config error"));
                return;
            }
            networkId = mWifiManager.addNetwork(config);
        }

        if (networkId == -1) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "add wifi config error"));
            return;
        }

        receiver.register(request, intentFilter);
        boolean connectResult = false;
        // connect by reflect is test work perfect before Android P.,but Android P also can not connect
        // wifi by reflect
        // which has connect by Settings or other apps
        if (Build.VERSION.SDK_INT >= 28) {
            connectResult = mWifiManager.enableNetwork(networkId, true);
        } else {
            connectResult = connectByReflect(networkId, mWifiManager);
        }
        if (!connectResult) {
            receiver.unregister(request);
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "connect wifi error"));
        }
    }

    private boolean connectByReflect(int networkId, WifiManager wifiManager) {
        if (networkId == -1 || wifiManager == null) {
            return false;
        }
        try {
            Method connect =
                    wifiManager
                            .getClass()
                            .getDeclaredMethod(
                                    "connect",
                                    int.class,
                                    Class.forName("android.net.wifi.WifiManager$ActionListener"));
            connect.setAccessible(true);
            connect.invoke(wifiManager, networkId, null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "connectByReflect failed", e);
        }
        return false;
    }

    private void scan(Request request) {
        if (!mWifiManager.isWifiEnabled()) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_CODE_WIFI_NOT_ENABLE, "wifi is not enabled!"));
            return;
        }
        if (!isLocationEnabled(request)) {
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    ERROR_CODE_LOCATION_SERVICE_NOT_ENABLE,
                                    "location service is not enabled!"));
            return;
        }
        boolean result = mWifiManager.startScan();
        request.getCallback().callback(result ? Response.SUCCESS : Response.ERROR);
    }

    private void onScanned(Request request) {
        if (request.getCallback().isValid()) {
            ScanResultCallbackContext callbackContext =
                    new ScanResultCallbackContext(request, false);
            putCallbackContext(callbackContext);
        } else {
            removeCallbackContext(request.getAction());
        }
    }

    private void onStateChanged(Request request) {
        if (request.getCallback().isValid()) {
            WifiStateChangedCallbackContext callbackContext =
                    new WifiStateChangedCallbackContext(request, false);
            putCallbackContext(callbackContext);
        } else {
            removeCallbackContext(request.getAction());
        }
    }

    private void getConnectedWifi(Request request) {
        if (!mWifiManager.isWifiEnabled()) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_CODE_WIFI_NOT_ENABLE, "wifi is not enabled!"));
            return;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null || SSID_NONE.equals(wifiInfo.getSSID())) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "current wifi is null!"));
            return;
        }
        request.getCallback().callback(new Response(makeResult(wifiInfo)));
    }

    private WifiConfiguration createWifiConfiguration(
            String ssid, String bssid, String password, String security) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = convertToQuotedString(ssid);
        config.BSSID = bssid;

        switch (security) {
            case WEP:
                if (!TextUtils.isEmpty(password)) {
                    config.wepKeys[0] = convertToQuotedString(password);
                }
                config.wepTxKeyIndex = 0;

                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                break;
            case WPA:
            case WPA2:
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedProtocols.set(
                        WPA2.equals(security)
                                ? WifiConfiguration.Protocol.RSN
                                : WifiConfiguration.Protocol.WPA);

                if (!TextUtils.isEmpty(password)) {
                    config.preSharedKey = convertToQuotedString(password);
                }
                break;
            case OPEN:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case WPA_EAP:
            case IEEE8021X:
                // not support
                return null;
            default:
                break;
        }
        config.status = WifiConfiguration.Status.ENABLED;
        return config;
    }

    private String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if (string.charAt(0) == '"' && string.charAt(lastPos) == '"') {
            return string;
        }

        return "\"" + string + "\"";
    }

    private String convertToDequotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if (string.charAt(0) == '"' && string.charAt(lastPos) == '"' && string.length() > 1) {
            return string.substring(1, lastPos);
        }

        return string;
    }

    @SuppressWarnings("MissingPermission")
    private WifiConfiguration getConfiguredNetwork(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs == null) {
            return null;
        }
        for (WifiConfiguration config : configs) {
            if (TextUtils.equals(config.SSID, convertToQuotedString(ssid))) {
                return config;
            }
        }
        return null;
    }

    private List<ScanResult> getScanResults() {
        try {
            return mWifiManager.getScanResults();
        } catch (SecurityException e) {
            Log.e(TAG, "getScanResults: ", e);
        }
        return null;
    }

    protected boolean isLocationEnabled(Request request) {
        boolean isProviderEnabled =
                (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        int locationMode = 0;
        try {
            locationMode =
                    Settings.Secure.getInt(
                            request.getNativeInterface().getActivity().getContentResolver(),
                            Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "isLocationEnabled: ", e);
            return false;
        }
        return isProviderEnabled && (locationMode != Settings.Secure.LOCATION_MODE_OFF);
    }

    private JSONObject makeResult(ScanResult scanResult) {
        return makeResult(
                scanResult.SSID, scanResult.BSSID, hasSecurity(getSecurity(scanResult)),
                scanResult.level);
    }

    private JSONObject makeResult(WifiInfo wifiInfo) {
        String security = OPEN;
        WifiConfiguration config = getConfiguredNetwork(wifiInfo.getSSID());
        if (config != null) {
            security = getSecurity(config);
        }
        return makeResult(
                convertToDequotedString(wifiInfo.getSSID()) /* WifiInfo.getSSID() is quoted*/,
                wifiInfo.getBSSID(),
                hasSecurity(security),
                wifiInfo.getRssi());
    }

    private JSONObject makeResult(
            String ssid, String bssid, boolean hasSecurity, int signalStrength) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_SSID, ssid);
            result.put(RESULT_BSSID, bssid);
            result.put(RESULT_SECURE, hasSecurity);
            result.put(RESULT_SIGNAL_STRENGTH, signalStrength);
        } catch (JSONException e) {
            Log.e(TAG, "makeResult: ", e);
        }
        return result;
    }

    private Response makeResponse(List<ScanResult> scanResults) {
        JSONObject result = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (ScanResult scanResult : scanResults) {
            JSONObject object = makeResult(scanResult);
            jsonArray.put(object);
        }
        try {
            result.put(RESULT_WIFI_LIST, jsonArray);
        } catch (JSONException e) {
            Log.e(TAG, "makeResponse: ", e);
        }
        return new Response(result);
    }

    private boolean hasSecurity(String security) {
        return !security.contains(OPEN);
    }

    private String getSecurityForSsid(List<ScanResult> scanResults, String ssid) {
        String security = "";
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(ssid)) {
                security = getSecurity(scanResult);
            }
        }
        return security;
    }

    private String getSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = {WEP, WPA, WPA2, WPA_EAP, IEEE8021X};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        return OPEN;
    }

    private String getSecurity(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            if (!wifiConfig.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.CCMP)
                    && (wifiConfig.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40)
                    || wifiConfig.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104))) {
                return WEP;
            } else {
                return OPEN;
            }
        } else if (wifiConfig.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
            return WPA_EAP;
        } else if (wifiConfig.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return IEEE8021X;
        } else if (wifiConfig.allowedProtocols.get(WifiConfiguration.Protocol.RSN)) {
            return WPA2;
        } else if (wifiConfig.allowedProtocols.get(WifiConfiguration.Protocol.WPA)) {
            return WPA;
        } else {
            Log.w(TAG, "Unknown security type from WifiConfiguration, falling back on open.");
            return OPEN;
        }
    }

    /**
     * used for {@link #EVENT_ON_SCANNED} to receive wifi scan result
     */
    private class ScanResultBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = getScanResults();
            if (scanResults != null) {
                runCallbackContext(EVENT_ON_SCANNED, CODE_SCANNED, makeResponse(scanResults));
            }
        }
    }

    private class ScanResultCallbackContext extends CallbackContext {
        private ScanResultBroadcastReceiver mReceiver;

        public ScanResultCallbackContext(Request request, boolean reserved) {
            super(Wifi.this, EVENT_ON_SCANNED, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            mReceiver = new ScanResultBroadcastReceiver();
            Context applicationContext =
                    mRequest.getNativeInterface().getActivity().getApplicationContext();
            applicationContext.registerReceiver(
                    mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mRequest
                    .getNativeInterface()
                    .getActivity()
                    .getApplicationContext()
                    .unregisterReceiver(mReceiver);
        }

        @Override
        public void callback(int what, Object obj) {
            if (what == CODE_SCANNED) {
                getRequest().getCallback().callback(((Response) obj));
            }
        }
    }

    /**
     * used for {@link #EVENT_ON_STATE_CHANGED} to listen any wifi connected
     */
    private class WifiStateChangedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (ConnectivityManager.TYPE_WIFI == info.getType()) {
                try {
                    if (NetworkInfo.State.CONNECTED == info.getState()) {
                        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                        if (!SSID_NONE.equals(wifiInfo.getSSID())) {
                            JSONObject result = makeResult(wifiInfo);
                            result.put(RESULT_STATE, 1);
                            runCallbackContext(EVENT_ON_STATE_CHANGED, CODE_CONNECTED,
                                    new Response(result));
                        }
                    } else if (NetworkInfo.State.DISCONNECTED == info.getState()) {
                        JSONObject result = new JSONObject();
                        result.put(RESULT_STATE, 0);
                        runCallbackContext(EVENT_ON_STATE_CHANGED, CODE_DISCONNECTED,
                                new Response(result));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "WifiStateChangedBroadcastReceiver: ", e);
                }
            }
        }
    }

    private class WifiStateChangedCallbackContext extends CallbackContext {
        private WifiStateChangedBroadcastReceiver mReceiver;

        public WifiStateChangedCallbackContext(Request request, boolean reserved) {
            super(Wifi.this, EVENT_ON_STATE_CHANGED, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            mReceiver = new WifiStateChangedBroadcastReceiver();
            Context applicationContext =
                    mRequest.getNativeInterface().getActivity().getApplicationContext();
            applicationContext.registerReceiver(
                    mReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mRequest
                    .getNativeInterface()
                    .getActivity()
                    .getApplicationContext()
                    .unregisterReceiver(mReceiver);
        }

        @Override
        public void callback(int what, Object obj) {
            if (what == CODE_CONNECTED || what == CODE_DISCONNECTED) {
                getRequest().getCallback().callback(((Response) obj));
            }
        }
    }

    /**
     * used for {@link #ACTION_CONNECT} to detect a specific wifi is connected or not
     */
    private class ConnectionBroadcastReceiver extends BroadcastReceiver {

        private Request mRequest;
        private boolean isTimeOut = false;
        private boolean isRegister = false;
        private Runnable mTimeoutCallback =
                new Runnable() {

                    @Override
                    public void run() {
                        isTimeOut = true;
                        mRequest
                                .getNativeInterface()
                                .getActivity()
                                .getApplicationContext()
                                .unregisterReceiver(ConnectionBroadcastReceiver.this);
                        mRequest
                                .getCallback()
                                .callback(new Response(ERROR_CODE_TIMEOUT, "connection timeout!"));
                    }
                };

        public ConnectionBroadcastReceiver(Request request) {
            mRequest = request;
            mRequest
                    .getNativeInterface()
                    .addLifecycleListener(
                            new LifecycleListener() {
                                @Override
                                public void onDestroy() {
                                    if (!isTimeOut) {
                                        unregister(mRequest);
                                    }
                                }
                            });
        }

        public void register(Request request, IntentFilter intentFilter) {
            request
                    .getNativeInterface()
                    .getActivity()
                    .getApplicationContext()
                    .registerReceiver(this, intentFilter);
            isRegister = true;
            mHandler.postDelayed(mTimeoutCallback, TIME_OUT_DURATION);
        }

        public void unregister(Request request) {
            if (isRegister) {
                isRegister = false;
                request.getNativeInterface().getActivity().getApplicationContext()
                        .unregisterReceiver(this);
            }
            mHandler.removeCallbacks(mTimeoutCallback);
        }

        private void handlePasswordError(int error) {
            // detect password error
            if (error == WifiManager.ERROR_AUTHENTICATING) {
                unregister(mRequest);
                mRequest
                        .getCallback()
                        .callback(
                                new Response(ERROR_CODE_PASSWD_ERROR, "wifi password incorrect!"));
            }
        }

        private void handleConnectionSuccess() {
            if (!mWifiManager.isWifiEnabled()) {
                mRequest
                        .getCallback()
                        .callback(new Response(ERROR_CODE_WIFI_NOT_ENABLE, "wifi is not enabled!"));
                return;
            }
            WifiInfo connectedWifiInfo = mWifiManager.getConnectionInfo();
            if (connectedWifiInfo == null
                    || SSID_NONE.equals(connectedWifiInfo.getSSID())
                    || connectedWifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
                return;
            }
            try {
                JSONObject params = mRequest.getJSONParams();
                String ssid = params.getString(PARAM_SSID);
                // compare connected wifi and target wifi
                if (TextUtils.equals(convertToQuotedString(ssid), connectedWifiInfo.getSSID())) {
                    unregister(mRequest);
                    mRequest.getCallback().callback(Response.SUCCESS);
                }
            } catch (JSONException e) {
                Log.e(TAG, "handleConnectionSuccess: ", e);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                handlePasswordError(intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0));
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                handleConnectionSuccess();
            }
        }
    }
}
