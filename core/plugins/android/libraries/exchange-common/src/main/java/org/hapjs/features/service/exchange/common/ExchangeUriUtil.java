/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.common;

import android.content.Context;
import android.net.Uri;

public class ExchangeUriUtil {

    public static Uri getDataUri(Context context, String pkg) {
        return getUri(context, Constant.URI_PATH_DATA, pkg);
    }

    public static Uri getPermissionUri(Context context, String pkg) {
        return getUri(context, Constant.URI_PATH_PERMISSION, pkg);
    }

    public static Uri getClearUri(Context context) {
        return getUri(context, Constant.URI_PATH_CLEAR, context.getPackageName());
    }

    private static Uri getUri(Context context, String path, String pkg) {
        String platform = ExchangeUriProvider.getPlatform(context);
        Uri uri = getBaseUri(platform, path);
        return ExchangeUriProvider.getUri(context, uri, pkg);
    }

    private static Uri getBaseUri(String platform, String path) {
        return Uri.parse("content://" + platform + Constant.AUTHORITY_SUFFIX + "/" + path);
    }
}
