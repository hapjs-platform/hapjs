/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data.internal;

import android.os.Bundle;
import android.text.TextUtils;
import java.util.Map;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.features.storage.data.StorageProvider;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.Runtime;

public class RemoteStorage implements IStorage {
    private ApplicationContext mApplicationContext;
    private String mPackage;

    public RemoteStorage(ApplicationContext context) {
        mApplicationContext = context;
        mPackage = context.getPackage();
    }

    @Override
    public String get(String key) {
        Bundle params = new Bundle();
        params.putString(StorageProvider.PARAM_KEY, key);
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_GET, params);
        if (result != null) {
            return result.getString(StorageProvider.METHOD_GET);
        }
        return null;
    }

    @Override
    public boolean set(String key, String value) {
        Bundle params = new Bundle();
        params.putString(StorageProvider.PARAM_KEY, key);
        params.putString(StorageProvider.PARAM_VALUE, value);
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_SET, params);
        if (result != null) {
            return result.getBoolean(StorageProvider.METHOD_SET);
        }
        return false;
    }

    @Override
    public Map<String, String> entries() {
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_ENTRIES, null);
        if (result != null) {
            return ((Map<String, String>) result.getSerializable(StorageProvider.METHOD_ENTRIES));
        }
        return null;
    }

    @Override
    public String key(int index) {
        Bundle params = new Bundle();
        params.putInt(StorageProvider.PARAM_INDEX, index);
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_KEY, params);
        if (result != null) {
            return result.getString(StorageProvider.METHOD_KEY);
        }
        return null;
    }

    @Override
    public int length() {
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_LENGTH, null);
        if (result != null) {
            return result.getInt(StorageProvider.METHOD_LENGTH);
        }
        return 0;
    }

    @Override
    public boolean delete(String key) {
        Bundle params = new Bundle();
        params.putString(StorageProvider.PARAM_KEY, key);
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_DELETE, params);
        if (result != null) {
            return result.getBoolean(StorageProvider.METHOD_DELETE);
        }
        return false;
    }

    @Override
    public boolean clear() {
        Bundle result = invokeRemoteCall(StorageProvider.METHOD_CLEAR, null);
        if (result != null) {
            return result.getBoolean(StorageProvider.METHOD_CLEAR);
        }
        return false;
    }

    private Bundle invokeRemoteCall(String method, Bundle params) {
        return mApplicationContext
                .getContext()
                .getContentResolver()
                .call(StorageProvider.getUri(AuthorityHolder.sAuthority), method, mPackage, params);
    }

    private static class AuthorityHolder {
        public static final String sAuthority;

        static {
            String platform = ResourceConfig.getInstance().getPlatform();
            if (TextUtils.isEmpty(platform)) {
                platform = Runtime.getInstance().getContext().getPackageName();
            }
            sAuthority = platform + ".storage";
        }
    }
}
