/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.annotation.DependencyAnnotation;
import org.hapjs.bridge.permission.RuntimePermissionProvider;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.cache.DefaultFileNotFoundHandler;
import org.hapjs.cache.InstallFileFlagManager;
import org.hapjs.cache.PackageListener;
import org.hapjs.card.support.CardConstants;
import org.hapjs.common.CommonMsgProvider;
import org.hapjs.common.DefaultCommonMsgProviderImpl;
import org.hapjs.common.net.BanNetworkProvider;
import org.hapjs.common.net.DefaultBanNetworkProviderImpl;
import org.hapjs.common.net.DefaultNetworkReportProviderImpl;
import org.hapjs.common.net.NetworkReportProvider;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.distribution.InstalledSubpackageManager;
import org.hapjs.launch.DeepLinkClient;
import org.hapjs.launch.LauncherManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.permission.RuntimePermissionProviderImpl;
import org.hapjs.persistence.AbstractDatabase;
import org.hapjs.persistence.HybridDatabaseHelper;
import org.hapjs.persistence.HybridProvider;
import org.hapjs.persistence.Table;
import org.hapjs.render.jsruntime.JsThreadFactory;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.utils.CrashHandler;
import org.hapjs.utils.ShortcutUtils;

@DependencyAnnotation(key = PlatformRuntime.PROPERTY_RUNTIME_IMPL_CLASS)
public class PlatformRuntime extends Runtime {
    private static final String TAG = "PlatformRuntime";

    @Override
    protected void doPreCreate(Context base) {
        super.doPreCreate(base);
        if (isDbProcess()) {
            // for more databases
            List<AbstractDatabase> databases = onCreateDatabase();
            HybridProvider.addDatabases(databases);
        }
        if (ProcessUtils.isAppProcess(base)) {
            JsThreadFactory.getInstance().preload(base);
        }
    }

    @Override
    protected void doCreate(Context context) {
        setLazyLoad(true);
        super.doCreate(context);
        Log.i(TAG, "Hybrid Application onCreate");
        Cache.setDefaultFileNotFoundHandler(new DefaultFileNotFoundHandler());
        onAllProcessInit();
        if (ProcessUtils.isMainProcess(context)) {
            onMainProcessInit();
        } else if (ProcessUtils.isAppProcess(context)) {
            onAppProcessInit();
        } else {
            onOtherProcessInit();
        }
    }

    protected boolean isDbProcess() {
        return ProcessUtils.isMainProcess(mContext);
    }

    /**
     * this method would only add tables in default hybrid database, use {@link
     * PlatformRuntime#onCreateDatabase()} and {@link AbstractDatabase#addTable(Table)} instead
     */
    @Deprecated
    protected List<Table> onCreateTable() {
        return null;
    }

    protected List<AbstractDatabase> onCreateDatabase() {
        List<AbstractDatabase> databases = new ArrayList<>();
        // for default hybrid database
        HybridDatabaseHelper hybridDatabase = new HybridDatabaseHelper(mContext);
        List<Table> tables = onCreateTable();
        hybridDatabase.addTables(tables);
        databases.add(hybridDatabase);
        return databases;
    }

    protected void onAllProcessInit() {
        LauncherManager.addClient(LauncherActivity.getLauncherClient());
        LauncherManager.addClient(DeepLinkClient.getInstance());
        ProviderManager.getDefault().addProvider(
                RuntimePermissionProvider.NAME, new RuntimePermissionProviderImpl(mContext, false));
        ProviderManager.getDefault().addProvider(
                BanNetworkProvider.NAME, new DefaultBanNetworkProviderImpl());
        ProviderManager.getDefault().addProvider(
                NetworkReportProvider.NAME, new DefaultNetworkReportProviderImpl(mContext));
        ProviderManager.getDefault().addProvider(
                CommonMsgProvider.NAME, new DefaultCommonMsgProviderImpl());
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
    }

    protected void onMainProcessInit() {
        CacheStorage.getInstance(mContext)
                .addPackageListener(
                        new PackageListener() {
                            @Override
                            public void onPackageInstalled(String pkg, AppInfo appInfo) {
                                if (appInfo == null) {
                                    Log.e(TAG, "expected a non-null appInfo.");
                                    return;
                                }
                                ShortcutUtils.updateShortcutAsync(mContext, pkg);
                                sendPackageChangeBroadcast(pkg,
                                        CardConstants.ACTION_PACKAGE_PACKAGE_ADDED);
                                InstalledSubpackageManager.clearOutdatedSubpackages(
                                        mContext, pkg, appInfo.getVersionCode());
                            }

                            @Override
                            public void onPackageUpdated(String pkg, AppInfo appInfo) {
                                if (appInfo == null) {
                                    Log.e(TAG, "expected a non-null appInfo.");
                                    return;
                                }
                                ShortcutUtils.updateShortcutAsync(mContext, pkg);
                                sendPackageChangeBroadcast(pkg,
                                        CardConstants.ACTION_PACKAGE_PACKAGE_UPDATED);
                                InstalledSubpackageManager.clearOutdatedSubpackages(
                                        mContext, pkg, appInfo.getVersionCode());
                            }

                            @Override
                            public void onPackageRemoved(String pkg) {
                                sendPackageChangeBroadcast(pkg,
                                        CardConstants.ACTION_PACKAGE_PACKAGE_REMOVED);
                                InstalledSubpackageManager.clearSubpackages(mContext, pkg);
                            }

                            @Override
                            public void onSubpackageInstalled(
                                    String pkg, SubpackageInfo subpackageInfo, int versionCode) {
                                if (subpackageInfo == null) {
                                    Log.e(TAG, "expected a non-null subpackageInfo.");
                                    return;
                                }
                                InstalledSubpackageManager.installSubpackage(
                                        mContext, pkg, subpackageInfo.getName(), versionCode);
                            }
                        });

        // 延迟执行, 以优化:
        // 1. 线程资源竞争
        // 2. 应用图标不存在时会触发安装, 与DistributionProviderProxy.addDebugingPkg形成竞争(调试场景下).
        new Handler()
                .postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                ShortcutUtils.updateAllShortcutsAsync(mContext);
                            }
                        },
                        10 * 1000);

        InstallFileFlagManager.clearAllFlags(mContext);
    }

    private void sendPackageChangeBroadcast(String pkg, String action) {
        Intent intent = new Intent(action);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(CardConstants.EXTRA_PACKAGE, pkg);
        intent.putExtra(CardConstants.EXTRA_PLATFORM, mContext.getPackageName());
        mContext.sendBroadcast(intent);
        // send to self explicitly above 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setPackage(mContext.getPackageName());
            mContext.sendBroadcast(intent);
        }
    }

    protected void onAppProcessInit() {
        Runtime.getInstance().ensureLoad();
    }

    protected void onOtherProcessInit() {
    }
}
