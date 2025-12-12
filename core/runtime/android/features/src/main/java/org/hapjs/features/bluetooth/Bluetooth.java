/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;

import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.utils.ArrayBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;

import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.bluetooth.callback.BleAdapterChangeCallback;
import org.hapjs.features.bluetooth.callback.BleConnectStateCallback;
import org.hapjs.features.bluetooth.callback.BleDiscoveryServiceCallback;
import org.hapjs.features.bluetooth.callback.BleNotificationCallback;
import org.hapjs.features.bluetooth.callback.BleOperationCallback;
import org.hapjs.features.bluetooth.data.BleConst;
import org.hapjs.features.bluetooth.data.ScanOperateResult;
import org.hapjs.features.bluetooth.utils.BleAdParser;
import org.hapjs.features.bluetooth.utils.PropertyUtils;
import org.hapjs.render.jsruntime.serialize.JavaSerializeArray;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Bluetooth.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Bluetooth.ACTION_OPEN_ADAPTER, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Bluetooth.ACTION_CLOSE_ADAPTER, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_START_DEVICES_DISCOVERY,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.ACCESS_FINE_LOCATION}),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_STOP_DEVICES_DISCOVERY,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Bluetooth.ACTION_GET_DEVICES, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_GET_ADAPTER_STATE,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_CREATE_BLE_CONNECTION,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_CLOSE_BLE_CONNECTION,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_READ_BLE_CHARACTERISTIC,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_WRITE_BLE_CHARACTERISTIC,
                        mode = FeatureExtension.Mode.ASYNC,
                        normalize = FeatureExtension.Normalize.RAW),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_NOTIFY_BLE_CHARACTERISTIC,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_GET_BLE_SERVICES,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_GET_BLE_CHARACTERISTICS,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.ACTION_GET_CONNECTED_DEVICES,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Bluetooth.EVENT_ON_DEVICE_FOUND,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Bluetooth.EVENT_ON_DEVICE_FOUND_ALIAS),
                @ActionAnnotation(
                        name = Bluetooth.EVENT_ON_CHARACTERISTIC_VALUE_CHANGE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Bluetooth.EVENT_ON_CHARACTERISTIC_VALUE_CHANGE_ALIAS),
                @ActionAnnotation(
                        name = Bluetooth.EVENT_ON_ADPTER_STATE_CHANGE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Bluetooth.EVENT_ON_ADPTER_STATE_CHANGE_ALIAS),
                @ActionAnnotation(
                        name = Bluetooth.EVENT_ON_CONNECTION_STATE_CHANGE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Bluetooth.EVENT_ON_CONNECTION_STATE_CHANGE_ALIAS),
        })
public class Bluetooth extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "system.bluetooth";
    protected static final String ACTION_OPEN_ADAPTER = "openAdapter";
    protected static final String ACTION_CLOSE_ADAPTER = "closeAdapter";
    protected static final String ACTION_GET_ADAPTER_STATE = "getAdapterState";
    protected static final String ACTION_START_DEVICES_DISCOVERY = "startDevicesDiscovery";
    protected static final String ACTION_STOP_DEVICES_DISCOVERY = "stopDevicesDiscovery";
    protected static final String ACTION_GET_DEVICES = "getDevices";
    protected static final String ACTION_GET_CONNECTED_DEVICES = "getConnectedDevices";
    protected static final String ACTION_CREATE_BLE_CONNECTION = "createBLEConnection";
    protected static final String ACTION_CLOSE_BLE_CONNECTION = "closeBLEConnection";
    protected static final String ACTION_GET_BLE_SERVICES = "getBLEDeviceServices";
    protected static final String ACTION_GET_BLE_CHARACTERISTICS = "getBLEDeviceCharacteristics";
    protected static final String ACTION_READ_BLE_CHARACTERISTIC = "readBLECharacteristicValue";
    protected static final String ACTION_WRITE_BLE_CHARACTERISTIC = "writeBLECharacteristicValue";
    protected static final String ACTION_NOTIFY_BLE_CHARACTERISTIC =
            "notifyBLECharacteristicValueChange";
    protected static final String EVENT_ON_DEVICE_FOUND = "__ondevicefound";
    protected static final String EVENT_ON_DEVICE_FOUND_ALIAS = "ondevicefound";
    protected static final String EVENT_ON_CHARACTERISTIC_VALUE_CHANGE =
            "__onblecharacteristicvaluechange";
    protected static final String EVENT_ON_CHARACTERISTIC_VALUE_CHANGE_ALIAS =
            "onblecharacteristicvaluechange";
    protected static final String EVENT_ON_ADPTER_STATE_CHANGE = "__onadapterstatechange";
    protected static final String EVENT_ON_ADPTER_STATE_CHANGE_ALIAS = "onadapterstatechange";
    protected static final String EVENT_ON_CONNECTION_STATE_CHANGE = "__onbleconnectionstatechange";
    protected static final String EVENT_ON_CONNECTION_STATE_CHANGE_ALIAS =
            "onbleconnectionstatechange";
    protected static final String PARAM_OPERATE_ADAPTER = "operateAdapter";
    protected static final String PARAM_DEVICE_ID = "deviceId";
    protected static final String PARAM_SERVICE_UUID = "serviceId";
    protected static final String PARAM_CHARACTERISTIC_UUID = "characteristicId";
    protected static final String PARAM_VALUE = "value";
    protected static final String PARAM_SERVICE_UUIDS = "services";
    protected static final String PARAM_INTERVAL = "interval";
    protected static final String PARAM_DUPLICATE = "allowDuplicatesKey";
    protected static final String PARAM_NOTIFY_STATE = "state";
    protected static final String PARAM_TIMEOUT = "timeout";
    protected static final String RESULT_DEVICES = "devices";
    protected static final String RESULT_NAME = "name";
    protected static final String RESULT_DEVICE_ID = "deviceId";
    protected static final String RESULT_SERVICE_UUID = "serviceId";
    protected static final String RESULT_CHARACTERISTIC_UUID = "characteristicId";
    protected static final String RESULT_RSSI = "RSSI";
    protected static final String RESULT_ADVERTIS_DATA = "advertisData";
    protected static final String RESULT_ADVERTIS_SERVICE_UUIDS = "advertisServiceUUIDs";
    protected static final String RESULT_LOCAL_NAME = "localName";
    protected static final String RESULT_SERVICE_DATA = "serviceData";
    protected static final String RESULT_CONNECTED = "connected";
    protected static final String RESULT_VALUE = "value";
    protected static final String RESULT_SERVICES = "services";
    protected static final String RESULT_UUID = "uuid";
    protected static final String RESULT_IS_PRIMARY = "isPrimary";
    protected static final String RESULT_CHARACTERISTICS = "characteristics";
    protected static final String RESULT_CHARACTERISTIC_PROPERTIES = "properties";
    protected static final String RESULT_CHARACTERISTIC_READ = "read";
    protected static final String RESULT_CHARACTERISTIC_WRITE = "write";
    protected static final String RESULT_CHARACTERISTIC_NOTIFY = "notify";
    protected static final String RESULT_CHARACTERISTIC_INDICATE = "indicate";
    protected static final String RESULT_AVAILABLE = "available";
    protected static final String RESULT_DISCOVERING = "discovering";
    protected static final String RESULT_SCAN_RECORD = "scanRecord";
    private static final String TAG = "Bluetooth";
    private static final int CODE_ON_LESCAN = 0;
    private static final int CODE_ON_CHARACTERISTIC_VALUE_CHANGE = 1;
    private static final int CODE_ON_ADAPTER_STATE_CHANGE = 2;
    private static final int CODE_ON_CONNECTION_STATE_CHANGE = 3;
    private static final int REQUEST_CODE_BASE = getRequestBaseCode();
    private static final int REQUEST_ENABLE_BT = REQUEST_CODE_BASE + 1;

    private volatile long mLastReportTime = 0;
    private volatile boolean mInit = false;
    private Semaphore mSemaphore = new Semaphore(1);
    private volatile long mDiscoveryInterval = 0;
    private volatile boolean mAllowDuplicate = false;
    private Set<LeDevice> mScannedDevice = new ConcurrentSkipListSet<>();
    private Vector<LeDevice> mPendingDevice = new Vector<>();
    private Set<LeDevice> mBatchDevice = new ConcurrentSkipListSet<>();

    private volatile HandlerThread mHandlerThread;
    private volatile Handler mHandler;
    private volatile boolean mReceivedBluetoothStatus = false;

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private boolean checkAdapterReady(Request request) {
        switch (request.getAction()) {
            case ACTION_CLOSE_ADAPTER:
            case ACTION_OPEN_ADAPTER:
            case EVENT_ON_DEVICE_FOUND:
            case EVENT_ON_ADPTER_STATE_CHANGE:
            case EVENT_ON_CONNECTION_STATE_CHANGE:
            case EVENT_ON_CHARACTERISTIC_VALUE_CHANGE:
                return true;
            case ACTION_GET_ADAPTER_STATE:
                if (!mInit) {
                    onOperateFail(request, BleConst.CODE_NOT_INIT, BleConst.MSG_NOT_INIT);
                    return false;
                }
                return true;
            default:
                if (!mInit) {
                    onOperateFail(request, BleConst.CODE_NOT_INIT, BleConst.MSG_NOT_INIT);
                    return false;
                }
                if (!BleManager.getInstance().isBluetoothEnable()) {
                    onOperateFail(request, BleConst.CODE_NOT_AVAILABLE, BleConst.MSG_NOT_AVAILABLE);
                    return false;
                }
                return true;
        }
    }

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        if (BleManager.getInstance().getBluetoothAdapter() == null) {
            onOperateFail(request, BleConst.CODE_SYSTEM_NOT_SUPPORT,
                    BleConst.MSG_SYSTEM_NOT_SUPPORT);
            return null;
        }

        if (!checkAdapterReady(request)) {
            return null;
        }

        String action = request.getAction();

        switch (action) {
            case ACTION_OPEN_ADAPTER:
                openAdapter(request);
                break;
            case ACTION_CLOSE_ADAPTER:
                closeAdapter(request);
                break;
            case ACTION_GET_ADAPTER_STATE:
                getAdapterState(request);
                break;
            case ACTION_START_DEVICES_DISCOVERY:
                startDevicesDiscovery(request);
                break;
            case ACTION_STOP_DEVICES_DISCOVERY:
                stopDevicesDiscovery(request);
                break;
            case ACTION_GET_DEVICES:
                getDevices(request);
                break;
            case ACTION_GET_CONNECTED_DEVICES:
                getConnectedDevices(request);
                break;
            case ACTION_CREATE_BLE_CONNECTION:
                createBLEConnection(request);
                break;
            case ACTION_CLOSE_BLE_CONNECTION:
                closeBLEConnection(request);
                break;
            case ACTION_READ_BLE_CHARACTERISTIC:
                readCharacteristic(request);
                break;
            case ACTION_WRITE_BLE_CHARACTERISTIC:
                writeCharacteristic(request);
                break;
            case ACTION_NOTIFY_BLE_CHARACTERISTIC:
                setCharacteristicNotification(request);
                break;
            case ACTION_GET_BLE_SERVICES:
                getBLEDeviceServices(request);
                break;
            case ACTION_GET_BLE_CHARACTERISTICS:
                getBLEDeviceCharacteristics(request);
                break;
            case EVENT_ON_DEVICE_FOUND:
            case EVENT_ON_ADPTER_STATE_CHANGE:
            case EVENT_ON_CONNECTION_STATE_CHANGE:
            case EVENT_ON_CHARACTERISTIC_VALUE_CHANGE:
                return handleEventRequest(request);
            default:
                break;
        }
        return null;
    }

    private void openAdapter(final Request request) throws JSONException {
        final BleManager manager = BleManager.getInstance();
        boolean operateAdapter = request.getJSONParams().optBoolean(PARAM_OPERATE_ADAPTER, false);
        try {
            mSemaphore.acquire();
            if (!mInit) {
                mHandlerThread = new HandlerThread("bluetooth");
                mHandlerThread.start();
                mHandler = new WorkHandler(mHandlerThread.getLooper());
                manager.registerBluetoothBroadcast(
                        request.getNativeInterface().getActivity().getApplicationContext());
                request
                        .getNativeInterface()
                        .addLifecycleListener(
                                new LifecycleListener() {
                                    @Override
                                    public void onDestroy() {
                                        request.getNativeInterface().removeLifecycleListener(this);
                                        destroy(request);
                                    }
                                });
                mInit = true;
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "open adapter is interrupted", e);
        } finally {
            mSemaphore.release();
        }
        if (manager.isBluetoothEnable()) {
            request.getCallback().callback(Response.SUCCESS);
        } else if (operateAdapter) {
            enableBluetooth(request);
        } else {
            onOperateFail(request, BleConst.CODE_NOT_AVAILABLE, BleConst.MSG_NOT_AVAILABLE);
        }
    }

    private void closeAdapter(final Request request) throws JSONException {
        BleManager manager = BleManager.getInstance();
        boolean operateAdapter = request.getJSONParams().optBoolean(PARAM_OPERATE_ADAPTER, false);
        destroy(request);
        if (operateAdapter && manager.isBluetoothEnable()) {
            disableBluetooth();
        }
        onOperateSuccess(request);
    }

    private void destroy(final Request request) {
        try {
            mSemaphore.acquire();
            if (mInit) {
                mInit = false;
                BleManager manager = BleManager.getInstance();
                manager.unregisterBluetoothBroadcast(
                        request.getNativeInterface().getActivity().getApplicationContext());
                manager.destroy();
                mHandler.removeMessages(WorkHandler.ON_INTERVAL_REACH);
                mHandlerThread.quit();
                clear();
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "destroy interrupted", e);
        } finally {
            mSemaphore.release();
        }
    }

    private synchronized void enableBluetooth(final Request request) {
        BroadcastReceiver bluetoothStatusReceiver;
        BleManager manager = BleManager.getInstance();
        Context applicationContext =
                request.getNativeInterface().getActivity().getApplicationContext();
        if (!mReceivedBluetoothStatus) {
            IntentFilter statusFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            applicationContext.registerReceiver(
                    bluetoothStatusReceiver =
                            new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    if (BluetoothAdapter.ACTION_STATE_CHANGED
                                            .equals(intent.getAction())) {
                                        int state =
                                                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                                        switch (state) {
                                            case BluetoothAdapter.STATE_ON:
                                                Log.i(TAG, "Bluetooth is on");
                                                onOperateSuccess(request);
                                                applicationContext.unregisterReceiver(this);
                                                mReceivedBluetoothStatus = false;
                                                break;
                                            case BluetoothAdapter.STATE_TURNING_ON:
                                                Log.i(TAG, "Bluetooth is turning on");
                                                break;
                                            default:
                                                onOperateFail(
                                                        request, BleConst.CODE_NOT_AVAILABLE,
                                                        BleConst.MSG_NOT_AVAILABLE);
                                                applicationContext.unregisterReceiver(this);
                                                mReceivedBluetoothStatus = false;
                                                break;
                                        }
                                    }
                                }
                            },
                    statusFilter);
            mReceivedBluetoothStatus = true;
            request
                    .getNativeInterface()
                    .addLifecycleListener(
                            new LifecycleListener() {
                                @Override
                                public void onDestroy() {
                                    super.onDestroy();
                                    if (null != bluetoothStatusReceiver
                                            && mReceivedBluetoothStatus) {
                                        applicationContext
                                                .unregisterReceiver(bluetoothStatusReceiver);
                                        mReceivedBluetoothStatus = false;
                                    }
                                    request.getNativeInterface().removeLifecycleListener(this);
                                }
                            });
        }
        manager.enableBluetooth(request.getNativeInterface().getActivity());
    }

    private void disableBluetooth() {
        BleManager manager = BleManager.getInstance();
        if (manager.isBluetoothEnable()) {
            manager.disableBluetooth();
        }
    }

    private void clear() {
        mDiscoveryInterval = 0;
        mAllowDuplicate = false;
        mLastReportTime = 0;
        mScannedDevice.clear();
        mPendingDevice.clear();
        mBatchDevice.clear();
    }

    private void getAdapterState(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        result.put(RESULT_AVAILABLE, BleManager.getInstance().isBluetoothEnable());
        result.put(RESULT_DISCOVERING, BleManager.getInstance().isScanning());
        request.getCallback().callback(new Response(result));
    }

    private void startDevicesDiscovery(final Request request) throws JSONException {
        if (needLocationEnable() && !isLocationEnabled(request)) {
            request
                    .getCallback()
                    .callback(
                            new Response(BleConst.CODE_LOCATION_NOT_TURNED,
                                    BleConst.MSG_LOCATION_NOT_TURNED));
            return;
        }
        JSONObject params = request.getJSONParams();
        ScanOperateResult result =
                BleManager.getInstance()
                        .startLeScan(
                                new BluetoothAdapter.LeScanCallback() {
                                    @Override
                                    public void onLeScan(BluetoothDevice device, int rssi,
                                                         byte[] scanRecord) {
                                        Message msg =
                                                mHandler.obtainMessage(WorkHandler.ON_SCANNED);
                                        Bundle data = new Bundle();
                                        data.putString(RESULT_NAME, device.getName());
                                        data.putString(RESULT_DEVICE_ID, device.getAddress());
                                        data.putInt(RESULT_RSSI, rssi);
                                        data.putByteArray(RESULT_SCAN_RECORD, scanRecord);
                                        msg.setData(data);
                                        msg.sendToTarget();
                                    }
                                },
                                parseUUID(request));
        if (result.getCode() == BleConst.CODE_SUCCESS) {
            mBatchDevice.clear();
            mLastReportTime = System.currentTimeMillis();
            mDiscoveryInterval = params.optLong(PARAM_INTERVAL, 0);
            mAllowDuplicate = params.optBoolean(PARAM_DUPLICATE, false);
            onOperateSuccess(request);
        } else {
            onOperateFail(request, result.getCode(), result.getMsg());
        }
    }

    protected boolean needLocationEnable() {
        return Build.VERSION.SDK_INT >= 23;
    }

    private boolean isLocationEnabled(Request request) {
        LocationManager locationManager =
                (LocationManager)
                        request.getNativeInterface().getActivity()
                                .getSystemService(Context.LOCATION_SERVICE);
        boolean isProviderEnabled =
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
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

    private void stopDevicesDiscovery(Request request) {
        mHandler.removeMessages(WorkHandler.ON_INTERVAL_REACH);
        BleManager.getInstance().stopLeScan();
        request.getCallback().callback(Response.SUCCESS);
    }

    private void getDevices(Request request) {
        List<LeDevice> result = new ArrayList<>(mScannedDevice);
        for (BluetoothGatt gatt : BleManager.getInstance().getConnectedDevices()) {
            LeDevice device = new LeDevice(gatt);
            if (!result.contains(device)) {
                result.add(device);
            }
        }
        request.getCallback().callback(makeLeDevicesResponse(result));
    }

    private void getConnectedDevices(final Request request) throws JSONException {
        request
                .getCallback()
                .callback(
                        makeConnectedDeviceResponse(
                                BleManager.getInstance().getConnectedDevices(),
                                parseUUID(request)));
    }

    private void createBLEConnection(final Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        final String address = params.getString(PARAM_DEVICE_ID);
        final long timeout = params.optLong(PARAM_TIMEOUT, 0); // 默认不进行超时，系统会自动停止连接
        BleManager.getInstance()
                .connectDevice(
                        request.getNativeInterface().getActivity(),
                        address,
                        getOperationCallback(request),
                        timeout);
    }

    private void closeBLEConnection(final Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        final String address = params.getString(PARAM_DEVICE_ID);
        BleManager.getInstance().disconnectDevice(address, getOperationCallback(request));
    }

    private void getBLEDeviceServices(final Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String address = params.getString(PARAM_DEVICE_ID);
        BleManager.getInstance()
                .getDeviceServices(
                        address,
                        new BleDiscoveryServiceCallback() {
                            @Override
                            public void onServicesDiscovery(BluetoothGatt gatt) {
                                try {
                                    request.getCallback().callback(makeServicesResponse(gatt));
                                } catch (JSONException e) {
                                    request.getCallback()
                                            .callback(getExceptionResponse(request, e));
                                }
                            }

                            @Override
                            public void onFail(int code, String msg) {
                                onOperateFail(request, code, msg);
                            }
                        });
    }

    private void getBLEDeviceCharacteristics(final Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String address = params.getString(PARAM_DEVICE_ID);
        final String serviceUUID = params.getString(PARAM_SERVICE_UUID);
        BleManager.getInstance()
                .getDeviceServices(
                        address,
                        new BleDiscoveryServiceCallback() {
                            @Override
                            public void onServicesDiscovery(BluetoothGatt gatt) {
                                try {
                                    request.getCallback().callback(
                                            makeCharacteristicsResponse(gatt, serviceUUID));
                                } catch (JSONException e) {
                                    request.getCallback()
                                            .callback(getExceptionResponse(request, e));
                                }
                            }

                            @Override
                            public void onFail(int code, String msg) {
                                onOperateFail(request, code, msg);
                            }
                        });
    }

    private void readCharacteristic(final Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String address = params.getString(PARAM_DEVICE_ID);
        String serviceUUID = params.getString(PARAM_SERVICE_UUID);
        String charaUUID = params.getString(PARAM_CHARACTERISTIC_UUID);
        BleManager.getInstance()
                .readCharacteristic(address, serviceUUID, charaUUID, getOperationCallback(request));
    }

    private void writeCharacteristic(final Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        String address = params.getString(PARAM_DEVICE_ID);
        String serviceUUID = params.getString(PARAM_SERVICE_UUID);
        String charaUUID = params.getString(PARAM_CHARACTERISTIC_UUID);
        byte[] buffer = (byte[]) params.get(PARAM_VALUE);
        BleManager.getInstance()
                .writeCharacteristic(
                        address, serviceUUID, charaUUID, buffer, getOperationCallback(request));
    }

    private void setCharacteristicNotification(final Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String address = params.getString(PARAM_DEVICE_ID);
        String serviceUUID = params.getString(PARAM_SERVICE_UUID);
        String charaUUID = params.getString(PARAM_CHARACTERISTIC_UUID);
        boolean state = params.getBoolean(PARAM_NOTIFY_STATE);

        BleManager.getInstance()
                .setNotification(address, serviceUUID, charaUUID, state,
                        getOperationCallback(request));
    }

    private Response handleEventRequest(Request request) {
        if (request.getCallback().isValid()) {
            putCallbackContext(new BleCallbackContext(request, true));
        } else {
            removeCallbackContext(request.getAction());
        }
        return Response.SUCCESS;
    }

    private UUID[] parseUUID(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        JSONArray stringIds = params.optJSONArray(PARAM_SERVICE_UUIDS);
        if (stringIds == null) {
            return null;
        }
        int len = stringIds.length();
        UUID[] uuids = new UUID[len];
        for (int i = 0; i < len; i++) {
            uuids[i] = BleAdParser.string2UUID(stringIds.getString(i));
        }
        return uuids;
    }

    private Response makeServicesResponse(BluetoothGatt gatt) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray servicesJSON = new JSONArray();
        result.put(RESULT_SERVICES, servicesJSON);
        for (BluetoothGattService service : gatt.getServices()) {
            JSONObject serviceJSON = new JSONObject();
            servicesJSON.put(serviceJSON);
            serviceJSON.put(RESULT_UUID, service.getUuid().toString().toUpperCase());
            serviceJSON.put(
                    RESULT_IS_PRIMARY,
                    service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY);
        }
        return new Response(result);
    }

    private Response makeCharacteristicsResponse(BluetoothGatt gatt, String serviceUUID)
            throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray characteristicsJSON = new JSONArray();
        result.put(RESULT_CHARACTERISTICS, characteristicsJSON);
        BluetoothGattService service = gatt.getService(BleAdParser.string2UUID(serviceUUID));
        if (service == null) {
            return new Response(BleConst.CODE_NO_SERVICE, BleConst.MSG_NO_SERVICE);
        }
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            JSONObject characteristicJSON = new JSONObject();
            JSONObject propertiesJSON = new JSONObject();
            characteristicJSON.put(RESULT_UUID, characteristic.getUuid().toString().toUpperCase());

            int properties = characteristic.getProperties();
            propertiesJSON.put(RESULT_CHARACTERISTIC_READ, PropertyUtils.canRead(properties));
            propertiesJSON.put(RESULT_CHARACTERISTIC_WRITE, PropertyUtils.canWrite(properties));
            propertiesJSON.put(RESULT_CHARACTERISTIC_NOTIFY, PropertyUtils.canNotify(properties));
            propertiesJSON
                    .put(RESULT_CHARACTERISTIC_INDICATE, PropertyUtils.canIndicate(properties));
            characteristicJSON.put(RESULT_CHARACTERISTIC_PROPERTIES, propertiesJSON);

            characteristicsJSON.put(characteristicJSON);
        }
        return new Response(result);
    }

    private LeDevice makeScanResult(String name, String deviceId, int rssi, byte[] scanRecord) {
        LeDevice leDevice = new LeDevice(deviceId);
        leDevice.mName = makeDeviceName(name);
        leDevice.mRssi = rssi;

        if (scanRecord == null) {
            Log.d(TAG, "scanRecord is null");
            return leDevice;
        }
        List<BleAdParser.BleAdData> datas;
        try {
            datas = BleAdParser.parseRecord(scanRecord);
        } catch (Exception e) {
            Log.w(TAG, "parse scan record failed ", e);
            return leDevice;
        }
        for (BleAdParser.BleAdData d : datas) {
            switch (d.getType()) {
                case BleAdParser.EBLE_16BitUUIDInc:
                case BleAdParser.EBLE_16BitUUIDCom:
                case BleAdParser.EBLE_32BitUUIDInc:
                case BleAdParser.EBLE_32BitUUIDCom:
                case BleAdParser.EBLE_128BitUUIDInc:
                case BleAdParser.EBLE_128BitUUIDCom:
                    String data = d.getData();
                    leDevice.mAdvertisServiceUUIDs.add(data);
                    break;
                case BleAdParser.EBLE_MANDATA:
                    byte[] array1 = leDevice.mAdvertisData;
                    byte[] array2 = d.getData();
                    byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
                    System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
                    leDevice.mAdvertisData = joinedArray;
                    break;
                case BleAdParser.EBLE_16BitSERVICEDATA:
                case BleAdParser.EBLE_32BitSERVICEDATA:
                case BleAdParser.EBLE_128BitSERVICEDATA:
                    Pair<String, byte[]> serviceData = d.getData();
                    leDevice.mServiceData.add(serviceData);
                    break;
                case BleAdParser.EBLE_SHORTNAME:
                case BleAdParser.EBLE_COMPLETENAME:
                    leDevice.mLocalName = d.getData();
                    break;
                default:
                    break;
            }
        }
        return leDevice;
    }

    private Response makeLeDevicesResponse(List<LeDevice> devices) {
        JavaSerializeObject result = new JavaSerializeObject(new HashMap<String, Object>());
        JavaSerializeArray devicesSerialize = new JavaSerializeArray();
        for (LeDevice device : devices) {
            devicesSerialize.put(device.toJavaSerializeObject());
        }
        result.put(RESULT_DEVICES, devicesSerialize);
        return new Response(result);
    }

    private Response makeConnectedDeviceResponse(List<BluetoothGatt> gatts, UUID[] uuids)
            throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray devicesJSON = new JSONArray();
        result.put(RESULT_DEVICES, devicesJSON);
        List<UUID> uuidParams;
        if (uuids != null) {
            uuidParams = Arrays.asList(uuids);
        } else {
            uuidParams = new ArrayList<>();
        }

        for (BluetoothGatt gatt : gatts) {
            List<BluetoothGattService> tempServices = gatt.getServices();
            if (tempServices == null) {
                continue;
            }
            List<UUID> serviceUuids = new ArrayList<>();
            for (int i = 0; i < tempServices.size(); i++) {
                serviceUuids.add(tempServices.get(i).getUuid());
            }
            BluetoothDevice device = gatt.getDevice();
            if (uuidParams.size() == 0 || !Collections.disjoint(uuidParams, serviceUuids)) {
                JSONObject deviceJSON = new JSONObject();
                deviceJSON.put(RESULT_NAME, makeDeviceName(device.getName()));
                deviceJSON.put(RESULT_DEVICE_ID, device.getAddress());
                devicesJSON.put(deviceJSON);
            }
        }
        return new Response(result);
    }

    private String makeDeviceName(String name) {
        return name == null ? "" : name;
    }

    private void onOperateSuccess(Request request) {
        request.getCallback().callback(Response.SUCCESS);
    }

    private void onOperateFail(Request request, int code, String msg) {
        request.getCallback().callback(new Response(code, msg));
    }

    private BleOperationCallback getOperationCallback(final Request request) {
        return new BleOperationCallback() {
            @Override
            public void onSuccess() {
                onOperateSuccess(request);
            }

            @Override
            public void onFail(int code, String msg) {
                onOperateFail(request, code, msg);
            }
        };
    }

    private class BleCallbackContext extends CallbackContext {

        public BleCallbackContext(Request request, boolean reserved) {
            super(Bluetooth.this, request.getAction(), request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            switch (getAction()) {
                case EVENT_ON_CHARACTERISTIC_VALUE_CHANGE:
                    BleManager.getInstance()
                            .setCharacteristicChangeCallback(
                                    new BleNotificationCallback() {
                                        @Override
                                        public void onCharacteristicChanged(
                                                String address,
                                                String serviceUUID,
                                                String characteristicUUID,
                                                byte[] data) {
                                            JavaSerializeObject result = new JavaSerializeObject();
                                            result.put(RESULT_DEVICE_ID, address);
                                            result.put(RESULT_SERVICE_UUID,
                                                    serviceUUID.toUpperCase());
                                            result.put(RESULT_CHARACTERISTIC_UUID,
                                                    characteristicUUID.toUpperCase());
                                            result.put(RESULT_VALUE, data);
                                            runCallbackContext(
                                                    EVENT_ON_CHARACTERISTIC_VALUE_CHANGE,
                                                    CODE_ON_CHARACTERISTIC_VALUE_CHANGE,
                                                    new Response(result));
                                        }
                                    });
                    break;
                case EVENT_ON_CONNECTION_STATE_CHANGE:
                    BleManager.getInstance()
                            .setConnectionStateChangeCallback(
                                    new BleConnectStateCallback() {
                                        @Override
                                        public void onConnectionStateChange(boolean connected,
                                                                            String address) {
                                            try {
                                                JSONObject result = new JSONObject();
                                                result.put(RESULT_DEVICE_ID, address);
                                                result.put(RESULT_CONNECTED, connected);
                                                runCallbackContext(
                                                        EVENT_ON_CONNECTION_STATE_CHANGE,
                                                        CODE_ON_CONNECTION_STATE_CHANGE,
                                                        new Response(result));
                                            } catch (JSONException e) {
                                                // ignore
                                            }
                                        }
                                    });
                    break;
                case EVENT_ON_ADPTER_STATE_CHANGE:
                    BleManager.getInstance()
                            .setBleAdapterChangedCallback(
                                    new BleAdapterChangeCallback() {
                                        @Override
                                        public void onAdapterChange(boolean state,
                                                                    boolean discovering) {
                                            JSONObject result = new JSONObject();
                                            try {
                                                result.put(RESULT_AVAILABLE, state);
                                                result.put(RESULT_DISCOVERING, discovering);
                                                runCallbackContext(
                                                        EVENT_ON_ADPTER_STATE_CHANGE,
                                                        CODE_ON_ADAPTER_STATE_CHANGE,
                                                        new Response(result));
                                            } catch (JSONException e) {
                                                // ignore
                                            }
                                        }
                                    });
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            switch (getAction()) {
                case EVENT_ON_CHARACTERISTIC_VALUE_CHANGE:
                    BleManager.getInstance().setCharacteristicChangeCallback(null);
                    break;
                case EVENT_ON_CONNECTION_STATE_CHANGE:
                    BleManager.getInstance().setConnectionStateChangeCallback(null);
                    break;
                case EVENT_ON_ADPTER_STATE_CHANGE:
                    BleManager.getInstance().setBleAdapterChangedCallback(null);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void callback(int what, Object obj) {
            mRequest.getCallback().callback((Response) obj);
        }
    }

    private class LeDevice implements Comparable<LeDevice> {
        private String mDeviceId;
        private String mName = "";
        private String mLocalName = "";
        private int mRssi;
        private byte[] mAdvertisData = new byte[0];
        private List<String> mAdvertisServiceUUIDs = new ArrayList<>();
        private List<Pair<String, byte[]>> mServiceData = new ArrayList<>();

        private LeDevice(@NonNull String deviceId) {
            mDeviceId = deviceId;
        }

        private LeDevice(BluetoothGatt gatt) {
            BluetoothDevice device = gatt.getDevice();
            mDeviceId = device.getAddress();
            mName = makeDeviceName(device.getName());
        }

        private JavaSerializeObject toJavaSerializeObject() {
            JavaSerializeObject result = new JavaSerializeObject(new HashMap<String, Object>());
            result.put(RESULT_DEVICE_ID, mDeviceId);
            result.put(RESULT_RSSI, mRssi);
            result.put(RESULT_NAME, mName);
            result.put(RESULT_LOCAL_NAME, mLocalName);
            result.put(
                    RESULT_ADVERTIS_SERVICE_UUIDS,
                    new JavaSerializeArray(new JSONArray(mAdvertisServiceUUIDs)));
            JavaSerializeObject serviceData = new JavaSerializeObject();
            for (Pair<String, byte[]> d : mServiceData) {
                byte[] bytes = d.second;
                serviceData.put(d.first, bytes);
            }
            result.put(RESULT_SERVICE_DATA, serviceData);
            result.put(RESULT_ADVERTIS_DATA, mAdvertisData);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof LeDevice) {
                return mDeviceId.equals(((LeDevice) obj).mDeviceId);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return mDeviceId.hashCode();
        }

        @Override
        public int compareTo(@NonNull LeDevice device) {
            if (this == device) {
                return 0;
            }
            return mDeviceId.compareTo(device.mDeviceId);
        }
    }

    private class WorkHandler extends Handler {
        private static final int ON_SCANNED = 0;
        private static final int ON_INTERVAL_REACH = 1;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ON_SCANNED:
                    Bundle data = msg.getData();
                    String name = data.getString(RESULT_NAME);
                    String deviceId = data.getString(RESULT_DEVICE_ID);
                    int rssi = data.getInt(RESULT_RSSI);
                    byte[] scanRecord = data.getByteArray(RESULT_SCAN_RECORD);
                    LeDevice leDevice = makeScanResult(name, deviceId, rssi, scanRecord);
                    long currentTime = System.currentTimeMillis();
                    if (mAllowDuplicate || !mBatchDevice.contains(leDevice)) {
                        mPendingDevice.add(leDevice);
                    }
                    mBatchDevice.add(leDevice);
                    mScannedDevice.add(leDevice);
                    removeMessages(ON_INTERVAL_REACH);
                    if (currentTime - mLastReportTime >= mDiscoveryInterval) {
                        reportScanResult();
                    } else {
                        sendEmptyMessageDelayed(
                                ON_INTERVAL_REACH,
                                mLastReportTime + mDiscoveryInterval - currentTime);
                    }
                    break;
                case ON_INTERVAL_REACH:
                    reportScanResult();
                    break;
                default:
                    break;
            }
        }

        private void reportScanResult() {
            if (mPendingDevice.size() > 0) {
                mLastReportTime = System.currentTimeMillis();
                runCallbackContext(
                        EVENT_ON_DEVICE_FOUND, CODE_ON_LESCAN,
                        makeLeDevicesResponse(mPendingDevice));
                mPendingDevice.clear();
            }
        }
    }
}
