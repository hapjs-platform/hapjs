/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import org.hapjs.bridge.AppInfoProvider;
import org.hapjs.bridge.ApplicationProvider;
import org.hapjs.bridge.DefaultAppInfoProvider;
import org.hapjs.bridge.DefaultApplicationProvider;
import org.hapjs.bridge.DefaultFitWidescreenProvider;
import org.hapjs.bridge.DependencyManager;
import org.hapjs.bridge.FitWidescreenProvider;
import org.hapjs.bridge.annotation.DependencyAnnotation;
import org.hapjs.bridge.provider.webview.WebviewSettingProvider;
import org.hapjs.bridge.provider.webview.WebviewSettingProviderImpl;
import org.hapjs.cache.DefaultInstallInterceptProviderImpl;
import org.hapjs.cache.DefaultPackageCheckProvider;
import org.hapjs.cache.InstallInterceptProvider;
import org.hapjs.cache.PackageCheckProvider;
import org.hapjs.common.net.UserAgentHelper;
import org.hapjs.common.utils.DefaultStatusBarSizeProvider;
import org.hapjs.common.utils.FrescoUtils;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.common.utils.SoLoaderHelper;
import org.hapjs.common.utils.StatusBarSizeProvider;
import org.hapjs.component.constants.DefaultFontSizeProvider;
import org.hapjs.component.constants.FontSizeProvider;
import org.hapjs.pm.DefaultNativePackageProviderImpl;
import org.hapjs.pm.NativePackageProvider;
import org.hapjs.render.DefaultFontFamilyProvider;
import org.hapjs.render.FontFamilyProvider;
import org.hapjs.render.jsruntime.Profiler;
import org.hapjs.system.DefaultSysOpProviderImpl;
import org.hapjs.system.SysOpProvider;

@DependencyAnnotation(key = Runtime.PROPERTY_RUNTIME_IMPL_CLASS)
public class Runtime {
    public static final String PROPERTY_RUNTIME_IMPL_CLASS = "RuntimeImplClass";
    private static final String TAG = "Runtime";
    private static String sPlatform;
    protected Context mContext;
    private boolean mPreCreated;
    private volatile boolean mCreated;
    private boolean mLazyLoad;
    private long mCreateStartTime;

    protected Runtime() {
    }

    public static String getPlatform() {
        return sPlatform;
    }

    public static void setPlatform(String platform) {
        sPlatform = platform;
    }

    public static Runtime getInstance() {
        return Holder.INSTANCE;
    }

    public final synchronized void onPreCreate(Context base) {
        if (ProcessUtils.isAppProcess(base)) {
            Profiler.recordAppStart(System.nanoTime());
        }
        if (mPreCreated) {
            Log.d(TAG, "already pre created! ");
            return;
        }
        long preCreateStartTime = System.currentTimeMillis();
        setContext(base);
        doPreCreate(mContext);
        mPreCreated = true;
        long preCreateEndTime = System.currentTimeMillis();
        Log.i(TAG, "onPreCreate last for: " + (preCreateEndTime - preCreateStartTime));
    }

    public final synchronized void onCreate(Context context) {
        if (mCreated) {
            Log.d(TAG, "already created! ");
            return;
        }
        mCreateStartTime = System.currentTimeMillis();
        setContext(context);
        doCreate(context);
        if (ProcessUtils.isAppProcess(context)) {
            Profiler.checkProfilerState();
        }
        synchronized (this) {
            mCreated = true;
            notifyAll();
        }
        long createEndTime = System.currentTimeMillis();
        Log.i(TAG, "onCreate last for: " + (createEndTime - mCreateStartTime));
    }

    public void waitUntilCreated() {
        if (mCreated) {
            return;
        }
        synchronized (this) {
            if (mCreated) {
                return;
            }
            try {
                wait(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "interrupted while waiting", e);
            }
            if (!mCreated) {
                throw new IllegalStateException(
                        "Application not created, onCreate start at: " + mCreateStartTime);
            }
        }
    }

    public Context getContext() {
        if (mContext == null) {
            waitUntilCreated();
        }
        return mContext;
    }

    private void setContext(Context context) {
        if (mContext == null) {
            if (context != null && context.getApplicationContext() != null) {
                mContext = context.getApplicationContext();
            } else {
                mContext = context;
            }
        }
    }

    public void setLazyLoad(boolean lazyLoad) {
        mLazyLoad = lazyLoad;
    }

    public void ensureLoad() {
        mLazyLoad = false;
        load();
    }

    public String getVendor() {
        return Build.MANUFACTURER.toLowerCase();
    }

    protected void doPreCreate(Context context) {
    }

    protected void doCreate(Context context) {
        ProviderManager pm = ProviderManager.getDefault();
        pm.addProvider(SysOpProvider.NAME, new DefaultSysOpProviderImpl());
        pm.addProvider(NativePackageProvider.NAME, new DefaultNativePackageProviderImpl());
        pm.addProvider(ApplicationProvider.NAME, new DefaultApplicationProvider());
        pm.addProvider(AppInfoProvider.NAME, new DefaultAppInfoProvider());
        pm.addProvider(FitWidescreenProvider.NAME, new DefaultFitWidescreenProvider());
        pm.addProvider(PackageCheckProvider.NAME, new DefaultPackageCheckProvider());
        pm.addProvider(ThemeProvider.NAME, new DefaultThemeProvider());
        ProviderManager.getDefault()
                .addProvider(InstallInterceptProvider.NAME,
                        new DefaultInstallInterceptProviderImpl());
        pm.addProvider(RouterManageProvider.NAME, new DefaultRouterManageProvider());
        pm.addProvider(HybridDialogProvider.NAME, new DefaultHybridDialogProviderImpl());
        pm.addProvider(StatusBarSizeProvider.NAME, new DefaultStatusBarSizeProvider());
        pm.addProvider(FontSizeProvider.NAME, new DefaultFontSizeProvider());
        pm.addProvider(FontFamilyProvider.NAME, new DefaultFontFamilyProvider());
        pm.addProvider(WebviewSettingProvider.NAME, new WebviewSettingProviderImpl());
        if (!mLazyLoad) {
            load();
        }
    }

    private void load() {
        UserAgentHelper.preLoad();
        FrescoUtils.initializeAsync(mContext);
        mContext.registerComponentCallbacks(
                new ComponentCallbacks2() {
                    @Override
                    public void onTrimMemory(int level) {
                        FrescoUtils.trimOnLowMemory();
                    }

                    @Override
                    public void onConfigurationChanged(Configuration newConfig) {
                        // ignore
                    }

                    @Override
                    public void onLowMemory() {
                        // ignore
                    }
                });
    }

    private static class Holder {
        static Runtime INSTANCE = createRuntime();

        private static Runtime createRuntime() {
            if (!ResourceConfig.getInstance().isLoadFromLocal()) {
                // card should use Runtime only
                return new Runtime();
            }
            DependencyManager.Dependency dependency =
                    DependencyManager.getInstance().getDependency(PROPERTY_RUNTIME_IMPL_CLASS);
            if (dependency != null) {
                try {
                    return (Runtime) Class.forName(dependency.getClassName()).newInstance();
                } catch (ReflectiveOperationException e) {
                    Log.e(TAG, "Fail to instantiate Runtime", e);
                }
            }
            return new Runtime();
        }
    }
}
