/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.HostCallbackManager;
import org.hapjs.bridge.HybridManager;
import org.hapjs.card.api.Card;
import org.hapjs.card.api.CardLifecycleCallback;
import org.hapjs.card.api.CardMessageCallback;
import org.hapjs.card.api.IRenderListener;
import org.hapjs.card.api.PackageListener;
import org.hapjs.card.support.CardConstants;
import org.hapjs.card.support.CardView;
import org.hapjs.card.support.R;
import org.hapjs.card.support.impl.utils.ReflectUtils;
import org.hapjs.card.support.utils.InternalConfig;
import org.hapjs.common.executors.Executors;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ResourceConfig;

class CardImpl implements Card {
    private static final String TAG = "CardImpl";
    private Activity mActivity;
    private CardView mCardView;
    private FrameLayout mCardViewWrapper;
    private String mUri;
    private HapEngine.Mode mMode;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mChangeVisibilityManually;
    private PackageListener mPackageListener;
    private BroadcastReceiver mReceiver;

    CardImpl(Activity activity, HapEngine.Mode mode) {
        mActivity = activity;
        mMode = mode;
    }

    CardImpl(Activity activity, String uri, HapEngine.Mode mode) {
        mActivity = activity;
        mUri = uri;
        mMode = mode;
    }

    @Override
    public View getView() {
        if (mCardViewWrapper == null) {
            String cardViewName = InternalConfig.getInstance(mActivity).getCardViewName();
            if (TextUtils.isEmpty(cardViewName)) {
                mCardView = new CardView(mActivity);
            } else {
                mCardView = ReflectUtils.createCardView(cardViewName, mActivity);
            }
            mCardView.initialize(mActivity, mMode, mChangeVisibilityManually);
            mCardView.setDestroyListener(
                    new CardView.DestroyListener() {
                        @Override
                        public void onDestroyed() {
                            if (mCardView.getCurrentPage() != null) {
                                HostCallbackManager.getInstance()
                                        .onCardDestroy(mCardView.getCurrentPage().pageId);
                            }
                            unregisterAppUpdateReceiver();
                        }
                    });

            mCardViewWrapper = new FrameLayout(mActivity);
            mCardViewWrapper.addView(mCardView);
        }
        return mCardViewWrapper;
    }

    private CardView getCardView() {
        getView();
        return mCardView;
    }

    @Override
    public void load() {
        load(mUri, "");
    }

    @Override
    public void load(String url) {
        mUri = url;
        load();
    }

    @Override
    public void load(String url, String cardData) {
        Log.d(TAG, "load url=" + url + ", cardData=" + cardData);
        getCardView().loadUrl(url, cardData);
        registerAppUpdateReceiver();
    }

    private synchronized void registerAppUpdateReceiver() {
        if (mReceiver == null) {
            mReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String intentPlatform =
                                    intent.getStringExtra(CardConstants.EXTRA_PLATFORM);
                            String intentPkg = intent.getStringExtra(CardConstants.EXTRA_PACKAGE);
                            if (TextUtils.equals(ResourceConfig.getInstance().getPlatform(),
                                    intentPlatform)
                                    && TextUtils.equals(mCardView.getPackage(), intentPkg)) {
                                if (CardConstants.ACTION_PACKAGE_PACKAGE_UPDATED
                                        .equals(intent.getAction())) {
                                    AppInfo oldAppInfo = mCardView.getAppInfo();
                                    if (oldAppInfo == null) {
                                        Log.d(TAG, "card has not loaded");
                                    } else {
                                        Executors.io()
                                                .execute(
                                                        () -> {
                                                            ApplicationContext appContext =
                                                                    HapEngine.getInstance(intentPkg)
                                                                            .getApplicationContext();
                                                            AppInfo newAppInfo =
                                                                    appContext.getAppInfo(false);
                                                            if (newAppInfo == null
                                                                    ||
                                                                    newAppInfo.getVersionCode()
                                                                            != oldAppInfo
                                                                                    .getVersionCode()) {
                                                                if (newAppInfo == null) {
                                                                    Log.e(TAG,
                                                                            "app updated but failed to get AppInfo");
                                                                } else {
                                                                    Log.d(TAG,
                                                                            "version code changed");
                                                                }
                                                                Executors.ui()
                                                                        .execute(
                                                                                () ->
                                                                                        handlePackageChanged(
                                                                                                context,
                                                                                                intentPkg,
                                                                                                intent.getAction()));
                                                            } else {
                                                                Log.d(TAG,
                                                                        "version code not changed");
                                                            }
                                                        });
                                    }
                                } else if (CardConstants.ACTION_PACKAGE_PACKAGE_REMOVED.equals(
                                        intent.getAction())) {
                                    handlePackageChanged(context, intentPkg, intent.getAction());
                                } else {
                                    Log.w(TAG, "unknown action: " + intent.getAction());
                                }
                            }
                        }
                    };
            IntentFilter filter = new IntentFilter();
            filter.addAction(CardConstants.ACTION_PACKAGE_PACKAGE_UPDATED);
            filter.addAction(CardConstants.ACTION_PACKAGE_PACKAGE_REMOVED);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            mActivity.registerReceiver(mReceiver, filter);
        }
    }

    private void handlePackageChanged(Context context, String pkg, String action) {
        PackageListener packageListener = mPackageListener;
        if (packageListener == null) {
            if (CardConstants.ACTION_PACKAGE_PACKAGE_UPDATED.equals(action)) {
                Log.d(TAG, "mPackageListener is null. just reload.");
                getCardView().reload();
            }
            // if the host does not deal with package changed event, don't destroy card.
            return;
        }

        if (CardConstants.ACTION_PACKAGE_PACKAGE_UPDATED.equals(action)) {
            packageListener.onPackageUpdated(pkg, CardImpl.this);
        } else if (CardConstants.ACTION_PACKAGE_PACKAGE_REMOVED.equals(action)) {
            packageListener.onPackageRemoved(pkg, CardImpl.this);
        } else {
            Log.w(TAG, "unknown action: " + action);
        }

        destroy();
        mCardViewWrapper.removeAllViews();
        View outdatedView =
                LayoutInflater.from(mCardViewWrapper.getContext())
                        .inflate(R.layout.card_default_layout, mCardViewWrapper, false);
        ((TextView) outdatedView.findViewById(R.id.tip)).setText(R.string.card_outdated);
        mCardViewWrapper.addView(outdatedView);
        Log.d(TAG, "destroy card on " + action + ", url=" + mUri);
    }

    private synchronized void unregisterAppUpdateReceiver() {
        if (mReceiver != null) {
            mActivity.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    @Override
    public String getUri() {
        return mUri;
    }

    @Override
    public void setVisible(boolean visible) {
        getCardView().setCurrentPageVisible(visible);
    }

    @Override
    public void setAutoDestroy(boolean autoDestroy) {
        getCardView().setAutoDestroy(autoDestroy);
    }

    @Override
    public void destroy() {
        getCardView().destroy();
    }

    @Override
    public boolean isDestroyed() {
        return getCardView().isDestroyed();
    }

    @Override
    public void setMessageCallback(CardMessageCallback callback) {
        if (mUri == null) {
            Log.e(
                    TAG,
                    "registerMessageCallBack failed ! Please call the method load() before registerMessageCallBack.");
            return;
        }
        if (callback == null) {
            removeCallbackInternal();
        } else {
            if (!isDestroyed()) {
                addCallbackInternal(callback);
            } else {
                Log.w(TAG, "CardView has destroyed");
            }
        }
    }

    @Override
    public void sendMessage(int code, String data) {
        if (mCardView.getHybridManager() != null) {
            HostCallbackManager.getInstance()
                    .doJsCallback(mCardView.getHybridManager(), code, data);
        }
    }

    @Override
    public void fold(boolean fold) {
        mCardView.fold(fold);
    }

    @Override
    public void setLifecycleCallback(CardLifecycleCallback callback) {
        if (mUri == null || callback == null) {
            Log.e(
                    TAG,
                    "setLifecycleCallback failed ! Please call the method load() before setLifecycleCallback.");
            return;
        }
        addLifecycleCallback(callback);
    }

    @Override
    public void setRenderListener(IRenderListener listener) {
        getCardView().setRenderListener(listener);
    }

    @Override
    public void onShow() {
        HybridManager hybridManager = getCardView().getHybridManager();
        if (hybridManager != null) {
            hybridManager.onStart();
            hybridManager.onResume();
        }

        getCardView().getAutoplayManager().startAll();

        if (!mChangeVisibilityManually) {
            Log.w(TAG, "call onShow but mChangeVisibilityManually is not enable");
        }
    }

    @Override
    public void onHide() {
        HybridManager hybridManager = getCardView().getHybridManager();
        if (hybridManager != null) {
            hybridManager.onPause();
            hybridManager.onStop();
        }

        getCardView().getAutoplayManager().stopAll();

        if (!mChangeVisibilityManually) {
            Log.w(TAG, "call onHide but mChangeVisibilityManually is not enable");
        }
    }

    private void addCallbackInternal(final CardMessageCallback callback) {
        if (mCardView.getHybridManager() == null) {
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            addCallbackInternal(callback);
                        }
                    },
                    10);
        } else {
            try {
                HostCallbackManager.getInstance()
                        .addHostCallback(mCardView.getHybridManager(), callback);
            } catch (Exception e) {
                Log.e(TAG, "addHostCallback failed", e);
            }
        }
    }

    private void removeCallbackInternal() {
        if (mCardView.getHybridManager() != null) {
            HostCallbackManager.getInstance().removeHostCallback(mCardView.getHybridManager());
        }
    }

    private void addLifecycleCallback(final CardLifecycleCallback callback) {
        if (mCardView.getCurrentPage() == null) {
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            addLifecycleCallback(callback);
                        }
                    },
                    10);
        } else {
            HostCallbackManager.getInstance()
                    .addLifecycleCallback(mCardView.getCurrentPage().pageId, callback);
        }
    }

    @Override
    public void changeVisibilityManually(boolean enable) {
        mChangeVisibilityManually = enable;
        if (mCardView != null) {
            mCardView.changeVisibilityManually(enable);
        }
    }

    @Override
    public void setPackageListener(PackageListener listener) {
        mPackageListener = listener;
    }
}
