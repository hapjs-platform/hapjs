/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.cache.CacheConstants;
import org.hapjs.common.executors.Executors;
import org.hapjs.features.service.exchange.common.Constant;
import org.hapjs.features.service.exchange.common.ExchangeUriUtil;

public class PackageRemoveReceiver extends BroadcastReceiver {
    private static final String TAG = "PackageRemoveReceiver";

    private static boolean clearExchangeData(Context context, String targetPkg) {
        if (TextUtils.isEmpty(targetPkg)) {
            throw new IllegalArgumentException("Illegal param");
        }
        Uri clearUri =
                ExchangeUriUtil.getClearUri(context)
                        .buildUpon()
                        .appendQueryParameter(Constant.PARAM_PKG, targetPkg)
                        .build();
        try {
            int result = context.getContentResolver().delete(clearUri, null, null);
            return result >= 0;
        } catch (Exception e) {
            Log.w(TAG, "force clear error", e);
            return false;
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String pkg = null;
        if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                pkg = uri.getSchemeSpecificPart();
            } else {
                Log.w(TAG, "onReceive: uri is null");
            }
        } else if (CacheConstants.ACTION_PACKAGE_PACKAGE_REMOVED.equals(intent.getAction())) {
            pkg = intent.getStringExtra(CacheConstants.EXTRA_PACKAGE);
        }
        if (!TextUtils.isEmpty(pkg)) {
            final String finalPkg = pkg;
            Executors.io().execute(() -> clearExchangeData(context, finalPkg));
        }
    }
}
