/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.widget.Toast;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.hapjs.common.utils.ExceptionDialogBuilder;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.R;

public class RootViewDialogManager {
    private static final String TAG = "RootViewDialogManager";

    private Activity mActivity;
    private AppInfo mAppInfo;

    private Dialog mIncompatitableDialog;
    private List<Dialog> mExceptionDialogs;

    public RootViewDialogManager(Activity activity, AppInfo appInfo) {
        mActivity = activity;
        mAppInfo = appInfo;
    }

    public void showExceptionDialog(Exception exception) {
        if (!mAppInfo.getConfigInfo().isDebug()) {
            return;
        }

        if (mActivity.isFinishing()) {
            return;
        }

        if (isAppMode()) {
            removeDismissedExceptionDialogs();

            Dialog dialog =
                    new ExceptionDialogBuilder(mActivity)
                            .setAppName(mAppInfo.getName())
                            .setException(exception)
                            .show();
            if (mExceptionDialogs == null) {
                mExceptionDialogs = new LinkedList<>();
            }
            mExceptionDialogs.add(dialog);
        } else {
            String tip = mActivity.getString(R.string.dlg_page_error_title, mAppInfo.getName());
            Toast.makeText(mActivity.getApplicationContext(), tip, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAppMode() {
        HapEngine.Mode mode = HapEngine.getInstance(mAppInfo.getPackage()).getMode();
        return mode == HapEngine.Mode.APP;
    }

    public void showIncompatibleAppDialog() {
        if (mActivity.isFinishing() || !isAppMode()) {
            return;
        }

        if (mIncompatitableDialog != null && mIncompatitableDialog.isShowing()) {
            return;
        }

        mIncompatitableDialog =
                new AlertDialog.Builder(mActivity, ThemeUtils.getAlertDialogTheme())
                        .setTitle(mActivity
                                .getString(R.string.platform_incompatible, mAppInfo.getName()))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
    }

    public void dismissDialog() {
        if (mIncompatitableDialog != null) {
            mIncompatitableDialog.dismiss();
            mIncompatitableDialog = null;
        }

        if (mExceptionDialogs != null) {
            for (Dialog dialog : mExceptionDialogs) {
                dialog.dismiss();
            }
            mExceptionDialogs = null;
        }
    }

    private void removeDismissedExceptionDialogs() {
        if (mExceptionDialogs != null) {
            Iterator<Dialog> itr = mExceptionDialogs.iterator();
            while (itr.hasNext()) {
                Dialog dialog = itr.next();
                if (!dialog.isShowing()) {
                    itr.remove();
                }
            }
        }
    }
}
