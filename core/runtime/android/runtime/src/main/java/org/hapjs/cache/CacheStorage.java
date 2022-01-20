/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hapjs.common.executors.Executors;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;

public class CacheStorage {
    private static final String TAG = "CacheStorage";
    private static volatile CacheStorage sInstance;
    private Context mContext;
    private Map<String, Cache> mCaches;
    private CacheLimitStrategy mCacheLimitStrategy;
    private CopyOnWriteArrayList<PackageListener> mPackageListeners;

    private CacheStorage(Context context) {
        mContext = context;
        mCaches = new HashMap<String, Cache>();
        mPackageListeners = new CopyOnWriteArrayList<>();
        mCacheLimitStrategy = CacheLimitStrategies.simpleCacheLimitStrategy();
    }

    public static synchronized CacheStorage getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CacheStorage(context.getApplicationContext());
        }
        return sInstance;
    }

    protected Context getContext() {
        return mContext;
    }

    public synchronized Cache getCache(String pkg) {
        Cache cache = mCaches.get(pkg);
        if (cache == null) {
            cache = new Cache(this, pkg);
            mCaches.put(pkg, cache);
        }
        return cache;
    }

    public synchronized boolean hasCache(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            Log.w(TAG, "hasCache >> pkg is null.");
            return false;
        }
        return getCache(pkg).ready();
    }

    public synchronized void removeCache(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            Log.w(TAG, "removeCache >> pkg is null.");
            return;
        }
        getCache(pkg).remove();
        mCaches.remove(pkg);
    }

    protected long availableSize() {
        return mCacheLimitStrategy.availableSize();
    }

    protected void setCacheLimitStrategy(CacheLimitStrategy cacheLimitStrategy) {
        if (cacheLimitStrategy != null) {
            mCacheLimitStrategy = cacheLimitStrategy;
        }
    }

    public List<Cache> availableCaches() {
        List<Cache> caches = new ArrayList<Cache>();
        File baseCacheDir = Cache.getResourceRootDir(mContext);
        File[] cacheFiles = baseCacheDir.listFiles();
        if (cacheFiles != null && cacheFiles.length > 0) {
            for (File cacheFile : cacheFiles) {
                Cache cache = getCache(cacheFile.getName());
                if (cache.ready()) {
                    caches.add(cache);
                }
            }
        }
        return caches;
    }

    protected long size() {
        long total = 0;
        for (Cache cache : availableCaches()) {
            total += cache.size();
        }
        return total;
    }

    public void install(String pkg, String packagePath) throws CacheException {
        install(pkg, new File(packagePath));
    }

    public void install(String pkg, File packageFile) throws CacheException {
        boolean isUpdate = getCache(pkg).isUpdate();
        install(pkg, new FilePackageInstaller(mContext, pkg, packageFile, isUpdate));
    }

    public void install(String pkg, PackageInstaller installer) throws CacheException {
        getCache(pkg).install(installer);
    }

    public synchronized void uninstall(String pkg) {
        getCache(pkg).uninstall();
        mCaches.remove(pkg);
    }

    public String getPackageSign(String pkg) {
        return getCache(pkg).getPackageSign();
    }

    public void addPackageListener(PackageListener packageListener) {
        mPackageListeners.add(packageListener);
    }

    public void removePackageListener(PackageListener packageListener) {
        mPackageListeners.remove(packageListener);
    }

    public void dispatchPackageInstalled(
            final String pkg, final AppInfo appInfo, final boolean update) {
        if (mPackageListeners.isEmpty()) {
            return;
        }
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(pkg,
                                            "CacheStorage#dispatchPackageInstalled");
                            for (final PackageListener l : mPackageListeners) {
                                if (update) {
                                    l.onPackageUpdated(pkg, appInfo);
                                } else {
                                    l.onPackageInstalled(pkg, appInfo);
                                }
                            }
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(pkg,
                                            "CacheStorage#dispatchPackageInstalled");
                        });
    }

    public void dispatchSubpackageInstalled(
            final String pkg, final SubpackageInfo subpackageInfo, final int versionCode) {
        if (mPackageListeners.isEmpty()) {
            return;
        }
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(pkg,
                                            "CacheStorage#dispatchSubpackageInstalled");
                            for (final PackageListener l : mPackageListeners) {
                                l.onSubpackageInstalled(pkg, subpackageInfo, versionCode);
                            }
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(pkg,
                                            "CacheStorage#dispatchSubpackageInstalled");
                        });
    }

    protected void dispatchPackageRemoved(final String pkg) {
        if (mPackageListeners.isEmpty()) {
            return;
        }
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(pkg,
                                            "CacheStorage#dispatchPackageRemoved");
                            for (final PackageListener l : mPackageListeners) {
                                l.onPackageRemoved(pkg);
                            }
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(pkg,
                                            "CacheStorage#dispatchPackageRemoved");
                        });
    }
}
