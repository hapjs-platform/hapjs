/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.pm;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class DebuggerInfo {
    private static final String TAG = "DebuggerInfo";

    private static final String KEY_LATEST_VERSION_CODE = "latestVersionCode";
    private int mLatestVersionCode;

    public static DebuggerInfo parse(String data) {
        DebuggerInfo debuggerInfo = new DebuggerInfo();
        try {
            JSONObject jsonObject = new JSONObject(data);
            debuggerInfo.mLatestVersionCode = jsonObject.optInt(KEY_LATEST_VERSION_CODE);
        } catch (JSONException e) {
            Log.e(TAG, "Fail to parse debugger info", e);
        }
        return debuggerInfo;
    }

    public int getVersionCode() {
        return mLatestVersionCode;
    }
}

