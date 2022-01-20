/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.net;

import android.os.Looper;
import android.util.Log;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.common.utils.HmacUtils;
import org.hapjs.common.utils.ThreadUtils;

public class DefaultNetLoaderProviderImpl<T> implements NetLoaderProvider<T> {
    protected static final OkHttpClient sNormalOkHttpClient =
            new OkHttpClient()
                    .newBuilder()
                    .addInterceptor(
                            new Interceptor() {
                                @Override
                                public Response intercept(Chain chain) throws IOException {
                                    Map<String, String> headerParamsMap = new HashMap<>();
                                    headerParamsMap.put(
                                            "Content-Type",
                                            "application/x-www-form-urlencoded;charset=utf-8");
                                    Request request = chain.request();
                                    Request.Builder requestBuilder = request.newBuilder();
                                    // process header params inject
                                    Headers.Builder headerBuilder = request.headers().newBuilder();
                                    requestBuilder.headers(headerBuilder.build());
                                    if (headerParamsMap.size() > 0) {
                                        Iterator iterator = headerParamsMap.entrySet().iterator();
                                        while (iterator.hasNext()) {
                                            Map.Entry entry = (Map.Entry) iterator.next();
                                            headerBuilder.add((String) entry.getKey(),
                                                    (String) entry.getValue());
                                        }
                                        requestBuilder.headers(headerBuilder.build());
                                    }
                                    request = requestBuilder.build();
                                    return chain.proceed(request);
                                }
                            })
                    .build();
    private static final String TAG = "RpkInfoDataLoader";
    // product
    private static final String RPK_INFO_URL =
            "https://userapi.quickapp.cn/api/p/titlebar/queryRpkInfo";
    // product
    private static final String PARAM_SIGN_KEY = "f3fc96e7021311eaa2b30235d2b38928dfgbea";
    private static Map<String, String> mLocalParams = new HashMap<>();

    public DefaultNetLoaderProviderImpl() {
    }

    public static boolean isUiThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }

    // 将hashmap拼接到url上
    public static String addParams(String url, Map<String, String> params) {
        if (url != null && params != null) {
            StringBuilder sb = new StringBuilder(url);
            Set<Map.Entry<String, String>> entrys = params.entrySet();
            for (Map.Entry<String, String> entry : entrys) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null) {
                    continue;
                }
                try {
                    key = URLEncoder.encode(key, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (value != null) {
                    try {
                        value = URLEncoder.encode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                sb.append("&" + key + "=" + value);
            }
            String finalUrl = sb.toString();
            if (finalUrl.contains("?")) {
                return finalUrl;
            } else {
                return finalUrl.replaceFirst("&", "?");
            }
        } else {
            return url;
        }
    }

    public NetLoadResult<T> loadSync(String url, Map<String, String> params) {
        if (isUiThread()) {
            throw new RuntimeException("NetLoadResult loadSync can not be exec in ui thread!");
        }
        return loadDataSync(url, params);
    }

    @Override
    public String getMenubarUrl() {
        return RPK_INFO_URL;
    }

    @Override
    public int getMenubarPostType() {
        return NetLoaderProvider.METHOD_POST;
    }

    @Override
    public Map<String, String> getMenubarParams(Map<String, String> preParams) {
        String mRpkName = "";
        if (null != preParams && preParams.containsKey("rpkPackage")) {
            mRpkName = preParams.get("rpkPackage");
        }
        Map<String, String> params = new HashMap<>();
        params.putAll(preParams);
        StringBuilder message = new StringBuilder();
        params.put("rpkPackage", null != mRpkName ? mRpkName : "");
        message.append("rpkPackage");
        message.append("=");
        message.append(null != mRpkName ? mRpkName : "");
        message.append("&");
        long currentTime = System.currentTimeMillis();
        String timeStr = currentTime + "";
        params.put("timestamp", timeStr);
        message.append("timestamp");
        message.append("=");
        message.append(timeStr);
        String signStr = HmacUtils.sha256HMAC(message.toString(), PARAM_SIGN_KEY);
        params.put("sign", null != signStr ? signStr : "");
        return params;
    }

    @Override
    public void initPrepareParams(Map<String, String> preParams) {
        if (null != preParams) {
            mLocalParams.clear();
            // for others default params
            mLocalParams.putAll(preParams);
        }
    }

    @Override
    public void loadData(
            String baseUrl, Map<String, String> param, DataLoadedCallback<T> callback, int method) {
        loadNetData(baseUrl, param, callback, method);
    }

    @Override
    public NetLoadResult<T> loadDataSync(String baseUrl, Map<String, String> params) {
        return null;
    }

    protected void loadNetData(
            final String baseUrl,
            Map<String, String> params,
            final DataLoadedCallback<T> callback,
            int method) {
        if (params == null) {
            params = new HashMap<String, String>();
        }
        final String url;
        Request request = null;
        if (method == METHOD_POST) {
            url = baseUrl;
            FormBody.Builder builder = new FormBody.Builder();
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                builder.addEncoded(entry.getKey(),
                        entry.getValue() == null ? "" : entry.getValue());
            }
            request = new Request.Builder().url(url).post(builder.build()).build();
        } else {
            url = addParams(baseUrl, params);
            request = new Request.Builder().url(url).get().build();
        }

        NetworkReportManager.getInstance()
                .reportNetwork(NetworkReportManager.KEY_DEFAULT_NET_LOADER, url);
        Call call = sNormalOkHttpClient.newCall(request);
        call.enqueue(
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        NetLoadResult<T> loadResult = new NetLoadResult<T>();
                        loadResult.setResultCode(NetLoadResult.ERR_NETWORK);
                        loadResult.setException(e);
                        onResult(baseUrl, url, callback, loadResult);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        int responseCode = response.code();
                        NetLoadResult<T> loadResult = new NetLoadResult<T>();
                        loadResult.setResultCode(responseCode);
                        if (responseCode != 200) {
                            onResult(baseUrl, url, callback, loadResult);
                            return;
                        }

                        try {
                            String body = response.body().string();
                            loadResult.setOriginData(body);
                            loadResult.setResultCode(NetLoadResult.SUCCESS);
                        } catch (IOException e) {
                            loadResult.setException(e);
                            loadResult.setResultCode(NetLoadResult.ERR_IO);
                        } catch (Exception e) {
                            loadResult.setException(e);
                            loadResult.setResultCode(NetLoadResult.ERR_SERVER);
                        }
                        onResult(baseUrl, url, callback, loadResult);
                    }
                });
    }

    protected NetLoadResult<T> onResult(
            String baseUrl,
            String url,
            final DataLoadedCallback<T> callback,
            final NetLoadResult<T> loadResult) {
        final int resultCode = loadResult.getResultCode();
        Exception exception = loadResult.getException();
        if (exception == null) {
            Log.i(TAG, "onResult, resultCode = " + resultCode);
        } else {
            Log.e(TAG, "onResult, resultCode = " + resultCode + "  message : "
                    + exception.getMessage());
        }
        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            if (resultCode != NetLoadResult.SUCCESS) {
                                callback.onFailure(loadResult);
                            } else {
                                callback.onSuccess(loadResult);
                            }
                        }
                    }
                });
        return loadResult;
    }
}
