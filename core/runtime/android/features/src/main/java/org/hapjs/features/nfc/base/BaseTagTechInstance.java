/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc.base;

import android.nfc.NfcAdapter;
import android.nfc.TagLostException;
import android.nfc.tech.TagTechnology;
import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.utils.ReflectUtils;
import org.hapjs.features.nfc.NFC;
import org.hapjs.features.nfc.NFCConstants;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

abstract public class BaseTagTechInstance extends BaseTechInstance {

    private static final String CLASS_NAME_BASIC_TAG_TECHNOLOGY = "android.nfc.tech.BasicTagTechnology";

    private static final String METHOD_NAME_TRANSCEIVE = "transceive";

    public BaseTagTechInstance(NfcAdapter nfcAdapter, TagTechnology tech) {
        super(nfcAdapter, tech);
    }

    abstract public int getMaxTransceiveLength();

    public void getMaxTransceiveLength(Request request) throws JSONException{
        int maxTransceiveLength = getMaxTransceiveLength();
        JSONObject resultObj = new JSONObject();
        resultObj.put(NFC.RESULT_MAX_TRANSCEIVE_LENGTH, maxTransceiveLength);
        request.getCallback().callback(new Response(resultObj));
    }

    abstract public void setTimeout(int timeout) throws IllegalArgumentException;

    public void setTimeout(Request request) throws JSONException {
        try {
            JSONObject params = request.getJSONParams();
            int timeout = params.optInt(NFC.PARAM_TIMEOUT, NFC.DEFAULT_TIMEOUT);
            if (timeout > 0) {
                setTimeout(timeout);
                request.getCallback().callback(Response.SUCCESS);
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_INVALID_PARAMETER, NFCConstants.DESC_INVALID_PARAMETER));
            }
        } catch (IllegalArgumentException e) {
            request.getCallback().callback(new Response(NFCConstants.CODE_INVALID_PARAMETER, NFCConstants.DESC_INVALID_PARAMETER));
        }
    }

    abstract public byte[] transceive(byte[] buffer) throws IOException;

    public void transceive(Request request) {
        if (mTech.isConnected()) {
            ArrayBuffer value = null;
            try {
                SerializeObject params = request.getSerializeParams();
                Object dataObj = params.opt(NFC.PARAM_TRANSCEIVE_DATA);
                if (dataObj instanceof ArrayBuffer) {
                    value = (ArrayBuffer) params.get(NFC.PARAM_TRANSCEIVE_DATA);
                } else {
                    Log.w(TAG, "Unsupport type: " + dataObj.getClass().getSimpleName());
                }
            } catch (Exception e) {
                request.getCallback().callback(new Response(NFCConstants.CODE_INVALID_PARAMETER, NFCConstants.DESC_INVALID_PARAMETER));
                return;
            }
            try {
                if (null != value) {
                    ByteBuffer byteBuffer = value.getByteBuffer();
                    byte[] transceiveParams = new byte[byteBuffer.remaining()];
                    byteBuffer.get(transceiveParams);

                    byte[] resultBytes = transceive(transceiveParams);
                    SerializeObject resultObj = new JavaSerializeObject();
                    ArrayBuffer resultBuffer = new ArrayBuffer(resultBytes);
                    resultObj.put(NFC.RESULT_TRANSCEIVE_DATA, resultBuffer);
                    request.getCallback().callback(new Response(resultObj));
                } else {
                    request.getCallback().callback(new Response(NFCConstants.CODE_INVALID_PARAMETER, NFCConstants.DESC_INVALID_PARAMETER));
                }
            } catch (TagLostException e) {
                request.getCallback().callback(new Response(NFCConstants.CODE_NO_DISCOVERED_TAG, NFCConstants.DESC_NO_DISCOVERED_TAG));
            } catch (IOException e) {
                request.getCallback().callback(new Response(NFCConstants.CODE_SYSTEM_INTERNAL_ERROR, NFCConstants.DESC_SYSTEM_INTERNAL_ERROR));
            } catch (Exception e) {
                request.getCallback().callback(new Response(NFCConstants.CODE_UNKNOWN_ERROR, NFCConstants.DESC_UNKNOWN_ERROR));
            }
        } else {
            request.getCallback().callback(new Response(NFCConstants.CODE_TECH_HAS_NOT_CONNECTED, NFCConstants.DESC_TECH_HAS_NOT_CONNECTED));
        }
    }

    protected byte[] transceive(Object receiver, byte[] buffer, boolean raw) {
        return (byte[]) ReflectUtils.invokeDeclaredMethod(CLASS_NAME_BASIC_TAG_TECHNOLOGY, receiver,
                METHOD_NAME_TRANSCEIVE, new Class[]{byte[].class, boolean.class},
                new Object[]{buffer, raw});
    }
}
