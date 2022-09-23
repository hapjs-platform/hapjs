/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.net.Uri;
import org.hapjs.bridge.HybridRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriUtils {
    public static final String SCHEMA_CONTENT = "content";
    public static final String SCHEMA_FILE = "file";
    public static final String SCHEMA_INTENT = "intent";
    private static final String ANDROID_ASSET_PREFIX = "file:///android_asset/";
    private static final String SCHEMA_HTTP = "http";
    private static final String SCHEMA_HTTPS = "https";

    private static final String[] DEEP_LINK_PREFIXES = new String[] {
            "http://qr.quickapp.cn/app/",
            "https://qr.quickapp.cn/app/",
            "http://hapjs.org/app/",
            "https://hapjs.org/app/",
            "hap://app/"
    };

    public static boolean isAssetUri(String uri) {
        return uri != null && uri.startsWith(ANDROID_ASSET_PREFIX);
    }

    public static String getAssetPath(String uri) {
        return uri == null ? null : uri.substring(ANDROID_ASSET_PREFIX.length());
    }

    public static boolean isWebUri(String uri) {
        return isWebSchema(getSchema(uri));
    }

    public static boolean isHybridUri(String uri) {
        for (String prefix : DEEP_LINK_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isContentUri(Uri uri) {
        return uri != null && SCHEMA_CONTENT.equals(uri.getScheme());
    }

    public static boolean isFileUri(Uri uri) {
        return uri != null && SCHEMA_FILE.equals(uri.getScheme());
    }

    public static boolean isFileUri(String uri) {
        return SCHEMA_FILE.equals(getSchema(uri));
    }

    public static boolean isWebSchema(String schema) {
        return SCHEMA_HTTP.equalsIgnoreCase(schema) || SCHEMA_HTTPS.equalsIgnoreCase(schema);
    }

    public static boolean isHybridSchema(String schema) {
        return HybridRequest.SCHEMA.equals(schema);
    }

    public static String getSchema(String uri) {
        if (uri == null) {
            return null;
        }

        int schemaEnding = uri.indexOf(":");
        if (schemaEnding < 0) {
            return null;
        } else {
            return uri.substring(0, schemaEnding);
        }
    }

    public static Uri computeUri(String source) {
        if (source == null) {
            return null;
        }

        try {
            Uri uri = Uri.parse(source);
            return uri.getScheme() == null ? null : uri;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean equals(Uri u1, Uri u2) {
        if (u1 == u2) {
            return true;
        }
        if (u1 == null || u2 == null) {
            return false;
        }
        return u1.equals(u2);
    }

    public static String getPkgFromHybridUri(Uri uri) {
        String appId;
        String regex = "(hap|http|https)://(hapjs\\.org/)?app/([^/?#]+)/?(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(uri.toString());
        if (matcher.find()) {
            appId = matcher.group(3);
        } else {
            return null;
        }
        return appId;
    }
}
