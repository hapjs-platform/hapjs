/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.scan;

import android.bluetooth.BluetoothAdapter;
import java.util.UUID;
import org.hapjs.bridge.Response;
import org.hapjs.features.bluetooth.BleManager;
import org.hapjs.features.bluetooth.data.BleConst;
import org.hapjs.features.bluetooth.data.ScanOperateResult;

public class BleScanner {
    private boolean mScanning = false;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    private BleScanner() {
    }

    public static BleScanner getInstance() {
        return BleScannerHolder.sBleScanner;
    }

    public synchronized ScanOperateResult startLeScan(
            BluetoothAdapter.LeScanCallback callback, UUID[] uuids) {
        BluetoothAdapter adapter = BleManager.getInstance().getBluetoothAdapter();
        if (!adapter.isEnabled()) {
            return new ScanOperateResult(BleConst.CODE_NOT_AVAILABLE, BleConst.MSG_NOT_AVAILABLE);
        }
        if (mScanning) {
            return new ScanOperateResult(Response.CODE_GENERIC_ERROR, "scan is already started");
        }
        mLeScanCallback = callback;
        if (adapter.startLeScan(uuids, callback)) {
            mScanning = true;
            return BleConst.SCAN_SUCCESS;
        } else {
            mScanning = false;
            return new ScanOperateResult(BleConst.CODE_SYSTEM_ERROR, "scan start fail");
        }
    }

    public synchronized void stopLeScan() {
        mScanning = false;
        BluetoothAdapter adapter = BleManager.getInstance().getBluetoothAdapter();
        adapter.stopLeScan(mLeScanCallback);
        mLeScanCallback = null;
    }

    public synchronized boolean isScanning() {
        return mScanning;
    }

    private static class BleScannerHolder {
        private static final BleScanner sBleScanner = new BleScanner();
    }
}
