/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.util.Log;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.json.JSONObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;

@FeatureExtensionAnnotation(
        name = Decode.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(
                        name = Decode.ACTION_DECODE,
                        mode = FeatureExtension.Mode.SYNC,
                        normalize = FeatureExtension.Normalize.RAW),
        })
public class Decode extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.decode";
    protected static final String ACTION_DECODE = "decode";
    protected static final String PARAMS_IGNORE_BOM = "ignoreBom";
    protected static final String PARAMS_FATAL = "fatal";
    protected static final String PARAMS_ENCODING = "encoding";
    protected static final String PARAMS_ARRAY_BUFFER = "arrayBuffer";
    protected static final String KEY_ERROR_CODE = "errorCode";
    protected static final String KEY_ERROR_MSG = "errorMsg";
    protected static final String KEY_RESULT = "result";
    protected static final String UTF_8 = "utf-8";
    protected static final String UTF_16 = "utf-16";
    protected static final String UTF_32 = "utf-32";
    protected static final int ERROR_CODE_TYPE_ERROR = Response.CODE_FEATURE_ERROR;
    private static final String TAG = "Decoder";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        if (ACTION_DECODE.equals(request.getAction())) {
            return decode(request);
        } else {
            return Response.NO_ACTION;
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Response decode(Request request) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        try {
            SerializeObject params = request.getSerializeParams();
            String encoding = params.optString(PARAMS_ENCODING, "UTF-8");
            boolean fatal = params.optBoolean(PARAMS_FATAL, false);
            boolean ignoreBom = params.optBoolean(PARAMS_IGNORE_BOM, false);
            TypedArray typedArray = params.optTypedArray(PARAMS_ARRAY_BUFFER);
            if (typedArray == null) {
                jsonObject.put(KEY_ERROR_CODE, ERROR_CODE_TYPE_ERROR);
                jsonObject.put(KEY_ERROR_MSG, "The encoded data was not valid.");
                return new Response(ERROR_CODE_TYPE_ERROR, jsonObject);
            }
            return decode(encoding, typedArray.getByteBuffer(), ignoreBom, fatal);
        } catch (Exception e) {
            Log.e(TAG, "params are not valid.", e);
            jsonObject.put(KEY_ERROR_CODE, Response.CODE_ILLEGAL_ARGUMENT);
            jsonObject.put(KEY_ERROR_MSG, "params are not valid.");
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, jsonObject);
        }
    }

    /**
     * Decode process.
     *
     * @param decodeScheme Decoding Scheme
     * @param buffer       A buffer which needs to be decoded
     * @param ignoreBom    Whether the data in the buffer should be ignored its BOM
     * @param fatal        Should any exception be posted to frontend
     * @return A response to frontend
     */
    private Response decode(String decodeScheme, ByteBuffer buffer, boolean ignoreBom,
                            boolean fatal)
            throws JSONException {
        JSONObject jsonObject = new JSONObject();
        try {
            Charset charset = Charset.forName(decodeScheme);
            CharsetDecoder decoder = charset.newDecoder();
            // 忽略BOM
            if (ignoreBom) {
                buffer = ignoreBom(buffer, decodeScheme);
            }
            CharBuffer charBuffer = decoder.decode(buffer);
            String decodedString = charBuffer.toString();
            jsonObject.put(KEY_RESULT, decodedString);
        } catch (CharacterCodingException ex) {
            Log.e(TAG, "The encoded data was not valid.", ex);
            if (fatal) {
                jsonObject.put(KEY_ERROR_CODE, ERROR_CODE_TYPE_ERROR);
                jsonObject.put(KEY_ERROR_MSG, "The encoded data was not valid.");
                return new Response(ERROR_CODE_TYPE_ERROR, jsonObject);
            }
        }
        return new Response(jsonObject);
    }

    /**
     * Ignore BOMs.
     *
     * @param buffer the buffer which should be decoded
     * @return A byte buffer which has ignored its BOM
     */
    public ByteBuffer ignoreBom(ByteBuffer buffer, String decodeScheme) {
        if (UTF_8.equalsIgnoreCase(decodeScheme)) {
            ignoreUtf8Bom(buffer);
        } else if (UTF_16.equalsIgnoreCase(decodeScheme)) {
            ignoreUtf16Bom(buffer);
        } else if (UTF_32.equalsIgnoreCase(decodeScheme)) {
            ignoreUtf32Bom(buffer);
        } else {
            Log.i(TAG, "Only unicode encoding scheme supports BOM.");
            return buffer;
        }
        byte[] bomIgnoredBuffer = new byte[buffer.remaining()];
        buffer.get(bomIgnoredBuffer, 0, bomIgnoredBuffer.length);
        return ByteBuffer.wrap(bomIgnoredBuffer);
    }

    /**
     * Ignore BOMs of UTF-32, common BOMs of UTF-32 (LE) and UTF-32 (BE) represented with hexadecimal
     * are "FF FE 00 00" and "00 00 FE FF" individually. In order to ignore BOMs, the start position
     * of buffer's reading should be 4.
     *
     * @param buffer the buffer which should be decoded
     */
    private void ignoreUtf32Bom(ByteBuffer buffer) {
        if (buffer.limit() > 4
                && (((buffer.get(0) == (byte) 0xFF)
                && (buffer.get(1) == (byte) 0xFE)
                && (buffer.get(2) == (byte) 0x00)
                && (buffer.get(3) == (byte) 0x00))
                || (buffer.get(0) == (byte) 0x00)
                && (buffer.get(1) == (byte) 0x00)
                && (buffer.get(2) == (byte) 0xFE)
                && (buffer.get(3) == (byte) 0xFF))) {
            buffer.position(4);
        }
    }

    /**
     * Ignore BOMs of UTF-8, common BOM of UTF-8 represented with hexadecimal is "EF BB BF".
     *
     * @param buffer the buffer which should be decoded
     */
    private void ignoreUtf8Bom(ByteBuffer buffer) {
        if (buffer.limit() > 3
                && (buffer.get(0) == (byte) 0xEF)
                && (buffer.get(1) == (byte) 0xBB)
                && (buffer.get(2) == (byte) 0xBF)) {
            buffer.position(3);
        }
    }

    /**
     * Ignore BOMs of UTF-16, common BOMs of UTF-16 (LE) and UTF-16 (BE) represented with hexadecimal
     * are "FF FE" and "FE FF" individually.
     *
     * @param buffer the buffer which should be decoded
     */
    private void ignoreUtf16Bom(ByteBuffer buffer) {
        if (buffer.limit() > 2
                && (((buffer.get(0) == (byte) 0xFF) && (buffer.get(1) == (byte) 0xFE))
                || ((buffer.get(0) == (byte) 0xFE) && (buffer.get(1) == (byte) 0xFF)))) {
            buffer.position(2);
        }
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }
}
