/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data.internal;

import android.text.TextUtils;
import android.util.Log;
import com.tencent.mmkv.MMKV;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.ApplicationContext;

public class MMKVStorage implements IStorage {
    private static final String TAG = "MMKVStorage";
    private static final ConcurrentHashMap<String, MMKV> sMMKVStorageMap =
            new ConcurrentHashMap<>();
    private ApplicationContext mApplicationContext;

    public MMKVStorage(ApplicationContext context) {
        String rootDir = MMKV.getRootDir();
        if (TextUtils.isEmpty(rootDir)) {
            MMKV.initialize(context.getContext().getApplicationInfo().dataDir);
        } else {
            Log.i(TAG, "MMKVStorage has already initialized: " + rootDir);
        }
        mApplicationContext = context;
    }

    public static void reset() {
        Iterator<String> it = sMMKVStorageMap.keySet().iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            MMKV mmkv = sMMKVStorageMap.get(pkg);
            if (mmkv != null) {
                mmkv.clearMemoryCache();
                mmkv.close();
            }
            it.remove();
        }
    }

    public static void reset(String packageName) {
        MMKV mmkv = sMMKVStorageMap.remove(packageName);
        if (mmkv != null) {
            mmkv.clearMemoryCache();
            mmkv.close();
        }
    }

    @Override
    public String get(String key) {
        return getMMKV(mApplicationContext).decodeString(key);
    }

    @Override
    public boolean set(String key, String value) {
        return getMMKV(mApplicationContext).encode(key, value);
    }

    @Override
    public Map<String, String> entries() {
        Map<String, String> result = new LinkedHashMap<>();
        String[] keys = getMMKV(mApplicationContext).allKeys();
        if (keys != null) {
            for (String key : keys) {
                result.put(key, get(key));
            }
        }
        return result;
    }

    @Override
    public String key(int index) {
        String[] keys = getMMKV(mApplicationContext).allKeys();
        if (keys != null && index < keys.length) {
            return keys[index];
        }
        return null;
    }

    @Override
    public int length() {
        String[] keys = getMMKV(mApplicationContext).allKeys();
        if (keys != null) {
            return keys.length;
        }
        return 0;
    }

    @Override
    public boolean delete(String key) {
        MMKV mmkv = getMMKV(mApplicationContext);
        if (mmkv.containsKey(key)) {
            getMMKV(mApplicationContext).removeValueForKey(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean clear() {
        getMMKV(mApplicationContext).clearAll();
        return true;
    }

    private MMKV getMMKV(ApplicationContext context) {
        String pkg = context.getPackage();
        MMKV mmkv = sMMKVStorageMap.get(pkg);
        if (mmkv == null) {
            mmkv =
                    MMKV.mmkvWithID(pkg, MMKV.MULTI_PROCESS_MODE, null,
                            context.getDatabaseDir().getPath());
            MMKV old = sMMKVStorageMap.putIfAbsent(pkg, mmkv);
            if (old != null) {
                mmkv = old;
            }
        }
        return mmkv;
    }
}
