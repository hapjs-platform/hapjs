/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data;

import java.util.HashMap;
import java.util.Map;
import org.hapjs.features.storage.data.internal.IStorage;
import org.hapjs.features.storage.data.internal.StorageFactory;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.inspect.protocols.IDOMStorage;

/**
 * {@link DOMStorageImpl} will be injected to debug engine via reflection
 */
public class DOMStorageImpl implements IDOMStorage {

    private static volatile DOMStorageImpl sInstance;
    private String mPackageName;
    private IStorage mLocalStorage;

    public DOMStorageImpl(String pkg) {
        mPackageName = pkg;
        mLocalStorage =
                StorageFactory.getInstance()
                        .create(HapEngine.getInstance(mPackageName).getApplicationContext());
    }

    public static DOMStorageImpl getInstance(String pkg) {
        if (sInstance == null) {
            synchronized (DOMStorageImpl.class) {
                if (sInstance == null) {
                    sInstance = new DOMStorageImpl(pkg);
                }
            }
        }
        return sInstance;
    }

    @Override
    public Map<String, String> entries() {
        if (!checkStorage()) {
            return new HashMap<>();
        }

        return mLocalStorage.entries();
    }

    @Override
    public String getItem(String key) {
        if (!checkStorage()) {
            return null;
        }
        return mLocalStorage.get(key);
    }

    @Override
    public void setItem(String key, String value) {
        if (!checkStorage()) {
            return;
        }
        mLocalStorage.set(key, value);
    }

    @Override
    public void removeItem(String key) {
        if (!checkStorage()) {
            return;
        }
        mLocalStorage.delete(key);
    }

    @Override
    public void clear() {
        if (!checkStorage()) {
            return;
        }
        mLocalStorage.clear();
    }

    private boolean checkStorage() {
        return mLocalStorage != null;
    }
}
