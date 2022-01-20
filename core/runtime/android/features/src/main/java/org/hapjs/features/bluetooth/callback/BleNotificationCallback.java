/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.callback;

public interface BleNotificationCallback {
    void onCharacteristicChanged(
            String address, String serviceUUID, String characteristicUUID, byte[] data);
}
