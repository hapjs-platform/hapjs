/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import android.view.View;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.hapjs.common.resident.ResidentManager;
import org.hapjs.common.utils.ReflectUtils;
import org.hapjs.component.bridge.ActivityStateListener;
import org.hapjs.debug.DebugUtils;
import org.hapjs.model.CardInfo;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.inspect.InspectorManager;

public class HybridManager implements HybridView.OnVisibilityChangedListener {
    public static final String TAG = "HybridManager";

    private Activity mActivity;
    private Fragment mFragment;
    private HybridView mView;
    private boolean mDetached;

    private NativeInterface mNativeInterface;
    private ResidentManager mResidentManager;

    private String mPackage;

    private Set<LifecycleListener> mListeners = new CopyOnWriteArraySet<>();
    private Set<ActivityStateListener> mForceListeners = new CopyOnWriteArraySet<>();
    private boolean mStarted;
    private boolean mResumed;
    private boolean mIsVisible;
    private boolean mChangeVisibilityManually;

    public HybridManager(Activity activity, HybridView view) {
        mActivity = activity;
        mView = view;
        mNativeInterface = new NativeInterface(this);
        mResidentManager = new ResidentManager();

        if (mView != null) {
            mView.setOnVisibilityChangedListener(this);
            mIsVisible = mView.getWebView().isShown();
        }
    }

    public HybridManager(Fragment fragment, HybridView view, int configResId) {
        this(fragment.getActivity(), view);
        mFragment = fragment;
    }

    public ApplicationContext getApplicationContext() {
        return getHapEngine().getApplicationContext();
    }

    public HapEngine getHapEngine() {
        return HapEngine.getInstance(mPackage);
    }

    public void loadUrl(String url) {
        String realUrl = DebugUtils.trySetupDebugger((RootView) mView.getWebView(), url);
        addLifecycleListener(
                new LifecycleListener() {
                    @Override
                    public void onDestroy() {
                        DebugUtils.resetDebugger();
                    }
                });
        mPackage = new HybridRequest.Builder().uri(realUrl).build().getPackage();
        initView();
        mView.loadUrl(realUrl);
    }

    public void setPackage(String pkg) {
        mPackage = pkg;
    }

    private void initView() {
        HybridSettings settings = mView.getSettings();
        initSettings(settings);

        HybridViewClient viewClient = new HybridViewClient();
        mView.setHybridViewClient(viewClient);

        HybridChromeClient chromeClient = new HybridChromeClient();
        mView.setHybridChromeClient(chromeClient);

        mView
                .getWebView()
                .addOnAttachStateChangeListener(
                        new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View v) {
                                mDetached = false;
                            }

                            @Override
                            public void onViewDetachedFromWindow(View v) {
                                mDetached = true;
                            }
                        });
        try {
            InspectorManager.getInspector().setRootView(mView.getWebView());
        } catch (java.lang.AbstractMethodError e) {
            Log.w(TAG, "setRootView error", e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initSettings(HybridSettings settings) {
        settings.setJavaScriptEnabled(true);
    }

    public boolean isDetached() {
        return mDetached;
    }

    public Activity getActivity() {
        return mActivity;
    }

    NativeInterface getNativeInterface() {
        return mNativeInterface;
    }


    public void addForceLifecycleListener(ActivityStateListener listener) {
        mForceListeners.add(listener);
    }

    public void removeForceLifecycleListener(ActivityStateListener listener) {
        mForceListeners.remove(listener);
    }

    public void addLifecycleListener(LifecycleListener listener) {
        mListeners.add(listener);
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        mListeners.remove(listener);
    }

    public void onPageChange() {
        for (LifecycleListener listener : mListeners) {
            listener.onPageChange();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        ConfigurationManager.getInstance().update(mActivity, newConfig);
    }

    public void onNewIntent(Intent intent) {
        for (LifecycleListener listener : mListeners) {
            listener.onNewIntent(intent);
        }
    }

    public void onRequest() {
        for (LifecycleListener listener : mListeners) {
            listener.onRequest();
        }
    }

    public void onStart() {
        if (!mChangeVisibilityManually && !mIsVisible) {
            Log.d(TAG, "not visible. skip onStart");
            return;
        }

        if (mStarted) {
            Log.d(TAG, "already started. skip onStart");
            return;
        }

        for (LifecycleListener listener : mListeners) {
            listener.onStart();
        }
        mStarted = true;
    }

    public void onResume() {
        if (!mChangeVisibilityManually && !mIsVisible) {
            Log.d(TAG, "not visible. skip onResume");
            return;
        }

        if (mResumed) {
            Log.d(TAG, "already resumed. skip onResume");
            return;
        }

        for (LifecycleListener listener : mListeners) {
            listener.onResume();
        }
        mResumed = true;
    }

    public void onPause() {
        if (!mResumed) {
            Log.d(TAG, "not resumed. skip onPause");
            for (ActivityStateListener listener : mForceListeners) {
                listener.onActivityPause();
            }
            return;
        }

        for (LifecycleListener listener : mListeners) {
            listener.onPause();
        }
        mResumed = false;
    }

    public void onStop() {
        if (!mStarted) {
            Log.d(TAG, "not started. skip onStop");
            return;
        }

        for (LifecycleListener listener : mListeners) {
            listener.onStop();
        }
        mStarted = false;
    }

    public void onDestroy() {
        for (LifecycleListener listener : mListeners) {
            listener.onDestroy();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (LifecycleListener listener : mListeners) {
            listener.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        for (LifecycleListener listener : mListeners) {
            listener.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public final boolean isResumed() {
        return mResumed;
    }

    public HybridView getHybridView() {
        return mView;
    }

    public ResidentManager getResidentManager() {
        return mResidentManager;
    }

    public CardInfo getCurrentPageCardInfo() {
        if (getHapEngine().isCardMode()) {
            RootView rootView = (RootView) mView.getWebView();
            Page page = rootView.getCurrentPage();
            if (page != null) {
                return (CardInfo) page.getRoutableInfo();
            }
        }
        return null;
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        if (mFragment == null) {
            mActivity.startActivityForResult(intent, requestCode);
        } else {
            mFragment.startActivityForResult(intent, requestCode);
        }
    }

    public void requestPermissions(String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                if (mFragment == null) {
                    Method method = Activity.class
                            .getMethod("requestPermissions", String[].class, int.class);
                    method.invoke(mActivity, permissions, requestCode);
                } else {
                    Method method = Fragment.class
                            .getMethod("requestPermissions", String[].class, int.class);
                    method.invoke(mFragment, permissions, requestCode);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            int result = mActivity.checkPermission(permission, Process.myPid(), Process.myUid());
            return PackageManager.PERMISSION_GRANTED == result;
        } else {
            return true;
        }
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        Log.d(TAG, "onVisibilityChanged visible=" + visible);

        boolean oldVisible = mIsVisible;
        mIsVisible = visible;

        if (!mChangeVisibilityManually) {
            if (!oldVisible && visible) {
                onStart();
                // NFC: Foreground dispatch can only be enabled when RuntimeActivity is resumed
                // so, cannot call HybridManager.onResume() ahead of RuntimeActivity.onResume()
                if (isResumed(getActivity())) {
                    onResume();
                }
            } else if (oldVisible && !visible) {
                onPause();
                onStop();
            }
        }
    }

    public void changeVisibilityManually(boolean enable) {
        mChangeVisibilityManually = enable;
    }

    private boolean isResumed(Activity activity) {
        Object obj = ReflectUtils.invokeMethod(Activity.class.getName(), activity,
                "isResumed", null, null);
        if (null != obj) {
            return (boolean) obj;
        }
        return false;
    }
}
