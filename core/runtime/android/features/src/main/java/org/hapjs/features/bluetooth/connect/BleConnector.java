/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.connect;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.hapjs.features.bluetooth.callback.BleDiscoveryServiceCallback;
import org.hapjs.features.bluetooth.callback.BleOperationCallback;
import org.hapjs.features.bluetooth.data.BleConst;
import org.hapjs.features.bluetooth.utils.BleAdParser;
import org.hapjs.features.bluetooth.utils.PropertyUtils;

public class BleConnector {
    private static final String TAG = "BleConnector";
    private static final int STATE_DISCONNECT = 0;
    private static final int STATE_CONNECTED = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int MSG_CONNECT_TIMEOUT = 0;
    private volatile int mConnectState = STATE_DISCONNECT;
    private volatile boolean mServiceDiscovered = false;

    private BluetoothDevice mDevice;
    private volatile BluetoothGatt mDeviceGatt;
    private List<BluetoothGattCallback> mOperateCallback = new LinkedList<>();
    private final BluetoothGattCallback mCoreCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    BleDeviceConnectController connectController =
                            BleDeviceConnectController.getInstance();
                    if (isConnectState(newState)) {
                        connectController.onConnectionStateChange(true, BleConnector.this);
                        mConnectState = STATE_CONNECTED;
                    } else {
                        connectController.onConnectionStateChange(false, BleConnector.this);
                        mConnectState = STATE_DISCONNECT;
                        close();
                        mServiceDiscovered = false;
                    }
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onConnectionStateChange(gatt, status, newState);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mServiceDiscovered = true;
                    }
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onServicesDiscovered(gatt, status);
                    }
                }

                @Override
                public void onCharacteristicRead(
                        BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                        int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onCharacteristicRead(gatt, characteristic, status);
                    }
                }

                @Override
                public void onCharacteristicWrite(
                        BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                        int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onCharacteristicWrite(gatt, characteristic, status);
                    }
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    List<BluetoothGattCallback> callbacks;
                    BleDeviceConnectController.getInstance()
                            .onCharacteristicChange(
                                    gatt.getDevice().getAddress(),
                                    characteristic.getService().getUuid().toString().toUpperCase(),
                                    characteristic.getUuid().toString().toUpperCase(),
                                    characteristic.getValue());

                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onCharacteristicChanged(gatt, characteristic);
                    }
                }

                @Override
                public void onDescriptorRead(
                        BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorRead(gatt, descriptor, status);
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onDescriptorRead(gatt, descriptor, status);
                    }
                }

                @Override
                public void onDescriptorWrite(
                        BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onDescriptorWrite(gatt, descriptor, status);
                    }
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    super.onReliableWriteCompleted(gatt, status);
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onReliableWriteCompleted(gatt, status);
                    }
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                    List<BluetoothGattCallback> callbacks;
                    synchronized (BleConnector.this) {
                        callbacks = new ArrayList<>(mOperateCallback);
                    }
                    for (BluetoothGattCallback callback : callbacks) {
                        callback.onReadRemoteRssi(gatt, rssi, status);
                    }
                }
            };
    private Handler mMainHandler =
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_CONNECT_TIMEOUT:
                            BleOperationCallback operationCallback = (BleOperationCallback) msg.obj;
                            operationCallback
                                    .onFail(BleConst.CODE_CONNECTION_FAIL, "connect timeout");
                            destroy();
                            break;
                        default:
                            break;
                    }
                }
            };

    public BleConnector(@NonNull BluetoothDevice device) {
        this.mDevice = device;
    }

    public synchronized void connect(
            Context context, final BleOperationCallback callback, long timeout) {
        int state = mConnectState;
        if (state == STATE_DISCONNECT) {
            mConnectState = STATE_CONNECTING;
            doConnectLeDevice(context, callback, timeout);
        } else if (state == STATE_CONNECTING) {
            addOperationCallback(
                    new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                            int newState) {
                            removeOperationCallback(this);
                            if (isConnectState(newState)) {
                                callback.onSuccess();
                            } else {
                                callback.onFail(BleConst.CODE_CONNECTION_FAIL,
                                        BleConst.MSG_CONNECTION_FAIL);
                            }
                        }
                    });
        } else if (state == STATE_CONNECTED) {
            callback.onSuccess();
        }
    }

    public synchronized void disconnect(final BleOperationCallback callback) {
        int lastState = mConnectState;
        mMainHandler.removeMessages(MSG_CONNECT_TIMEOUT);
        if (lastState == STATE_DISCONNECT) {
            if (callback != null) {
                callback.onSuccess();
            } else {
                Log.w(TAG, "callback is null.");
            }
        } else {
            mConnectState = STATE_DISCONNECT;
            BluetoothGatt gatt = mDeviceGatt;
            if (gatt != null) {
                gatt.disconnect();
                mServiceDiscovered = false;
                if (lastState == STATE_CONNECTING) {
                    // connection is not establish, close directly
                    gatt.close();
                }
                if (callback != null) {
                    addOperationCallback(
                            new BluetoothGattCallback() {
                                @Override
                                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                                    int newState) {
                                    removeOperationCallback(this);
                                    if (isConnectState(newState)) {
                                        callback.onFail(BleConst.CODE_SYSTEM_ERROR,
                                                "device is connected");
                                    } else {
                                        callback.onSuccess();
                                    }
                                }
                            });
                } else {
                    Log.w(TAG, "callback is null.");
                }
            } else {
                if (callback != null) {
                    callback.onFail(BleConst.CODE_NO_CONNECTION, BleConst.MSG_NO_CONNECTION);
                } else {
                    Log.w(TAG, "callback is null.");
                }
            }
        }
    }

    public void disconnect() {
        disconnect(null);
    }

    public void discoverService(final BleDiscoveryServiceCallback callback) {
        BluetoothGatt gatt = mDeviceGatt;
        if (mConnectState != STATE_CONNECTED || gatt == null) {
            callback.onFail(BleConst.CODE_NO_CONNECTION, BleConst.MSG_NO_CONNECTION);
        } else if (!mServiceDiscovered) {
            doDiscoverService(gatt, callback);
        } else {
            callback.onServicesDiscovery(gatt);
        }
    }

    public void writeCharacteristic(
            String serviceUUID,
            final String characteristicUUID,
            byte[] value,
            final BleOperationCallback callback) {
        BluetoothGatt gatt = mDeviceGatt;
        BluetoothGattCharacteristic chara =
                handleFindCharacteristic(serviceUUID, characteristicUUID, gatt, callback);
        if (chara == null) {
            return;
        }
        if (!PropertyUtils.canWrite(chara.getProperties())) {
            callback.onFail(BleConst.CODE_PROPERTY_NOT_SUPPORT, BleConst.MSG_PROPERTY_NOT_SUPPORT);
            return;
        }
        if (chara.setValue(value)) {
            if (gatt.writeCharacteristic(chara)) {
                addOperationCallback(
                        new BluetoothGattCallback() {
                            @Override
                            public void onCharacteristicWrite(
                                    BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                    int status) {
                                if (characteristic.getUuid()
                                        .equals(BleAdParser.string2UUID(characteristicUUID))) {
                                    BleConnector.this.removeOperationCallback(this);
                                    if (status == BluetoothGatt.GATT_SUCCESS) {
                                        callback.onSuccess();
                                    } else {
                                        callback.onFail(BleConst.CODE_SYSTEM_ERROR,
                                                "write fail: status=" + status);
                                    }
                                }
                            }
                        });
            } else {
                callback.onFail(BleConst.CODE_SYSTEM_ERROR, "write fail: device is busy");
            }
        } else {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR,
                    "requested value could not be stored locally");
        }
    }

    public void readCharacteristic(
            String serviceUUID, final String characteristicUUID,
            final BleOperationCallback callback) {
        BluetoothGatt gatt = mDeviceGatt;
        final BluetoothGattCharacteristic chara =
                handleFindCharacteristic(serviceUUID, characteristicUUID, gatt, callback);
        if (chara == null) {
            return;
        }
        if (!PropertyUtils.canRead(chara.getProperties())) {
            callback.onFail(BleConst.CODE_PROPERTY_NOT_SUPPORT, BleConst.MSG_PROPERTY_NOT_SUPPORT);
            return;
        }
        if (gatt.readCharacteristic(chara)) {
            addOperationCallback(
                    new BluetoothGattCallback() {
                        @Override
                        public void onCharacteristicRead(
                                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                int status) {
                            if (characteristic.getUuid()
                                    .equals(BleAdParser.string2UUID(characteristicUUID))) {
                                BleConnector.this.removeOperationCallback(this);
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    callback.onSuccess();
                                    BleDeviceConnectController.getInstance()
                                            .onCharacteristicChange(
                                                    gatt.getDevice().getAddress(),
                                                    characteristic.getService().getUuid().toString()
                                                            .toUpperCase(),
                                                    characteristic.getUuid().toString()
                                                            .toUpperCase(),
                                                    characteristic.getValue());
                                } else {
                                    callback.onFail(BleConst.CODE_SYSTEM_ERROR,
                                            "read fail: status=" + status);
                                }
                            }
                        }
                    });
        } else {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR, "read fail: device is busy");
        }
    }

    public void setNotification(
            String serviceUUID,
            final String characteristicUUID,
            final boolean enable,
            final BleOperationCallback callback) {
        BluetoothGatt gatt = mDeviceGatt;
        final BluetoothGattCharacteristic chara =
                handleFindCharacteristic(serviceUUID, characteristicUUID, gatt, callback);
        if (chara == null) {
            return;
        }

        int properties = chara.getProperties();
        if (!PropertyUtils.canNotify(properties) && !PropertyUtils.canIndicate(properties)) {
            callback.onFail(BleConst.CODE_PROPERTY_NOT_SUPPORT, BleConst.MSG_PROPERTY_NOT_SUPPORT);
            return;
        }

        final boolean success = gatt.setCharacteristicNotification(chara, enable);
        if (!success) {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR, "set characteristic notification fail!");
            return;
        }

        final BluetoothGattDescriptor desc = chara.getDescriptor(BleConst.DESCRIPTOR_NOTIFY);
        if (desc == null) {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR, "notify descriptor not found!");
            return;
        }

        if (PropertyUtils.canNotify(properties)) {
            // 支持通知
            desc.setValue(
                    enable
                            ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        } else {
            // 支持指示
            desc.setValue(
                    enable
                            ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                            : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        boolean success2 = gatt.writeDescriptor(desc);
        if (!success2) {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR,
                    "notify descriptor write fail: device is busy");
            return;
        }
        addOperationCallback(
                new BluetoothGattCallback() {
                    @Override
                    public void onDescriptorWrite(
                            BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        if (descriptor.getUuid().equals(desc.getUuid())) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                callback.onSuccess();
                            } else {
                                callback.onFail(
                                        BleConst.CODE_SYSTEM_ERROR,
                                        "notify descriptor write fail: status=" + status);
                            }
                            BleConnector.this.removeOperationCallback(this);
                        }
                    }
                });
    }

    public void close() {
        BluetoothGatt gatt = mDeviceGatt;
        if (gatt != null) {
            gatt.close();
        }
    }

    public void destroy() {
        disconnect();
        close();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public BluetoothGatt getDeviceGatt() {
        return mDeviceGatt;
    }

    public String getKey() {
        return mDevice.getAddress();
    }

    private void doConnectLeDevice(
            Context context, final BleOperationCallback callback, long timeout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDeviceGatt = mDevice.connectGatt(context, false, mCoreCallback, TRANSPORT_LE);
        } else {
            mDeviceGatt = mDevice.connectGatt(context, false, mCoreCallback);
        }
        if (mDeviceGatt == null) {
            mConnectState = STATE_DISCONNECT;
            callback.onFail(BleConst.CODE_CONNECTION_FAIL, BleConst.MSG_CONNECTION_FAIL);
            return;
        }
        if (timeout > 0) {
            Message timeoutMsg = mMainHandler.obtainMessage(MSG_CONNECT_TIMEOUT);
            timeoutMsg.obj = callback;
            mMainHandler.sendMessageDelayed(timeoutMsg, timeout);
        }
        BleDeviceConnectController.getInstance().onConnectorConnecting(this);
        addOperationCallback(
                new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                        int newState) {
                        mMainHandler.removeMessages(MSG_CONNECT_TIMEOUT);
                        if (isConnectState(newState)) {
                            callback.onSuccess();
                        } else {
                            callback.onFail(BleConst.CODE_CONNECTION_FAIL,
                                    BleConst.MSG_CONNECTION_FAIL);
                        }
                        BleConnector.this.removeOperationCallback(this);
                    }
                });
    }

    private void doDiscoverService(BluetoothGatt gatt, final BleDiscoveryServiceCallback callback) {
        if (gatt.discoverServices()) {
            addOperationCallback(
                    new BluetoothGattCallback() {
                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                callback.onServicesDiscovery(gatt);
                            } else {
                                callback.onFail(status, "discovery service fail");
                            }
                            BleConnector.this.removeOperationCallback(this);
                        }
                    });
        } else {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR, "discover service fail");
        }
    }

    private boolean isConnectState(int newState) {
        return newState == BluetoothProfile.STATE_CONNECTED;
    }

    private BluetoothGattCharacteristic handleFindCharacteristic(
            String serviceUUID,
            String characteristicUUID,
            BluetoothGatt gatt,
            BleOperationCallback callback) {
        try {
            if (mConnectState != STATE_CONNECTED || gatt == null) {
                callback.onFail(BleConst.CODE_NO_CONNECTION, BleConst.MSG_NO_CONNECTION);
                return null;
            }
            BluetoothGattService service = gatt.getService(BleAdParser.string2UUID(serviceUUID));
            if (service == null) {
                callback.onFail(BleConst.CODE_NO_SERVICE, BleConst.MSG_NO_SERVICE);
                return null;
            }
            BluetoothGattCharacteristic chara =
                    service.getCharacteristic(BleAdParser.string2UUID(characteristicUUID));
            if (chara == null) {
                callback.onFail(BleConst.CODE_NO_CHARACTERISTIC, BleConst.MSG_NO_CHARACTERISTIC);
                return null;
            }
            return chara;
        } catch (IllegalArgumentException e) {
            callback.onFail(BleConst.CODE_SYSTEM_ERROR, e.getMessage());
            return null;
        }
    }

    private synchronized void addOperationCallback(BluetoothGattCallback callback) {
        mOperateCallback.add(callback);
    }

    private synchronized void removeOperationCallback(BluetoothGattCallback callback) {
        mOperateCallback.remove(callback);
    }
}
