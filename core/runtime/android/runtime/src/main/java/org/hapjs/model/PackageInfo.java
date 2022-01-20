/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.util.Log;
import org.json.JSONObject;

public class PackageInfo {

    // {"toolkit":"0.7.6","timeStamp":"2020-07-17T10:10:06.631Z","node":"v12.16.1","platform":"darwin","arch":"x64"}
    public static final String KEY_TOOLKIT_VERSION = "toolkit";
    public static final String KEY_PACKAGE_TIMESTAMP = "timeStamp";
    private static final String TAG = "PackageInfo";
    private String mToolkitVersion;
    private String mTimeStamp;

    public static PackageInfo parse(JSONObject jsonObject) {
        if (null == jsonObject) {
            Log.w(TAG, "parse jsonObject is null.");
            return null;
        }
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.mToolkitVersion = jsonObject.optString(KEY_TOOLKIT_VERSION, "");
        packageInfo.mTimeStamp = jsonObject.optString(KEY_PACKAGE_TIMESTAMP, "");
        return packageInfo;
    }

    public String getToolkitVersion() {
        return mToolkitVersion;
    }

    public String getTimeStamp() {
        return mTimeStamp;
    }
}
