/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.CacheControl;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hapjs.debugger.DebuggerApplication;

public class HttpUtils {
    public static final String LOCAL_SERVER = "http://127.0.0.1:12306";
    public static final String HEADER_SERIAL_NUMBER = "device-serial-number";
    public static final String PATH_BUNDLE = "/bundle";
    public static final String PATH_NOTIFY = "/notify";
    public static final String PATH_SEARCH_SN = "/searchsn";
    private static final String TAG = "Fetcher";
    private static final String PLATFORM_VERSION = "platformVersion";

    private static final long READ_TIMEOUT = 30_000;
    private static final long WRITE_TIMEOUT = 30_000;

    private static final OkHttpClient CLIENT = createOkHttpClient();

    public static OkHttpClient getOkHttpClient() {
        return CLIENT;
    }

    public static boolean downloadFile(String url, File destFile) {
        Response response = get(url);
        if (response == null) {
            return false;
        } else {
            InputStream inputStream = response.body().byteStream();
            try {
                return FileUtils.saveToFile(inputStream, destFile);
            } finally {
                FileUtils.closeQuietly(inputStream);
            }
        }
    }

    public static boolean searchSerialNumber(Context context) {
        Response response = get(getSerialNumberUrl(context));
        return response != null;
    }

    public static Response get(String url) {
        Request.Builder request = new okhttp3.Request.Builder()
                .url(url)
                .method("GET", null)
                .cacheControl(CacheControl.FORCE_NETWORK);
        if (PreferenceUtils.isUseADB(DebuggerApplication.getInstance())) {
            request.header(HEADER_SERIAL_NUMBER, AppUtils.getSerialNumber());
        }
        try {
            Response response = CLIENT.newCall(request.build()).execute();
            if (response.code() != 200) {
                Log.w(TAG, "Invalid response: code=" + response.code());
                return null;
            } else {
                return response;
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to get: " + url, e);
            return null;
        }
    }

    public static String getUpdateUrl(Context context) {
        String server = getDebugServer(context);
        if (TextUtils.isEmpty(server)) {
            return "";
        } else if (PreferenceUtils.isUniversalScan(context)) {
            return server;
        } else {
            int version = getPlatformVersion(context);
            if (version < 1040) {
                return server + PATH_BUNDLE;
            } else {
                return server + PATH_BUNDLE + "?" + PLATFORM_VERSION + "=" + version;
            }
        }
    }

    /**
     * 获取update请求URL
     *
     * @param context
     * @return
     */
    public static String getNotifyUrl(Context context) {
        String server = getDebugServer(context);
        if (TextUtils.isEmpty(server)) {
            return "";
        } else {
            return server + PATH_NOTIFY;
        }
    }

    public static String getSerialNumberUrl(Context context) {
        String server = getDebugServer(context);
        if (TextUtils.isEmpty(server)) {
            return "";
        } else {
            return server + PATH_SEARCH_SN;
        }
    }

    public static String getDebugServer(Context context) {
        String server = PreferenceUtils.isUseADB(context) ?
                HttpUtils.LOCAL_SERVER : PreferenceUtils.getServer(context);
        if (TextUtils.isEmpty(server) && PreferenceUtils.isUseAnalyzer(context)) {
            server = HttpUtils.LOCAL_SERVER;
        }
        return server;
    }

    private static int getPlatformVersion(Context context) {
        String pkg = PreferenceUtils.getPlatformPackage(context);
        if (!TextUtils.isEmpty(pkg)) {
            try {
                ApplicationInfo appInfo = context.getPackageManager()
                        .getApplicationInfo(pkg, PackageManager.GET_META_DATA);
                return appInfo.metaData.getInt(PLATFORM_VERSION);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Platform not found: ", e);
            }
        }
        return 0;
    }

    private static OkHttpClient createOkHttpClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(3);
        dispatcher.setMaxRequestsPerHost(2);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher);
        return builder.build();
    }

    public static UrlEntity parse(String url) {
        if (TextUtils.isEmpty(url) || "".equals(url.trim())) {
            return null;
        }
        UrlEntity entity = new UrlEntity();
        url = url.trim();
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 0) {
            entity.baseUrl = urlParts[0];
        } else {
            return null;
        }
        // 没有参数
        if (urlParts.length == 1) {
            return entity;
        }
        // 有参数
        String[] params = urlParts[1].split("&");
        entity.params = new HashMap<>();
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                entity.params.put(keyValue[0], keyValue[1]);
            }
        }
        return entity;
    }

    public static class UrlEntity {
        public String baseUrl;
        public Map<String, String> params;
    }
}
