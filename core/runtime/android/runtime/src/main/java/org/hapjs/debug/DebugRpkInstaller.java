/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import static org.hapjs.debug.DebugService.ResultCode.CODE_CERTIFICATION_MISMATCH;
import static org.hapjs.debug.DebugService.ResultCode.CODE_INSTALL_ERROR_FAIL_TO_CREATE_TEMP_FILE;
import static org.hapjs.debug.DebugService.ResultCode.CODE_INSTALL_ERROR_INVALID_PACKAGE;
import static org.hapjs.debug.DebugService.ResultCode.CODE_INSTALL_ERROR_INVALID_URI;
import static org.hapjs.debug.DebugService.ResultCode.CODE_OK;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.FileUtils;

public class DebugRpkInstaller {
    private static final String TAG = "DebugRpkInstaller";

    public static int installPackage(Context context, String pkg, Uri uri) {
        if (TextUtils.isEmpty(pkg)) {
            Log.e(TAG, "Invalid package: " + pkg);
            return CODE_INSTALL_ERROR_INVALID_PACKAGE;
        }
        if (uri == null) {
            Log.e(TAG, "package uri can't be null");
            return CODE_INSTALL_ERROR_INVALID_URI;
        }

        File tmpFile;
        InputStream in = null;
        try {
            tmpFile = File.createTempFile(pkg, ".rpk", context.getCacheDir());
            in = context.getContentResolver().openInputStream(uri);
            FileUtils.saveToFile(in, tmpFile);
        } catch (IOException e) {
            Log.e(TAG, "Fail to install package", e);
            return CODE_INSTALL_ERROR_FAIL_TO_CREATE_TEMP_FILE;
        } finally {
            FileUtils.closeQuietly(in);
        }

        try {
            CacheStorage.getInstance(context).install(pkg, tmpFile.getPath());
            return CODE_OK;
        } catch (CacheException e) {
            if (e.getErrorCode() == CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED) {
                return CODE_CERTIFICATION_MISMATCH;
            }
            Log.e(TAG, "Fail to install package", e);
            return e.getErrorCode();
        } finally {
            tmpFile.delete();
        }
    }
}
