/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.nfc.base.BaseTagTechInstance;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class NfcAInstance extends BaseTagTechInstance {

    protected NfcA mNfcA;

    public NfcAInstance(NfcAdapter nfcAdapter, NfcA nfcA) {
        super(nfcAdapter, nfcA);
        this.mNfcA = nfcA;
    }

    @Override
    public int getMaxTransceiveLength() {
        return mNfcA.getMaxTransceiveLength();
    }

    @Override
    public void setTimeout(int timeout) {
        mNfcA.setTimeout(timeout);
    }

    @Override
    public byte[] transceive(byte[] buffer) throws IOException {
        return mNfcA.transceive(buffer);
    }

    public void getAtqa(Request request) {
        byte[] atqa = mNfcA.getAtqa();
        SerializeObject resultObj = new JavaSerializeObject();
        resultObj.put(NFC.RESULT_ATQA, new ArrayBuffer(atqa));
        request.getCallback().callback(new Response(resultObj));
    }

    public void getSak(Request request) throws JSONException {
        short sak = mNfcA.getSak();
        JSONObject resultObj = new JSONObject();
        resultObj.put(NFC.RESULT_SAK, sak);
        request.getCallback().callback(new Response(resultObj));
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }
}
