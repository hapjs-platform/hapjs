/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.permission;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.hapjs.bridge.permission.RuntimePermissionProvider;
import org.hapjs.runtime.CheckableAlertDialog;

public class RuntimePermissionProviderImpl implements RuntimePermissionProvider {
    protected Context mContext;
    protected boolean mEnableForbidden;
    protected Set<String> mRejectPermissionCache;

    public RuntimePermissionProviderImpl(Context context, boolean enableForbidden) {
        mContext = context.getApplicationContext();
        mEnableForbidden = enableForbidden;
        mRejectPermissionCache = new HashSet<>();
    }

    @Override
    public int[] checkPermissions(String pkg, String[] permissions) {
        int[] result = Permission.checkPermissions(mContext, pkg, permissions);
        for (int i = 0; i < permissions.length; i++) {
            if (mRejectPermissionCache.contains(permissions[i])) {
                result[i] = MODE_REJECT;
            }
        }
        return result;
    }

    @Override
    public int[] getPermissionsMode(String pkg, String[] permissions) {
        return Permission.checkPermissions(mContext, pkg, permissions);
    }

    @Override
    public void onPermissionForbidden(Activity activity, String permission) {
    }

    @Override
    public void grantPermissions(String pkg, String[] permissions) {
        Permission.grantPermissions(mContext, pkg, permissions);
    }

    @Override
    public void rejectPermissions(String pkg, String[] permissions, boolean forbidden) {
        mRejectPermissionCache.addAll(Arrays.asList(permissions));
        Permission.rejectPermissions(mContext, pkg, permissions, forbidden);
    }

    @Override
    public int getPermissionFlag(String pkg, String permission) {
        if (mEnableForbidden) {
            boolean showForbidden = Permission.shouldShowForbidden(mContext, pkg, permission);
            return showForbidden ? FLAG_SHOW_FORBIDDEN : 0;
        }
        return 0;
    }

    @Override
    public void clearRejectPermissionCache() {
        mRejectPermissionCache.clear();
    }

    @Override
    public Dialog createPermissionDialog(
            Activity activity,
            String permission,
            String appName,
            String desc,
            DialogInterface.OnClickListener listener,
            boolean enableCheckBox) {
        CheckableAlertDialog dialog = new CheckableAlertDialog(activity);
        dialog.setContentView(org.hapjs.platform.R.layout.permission_dialog);
        TextView messageView = (TextView) dialog.findViewById(org.hapjs.platform.R.id.perm_message);
        messageView.setText(desc);
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                org.hapjs.platform.R.string.dlg_permission_accept,
                listener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                org.hapjs.platform.R.string.dlg_permission_reject,
                listener);
        if (enableCheckBox) {
            dialog.setCheckBox(false, org.hapjs.platform.R.string.dlg_permission_remind);
        }
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public boolean isHidePermissionDialog(Activity activity, Dialog dialog) {
        return false;
    }
}
