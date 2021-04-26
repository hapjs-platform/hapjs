/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.MifareUltralight;

import org.hapjs.features.nfc.base.BaseTagTechInstance;

import java.io.IOException;

public class MifareUltralightInstance extends BaseTagTechInstance {

    private static final byte CMD_READ_PAGE = (byte) 0x30;

    private static final byte CMD_WRITE_PAGE = (byte) 0xA2;

    protected MifareUltralight mMifareUltralight;

    public MifareUltralightInstance(NfcAdapter nfcAdapter, MifareUltralight mifareUltralight) {
        super(nfcAdapter, mifareUltralight);
        this.mMifareUltralight = mifareUltralight;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mMifareUltralight.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        mMifareUltralight.setTimeout(timeout);
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        if (buffer[0] == CMD_READ_PAGE || buffer[0] == CMD_WRITE_PAGE) {
            return transceive(mMifareUltralight, buffer, false);

        } else {
            return mMifareUltralight.transceive(buffer);
        }
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

}
