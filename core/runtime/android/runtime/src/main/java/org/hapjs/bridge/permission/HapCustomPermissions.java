/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import android.content.Context;
import java.util.HashMap;
import org.hapjs.runtime.R;

public class HapCustomPermissions {

    public static final String HAP_PERMISSION_RINGTONE = "hap.permission.RINGTONE";
    public static final String HAP_PERMISSION_STEP_COUNTER = "hap.permission.STEP_COUNTER";
    public static final String HAP_PERMISSION_RECEIVE_BROADCAST = ".permission.RECEIVE_BROADCAST";
    public static final String HAP_PERMISSION_ACCESS_CLIPBOARD = "hap.permission.ACCESS_CLIPBOARD";
    public static final String HAP_PERMISSION_WRITE_CLIPBOARD = "hap.permission.WRITE_CLIPBOARD";

    // key is permission name, value is permission description resource id
    private static HashMap<String, Integer> sHapPermissions = new HashMap<>();

    static {
        sHapPermissions.put(HAP_PERMISSION_RINGTONE, R.string.hap_permission_ringtone_desc);
        sHapPermissions.put(HAP_PERMISSION_STEP_COUNTER, R.string.hap_permission_step_counter_desc);
        sHapPermissions.put(HAP_PERMISSION_ACCESS_CLIPBOARD, R.string.hap_permission_access_clipboard_desc);
        sHapPermissions.put(HAP_PERMISSION_WRITE_CLIPBOARD, R.string.hap_permission_write_clipboard_desc);
    }

    public static boolean isHapPermission(String permission) {
        return sHapPermissions.containsKey(permission);
    }

    public static void addHapPermission(String permission, int descResId) {
        sHapPermissions.put(permission, descResId);
    }

    public static String getHapPermissionDesc(String permission, Context context) {
        Integer integer = sHapPermissions.get(permission);
        if (integer == null) {
            return null;
        }
        return context.getString(integer);
    }

    public static void setHapPermissionDesc(String permission, int descResId) {
        sHapPermissions.put(permission, descResId);
    }

    public static String getHapPermissionReceiveBroadcast(Context context) {
        return context.getPackageName() + HAP_PERMISSION_RECEIVE_BROADCAST;
    }
}
