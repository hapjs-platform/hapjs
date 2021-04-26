/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.Ndef;
import android.util.Log;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.nfc.base.BaseTechInstance;
import org.hapjs.render.jsruntime.serialize.SerializeArray;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NdefInstance extends BaseTechInstance {

    private Ndef mNdef;

    public NdefInstance(NfcAdapter nfcAdapter, Ndef ndef) {
        super(nfcAdapter, ndef);
        this.mNdef = ndef;
    }

    @Override
    public String getFeatureName() {
        return NFC.FEATURE_NAME;
    }

    public void writeNdefMessage(Request request) {
        try {
            if (!mNdef.isConnected()) {
                request.getCallback().callback(new Response(NFCConstants.CODE_TECH_HAS_NOT_CONNECTED,
                        NFCConstants.DESC_TECH_HAS_NOT_CONNECTED));
                return;
            }
            SerializeObject params = request.getSerializeParams();
            SerializeArray arrayUris = params.optSerializeArray(NFC.PARAM_URIS);
            SerializeArray arrayTexts = params.optSerializeArray(NFC.PARAM_TEXTS);
            SerializeArray arrayRecords = params.optSerializeArray(NFC.PARAM_RECORDS);
            NdefMessage ndefMessage = null;

            if (null != arrayUris && arrayUris.length() > 0) {
                ndefMessage = createUriNdefMessage(arrayUris);

            } else if (null != arrayTexts && arrayTexts.length() > 0) {
                ndefMessage = createTextNdefMessage(arrayTexts);

            } else if (null != arrayRecords && arrayRecords.length() > 0) {
                ndefMessage = createBufferNdefMessage(arrayRecords);
            }
            if (null != ndefMessage) {
                if (mNdef.isWritable()) {
                    int size = ndefMessage.toByteArray().length;
                    if (size <= mNdef.getMaxSize()) {
                        mNdef.writeNdefMessage(ndefMessage);
                        request.getCallback().callback(Response.SUCCESS);
                    } else {
                        request.getCallback().callback(new Response(NFCConstants.CODE_INSUFFICIENT_STORAGE_CAPACITY,
                                NFCConstants.DESC_INSUFFICIENT_STORAGE_CAPACITY));
                    }
                } else {
                    request.getCallback().callback(new Response(NFCConstants.CODE_FUNCTION_NOT_SUPPORT,
                            NFCConstants.DESC_FUNCTION_NOT_SUPPORT));
                }
            } else {
                request.getCallback().callback(new Response(NFCConstants.CODE_PARSE_NDEF_MESSAGE_FAILED,
                        NFCConstants.DESC_PARSE_NDEF_MESSAGE_FAILED));
            }
        } catch (FormatException e) {
            Log.e(TAG, "Format error", e);
            request.getCallback().callback(new Response(NFCConstants.CODE_INVALID_PARAMETER, NFCConstants.DESC_INVALID_PARAMETER));
        } catch (IOException e) {
            Log.e(TAG, "Failed to write ndef message", e);
            request.getCallback().callback(new Response(NFCConstants.CODE_SYSTEM_INTERNAL_ERROR, NFCConstants.DESC_SYSTEM_INTERNAL_ERROR));
        } catch (Exception e) {
            Log.e(TAG, "Failed to write ndef message", e);
            request.getCallback().callback(new Response(NFCConstants.CODE_UNKNOWN_ERROR, NFCConstants.DESC_UNKNOWN_ERROR));
        }
    }

    private NdefMessage createUriNdefMessage(SerializeArray arrayUris) {
        if (null != arrayUris && arrayUris.length() > 0) {
            NdefRecord[] recordArray = new NdefRecord[arrayUris.length()];
            for (int i = 0; i < arrayUris.length(); i++) {
                try {
                    String uriStr = arrayUris.getString(i);
                    NdefRecord record = NdefRecord.createUri(uriStr);
                    recordArray[i] = record;
                } catch (IllegalArgumentException | NullPointerException | SerializeException e) {
                    Log.e(TAG, "Failed to parse params.", e);
                    return null;
                }
            }
            return new NdefMessage(recordArray);
        }
        return null;
    }

    private NdefMessage createTextNdefMessage(SerializeArray arrayTexts) {
        if (null != arrayTexts && arrayTexts.length() > 0) {
            NdefRecord[] recordArray = new NdefRecord[arrayTexts.length()];
            for (int i = 0; i < arrayTexts.length(); i++) {
                try {
                    String text = arrayTexts.getString(i);
                    NdefRecord record = NdefRecord.createTextRecord(StandardCharsets.UTF_8.name(), text);
                    recordArray[i] = record;
                } catch (IllegalArgumentException | NullPointerException | SerializeException e) {
                    Log.e(TAG, "failed to parse params.", e);
                    return null;
                }
            }
            return new NdefMessage(recordArray);
        }
        return null;
    }

    private NdefMessage createBufferNdefMessage(SerializeArray arrayRecords) {
        if (null != arrayRecords && arrayRecords.length() > 0) {
            NdefRecord[] recordArray = new NdefRecord[arrayRecords.length()];
            for (int i = 0; i < arrayRecords.length(); i++) {
                try {
                    SerializeObject obj = arrayRecords.getSerializeObject(i);
                    short tnf = (short) obj.getInt(NFC.PARAM_RECORDS_TNF);

                    ArrayBuffer typeArrayBuffer = obj.getArrayBuffer(NFC.PARAM_RECORDS_TYPE);
                    ByteBuffer typeByteBuffer = typeArrayBuffer.getByteBuffer();
                    byte[] type = new byte[typeByteBuffer.remaining()];
                    typeByteBuffer.get(type);

                    ArrayBuffer idArrayBuffer = obj.getArrayBuffer(NFC.PARAM_RECORDS_ID);
                    ByteBuffer idByteBuffer = idArrayBuffer.getByteBuffer();
                    byte[] id = new byte[idByteBuffer.remaining()];
                    idByteBuffer.get(id);

                    ArrayBuffer payloadArrayBuffer = obj.getArrayBuffer(NFC.PARAM_RECORDS_PAYLOAD);
                    ByteBuffer payloadByteBuffer = payloadArrayBuffer.getByteBuffer();
                    byte[] payload = new byte[payloadByteBuffer.remaining()];
                    payloadByteBuffer.get(payload);

                    recordArray[i] = new NdefRecord(tnf, type, id, payload);
                } catch (IllegalArgumentException | NullPointerException | SerializeException e) {
                    Log.e(TAG, "failed to parse params.", e);
                    return null;
                }
            }
            return new NdefMessage(recordArray);
        }
        return null;
    }
}
