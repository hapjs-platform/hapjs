/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.utils;

import android.bluetooth.BluetoothGattCharacteristic;

public class PropertyUtils {

    public static boolean canWrite(int properties) {
        return (properties
                & (BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE))
                != 0;
    }

    public static boolean canRead(int properties) {
        return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
    }

    public static boolean canNotify(int properties) {
        return (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    public static boolean canIndicate(int preperties) {
        return (preperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
    }
}
