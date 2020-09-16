/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.utils.FontFileManager;
import org.hapjs.component.Component;
import org.hapjs.model.AppInfo;
import org.hapjs.render.FontFamilyProvider;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FontParser {

    private static final String TAG = "FontParser";

    private static final String KEY_FONT_NAME = "fontName";
    private static final String KEY_FONT_SRC = "fontSrc";

    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_SRC = "src";

    private static final String SCHEME_FILE = "file";
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String SCHEME_LOCAL = "local";
    private static final String SCHEME_CONTENT = "content";

    private Context mContext;
    private Component mComponent;

    public FontParser(Context context, Component component) {
        mContext = context;
        mComponent = component;
    }

    public void parse(String fontFamily, final ParseCallback callback) {
        if (TextUtils.isEmpty(fontFamily)) {
            callback.onParseComplete(null);
            return;
        }
        try {
            final JSONArray fontArray = new JSONArray(fontFamily);
            parseInternal(fontArray, callback, 0);
        } catch (JSONException e) {
            Log.e(TAG, "parse font family error. font family string: " + fontFamily);
        }
    }

    private Uri createLocalUri(String uriStr) {
        Uri.Builder builder = new Uri.Builder();
        return builder.scheme("local").path(uriStr).build();
    }

    private void parseInternal(
            final JSONArray fontArray, final ParseCallback callback, final int index) {
        if (fontArray == null || index < 0 || index >= fontArray.length()) {
            callback.onParseComplete(null);
            return;
        }

        JSONObject fontObj = fontArray.optJSONObject(index);
        // 字体名称关键字改为fontFamily
        String fontName = fontObj.optString(KEY_FONT_FAMILY);
        if (TextUtils.isEmpty(fontName)) {
            fontName = fontObj.optString(KEY_FONT_NAME);
        }
        List<Uri> uris = new ArrayList<>();

        // 字体来源关键字改为src
        JSONArray fontUrlArr = fontObj.optJSONArray(KEY_SRC);
        if (fontUrlArr == null) {
            fontUrlArr = fontObj.optJSONArray(KEY_FONT_SRC);
        }
        if (fontUrlArr != null) {
            for (int j = 0; j < fontUrlArr.length(); j++) {
                String fontUrl = fontUrlArr.optString(j);
                Uri fontUri = mComponent.tryParseUri(fontUrl);
                if (fontUri == null) {
                    // fontUrl may be a local font name, for example, 'serif', 'cursive'.
                    fontUri = createLocalUri(fontUrl);
                }
                uris.add(fontUri);
            }
        }

        // if there is no uri, we create a local uri according to font name.
        if (uris.isEmpty()) {
            uris.add(createLocalUri(fontName));
        }

        parseTypeface(
                uris,
                new ParseCallback() {
                    @Override
                    public void onParseComplete(Typeface typeface) {
                        if (typeface == null) {
                            parseInternal(fontArray, callback, index + 1);
                        } else {
                            callback.onParseComplete(typeface);
                        }
                    }
                },
                0);
    }

    private void parseTypeface(
            final List<Uri> uris, final ParseCallback callback, final int uriIndex) {
        if (uris == null || uriIndex < 0 || uriIndex >= uris.size()) {
            callback.onParseComplete(null);
            return;
        }
        ParseCallback pcb =
                new ParseCallback() {
                    @Override
                    public void onParseComplete(Typeface typeface) {
                        if (typeface == null) {
                            parseTypeface(uris, callback, uriIndex + 1);
                        } else {
                            callback.onParseComplete(typeface);
                        }
                    }
                };

        Uri uri = uris.get(uriIndex);
        if (TextUtils.equals(uri.getScheme(), SCHEME_LOCAL)) {
            parseTypefaceFromLocal(uri, pcb);
        } else if (TextUtils.equals(uri.getScheme(), SCHEME_FILE)) {
            parseTypefaceFromFile(uri, pcb);
        } else if (TextUtils.equals(uri.getScheme(), SCHEME_HTTP)
                || TextUtils.equals(uri.getScheme(), SCHEME_HTTPS)) {
            parseTypefaceFromNet(uri, pcb);
        } else if (TextUtils.equals(uri.getScheme(), SCHEME_CONTENT)) {
            parseTypefaceFromContentUri(uri, pcb);
        } else {
            Log.e(TAG, "parse typeface failed: wrong uri scheme: " + uri.getScheme());
            callback.onParseComplete(null);
        }
    }

    private void parseTypefaceFromLocal(Uri fontUri, ParseCallback callback) {
        if (fontUri == null) {
            callback.onParseComplete(null);
            return;
        }
        String name = fontUri.getLastPathSegment();

        FontFamilyProvider fontFamilyProvider = ProviderManager.getDefault().getProvider(FontFamilyProvider.NAME);
        Typeface typeface = null;
        if (fontFamilyProvider != null) {
            typeface = fontFamilyProvider.getTypefaceFromLocal(fontUri, Typeface.NORMAL);
        }

        if (typeface == null) {
            typeface = Typeface.create(name, Typeface.NORMAL);
        }

        Typeface result = Typeface.DEFAULT.equals(typeface) ? null : typeface;
        callback.onParseComplete(result);
    }

    private void parseTypefaceFromFile(Uri fontUri, ParseCallback callback) {
        if (fontUri == null) {
            callback.onParseComplete(null);
            return;
        }
        Typeface typeface = null;
        try {
            typeface = Typeface.createFromFile(fontUri.getPath());
        } catch (RuntimeException e) {
            Log.e(TAG, "parse typeface from file error", e);
        }
        callback.onParseComplete(typeface);
    }

    private void parseTypefaceFromNet(Uri fontUri, final ParseCallback callback) {
        AppInfo appInfo = mComponent.getRootComponent().getAppInfo();
        File cache = FontFileManager.getInstance().getOrCreateFontFile(mContext, appInfo, fontUri);
        if (cache != null && cache.exists()) {
            parseTypefaceFromFile(Uri.fromFile(cache), callback);
            return;
        }

        if (!isNetworkConnected()) {
            callback.onParseComplete(null);
            Log.w(TAG, "the network is not available");
            return;
        }

        FontFileManager.FontFilePrepareCallback fontFilePrepareCallback =
                new FontFileManager.FontFilePrepareCallback() {
                    @Override
                    public void onFontFilePrepared(Uri fileUri) {
                        parseTypefaceFromFile(fileUri, callback);
                    }
                };
        FontFileManager.getInstance()
                .enqueueTask(mContext, appInfo, fontUri, fontFilePrepareCallback);
    }

    @SuppressWarnings("all")
    private boolean isNetworkConnected() {
        if (mContext != null) {
            ConnectivityManager manager =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    private void parseTypefaceFromContentUri(Uri fontUri, final ParseCallback callback) {
        AppInfo appInfo = mComponent.getRootComponent().getAppInfo();
        File cache = FontFileManager.getInstance().getOrCreateFontFile(mContext, appInfo, fontUri);
        if (cache != null && cache.exists()) {
            parseTypefaceFromFile(Uri.fromFile(cache), callback);
            return;
        }

        FontFileManager.FontFilePrepareCallback fontFilePrepareCallback =
                new FontFileManager.FontFilePrepareCallback() {
                    @Override
                    public void onFontFilePrepared(Uri fileUri) {
                        parseTypefaceFromFile(fileUri, callback);
                    }
                };
        FontFileManager.getInstance()
                .enqueueTask(mContext, appInfo, fontUri, fontFilePrepareCallback);
    }

    public interface ParseCallback {
        void onParseComplete(Typeface typeface);
    }
}
