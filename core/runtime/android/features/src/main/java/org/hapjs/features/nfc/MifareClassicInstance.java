/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;

import org.hapjs.features.nfc.base.BaseTagTechInstance;

import java.io.IOException;

public class MifareClassicInstance extends BaseTagTechInstance {

    protected static final byte CMD_READ_BLOCK = (byte) 0x30;

    protected static final byte CMD_WRITE_BLOCK = (byte) 0xA0;

    protected static final byte CMD_AUTHENTICATE_A = (byte) 0x60;

    protected static final byte CMD_AUTHENTICATE_B = (byte) 0x61;

    protected static final byte CMD_INCREMENT_BLOCK = (byte) 0xC1;

    protected static final byte CMD_DECREMENT_BLOCK = (byte) 0xC0;

    protected static final byte CMD_RESTORE_BLOCK = (byte) 0xC2;

    protected static final byte CMD_TRANSFER_BLOCK = (byte) 0xB0;

    protected MifareClassic mMifareClassic;

    public MifareClassicInstance(NfcAdapter nfcAdapter, MifareClassic mifareClassic) {
        super(nfcAdapter, mifareClassic);
        this.mMifareClassic = mifareClassic;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mMifareClassic.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        mMifareClassic.setTimeout(timeout);
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        byte cmd = buffer[0];
        if (cmd == CMD_READ_BLOCK
                || cmd == CMD_WRITE_BLOCK
                || cmd == CMD_AUTHENTICATE_A
                || cmd == CMD_AUTHENTICATE_B
                || cmd == CMD_INCREMENT_BLOCK
                || cmd == CMD_DECREMENT_BLOCK
                || cmd == CMD_RESTORE_BLOCK
                || cmd == CMD_TRANSFER_BLOCK
        ) {
            return transceive(mMifareClassic, buffer, false);
        } else {
            return mMifareClassic.transceive(buffer);
        }
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

}
