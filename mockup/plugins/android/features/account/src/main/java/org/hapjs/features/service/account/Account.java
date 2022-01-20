/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.account;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Account.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Account.ACTION_GET_PROVIDER, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Account.ACTION_AUTHORIZE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Account.ACTION_GET_PROFILE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Account.ACTION_IS_LOGIN, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Account.ACTION_GET_PHONE_NUMBER, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Account.ACTION_GET_ENCRYPTED_ID, mode = FeatureExtension.Mode.ASYNC)
        })
public class Account extends FeatureExtension {

    protected static final String FEATURE_NAME = "service.account";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    protected static final String ACTION_AUTHORIZE = "authorize";
    protected static final String ACTION_GET_PROFILE = "getProfile";
    protected static final String ACTION_IS_LOGIN = "isLogin";
    protected static final String ACTION_GET_PHONE_NUMBER = "getPhoneNumber";
    protected static final String ACTION_GET_ENCRYPTED_ID = "getEncryptedID";
    protected static final String PARAM_KEY_RESPONSE_TYPE = "type";
    protected static final String PARAM_KEY_REDIRECT_URI = "redirectUri";
    protected static final String PARAM_KEY_SCOPE = "scope";
    protected static final String PARAM_KEY_STATE = "state";
    protected static final String PARAM_KEY_TOKEN = "token";
    protected static final String RESULT_KEY_STATE = "state";
    protected static final String RESULT_KEY_CODE = "code";
    protected static final String RESULT_KEY_ACCESS_TOKEN = "accessToken";
    protected static final String RESULT_KEY_TOKEN_TYPE = "tokenType";
    protected static final String RESULT_KEY_EXPRIRESIN = "expiresIn";
    protected static final String RESULT_KEY_SCOPE = "scope";
    protected static final String RESULT_KEY_OPENID = "openid";
    protected static final String RESULT_KEY_ID = "id";
    protected static final String RESULT_KEY_NICKNAME = "nickname";
    protected static final String RESULT_KEY_AVATAR = "avatar";
    protected static final String RESULT_KEY_ENCRYPTED_ID = "encryptedid";
    private static final String TAG = "Account";
    private static final String PRARM_RESPONSE_TYPE_CODE = "code";
    private static final String PRARM_RESPONSE_TYPE_TOKEN = "token";
    private static final String PRARM_SCOPE＿TYPE_BASE = "scope.baseProfile";

    @Override
    public Response invokeInner(Request request) throws JSONException {
        if (ACTION_GET_PROVIDER.equals(request.getAction())) {
            return getProvider();
        } else if (ACTION_AUTHORIZE.equals(request.getAction())) {
            authorize(request);
        } else if (ACTION_GET_PROFILE.equals(request.getAction())) {
            getProfile(request);
        } else if (ACTION_IS_LOGIN.equals(request.getAction())) {
            isLogin(request);
        } else if (ACTION_GET_PHONE_NUMBER.equals(request.getAction())) {
            getPhoneNumber(request);
        } else if (ACTION_GET_ENCRYPTED_ID.equals(request.getAction())) {
            getEncryptedID(request);
        }
        return Response.SUCCESS;
    }

    protected Response getProvider() {
        return new Response("");
    }

    protected void isLogin(Request request) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isLogin", false);
            request.getCallback().callback(new Response(jsonObject));
        } catch (JSONException e) {
            request.getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "json exception!"));
        }
    }

    private void authorize(Request request) throws JSONException {
        String rawParams = request.getRawParams();
        JSONObject jsPara = new JSONObject(rawParams);
        String type = jsPara.optString(PARAM_KEY_RESPONSE_TYPE);
        String redirectUri = jsPara.optString(PARAM_KEY_REDIRECT_URI);
        String scope = jsPara.optString(PARAM_KEY_SCOPE);
        String state = jsPara.optString(PARAM_KEY_STATE);

        try {
            if (!PRARM_RESPONSE_TYPE_CODE.equals(type) && !PRARM_RESPONSE_TYPE_TOKEN.equals(type)) {
                request
                        .getCallback()
                        .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "unknown type"));
                return;
            }

            // success
            JSONObject result = new JSONObject();
            if (PRARM_RESPONSE_TYPE_CODE.equals(type)) {
                result.put(RESULT_KEY_CODE, "code").put(RESULT_KEY_STATE, "state");
            } else {
                result
                        .put(RESULT_KEY_ACCESS_TOKEN, "token")
                        .put(RESULT_KEY_STATE, "state")
                        .put(RESULT_KEY_TOKEN_TYPE, "type")
                        .put(RESULT_KEY_SCOPE, "scope")
                        .put(RESULT_KEY_EXPRIRESIN, "");
            }
            request.getCallback().callback(new Response(result));
        } catch (Exception e) {
            request.getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, e.getMessage()));
        }
    }

    private void getProfile(final Request request) throws JSONException {
        String rawParams = request.getRawParams();
        JSONObject jsPara = new JSONObject(rawParams);
        String token = jsPara.optString(PARAM_KEY_TOKEN);
        try {

            JSONObject avatar =
                    new JSONObject()
                            .put("90", "http://a.com/abc_90.jpg")
                            .put("120", "http://a.com/abc_120.jpg")
                            .put("360", "http://a.com/abc_360.jpg");

            JSONObject result =
                    new JSONObject()
                            .put(RESULT_KEY_OPENID, "openid")
                            .put(RESULT_KEY_ID, "id")
                            .put(RESULT_KEY_AVATAR, avatar)
                            .put(RESULT_KEY_NICKNAME, "nick");

            request.getCallback().callback(new Response(result));
        } catch (Exception e) {
            request.getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, e.getMessage()));
        }
    }

    protected void getPhoneNumber(Request request) {
        request
                .getCallback()
                .callback(new Response(Response.CODE_SERVICE_UNAVAILABLE,
                        "has no implement action!"));
    }

    private void getEncryptedID(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_KEY_ENCRYPTED_ID, "");
            request.getCallback().callback(new Response(result));
        } catch (Exception e) {
            request.getCallback().callback(Response.ERROR);
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
