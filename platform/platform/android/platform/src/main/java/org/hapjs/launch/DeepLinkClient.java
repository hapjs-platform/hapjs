/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.launch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hapjs.LauncherActivity;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.Source;
import org.hapjs.utils.ActivityUtils;

public class DeepLinkClient implements LauncherManager.LauncherClient {
    protected static final String PARAM_SOURCE = "__SRC__";
    protected static final String PARAM_SOURCE_SCENE = "__SS__";
    private static final String TAG = "DeepLinkClient";
    private static final String[] DEEP_LINK_PREFIXES =
            new String[] {
                    "http://hapjs.org/app/",
                    "https://hapjs.org/app/",
                    "http://qr.quickapp.cn/app/",
                    "https://qr.quickapp.cn/app/",
                    "hap://app/"
            };
    protected Uri mUri;
    protected String mPackage;
    protected String mPath;
    protected String mSource;
    protected String mSourceScene;
    protected String mSourceHapPackage;
    protected String mSourceEntry;
    protected String mSession;

    protected DeepLinkClient() {
    }

    public static DeepLinkClient getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean respond(Intent intent) {
        return isDeepLink(intent);
    }

    @Override
    public boolean needLauncherId() {
        return false;
    }

    @Override
    public String getPackage(Intent intent) {
        prepare(intent);
        return mPackage;
    }

    @Override
    public String getClassName(int launcherId) {
        return "";
    }

    @Override
    public void launch(Context context, Intent intent) {
        prepare(intent);
        Source source = getSource(context);
        String session = getSession();
        LogHelper.addPackage(mPackage, source, session);
        LauncherActivity.launch(context, mPackage, mPath, source);
    }

    private boolean isDeepLink(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) || TextUtils.isEmpty(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                String uriText = uri.toString();
                if (getMatchedPrefix(uriText) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String getMatchedPrefix(String uri) {
        for (String prefix : DEEP_LINK_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    protected String getMatchedPath(Uri uri) {
        String uriText = uri.toString();
        String prefix = getMatchedPrefix(uriText);
        if (prefix == null) {
            return null;
        } else {
            return uriText.substring(prefix.length());
        }
    }

    private void prepare(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            throw new IllegalArgumentException("uri can't be empty");
        }
        if (!uri.equals(mUri)) {
            reset();
            try {
                parseUri(uri);
                mUri = uri;
            } catch (Exception e) {
                reset();
                Log.e(TAG, "parse uri failed ", e);
            }
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mSourceHapPackage = extras.getString(RouterUtils.EXTRA_HAP_PACKAGE);
            mSourceEntry = extras.getString(RouterUtils.EXTRA_HAP_SOURCE_ENTRY);
            if (TextUtils.isEmpty(mSource)) {
                mSource = extras.getString(RouterUtils.EXTRA_CARD_HOST_SOURCE);
            }
            mSession = extras.getString(RouterUtils.EXTRA_SESSION);
        } else {
            mSourceHapPackage = null;
            mSourceEntry = null;
            mSession = null;
        }
    }

    protected void reset() {
        mPackage = null;
        mPath = null;
        mSource = null;
        mUri = null;
        mSourceScene = null;
    }

    protected void parseUri(Uri uri) {
        String path = getMatchedPath(uri);
        if (path == null) {
            throw new IllegalArgumentException("Invalid uri: " + uri);
        }
        parsePath(path);
    }

    private void parsePath(String path) {
        int pkgEndIndex = path.indexOf('/');
        if (pkgEndIndex < 0) {
            pkgEndIndex = path.indexOf('?');
            if (pkgEndIndex < 0) {
                pkgEndIndex = path.indexOf('#');
            }
        }

        if (pkgEndIndex >= 0) {
            mPackage = path.substring(0, pkgEndIndex);
            String subPath = path.substring(pkgEndIndex);
            parseSubPath(subPath);
        } else {
            mPackage = path;
        }
    }

    private void parseSubPath(String subPath) {
        Uri uri = Uri.parse(subPath);
        mSource = uri.getQueryParameter(PARAM_SOURCE);
        mSourceScene = uri.getQueryParameter(PARAM_SOURCE_SCENE);
        mPath = removeParamsFromUri(uri, PARAM_SOURCE, PARAM_SOURCE_SCENE).toString();
    }

    protected Source getSource(Context context) {
        Source source = Source.fromJson(mSource);
        if (source == null) {
            source = new Source();
        }
        source.putInternal(Source.INTERNAL_CHANNEL, Source.CHANNEL_DEEPLINK);
        if (TextUtils.isEmpty(source.getPackageName()) && !TextUtils.isEmpty(mSourceHapPackage)) {
            source.setPackageName(mSourceHapPackage);
        }
        // source 中没有 hostPackageName 时为其设置默认值
        if (TextUtils.isEmpty(source.getOriginalHostPackageName()) && context instanceof Activity) {
            String launchPackage = ActivityUtils.getCallingPackage((Activity) context);
            source.setHostPackageName(launchPackage);
            if (TextUtils.isEmpty(source.getPackageName())) {
                source.setPackageName(launchPackage);
            }
        }
        if (TextUtils.isEmpty(source.getType())) {
            source.setType(Source.TYPE_URL);
        }
        if (!TextUtils.isEmpty(mSourceEntry)
                && !source.getInternal().containsKey(Source.INTERNAL_ENTRY)) {
            source.putInternal(Source.INTERNAL_ENTRY, mSourceEntry);
        }
        if (!TextUtils.isEmpty(mSourceScene)) {
            source.putExtra(Source.EXTRA_SCENE, mSourceScene);
        }
        return source;
    }

    protected String getSession() {
        return mSession;
    }

    protected Uri removeParamsFromUri(Uri uri, String... paramNames) {
        Set<String> paramNameSet = new HashSet<>(paramNames.length);
        Collections.addAll(paramNameSet, paramNames);

        Uri.Builder builder = new Uri.Builder();
        builder.path(uri.getPath());
        for (String paramName : uri.getQueryParameterNames()) {
            if (!paramNameSet.contains(paramName)) {
                builder.appendQueryParameter(paramName, uri.getQueryParameter(paramName));
            }
        }
        builder.fragment(uri.getFragment());

        return builder.build();
    }

    private static class Holder {
        static final DeepLinkClient INSTANCE = new DeepLinkClient();
    }
}
