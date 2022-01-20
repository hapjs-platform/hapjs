/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.signature;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.utils.SignatureStore;
import org.hapjs.common.utils.DigestUtils;
import org.hapjs.runtime.ResourceConfig;

public class SignatureManager {
    private static final String TAG = "SignatureManager";

    public static String getSignature(Context context, String pkg) {
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            return getSignatureFromLocal(context, pkg);
        } else {
            return getSignatureFromRemote(context, pkg);
        }
    }

    private static String getSignatureFromLocal(Context context, String pkg) {
        if (CacheStorage.getInstance(context).hasCache(pkg)) {
            String sign = CacheStorage.getInstance(context).getPackageSign(pkg);
            return DigestUtils.getSha256(Base64.decode(sign, Base64.DEFAULT));
        }
        throw new IllegalStateException("pkg not installed: " + pkg);
    }

    private static String getSignatureFromRemote(Context context, String pkg) {
        Uri uri = SignatureProvider.getSignatureUri(context, pkg);
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            byte[] existCert = SignatureStore.load(input);
            return DigestUtils.getSha256(existCert);
        } catch (IOException e) {
            Log.e(TAG, "failed to get signature", e);
        }

        throw new IllegalStateException("failed to get signature: " + pkg);
    }
}
