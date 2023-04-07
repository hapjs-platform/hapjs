/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.impl.android.HybridViewImpl;
import org.hapjs.card.api.CardConfig;
import org.hapjs.card.api.IRenderListener;
import org.hapjs.card.api.InstallListener;
import org.hapjs.card.sdk.utils.CardConfigUtils;
import org.hapjs.card.support.utils.CardRuntimeErrorManager;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.SimpleActivityLifecycleCallbacks;
import org.hapjs.io.AssetSource;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.io.JavascriptReader;
import org.hapjs.model.AppInfo;
import org.hapjs.render.IHybridViewHolder;
import org.hapjs.render.MainThreadFrameWorker;
import org.hapjs.render.Page;
import org.hapjs.render.RenderAction;
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.Runtime;
import org.json.JSONException;
import org.json.JSONObject;

public class CardView extends RootView
        implements IHybridViewHolder, CardConfigUtils.CardConfigChangeListener {
    private static final String TAG = "CardView";
    protected Context mThemeContext;
    private HybridView mHybridView;
    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks;
    private Application mAppContext;
    private boolean mIsStopped;
    private boolean mAutoDestroy = true;
    private View mLoadingView;
    private View mFailView;
    private HapEngine.Mode mMode;
    private boolean mChangeVisibilityManually;
    private IRenderListener mRenderListener;
    private DestroyListener mDestroyListener;
    private boolean mResumed;
    private ComponentCallbacks2 mComponentCallbacks2 =
            new ComponentCallbacks2() {
                @Override
                public void onTrimMemory(int level) {
                }

                @Override
                public void onLowMemory() {
                }

                @Override
                public void onConfigurationChanged(@NonNull Configuration newConfig) {
                    if (mHybridView != null && !mIsDestroyed) {
                        mHybridView.getHybridManager().onConfigurationChanged(newConfig);
                    }
                }
            };
    private MainThreadFrameWorker mApplyActionWorker =
            new MainThreadFrameWorker(this) {

                @Override
                public boolean doMiniTask() {
                    RenderAction action = pollRenderAction();
                    if (action == null) {
                        return false;
                    }

                    applyAction(getDocument(), action);
                    return true;
                }
            };

    public CardView(Context context) {
        super(context);
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* Insert a theme field to prevent an error when using theme context */
    @Override
    protected Context getThemeContext() {
        if (mThemeContext == null) {
            mThemeContext = new DummyActivity((Activity) getContext());
        }
        return mThemeContext;
    }

    /* inject a theme and LayoutInflater to context */
    public static Context getThemeContext(final Context context, int resId) {
        return new ContextThemeWrapper(context, resId) {
            private LayoutInflater inflater;

            @Override
            public ClassLoader getClassLoader() {
                return RootView.class.getClassLoader();
            }

            @Override
            public Object getSystemService(String name) {
                if (LAYOUT_INFLATER_SERVICE.equals(name)) {
                    if (inflater == null) {
                        inflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
                    }
                    return inflater;
                }
                return getBaseContext().getSystemService(name);
            }
        };
    }

    @Override
    protected boolean hasAppResourcesPreloaded(String pkg) {
        return false;
    }

    @Override
    protected String getAppJs() {
        if (mMode == HapEngine.Mode.CARD) {
            return JavascriptReader.get().read(new AssetSource(getContext(), "app/card.js"));
        } else {
            return super.getAppJs();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAppContext.registerComponentCallbacks(mComponentCallbacks2);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAppContext.unregisterComponentCallbacks(mComponentCallbacks2);
        super.release();
    }

    public void initialize(Activity activity, HapEngine.Mode mode,
                           boolean changeVisibilityManually) {
        mMode = mode;
        mChangeVisibilityManually = changeVisibilityManually;
        mAppContext = ((Application) Runtime.getInstance().getContext());
        mHybridView = new HybridViewImpl(this);
        mHybridView.getHybridManager().changeVisibilityManually(changeVisibilityManually);
        registerLifecycleCallback();
        mHybridView
                .getHybridManager()
                .addLifecycleListener(
                        new LifecycleListener() {
                            @Override
                            public void onStart() {
                                mIsStopped = false;
                                onActivityStart();
                                CardConfigUtils.registerCardConfigChangeListener(CardView.this);
                            }

                            @Override
                            public void onResume() {
                                onActivityResume();
                            }

                            @Override
                            public void onPause() {
                                onActivityPause();
                            }

                            @Override
                            public void onStop() {
                                CardConfigUtils.unRegisterCardConfigChangeListener();
                                mIsStopped = true;
                                onActivityStop();
                            }

                            @Override
                            public void onDestroy() {
                                onActivityDestroy();
                            }
                        });
    }

    public void loadUrl(final String url, String cardData) {
        if (mHybridView == null) {
            handleRenderFail(
                    IRenderListener.ErrorCode.ERROR_INITIAL,
                    "Please call initialize(context) before load(string) !");
            return;
        } else if (TextUtils.isEmpty(url)) {
            handleRenderFail(IRenderListener.ErrorCode.ERROR_URL, "Please check url");
            return;
        }

        mUrl = url;
        HybridRequest hybridRequest = null;
        try {
            hybridRequest = new HybridRequest.Builder().uri(url).build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to parse the card url, url = " + url, e);
            handleRenderFail(IRenderListener.ErrorCode.ERROR_URL, "Failed to parse the card url");
            return;
        }

        if (!(hybridRequest instanceof HybridRequest.HapRequest)) {
            Log.e(TAG, "Not the correct type of card url, url = " + url);
            handleRenderFail(IRenderListener.ErrorCode.ERROR_URL,
                    "Not the correct type of card url");
            return;
        }
        String cardPath = ((HybridRequest.HapRequest) hybridRequest).getPagePath();
        if (TextUtils.isEmpty(cardPath)) {
            Log.e(TAG, "Cannot find card path. load failed, url = " + url);
            handleRenderFail(IRenderListener.ErrorCode.ERROR_URL, "Cannot find card path");
            return;
        }

        String downloadUrl = null;
        int versionCode = -1;
        if (!TextUtils.isEmpty(cardData)) {
            try {
                JSONObject jsonObject = new JSONObject(cardData);
                downloadUrl = jsonObject.optString("downloadUrl");
                versionCode = jsonObject.optInt("versionCode");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (LogHelper.getSession(hybridRequest.getPackage()) == null) {
            LogHelper.addPackage(hybridRequest.getPackage(), null);
        }

        HapEngine engine = HapEngine.getInstance(hybridRequest.getPackage());
        engine.setMode(mMode);
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            HybridManager hybridManager = mHybridView.getHybridManager();
            if (hybridManager != null) {
                hybridManager.loadUrl(url);
            }
        } else {
            final int finalVersionCode = versionCode;
            final String finalDownloadUrl = downloadUrl;
            final HybridRequest finalHybridRequest = hybridRequest;
            Executors.io()
                    .execute(
                            new Runnable() {

                                @Override
                                public void run() {
                                    final AppInfo appInfo =
                                            engine.getApplicationContext().getAppInfo();
                                    if (appInfo == null
                                            || finalVersionCode > appInfo.getVersionCode()) {
                                        Executors.ui().execute(() -> handleRenderPreparing());
                                        if (!TextUtils.isEmpty(finalDownloadUrl)
                                                || finalVersionCode <= 0) {
                                            CardInstaller.getInstance()
                                                    .installCard(
                                                            finalHybridRequest.getPackage(),
                                                            finalDownloadUrl,
                                                            cardPath,
                                                            (pkg, resultCode, errorCode) -> {
                                                                Executors.ui()
                                                                        .execute(
                                                                                () -> handleInstallResult(
                                                                                        pkg,
                                                                                        resultCode,
                                                                                        errorCode));
                                                            });
                                        } else {
                                            CardInstaller.getInstance()
                                                    .install(
                                                            finalHybridRequest.getPackage(),
                                                            finalVersionCode,
                                                            new InstallListener() {
                                                                @Override
                                                                public void onInstallResult(
                                                                        String pkg,
                                                                        int resultCode) {
                                                                    onInstallResult(
                                                                            pkg, resultCode,
                                                                            InstallListener.INSTALL_ERROR_UNKNOWN);
                                                                }

                                                                @Override
                                                                public void onInstallResult(
                                                                        String pkg, int resultCode,
                                                                        int errorCode) {
                                                                    Executors.ui()
                                                                            .execute(
                                                                                    () -> handleInstallResult(
                                                                                            pkg,
                                                                                            resultCode,
                                                                                            errorCode));
                                                                }
                                                            });
                                        }
                                    } else {
                                        Executors.ui().execute(() -> loadUrlInternal(url));
                                    }
                                }
                            });
        }
    }

    private void handleInstallResult(String pkg, int resultCode, int errorCode) {
        if (mIsDestroyed) {
            Log.w(TAG, "card has been distroyed. skip handleInstallResult");
            return;
        }

        hideLoadingView();
        if (resultCode == InstallListener.INSTALL_RESULT_OK) {
            loadUrlInternal(mUrl);
        } else {
            handleRenderFail(IRenderListener.ErrorCode.ERROR_INSTALL_FAILED, "Install failed");
        }
    }

    private void handleRenderFail(int errorCode, String message) {
        boolean handled = onRenderFailed(errorCode, message);
        if (!handled) {
            showFailView();
        }
    }

    private void handleRenderPreparing() {
        if (mIsDestroyed) {
            Log.w(TAG, "card has been distroyed. skip handleRenderPreparing");
            return;
        }

        boolean handled = onRenderProgress();
        if (!handled) {
            showLoadingView();
        }
    }

    private synchronized void showLoadingView() {
        if (mLoadingView == null) {
            initLoadingView();
            addView(mLoadingView);
        }
    }

    private void initLoadingView() {
        mLoadingView =
                LayoutInflater.from(getContext())
                        .inflate(R.layout.card_default_layout, this, false);
        ((TextView) mLoadingView.findViewById(R.id.tip)).setText(R.string.card_loading);
    }

    private synchronized void hideLoadingView() {
        if (mLoadingView != null) {
            removeView(mLoadingView);
            mLoadingView = null;
        }
    }

    private synchronized void showFailView() {
        if (mLoadingView == null) {
            initFailView();
            addView(mFailView);
        }
    }

    private void initFailView() {
        mFailView =
                LayoutInflater.from(getContext())
                        .inflate(R.layout.card_default_layout, this, false);
        ((TextView) mFailView.findViewById(R.id.tip)).setText(R.string.card_load_failed);
        mFailView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideFailView();
                        reload();
                    }
                });
    }

    private synchronized void hideFailView() {
        if (mFailView != null) {
            removeView(mFailView);
            mFailView = null;
        }
    }

    public void reload() {
        loadUrl(mUrl, "");
    }

    private void loadUrlInternal(String url) {
        if (mIsDestroyed) {
            Log.w(TAG, "card has been distroyed. skip loadUrlInternal");
            return;
        }

        HybridManager hybridManager = mHybridView.getHybridManager();
        if (hybridManager != null) {
            hybridManager.loadUrl(url);
        }
    }

    private void registerLifecycleCallback() {
        unregisterLifecycleCallback();
        mActivityLifecycleCallbacks =
                new SimpleActivityLifecycleCallbacks() {
                    private boolean mStarted;
                    private boolean mResumed;
                    private boolean mDestroyed;

                    @Override
                    public void onActivityStarted(Activity activity) {
                        if (mChangeVisibilityManually || mStarted || mDestroyed) {
                            return;
                        }

                        HybridManager manager = mHybridView.getHybridManager();
                        if (manager != null && activity == manager.getActivity()) {
                            manager.onStart();
                        }
                        mStarted = true;
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        if (mChangeVisibilityManually || mResumed || mDestroyed) {
                            return;
                        }

                        HybridManager manager = mHybridView.getHybridManager();
                        if (manager != null && activity == manager.getActivity()) {
                            manager.onResume();
                        }
                        mResumed = true;
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        if (mChangeVisibilityManually || !mResumed || mDestroyed) {
                            return;
                        }

                        HybridManager manager = mHybridView.getHybridManager();
                        if (manager != null && activity == manager.getActivity()) {
                            manager.onPause();
                        }
                        mResumed = false;
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        if (mChangeVisibilityManually || !mStarted || mDestroyed) {
                            return;
                        }

                        HybridManager manager = mHybridView.getHybridManager();
                        if (manager != null && activity == manager.getActivity()) {
                            manager.onStop();
                        }
                        mStarted = false;
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (mDestroyed) {
                            return;
                        }

                        boolean oldEnable = mChangeVisibilityManually;
                        mChangeVisibilityManually = false;
                        onActivityPaused(activity);
                        onActivityStopped(activity);
                        mChangeVisibilityManually = oldEnable;

                        HybridManager manager = mHybridView.getHybridManager();
                        if (manager != null && activity == manager.getActivity()) {
                            manager.onDestroy();
                            unregisterLifecycleCallback();
                        }
                        mDestroyed = true;
                    }
                };
        mAppContext.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    private void unregisterLifecycleCallback() {
        if (mActivityLifecycleCallbacks != null) {
            mAppContext.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mHybridView.getHybridManager().onActivityResult(requestCode, resultCode, data);
    }

    public void setAutoDestroy(boolean autoDestroy) {
        mAutoDestroy = autoDestroy;
    }

    public void destroy() {
        if (mDestroyListener != null) {
            mDestroyListener.onDestroyed();
        }
        super.destroy(false);
        unregisterLifecycleCallback();
    }

    @Override
    public void destroy(boolean immediately) {
        if (mAutoDestroy) {
            if (mDestroyListener != null) {
                mDestroyListener.onDestroyed();
            }
            super.destroy(immediately);
            unregisterLifecycleCallback();
        }
    }

    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        mHybridView
                .getHybridManager()
                .onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public HybridManager getHybridManager() {
        return mHybridView.getHybridManager();
    }

    public void setRenderListener(IRenderListener renderListener) {
        mRenderListener = renderListener;
    }

    @Override
    public void setCurrentPageVisible(boolean visible) {
        super.setCurrentPageVisible(visible && (mChangeVisibilityManually || isVisible()));
    }

    private boolean isVisible() {
        return !mIsStopped && isShown();
    }

    public void fold(boolean fold) {
        if (mPageManager.getCurrPage() != null) {
            int id = mPageManager.getCurrPage().pageId;
            getJsThread().postFoldCard(id, fold);
        }
    }

    @Override
    public void applyActions(VDocument document, Page page) {
        // 将 applyAction 的任务分解成小的批次, 防止阻塞主应用的UI线程任务
        mApplyActionWorker.start();
    }

    private RenderAction pollRenderAction() {
        if (getDocument() == null || getDocument().getComponent().isOpenWithAnimation()) {
            return null;
        }

        Page currentPage = mPageManager.getCurrPage();
        if (currentPage != null) {
            return currentPage.pollRenderAction();
        }

        return null;
    }

    public void changeVisibilityManually(boolean enable) {
        mChangeVisibilityManually = enable;
        if (mHybridView != null && mHybridView.getHybridManager() != null) {
            mHybridView.getHybridManager().changeVisibilityManually(enable);
        }
    }

    @Override
    protected void showUserException(Exception exception) {
        boolean handled = CardRuntimeErrorManager.onError(getUrl(), exception);
        if (!handled) {
            super.showUserException(exception);
        }
    }

    @Override
    protected void onRenderSuccess() {
        RuntimeLogManager.getDefault().logCardRender(getPackage(), mUrl, true, -1, "");

        IRenderListener renderListener = mRenderListener;
        if (renderListener != null) {
            renderListener.onRenderSuccess();
        }
    }

    @Override
    protected boolean onRenderFailed(int errorCode, String message) {
        RuntimeLogManager.getDefault().logCardRender(getPackage(), mUrl, false, errorCode, message);

        IRenderListener renderListener = mRenderListener;
        if (renderListener != null) {
            try {
                return renderListener.onRenderFailed(errorCode, message);
            } catch (NoSuchMethodError e) {
                Log.w(TAG, "failed to invoke onRenderFailed", e);
                renderListener.onRenderException(errorCode, message);
            }
        }
        return false;
    }

    @Override
    protected boolean onRenderProgress() {
        IRenderListener renderListener = mRenderListener;
        if (renderListener != null) {
            try {
                return renderListener.onRenderProgress();
            } catch (NoSuchMethodError e) {
                Log.w(TAG, "failed to invoke onRenderProgress", e);
            }
        }
        return false;
    }

    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    public void setDestroyListener(DestroyListener listener) {
        mDestroyListener = listener;
    }

    @Override
    public HybridView getHybridView() {
        return mHybridView;
    }

    @Override
    protected void onPageInitialized(Page page) {
        if (mChangeVisibilityManually && !mResumed) {
            Log.d(TAG, "mChangeVisibilityManually is true. skip page: " + page);
        } else {
            super.onPageInitialized(page);
        }
    }

    @Override
    public void onCardConfigChanged(CardConfig config) {
        Object keyDarkMode = CardConfigUtils.get(CardConfig.KEY_DARK_MODE);
        if (keyDarkMode instanceof Integer
                && (int) keyDarkMode == DarkThemeUtil.THEME_NIGHT_YES
                && mPageManager != null) {
            getJsThread().getRenderActionManager()
                    .updateMediaPropertyInfo(mPageManager.getCurrPage());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (CardConfigUtils.isCloseGlobalDefaultNightMode()) {
                ((Activity) getContext()).getWindow().getDecorView().setForceDarkAllowed(false);
            } else {
                ((Activity) getContext()).getWindow().getDecorView().setForceDarkAllowed(true);
            }
        }
    }

    @Override
    protected void resume() {
        super.resume();
        mResumed = true;
    }

    @Override
    protected void pause() {
        super.pause();
        mResumed = false;
    }

    public interface DestroyListener {
        void onDestroyed();
    }
}
