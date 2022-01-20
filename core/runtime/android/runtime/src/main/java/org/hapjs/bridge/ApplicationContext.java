/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.ArrayMap;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.hapjs.bridge.storage.file.IResourceFactory;
import org.hapjs.bridge.storage.file.Resource;
import org.hapjs.bridge.storage.file.ResourceFactory;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.event.ClearDataEvent;
import org.hapjs.event.EventManager;
import org.hapjs.model.AppInfo;
import org.hapjs.render.IPage;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;

public class ApplicationContext {

    private static final String TAG = "ApplicationContext";

    private static ArrayMap<String, ArrayMap<File, SharedPreferences>> sSharedPrefsCache;
    private ArrayList<PageLifecycleCallbacks> mPageLifecycleCallbacks = new ArrayList<>();
    private Context mContext;
    private String mPackage;
    private IResourceFactory mResourceFactory;
    private ArrayMap<String, File> mSharedPrefsPaths;

    /**
     * Create a new ApplicationContext instance from a context and package name.
     *
     * @param context android context
     * @param pkg     package name of current App
     * @throws IllegalArgumentException If pkg is null.
     */
    public ApplicationContext(Context context, String pkg) {
        mContext = context.getApplicationContext();
        if (pkg == null) {
            // getXXXDir may throws NullPointerException when pkg is null
            throw new IllegalArgumentException("Package Name is not valid");
        }
        mPackage = pkg;
        mResourceFactory = new ResourceFactory(this);
    }

    private static ApplicationProvider getApplicationProvider() {
        return ApplicationProviderHolder.get();
    }

    public Context getContext() {
        return mContext;
    }

    public String getPackage() {
        return mPackage;
    }

    public IResourceFactory getResourceFactory() {
        return mResourceFactory;
    }

    public AppInfo getAppInfo() {
        return getAppInfo(true);
    }

    public AppInfo getAppInfo(boolean useCache) {
        Cache cache = CacheStorage.getInstance(mContext).getCache(mPackage);
        return cache.getAppInfo(useCache);
    }

    public void reset() {
        CacheStorage.getInstance(mContext).getCache(mPackage).clearAppInfo();
    }

    public String getName() {
        AppInfo appInfo = getAppInfo();
        if (appInfo == null) {
            return null;
        } else {
            return appInfo.getName();
        }
    }

    public Uri getIcon() {
        AppInfo appInfo = getAppInfo();
        if (appInfo == null || TextUtils.isEmpty(appInfo.getIcon())) {
            return null;
        }
        return HapEngine.getInstance(mPackage).getResourceManager().getResource(appInfo.getIcon());
    }

    public File getCacheDir() {
        return getApplicationProvider().getCacheDir(mContext, mPackage);
    }

    public File getFilesDir() {
        return getApplicationProvider().getFilesDir(mContext, mPackage);
    }

    public File getMassDir() {
        return getApplicationProvider().getMassDir(mContext, mPackage);
    }

    public File getDatabaseDir() {
        return getApplicationProvider().getDatabaseDir(mContext, mPackage);
    }

    public File getApplicationDir() {
        File dir = new File(mContext.getDir("resource", Context.MODE_PRIVATE), mPackage);
        return ensureDir(dir);
    }

    public File getSharedPrefDir() {
        return getApplicationProvider().getSharedPrefDir(mContext, mPackage);
    }

    public File getDatabasePath(String name) {
        return new File(getDatabaseDir(), name);
    }

    public File getSharePrefFile(String name) {
        return new File(getSharedPrefDir(), name + ".xml");
    }

    public File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix, getCacheDir());
    }

    public String getInternalUri(File file) {
        Resource resource = mResourceFactory.create(file);
        return resource != null ? resource.toUri() : null;
    }

    public String getInternalUri(Uri uri) {
        return getInternalUri(uri, true);
    }

    public String getInternalUri(Uri uri, boolean needFilename) {
        Resource resource = mResourceFactory.create(uri, needFilename);
        return resource != null ? resource.toUri() : null;
    }

    public String getInternalUri(ParcelFileDescriptor fileDescriptor) {
        Resource resource = mResourceFactory.create(fileDescriptor);
        return resource != null ? resource.toUri() : null;
    }

    public Uri getUnderlyingUri(String internalUri) {
        Resource resource = mResourceFactory.create(internalUri);
        return resource != null ? resource.getUnderlyingUri() : null;
    }

    public File getUnderlyingFile(String internalUri) {
        Resource resource = mResourceFactory.create(internalUri);
        return resource != null ? resource.getUnderlyingFile() : null;
    }

    public Resource getResource(String internalUri) {
        return mResourceFactory.create(internalUri);
    }

    public SharedPreferences getSharedPreference() {
        return getSharedPreferences(getSharedPreferenceName(), Context.MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences(String name, int mode) {
        File file;
        synchronized (ApplicationContext.class) {
            if (mSharedPrefsPaths == null) {
                mSharedPrefsPaths = new ArrayMap<>();
            }
            file = mSharedPrefsPaths.get(name);
            if (file == null) {
                file = getSharePrefFile(name);
                mSharedPrefsPaths.put(name, file);
            }
        }
        return getSharedPreferences(file, mode);
    }

    private SharedPreferences getSharedPreferences(File file, int mode) {
        checkMode(mode);
        SharedPreferences sp;
        synchronized (ApplicationContext.class) {
            final ArrayMap<File, SharedPreferences> cache = getSharedPreferencesCacheLocked();
            sp = cache.get(file);
            if (sp == null) {
                try {
                    Class spiClass = Class.forName("android.app.SharedPreferencesImpl");
                    Constructor constructor =
                            spiClass.getDeclaredConstructor(File.class, int.class);
                    constructor.setAccessible(true);
                    sp = (SharedPreferences) constructor.newInstance(file, mode);
                    cache.put(file, sp);
                    return sp;
                } catch (ClassNotFoundException
                        | NoSuchMethodException
                        | IllegalAccessException
                        | InstantiationException
                        | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return sp;
    }

    private ArrayMap<File, SharedPreferences> getSharedPreferencesCacheLocked() {
        if (sSharedPrefsCache == null) {
            sSharedPrefsCache = new ArrayMap<>();
        }

        ArrayMap<File, SharedPreferences> packagePrefs = sSharedPrefsCache.get(mPackage);
        if (packagePrefs == null) {
            packagePrefs = new ArrayMap<>();
            sSharedPrefsCache.put(mPackage, packagePrefs);
        }

        return packagePrefs;
    }

    private void checkMode(int mode) {
        if (mContext.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.N) {
            if ((mode & Context.MODE_WORLD_READABLE) != 0) {
                throw new SecurityException("MODE_WORLD_READABLE no longer supported");
            }
            if ((mode & Context.MODE_WORLD_WRITEABLE) != 0) {
                throw new SecurityException("MODE_WORLD_WRITEABLE no longer supported");
            }
        }
    }

    private String getSharedPreferenceName() {
        return "default";
    }

    private File ensureDir(File dir) {
        return FileUtils.mkdirs(dir) ? dir : null;
    }

    public long getDiskUsage() {
        return getApplicationProvider().getDiskUsage(mContext, mPackage);
    }

    public void clearData() {
        getApplicationProvider().clearData(mContext, mPackage);
        EventManager.getInstance().invoke(new ClearDataEvent(mPackage));
    }

    @Override
    public int hashCode() {
        return mPackage.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof ApplicationContext)) {
            return false;
        }
        return mPackage.equals(((ApplicationContext) obj).mPackage);
    }

    public void registerPageLifecycleCallbacks(PageLifecycleCallbacks callback) {
        mPageLifecycleCallbacks.add(callback);
    }

    public void unregisterPageLifecycleCallbacks(PageLifecycleCallbacks callback) {
        mPageLifecycleCallbacks.remove(callback);
    }

    public void dispatchPageStart(IPage page) {
        for (PageLifecycleCallbacks callback : mPageLifecycleCallbacks) {
            callback.onPageStart(page);
        }
    }

    public void dispatchPageStop(IPage page) {
        for (PageLifecycleCallbacks callback : mPageLifecycleCallbacks) {
            callback.onPageStop(page);
        }
    }

    public void dispatchPageDestroy(IPage page) {
        for (PageLifecycleCallbacks callback : mPageLifecycleCallbacks) {
            callback.onPageDestroy(page);
        }
    }

    public interface PageLifecycleCallbacks {
        void onPageStart(@NonNull IPage page);

        void onPageStop(@NonNull IPage page);

        void onPageDestroy(@NonNull IPage page);
    }

    private static class ApplicationProviderHolder {
        private static volatile ApplicationProvider sApplicationProvider;

        static ApplicationProvider get() {
            if (sApplicationProvider == null) {
                sApplicationProvider =
                        ProviderManager.getDefault().getProvider(ApplicationProvider.NAME);
            }
            return sApplicationProvider;
        }
    }
}
