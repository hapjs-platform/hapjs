/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;

public interface RuntimePermissionProvider {
    String NAME = "permission";
    int MODE_ACCEPT = 0;
    int MODE_PROMPT = 1;
    int MODE_REJECT = 2;

    int FLAG_SHOW_FORBIDDEN = 0x01;

    int[] checkPermissions(String pkg, String[] permissions);

    void onPermissionForbidden(Activity activity, String permission);

    void grantPermissions(String pkg, String[] permissions);

    void rejectPermissions(String pkg, String[] permissions, boolean forbidden);

    int getPermissionFlag(String pkg, String permission);

    void clearRejectPermissionCache();

    Dialog createPermissionDialog(
            Activity activity,
            String permission,
            String appName,
            String desc,
            DialogInterface.OnClickListener listener,
            boolean enableCheckBox);

    boolean isHidePermissionDialog(Activity activity, Dialog dialog);
    int[] getPermissionsMode(String pkg, String[] permissions);
}