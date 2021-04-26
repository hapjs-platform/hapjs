/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc.base;

import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.util.Log;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.nfc.NFC;
import org.hapjs.features.nfc.NFCConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

abstract public class BaseTechInstance extends BaseInstance {

    protected TagTechnology mTech;

    public BaseTechInstance(NfcAdapter nfcAdapter, TagTechnology tech) throws NullPointerException {
        super(nfcAdapter);
        if (null != tech) {
            this.mTech = tech;
        } else {
            throw new NullPointerException("Null of TagTechnology");
        }
    }

    public void close(Request request) {
        try {
            mTech.close();
            if (null != request) {
                request.getCallback().callback(Response.SUCCESS);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to close", e);
            if (null != request) {
                request.getCallback().callback(new Response(NFCConstants.CODE_SYSTEM_INTERNAL_ERROR, NFCConstants.DESC_SYSTEM_INTERNAL_ERROR));
            }
        }
    }

    public void connect(Request request) {
        try {
            if (!mTech.isConnected()) {
                mTech.connect();
            }
            request.getCallback().callback(Response.SUCCESS);
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect", e);
            request.getCallback().callback(new Response(NFCConstants.CODE_CONNECT_FAILED, NFCConstants.DESC_CONNECT_FAILED));
        }
    }

    public void isConnected(Request request) throws JSONException{
        boolean isConnected = mTech.isConnected();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(NFC.RESULT_IS_CONNECTED, isConnected);
        request.getCallback().callback(new Response(jsonObject));
    }

    public void release() {
        close(null);
    }
}
