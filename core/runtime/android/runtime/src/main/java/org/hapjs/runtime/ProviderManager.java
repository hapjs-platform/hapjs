/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.util.ArrayMap;
import android.util.Log;
import java.util.Map;

public class ProviderManager {
    private static final String TAG = "ProviderManager";
    private Map<String, Object> mProviders = new ArrayMap<>();

    private ProviderManager() {
    }

    public static ProviderManager getDefault() {
        return Holder.INSTANCE;
    }

    public synchronized <T> T getProvider(String providerName) {
        T provider = (T) mProviders.get(providerName);
        if (provider == null) {
            String msg = "provider is null: providerName=" + providerName;
            Log.e(TAG, msg, new Exception(msg));
        }
        return provider;
    }

    public synchronized void addProvider(String providerName, Object provider) {
        mProviders.put(providerName, provider);
    }

    private static class Holder {
        private static final ProviderManager INSTANCE = new ProviderManager();
    }
}
