/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc.base;

import android.nfc.NfcAdapter;

import org.hapjs.bridge.InstanceManager;

abstract public class BaseInstance implements InstanceManager.IInstance {

    public static final String TAG = "NfcInstance";

    public NfcAdapter mNfcAdapter;

    public BaseInstance(NfcAdapter nfcAdapter) {
        this.mNfcAdapter = nfcAdapter;
    }

    public boolean isSupportNFC() {
        return mNfcAdapter != null;
    }

    public boolean isEnabled() {
        return isSupportNFC() && mNfcAdapter.isEnabled();
    }

}
