/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.NfcB;

import org.hapjs.features.nfc.base.BaseTagTechInstance;

import java.io.IOException;

public class NfcBInstance extends BaseTagTechInstance {

    protected NfcB mNfcB;

    public NfcBInstance(NfcAdapter nfcAdapter, NfcB nfcB) {
        super(nfcAdapter, nfcB);
        this.mNfcB = nfcB;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mNfcB.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        // unsupport
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        return mNfcB.transceive(buffer);
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

}
