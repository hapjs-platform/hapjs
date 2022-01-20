/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hapjs.model.NetworkConfig;
import org.hapjs.runtime.Runtime;

public class HttpConfig {
    private static final String TAG = "HttpConfig";
    private static volatile boolean sIsAssertionErrorPreAvoid = false;
    private volatile OkHttpClient mOkHttpClient;
    private NetworkInterceptorProxy mNetworkInterceptorProxy;

    private HttpConfig() {
        mNetworkInterceptorProxy = new NetworkInterceptorProxy();
    }

    public static HttpConfig get() {
        return Holder.INSTANCE;
    }

    // pre execute to avoid AssertionError
    private static void preExecuteToAvoidError() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O
                || Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
            if (!sIsAssertionErrorPreAvoid) {
                synchronized (HttpConfig.class) {
                    if (!sIsAssertionErrorPreAvoid) {
                        try {
                            new Date().toString();
                        } catch (Exception | Error e) {
                            Log.e(TAG, "execute Date().toString() failed.", e);
                        }
                        sIsAssertionErrorPreAvoid = true;
                    }
                }
            }
        }
    }

    public OkHttpClient getOkHttpClient() {
        if (mOkHttpClient == null) {
            synchronized (this) {
                if (mOkHttpClient == null) {
                    mOkHttpClient =
                            createOkHttpClientBuilder(Runtime.getInstance().getContext()).build();
                }
            }
        }
        return mOkHttpClient;
    }

    public void setNetworkInterceptor(Interceptor interceptor) {
        mNetworkInterceptorProxy.setBase(interceptor);
    }

    private OkHttpClient.Builder createOkHttpClientBuilder(Context context) {
        OkHttpClient.Builder builder = OkHttpClientBuilderFactory.create(context);
        builder.addInterceptor(new InterceptorImpl());
        builder.addNetworkInterceptor(mNetworkInterceptorProxy);
        return builder;
    }

    public void onConfigChange(NetworkConfig networkConfig) {
        synchronized (this) {
            long connectTimeout = networkConfig.getConnectTimeout();
            long readTimeout = networkConfig.getReadTimeout();
            long writeTimeout = networkConfig.getWriteTimeout();

            if (mOkHttpClient != null
                    && connectTimeout == mOkHttpClient.connectTimeoutMillis()
                    && readTimeout == mOkHttpClient.readTimeoutMillis()
                    && writeTimeout == mOkHttpClient.writeTimeoutMillis()) {
                return;
            }

            OkHttpClient.Builder builder =
                    createOkHttpClientBuilder(Runtime.getInstance().getContext());
            builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            builder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
            mOkHttpClient = builder.build();
        }
    }

    private static class Holder {
        static final HttpConfig INSTANCE = new HttpConfig();
    }

    private static class InterceptorImpl implements Interceptor {
        private static final String HEADER_USER_AGENT = "User-Agent";
        private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

        @Override
        public Response intercept(Chain chain) throws IOException {
            preExecuteToAvoidError();
            try {
                Request request = chain.request();
                okhttp3.Request.Builder requestBuilder = request.newBuilder();
                if (chain.request().header(HEADER_USER_AGENT) == null) {
                    String userAgent;
                    RequestTag requestTag = request.tag(RequestTag.class);
                    if (null != requestTag) {
                        userAgent =
                                UserAgentHelper.getFullWebkitUserAgent(requestTag.getPackageName());
                    } else {
                        userAgent = UserAgentHelper.getFullWebkitUserAgent();
                    }
                    requestBuilder.addHeader(HEADER_USER_AGENT, userAgent);
                }
                if (chain.request().header(HEADER_ACCEPT_LANGUAGE) == null) {
                    String acceptLanguage = AcceptLanguageUtils.getAcceptLanguage();
                    if (!TextUtils.isEmpty(acceptLanguage)) {
                        requestBuilder.addHeader(HEADER_ACCEPT_LANGUAGE, acceptLanguage);
                    }
                }
                return chain.proceed(requestBuilder.build());
            } catch (RuntimeException e) {
                // catch runtime exception throw by okhttp when uri has syntax exception avoid crash
                throw new IOException(e);
            } catch (AssertionError e) {
                // catch AssertionError throw by okhttp
                throw new IOException(e);
            }
        }
    }

    private static class NetworkInterceptorProxy implements Interceptor {
        private Interceptor mBase;

        public void setBase(Interceptor interceptor) {
            mBase = interceptor;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Interceptor base = mBase;
            if (base == null) {
                return chain.proceed(chain.request());
            } else {
                return base.intercept(chain);
            }
        }
    }
}
