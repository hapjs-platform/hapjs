/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.pm;

import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlatformInfo {

    public static final String PREVIEW_PACKAGE = "org.hapjs.mockup";
    private static final String TAG = "PlatformInfo";
    private static final String KEY_PLATFORMS = "platforms";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_NAME = "name";
    private static final String KEY_URL = "url";
    private static final String KEY_PLATFORM_VERSION = "platformVersion";
    private static final String KEY_APP_VERSION = "appVersion";

    public String packageName;
    public String name;
    public String url;
    public int platformVersion;
    public int appVersion;

    public static List<PlatformInfo> parseJson(String data) {
        try {
            JSONObject obj = new JSONObject(data);
            JSONArray array = obj.optJSONArray(PlatformInfo.KEY_PLATFORMS);
            if (array != null && array.length() > 0) {
                List<PlatformInfo> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    PlatformInfo platformInfo = new PlatformInfo();
                    platformInfo.packageName = item.optString(KEY_PACKAGE, PREVIEW_PACKAGE);
                    platformInfo.name = item.optString(KEY_NAME);
                    platformInfo.url = item.optString(KEY_URL);
                    platformInfo.platformVersion = item.optInt(KEY_PLATFORM_VERSION);
                    platformInfo.appVersion = item.optInt(KEY_APP_VERSION);
                    list.add(platformInfo);
                }
                return list;
            }
        } catch (Exception e) {
            Log.e(TAG, "parseJson: ", e);
        }
        return null;
    }

    public String getFileName() {
        Uri uri = Uri.parse(url);
        return uri.getLastPathSegment();
    }
}
