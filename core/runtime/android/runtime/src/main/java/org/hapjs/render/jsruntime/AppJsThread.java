/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.eclipsesource.v8.V8RuntimeException;

import org.hapjs.bridge.EnvironmentManager;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.chunk.JsChunksManager;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FrescoUtils;
import org.hapjs.common.utils.LogUtils;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.component.ComponentRegistry;
import org.hapjs.component.bridge.RenderEventCallback;
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
import org.hapjs.render.IdGenerator;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hapjs.render.RootView.MSG_APP_LOAD_END;

public class AppJsThread extends JsThread {

    private static final String TAG = "AppJsThread";

    public static final String INFRASJS_SNAPSHOT_SO_NAME = "infrasjs_snapshot";
    public static final boolean HAS_INFRASJS_SNAPSHOT;

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

    Handler mMainHandler;
    PageManager mPageManager;
    AppInfo mAppInfo;
    RootView mRootView;
    private LifecycleCallback mCallback;

    private JsContext mJsContext;
    private JsBridgeRegisterHelper mJsBridgeRegisterHelper;
    private ExtensionManager mExtensionManager;
    private RenderActionManager mRenderActionManager;
    private JsChunksManager mJsChunksManager;

    private volatile boolean mBlocked;
    private String mSessionLastAppShow;
    private boolean mIsTerminateExecution;
    private static final int STATE_NONE = -1;
    private static final int STATE_RUNTIME_INITED = 0;
    private static final int STATE_DESTROYING = 1;
    private static final int STATE_DESTROYED = 2;
    private int mApplicationState = STATE_NONE;

    final private List<Integer> APPLICATION_MSGS = Arrays.asList(
            H.MSG_CREATE_APPLICATION,
            H.MSG_DESTROY_APPLICATION,
            H.MSG_CREATE_PAGE,
            H.MSG_REFRESH_PAGE,
            H.MSG_PAGE_NOT_FOUND,
            H.MSG_RECREATE_PAGE,
            H.MSG_DESTROY_PAGE,
            H.MSG_FIRE_EVENT,
            H.MSG_FIRE_KEY_EVENT,
            H.MSG_BACK_PRESS,
            H.MSG_MENU_PRESS,
            H.MSG_FIRE_CALLBACK,
            H.MSG_ORIENTATION_CHANGE
    );

    public class H extends JsThread.H {
        static final private int MSG_CODE_BASE = 100;
        static final private int MSG_INIT_INSPECTOR_JSCONTEXT = MSG_CODE_BASE + 1;

        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if ((mApplicationState == STATE_NONE || mApplicationState == STATE_DESTROYED)
                    && Collections.binarySearch(APPLICATION_MSGS, msg.what) >= 0) {
                // not handle application msg when runtime is not initialized or is destroying
                return;
            }
            switch (msg.what) {
                case MSG_INIT_INSPECTOR_JSCONTEXT: {
                    onJsContextCreated();
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    protected AppJsThread(Context context) {
        super("AppJsThread");

        mContext = context;
        mAppId = IdGenerator.generateAppId();
        mRenderActionManager = new RenderActionManager();
        Log.d(TAG, "AppJsThread create");
        Message.obtain(mHandler, H.MSG_INIT).sendToTarget();
    }

    @Override
    protected H createHandler() {
        return new H(getLooper());
    }

    public void attach(Handler mainHandler,
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

    @Override
    protected void onInit() {
        SandboxProvider provider = ProviderManager.getDefault().getProvider(SandboxProvider.NAME);
        mNative = provider.createNativeImpl(mRenderActionManager,
                frameTimeNanos -> {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mEngine.onFrameCallback(frameTimeNanos);
                        }
                    });
                });

        if (provider.isSandboxEnabled()) {
            ParcelFileDescriptor[][] channelDescriptors = SandboxProcessLauncher.getInstance().getChannelDescriptor();
            ParcelFileDescriptor[] positiveDescriptors = channelDescriptors[0];
            ParcelFileDescriptor[] passiveDescriptors = channelDescriptors[1];

            mEngine = provider.createAppChannelSender(positiveDescriptors[0], positiveDescriptors[1], mHandler);
            provider.createAppChannelReceiver(passiveDescriptors[0], passiveDescriptors[1], mNative);
        } else {
            mJsContext = new JsContext(this);
            mJsBridgeRegisterHelper = new JsBridgeRegisterHelper(mContext, mJsContext, this, getId(), mNative);
            mJsBridgeRegisterHelper.registerBridge();
            mEngine = provider.createEngineImpl(mJsContext,
                    new InspectorNativeCallbackImpl(),
                    e -> processV8Exception(e),
                    frameTimeNanos -> mJsBridgeRegisterHelper.onFrameCallback(frameTimeNanos));
        }

        InspectorManager.getInstance().notifyJsThreadReady(this);

        createRuntime();
        initInfras();

        mExtensionManager = new ExtensionManager(this, mContext);
        mExtensionManager.onRuntimeInit(mEngine);

        ComponentRegistry.registerBuiltInComponents(mEngine);
        FrescoUtils.initialize(mContext);
    }

    private void initInfras() {
        executeVoidFunction(new Object[]{"initInfras", null});
    }

    public boolean isApplicationDebugEnabled() {
        return (mContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)
                == ApplicationInfo.FLAG_DEBUGGABLE;
    }

    @Override
    protected void onAttach(Object msgObj) {
        mRenderActionManager.attach(mRootView);
        mExtensionManager.attach(mRootView, mPageManager, mAppInfo);
        mEngine.setQuickAppPkg(mAppInfo.getPackage());
        mNative.setQuickAppPkg(mAppInfo.getPackage());
        ((JavaNativeImpl) mNative).attachView(mRootView);
        if (mJsBridgeRegisterHelper != null) {
            mJsBridgeRegisterHelper.attach(mAppInfo.getPackage());
        }

        mEngine.onAttach(EnvironmentManager.buildRegisterJavascript(mContext, mAppInfo), mAppInfo.getPackage());

        if (mCallback != null) {
            mCallback.onRuntimeCreate();
        }
        mExtensionManager.onRuntimeCreate(mEngine, mAppInfo);
    }

    public ExtensionManager getBridgeManager() {
        return mExtensionManager;
    }

    public RenderActionManager getRenderActionManager() {
        return mRenderActionManager;
    }

    private void createRuntime() {
        try {
            if (!HAS_INFRASJS_SNAPSHOT) {
                String script = null;
                if (isApplicationDebugEnabled()) {
                    File file = new File(Environment.getExternalStorageDirectory(), "quickapp/assets/js/infras.js");
                    script = JavascriptReader.get().read(new FileSource(file));
                    if (script != null) {
                        Toast.makeText(mContext, "load infras.js from sdcard, please remove quickapp folder in sdcard if you are not dev", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "load infras.js from sdcard");
                    }
                }

                RuntimeLogManager.getDefault().logJsThreadTaskStart(mContext.getPackageName(), RuntimeLogManager.KEY_INFRAS_JS_LOAD);
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
                RuntimeLogManager.getDefault().logJsThreadTaskEnd(mContext.getPackageName(), RuntimeLogManager.KEY_INFRAS_JS_LOAD);
                executeVoidScript(new Object[]{script, "infras.js", 0});
            }
            mApplicationState = STATE_RUNTIME_INITED;
        } catch (V8RuntimeException ex) {
            processV8Exception(ex);
        }
    }

    private String readInfrasAsset() {
        return JavascriptReader.get().read(new AssetSource(mContext, "js/infras.js") {
            @Override
            public InputStream open() throws IOException {
                try {
                    return super.open();
                } catch (IOException e) {
                    String host = mContext.getPackageName();
                    String platform = ResourceConfig.getInstance().getPlatform();
                    RuntimeLogManager.getDefault().logResourceNotFound(host, platform, "js/infras.js", e);

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

    public void postNotifyConfigurationChanged(Page page, String type) {
        if (page == null) {
            return;
        }
        postNotifyConfigurationChanged(page.pageId, type);
    }

    public void postUpdateLocale(Locale locale, Map<String, JSONObject> resources) {
        if (locale == null || resources == null) {
            return;
        }
        postUpdateLocale(locale.getLanguage(), locale.getCountry(), resources);
    }

    public void postCreateApplication(String jsContent, String css) {
        Object[] params = new Object[]{mAppId, jsContent, css, mAppInfo.getMetaInfo()};
        Message.obtain(mHandler, H.MSG_CREATE_APPLICATION, params)
                .sendToTarget();
    }

    public void postOnRequestApplication() {
        Message.obtain(mHandler, H.MSG_ON_REQUEST_APPLICATION, new Object[]{mAppId}).sendToTarget();
    }

    public void postOnShowApplication() {
        Message.obtain(mHandler, H.MSG_ON_SHOW_APPLICATION, new Object[]{mAppId}).sendToTarget();
    }

    public void postOnHideApplication() {
        Message.obtain(mHandler, H.MSG_ON_HIDE_APPLICATION, new Object[]{mAppId}).sendToTarget();
    }

    public void postOnMenuButtonPress(Page page, HybridView.OnKeyUpListener onKeyUpIsConsumption) {
        Object[] params = new Object[]{page, onKeyUpIsConsumption};
        mHandler.obtainMessage(H.MSG_ON_MENU_BUTTON_PRESS, params).sendToTarget();
    }

    public void postPageNotFound(Page page) {
        Message.obtain(mHandler, H.MSG_PAGE_NOT_FOUND, new Object[]{mAppId, page.getTargetPageUri(), page.getPageId()}).sendToTarget();
    }

    public void postBackPress(Page page) {
        mHandler.obtainMessage(H.MSG_BACK_PRESS, page).sendToTarget();
    }

    @Override
    protected boolean backPress(Object msgObj) {
        Page page = (Page) msgObj;
        boolean consumed = false;
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            consumed = super.backPress(new Object[]{page.pageId});
        }
        if (!consumed && null != mMainHandler) {
            mMainHandler.sendEmptyMessage(RootView.MSG_BACK_PRESS);
        }
        return consumed;
    }

    @Override
    protected boolean menuButtonPressPage(Object msgObj) {
        Page page = (Page) ((Object[]) msgObj)[0];
        HybridView.OnKeyUpListener onKeyUpIsConsumption = (HybridView.OnKeyUpListener) ((Object[]) msgObj)[1];
        boolean consumed = false;
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            consumed = super.menuButtonPressPage(new Object[]{page.pageId});
            // TODO: onKeyUpIsConsumption.consume(false);?
        }
        onKeyUpIsConsumption.consume(consumed);
        return consumed;
    }

    @Override
    protected boolean firePageKeyEvent(JsEventCallbackData data) {
        Page page = mPageManager.getPageById(data.pageId);
        if (page == null) {
            return false;
        }

        boolean consumed = super.firePageKeyEvent(data);

        if (null != mMainHandler) {
            Object hashCode = data.params.get(KeyEventDelegate.KEY_HASHCODE);
            if (hashCode instanceof Integer) {
                KeyEventManager.getInstance().injectKeyEvent(consumed, mRootView, (Integer) hashCode);
            }
        }

        return consumed;
    }

    public void postMenuPress(Page page) {
        mHandler.obtainMessage(H.MSG_MENU_PRESS, new Object[]{page}).sendToTarget();
    }

    @Override
    protected boolean onMenuPress(Object msgObj) {
        Page page = (Page) ((Object[]) msgObj)[0];
        boolean consumed = false;
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            consumed = super.onMenuPress(new Object[]{page.pageId});
        }

        if (!consumed) {
            mMainHandler.obtainMessage(RootView.MSG_MENU_PRESS, page).sendToTarget();
        }
        return consumed;
    }

    public void postOrientationChange(Page page, ScreenOrientation screenOrientation) {
        mHandler.obtainMessage(H.MSG_ORIENTATION_CHANGE, new Object[]{page, screenOrientation}).sendToTarget();
    }

    @Override
    protected void onOrientationChange(Object msgObj) {
        Page page = (Page) ((Object[]) msgObj)[0];
        ScreenOrientation screenOrientation = (ScreenOrientation) ((Object[]) msgObj)[1];
        if (page != null && page.getState() >= Page.STATE_CREATED) {
            super.onOrientationChange(new Object[]{page.pageId, screenOrientation.getOrientation(), screenOrientation.getAngel()});
        }
    }

    public void postChangeVisiblePage(Page page, boolean visible) {
        if (page != null) {
            // 当thread被block的时候,activity已经stop,不应该发送visible=true事件,
            // 仅发送visible=false事件,在activity变为start时会重新发送visible=true事件
            if (visible && page.getState() == Page.STATE_INITIALIZED && !mBlocked) {
                if (page.shouldReload()) {
                    RouterUtils.replace(mPageManager, page.getRequest());
                    return;
                }
                requestFocus();
                page.setState(Page.STATE_VISIBLE);
                postExecuteScript("changeVisiblePage(" + page.pageId + ", " + JsUtils.toJsBoolean(visible) + ");");
                Log.d(TAG, "show page: " + page.getName());

                String session = System.getProperty(RuntimeActivity.PROP_SESSION);
                if (!TextUtils.equals(session, mSessionLastAppShow)) {
                    mSessionLastAppShow = session;
                    RuntimeLogManager.getDefault().logAppShow(mAppInfo.getPackage(), mAppInfo.getVersionCode());
                }

                Page referrer = page.getReferrer();
                String referrerName = referrer == null ? null : referrer.getName();
                RuntimeLogManager.getDefault().logPageViewStart(
                        mAppInfo.getPackage(), page.getName(), referrerName);
            } else if (!visible && page.getState() == Page.STATE_VISIBLE) {
                page.setState(Page.STATE_INITIALIZED);
                postExecuteScript("changeVisiblePage(" + page.pageId + ", " + JsUtils.toJsBoolean(visible) + ");");
                Log.d(TAG, "hide page: " + page.getName());
                RuntimeLogManager.getDefault().logPageViewEnd(mAppInfo.getPackage(), page.getName());
            } else {
                Log.i(TAG, "Skip page visible change: page=" + page + ", visible=" + visible + ", mBlocked=" + mBlocked);
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
            mHandler.obtainMessage(H.MSG_DESTROY_PAGE, new Object[]{page.pageId}).sendToTarget();
            page.setState(Page.STATE_NONE);
        } else {
            Log.d(TAG, "skip page destroy: " + page.toString());
        }
    }

    public void loadPage(final Page page) {
        RuntimeLogManager.getDefault().logPageLoadStart(
                mAppInfo.getPackage(), page.getName());

        mMainHandler.obtainMessage(RootView.MSG_LOAD_PAGE_JS_START, page).sendToTarget();
        Executors.io().execute(new AbsTask<String[]>() {
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
                RuntimeLogManager.getDefault().logPageLoadEnd(
                        mAppInfo.getPackage(), page.getName());
                int result = TextUtils.isEmpty(contents[0]) ? Page.JS_LOAD_RESULT_FAIL : Page.JS_LOAD_RESULT_SUCC;
                page.setLoadJsResult(result);
                mMainHandler.obtainMessage(RootView.MSG_LOAD_PAGE_JS_FINISH, page).sendToTarget();

                RoutableInfo routableInfo = page.getRoutableInfo();
                final String jsUri = routableInfo.getUri();
                postCreatePage(page, contents[0], jsUri, contents[1]);
                Log.d(TAG, "loadPage onPostExecute uri=" + jsUri + " result=" + result);
            }
        });
    }

    private void parseStyleSheets(String css, Page page) {
        if (TextUtils.isEmpty(css)) {
            return;
        }
        Executors.io().execute(new AbsTask<Void>() {
            @Override
            protected Void doInBackground() {
                RuntimeLogManager.getDefault().logAsyncThreadTaskStart(mAppInfo.getPackage(), "parseStyleSheets");
                try {
                    org.hapjs.common.json.JSONObject styles = new org.hapjs.common.json.JSONObject(css);
                    org.hapjs.common.json.JSONArray styleList = styles.getJSONArray("list");
                    int N = styleList.length();
                    for (int i = 0; i < N; i++) {
                        org.hapjs.common.json.JSONObject styleSheetPlain = styleList.getJSONObject(i);
                        CSSStyleSheet styleSheet = CSSParser.parseCSSStyleSheet(styleSheetPlain);

                        // 注册样式表
                        RenderActionDocument document = mRenderActionManager.getOrCreateDocument(page.getPageId());
                        if (isVue()) {
                            document.registerDocLevelStyleSheet(styleSheet.getStyleObjectId(), styleSheet);
                        } else {
                            document.registerStyleSheet(styleSheet.getStyleObjectId(), styleSheet);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "parse css failed: " + e.getMessage());
                }
                RuntimeLogManager.getDefault().logAsyncThreadTaskEnd(mAppInfo.getPackage(), "parseStyleSheets");
                return null;
            }
        });
    }

    private boolean isVue() {
        JSONObject config = mRootView.getAppInfo().getConfigInfo().getData();
        JSONObject dsl = config.optJSONObject("dsl");
        if (dsl != null) {
            String dslName = dsl.optString("name");
            return TextUtils.equals(dslName, "vue");
        }
        return false;
    }

    private void postCreatePage(Page page, String js, String uri, String css) {
        Object[] params = new Object[]{page, js, uri, css};
        Message.obtain(mHandler, H.MSG_CREATE_PAGE, params).sendToTarget();
        page.setState(Page.STATE_CREATED);
    }

    protected void createPage(Object msgObj) {
        Page page = (Page) ((Object[]) msgObj)[0];
        String js = (String) ((Object[]) msgObj)[1];
        String css = (String) ((Object[]) msgObj)[2];

        preCreateSkeleton(page);
        preCreateBody(page.pageId);
        super.createPage(new Object[]{mAppId, page.pageId, js, css, page.params, page.intent, page.meta});
        Message.obtain(mMainHandler, MSG_APP_LOAD_END).sendToTarget();
    }

    private void preCreateSkeleton(Page page) {
        if (mAppInfo != null && mContext != null && page != null) {
            SkeletonProvider skeletonProvider = ProviderManager.getDefault().getProvider(SkeletonProvider.NAME);
            if (skeletonProvider == null) {
                // If you have not added a customized provider, use the default (enable function))
                skeletonProvider = new DefaultSkeletonProviderImpl(mContext);
            }
            if (skeletonProvider.isSkeletonEnable(mAppInfo.getPackage())) {
                Executors.io().execute(new AbsTask<JSONObject>() {
                    @Override
                    protected JSONObject doInBackground() {
                        RpkSource skeletonConfigSource = new RpkSource(mContext, mAppInfo.getPackage(), "skeleton/config.json");
                        String skeletonConfig = TextReader.get().read(skeletonConfigSource);
                        JSONObject parseResult = null;
                        if (!TextUtils.isEmpty(skeletonConfig)) {
                            String skFileName = SkeletonConfigParser.getSkeletonFileName(page, skeletonConfig);
                            Log.i(TAG, "LOG_SKELETON parse skeleton config, current page is " + page.getName());
                            if (!TextUtils.isEmpty(skFileName)) {
                                // read and parse sk file
                                String skFilePathName = "skeleton/page/" + skFileName;
                                RpkSource skFileSource = new RpkSource(mContext, mAppInfo.getPackage(), skFilePathName);
                                InputStream inputStream = null;
                                try {
                                    Log.i(TAG, "LOG_SKELETON parse sk file, path = " + skFilePathName);
                                    inputStream = skFileSource.open();
                                    parseResult = SkeletonDSLParser.parse(inputStream);
                                } catch (Exception e) {
                                    Log.e(TAG, "LOG_SKELETON parse sk file fail, ", e);
                                } finally {
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (IOException ioe) {
                                            Log.e(TAG, "LOG_SKELETON close sk inputStream fail, ", ioe);
                                        }
                                    }
                                }
                            } else {
                                Log.i(TAG, "LOG_SKELETON no matching sk file for current page");
                            }
                        } else {
                            Log.i(TAG, "LOG_SKELETON skeleton config file is empty");
                        }
                        return parseResult;
                    }

                    @Override
                    protected void onPostExecute(JSONObject parseResult) {
                        if (mRenderActionManager != null) {
                            mRenderActionManager.showSkeleton(mAppInfo.getPackage(), parseResult);
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
    private boolean isTerminateExecution(String exception_message) {
        return mIsTerminateExecution && "null".equals(exception_message);
    }

    @Override
    protected void recreatePage(Object msgObj) {
        int pageId = (int) ((Object[]) msgObj)[0];
        // page VDoc is destroyed, apply the cached render actions will throw exception
        mMainHandler.obtainMessage(RootView.MSG_PAGE_CLEAR_CACHE, pageId).sendToTarget();
        preCreateBody(pageId);
        super.recreatePage(msgObj);
    }

    @Override
    protected void refreshPage(Object msgObj) {
        int pageId = (int) ((Object[]) msgObj)[0];
        preCreateBody(pageId);
        super.refreshPage(msgObj);
    }

    @Override
    protected void notifyPageNotFound(Object msgObj) {
        int pageId = (int) ((Object[]) msgObj)[2];
        preCreateBody(pageId);
        super.notifyPageNotFound(msgObj);
    }

    private void preCreateBody(int pageId) {
        VDomChangeAction action = new VDomChangeAction();
        action.action = VDomChangeAction.ACTION_PRE_CREATE_BODY;
        action.pageId = pageId;
        RenderActionPackage renderActionPackage = new RenderActionPackage(pageId,
                RenderActionPackage.TYPE_PRE_CREATE_BODY);
        renderActionPackage.renderActionList.add(action);
        mRenderActionManager.sendRenderActions(renderActionPackage);
    }

    @Override
    protected void destroyPage(Object msgObj) {
        int pageId = (int) ((Object[]) msgObj)[0];
        super.destroyPage(msgObj);
        if (mJsBridgeRegisterHelper != null) {
            mJsBridgeRegisterHelper.destroyPage(pageId);
        }
        mRenderActionManager.destroyPage(pageId);
    }

    private void postDestroyApplication() {
        Message.obtain(mHandler, H.MSG_DESTROY_APPLICATION, new Object[]{mAppId}).sendToTarget();
    }

    @Override
    protected void destroyApplication(Object msgObj) {
        try {
            super.destroyApplication(msgObj);
        } finally {
            mApplicationState = STATE_DESTROYED;
        }
    }

    public void postFireEvent(final int pageId, final List<JsEventCallbackData> datas, final RenderEventCallback.EventPostListener listener) {
        post(new Runnable() {
            @Override
            public void run() {
                fireEvent(new Object[]{pageId, datas, listener});
            }
        });
    }

    protected void fireEvent(Object msgObj) {
        RenderEventCallback.EventPostListener listener = (RenderEventCallback.EventPostListener) ((Object[]) msgObj)[2];
        try {
            super.fireEvent(msgObj);
        } finally {
            if (listener != null) {
                listener.finish();
            }
        }
    }

    public void postInitInspectorJsContext() {
        Message.obtain(mHandler, H.MSG_INIT_INSPECTOR_JSCONTEXT).sendToTarget();
    }

    private void onJsContextCreated() {
        InspectorManager.getInspector().onJsContextCreated(mEngine);
    }

    public void block(long delay) {
        super.block(delay);
        mBlocked = true;
    }

    @Override
    public void unblock() {
        super.unblock();
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

    @Override
    protected void doShutdown() {
        InspectorManager.getInspector().onJsContextDispose(mEngine);
        if (mCallback != null) {
            mCallback.onRuntimeDestroy();
        }
        mRenderActionManager.release();
        if (mJsBridgeRegisterHelper != null) {
            mJsBridgeRegisterHelper.unregister();
        }
        mExtensionManager.dispose();
        quit();
        super.doShutdown();
        Log.d(TAG, "shutdown finish: " + this);
    }

    public void processV8Exception(Exception ex) {
        if (isTerminateExecution(ex.getMessage())) {
            mIsTerminateExecution = false;
        } else {
            String msg = LogUtils.getStackTrace(ex);
            Log.e(TAG, msg);
            InspectorManager.getInspector().onConsoleMessage(
                    InspectorProvider.CONSOLE_ERROR, msg);
            Message.obtain(mMainHandler, RootView.MSG_USER_EXCEPTION, ex).sendToTarget();
        }
        notifyAppError(ex);
    }

    @Override
    protected void terminateExecution() {
        mIsTerminateExecution = true;
        super.terminateExecution();
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

    private class InspectorNativeCallbackImpl implements V8InspectorNative.InspectorNativeCallback {
        @Override
        public void inspectorResponse(int sessionId, int callId, String message) {
            InspectorManager.getInspector().inspectorResponse(sessionId, callId, message);
        }

        @Override
        public void inspectorSendNotification(int sessionId, int callId, String message) {
            InspectorManager.getInspector().inspectorSendNotification(sessionId, callId, message);
        }

        @Override
        public void inspectorRunMessageLoopOnPause(int contextGroupId) {
            InspectorManager.getInspector().inspectorRunMessageLoopOnPause(contextGroupId);
        }

        @Override
        public void inspectorQuitMessageLoopOnPause() {
            InspectorManager.getInspector().inspectorQuitMessageLoopOnPause();
        }
    }
}
