/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.internal.Util;
import okhttp3.internal.platform.Platform;
import org.hapjs.model.NetworkConfig;

public class OkHttpClientBuilderFactory {

    private static final String TAG = "OkHttpClientBuilderFac";

    private static final String CACHE_DIR = "http";
    private static final int MAX_CACHE_SIZE = 5 * 1024 * 1024; // 5M

    public static OkHttpClient.Builder create(Context context) {
        File cacheFile = new File(context.getCacheDir(), CACHE_DIR);
        Cache cache = new Cache(cacheFile, MAX_CACHE_SIZE);
        Dispatcher dispatcher = new Dispatcher();
        OkHttpClient.Builder builder =
                new OkHttpClient.Builder()
                        .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                        .cache(cache)
                        .addInterceptor(new BanNetworkInterceptor())
                        .dispatcher(dispatcher);
        setSslSocketFactoryIfNeed(builder);
        return builder;
    }

    /**
     * try enable all support tls protocol below api 20
     */
    private static void setSslSocketFactoryIfNeed(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            return;
        }

        X509TrustManager trustManager = Util.platformTrustManager();
        SSLSocketFactory sslSocketFactory = null;
        try {
            SSLContext sslContext = Platform.get().getSSLContext();
            sslContext.init(null, new TrustManager[] {trustManager}, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "create sslSocketFactory error", e);
        }
        if (sslSocketFactory != null) {
            ConnectionSpec tlsSpec =
                    new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .cipherSuites(sslSocketFactory.getSupportedCipherSuites())
                            .build();
            builder.connectionSpecs(Util.immutableList(tlsSpec, ConnectionSpec.CLEARTEXT));
            builder.sslSocketFactory(new TLSSocketFactory(sslSocketFactory), trustManager);
        }
    }
}
