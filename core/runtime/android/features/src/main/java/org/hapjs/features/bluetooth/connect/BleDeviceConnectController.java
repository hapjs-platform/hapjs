/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.hapjs.features.bluetooth.callback.BleConnectStateCallback;
import org.hapjs.features.bluetooth.callback.BleNotificationCallback;
import org.hapjs.features.bluetooth.callback.BleOperationCallback;
import org.hapjs.features.bluetooth.data.BleConst;

public class BleDeviceConnectController {

    /**
     * 蓝牙设备列表，只保存已连接的设备，暂时不在引擎端限制数量
     */
    private LinkedHashMap<String, BleConnector> mConnectedDevices =
            new BleLruHashMap<>(Integer.MAX_VALUE, true);
    /**
     * 连接中的设备
     */
    private LinkedHashMap<String, BleConnector> mConnectingDevices =
            new BleLruHashMap<>(Integer.MAX_VALUE, false);

    private volatile BleConnectStateCallback mBleStateWatcher;
    private volatile BleNotificationCallback mBleNotificationWatcher;

    private BleDeviceConnectController() {
    }

    public static BleDeviceConnectController getInstance() {
        return BleDeviceHolder.sInstance;
    }

    public synchronized List<BluetoothGatt> getConnectedDevices() {
        List<BluetoothGatt> result = new ArrayList<>();
        for (BleConnector connector : mConnectedDevices.values()) {
            BluetoothGatt gatt = connector.getDeviceGatt();
            if (gatt != null) {
                result.add(gatt);
            }
        }
        return result;
    }

    public synchronized BleConnector findConnector(String address) {
        BleConnector connector = mConnectedDevices.get(address);
        if (connector != null) {
            return connector;
        }
        return mConnectingDevices.get(address);
    }

    public synchronized BleConnector findConnectedConnector(String address) {
        return mConnectedDevices.get(address);
    }

    public synchronized void connectDevice(
            Context context, String address, @NonNull BleOperationCallback callback, long timeout) {
        BleConnector connector = findConnector(address);
        if (connector == null) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
            if (device == null) {
                callback.onFail(BleConst.CODE_NO_DEVICE, BleConst.MSG_NO_DEVICE);
            } else {
                connector = new BleConnector(device);
                connector.connect(context, callback, timeout);
            }
        } else {
            connector.connect(context, callback, timeout);
        }
    }

    public synchronized void disconnectDevice(
            String address, @NonNull BleOperationCallback callback) {
        BleConnector connector = findConnector(address);
        if (connector != null) {
            connector.disconnect();
            callback.onSuccess();
        } else {
            callback.onFail(BleConst.CODE_NO_DEVICE, BleConst.MSG_NO_DEVICE);
        }
    }

    /*package*/
    synchronized void onConnectorConnecting(BleConnector bleConnector) {
        mConnectingDevices.put(bleConnector.getKey(), bleConnector);
    }

    public void destroy() {
        ArrayList<BleConnector> devices;
        synchronized (this) {
            devices = new ArrayList<>(mConnectedDevices.values());
            devices.addAll(mConnectingDevices.values());
            mConnectedDevices.clear();
            mConnectingDevices.clear();
        }
        for (int i = 0; i < devices.size(); i++) {
            BleConnector device = devices.get(i);
            if (device != null) {
                device.destroy();
            }
        }
    }

    public void setConnectionStateChangeCallback(BleConnectStateCallback callBack) {
        this.mBleStateWatcher = callBack;
    }

    /*package*/
    synchronized void onConnectionStateChange(
            boolean connected, BleConnector bleConnector) {
        BleConnectStateCallback watcher = mBleStateWatcher;
        String key = bleConnector.getKey();
        if (connected) {
            mConnectedDevices.put(key, bleConnector);
            mConnectingDevices.remove(bleConnector.getKey());
        } else {
            mConnectedDevices.remove(key);
            mConnectingDevices.remove(key);
        }
        if (watcher != null) {
            watcher.onConnectionStateChange(connected, bleConnector.getKey());
        }
    }

    public void setCharacteristicChangeCallback(BleNotificationCallback callback) {
        mBleNotificationWatcher = callback;
    }

    /*package*/ void onCharacteristicChange(
            String address, String serviceUUID, String characteristicUUID, byte[] data) {
        BleNotificationCallback watcher = mBleNotificationWatcher;
        if (watcher != null) {
            watcher.onCharacteristicChanged(address, serviceUUID, characteristicUUID, data);
        }
    }

    private static class BleDeviceHolder {
        private static final BleDeviceConnectController sInstance =
                new BleDeviceConnectController();
    }
}
