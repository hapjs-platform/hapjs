/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.view.GenericDraweeView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.hapjs.animation.RoundedLineAnimationDrawable;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.HybridView;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FrescoUtils;
import org.hapjs.common.utils.IconUtils;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.distribution.DistributionManager;
import org.hapjs.distribution.PreviewInfo;
import org.hapjs.event.FirstRenderActionEvent;
import org.hapjs.launch.Launcher;
import org.hapjs.launch.LauncherManager;
import org.hapjs.launch.ResidentDbUpdatorImpl;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.platform.R;
import org.hapjs.render.AppResourcesLoader;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.PageNotFoundException;
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.runtime.Checkable;
import org.hapjs.runtime.CheckableAlertDialog;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.utils.ActivityUtils;
import org.hapjs.utils.LocalBroadcastHelper;
import org.hapjs.utils.PreferenceUtils;
import org.hapjs.utils.ShortcutUtils;
import org.hapjs.utils.SystemController;

public class LauncherActivity extends RuntimeActivity {
    public static final String EXTRA_THEME_MODE = "THEME_MODE";
    protected static final int ON_APP_START = 0;
    protected static final int ON_APP_EXIT = 1;
    private static final String TAG = "LauncherActivity";
    private static final String FRAGMENT_TAG_DIALOG = "dialog";
    private static final int MESSAGE_REMIND_SHORTCUT = 1;
    private static final int MESSAGE_INSTALL_TIMEOUT = 2;
    private static final int MESSAGE_UPDATE_TIMEOUT = 3;
    private static final int RELOAD_EXTRA_DELAY_TIME = 1000;
    private static final int INSTALL_TIMEOUT = 60 * 1000;
    private static final int UPDATE_TIMEOUT = 2 * 1000;
    private static final int MIN_SPLASH_DISPLAY_DURATION = 800;
    private static final int SPLASH_DISMISS_DURATION = 300;
    private static final int SPLASH_TIME_OUT = 5000;
    protected View mSplashRoot;
    protected int mAppStatus;
    protected int mCurrentStatusCode;
    protected long mLaunchAppTime = 0;
    protected View.OnClickListener mBackClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
                    decorView.removeView(decorView.findViewById(R.id.loading_failed_view));
                    HybridView hybridView = getHybridView();
                    if (hybridView != null && hybridView.getWebView() instanceof RootView) {
                        PageManager pm = ((RootView) hybridView.getWebView()).getPageManager();
                        if (pm == null || pm.getPageCount() <= 1) {
                            finish();
                        } else {
                            getHybridView().goBack();
                        }
                    } else {
                        finish();
                    }
                }
            };
    private boolean mLoading;
    private boolean mLoadNewVersion;
    private TextView mAppName;
    private TextView mLoadingMsg;
    private RoundedLineAnimationDrawable mAnimationDrawable;
    private Handler mHandler;
    private ShortcutDialogFragment mShortcutDialog;
    private boolean mHasBroadcast;
    private String mTaskDescriptionPackage;
    private PreviewInfo mPreviewInfo;
    private int mInstallErrorCache;
    private long mSplashStartTs;
    private boolean mNewIntent = false;
    private DistributionManager.InstallStatusListener mInstallStatusListener;
    private boolean mIsFromRecentsTask = false;
    protected View.OnClickListener mInstallAndReloadClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
                    decorView.removeView(decorView.findViewById(R.id.loading_failed_view));
                    HybridView hybridView = getHybridView();
                    if (hybridView != null && hybridView.getWebView() instanceof RootView) {
                        RootView rootView = (RootView) hybridView.getWebView();
                        PageManager pm = rootView.getPageManager();
                        if (pm == null || pm.getPageCount() <= 1) {
                            removeHybridView();
                            installAndReload();
                        } else {
                            try {
                                Page curPage = pm.getCurrPage();
                                if (curPage != null) {
                                    Page newPage = pm.buildPage(curPage.getRequest());
                                    pm.replace(newPage);

                                    registerInstallListener(getPackage());
                                    install(curPage.getPath());
                                } else {
                                    Log.e(TAG, "onClick: curPage is null");
                                }
                            } catch (PageNotFoundException e) {
                                Log.e(TAG, "failed to build page", e);
                            }
                        }
                    } else {
                        removeHybridView();
                        installAndReload();
                    }
                }
            };

    public static LauncherManager.LauncherClient getLauncherClient() {
        return Client.IMPL;
    }

    public static void launch(Context context, String packageName, String path, Source source) {
        Intent intent = new Intent(IntentUtils.getLaunchAction(context));
        intent.putExtra(LauncherActivity.EXTRA_APP, packageName);
        intent.putExtra(LauncherActivity.EXTRA_PATH, path);
        intent.putExtra(LauncherActivity.EXTRA_SOURCE, source.toJson().toString());
        LauncherManager.launch(context, intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIsFromRecentsTask = isLaunchFromRecentsTask(savedInstanceState);
        mLoadNewVersion = false;
        mHandler = new CustomHandler(this);
        SystemController.getInstance().config(this, getIntent());
        setDefaultThemeMode();
        super.onCreate(savedInstanceState);
        setupDirectBack();
    }

    @Override
    public void startActivity(Intent intent) {
        boolean consumed = ActivityHookManager.onStartActivity(this, intent);
        if (!consumed) {
            super.startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        String pkg = getPackage();
        if (pkg != null && !pkg.isEmpty()) {
            activeApp(pkg);
            remindShortcut(pkg, ON_APP_START);
        }
        if (!TextUtils.isEmpty(getRunningPackage())) {
            mLaunchAppTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String pkg = getPackage();
        if (!TextUtils.isEmpty(pkg)) {
            broadcastRunningApp(pkg);
        }
    }

    @Override
    protected void onPause() {
        mHasBroadcast = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHandler.removeMessages(MESSAGE_REMIND_SHORTCUT);
        super.onStop();
        EventBus.getDefault().unregister(this);
        dismissSplashView(true);
        PlatformLogManager.getDefault().logAppUsage(getPackage(), mLaunchAppTime);
        mLaunchAppTime = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inactiveApp();
        cancelDialogs();
        unregisterInstallListener(getPackage());
        if (DistributionManager.CODE_APPLY_UPDATE_DELAYED == mCurrentStatusCode) {
            DistributionManager.getInstance().applyUpdate(getPackage());
        }
        if (!TextUtils.isEmpty(getPackage())) {
            AppResourcesLoader.clearPreloadedResources(getPackage());
        }
    }

    protected boolean isLaunchFromRecentsTask(Bundle savedInstanceState) {
        return (savedInstanceState != null)
                || ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
    }

    protected void onPageNotFound(Bundle extras) {
        setContentView(R.layout.activity_launcher_page_not_found_layout);
    }

    protected void configNotch(boolean extend) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    extend
                            ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                            : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            getWindow().setAttributes(lp);
        }
    }

    private String getLastPackageName() {
        int launcherId = LauncherManager.getCurrentLauncherId(this);
        if (launcherId >= 0) {
            Launcher.LauncherInfo info = Launcher.getLauncherInfo(this, launcherId);
            if (info != null) {
                return info.pkg;
            }
        }
        return null;
    }

    private void reloadExtras(Bundle extras) {
        super.load(extras);
    }

    protected void cancelDialogs() {
        if (mShortcutDialog != null && mShortcutDialog.isAdded()) {
            mShortcutDialog.dismissAllowingStateLoss();
            mShortcutDialog = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mHasBroadcast = false;
        mNewIntent = true;
        SystemController.getInstance().config(this, intent);
        super.onNewIntent(intent);
        setAppTaskDescription(getPackage());
        setupDirectBack();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && mAnimationDrawable != null) {
            mAnimationDrawable.start();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        SystemController.getInstance().onUserLeaveHint(this);
        setupDirectBack();
    }

    @Override
    public void onBackPressed() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        String pkg = getPackage();
        PlatformLogManager.getDefault().logBackPressed(pkg, mLoading);
        if (remindShortcut(pkg, ON_APP_EXIT)) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        onActivityFinish();
    }

    protected void onActivityFinish() {
        if (ActivityUtils.shouldOverrideExitAnimation(this)) {
            overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
        }
    }

    @Override
    protected void load(Bundle extras) {
        if (extras == null) {
            String lastPkg = getLastPackageName();
            if (!TextUtils.isEmpty(lastPkg)) {
                extras = new Bundle();
                extras.putString(EXTRA_APP, lastPkg);
                Log.i(TAG, "no extra. load last app on this process: " + lastPkg);
            } else {
                Log.e(TAG, "no extra and last app on this process not found");
            }
        }

        if (extras == null) {
            onPageNotFound(extras);
        } else {
            try {
                super.load(extras);
            } catch (Exception e) {
                Log.e(TAG, "load fail!", e);
                onPageNotFound(extras);
                final Bundle finalExtras = extras;
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                getContentView()
                                        .removeView(
                                                getContentView().findViewById(
                                                        R.id.activity_launcher_page_not_found));
                                finalExtras.putString(EXTRA_PATH, null);
                                reloadExtras(finalExtras);
                            }
                        },
                        RELOAD_EXTRA_DELAY_TIME);
                String pkg = extras.getString(EXTRA_APP);
                Source source = Source.fromJson(extras.getString(EXTRA_SOURCE));
                PlatformLogManager.getDefault().logAppError(pkg, TAG, e, source);
            }
        }
    }

    @Override
    protected void load(HybridRequest.HapRequest request) {
        String pkg = request.getPackage();
        PlatformLogManager.getDefault().logAppPreLoad(pkg);
        broadcastRunningApp(pkg);

        mLoading = false;
        mHandler.removeMessages(MESSAGE_INSTALL_TIMEOUT);
        mHandler.removeMessages(MESSAGE_UPDATE_TIMEOUT);
        cancelDialogs();

        if (mAppStatus == DistributionManager.APP_STATUS_READY) {
            showSplashView(pkg);
            launchApp(request);
            clearLoadingView();
        } else {
            // 从最近任务启动时，如果是APP_STATUS_NONE，直接发起安装
            if (mIsFromRecentsTask && mAppStatus == DistributionManager.APP_STATUS_NONE) {
                install(getHybridRequest().getPagePath());
                mIsFromRecentsTask = false;
            }
            // 本地缺少部分分包的情况下, 更新时不允许超时回退到老版本
            if (mAppStatus == DistributionManager.APP_STATUS_UPDATE
                    && DistributionManager.getInstance().isPackageComplete(pkg)) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_TIMEOUT, UPDATE_TIMEOUT);
            } else {
                mHandler.sendEmptyMessageDelayed(MESSAGE_INSTALL_TIMEOUT, getInstallTimeout());
            }
            mLoading = true;
            showLoadingView(request);
            registerInstallListener(pkg);
        }
    }

    @Override
    protected HybridRequest.HapRequest parseRequest(Bundle extras) {
        String pkg = null;
        HybridRequest.HapRequest request = super.parseRequest(extras);
        if (request != null) {
            pkg = request.getPackage();
        }
        mAppStatus = getAppStatus(extras, pkg);

        String oldPkg = getPackage();
        if (oldPkg != null && !oldPkg.equals(pkg)) {
            unregisterInstallListener(oldPkg);
            DistributionManager.getInstance().cancelInstall(oldPkg);
        }
        return request;
    }

    protected int getAppStatus(Bundle extras, String pkg) {
        if (!TextUtils.isEmpty(pkg)) {
            return DistributionManager.getInstance().getAppStatus(pkg);
        }
        return DistributionManager.APP_STATUS_NONE;
    }

    private void activeApp(String pkg) {
        if (pkg != null) {
            Executors.io()
                    .execute(
                            new AbsTask<Boolean>() {
                                @Override
                                protected Boolean doInBackground() {
                                    RuntimeLogManager.getDefault()
                                            .logAsyncThreadTaskStart(pkg,
                                                    "LauncherActivity#activeApp");
                                    return LauncherManager.active(getApplicationContext(), pkg);
                                }

                                @Override
                                protected void onPostExecute(Boolean success) {
                                    RuntimeLogManager.getDefault()
                                            .logAsyncThreadTaskEnd(pkg,
                                                    "LauncherActivity#activeApp");
                                    if (!success
                                            && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                                            && getIntent() != null
                                            && (getIntent().getFlags()
                                            & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                                            == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
                                        Log.e(TAG, "Finish activity for active failure");
                                        Toast.makeText(getApplicationContext(), "active failure",
                                                Toast.LENGTH_SHORT)
                                                .show();
                                        finish();
                                    }
                                }
                            });
        }
    }

    private void inactiveApp() {
        String pkg = getPackage();
        if (pkg != null) {
            LauncherManager.inactiveAsync(getApplicationContext(), pkg);
        }
    }

    private void broadcastRunningApp(String pkg) {
        if (!mHasBroadcast) {
            LocalBroadcastHelper.getInstance().broadcastRunningApp(pkg, getClass());
            mHasBroadcast = true;
        }
    }

    protected void showCreateShortcutDialog(
            String pkg, String source, int msgType, boolean autoExit) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (mLoading || !TextUtils.equals(pkg, getRunningPackage())) {
            return;
        }
        CacheStorage cacheStorage = CacheStorage.getInstance(this);
        if (!cacheStorage.hasCache(pkg)) {
            return;
        }
        try {
            if (!cacheStorage.getCache(pkg).hasIcon()) {
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "create shortcut dialog failed!", e);
            Source originSource = Source.fromJson(source);
            PlatformLogManager.getDefault().logAppError(pkg, TAG, e, originSource);
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        if (mShortcutDialog != null
                && mShortcutDialog.getDialog() != null
                && mShortcutDialog.getDialog().isShowing()) {
            return;
        }
        mShortcutDialog = onCreateShortcutDialog();
        mShortcutDialog.setAutoExit(autoExit);
        mShortcutDialog.setMessage(msgType);
        mShortcutDialog.setSource(source);
        mShortcutDialog.show(fm, FRAGMENT_TAG_DIALOG);
        PlatformLogManager.getDefault().logShortcutPromptShow(pkg, source);
    }

    protected ShortcutDialogFragment onCreateShortcutDialog() {
        return new ShortcutDialogFragmentImpl();
    }

    protected void showLoadingView(HybridRequest request) {
        setContentView(R.layout.loading);
        mLoadingMsg = (TextView) findViewById(R.id.loadingMsg);
        mAnimationDrawable = getAnimationDrawable();
        mAnimationDrawable.setBounds(
                0, 0, mAnimationDrawable.getIntrinsicWidth(),
                mAnimationDrawable.getIntrinsicHeight());
        mAnimationDrawable.setCallback(mLoadingMsg);
        mLoadingMsg.setCompoundDrawables(null, null, null, mAnimationDrawable);
        mAnimationDrawable.start();
    }

    private RoundedLineAnimationDrawable getAnimationDrawable() {
        Resources res = getResources();
        int lineWidth = res.getDimensionPixelSize(R.dimen.anim_loading_line_width);
        int lineHeight = res.getDimensionPixelSize(R.dimen.anim_loading_line_height);
        int lineSpace = res.getDimensionPixelSize(R.dimen.anim_loading_line_space);
        int lineColor = res.getColor(R.color.anim_loading_line_color);
        int duration = res.getInteger(R.integer.anim_loading_duration);
        return new RoundedLineAnimationDrawable(lineWidth, lineHeight, lineSpace, lineColor,
                duration);
    }

    protected void updateLoadingView(PreviewInfo previewInfo) {
        if (!mLoading) {
            return;
        }
        if (mAppName == null) {
            ViewStub appNameStub = (ViewStub) findViewById(R.id.appNameStub);
            if (appNameStub == null) {
                Log.e(TAG, "appNameStub is null");
                return;
            }
            mAppName = (TextView) appNameStub.inflate();
        }
        final String pkg = previewInfo.getId();
        final String name = previewInfo.getName();
        if (!TextUtils.isEmpty(name)) {
            mAppName.setText(name);
        }
        final String iconUrl = previewInfo.getIconUrl();
        if (!TextUtils.isEmpty(iconUrl)) {
            IconUtils.getIconDrawableAsync(
                    this,
                    Uri.parse(iconUrl),
                    new IconUtils.OnDrawableCallback() {
                        @Override
                        public void onResult(final Drawable drawable) {
                            mHandler.post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!mLoading || !TextUtils.equals(pkg, getPackage())) {
                                                return;
                                            }
                                            if (drawable != null && mAppName != null) {
                                                drawable.setBounds(
                                                        0, 0, drawable.getIntrinsicWidth(),
                                                        drawable.getIntrinsicHeight());
                                                mAppName.setCompoundDrawables(null, drawable, null,
                                                        null);
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.w(TAG, "getIconDrawableAsync", throwable);
                        }
                    });
        }
    }

    private void reloadHybridView(HybridRequest.HapRequest request) {
        clearLoadingView();
        launchApp(request);
    }

    protected boolean verifyPreviewInfo(PreviewInfo previewInfo) {
        Log.v(TAG, "PreviewInfo=" + previewInfo + ", pkg=" + getPackage());
        return previewInfo != null && TextUtils.equals(getPackage(), previewInfo.getId());
    }

    protected void clearLoadingView() {
        mAppName = null;
        mLoadingMsg = null;
        mAnimationDrawable = null;
    }

    protected void clearFailView() {
        View failView = findViewById(R.id.loading_failed_view);
        if (failView != null) {
            getContentView().removeView(failView);
        }
    }

    private void launchApp(HybridRequest.HapRequest request) {
        AppResourcesLoader.preload(getApplicationContext(), getHybridRequest());

        clearFailView();
        configNotch(true);
        String pkg = request.getPackage();
        boolean appChanged = !pkg.equals(getRunningPackage());
        if (appChanged) {
            notifyAppChanged(request);
            PreferenceUtils.addUseRecord(pkg);
            mLaunchAppTime = System.currentTimeMillis();
        }

        PlatformLogManager.getDefault().logAppLaunch(pkg, request.getPagePath());
        setAppTaskDescription(getPackage());

        onPackageReady(request);

        if (getHybridView() != null) {
            final View root = getHybridView().getWebView();
            if (root instanceof RootView) {
                Log.d(TAG, "register LoadPageJsListener");
                ((RootView) root)
                        .setLoadPageJsListener(
                                new Page.LoadPageJsListener() {
                                    @Override
                                    public void onLoadStart(final Page page) {
                                        runOnUiThread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        handleLoadPageJsStart(page);
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onLoadFinish(final Page page) {
                                        runOnUiThread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        handleLoadPageJsFinish(page);
                                                    }
                                                });
                                    }
                                });
            }
            getHybridView()
                    .getHybridManager()
                    .getResidentManager()
                    .setUpdateDbListener(new ResidentDbUpdatorImpl());
        }
    }

    protected void onPackageReady(HybridRequest.HapRequest request) {
        super.load(request);
    }

    private void notifyAppChanged(HybridRequest request) {
        activeApp(getPackage());
    }

    private void setupDirectBack() {
        if (getHybridView() != null) {
            View root = getHybridView().getWebView();
            if (root instanceof RootView) {
                boolean directBack = SystemController.getInstance().isDirectBack();
                ((RootView) root).setDirectBack(directBack);
            }
        }
    }

    private void setAppTaskDescription(final String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            Log.w(TAG, "setAppTaskDescription: pkg is null");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && !TextUtils.equals(pkg, mTaskDescriptionPackage)) {
            Executors.io()
                    .execute(
                            new AbsTask<ActivityManager.TaskDescription>() {
                                @Override
                                protected ActivityManager.TaskDescription doInBackground() {
                                    RuntimeLogManager.getDefault()
                                            .logAsyncThreadTaskStart(pkg,
                                                    "LauncherActivity#setAppTaskDescription");
                                    Cache cache = CacheStorage.getInstance(LauncherActivity.this)
                                            .getCache(pkg);
                                    AppInfo appInfo = cache.getAppInfo();
                                    if (appInfo == null) {
                                        return new ActivityManager.TaskDescription();
                                    }

                                    Bitmap bitmap =
                                            IconUtils.getRoundIconBitmap(LauncherActivity.this,
                                                    cache.getIconUri());
                                    ActivityManager.TaskDescription td =
                                            new ActivityManager.TaskDescription(appInfo.getName(),
                                                    bitmap);
                                    return td;
                                }

                                @Override
                                protected void onPostExecute(
                                        ActivityManager.TaskDescription taskDescription) {
                                    RuntimeLogManager.getDefault()
                                            .logAsyncThreadTaskEnd(pkg,
                                                    "LauncherActivity#setAppTaskDescription");
                                    if (isFinishing() || isDestroyed()
                                            || !TextUtils.equals(pkg, getPackage())) {
                                        return;
                                    }
                                    String targetId =
                                            TextUtils.isEmpty(taskDescription.getLabel()) ? null :
                                                    pkg;
                                    if (!TextUtils.equals(pkg, mTaskDescriptionPackage)) {
                                        mTaskDescriptionPackage = targetId;
                                        setTaskDescription(taskDescription);
                                    }
                                }
                            });
        }
    }

    private void handleInstallResult(String pkg, int statusCode, int errorCode) {
        Log.d(
                TAG,
                "handle install result: pkg="
                        + pkg
                        + ", statusCode="
                        + statusCode
                        + ", errorCode="
                        + errorCode);
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (!TextUtils.equals(pkg, getPackage())) {
            Log.d(TAG, "Package is different: EXTRA_APP=" + pkg + ", pkg=" + getPackage());
            return;
        }
        PlatformLogManager.getDefault().logAppLoadingResult(pkg, statusCode, errorCode);

        mCurrentStatusCode = statusCode;
        if (statusCode == DistributionManager.CODE_INSTALL_OK) {
            unregisterInstallListener(pkg);
            onPackageInstallSuccess(pkg);
        } else if (statusCode == DistributionManager.CODE_INSTALLING) {
            onPackageInstallStart(pkg);
        } else if (statusCode == DistributionManager.CODE_INSTALL_CANCEL) {
            unregisterInstallListener(pkg);
            onPackageInstallCancel(pkg, mAppStatus, errorCode);
        } else if (statusCode == DistributionManager.CODE_INSTALL_ERROR) {
            onPackageInstallFailed(pkg, statusCode, errorCode);
        } else if (statusCode == DistributionManager.CODE_APPLY_UPDATE_DELAYED) {
            onPackageUpdateDelayed(pkg, errorCode);
        } else if (statusCode == DistributionManager.CODE_INSTALL_STREAM) {
            onPackageInstallStream(pkg, errorCode);
        } else {
            showFailView(errorCode, mPreviewInfo);
        }
    }

    private void handleLoadPageJsStart(Page page) {
        HybridView hybridView = getHybridView();
        if (page == null || hybridView == null || !(hybridView.getWebView() instanceof RootView)) {
            return;
        }
        Log.d(
                TAG, "handleLoadPageJsStart page=" + page.getName() + ", result="
                        + page.getLoadJsResult());
        RootView rootView = (RootView) hybridView.getWebView();
        PageManager pm = rootView.getPageManager();
        if (pm != null && pm.getCurrPage() == page) {
            showPageJsLoadingProgress();
        }
    }

    private void handleLoadPageJsFinish(Page page) {
        HybridView hybridView = getHybridView();
        if (page == null || hybridView == null || !(hybridView.getWebView() instanceof RootView)) {
            return;
        }
        Log.d(
                TAG,
                "handleLoadPageJsFinish page=" + page.getName() + ", result="
                        + page.getLoadJsResult());
        RootView rootView = (RootView) hybridView.getWebView();
        PageManager pm = rootView.getPageManager();
        if (pm != null && pm.getCurrPage() == page) {
            hidePageJsLoadingProgress();
            if (page.getLoadJsResult() == Page.JS_LOAD_RESULT_FAIL) {
                if (mInstallErrorCache == CacheErrorCode.OK) {
                    Log.d(TAG, "page not found or install is still in progress.");
                } else {
                    Log.d(TAG, "showFailView mInstallErrorCache=" + mInstallErrorCache);
                    showFailView(mInstallErrorCache, mPreviewInfo);
                }
            }
        }
    }

    protected void showPageJsLoadingProgress() {
        HybridView hybridView = getHybridView();
        if (hybridView == null || !(hybridView.getWebView() instanceof RootView)) {
            return;
        }

        RootView rootView = (RootView) hybridView.getWebView();
        VDocument document = rootView.getDocument();
        if (document != null) {
            document.getComponent().showProgress();
        }
    }

    protected void hidePageJsLoadingProgress() {
        HybridView hybridView = getHybridView();
        if (hybridView == null || !(hybridView.getWebView() instanceof RootView)) {
            return;
        }

        RootView rootView = (RootView) hybridView.getWebView();
        VDocument document = rootView.getDocument();
        if (document != null) {
            document.getComponent().hideProgress();
        }
    }

    protected boolean remindShortcut(String pkg, int when) {
        if (mLoading || TextUtils.isEmpty(pkg) || !TextUtils.equals(pkg, getRunningPackage())) {
            return false;
        }

        if (!ShortcutUtils.shouldCreateShortcutByPlatform(this, pkg)) {
            return false;
        }

        mHandler.removeMessages(MESSAGE_REMIND_SHORTCUT);
        if (when == ON_APP_START) {
            // 启动时投放延时任务进行弹窗
            if (ShortcutUtils.isUseTimesReachRemind(pkg)) {
                Message msg =
                        mHandler.obtainMessage(MESSAGE_REMIND_SHORTCUT,
                                Source.DIALOG_SCENE_USE_TIMES);
                msg.arg1 = ShortcutDialogFragment.MSG_TYPE_COUNT;
                mHandler.sendMessageDelayed(msg, ShortcutUtils.REMIND_LAUNCH_DELAY);
            } else {
                Message msg =
                        mHandler.obtainMessage(MESSAGE_REMIND_SHORTCUT,
                                Source.DIALOG_SCENE_USE_DURATION);
                msg.arg1 = ShortcutDialogFragment.MSG_TYPE_TIMING;
                mHandler.sendMessageDelayed(msg, ShortcutUtils.REMIND_LEAST_USE_DURATION);
            }
        } else if (when == ON_APP_EXIT) {
            // 退出时立即弹窗
            showCreateShortcutDialog(
                    pkg, Source.DIALOG_SCENE_EXIT_APP, ShortcutDialogFragment.MSG_TYPE_EXIT, true);
        }
        return true;
    }

    protected void onPackageInstallSuccess(String packageName) {
        Log.d(TAG, "onPackageInstallSuccess:" + packageName + ", current:" + getPackage());
        if (mLoading) {
            finishLoading();
            reloadApp();
        }
    }

    protected void onPackageInstallStart(String packageName) {
        Log.d(TAG, "onPackageInstallStart:" + packageName + ", current:" + getPackage());
        mInstallErrorCache = CacheErrorCode.OK;
    }

    protected void onPackageInstallCancel(String packageName, int appStatus, int reason) {
        Log.d(
                TAG,
                "onPackageInstallCancel:"
                        + packageName
                        + ", current:"
                        + getPackage()
                        + ", reason "
                        + reason
                        + ", app status >>> "
                        + appStatus);
        if (appStatus == DistributionManager.APP_STATUS_NONE) {
            onPackageInstallFailed(packageName, DistributionManager.CODE_INSTALL_CANCEL, reason);
        } else {
            if (mLoading) {
                finishLoading();
                reloadApp();
            }
        }
    }

    protected void onPackageUpdateDelayed(String packageName, int reason) {
        Log.d(
                TAG,
                "onPackageUpdateDelayed:"
                        + packageName
                        + ", current:"
                        + getPackage()
                        + ", reason "
                        + reason);
        if (reason == DistributionManager.APP_STATUS_NONE) {
            installAndReload();
            return;
        }
        unregisterInstallListener(packageName);
        if (mLoading) {
            finishLoading();
            reloadApp();
        }
    }

    protected void onPackageInstallFailed(final String packageName, int statusCode, int errorCode) {
        Log.d(
                TAG,
                "onPackageInstallFailed:"
                        + packageName
                        + ", current:"
                        + getPackage()
                        + ", statusCode:"
                        + statusCode
                        + ", errorCode:"
                        + errorCode);
        boolean loading = mLoading;
        finishLoading();
        clearLoadingView();

        // 未展示应用内容前, 如果应用的低版本已经完整安装, 并且安装错误不是不兼容或签名变更,
        // 直接展示旧版本, 不显示错误信息.
        if (loading
                && DistributionManager.getInstance().isPackageComplete(packageName)
                && errorCode != CacheErrorCode.PACKAGE_INCOMPATIBLE
                && errorCode != CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED) {
            reloadApp();
        } else {
            showFailView(errorCode, mPreviewInfo);
        }
    }

    /**
     * 收到流式安装通知后，安装并未结束，后面会收到安装成功或失败的通知，所以listener不能被取消
     */
    protected void onPackageInstallStream(final String packageName, int errorCode) {
        Log.d(TAG, "onPackageInstallStream: " + packageName + ", current:" + getPackage());
        if (mLoading) {
            finishLoading();
            reloadApp();
        } else if (errorCode == CacheErrorCode.PACKAGE_CACHE_OBSOLETE) {
            // 新版本应用安装，清除已加载旧版本页面重新加载
            if (!mLoadNewVersion) {
                // 避免重复触发load
                mLoadNewVersion = true;
                removeHybridView();
                reloadApp();
            }
        }
    }

    private void finishLoading() {
        mHandler.removeMessages(MESSAGE_INSTALL_TIMEOUT);
        mHandler.removeMessages(MESSAGE_UPDATE_TIMEOUT);
        mLoading = false;
    }

    private void registerInstallListener(String packageName) {
        mInstallErrorCache = CacheErrorCode.OK;

        if (mInstallStatusListener == null) {
            mInstallStatusListener =
                    new DistributionManager.InstallStatusListener() {
                        @Override
                        public void onInstallResult(String pkg, int statusCode, int errorCode) {
                            handleInstallResult(pkg, statusCode, errorCode);
                        }

                        @Override
                        public void onPreviewInfo(String pkg, PreviewInfo previewInfo) {
                            if (!verifyPreviewInfo(previewInfo)) {
                                return;
                            }
                            mPreviewInfo = previewInfo;
                            updateLoadingView(previewInfo);
                        }
                    };
        }
        DistributionManager.getInstance().addInstallStatusListener(mInstallStatusListener);
    }

    private void unregisterInstallListener(String packageName) {
        if (mInstallStatusListener == null) {
            return;
        }
        DistributionManager.getInstance().removeInstallStatusListener(mInstallStatusListener);
        mInstallStatusListener = null;
    }

    protected void reloadApp() {
        mAppStatus = DistributionManager.APP_STATUS_READY;
        reloadHybridView(getHybridRequest());
        remindShortcut(getPackage(), ON_APP_START);
    }

    protected boolean showFailView(int errorCode, PreviewInfo previewInfo) {
        configNotch(false);
        if (errorCode == CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED
                || errorCode == CacheErrorCode.PACKAGE_INCOMPATIBLE
                || errorCode == CacheErrorCode.PACKAGE_UNAVAILABLE) {
            removeHybridView();
            return showNonRetriableFailView(errorCode, previewInfo);
        } else {
            boolean result = false;
            if (shouldShowRetriableFailedView(errorCode)) {
                result = showRetriableFailedView(errorCode);
            }
            return result;
        }
    }

    protected boolean showNonRetriableFailView(int errorCode, PreviewInfo previewInfo) {
        FrescoUtils.initialize(this.getApplicationContext());
        setContentView(R.layout.loading_nonretriable_fail);
        TextView messageView = (TextView) findViewById(R.id.error_message);
        SimpleDraweeView appIconView = (SimpleDraweeView) findViewById(R.id.error_app_icon);
        TextView appNameView = (TextView) findViewById(R.id.error_app_name);

        switch (errorCode) {
            case CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED:
                appIconView.getHierarchy().setPlaceholderImage(getApplicationInfo().icon);
                if (verifyPreviewInfo(previewInfo)) {
                    appNameView.setText(previewInfo.getName());
                    setActualImage(appIconView, previewInfo.getIconUrl(), null, R.drawable.flag);
                }
                messageView.setText(R.string.loading_fail_message_package_certificate_changed);
                Button continueBtn = (Button) findViewById(R.id.error_btn_top);
                continueBtn.setVisibility(View.VISIBLE);
                continueBtn.setText(R.string.loading_fail_btn_continue);
                continueBtn.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                reloadApp();
                            }
                        });
                Button reinstallBtn = (Button) findViewById(R.id.error_btn_bottom);
                reinstallBtn.setVisibility(View.VISIBLE);
                reinstallBtn.setText(R.string.loading_fail_btn_reinstall);
                reinstallBtn.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CacheStorage.getInstance(getApplicationContext())
                                        .uninstall(getPackage());
                                installAndReload();
                            }
                        });
                break;
            case CacheErrorCode.PACKAGE_INCOMPATIBLE:
                appIconView.getHierarchy().setPlaceholderImage(getApplicationInfo().icon);
                appNameView.setText(getApplicationInfo().loadLabel(getPackageManager()));
                messageView.setText(R.string.loading_fail_message_incompatible_platform);
                break;
            case CacheErrorCode.PACKAGE_UNAVAILABLE:
                appIconView.getHierarchy().setPlaceholderImage(getApplicationInfo().icon);
                if (verifyPreviewInfo(previewInfo)) {
                    appNameView.setText(previewInfo.getName());
                    ColorFilter colorFilter =
                            new PorterDuffColorFilter(
                                    getResources().getColor(R.color.app_icon_disable_filter),
                                    PorterDuff.Mode.MULTIPLY);
                    setActualImage(
                            appIconView, previewInfo.getIconUrl(), colorFilter,
                            R.drawable.ic_disabled);
                }
                messageView.setText(R.string.loading_fail_message_package_unavailable);
                break;
            default:
                throw new IllegalArgumentException("not support errorCode: " + errorCode);
        }

        return true;
    }

    protected boolean showRetriableFailedView(int errorCode) {

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        if (decorView.findViewById(R.id.loading_failed_view) != null) {
            Log.d(TAG, "failedView is showing");
            return false;
        }
        View failedView =
                getLayoutInflater().inflate(R.layout.loading_retriable_fail, decorView, false);
        failedView.setBackgroundColor(getResources().getColor(android.R.color.white));
        decorView.addView(failedView, -1, -1);

        Button back = (Button) failedView.findViewById(R.id.back_btn);
        back.setOnClickListener(mBackClickListener);
        Button refresh = (Button) failedView.findViewById(R.id.refresh_btn);
        refresh.setOnClickListener(mInstallAndReloadClickListener);

        TextView messageView = failedView.findViewById(R.id.error_message);
        int msgTextId =
                errorCode == CacheErrorCode.NETWORK_UNAVAILABLE
                        ? R.string.loading_fail_message_no_network
                        : R.string.loading_fail_message_install_error;
        messageView.setText(msgTextId);
        int iconId =
                errorCode == CacheErrorCode.NETWORK_UNAVAILABLE
                        ? R.drawable.ic_no_network
                        : R.drawable.ic_load_fail;
        messageView.setCompoundDrawablesWithIntrinsicBounds(
                null, getResources().getDrawable(iconId), null, null);

        return true;
    }

    protected boolean shouldShowRetriableFailedView(int errorCode) {
        HybridView hybridView = getHybridView();
        if (hybridView != null && hybridView.getWebView() instanceof RootView) {
            RootView rootView = (RootView) hybridView.getWebView();
            PageManager pm = rootView.getPageManager();
            if (pm != null && pm.getCurrPage() != null) {
                int loadResult = pm.getCurrPage().getLoadJsResult();
                if (loadResult == Page.JS_LOAD_RESULT_SUCC) {
                    Log.d(TAG, "page has been created. skip showing failed view.");
                    return false;
                } else if (loadResult == Page.JS_LOAD_RESULT_NONE) {
                    Log.d(TAG,
                            "page is still in loading progress. will check again when loading finished.");
                    mInstallErrorCache = errorCode;
                    return false;
                }
            }
        }
        return true;
    }

    protected void setActualImage(
            final GenericDraweeView draweeView,
            String actualImageUri,
            ColorFilter colorFilter,
            final int overlayImage) {
        BaseControllerListener<ImageInfo> listener =
                new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(
                            String id, @Nullable ImageInfo imageInfo,
                            @Nullable Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        draweeView.getHierarchy()
                                .setOverlayImage(getResources().getDrawable(overlayImage));
                    }
                };
        draweeView.setController(
                Fresco.newDraweeControllerBuilder()
                        .setUri(actualImageUri)
                        .setOldController(draweeView.getController())
                        .setControllerListener(listener)
                        .build());
        draweeView.getHierarchy().setActualImageColorFilter(colorFilter);
    }

    private void install(String curPath) {
        // 重新安装时重置状态
        mLoadNewVersion = false;
        Log.d(TAG, "install curPath=" + curPath);
        String pkg = getPackage();
        Source source = Source.currentSource();
        DistributionManager.getInstance().scheduleInstall(pkg, curPath, source);
    }

    protected void installAndReload() {
        mAppStatus =
                getAppStatus(getIntent() != null ? getIntent().getExtras() : null, getPackage());
        install(getHybridRequest().getPagePath());
        load(getHybridRequest());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFirstRenderActionEvent(FirstRenderActionEvent event) {
        dismissSplashView(false);
    }

    protected int getMinSplashDisplayDuration() {
        return MIN_SPLASH_DISPLAY_DURATION;
    }

    protected int getSplashDismissDuration() {
        return SPLASH_DISMISS_DURATION;
    }

    protected int getSplashTimeout() {
        return SPLASH_TIME_OUT;
    }

    protected boolean needShowSplash() {
        if (mNewIntent) {
            return false;
        }

        // we don't want splash to show unless the app is from shortcut
        Source source = Source.currentSource();
        return source != null && TextUtils.equals(source.getType(), Source.TYPE_SHORTCUT);
    }

    protected void showSplashView(String packageName) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!needShowSplash()) {
            return;
        }

        if (mSplashRoot == null) {
            ViewGroup splash =
                    (ViewGroup) LayoutInflater.from(LauncherActivity.this)
                            .inflate(R.layout.splash, null);
            getContentView().addView(splash);
            mSplashRoot = findViewById(R.id.splash_root);
        }
        mSplashRoot.setVisibility(View.VISIBLE);
        Cache cache = CacheStorage.getInstance(this).getCache(packageName);
        AppInfo appInfo = cache.getAppInfo();
        Uri iconUri = cache.getIconUri();
        TextView appName = ((TextView) findViewById(R.id.app_name));
        ImageView appLogo = ((ImageView) findViewById(R.id.app_logo));
        appName.setText(appInfo.getName());
        appLogo.setImageBitmap(IconUtils.getCircleIconNoFlagBitmap(this, iconUri));
        mSplashStartTs = System.currentTimeMillis();
        mSplashRoot.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        dismissSplashView(true);
                    }
                },
                getSplashTimeout());
    }

    protected void dismissSplashView(boolean immediately) {
        if (mSplashRoot == null || mSplashRoot.getVisibility() == View.GONE) {
            Log.i(TAG, "splash view is already dismissed");
            return;
        }
        if (immediately) {
            mSplashRoot.setVisibility(View.GONE);
            return;
        }

        long elapsedTime = System.currentTimeMillis() - mSplashStartTs;
        // at least display splash for a while
        long delay =
                elapsedTime >= getMinSplashDisplayDuration()
                        ? 0
                        : getMinSplashDisplayDuration() - elapsedTime;
        mSplashRoot.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        hideSplashView();
                    }
                },
                delay);
        String pkg = getPackage();
        PlatformLogManager.getDefault().logAppShowSplash(pkg, elapsedTime);
        Log.d(TAG, "Splash of " + pkg + " showed for " + elapsedTime + " ms");
    }

    protected void hideSplashView() {
        if (mSplashRoot == null) {
            Log.i(TAG, "hideSplashView: splash view is null");
            return;
        }
        mSplashRoot
                .animate()
                .alpha(0.0f)
                .setDuration(getSplashDismissDuration())
                .setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mSplashRoot.setAlpha(1);
                                mSplashRoot.setVisibility(View.GONE);
                            }
                        })
                .start();
    }

    private void onUpdateTimeout(final String pkg) {
        DistributionManager.getInstance().delayApplyUpdate(pkg);
    }

    protected int getInstallTimeout() {
        return INSTALL_TIMEOUT;
    }

    private void setDefaultThemeMode() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String packages = "unknown";
        if (extras != null) {
            packages = extras.getString(EXTRA_APP);
        }
        if (TextUtils.isEmpty(packages) || TextUtils.equals(packages, "unknown")) {
            return;
        }
        GrayModeManager.getInstance().setCurrentPkg(packages);
        int theme = intent.getIntExtra(EXTRA_THEME_MODE, -1);
        if (DarkThemeUtil.needChangeDefaultNightMode(theme)) {
            AppCompatDelegate.setDefaultNightMode(DarkThemeUtil.convertThemeMode(theme));
        }
    }

    private static class CustomHandler extends Handler {
        private WeakReference<LauncherActivity> launcherActivityWeakReference;

        CustomHandler(LauncherActivity activity) {
            launcherActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LauncherActivity activity = launcherActivityWeakReference.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            if (msg.what == MESSAGE_REMIND_SHORTCUT) {
                String pkg = activity.getPackage();
                String source = (String) msg.obj;
                if (ShortcutUtils.shouldCreateShortcutByPlatform(activity, pkg)) {
                    activity.showCreateShortcutDialog(pkg, source, msg.arg1, false);
                }
            } else if (msg.what == MESSAGE_INSTALL_TIMEOUT) {
                String pkg = activity.getPackage();
                Log.w(TAG, "Install app timeout: " + pkg);
                activity.onPackageInstallFailed(pkg, DistributionManager.CODE_INSTALL_TIMEOUT, 0);
            } else if (msg.what == MESSAGE_UPDATE_TIMEOUT) {
                String pkg = activity.getPackage();
                activity.onUpdateTimeout(pkg);
            }
        }
    }

    public abstract static class ShortcutDialogFragment<T extends Dialog & Checkable>
            extends DialogFragment implements DialogInterface.OnClickListener {
        public static final int MSG_TYPE_COUNT = 0;
        public static final int MSG_TYPE_TIMING = 1;
        public static final int MSG_TYPE_EXIT = 2;

        protected T dialog;
        protected int msgType = MSG_TYPE_EXIT;
        protected String source;
        private boolean autoExit;

        public void setMessage(int msgType) {
            this.msgType = msgType;
        }

        public void setAutoExit(boolean autoExit) {
            this.autoExit = autoExit;
        }

        public void setSource(String source) {
            this.source = source;
        }

        @Override
        public final Dialog onCreateDialog(Bundle savedInstanceState) {
            LauncherActivity activity = (LauncherActivity) getActivity();
            String pkg = activity.getPackage();
            Cache cache = CacheStorage.getInstance(getActivity()).getCache(pkg);
            dialog = createShortcutDialog(pkg, cache);
            return dialog;
        }

        protected abstract T createShortcutDialog(String pkgName, Cache cache);

        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            LauncherActivity activity = (LauncherActivity) getActivity();
            String pkg = activity.getPackage();

            if (which == DialogInterface.BUTTON_POSITIVE) {
                PlatformLogManager.getDefault().logShortcutPromptAccept(pkg, source);
                if (!ShortcutUtils.hasShortcutInstalled(activity, pkg)) {
                    Source sourceClass = new Source();
                    sourceClass.putExtra(Source.EXTRA_SCENE, Source.SHORTCUT_SCENE_DIALOG);
                    sourceClass.putInternal(Source.INTERNAL_SUB_SCENE, source);
                    ShortcutUtils.installShortcut(activity, pkg, sourceClass);
                } else {
                    Log.w(TAG, "Shortcut already existed, pkg:" + pkg);
                }
            } else {
                boolean forbidden = dialog.isChecked();
                PlatformLogManager.getDefault().logShortcutPromptReject(pkg, forbidden, source);
                if (forbidden) {
                    PreferenceUtils.setShortcutForbiddenTime(pkg, System.currentTimeMillis());
                }
                if (!autoExit) {
                    PreferenceUtils.setShortcutRefusedTimeByCount(pkg, System.currentTimeMillis());
                }
            }
            if (autoExit) {
                activity.finish();
            }
        }

        @Override
        public void show(FragmentManager manager, String tag) {
            if (!manager.isStateSaved()) {
                if (getDialog() != null) {
                    DarkThemeUtil.disableForceDark(getDialog());
                }
                super.show(manager, tag);
            }
        }
    }

    public static class ShortcutDialogFragmentImpl
            extends ShortcutDialogFragment<CheckableAlertDialog> {
        @NonNull
        @Override
        protected CheckableAlertDialog createShortcutDialog(String pkgName, Cache cache) {
            CheckableAlertDialog dialog = new CheckableAlertDialog(getActivity());

            AppInfo appInfo = cache.getAppInfo();
            String appName = appInfo == null ? "" : appInfo.getName(); // fall back to empty str
            String title = getString(R.string.dlg_shortcut_title);
            String message;
            switch (msgType) {
                case MSG_TYPE_COUNT:
                    message = getString(R.string.dlg_shortcut_message_on_count, appName);
                    break;
                case MSG_TYPE_TIMING:
                    message = getString(R.string.dlg_shortcut_message_on_timing, appName);
                    break;
                case MSG_TYPE_EXIT:
                    message = getString(R.string.dlg_shortcut_message_on_exit, appName);
                    break;
                default:
                    message = getString(R.string.dlg_shortcut_message_on_exit, appName);
                    break;
            }

            dialog.setTitle(title);
            dialog.setMessage(message);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dlg_shortcut_ok),
                    this);
            dialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE, getString(R.string.dlg_shortcut_cancel), this);
            dialog.setCheckBox(false, getString(R.string.dlg_shortcut_silent));
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            setCancelable(false);
            return dialog;
        }
    }

    /**
     * This class would run in main process
     */
    protected static class Client implements LauncherManager.LauncherClient {
        private static Client IMPL = new Client(Runtime.getInstance().getContext());

        private Context applicationContext;

        public Client(Context applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public boolean respond(Intent intent) {
            return IntentUtils.getLaunchAction(applicationContext).equals(intent.getAction());
        }

        @Override
        public boolean needLauncherId() {
            return true;
        }

        @Override
        public String getPackage(Intent intent) {
            return intent.getStringExtra(RuntimeActivity.EXTRA_APP);
        }

        @Override
        public String getClassName(int launcherId) {
            return "org.hapjs.LauncherActivity$Launcher" + launcherId;
        }

        @Override
        public void launch(Context context, Intent intent) {
            Bundle options;
            if (context instanceof Activity) {
                String launchPackage = ActivityUtils.getCallingPackage((Activity) context);
                // use calling package name as default source
                if (TextUtils.isEmpty(intent.getStringExtra(EXTRA_SOURCE))) {
                    Source source = new Source();
                    source.setPackageName(launchPackage);
                    intent.putExtra(EXTRA_SOURCE, source.toJson().toString());
                }
                options = null;
                if (intent.getBooleanExtra(RuntimeActivity.EXTRA_ENABLE_DEBUG, false)
                        && !TextUtils.equals(launchPackage, context.getPackageName())) {
                    Log.e(TAG, launchPackage + " has no permission to access debug mode!");
                    return;
                }
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                options =
                        ActivityOptionsCompat.makeCustomAnimation(
                                context, R.anim.activity_open_enter, R.anim.activity_open_exit)
                                .toBundle();
            }

            String pkg = intent.getStringExtra(RuntimeActivity.EXTRA_APP);
            String path = intent.getStringExtra(RuntimeActivity.EXTRA_PATH);
            Source source = Source.fromJson(intent.getStringExtra(RuntimeActivity.EXTRA_SOURCE));
            SystemController.getInstance().config(context, intent);

            Cache cache = CacheStorage.getInstance(context).getCache(pkg);
            if (cache.ready()) {
                AppInfo appInfo = cache.getAppInfo(); // load manifest.json
                if (appInfo != null && appInfo.getDisplayInfo() != null) {
                    intent.putExtra(EXTRA_THEME_MODE, appInfo.getDisplayInfo().getThemeMode());
                }
            }

            DistributionManager distributionManager = DistributionManager.getInstance();
            int status = distributionManager.getAppStatus(pkg);
            if (status != DistributionManager.APP_STATUS_READY) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                distributionManager.scheduleInstall(pkg, path, source);
            }
            intent.putExtra(EXTRA_SESSION, LogHelper.getSession(pkg));
            intent.putExtra(EXTRA_SESSION_EXPIRE_TIME,
                    System.currentTimeMillis() + SESSION_EXPIRE_SPAN);
            PlatformLogManager.getDefault().logAppPreLaunch(pkg, path, status, source);
            context.startActivity(intent, options);
        }
    }

    public static class Launcher0 extends LauncherActivity {
    }

    public static class Launcher1 extends LauncherActivity {
    }

    public static class Launcher2 extends LauncherActivity {
    }

    public static class Launcher3 extends LauncherActivity {
    }

    public static class Launcher4 extends LauncherActivity {
    }
}
