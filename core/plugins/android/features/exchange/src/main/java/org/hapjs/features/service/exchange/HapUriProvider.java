/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import org.hapjs.common.signature.SignatureManager;
import org.hapjs.features.service.exchange.common.Constant;
import org.hapjs.features.service.exchange.common.ExchangeUriProvider;
import org.hapjs.runtime.ResourceConfig;

public class HapUriProvider implements ExchangeUriProvider.Provider {
    private static final String TAG = "HapUriProvider";

    @Override
    public String getPlatform(Context context) {
        return ResourceConfig.getInstance().getPlatform();
    }

    @Override
    public Uri getUri(Context context, Uri uri, String pkg) {
        if (TextUtils.isEmpty(pkg) || TextUtils.equals(context.getPackageName(), pkg)) {
            return uri;
        }
        String sign = getHapAppSignDigest(context, pkg);
        if (TextUtils.isEmpty(sign)) {
            throw new IllegalStateException("can't get sign for this app");
        }
        return uri.buildUpon()
                .appendQueryParameter(Constant.PARAM_CALLING_PKG, pkg)
                .appendQueryParameter(Constant.PARAM_CALLING_SIGN, sign)
                .build();
    }

    protected String getHapAppSignDigest(Context context, String pkg) {
        return SignatureManager.getSignature(context, pkg);
    }
}
