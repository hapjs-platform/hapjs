/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;

import org.hapjs.features.nfc.base.BaseTagTechInstance;

import java.io.IOException;

public class NfcFInstance extends BaseTagTechInstance {

    protected NfcF mNfcF;

    public NfcFInstance(NfcAdapter nfcAdapter, NfcF nfcF) {
        super(nfcAdapter, nfcF);
        this.mNfcF = nfcF;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mNfcF.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        mNfcF.setTimeout(timeout);
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        return mNfcF.transceive(buffer);
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

}
