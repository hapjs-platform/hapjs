/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.util.Base64;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.DigestUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@FeatureExtensionAnnotation(
        name = CipherFeature.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = CipherFeature.ACTION_RSA, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = CipherFeature.ACTION_AES, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = CipherFeature.ACTION_BASE64, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = CipherFeature.ACTION_CRC32, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = CipherFeature.ACTION_HASH, mode = FeatureExtension.Mode.SYNC),
        })
public class CipherFeature extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.cipher";

    protected static final String ACTION_RSA = "rsa";
    protected static final String ACTION_AES = "aes";
    protected static final String ACTION_BASE64 = "base64";
    protected static final String ACTION_CRC32 = "crc32";
    protected static final String ACTION_HASH = "hash";

    protected static final String PARAM_ACTION = "action";
    protected static final String PARAM_TEXT = "text";
    protected static final String PARAM_CONTENT = "content";
    protected static final String PARAM_KEY = "key";
    protected static final String PARAM_TRANSFORMATION = "transformation";
    protected static final String PARAM_IV = "iv";
    protected static final String PARAM_IV_LEN = "ivLen";
    protected static final String PARAM_IV_OFFSET = "ivOffset";
    protected static final String PARAM_ALGORITHM = "algorithm";

    protected static final String RESULT_TEXT = "text";
    private static final String TRANSFORMATION_DEFAULT = "RSA/None/OAEPwithSHA-256andMGF1Padding";
    private static final String AES_TRANSFORMATION_DEFAULT = "AES/CBC/PKCS5Padding";
    private static final String ACTION_ENCRYPT = "encrypt";
    private static final String ACTION_DECRYPT = "decrypt";
    private static final String ALGORITHM_HASH = "md5";
    private static final String ALGORITHM_SHA256 = "sha256";
    private static final int IV_LEN_DEFAULT = 16;

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        switch (action) {
            case ACTION_RSA:
                rsa(request);
                break;
            case ACTION_AES:
                aes(request);
                break;
            case ACTION_BASE64:
                base64(request);
                break;
            case ACTION_CRC32:
                crc32(request);
                break;
            case ACTION_HASH:
                return hash(request);
            default:
                return Response.NO_ACTION;
        }
        return Response.SUCCESS;
    }

    private Response hash(Request request) throws Exception {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param");
        }
        String content = jsonParams.getString(PARAM_CONTENT);
        String algorithm = jsonParams.getString(PARAM_ALGORITHM);
        String result;
        if (ALGORITHM_HASH.equals(algorithm)) {
            result = DigestUtils.getMd5(content.getBytes(StandardCharsets.UTF_8));
        } else if (ALGORITHM_SHA256.equals(algorithm)) {
            result = DigestUtils.getSha256(content.getBytes(StandardCharsets.UTF_8));
        } else {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid algorithm");
        }

        return new Response(result);
    }

    private void crc32(Request request) throws Exception {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param"));
            return;
        }
        String content = jsonParams.getString(PARAM_CONTENT);
        long result = DigestUtils.crc32(content.getBytes(StandardCharsets.UTF_8));
        JSONObject data = new JSONObject();
        data.put(PARAM_TEXT, result);
        request.getCallback().callback(new Response(data));
    }

    private void base64(Request request) throws Exception {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param"));
            return;
        }
        String action = jsonParams.getString(PARAM_ACTION);
        String text = jsonParams.getString(PARAM_TEXT);
        String result;
        if (ACTION_ENCRYPT.equals(action)) {
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            result = Base64.encodeToString(textBytes, Base64.DEFAULT);
        } else if (ACTION_DECRYPT.equals(action)) {
            byte[] textBytes = Base64.decode(text, Base64.DEFAULT);
            result = new String(textBytes, StandardCharsets.UTF_8);
        } else {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid action");
            request.getCallback().callback(response);
            return;
        }

        JSONObject data = new JSONObject();
        data.put(RESULT_TEXT, result);
        request.getCallback().callback(new Response(data));
    }

    private void rsa(Request request) throws Exception {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param"));
            return;
        }
        String action = jsonParams.getString(PARAM_ACTION);
        String text = jsonParams.getString(PARAM_TEXT);
        String key = jsonParams.getString(PARAM_KEY);
        String transformation = jsonParams.optString(PARAM_TRANSFORMATION, TRANSFORMATION_DEFAULT);

        String result;
        if (ACTION_ENCRYPT.equals(action)) {
            result = rsaEncrypt(text, key, transformation);
        } else if (ACTION_DECRYPT.equals(action)) {
            result = rsaDecrypt(text, key, transformation);
        } else {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid action");
            request.getCallback().callback(response);
            return;
        }

        JSONObject data = new JSONObject();
        data.put(RESULT_TEXT, result);
        request.getCallback().callback(new Response(data));
    }

    /**
     * Encrypt text with rsa algorithm
     *
     * @param text           the plain text to encrypt
     * @param key            the base64 encoded key
     * @param transformation the transformation
     * @return the base64 encoded value
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private String rsaEncrypt(String text, String key, String transformation)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, UnsupportedEncodingException, BadPaddingException,
            IllegalBlockSizeException {
        byte[] keyBytes = Base64.decode(key, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher cp = Cipher.getInstance(transformation);
        cp.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] textBytes = text.getBytes("UTF-8");
        byte[] resultBytes = cp.doFinal(textBytes);
        return Base64.encodeToString(resultBytes, Base64.DEFAULT);
    }

    /**
     * Decrypt text with rsa algorithm
     *
     * @param text           the base64 encoded value
     * @param key            the base64 encoded key
     * @param transformation the transformation
     * @return the decrypted plain text
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private String rsaDecrypt(String text, String key, String transformation)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, UnsupportedEncodingException, BadPaddingException,
            IllegalBlockSizeException {
        byte[] keyBytes = Base64.decode(key, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        Cipher cp = Cipher.getInstance(transformation);
        cp.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] textBytes = Base64.decode(text, Base64.DEFAULT);
        byte[] resultBytes = cp.doFinal(textBytes);
        return new String(resultBytes, "UTF-8");
    }

    private void aes(Request request) throws Exception {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid param"));
            return;
        }
        String action = jsonParams.getString(PARAM_ACTION);
        String text = jsonParams.getString(PARAM_TEXT);
        String key = jsonParams.getString(PARAM_KEY);
        String transformation =
                jsonParams.optString(PARAM_TRANSFORMATION, AES_TRANSFORMATION_DEFAULT);
        IvParameterSpec ivSpec = null;
        // ECB mode does not support IV
        if (!transformation.toUpperCase().contains("ECB")) {
            String iv = jsonParams.optString(PARAM_IV, key);
            int offset = jsonParams.optInt(PARAM_IV_OFFSET);
            int len = jsonParams.optInt(PARAM_IV_LEN, IV_LEN_DEFAULT);
            ivSpec = new IvParameterSpec(Base64.decode(iv, Base64.DEFAULT), offset, len);
        }
        byte[] keyBytes = Base64.decode(key, Base64.DEFAULT);

        String result;
        if (ACTION_ENCRYPT.equals(action)) {
            result = aesEncrypt(text, keyBytes, transformation, ivSpec);
        } else if (ACTION_DECRYPT.equals(action)) {
            result = aesDecrypt(text, keyBytes, transformation, ivSpec);
        } else {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid action");
            request.getCallback().callback(response);
            return;
        }

        JSONObject data = new JSONObject();
        data.put(RESULT_TEXT, result);
        request.getCallback().callback(new Response(data));
    }

    private String aesEncrypt(
            String text, byte[] keyBytes, String transformation, IvParameterSpec ivSpec)
            throws NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cp = Cipher.getInstance(transformation);
        if (ivSpec != null) {
            cp.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        } else {
            cp.init(Cipher.ENCRYPT_MODE, keySpec);
        }
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] resultBytes = cp.doFinal(textBytes);
        return Base64.encodeToString(resultBytes, Base64.DEFAULT);
    }

    private String aesDecrypt(
            String text, byte[] keyBytes, String transformation, IvParameterSpec ivSpec)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cp = Cipher.getInstance(transformation);
        if (ivSpec != null) {
            cp.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        } else {
            cp.init(Cipher.DECRYPT_MODE, keySpec);
        }
        byte[] textBytes = Base64.decode(text, Base64.DEFAULT);
        byte[] resultBytes = cp.doFinal(textBytes);
        return new String(resultBytes, StandardCharsets.UTF_8);
    }
}
