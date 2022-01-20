/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.UUID;
import org.hapjs.features.bluetooth.callback.BleAdapterChangeCallback;
import org.hapjs.features.bluetooth.callback.BleConnectStateCallback;
import org.hapjs.features.bluetooth.callback.BleDiscoveryServiceCallback;
import org.hapjs.features.bluetooth.callback.BleNotificationCallback;
import org.hapjs.features.bluetooth.callback.BleOperationCallback;
import org.hapjs.features.bluetooth.connect.BleConnector;
import org.hapjs.features.bluetooth.connect.BleDeviceConnectController;
import org.hapjs.features.bluetooth.data.BleConst;
import org.hapjs.features.bluetooth.data.ScanOperateResult;
import org.hapjs.features.bluetooth.scan.BleScanner;

public class BleManager {

    private BluetoothAdapter mBluetoothAdapter;
    private BleScanner mBleScanner;
    private BleAdapterChangeCallback mBleAdapterChangedCallback;
    private AdapterBroadcastReceiver mAdapterBroadcastReceiver;

    private BleManager() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleScanner = BleScanner.getInstance();
    }

    public static BleManager getInstance() {
        return BleManagerHolder.sBleManager;
    }

    public boolean isBluetoothEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void enableBluetooth(Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivity(enableBtIntent);
    }

    public void disableBluetooth() {
        mBluetoothAdapter.disable();
    }

    public ScanOperateResult startLeScan(
            BluetoothAdapter.LeScanCallback leScanCallback, UUID[] uuids) {
        return mBleScanner.startLeScan(leScanCallback, uuids);
    }

    public void stopLeScan() {
        mBleScanner.stopLeScan();
    }

    public boolean isScanning() {
        return mBleScanner.isScanning();
    }

    public void connectDevice(
            Context context, String address, @NonNull BleOperationCallback callback, long timeout) {
        BleDeviceConnectController.getInstance().connectDevice(context, address, callback, timeout);
    }

    public void disconnectDevice(String address, @NonNull BleOperationCallback callback) {
        BleDeviceConnectController.getInstance().disconnectDevice(address, callback);
    }

    public void setConnectionStateChangeCallback(BleConnectStateCallback callback) {
        BleDeviceConnectController.getInstance().setConnectionStateChangeCallback(callback);
    }

    public void getDeviceServices(String address, BleDiscoveryServiceCallback callback) {
        BleConnector connector =
                BleDeviceConnectController.getInstance().findConnectedConnector(address);
        if (connector != null) {
            connector.discoverService(callback);
        } else {
            callback.onFail(BleConst.CODE_NO_DEVICE, BleConst.MSG_NO_DEVICE);
        }
    }

    public void writeCharacteristic(
            String address,
            String serviceUUID,
            String characteristicUUID,
            byte[] value,
            BleOperationCallback callback) {
        BleConnector connector =
                BleDeviceConnectController.getInstance().findConnectedConnector(address);
        if (connector != null) {
            connector.writeCharacteristic(serviceUUID, characteristicUUID, value, callback);
        } else {
            callback.onFail(BleConst.CODE_NO_DEVICE, BleConst.MSG_NO_DEVICE);
        }
    }

    public void readCharacteristic(
            String address,
            String serviceUUID,
            String characteristicUUID,
            BleOperationCallback callback) {
        BleConnector connector =
                BleDeviceConnectController.getInstance().findConnectedConnector(address);
        if (connector != null) {
            connector.readCharacteristic(serviceUUID, characteristicUUID, callback);
        } else {
            callback.onFail(BleConst.CODE_NO_DEVICE, BleConst.MSG_NO_DEVICE);
        }
    }

    public void setNotification(
            String address,
            String serviceUUID,
            String characteristicUUID,
            boolean enable,
            final BleOperationCallback callback) {
        BleConnector connector =
                BleDeviceConnectController.getInstance().findConnectedConnector(address);
        if (connector != null) {
            connector.setNotification(serviceUUID, characteristicUUID, enable, callback);
        } else {
            callback.onFail(BleConst.CODE_NO_DEVICE, BleConst.MSG_NO_DEVICE);
        }
    }

    public void setCharacteristicChangeCallback(final BleNotificationCallback callback) {
        BleDeviceConnectController.getInstance().setCharacteristicChangeCallback(callback);
    }

    public void destroy() {
        mBleScanner.stopLeScan();
        BleDeviceConnectController.getInstance().destroy();
    }

    public void setBleAdapterChangedCallback(BleAdapterChangeCallback changedCallback) {
        mBleAdapterChangedCallback = changedCallback;
    }

    public void registerBluetoothBroadcast(Context context) {
        if (mAdapterBroadcastReceiver == null) {
            mAdapterBroadcastReceiver = new AdapterBroadcastReceiver();
            context.registerReceiver(
                    mAdapterBroadcastReceiver,
                    new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }
    }

    public void unregisterBluetoothBroadcast(Context context) {
        if (mAdapterBroadcastReceiver != null) {
            try {
                context.unregisterReceiver(mAdapterBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                // ignore unregister exception
            }
            mAdapterBroadcastReceiver = null;
        }
    }

    public List<BluetoothGatt> getConnectedDevices() {
        return BleDeviceConnectController.getInstance().getConnectedDevices();
    }

    private static class BleManagerHolder {
        private static final BleManager sBleManager = new BleManager();
    }

    private class AdapterBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                boolean isScanning = isScanning();
                BleAdapterChangeCallback callback = mBleAdapterChangedCallback;
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        if (callback != null) {
                            callback.onAdapterChange(true, isScanning);
                        }
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        if (isScanning) {
                            stopLeScan();
                        }
                        if (callback != null) {
                            callback.onAdapterChange(false, false);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
