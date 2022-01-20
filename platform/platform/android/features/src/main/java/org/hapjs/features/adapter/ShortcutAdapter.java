/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import org.hapjs.PlatformLogManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.annotation.InheritedAnnotation;
import org.hapjs.features.Shortcut;
import org.hapjs.logging.Source;
import org.hapjs.platform.R;
import org.hapjs.runtime.CheckableAlertDialog;
import org.hapjs.utils.PreferenceUtils;
import org.hapjs.utils.ShortcutUtils;

@InheritedAnnotation
public class ShortcutAdapter extends Shortcut {

    @Override
    protected boolean isForbidden(Context context, String pkg) {
        return !ShortcutUtils.shouldCreateShortcutByFeature(pkg);
    }

    @Override
    protected void showCreateDialog(
            Request request,
            Activity activity,
            String pkg,
            String name,
            String message,
            Uri iconUri,
            Drawable iconDrawable) {
        super.showCreateDialog(request, activity, pkg, name, message, iconUri, iconDrawable);
        PlatformLogManager.getDefault().logShortcutPromptShow(pkg, Source.SHORTCUT_SCENE_API);
    }

    @Override
    protected Dialog onCreateDialog(
            final Request request,
            final Context context,
            final String pkgName,
            final String name,
            final String message,
            final Uri iconUri,
            Drawable iconDrawable) {
        final CheckableAlertDialog checkableAlertDialog =
                (CheckableAlertDialog)
                        super.onCreateDialog(request, context, pkgName, name, message, iconUri,
                                iconDrawable);
        checkableAlertDialog.setCheckBox(false, R.string.dlg_shortcut_silent);
        checkableAlertDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                org.hapjs.features.R.string.features_dlg_shortcut_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean forbidden = checkableAlertDialog.isChecked();
                        if (forbidden) {
                            PreferenceUtils
                                    .setShortcutForbiddenTime(pkgName, System.currentTimeMillis());
                        }
                        onReject(request, forbidden);
                    }
                });
        return checkableAlertDialog;
    }

    @Override
    protected void onAccept(
            Request request, Context context, String pkgName, String name, Uri iconUri) {
        super.onAccept(request, context, pkgName, name, iconUri);
        PlatformLogManager.getDefault().logShortcutPromptAccept(pkgName, Source.SHORTCUT_SCENE_API);
    }

    @Override
    protected void onReject(Request request, boolean forbidden) {
        super.onReject(request, forbidden);
        String pkg = request.getApplicationContext().getPackage();
        PlatformLogManager.getDefault()
                .logShortcutPromptReject(pkg, forbidden, Source.SHORTCUT_SCENE_API);
    }
}
