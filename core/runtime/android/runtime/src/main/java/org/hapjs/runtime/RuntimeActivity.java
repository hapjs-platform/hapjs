/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.hapjs.analyzer.Analyzer;
import org.hapjs.analyzer.AnalyzerContext;
import org.hapjs.analyzer.panels.PanelDisplay;
import org.hapjs.bridge.Constants;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.common.utils.IntentUtils;
import org.hapjs.common.utils.WebViewUtils;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.Source;
import org.hapjs.model.videodata.VideoCacheManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.system.utils.TalkBackUtils;

public class RuntimeActivity extends AppCompatActivity {
    public static final String PROP_APP = "runtime.app";
    public static final String PROP_SOURCE = "runtime.source";
    public static final String PROP_SESSION = "runtime.session";
    public static final String PROP_DEBUG = "runtime.debug";
    public static final String PROP_FROM_DEBUGGER = "runtime.fromDebugger";
    public static final String EXTRA_APP = "EXTRA_APP";
    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_SOURCE = "EXTRA_SOURCE";
    public static final String EXTRA_SESSION = "EXTRA_SESSION";
    public static final String EXTRA_SESSION_EXPIRE_TIME = "EXTRA_SESSION_EXPIRE_TIME";
    public static final long SESSION_EXPIRE_SPAN = 3000L; // extra中session过期时间为3s
    @Deprecated
    public static final String EXTRA_LAUNCH_FROM = "EXTRA_LAUNCH_FROM";
    public static final String EXTRA_ENABLE_DEBUG = "ENABLE_DEBUG";
    public static final String EXTRA_FROM_DEBUGGER = "EXTRA_FROM_DEBUGGER";
    public static final String EXTRA_SHOULD_RELOAD = "SHOULD_RELOAD";
    public static final String EXTRA_WEB_DEBUG_ENABLED = "WEB_DEBUG_ENABLED";
    /**
     * Launch task from history. It can be used along with {@link #MODE_RESET_TASK_IF_NEEDED}
     */
    public static final int MODE_LAUNCHED_FROM_HISTORY = 1;
    /**
     * Reset task if need. It can be used along with {@link #MODE_LAUNCHED_FROM_HISTORY}. Currently it
     * would clear task if target path is '/'.
     */
    public static final int MODE_RESET_TASK_IF_NEEDED = 2;
    /**
     * Clear task and reload app
     */
    public static final int MODE_CLEAR_TASK = 4;
    /**
     * Default mode
     */
    public static final int MODE_DEFAULT = MODE_LAUNCHED_FROM_HISTORY;
    private static final String TAG = "RuntimeActivity";
    // Runtime parameter is disabled currently
    private static final String EXTRA_RUNTIME = "EXTRA_RUNTIME";
    private static final String RUNTIME_WEBKIT = "webkit";
    private static final String RUNTIME_ANDROID = "android";

    private static final int LOAD_MODE_CLEAR = 0;
    private static final int LOAD_MODE_STANDARD = 1;
    private static final int LOAD_MODE_HISTORY = 2;
    private static int sRequestBaseCode = Constants.ACTIVITY_REQUEST_CODE_BASE;

    protected HybridView mHybridView;
    protected String mSession;
    protected boolean mEnableDebug;
    private HybridRequest.HapRequest mRequest;
    private int mMode;
    private String mLaunchFrom;
    private boolean mFromDebugger;
    private boolean mShouldReload;

    protected static int getRequestBaseCode() {
        sRequestBaseCode += 100;
        return sRequestBaseCode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        // onSaveInstanceState is disabled, so only use intent
        load(intent.getExtras());
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        if (mHybridView != null && mHybridView.getWebView() instanceof RootView) {
            ((RootView) mHybridView.getWebView()).setInMultiWindowMode(isInMultiWindowMode);
        }
    }

    protected void load(Bundle extras) {
        mRequest = parseRequest(extras);
        mMode = parseMode(extras);
        Source source = Source.fromIntent(getIntent());
        if (source == null) {
            source = new Source();
            source.setType(Source.TYPE_UNKNOWN);
        }
        mLaunchFrom = source.toJson().toString();
        mSession = parseSession(extras);

        if (mRequest == null) {
            throw new RuntimeException("hybridUrl is null");
        } else {
            System.setProperty(PROP_APP, mRequest.getPackage());
            System.setProperty(PROP_SOURCE, mLaunchFrom);
            System.setProperty(PROP_SESSION, mSession);
            System.setProperty(PROP_DEBUG, mEnableDebug ? "true" : "false");
            System.setProperty(PROP_FROM_DEBUGGER, mFromDebugger ? "true" : "false");
            LogHelper.addPackage(mRequest.getPackage(), mLaunchFrom, mSession);
            setupWebViewDataDirectory(mRequest.getPackage());
            load(mRequest);
        }

        boolean webDebugEnabled = extras.getBoolean(EXTRA_WEB_DEBUG_ENABLED);
        if (webDebugEnabled) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    protected void load(HybridRequest.HapRequest request) {
        int loadMode = getLoadMode(request);
        String url = request.getUri();
        Log.d(TAG, "loadUrl: url=" + url + ", mode=" + loadMode);
        switch (loadMode) {
            case LOAD_MODE_CLEAR:
                if (mHybridView != null && mShouldReload) {
                    View root = mHybridView.getWebView();
                    if (root instanceof RootView) {
                        notifyOnRequest();
                        ((RootView) root).reloadPackage();
                    }
                } else {
                    RootView rootView = initializeAndroidRuntime(url);
                    removeHybridView();
                    notifyOnRequest();
                    getContentView().addView(rootView, 0);
                }
                break;
            case LOAD_MODE_STANDARD:
                notifyOnRequest();
                mHybridView.loadUrl(url);
                break;
            case LOAD_MODE_HISTORY:
                break;
            default:
                break;
        }
    }

    /**
     * TODO: need a method to query current loading url
     *
     * @return
     */
    protected HybridRequest.HapRequest getHybridRequest() {
        return mRequest;
    }

    /**
     * @return app id retrieved from intent or savedInstanceState. It may different from {@link
     * #getRunningPackage()} if loading process has not finished
     */
    public String getPackage() {
        return mRequest == null ? null : mRequest.getPackage();
    }

    /**
     * @return app id running in hybrid view. It may different from {@link #getPackage()} if loading
     * process has not finished
     */
    public String getRunningPackage() {
        return mHybridView == null
                ? null
                : mHybridView.getHybridManager().getApplicationContext().getPackage();
    }

    protected HybridRequest.HapRequest parseRequest(Bundle extras) {
        if (extras != null) {
            mEnableDebug = extras.getBoolean(EXTRA_ENABLE_DEBUG);
            mFromDebugger = extras.getBoolean(EXTRA_FROM_DEBUGGER);
            String pkg = extras.getString(EXTRA_APP);
            String path = extras.getString(EXTRA_PATH);
            if (pkg != null && pkg.length() > 0) {
                HybridRequest request = new HybridRequest.Builder().pkg(pkg).uri(path).build();
                if (request instanceof HybridRequest.HapRequest) {
                    return (HybridRequest.HapRequest) request;
                }
            }
            mEnableDebug = false;
        }
        return null;
    }

    private int parseMode(Bundle extras) {
        int mode;
        if (extras != null) {
            mode = extras.getInt(EXTRA_MODE, MODE_DEFAULT);
        } else {
            mode = MODE_DEFAULT;
        }
        return mode;
    }

    protected String parseSession(Bundle extras) {
        String session = extras != null ? extras.getString(EXTRA_SESSION) : null;
        long sessionExpireTime = extras != null ? extras.getLong(EXTRA_SESSION_EXPIRE_TIME) : 0L;
        if (TextUtils.isEmpty(session) || sessionExpireTime <= System.currentTimeMillis()) {
            session = LogHelper.createSession();
        }
        return session;
    }

    private String parseRuntime(Bundle extras) {
        return extras == null ? null : extras.getString(EXTRA_RUNTIME);
    }

    protected void setupWebViewDataDirectory(String pkg) {
        WebViewUtils.setDataDirectory(pkg);
    }

    private int getLoadMode(HybridRequest.HapRequest request) {
        if (!request.getPackage().equals(getRunningPackage())) {
            return LOAD_MODE_CLEAR;
        }

        if (mHybridView == null || !mHybridView.getWebView().isAttachedToWindow()) {
            return LOAD_MODE_CLEAR;
        }

        if ((mMode & MODE_CLEAR_TASK) == MODE_CLEAR_TASK) {
            return LOAD_MODE_CLEAR;
        }

        if ("/".equals(request.getPagePath())) {
            if ((mMode & MODE_RESET_TASK_IF_NEEDED) == MODE_RESET_TASK_IF_NEEDED) {
                return LOAD_MODE_CLEAR;
            } else if (request.getLaunchFlags() != null && request.getLaunchFlags().size() > 0) {
                return LOAD_MODE_STANDARD;
            } else {
                return LOAD_MODE_HISTORY;
            }
        } else {
            return LOAD_MODE_STANDARD;
        }
    }

    protected void removeHybridView() {
        View hybridRoot = findViewById(R.id.hybrid_view);
        if (hybridRoot != null) {
            ViewGroup hybridParent = (ViewGroup) hybridRoot.getParent();
            hybridParent.removeView(hybridRoot);
            if (hybridRoot instanceof RootView) {
                ((RootView) hybridRoot).destroy(true);
            }
        }
    }

    private RootView initializeAndroidRuntime(String url) {
        final RootView rootView = getInnerRootView();
        HybridView hybridView = new org.hapjs.bridge.impl.android.HybridViewImpl(rootView);
        registerHybridView(hybridView, url);
        hybridView
                .getHybridManager()
                .addLifecycleListener(
                        new LifecycleListener() {

                            @Override
                            public void onRequest() {
                                rootView.onActivityRequest();
                            }

                            @Override
                            public void onStart() {
                                rootView.onActivityStart();
                            }

                            @Override
                            public void onResume() {
                                rootView.onActivityResume();
                            }

                            @Override
                            public void onPause() {
                                rootView.onActivityPause();
                            }

                            @Override
                            public void onStop() {
                                rootView.onActivityStop();
                            }

                            @Override
                            public void onDestroy() {
                                rootView.onActivityDestroy();
                            }
                        });

        rootView.setOnDetachedListener(
                () -> {
                    if (!isDestroyed()) {
                        JsThread jsThread = rootView.getJsThread();
                        if (jsThread != null) {
                            jsThread.block(0);
                        }
                    } else {
                        rootView.release();
                    }
                });
        return rootView;
    }

    protected RootView getInnerRootView() {
        final RootView rootView = new RootView(this);
        rootView.setId(R.id.hybrid_view);
        return rootView;
    }

    public HybridView getHybridView() {
        return mHybridView;
    }

    public ViewGroup getContentView() {
        return (ViewGroup) findViewById(android.R.id.content);
    }

    /**
     * Register a hybrid view to be managed by current activity. Note the hybrid view will not be
     * added to UI in this method.
     *
     * @param hybridView the hybrid view to be managed.
     * @throws IllegalArgumentException if view is not a hybrid view.
     */
    protected void registerHybridView(HybridView hybridView) {
        registerHybridView(hybridView, null);
    }

    /**
     * Register a hybrid view to be managed by current activity. Note the hybrid view will not be
     * added to UI in this method.
     *
     * @param hybridView the hybrid view to be managed.
     * @param url        url to be loaded.
     * @throws IllegalArgumentException if view is not a hybrid view.
     */
    protected void registerHybridView(HybridView hybridView, String url) {
        mHybridView = hybridView;
        HybridManager manager = hybridView.getHybridManager();
        manager.loadUrl(url);
    }

    protected void notifyOnRequest() {
        if (mHybridView != null) {
            mHybridView.getHybridManager().onRequest();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mHybridView != null) {
            mHybridView.getHybridManager().onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (IntentUtils.getLaunchAction(this).equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mShouldReload = extras.getBoolean(EXTRA_SHOULD_RELOAD);
                extras.putBoolean(EXTRA_ENABLE_DEBUG, mEnableDebug);
            } else {
                mShouldReload = false;
            }
            load(extras);
        } else if (mHybridView != null) {
            mHybridView.getHybridManager().onNewIntent(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mHybridView != null) {
            mHybridView.getHybridManager().onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mHybridView != null) {
            mHybridView.getHybridManager().onResume();
        }
        TalkBackUtils.isEnableTalkBack(RuntimeActivity.this,true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHybridView != null) {
            mHybridView.getHybridManager().onPause();
        }
    }

    /**
     * Override this method, do not use onSaveInstanceState state, in case of onRestoreInstanceState
     * causing fragment crash
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // do nothing, do not call super onSaveInstanceState
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mHybridView != null) {
            mHybridView.getHybridManager().onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHybridView != null) {
            mHybridView.getHybridManager().onDestroy();
        }
        VideoCacheManager.getInstance().clearAllVideoData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mHybridView != null) {
            mHybridView.getHybridManager().onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (mHybridView != null) {
            mHybridView
                    .getHybridManager()
                    .onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        final boolean[] isIntercept = new boolean[1];
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event.isCanceled()) {
            if (mHybridView != null
                    && mHybridView.canGoBack()
                    && !mHybridView.getHybridManager().isDetached()) {
                if (!interceptedByAnalyzerPanel()) {
                    mHybridView.goBack();
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mHybridView != null && !mHybridView.getHybridManager().isDetached()) {
                try {

                    CountDownLatch countDownLatch = new CountDownLatch(1);
                    mHybridView.menuButtonPressPage(
                            new HybridView.OnKeyUpListener() {
                                @Override
                                public void consume(boolean isConsumption) {
                                    isIntercept[0] = isConsumption;
                                    countDownLatch.countDown();
                                }
                            });
                    boolean cdlResulet = countDownLatch.await(2000, TimeUnit.MILLISECONDS);
                    if (isIntercept[0] && cdlResulet) {
                        return true;
                    } else {
                        return super.onKeyUp(keyCode, event);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean interceptedByAnalyzerPanel() {
        AnalyzerContext analyzerContext = Analyzer.get().getAnalyzerContext();
        if (analyzerContext != null) {
            PanelDisplay panelDisplay = analyzerContext.getPanelDisplay();
            if (panelDisplay != null) {
                return panelDisplay.onBackPressed();
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mHybridView != null && mHybridView.needRunInBackground()) {
            moveTaskToBack(true);
        } else {
            finish();
        }
    }
}
