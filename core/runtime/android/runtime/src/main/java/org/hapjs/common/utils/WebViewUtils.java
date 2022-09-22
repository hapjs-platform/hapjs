/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import java.io.File;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class WebViewUtils {

    private static final String TAG = "WebViewUtils";
    private static boolean sHasSetDataDirectory = false;

    /**
     * Android P之后不允许多进程同时访问同一个webview目录
     *
     * <p>https://developer.android.com/about/versions/pie/android-9.0-changes-28#web-data-dirs
     */
    public static void setDataDirectory(String pkg) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || sHasSetDataDirectory) {
            return;
        }
        try {
            // WebView 初始化后不允许再调用
            WebView.setDataDirectorySuffix(pkg);
            sHasSetDataDirectory = true;
        } catch (Exception e) {
            Log.e(TAG, "setDataDirectory failed!", e);
        }
    }

    /**
     * 获取 WebView 数据目录
     *
     * <p>/data/data/com.xxx.hybrid/app_webview_{pkg}
     */
    @RequiresApi(Build.VERSION_CODES.P)
    public static File getWebViewData(Context context, String pkg) {
        return context.getDir("webview_" + pkg, Context.MODE_PRIVATE);
    }

    /**
     * 获取 WebView 缓存目录
     *
     * <p>/data/data/com.xxx.hybrid/cache/webview_{pkg}
     */
    @RequiresApi(Build.VERSION_CODES.P)
    public static File getWebViewCache(Context context, String pkg) {
        return new File(context.getCacheDir(), "webview_" + pkg);
    }

    public static Uri[] getFileUriList(Intent data) {
        if (data == null) {
            return null;
        }
        Uri[] resultFileList = null;

        // from stream
        ArrayList<Uri> uris = data.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (uris != null && uris.size() != 0) {
            resultFileList = new Uri[uris.size()];
            uris.toArray(resultFileList);
            return resultFileList;
        }

        // from files
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            resultFileList = new Uri[clipData.getItemCount()];
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                if (item == null) {
                    continue;
                }
                resultFileList[i] = item.getUri();
            }
        } else if (data.getData() != null) {
            resultFileList = new Uri[]{data.getData()};
        }
        return resultFileList;
    }

    /**
     * 检查WebView与H5通信的安全性
     * @param webView
     * @param url
     * @param trustedUrls
     * @param listener
     */
    public static void checkHandleMessage(WebView webView, String url, ArraySet<String> trustedUrls, UrlCheckListener listener) {
        if (webView == null || TextUtils.isEmpty(url) || listener == null || trustedUrls == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < trustedUrls.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(trustedUrls.valueAt(i));
        }
        builder.append(']');
        final String trustedUrlsStr = builder.toString();
        webView.post(() -> {
            webView.evaluateJavascript(getCheckJsStr(url, trustedUrlsStr), value -> {
                if (Boolean.TRUE.toString().equals(value)) {
                    listener.onTrusted();
                } else {
                    checkDecodeUrl(webView, url, trustedUrlsStr, listener);
                }
            });
        });
    }

    private static void checkDecodeUrl(WebView webView, String url, String trustedUrlsStr, UrlCheckListener listener) {
        if (webView == null || TextUtils.isEmpty(url)
                || TextUtils.isEmpty(trustedUrlsStr)) {
            if (null != listener) {
                listener.onUnTrusted();
            } else {
                Log.e(TAG, "checkDecodeUrl listener null");
            }
            return;
        }
        final String decodeUrl = decodeUrl(url);
        webView.post(() -> {
            webView.evaluateJavascript(getCheckJsStr(url, decodeUrl), value -> {
                if (Boolean.TRUE.toString().equals(value)) {
                    listener.onTrusted();
                } else {
                    listener.onUnTrusted();
                }
            });
        });
    }

    private static String getCheckJsStr(String url, String trustedUrls) {
        return "javascript:" +
                "function checkUrl (url, trustedUrl) {\n" +
                "  return trustedUrl.some(function(item) {\n" +
                "    if (typeof item === 'string') {\n" +
                "       if (url[url.length-1] === '/') {\n" +
                "         if (item[item.length-1] !== '/') {\n" +
                "           item += '/'\n" +
                "         }\n" +
                "      } else {\n" +
                "        if (item[item.length-1] === '/') {\n" +
                "          url += '/'\n" +
                "        }\n" +
                "      }\n" +
                "      return url === item\n" +
                "    }\n" +
                "    else {\n" +
                "      if (item.type === 'regexp') {\n" +
                "        var reg = new RegExp(item.source, item.flags)\n" +
                "        return reg.test(url)\n" +
                "      }\n" +
                "    }\n" +
                "    return false\n" +
                "  })\n" +
                "}\n" +
                "checkUrl(\'" + url + "\', " + trustedUrls + ")";
    }

    private static String decodeUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        String decodedUrl = null;
        try {
            decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            Log.e(TAG, "decode url failed :" + url, e);
        }
        return decodedUrl;
    }

    public interface UrlCheckListener {
        void onTrusted();

        void onUnTrusted();
    }
}
