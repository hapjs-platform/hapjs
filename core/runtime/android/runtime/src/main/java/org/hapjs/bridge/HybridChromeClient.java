/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.DialogInterface;
import android.view.KeyEvent;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.runtime.HybridDialog;
import org.hapjs.runtime.HybridDialogProvider;
import org.hapjs.runtime.ProviderManager;

public class HybridChromeClient {
    public boolean onJsAlert(HybridView view, String url, String message, final JsResult result) {
        HybridDialogProvider provider =
                ProviderManager.getDefault().getProvider(HybridDialogProvider.NAME);
        HybridDialog dialog =
                provider.createAlertDialog(
                        view.getHybridManager().getActivity(), ThemeUtils.getAlertDialogTheme());
        dialog.setMessage(message);
        dialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        result.cancel();
                    }
                });
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                view.getHybridManager().getActivity().getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
        dialog.setOnKeyListener(
                new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            result.confirm();
                            return false;
                        }
                        return true;
                    }
                });

        dialog.show();
        return true;
    }

    public boolean onJsConfirm(HybridView view, String url, String message, final JsResult result) {
        HybridDialogProvider provider =
                ProviderManager.getDefault().getProvider(HybridDialogProvider.NAME);
        HybridDialog dialog =
                provider.createAlertDialog(
                        view.getHybridManager().getActivity(), ThemeUtils.getAlertDialogTheme());
        dialog.setMessage(message);
        dialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        result.cancel();
                    }
                });
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                view.getHybridManager().getActivity().getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                view.getHybridManager().getActivity().getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                });
        dialog.setOnKeyListener(
                new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            result.confirm();
                            return false;
                        }
                        return true;
                    }
                });

        dialog.show();
        return true;
    }

    public void onGeolocationPermissionsShowPrompt(
            String origin, GeolocationPermissions.Callback callback) {
    }

    public void onProgressChanged(HybridView view, int progress) {
    }

    public void onReceivedTitle(HybridView view, String title) {
    }
}
