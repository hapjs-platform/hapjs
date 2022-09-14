/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;
import java.io.File;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.logging.RuntimeLogManager;

public class DocumentUtils {
    private static final String TAG = "DocumentUtils";
    private static String sRouterAppFrom = "";
    private static String sSourceH5 = "";

    public static boolean open(ApplicationContext appContext, String uri, Bundle extras, String routerAppFrom, String sourceH5) {
        sRouterAppFrom = "";
        sSourceH5 = "";
        if (!InternalUriUtils.isInternalPath(uri)) {
            return false;
        }
        sRouterAppFrom = routerAppFrom;
        sSourceH5 = sourceH5;
        File underlyingFile = appContext.getUnderlyingFile(uri);
        if (underlyingFile == null) {
            Uri underlyingUri = appContext.getUnderlyingUri(uri);
            if (underlyingUri != null) {
                return openUri(appContext, underlyingUri, extras);
            } else {
                Log.e(TAG, "uri is not valid: " + uri);
                return false;
            }
        } else {
            return openFile(appContext, underlyingFile, extras);
        }
    }

    private static boolean openFile(ApplicationContext appContext, File file, Bundle extras) {
        Context context = appContext.getContext();
        Uri uri;
        if (Build.VERSION_CODES.M >= Build.VERSION.SDK_INT) {
            uri = Uri.fromFile(file);
        } else {
            try {
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".file", file);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Fail to getUriForFile: " + file.getPath(), e);
                uri = Uri.fromFile(file);
            }
        }
        if (uri == null) {
            Log.e(TAG, "file is not public: " + file.getPath());
            return false;
        }

        String mimeType = getMimeTypeFromFile(file);
        return openUri(appContext, uri, mimeType, extras);
    }

    private static boolean openUri(ApplicationContext appContext, Uri uri, Bundle extras) {
        Context context = appContext.getContext();
        String mimeType = getMimeTypeFromUri(context, uri);
        return openUri(appContext, uri, mimeType, extras);
    }

    private static boolean openUri(
            ApplicationContext appContext, Uri uri, String mimeType, Bundle extras) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtras(extras);
        if (TextUtils.isEmpty(mimeType)) {
            intent.setData(uri);
        } else {
            intent.setDataAndType(uri, mimeType);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return startViewActivity(appContext, intent, uri);
    }

    private static String getMimeTypeFromFile(File file) {
        String extension = FileUtils.getFileExtension(file);
        if (extension.isEmpty()) {
            return "";
        } else {
            // remove leading '.'
            extension = extension.substring(1);
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
    }

    private static String getMimeTypeFromUri(Context context, Uri uri) {
        try {
            return context.getContentResolver().getType(uri);
        } catch (Exception e) {
            Log.e(TAG, "Fail to get type for uri: " + uri, e);
            return null;
        }
    }

    private static boolean startViewActivity(ApplicationContext appContext, Intent intent,
                                             Uri uri) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            Context context = appContext.getContext();
            context.startActivity(intent);
            ResolveInfo info = context.getPackageManager().resolveActivity(intent, 0);
            if (info != null) {
                RuntimeLogManager.getDefault()
                        .logAppRouterNativeApp(
                                appContext.getPackage(),
                                uri.toString(),
                                info.activityInfo.packageName,
                                info.activityInfo.name,
                                sRouterAppFrom,
                                true,
                                null,
                                sSourceH5);
            }
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No compatible activity found", e);
        }
        RuntimeLogManager.getDefault()
                .logAppRouterNativeApp(
                        appContext.getPackage(), uri.toString(), "", "", sRouterAppFrom, false, "no compatible activity found", sSourceH5);
        return false;
    }
}
