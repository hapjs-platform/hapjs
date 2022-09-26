/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.NfcV;

import org.hapjs.features.nfc.base.BaseTagTechInstance;

import java.io.IOException;

public class NfcVInstance extends BaseTagTechInstance {

    protected NfcV mNfcV;

    public NfcVInstance(NfcAdapter nfcAdapter, NfcV nfcV) {
        super(nfcAdapter, nfcV);
        this.mNfcV = nfcV;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mNfcV.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        // unsupport
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        return mNfcV.transceive(buffer);
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

}
