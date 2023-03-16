/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.LayoutInflaterCompat;
import java.lang.reflect.Field;
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
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.utils.CrashHandler;
import org.hapjs.utils.ShortcutUtils;

@DependencyAnnotation(key = PlatformRuntime.PROPERTY_RUNTIME_IMPL_CLASS)
public class PlatformRuntime extends Runtime implements Application.ActivityLifecycleCallbacks{
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
        Context applicationContext = Runtime.getInstance().getContext().getApplicationContext();
        if (applicationContext instanceof Application) {
            ((Application) applicationContext).registerActivityLifecycleCallbacks(this);
        }
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
                                updateShortcutAsync(mContext, false, pkg);
                                sendPackageChangeBroadcast(pkg,
                                        CardConstants.ACTION_PACKAGE_PACKAGE_ADDED);
                                InstalledSubpackageManager.getInstance().clearOutdatedSubpackages(
                                        mContext, pkg, appInfo.getVersionCode());
                            }

                            @Override
                            public void onPackageUpdated(String pkg, AppInfo appInfo) {
                                if (appInfo == null) {
                                    Log.e(TAG, "expected a non-null appInfo.");
                                    return;
                                }
                                updateShortcutAsync(mContext, false, pkg);
                                sendPackageChangeBroadcast(pkg,
                                        CardConstants.ACTION_PACKAGE_PACKAGE_UPDATED);
                                InstalledSubpackageManager.getInstance().clearOutdatedSubpackages(
                                        mContext, pkg, appInfo.getVersionCode());
                            }

                            @Override
                            public void onPackageRemoved(String pkg) {
                                sendPackageChangeBroadcast(pkg,
                                        CardConstants.ACTION_PACKAGE_PACKAGE_REMOVED);
                                InstalledSubpackageManager.getInstance().clearSubpackages(mContext, pkg);
                            }

                            @Override
                            public void onSubpackageInstalled(
                                    String pkg, SubpackageInfo subpackageInfo, int versionCode) {
                                if (subpackageInfo == null) {
                                    Log.e(TAG, "expected a non-null subpackageInfo.");
                                    return;
                                }
                                InstalledSubpackageManager.getInstance().installSubpackage(
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
                                updateShortcutAsync(mContext, true, null);
                            }
                        },
                        10 * 1000);

        InstallFileFlagManager.clearAllFlags(mContext);
    }

    //新增方法，由厂商自己实现
    protected void updateShortcutAsync(Context context, boolean isAllShortcut, String packageName) {
        if (isAllShortcut) {
            ShortcutUtils.updateAllShortcutsAsync(context);
        } else if (!TextUtils.isEmpty(packageName)) {
            ShortcutUtils.updateShortcutAsync(context, packageName);
        }
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

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        GrayModeManager.getInstance().init(activity.getApplicationContext());
        if (!GrayModeManager.getInstance().shouldApplyGrayMode()) {
            return;
        }
        // 设置弹窗变灰
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        if (activity instanceof AppCompatActivity || layoutInflater.getFactory2() != null) {
            try {
                //通过反射设置mFactorySet为false,以便下一步重新设置Factory2解决 nfc dialog无黑白化处理问题
                Field mFactorySet = LayoutInflater.class.getDeclaredField("mFactorySet");
                mFactorySet.setAccessible(true);
                mFactorySet.set(layoutInflater, false);
            } catch (Exception e) {
                // 反射异常时只对界面做黑白化处理 降级方案
                Log.e("GrayMode", "Refactor exception", e);
            }
        }
        LayoutInflaterCompat.setFactory2(layoutInflater, new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                //方法进入即可对activity /fragment /dialog 做黑白化处理
                if ("FrameLayout".equals(name)) {
                    int count = attrs.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        String attrName = attrs.getAttributeName(i);
                        String attrValue = attrs.getAttributeValue(i);
                        if (TextUtils.equals(attrName, "id")) {
                            int id = Integer.parseInt(attrValue.substring(1));
                            String idValue = getContext().getResources().getResourceName(id);
                            if ("android:id/content".equals(idValue)) {
                                FrameLayout grayFrameLayout = new FrameLayout(context, attrs);
                                //这里不采用自定义FrameLayout 解决webview显示问题
                                GrayModeManager.getInstance().applyGrayMode(grayFrameLayout, true);
                                //替换掉根布局的FrameLayout 采用原生FrameLayout 无需自定义
                                return grayFrameLayout;
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                Log.e("GrayMode", "onCreateView name:" + name + ",no parent view");
                return null;
            }
        });
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
