/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.common;

import android.content.Context;
import android.net.Uri;

public class ExchangeUriProvider {
    private static Provider sUriProvider;

    public static String getPlatform(Context context) {
        return getProvider().getPlatform(context);
    }

    public static Uri getUri(Context context, Uri uri, String pkg) {
        return getProvider().getUri(context, uri, pkg);
    }

    private static synchronized Provider getProvider() {
        if (sUriProvider == null) {
            sUriProvider = new DefaultProvider();
        }
        return sUriProvider;
    }

    public static synchronized void setProvider(Provider provider) {
        sUriProvider = provider;
    }

    public interface Provider {
        String getPlatform(Context context);

        Uri getUri(Context context, Uri uri, String pkg);
    }

    private static class DefaultProvider implements Provider {

        @Override
        public String getPlatform(Context context) {
            return context.getPackageName();
        }

        @Override
        public Uri getUri(Context context, Uri uri, String pkg) {
            return uri;
        }
    }
}
