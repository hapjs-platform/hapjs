/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class PermissionInfo {
    private static final String KEY_ORIGIN = "origin";
    private String mOrigin;

    public static PermissionInfo parse(JSONObject jsonObject) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.mOrigin = jsonObject.optString(KEY_ORIGIN);
        return permissionInfo;
    }

    public static List<PermissionInfo> parse(JSONArray permissionObjects) {
        List<PermissionInfo> permissionInfos = new ArrayList<PermissionInfo>();
        if (permissionObjects != null) {
            for (int i = 0; i < permissionObjects.length(); i++) {
                permissionInfos.add(parse(permissionObjects.optJSONObject(i)));
            }
        }
        return permissionInfos;
    }

    public String getOrigin() {
        return mOrigin;
    }
}
