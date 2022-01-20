/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data.internal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.Runtime;

public class StorageFactory {

    private ConcurrentHashMap<String, IStorage> mStorageMap = new ConcurrentHashMap<>();

    private StorageFactory() {
    }

    public static StorageFactory getInstance() {
        return Holder.INSTANCE;
    }

    public void preload(ApplicationContext context) {
        // just create storage instance, when this function is finished, data transform will be done
        create(context);
    }

    public void preloadAll() {
        List<Cache> caches =
                CacheStorage.getInstance(Runtime.getInstance().getContext()).availableCaches();
        for (Cache cache : caches) {
            if (cache != null) {
                create(HapEngine.getInstance(cache.getPackageName()).getApplicationContext());
            }
        }
    }

    public IStorage create(ApplicationContext context) {
        IStorage storage = mStorageMap.get(context.getPackage());
        if (storage == null) {
            if (HapEngine.getInstance(context.getPackage()).isCardMode()) {
                storage = new RemoteStorage(context);
            } else {
                storage = new LocalStorage(context);
            }
            mStorageMap.put(context.getPackage(), storage);
        }
        return storage;
    }

    public void clear() {
        SQLiteStorage.reset();
        MMKVStorage.reset();
        mStorageMap.clear();
    }

    public void clear(String packageName) {
        SQLiteStorage.reset(packageName);
        MMKVStorage.reset(packageName);
        mStorageMap.remove(packageName);
    }

    private static class Holder {
        static final StorageFactory INSTANCE = new StorageFactory();
    }
}
