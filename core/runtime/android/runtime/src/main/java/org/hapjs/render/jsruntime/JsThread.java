/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import static org.hapjs.render.RootView.MSG_APP_LOAD_END;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8RuntimeException;
import com.eclipsesource.v8.V8Value;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hapjs.bridge.EnvironmentManager;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.V8ObjectHelper;
import org.hapjs.chunk.JsChunksManager;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FrescoUtils;
import org.hapjs.common.utils.LogUtils;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.component.ComponentRegistry;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.component.view.keyevent.KeyEventManager;
import org.hapjs.io.AssetSource;
import org.hapjs.io.FileSource;
import org.hapjs.io.JavascriptReader;
import org.hapjs.io.RpkSource;
import org.hapjs.io.TextReader;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.RoutableInfo;
import org.hapjs.model.ScreenOrientation;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.IdGenerator;
import org.hapjs.render.MultiWindowManager;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.RenderActionPackage;
import org.hapjs.render.AppResourcesLoader;
import org.hapjs.render.RootView;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.action.RenderActionDocument;
import org.hapjs.render.action.RenderActionManager;
import org.hapjs.render.css.CSSParser;
import org.hapjs.render.css.CSSStyleSheet;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.skeleton.DefaultSkeletonProviderImpl;
import org.hapjs.render.skeleton.SkeletonConfigParser;
import org.hapjs.render.skeleton.SkeletonDSLParser;
import org.hapjs.render.skeleton.SkeletonProvider;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.runtime.inspect.InspectorManager;
import org.hapjs.runtime.inspect.InspectorProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class JsThread extends HandlerThread {

    public static final String CONFIGURATION_TYPE_LOCALE = "locale";
    public static final String CONFIGURATION_TYPE_THEME_MODE = "themeMode";
    public static final String CONFIGURATION_TYPE_ORIENTATION = "orientation";
    public static final String CONFIGURATION_TYPE_SCREEN_SIZE = "screenSize";
    public static final String INFRASJS_SNAPSHOT_SO_NAME = "infrasjs_snapshot";
    public static final boolean HAS_INFRASJS_SNAPSHOT;
    private static final String TAG = "JsThread";
    private static final int STATE_NONE = -1;
    private static final int STATE_RUNTIME_INITED = 0;
    private static final int STATE_DESTROYING = 1;
    private static final int STATE_DESTROYED = 2;

    static {
        boolean hasInfraSnapshot;
        try {
            System.loadLibrary(INFRASJS_SNAPSHOT_SO_NAME);
            hasInfraSnapshot = true;
        } catch (UnsatisfiedLinkError e) {
            hasInfraSnapshot = false;
        }
        HAS_INFRASJS_SNAPSHOT = hasInfraSnapshot;
    }

    private final Context mContext;
    private final int mAppId;
    private final H mHandler;
    Handler mMainHandler;
    PageManager mPageManager;
    AppInfo mAppInfo;
    RootView mRootView;
    private final JavaVoidCallback keyEventCallback =
            new JavaVoidCallback() {
                @Override
                public void invoke(V8Object v8Object, V8Array args) {
                    try {
                        boolean consumed = Boolean.parseBoolean(args.get(0).toString());
                        int hashcode = Integer.parseInt(args.get(1).toString());
                        KeyEventManager.getInstance().injectKeyEvent(consumed, mRootView, hashcode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        JsUtils.release(args);
                    }
                }
            };
    private LifecycleCallback mCallback;
    private JsContext mJsContext;
    private JsBridge mJsBridge;
    private JsBridgeTimer mJsTimer;
    private JsBridgeHistory mJsBridgeHistory;
    private Profiler mProfiler;
    private ExtensionManager mExtensionManager;
    private RenderActionManager mRenderActionManager;
    private JsChunksManager mJsChunksManager;
    private ConditionVariable mBlocker = new ConditionVariable(true);
    private volatile boolean mBlocked;
    private String mSessionLastAppShow;
    private boolean mIsTerminateExecution;
    private int mApplicationState = STATE_NONE;

    protected JsThread(Context context) {
        super("JsThread");

        start();

        mContext = context;
        mAppId = IdGenerator.generateAppId();
        mHandler = new H(getLooper());
        mRenderActionManager = new RenderActionManager();

        Message.obtain(mHandler, H.MSG_INIT).sendToTarget();
    }

    public void attach(
            Handler mainHandler,
            AppInfo appInfo,
            RootView rootView,
            LifecycleCallback lifecycleCallback,
            PageManager pageManager) {
        mMainHandler = mainHandler;
        mAppInfo = appInfo;
        mRootView = rootView;
        mCallback = lifecycleCallback;
        mPageManager = pageManager;

        Message.obtain(mHandler, H.MSG_ATTACH).sendToTarget();
    }

    private void onInit() {
        mJsContext = new JsContext(this);
        V8 v8 = mJsContext.getV8();

        mJsBridge = new JsBridge(this, mRenderActionManager);
        mJsBridge.register(v8);
        registerKeyEventCallback();

        mJsTimer = new JsBridgeTimer(mJsContext, mHandler);
        JsUtils.registerAllPublicMethodsToRoot(mJsTimer);

        mProfiler = new Profiler(v8, getId());
        v8.add("profiler", mProfiler);
        mProfiler.registerJavaMethod(mProfiler.isEnabled, "isEnabled");
        mProfiler.registerJavaMethod(mProfiler.record, "record");
        mProfiler.registerJavaMethod(mProfiler.time, "time");
        mProfiler.registerJavaMethod(mProfiler.timeEnd, "timeEnd");
        mProfiler.registerJavaMethod(mProfiler.saveProfilerData, "saveProfilerData");

        mJsBridgeHistory = new JsBridgeHistory(mContext, mJsContext);
        v8.add("history", mJsBridgeHistory);
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.back, "back");
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.push, "push");
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.replace, "replace");
        mJsBridgeHistory.registerJavaMethod(mJsBridgeHistory.clear, "clear");

        InspectorManager.getInstance().notifyJsThreadReady(this);

        createRuntime();
        initInfras();

        mExtensionManager = new ExtensionManager(this, mContext);
        mExtensionManager.onRuntimeInit(v8);

        ComponentRegistry.registerBuiltInComponents(v8);
        FrescoUtils.initialize(mContext);
    }

    private void initInfras() {
        V8 v8 = mJsContext.getV8();
        try {
            v8.executeVoidFunction("initInfras", null);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        }
    }

    public boolean isApplicationDebugEnabled() {
        return (mContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)
                == ApplicationInfo.FLAG_DEBUGGABLE;
    }

    private void onAttach() {
        mJsBridge.attach(mRootView);
        mJsBridgeHistory.attach(mPageManager);
        mExtensionManager.attach(mRootView, mPageManager, mAppInfo);

        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        V8 v8 = mJsContext.getV8();
        v8.executeScript(EnvironmentManager.buildRegisterJavascript(mContext, mAppInfo));

        if (mCallback != null) {
            mCallback.onRuntimeCreate();
        }
        mExtensionManager.onRuntimeCreate(mAppInfo);
    }

    public Handler getHandler() {
        return mHandler;
    }

    public JsContext getJsContext() {
        return mJsContext;
    }

    public ExtensionManager getBridgeManager() {
        return mExtensionManager;
    }

    public RenderActionManager getRenderActionManager() {
        return mRenderActionManager;
    }

    public void postExecuteScript(String script) {
        Message.obtain(mHandler, H.MSG_EXECUTE_SCRIPT, script).sendToTarget();
    }

    public void postExecuteFunction(String name, Object... params) {
        Message.obtain(mHandler, H.MSG_EXECUTE_FUNCTION, new Pair(name, params)).sendToTarget();
    }

    private void createRuntime() {
        try {
            if (!HAS_INFRASJS_SNAPSHOT) {
                String script = null;
                if (isApplicationDebugEnabled()) {
                    File file =
                            new File(Environment.getExternalStorageDirectory(),
                                    "quickapp/assets/js/infras.js");
                    script = JavascriptReader.get().read(new FileSource(file));
                    if (script != null) {
                        Toast.makeText(
                                mContext,
                                "load infras.js from sdcard, please remove quickapp folder in sdcard if you are not dev",
                                Toast.LENGTH_SHORT)
                                .show();
                        Log.d(TAG, "load infras.js from sdcard");
                    }
                }

                RuntimeLogManager.getDefault()
                        .logJsThreadTaskStart(mContext.getPackageName(),
                                RuntimeLogManager.KEY_INFRAS_JS_LOAD);
                if (script == null) {
                    script = readInfrasAsset();
                }

                if (script == null) {
                    Log.e(TAG, "failed to read js/infras.js");
                    String platform = ResourceConfig.getInstance().getPlatform();
                    if (!TextUtils.equals(platform, mContext.getPackageName())) {
                        script = readInfrasAsset(platform);
                    }
                }
                RuntimeLogManager.getDefault()
                        .logJsThreadTaskEnd(mContext.getPackageName(),
                                RuntimeLogManager.KEY_INFRAS_JS_LOAD);
                mJsContext.getV8().executeVoidScript(script, "infras.js", 0);
            }
            mApplicationState = STATE_RUNTIME_INITED;
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        }
    }

    private String readInfrasAsset() {
        return JavascriptReader.get()
                .read(
                        new AssetSource(mContext, "js/infras.js") {
                            @Override
                            public InputStream open() throws IOException {
                                try {
                                    return super.open();
                                } catch (IOException e) {
                                    String host = mContext.getPackageName();
                                    String platform = ResourceConfig.getInstance().getPlatform();
                                    RuntimeLogManager.getDefault()
                                            .logResourceNotFound(host, platform, "js/infras.js", e);

                                    throw e;
                                }
                            }
                        });
    }

    private String readInfrasAsset(String platform) {
        Log.i(TAG, "try to load infras.js from " + platform);
        try {
            Context platformContext = mContext.createPackageContext(platform, 0);
            return JavascriptReader.get().read(new AssetSource(platformContext, "js/infras.js"));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "failed to createPackageContext for " + platform, e);
        }
        return null;
    }

    public Context getPlatformContext(Context context) {
        if (null == context) {
            return null;
        }
        String platform = ResourceConfig.getInstance().getPlatform();
        try {
            Context platformContext = context.createPackageContext(platform, 0);
            return platformContext;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "failed to getPlatformContext for " + platform, e);
        }
        return null;
    }

    public void postNotifyConfigurationChanged(Page page, String type) {
        Object[] params = new Object[] {page, type};
        mHandler.obtainMessage(H.MSG_NOTIFY_CONFIGURATION_CHANGED, params).sendToTarget();
    }

    private void notifyConfigurationChanged(Page page, String type) {
        if (page == null) {
            return;
        }
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        int pageId = page.pageId;
        args.push(pageId);
        V8Object options = new V8Object(v8);
        options.add("type", type);
        args.push(options);
        try {
            v8.executeVoidFunction("notifyConfigurationChanged", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args, options);
        }
    }

    public void postUpdateLocale(Locale locale, Map<String, JSONObject> resources) {
        Object[] params = new Object[] {locale, resources};
        mHandler.obtainMessage(H.MSG_UPDATE_LOCALE, params).sendToTarget();
    }

    private void updateLocale(Locale newLocale, Map<String, JSONObject> resourcesJson) {
        if (newLocale == null || resourcesJson == null) {
            return;
        }
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        // locale
        V8Object locale = new V8Object(v8);
        locale.add("language", newLocale.getLanguage());
        locale.add("countryOrRegion", newLocale.getCountry());
        args.push(locale);
        // resources array
        V8Array resources = new V8Array(v8);
        for (JSONObject resJson : resourcesJson.values()) {
            JavaSerializeObject object = new JavaSerializeObject(resJson);
            V8Object res = V8ObjectHelper.toV8Object(v8, object.toMap());
            resources.push(res);
            JsUtils.release(res);
        }
        args.push(resources);
        try {
            v8.executeVoidFunction("changeAppLocale", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args, locale, resources);
        }
    }

    private void registerManifest() {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        try {
            args.push(mAppInfo.getMetaInfo());
            v8.executeVoidFunction("registerManifest", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args);
        }
    }

    public void postRegisterBundleChunks(String content) {
        Object[] params = new Object[] {content};
        Message.obtain(mHandler, H.MSG_REGISTER_BUNDLE_CHUNKS, params).sendToTarget();
    }

    private void registerBundleChunks(String content) {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        try {
            args.push(content);
            v8.executeVoidFunction("registerBundleChunks", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args);
        }
    }

    public void postCreateApplication(String jsContent, String css, HybridRequest request) {
        Object[] params = new Object[] {jsContent, css, request};
        Message.obtain(mHandler, H.MSG_CREATE_APPLICATION, params).sendToTarget();
    }

    public void postInitInspectorJsContext() {
        Message.obtain(mHandler, H.MSG_INIT_INSPECTOR_JSCONTEXT).sendToTarget();
    }

    private void createApplication(String js, String css, HybridRequest request) {
        registerManifest();
        V8 v8 = mJsContext.getV8();
        v8.executeVoidFunction("locateDsl", null);

        V8Array args1 = new V8Array(v8);
        try {
            args1.push(mAppId);
            args1.push(js);
            args1.push(css);
            v8.executeVoidFunction("createApplication", args1);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args1);
        }

        RuntimeLogManager.getDefault().logAppLoadEnd(mAppInfo.getPackage());
    }

    public void postOnRequestApplication() {
        Message.obtain(mHandler, H.MSG_ON_REQUEST_APPLICATION).sendToTarget();
    }

    private void onRequestApplication() {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(mAppId);
        try {
            v8.executeVoidFunction("onRequestApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
        }
    }

    public void postOnShowApplication() {
        Message.obtain(mHandler, H.MSG_ON_SHOW_APPLICATION).sendToTarget();
    }

    private void onShowApplication() {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(mAppId);
        try {
            v8.executeVoidFunction("onShowApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
        }
    }

    public void postOnHideApplication() {
        Message.obtain(mHandler, H.MSG_ON_HIDE_APPLICATION).sendToTarget();
    }

    private void onHideApplication() {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(mAppId);
        try {
            v8.executeVoidFunction("onHideApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
            return;
        } finally {
            JsUtils.release(args);
        }
    }

    public void postRecreatePage(Page page) {
        mHandler.obtainMessage(H.MSG_RECREATE_PAGE, page).sendToTarget();
    }

    public void postRefreshPage(Page page) {
        Message.obtain(mHandler, H.MSG_REFRESH_PAGE, page).sendToTarget();
    }

    public void postOnMenuButtonPress(Page page, HybridView.OnKeyUpListener onKeyUpIsConsumption) {
        Object[] params = new Object[] {page, onKeyUpIsConsumption};
        mHandler.obtainMessage(H.MSG_ON_MENU_BUTTON_PRESS, params).sendToTarget();
    }

    public void postPageNotFound(Page page) {
        Message.obtain(mHandler, H.MSG_PAGE_NOT_FOUND, page).sendToTarget();
    }

    public void postBackPress(Page page) {
        mHandler.obtainMessage(H.MSG_BACK_PRESS, page).sendToTarget();
    }

    void backPress(Page page) {
        boolean consumed = false;
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            V8 v8 = mJsContext.getV8();
            try {
                consumed = v8.executeBooleanScript("backPressPage(" + page.pageId + ");");
            } catch (V8RuntimeException ex) {
                processV8Exception(ex);
                return;
            }
        }
        if (!consumed && null != mMainHandler) {
            mMainHandler.sendEmptyMessage(RootView.MSG_BACK_PRESS);
        }
    }

    void menuButtonPressPage(Page page, HybridView.OnKeyUpListener onKeyUpIsConsumption) {
        boolean consumed = false;
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            V8 v8 = mJsContext.getV8();
            try {
                consumed = v8.executeBooleanScript("menuButtonPressPage(" + page.pageId + ");");
            } catch (V8RuntimeException ex) {
                onKeyUpIsConsumption.consume(false);
                processV8Exception(ex);
                return;
            }
        }
        onKeyUpIsConsumption.consume(consumed);
    }

    private void firePageKeyEvent(JsEventCallbackData data) {
        Page page = mPageManager.getPageById(data.pageId);
        if (page == null) {
            return;
        }

        boolean consumed = false;
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(data.pageId);
        V8Object paramsObj = JsUtils.mapToV8Object(v8, data.params);
        args.push(paramsObj);
        try {
            consumed = v8.executeBooleanFunction("keyPressPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, paramsObj);
        }

        if (null != mMainHandler) {
            Object hashCode = data.params.get(KeyEventDelegate.KEY_HASHCODE);
            if (hashCode instanceof Integer) {
                KeyEventManager.getInstance()
                        .injectKeyEvent(consumed, mRootView, (Integer) hashCode);
            }
        }
    }

    public void postMenuPress(Page page) {
        mHandler.obtainMessage(H.MSG_MENU_PRESS, page).sendToTarget();
    }

    private void onMenuPress(Page page) {
        boolean consumed = false;
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            V8 v8 = mJsContext.getV8();
            try {
                consumed = v8.executeBooleanScript("menuPressPage(" + page.pageId + ");");
            } catch (V8RuntimeException ex) {
                processV8Exception(ex);
                return;
            }
        }

        if (!consumed) {
            mMainHandler.obtainMessage(RootView.MSG_MENU_PRESS, page).sendToTarget();
        }
    }

    public void postOrientationChange(Page page, ScreenOrientation screenOrientation) {
        mHandler
                .obtainMessage(H.MSG_ORIENTATION_CHANGE, new Pair<>(page, screenOrientation))
                .sendToTarget();
    }

    private void onOrientationChange(Page page, ScreenOrientation screenOrientation) {
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            V8 v8 = mJsContext.getV8();
            V8Array array = new V8Array(v8);
            array.push(page.pageId);
            V8Object object = new V8Object(v8);
            object.add("orientation", screenOrientation.getOrientation());
            object.add("angel", screenOrientation.getAngel());
            array.push(object);
            try {
                v8.executeVoidFunction("orientationChangePage", array);
            } catch (V8RuntimeException ex) {
                processV8Exception(ex);
                return;
            } finally {
                JsUtils.release(array, object);
            }
        }
    }

    public void postChangeVisiblePage(Page page, boolean visible) {
        if (page != null) {
            // 当thread被block的时候,activity已经stop,不应该发送visible=true事件,
            // 仅发送visible=false事件,在activity变为start时会重新发送visible=true事件
            if (visible && page.getState() == Page.STATE_INITIALIZED && !mBlocked) {
                if (page.shouldReload() && page.getRequest() != null) {
                    if (MultiWindowManager.shouldApplyMultiWindowMode(mContext) && page.getIsMultiWindowLeftPage()) {
                        RouterUtils.replaceLeftPage(mPageManager, page.getRequest());
                    } else {
                        RouterUtils.replace(mPageManager, page.getRequest());
                    }
                    return;
                }
                requestFocus();
                page.setState(Page.STATE_VISIBLE);
                postExecuteScript(
                        "changeVisiblePage(" + page.pageId + ", " + JsUtils.toJsBoolean(visible)
                                + ");");
                Log.d(TAG, "show page: " + page.getName());

                String session = System.getProperty(RuntimeActivity.PROP_SESSION);
                if (!TextUtils.equals(session, mSessionLastAppShow)) {
                    mSessionLastAppShow = session;
                    RuntimeLogManager.getDefault()
                            .logAppShow(mAppInfo.getPackage(), mAppInfo.getVersionCode());
                }

                Page referrer = page.getReferrer();
                String referrerName = referrer == null ? null : referrer.getName();
                RuntimeLogManager.getDefault()
                        .logPageViewStart(mAppInfo.getPackage(), page.getName(), referrerName);
            } else if (!visible && page.getState() == Page.STATE_VISIBLE) {
                page.setState(Page.STATE_INITIALIZED);
                postExecuteScript(
                        "changeVisiblePage(" + page.pageId + ", " + JsUtils.toJsBoolean(visible)
                                + ");");
                Log.d(TAG, "hide page: " + page.getName());
                RuntimeLogManager.getDefault()
                        .logPageViewEnd(mAppInfo.getPackage(), page.getName());
            } else {
                Log.i(
                        TAG,
                        "Skip page visible change: page="
                                + page
                                + ", visible="
                                + visible
                                + ", mBlocked="
                                + mBlocked);
            }
        }
    }

    private void requestFocus() {
        RootView rootView = mRootView;
        if (!BuildPlatform.isTV() || rootView == null || rootView.hasFocus()) {
            return;
        }
        rootView.post(rootView::requestFocus);
    }

    public void postInitializePage(int pageId) {
        Page page = mPageManager.getPageById(pageId);
        if (page != null) {
            page.setState(Page.STATE_INITIALIZED);
            Message.obtain(mMainHandler, RootView.MSG_PAGE_INITIALIZED, page).sendToTarget();
        } else {
            Log.w(TAG, "postInitializePage: page is null");
        }
    }

    public void postDestroyPage(Page page) {
        if (page.getState() > Page.STATE_NONE) {
            mHandler.obtainMessage(H.MSG_DESTROY_PAGE, 0, 0, page).sendToTarget();
            page.setState(Page.STATE_NONE);
        } else {
            Log.d(TAG, "skip page destroy: " + page.toString());
        }
    }

    public void loadPage(final Page page) {
        RuntimeLogManager.getDefault().logPageLoadStart(mAppInfo.getPackage(), page.getName());
        mMainHandler.obtainMessage(RootView.MSG_LOAD_PAGE_JS_START, page).sendToTarget();
        Executors.io()
                .execute(
                        new AbsTask<String[]>() {
                            @Override
                            protected String[] doInBackground() {
                                mJsChunksManager.registerPageChunks(page);
                                String pageJs = AppResourcesLoader.getPageJs(mContext, mAppInfo.getPackage(), page);
                                String pageCss = AppResourcesLoader.getPageCss(mContext, mAppInfo.getPackage(), page);
                                parseStyleSheets(pageCss, page);
                                return new String[]{pageJs, pageCss};
                            }

                            @Override
                            protected void onPostExecute(String[] contents) {
                                RuntimeLogManager.getDefault()
                                        .logPageLoadEnd(mAppInfo.getPackage(), page.getName());
                                int result =
                                        TextUtils.isEmpty(contents[0])
                                                ? Page.JS_LOAD_RESULT_FAIL
                                                : Page.JS_LOAD_RESULT_SUCC;
                                page.setLoadJsResult(result);
                                mMainHandler.obtainMessage(RootView.MSG_LOAD_PAGE_JS_FINISH, page)
                                        .sendToTarget();

                                RoutableInfo routableInfo = page.getRoutableInfo();
                                final String jsUri = routableInfo.getUri();
                                postCreatePage(page, contents[0], jsUri, contents[1]);
                                Log.d(TAG, "loadPage onPostExecute uri=" + jsUri + " result="
                                        + result);
                            }
                        });
    }

    private void parseStyleSheets(String css, Page page) {
        if (TextUtils.isEmpty(css)) {
            return;
        }
        Executors.io()
                .execute(
                        new AbsTask<Void>() {
                            @Override
                            protected Void doInBackground() {
                                RuntimeLogManager.getDefault()
                                        .logAsyncThreadTaskStart(mAppInfo.getPackage(),
                                                "parseStyleSheets");
                                try {
                                    org.hapjs.common.json.JSONObject styles =
                                            new org.hapjs.common.json.JSONObject(css);
                                    org.hapjs.common.json.JSONArray styleList =
                                            styles.getJSONArray("list");
                                    int n = styleList.length();
                                    for (int i = 0; i < n; i++) {
                                        org.hapjs.common.json.JSONObject styleSheetPlain =
                                                styleList.getJSONObject(i);
                                        CSSStyleSheet styleSheet =
                                                CSSParser.parseCSSStyleSheet(styleSheetPlain);

                                        // 注册样式表
                                        RenderActionDocument document =
                                                mRenderActionManager
                                                        .getOrCreateDocument(page.getPageId());
                                        document.registerStyleSheet(styleSheet.getStyleObjectId(),
                                                styleSheet);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "parse css failed: " + e.getMessage());
                                }
                                RuntimeLogManager.getDefault()
                                        .logAsyncThreadTaskEnd(mAppInfo.getPackage(),
                                                "parseStyleSheets");
                                return null;
                            }
                        });
    }

    private void postCreatePage(Page page, String js, String uri, String css) {
        Object[] params = new Object[] {page, js, uri, css};
        Message.obtain(mHandler, H.MSG_CREATE_PAGE, params).sendToTarget();
        page.setState(Page.STATE_CREATED);
    }

    private void createPage(Page page, String js, String uri, String css) {
        preCreateSkeleton(page);
        preCreateBody(page);
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(page.pageId);
        args.push(mAppId);
        args.push(js);
        V8Object paramsObj = JsUtils.mapToV8Object(v8, page.params);
        args.push(paramsObj);
        V8Object intentObj = JsUtils.mapToV8Object(v8, page.intent);
        args.push(intentObj);
        V8Object metaObj = JsUtils.mapToV8Object(v8, page.meta);
        args.push(metaObj);
        args.push(css);
        try {
            v8.executeVoidFunction("createPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, paramsObj, intentObj, metaObj);
        }
        Message.obtain(mMainHandler, MSG_APP_LOAD_END).sendToTarget();
    }

    private void preCreateSkeleton(Page page) {
        if (mAppInfo != null && mContext != null && page != null) {
            SkeletonProvider skeletonProvider =
                    ProviderManager.getDefault().getProvider(SkeletonProvider.NAME);
            if (skeletonProvider == null) {
                // If you have not added a customized provider, use the default (enable function))
                skeletonProvider = new DefaultSkeletonProviderImpl(mContext);
            }
            if (skeletonProvider.isSkeletonEnable(mAppInfo.getPackage())) {
                Executors.io()
                        .execute(
                                new AbsTask<JSONObject>() {
                                    @Override
                                    protected JSONObject doInBackground() {
                                        RpkSource skeletonConfigSource =
                                                new RpkSource(mContext, mAppInfo.getPackage(),
                                                        "skeleton/config.json");
                                        String skeletonConfig =
                                                TextReader.get().read(skeletonConfigSource);
                                        JSONObject parseResult = null;
                                        if (!TextUtils.isEmpty(skeletonConfig)) {
                                            String skFileName =
                                                    SkeletonConfigParser.getSkeletonFileName(page,
                                                            skeletonConfig);
                                            Log.i(
                                                    TAG,
                                                    "LOG_SKELETON parse skeleton config, current page is "
                                                            + page.getName());
                                            if (!TextUtils.isEmpty(skFileName)) {
                                                // read and parse sk file
                                                String skFilePathName =
                                                        "skeleton/page/" + skFileName;
                                                RpkSource skFileSource =
                                                        new RpkSource(mContext,
                                                                mAppInfo.getPackage(),
                                                                skFilePathName);
                                                InputStream inputStream = null;
                                                try {
                                                    Log.i(TAG,
                                                            "LOG_SKELETON parse sk file, path = "
                                                                    + skFilePathName);
                                                    inputStream = skFileSource.open();
                                                    parseResult =
                                                            SkeletonDSLParser.parse(inputStream);
                                                } catch (Exception e) {
                                                    Log.e(TAG, "LOG_SKELETON parse sk file fail, ",
                                                            e);
                                                } finally {
                                                    if (inputStream != null) {
                                                        try {
                                                            inputStream.close();
                                                        } catch (IOException ioe) {
                                                            Log.e(TAG,
                                                                    "LOG_SKELETON close sk inputStream fail, ",
                                                                    ioe);
                                                        }
                                                    }
                                                }
                                            } else {
                                                Log.i(TAG,
                                                        "LOG_SKELETON no matching sk file for current page");
                                            }
                                        } else {
                                            Log.i(TAG,
                                                    "LOG_SKELETON skeleton config file is empty");
                                        }
                                        return parseResult;
                                    }

                                    @Override
                                    protected void onPostExecute(JSONObject parseResult) {
                                        if (mRenderActionManager != null) {
                                            mRenderActionManager.showSkeleton(mAppInfo.getPackage(),
                                                    parseResult);
                                        }
                                    }
                                });
            } else {
                Log.i(TAG, "LOG_SKELETON prevent render skeleton because not enable");
            }
        }
    }

    // Debugger: When paused at any breakpoint, v8.terminateExecution will terminate javascript
    // execution and cause JsThread throwing exception which is no need for processing.
    private boolean isTerminateExecution(String exceptionMessage) {
        return mIsTerminateExecution && "null".equals(exceptionMessage);
    }

    void recreatePage(Page page) {
        // page VDoc is destroyed, apply the cached render actions will throw exception
        mMainHandler.obtainMessage(RootView.MSG_PAGE_CLEAR_CACHE, page).sendToTarget();
        preCreateBody(page);
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(page.pageId);
        String uri = page.getRoutableInfo().getUri();
        try {
            v8.executeVoidFunction("recreatePage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
    }

    private void refreshPage(Page page) {
        preCreateBody(page);
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(page.pageId);
        V8Object paramsObj = JsUtils.mapToV8Object(v8, page.params);
        args.push(paramsObj);
        V8Object intentObj = JsUtils.mapToV8Object(v8, page.intent);
        args.push(intentObj);
        try {
            v8.executeVoidFunction("refreshPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, paramsObj, intentObj);
        }
    }

    private void notifyPageNotFound(Page page) {
        preCreateBody(page);
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(mAppId);
        V8Object uriObj =
                JsUtils.mapToV8Object(v8, Collections.singletonMap("uri", page.getTargetPageUri()));
        args.push(uriObj);
        try {
            v8.executeVoidFunction("notifyPageNotFound", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, uriObj);
        }
    }

    private void preCreateBody(Page page) {
        VDomChangeAction action = new VDomChangeAction();
        action.action = VDomChangeAction.ACTION_PRE_CREATE_BODY;
        action.pageId = page.pageId;
        RenderActionPackage renderActionPackage =
                new RenderActionPackage(page.pageId, RenderActionPackage.TYPE_PRE_CREATE_BODY);
        renderActionPackage.renderActionList.add(action);
        mJsBridge.sendRenderActions(renderActionPackage);
    }

    void destroyPage(Page page) {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(page.pageId);
        try {
            v8.executeVoidFunction("destroyPage", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
        mJsTimer.clearTimers(page.pageId);
        mRenderActionManager.destroyPage(page.pageId);
    }

    private void postDestroyApplication() {
        Message.obtain(mHandler, H.MSG_DESTROY_APPLICATION).sendToTarget();
    }

    private void destroyApplication() {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        try {
            args.push(mAppId);
            v8.executeVoidFunction("destroyApplication", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            mApplicationState = STATE_DESTROYED;
            JsUtils.release(args);
        }
    }

    public void postFoldCard(int id, boolean fold) {
        Object[] params = new Object[] {id, fold};
        mHandler.obtainMessage(H.MSG_FOLD_CARD, params).sendToTarget();
    }

    public void postFireKeyEvent(JsEventCallbackData data) {
        Message.obtain(mHandler, JsThread.H.MSG_FIRE_KEY_EVENT, data).sendToTarget();
    }

    public void postFireEvent(JsEventCallbackData data) {
        Message.obtain(mHandler, JsThread.H.MSG_FIRE_EVENT, data).sendToTarget();
    }

    public void postFireEvent(
            final int pageId,
            final List<JsEventCallbackData> datas,
            final RenderEventCallback.EventPostListener listener) {
        post(
                new Runnable() {
                    @Override
                    public void run() {
                        fireEvent(pageId, datas, listener);
                    }
                });
    }

    private void post(Runnable runnable) {
        mHandler.postAtFrontOfQueue(runnable);
    }

    public void postInJsThread(Runnable runnable) {
        mHandler.post(runnable);
    }

    private void fireEvent(
            int pageId, List<JsEventCallbackData> datas,
            RenderEventCallback.EventPostListener listener) {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(pageId); // pageId

        V8Array events = new V8Array(v8);
        List<V8Value> releaseObj = new ArrayList<>();
        for (JsEventCallbackData data : datas) {
            V8Object event = new V8Object(v8);
            event.add("action", 1); // action

            V8Array eventArg = new V8Array(v8);
            eventArg.push(data.elementId); // ref
            eventArg.push(data.eventName); // eventType
            V8Object paramsObj = JsUtils.mapToV8Object(v8, data.params);
            eventArg.push(paramsObj); // params
            V8Object attributesObj = JsUtils.mapToV8Object(v8, data.attributes);
            eventArg.push(attributesObj); // attributes

            event.add("args", eventArg);
            events.push(event);

            releaseObj.add(event);
            releaseObj.add(eventArg);
            releaseObj.add(paramsObj);
            releaseObj.add(attributesObj);
        }
        args.push(events);
        releaseObj.add(events);

        try {
            v8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            if (listener != null) {
                listener.finish();
            }
            int size = releaseObj.size();
            V8Value[] temp = new V8Value[size];
            for (int i = 0; i < size; i++) {
                temp[i] = releaseObj.get(i);
            }

            JsUtils.release(args, temp);
        }
    }

    private void fireEvent(JsEventCallbackData data) {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(data.pageId);

        V8Array events = new V8Array(v8);
        V8Object event = new V8Object(v8);

        event.add("action", 1);

        V8Array eventArg = new V8Array(v8);
        eventArg.push(data.elementId);
        eventArg.push(data.eventName);
        V8Object paramsObj = JsUtils.mapToV8Object(v8, data.params);
        eventArg.push(paramsObj);
        V8Object attributesObj = JsUtils.mapToV8Object(v8, data.attributes);
        eventArg.push(attributesObj);
        event.add("args", eventArg);
        events.push(event);
        args.push(events);

        try {
            v8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, events, event, eventArg, paramsObj, attributesObj);
        }
    }

    private void fireKeyEvent(JsEventCallbackData data) {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(data.pageId);
        V8Array events = new V8Array(v8);
        V8Object event = new V8Object(v8);
        event.add("action", 1);
        V8Array eventArg = new V8Array(v8);
        eventArg.push(data.elementId);
        eventArg.push(data.eventName);
        V8Object paramsObj = JsUtils.mapToV8Object(v8, data.params);
        eventArg.push(paramsObj);
        event.add("args", eventArg);
        events.push(event);
        args.push(events);

        try {
            v8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, events, event, eventArg, paramsObj);
        }
    }

    private void registerKeyEventCallback() {
        mJsContext.getV8().registerJavaMethod(keyEventCallback, "callKeyEvent");
    }

    public void postFireCallback(JsMethodCallbackData data) {
        Message.obtain(mHandler, H.MSG_FIRE_CALLBACK, data).sendToTarget();
    }

    void fireCallback(JsMethodCallbackData data) {
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        args.push(data.pageId);

        V8Array methodArray = new V8Array(v8);
        V8Object methodObject = new V8Object(v8);

        methodObject.add("action", 2);

        V8Array callbackArgs = new V8Array(v8);
        callbackArgs.push(data.callbackId);
        V8Array callbackArgsParams = new V8Array(v8);
        for (Object obj : data.params) {
            JsUtils.push(callbackArgsParams, obj);
        }
        callbackArgs.push(callbackArgsParams);

        methodObject.add("args", callbackArgs);
        methodArray.push(methodObject);
        args.push(methodArray);

        try {
            v8.executeVoidFunction("execJSBatch", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args, methodArray, methodObject, callbackArgs, callbackArgsParams);
        }
    }

    private void onFoldCard(int s, boolean f) {
        V8Array args = new V8Array(mJsContext.getV8());
        try {
            args.push(s);
            args.push(f);
            mJsContext.getV8().executeVoidFunction("foldCard", args);
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        } finally {
            JsUtils.release(args);
        }
    }

    public void block(long delay) {
        mHandler.sendEmptyMessageDelayed(H.MSG_BLOCK, delay);
        mBlocked = true;
    }

    private void doBlock() {
        mBlocker.close();
        mBlocker.block();
    }

    public void unblock() {
        mHandler.removeMessages(H.MSG_BLOCK);
        mBlocker.open();
        mBlocked = false;
    }

    public boolean isBlocked() {
        return mBlocked;
    }

    public void shutdown(long delay) {
        Log.d(TAG, "shutdown: " + this);
        unblock();
        if (mApplicationState == STATE_RUNTIME_INITED) {
            mApplicationState = STATE_DESTROYING;
            Page page = null;
            if (null != mPageManager) {
                page = mPageManager.getCurrPage();
            }
            if (page != null) {
                postChangeVisiblePage(page, false);
                postDestroyPage(page);
            }
            postDestroyApplication();
        }
        mHandler.sendEmptyMessageDelayed(H.MSG_SHUTDOWN, delay);
    }

    private void doShutdown() {
        InspectorManager.getInspector().onJsContextDispose(mJsContext.getV8());
        if (mCallback != null) {
            mCallback.onRuntimeDestroy();
        }
        mRenderActionManager.release();
        JsUtils.release(mJsTimer, mJsBridgeHistory, mProfiler);
        mExtensionManager.dispose();
        mJsContext.dispose();
        quit();
        Log.d(TAG, "shutdown finish: " + this);
    }

    public void processV8Exception(Exception ex) {
        if (isTerminateExecution(ex.getMessage())) {
            mIsTerminateExecution = false;
        } else {
            String msg = LogUtils.getStackTrace(ex);
            Log.e(TAG, msg);
            InspectorManager.getInspector().onConsoleMessage(InspectorProvider.CONSOLE_ERROR, msg);
            Message.obtain(mMainHandler, RootView.MSG_USER_EXCEPTION, ex).sendToTarget();
        }
        notifyAppError(ex);
    }

    private void notifyAppError(Exception ex) {
        String message = ex.getMessage();
        if (NotifyAppErrorHelper.isExceptionFromOnError(message)) {
            Log.i(TAG, "Exception from onError()");
            return;
        }
        String stack = LogUtils.getStackTrace(ex);
        String script = NotifyAppErrorHelper.generateScript(mAppId, message, stack);
        postExecuteScript(script);
    }

    public AppInfo getAppInfo() {
        return mAppInfo;
    }

    public synchronized JsChunksManager getJsChunksManager() {
        if (mJsChunksManager == null) {
            mJsChunksManager = new JsChunksManager(this);
        }
        return mJsChunksManager;
    }

    public void postPageReachTop(Page page) {
        mHandler.obtainMessage(H.MSG_PAGE_REACH_TOP, page).sendToTarget();
    }

    public void postPageReachBottom(Page page) {
        mHandler.obtainMessage(H.MSG_PAGE_REACH_BOTTOM, page).sendToTarget();
    }

    public void postPageScroll(Page page, int scrollTop) {
        Object[] args = new Object[] {page, scrollTop};
        mHandler.obtainMessage(H.MSG_PAGE_SCROLL, args).sendToTarget();
    }

    private void onPageReachTop(Page page) {
        if (page == null) {
            return;
        }
        V8 v8 = mJsContext.getV8();
        try {
            v8.executeVoidScript("reachPageTop(" + page.pageId + ");");
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        }
    }

    private void onPageReachBottom(Page page) {
        if (page == null) {
            return;
        }
        V8 v8 = mJsContext.getV8();
        try {
            v8.executeVoidScript("reachPageBottom(" + page.pageId + ");");
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        }
    }

    private void onPageScroll(Page page, int scrollTop) {
        if (page == null) {
            return;
        }
        V8 v8 = mJsContext.getV8();
        V8Array args = new V8Array(v8);
        int pageId = page.pageId;
        args.push(pageId);
        V8Object options = new V8Object(v8);
        options.add("scrollTop", scrollTop);
        args.push(options);
        try {
            v8.executeVoidFunction("pageScroll", args);
        } catch (V8RuntimeException e) {
            processV8Exception(e);
        } finally {
            JsUtils.release(args, options);
        }
    }

    public interface LifecycleCallback {
        /**
         * Run in js core thread
         */
        void onRuntimeCreate();

        /**
         * Run in js core thread
         */
        void onRuntimeDestroy();
    }

    public static class JsEventCallbackData {

        public final int pageId;
        public final int elementId;
        public final String eventName;
        public final Map<String, Object> params;
        public final Map<String, Object> attributes;

        public JsEventCallbackData(
                int pageId,
                int elementId,
                String eventName,
                Map<String, Object> params,
                Map<String, Object> attributes) {
            this.pageId = pageId;
            this.elementId = elementId;
            this.eventName = eventName;
            this.params = params;
            this.attributes = attributes;
        }
    }

    public static class JsMethodCallbackData {

        public final int pageId;
        public final String callbackId;
        public final Object[] params;

        public JsMethodCallbackData(int pageId, String callbackId, Object... params) {
            this.pageId = pageId;
            this.callbackId = callbackId;
            this.params = params;
        }
    }

    public class H extends Handler {

        private static final int MSG_INIT = 1;
        private static final int MSG_ATTACH = 2;
        private static final int MSG_EXECUTE_SCRIPT = 3;
        private static final int MSG_CREATE_APPLICATION = 4;
        private static final int MSG_DESTROY_APPLICATION = 5;
        private static final int MSG_CREATE_PAGE = 6;
        private static final int MSG_RECREATE_PAGE = 7;
        private static final int MSG_DESTROY_PAGE = 8;
        private static final int MSG_FIRE_EVENT = 9;
        private static final int MSG_BACK_PRESS = 10;
        private static final int MSG_BLOCK = 11;
        private static final int MSG_SHUTDOWN = 12;
        private static final int MSG_MENU_PRESS = 13;
        private static final int MSG_FIRE_CALLBACK = 14;
        private static final int MSG_ORIENTATION_CHANGE = 15;
        private static final int MSG_EXECUTE_FUNCTION = 16;
        private static final int MSG_TERMINATE_EXECUTION = 17;
        private static final int MSG_FOLD_CARD = 18;
        private static final int MSG_INIT_INSPECTOR_JSCONTEXT = 19;
        private static final int MSG_REFRESH_PAGE = 20;
        private static final int MSG_UPDATE_LOCALE = 21;
        private static final int MSG_NOTIFY_CONFIGURATION_CHANGED = 22;
        private static final int MSG_PAGE_NOT_FOUND = 23;
        private static final int MSG_ON_REQUEST_APPLICATION = 24;
        private static final int MSG_ON_SHOW_APPLICATION = 25;
        private static final int MSG_ON_HIDE_APPLICATION = 26;
        private static final int MSG_REGISTER_BUNDLE_CHUNKS = 27;
        private static final int MSG_PAGE_SCROLL = 28;
        private static final int MSG_PAGE_REACH_TOP = 29;
        private static final int MSG_PAGE_REACH_BOTTOM = 30;
        private static final int MSG_FIRE_KEY_EVENT = 31;
        private static final int MSG_ON_MENU_BUTTON_PRESS = 32;

        private final List<Integer> mApplicationMessages =
                Arrays.asList(
                        MSG_CREATE_APPLICATION,
                        MSG_DESTROY_APPLICATION,
                        MSG_CREATE_PAGE,
                        MSG_REFRESH_PAGE,
                        MSG_PAGE_NOT_FOUND,
                        MSG_RECREATE_PAGE,
                        MSG_DESTROY_PAGE,
                        MSG_FIRE_EVENT,
                        MSG_FIRE_KEY_EVENT,
                        MSG_BACK_PRESS,
                        MSG_MENU_PRESS,
                        MSG_FIRE_CALLBACK,
                        MSG_ORIENTATION_CHANGE);

        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if ((mApplicationState == STATE_NONE || mApplicationState == STATE_DESTROYED)
                    && Collections.binarySearch(mApplicationMessages, msg.what) >= 0) {
                // not handle application msg when runtime is not initialized or is destroying
                return;
            }
            switch (msg.what) {
                case MSG_INIT:
                    RuntimeLogManager.getDefault()
                            .logJsThreadTaskStart(mContext.getPackageName(),
                                    RuntimeLogManager.KEY_JS_ENV_INIT);
                    onInit();
                    RuntimeLogManager.getDefault()
                            .logJsThreadTaskEnd(mContext.getPackageName(),
                                    RuntimeLogManager.KEY_JS_ENV_INIT);
                    break;
                case MSG_ATTACH:
                    onAttach();
                    break;
                case MSG_EXECUTE_SCRIPT: {
                    if (DebugUtils.DBG) {
                        DebugUtils.startRecord("JsThreadExecuteScript");
                    }
                    try {
                        mJsContext.getV8().executeVoidScript((String) msg.obj);
                    } catch (V8RuntimeException ex) {
                        processV8Exception(ex);
                    }
                    if (DebugUtils.DBG) {
                        DebugUtils.endRecord("JsThreadExecuteScript");
                    }
                    break;
                }
                case MSG_EXECUTE_FUNCTION: {
                    if (DebugUtils.DBG) {
                        DebugUtils.startRecord("JsThreadExecuteFunction");
                    }
                    V8 v8 = mJsContext.getV8();
                    Pair<String, Object[]> pair = (Pair<String, Object[]>) msg.obj;
                    V8Array params =
                            pair.second == null
                                    ? new V8Array(v8)
                                    : V8ObjectHelper.toV8Array(v8, Arrays.asList(pair.second));
                    try {
                        mJsContext.getV8().executeVoidFunction(pair.first, params);
                    } catch (V8RuntimeException ex) {
                        processV8Exception(ex);
                    } finally {
                        JsUtils.release(params);
                    }
                    if (DebugUtils.DBG) {
                        DebugUtils.endRecord("JsThreadExecuteFunction");
                    }
                    break;
                }
                case MSG_CREATE_APPLICATION: {
                    if (DebugUtils.DBG) {
                        DebugUtils.startRecord("JsThreadExecuteApp");
                    }
                    Object[] params = (Object[]) msg.obj;
                    createApplication((String) params[0], (String) params[1],
                            (HybridRequest) params[2]);
                    if (DebugUtils.DBG) {
                        DebugUtils.endRecord("JsThreadExecuteApp");
                    }
                    break;
                }
                case MSG_DESTROY_APPLICATION: {
                    destroyApplication();
                    break;
                }
                case MSG_CREATE_PAGE: {
                    Object[] params = (Object[]) msg.obj;
                    createPage(
                            (Page) params[0], (String) params[1], (String) params[2],
                            (String) params[3]);
                    break;
                }
                case MSG_RECREATE_PAGE: {
                    recreatePage((Page) msg.obj);
                    break;
                }
                case MSG_PAGE_NOT_FOUND: {
                    notifyPageNotFound((Page) msg.obj);
                    break;
                }
                case MSG_REFRESH_PAGE: {
                    refreshPage((Page) msg.obj);
                    break;
                }
                case MSG_DESTROY_PAGE:
                    destroyPage((Page) msg.obj);
                    break;
                case MSG_FIRE_EVENT: {
                    if (DebugUtils.DBG) {
                        DebugUtils.startRecord("JsThreadFireEvent");
                    }
                    fireEvent((JsEventCallbackData) msg.obj);
                    if (DebugUtils.DBG) {
                        DebugUtils.endRecord("JsThreadFireEvent");
                    }
                    break;
                }
                case MSG_FIRE_KEY_EVENT: {
                    if (DebugUtils.DBG) {
                        DebugUtils.startRecord("JsThreadFireKeyEvent");
                    }
                    JsEventCallbackData data = ((JsEventCallbackData) msg.obj);
                    if (Attributes.Event.KEY_EVENT.equals(data.eventName)) {
                        fireKeyEvent(data);
                    } else if (Attributes.Event.KEY_EVENT_PAGE.equals(data.eventName)) {
                        firePageKeyEvent(data);
                    }
                    if (DebugUtils.DBG) {
                        DebugUtils.endRecord("JsThreadFireKeyEvent");
                    }
                    break;
                }
                case MSG_BACK_PRESS: {
                    backPress((Page) msg.obj);
                    break;
                }
                case MSG_ON_MENU_BUTTON_PRESS: {
                    Object[] params = (Object[]) msg.obj;

                    menuButtonPressPage((Page) params[0], (HybridView.OnKeyUpListener) params[1]);
                    break;
                }
                case MSG_FIRE_CALLBACK: {
                    if (DebugUtils.DBG) {
                        DebugUtils.startRecord("JsThreadFireCallback");
                    }
                    fireCallback((JsMethodCallbackData) msg.obj);
                    if (DebugUtils.DBG) {
                        DebugUtils.endRecord("JsThreadFireCallback");
                    }
                    break;
                }
                case MSG_BLOCK: {
                    doBlock();
                    break;
                }
                case MSG_SHUTDOWN: {
                    doShutdown();
                    break;
                }
                case MSG_MENU_PRESS: {
                    onMenuPress((Page) msg.obj);
                    break;
                }
                case MSG_ORIENTATION_CHANGE: {
                    Pair<Page, ScreenOrientation> pair = (Pair<Page, ScreenOrientation>) msg.obj;
                    onOrientationChange(pair.first, pair.second);
                    break;
                }
                case MSG_UPDATE_LOCALE: {
                    Object[] params = (Object[]) msg.obj;
                    updateLocale(((Locale) params[0]), ((Map<String, JSONObject>) params[1]));
                    break;
                }
                case MSG_NOTIFY_CONFIGURATION_CHANGED: {
                    Object[] params = ((Object[]) msg.obj);
                    notifyConfigurationChanged(((Page) params[0]), ((String) params[1]));
                    break;
                }
                case MSG_TERMINATE_EXECUTION: {
                    // TODO: Fix this. Terminate may cause unknown error.
                    mIsTerminateExecution = true;
                    mJsContext.getV8().terminateExecution();
                    break;
                }
                case MSG_FOLD_CARD: {
                    Object[] params = (Object[]) msg.obj;
                    onFoldCard((int) params[0], (boolean) params[1]);
                    break;
                }
                case MSG_INIT_INSPECTOR_JSCONTEXT: {
                    InspectorManager.getInspector().onJsContextCreated(mJsContext.getV8());
                    break;
                }
                case MSG_ON_REQUEST_APPLICATION: {
                    onRequestApplication();
                    break;
                }
                case MSG_ON_SHOW_APPLICATION: {
                    onShowApplication();
                    break;
                }
                case MSG_ON_HIDE_APPLICATION: {
                    onHideApplication();
                    break;
                }
                case MSG_REGISTER_BUNDLE_CHUNKS: {
                    Object[] params = (Object[]) msg.obj;
                    registerBundleChunks((String) params[0]);
                    break;
                }
                case MSG_PAGE_SCROLL: {
                    Object[] args = (Object[]) msg.obj;
                    onPageScroll((Page) args[0], (int) args[1]);
                    break;
                }
                case MSG_PAGE_REACH_TOP: {
                    onPageReachTop((Page) msg.obj);
                    break;
                }
                case MSG_PAGE_REACH_BOTTOM: {
                    onPageReachBottom((Page) msg.obj);
                    break;
                }
                default: {
                    super.handleMessage(msg);
                    break;
                }
            }
        }
    }
}
