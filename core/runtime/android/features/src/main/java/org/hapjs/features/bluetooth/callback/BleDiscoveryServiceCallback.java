/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.callback;

import android.bluetooth.BluetoothGatt;

public abstract class BleDiscoveryServiceCallback extends BleOperationCallback {
    public abstract void onServicesDiscovery(BluetoothGatt gatt);
}
