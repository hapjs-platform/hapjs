/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.system;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import org.hapjs.logging.Source;
import org.hapjs.utils.ShortcutParamsHelper;

public class PlatformSysOpProviderImpl extends DefaultSysOpProviderImpl {
    @Override
    public void onShortcutInstallComplete(
            Context context,
            String pkg,
            String path,
            String params,
            String appName,
            Uri iconUri,
            String type,
            String serverIconUrl,
            Source source,
            boolean isComplete) {
        super.onShortcutInstallComplete(context, pkg, path, params, appName, iconUri,
                type, serverIconUrl, source, isComplete);
        if (isComplete) {
            ShortcutParamsHelper.insertShortParams(context, pkg, path, params);
        }
    }

    @Override
    public boolean updateShortcut(Context context, String pkg, String path, String params, String appName,
                                  Bitmap icon, boolean isOpIconUpdate) {
        boolean result = super.updateShortcut(context, pkg, path, params, appName, icon, isOpIconUpdate);
        result = ShortcutParamsHelper.updateShortParams(context, pkg, path, params) || result;
        return result;
    }

    @Override
    public boolean uninstallShortcut(Context context, String pkg, String appName) {
        boolean result = super.uninstallShortcut(context, pkg, appName);
        if (result) {
            ShortcutParamsHelper.deleteShortParams(context, pkg);
        }
        return result;
    }
}
