/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.signature;

import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import org.hapjs.AbstractContentProvider;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.runtime.PermissionChecker;
import org.hapjs.runtime.ResourceConfig;

public class SignatureProvider extends AbstractContentProvider {
    private static final String TAG = "SignatureProvider";

    private static final String PATH_SIGNATURE = "signature";

    public static Uri getSignatureUri(Context context, String pkg) {
        return Uri.parse("content://" + getAuthority(context) + "/" + PATH_SIGNATURE + "/" + pkg);
    }

    private static String getAuthority(Context context) {
        String platform;
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            platform = context.getPackageName();
        } else {
            platform = ResourceConfig.getInstance().getPlatform();
        }
        return getAuthority(platform);
    }

    private static String getAuthority(String platform) {
        return platform + ".signature";
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    public @Nullable
            ParcelFileDescriptor doOpenFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        if (!PermissionChecker.verify(getContext(), Binder.getCallingUid())) {
            throw new FileNotFoundException("no permission to open file");
        }

        String path = uri.getLastPathSegment();
        if (TextUtils.isEmpty(path) || path.contains("/")) {
            Log.e(TAG, "path not found: " + path);
            throw new FileNotFoundException("path not found: " + path);
        }

        String pkg = path;
        if (CacheStorage.getInstance(getContext()).hasCache(pkg)) {
            Cache cache = CacheStorage.getInstance(getContext()).getCache(pkg);
            File file = cache.getSignatureFile();
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }

        Log.w(TAG, "app not installed: " + pkg);
        throw new FileNotFoundException("app not installed: " + pkg);
    }
}
