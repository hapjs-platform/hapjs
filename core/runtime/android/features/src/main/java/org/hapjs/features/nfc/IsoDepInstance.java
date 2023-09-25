/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.nfc.base.BaseTagTechInstance;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;

import java.io.IOException;
import java.nio.ByteBuffer;

public class IsoDepInstance extends BaseTagTechInstance {

    protected IsoDep mIsoDep;

    public IsoDepInstance(NfcAdapter nfcAdapter, IsoDep isoDep) {
        super(nfcAdapter, isoDep);
        this.mIsoDep = isoDep;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mIsoDep.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        mIsoDep.setTimeout(timeout);
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        return mIsoDep.transceive(buffer);
    }

    public void getHistoricalBytes(Request request) {
        byte[] historicalBytes = mIsoDep.getHistoricalBytes();
        JavaSerializeObject result = new JavaSerializeObject();
        result.put(NFC.RESULT_HISTORICAL_BYTES, historicalBytes);
        request.getCallback().callback(new Response(result));
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

}
