/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import static org.hapjs.logging.RuntimeLogManager.VALUE_ROUTER_APP_FROM_WEB;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.FitWindowsViewGroup;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.eventbus.EventBus;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.impl.android.AndroidViewClient;
import org.hapjs.card.api.IRenderListener;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.json.JSONObject;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.net.UserAgentHelper;
import org.hapjs.common.resident.ResidentManager;
import org.hapjs.common.utils.BrightnessUtils;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.MediaUtils;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.ResizeEventDispatcher;
import org.hapjs.component.bridge.ActivityStateListener;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureDispatcher;
import org.hapjs.component.view.keyevent.KeyEventManager;
import org.hapjs.event.ApplicationLaunchEvent;
import org.hapjs.event.EventManager;
import org.hapjs.event.FirstRenderActionEvent;
import org.hapjs.io.JavascriptReader;
import org.hapjs.io.RpkSource;
import org.hapjs.io.Source;
import org.hapjs.io.TextReader;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.NetworkConfig;
import org.hapjs.model.RoutableInfo;
import org.hapjs.model.ScreenOrientation;
import org.hapjs.render.component.CallingComponent;
import org.hapjs.render.jsruntime.JsBridge;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.jsruntime.JsThreadFactory;
import org.hapjs.render.jsruntime.Profiler;
import org.hapjs.render.skeleton.SkeletonProvider;
import org.hapjs.render.skeleton.SkeletonSvgView;
import org.hapjs.render.vdom.DocAnimator;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VDomActionApplier;
import org.hapjs.runtime.BuildConfig;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.LocaleResourcesParser;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.R;
import org.hapjs.runtime.inspect.InspectorManager;
import org.hapjs.system.SysOpProvider;
import org.json.JSONException;

/**
 * It's a view like WebView, used to render a native app
 */
public class RootView extends FrameLayout
        implements JsBridge.JsBridgeCallback, PageManager.PageChangedListener {

    public static final int BLOCK_JS_THREAD_DELAY_TIME = 5 * 1000; // wait 5s until js finish
    public static final int MSG_RENDER_ACTIONS = 0;
    public static final int MSG_BACK_PRESS = 1;
    public static final int MSG_USER_EXCEPTION = 2;
    public static final int MSG_MENU_PRESS = 3;
    public static final int MSG_PAGE_CLEAR_CACHE = 4;
    public static final int MSG_LOAD_PAGE_JS_START = 5;
    public static final int MSG_LOAD_PAGE_JS_FINISH = 6;
    public static final int MSG_PAGE_INITIALIZED = 7;
    public static final int MSG_CHECK_IS_SHOW = 8;
    public static final int MSG_APP_LOAD_END = 1000;
    private static final String TAG = "RootView";
    private static final int PAGE_CACHE_NUM_MAX = 5;
    private static final int SUCCESS = 0;
    private static final int GET_APP_INFO_NULL = -1;
    private static final int SHOW_INCOMPATIBLE_APP_DIALOG = -2;
    private static final int SHOW_INSPECTOR_UNREADY_TOAST = -3;
    private static final int APP_JS_EMPTY = -4;
    private static final Object EVENT_COUNT_DOWN_LATCH_LOCK = new Object();
    public PageManager mPageManager;
    protected VDocument mDocument;
    protected String mPackage;
    protected volatile boolean mIsDestroyed;
    protected AppInfo mAppInfo;
    protected String mUrl;
    protected boolean mInitialized;
    JsThread mJsThread;
    Handler mHandler = new H();
    VDomActionApplier mVdomActionApplier = new VDomActionApplier();
    CallingComponent mCallingComponent = new CallingComponent();
    List<ActivityStateListener> mActivityStateListeners = new ArrayList<>();
    private boolean mExceptionCaught;
    private AndroidViewClient mAndroidViewClient;
    private RuntimeLifecycleCallbackImpl mRuntimeLifecycleCallback;
    private RootViewDialogManager mDialogManager;
    private HybridRequest mRequest;
    private boolean mWaitDevTools;
    private boolean mDirectBack;
    private AtomicBoolean mOnRequestWait = new AtomicBoolean();
    private AtomicBoolean mOnShowWait = new AtomicBoolean();
    private AtomicBoolean mOnHideWait = new AtomicBoolean();
    private AtomicBoolean mHasAppCreated = new AtomicBoolean();
    // add for orientation listener
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener;
    private Page.LoadPageJsListener mLoadPageJsListener;
    private boolean mIsInMultiWindowMode = false;
    private FitWindowsViewGroup.OnFitSystemWindowsListener mListener;
    private ResidentManager mResidentManager;
    private HybridView.OnVisibilityChangedListener mVisibilityChangedListener;
    private ConcurrentLinkedQueue<RenderActionPackage> mRenderActionPackagesBuffer =
            new ConcurrentLinkedQueue<>();
    private List<PageRemoveActionListener> mRemoveActionListenerList;
    private int mCurMenubarStatus = Display.DISPLAY_STATUS_FINISH;
    private Set<OnBackPressedFeatureListener> onBackPressedFeatureListeners =
            new CopyOnWriteArraySet<>();
    private ConfigurationManager.ConfigurationListener mConfigurationListener;
    private AutoplayManager mAutoplayManager;
    private OnDetachedListener mOnDetachedListener;
    private CountDownLatch mEventCountDownLatch;
    protected RenderEventCallback mRenderEventCallback =
            new RenderEventCallback() {
                @Override
                public void onJsEventCallback(
                        int pageId,
                        int ref,
                        String eventName,
                        Component component,
                        Map<String, Object> params,
                        Map<String, Object> attributes) {
                    if (pageId <= -1) {
                        Page currentPage = mPageManager.getCurrPage();
                        if (currentPage == null) {
                            Log.e(TAG, "Fail to call onJsEventCallback for page id: " + pageId);
                            return;
                        }

                        pageId = currentPage.pageId;
                    }

                    JsThread.JsEventCallbackData data =
                            new JsThread.JsEventCallbackData(pageId, ref, eventName, params,
                                    attributes);
                    if (Attributes.Event.KEY_EVENT.equals(data.eventName)
                            || Attributes.Event.KEY_EVENT_PAGE.equals(data.eventName)) {
                        mJsThread.postFireKeyEvent(data);
                        return;
                    }
                    mJsThread.postFireEvent(data);
                }

                @Override
                public void onJsMultiEventCallback(int pageId, List<EventData> events) {
                    if (events == null || events.isEmpty()) {
                        return;
                    }

                    if (pageId <= -1) {
                        Page currentPage = mPageManager.getCurrPage();
                        if (currentPage == null) {
                            Log.e(TAG,
                                    "Fail to call onJsMultiEventCallback for page id: " + pageId);
                            return;
                        }

                        pageId = currentPage.pageId;
                    }

                    List<JsThread.JsEventCallbackData> datas = new ArrayList<>();
                    for (EventData event : events) {
                        JsThread.JsEventCallbackData data =
                                new JsThread.JsEventCallbackData(
                                        pageId, event.elementId, event.eventName, event.params,
                                        event.attributes);
                        datas.add(data);
                    }

                    synchronized (EVENT_COUNT_DOWN_LATCH_LOCK) {
                        mEventCountDownLatch = new CountDownLatch(1);
                    }
                    mJsThread.postFireEvent(
                            pageId,
                            datas,
                            new EventPostListener() {
                                @Override
                                public void finish() {
                                    synchronized (EVENT_COUNT_DOWN_LATCH_LOCK) {
                                        if (mEventCountDownLatch != null
                                                && mEventCountDownLatch.getCount() > 0) {
                                            mEventCountDownLatch.countDown();
                                        }
                                    }
                                }
                            });
                    try {
                        mEventCountDownLatch.await(30, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    } finally {
                        synchronized (EVENT_COUNT_DOWN_LATCH_LOCK) {
                            mEventCountDownLatch = null;
                        }
                    }
                }

                public void onJsMethodCallback(int pageId, String callbackId, Object... params) {
                    if (pageId <= -1) {
                        Page currentPage = mPageManager.getCurrPage();
                        if (currentPage == null) {
                            Log.e(TAG, "Fail to call onJsMethodCallback for page id: " + pageId);
                            return;
                        }

                        pageId = currentPage.pageId;
                    }

                    JsThread.JsMethodCallbackData data =
                            new JsThread.JsMethodCallbackData(pageId, callbackId, params);
                    mJsThread.postFireCallback(data);
                }

                public void onJsException(Exception exception) {
                    processUserException(exception);
                }

                @Override
                public void addActivityStateListener(ActivityStateListener listener) {
                    mActivityStateListeners.add(listener);
                }

                @Override
                public void removeActivityStateListener(ActivityStateListener listener) {
                    mActivityStateListeners.remove(listener);
                }

                @Override
                public Uri getCache(final String resourcePath) {
                    Page currentPage = mPageManager.getCurrPage();
                    if (currentPage == null) {
                        Log.e(TAG, "Fail to getCache for current page is null");
                        return null;
                    } else {
                        return HapEngine.getInstance(mPackage)
                                .getResourceManager()
                                .getResource(resourcePath, currentPage.getName());
                    }
                }

                @Override
                public void onPostRender() {
                    Page currentPage = mPageManager.getCurrPage();
                    if (currentPage != null) {
                        RuntimeLogManager.getDefault()
                                .logPageRenderEnd(mPackage, currentPage.getName());
                    }
                }

                @Override
                public Uri getUnderlyingUri(String internalPath) {
                    return getAppContext().getUnderlyingUri(internalPath);
                }

                @Override
                public void loadUrl(String url) {
                    RootView.this.load(url, false);
                }

                @Override
                public boolean shouldOverrideUrlLoading(String url, String sourceH5,int pageId) {
                    HybridRequest request =
                            new HybridRequest.Builder().uri(url).isDeepLink(true).pkg(mPackage)
                                    .build();
                    return RouterUtils.router(
                            getContext(), mPageManager, pageId, request, VALUE_ROUTER_APP_FROM_WEB, sourceH5);
                }

                @Override
                public File createFileOnCache(String prefix, String suffix) throws IOException {
                    return getAppContext().createTempFile(prefix, suffix);
                }

                @Override
                public void onPageReachTop() {
                    mJsThread.postPageReachTop(getCurrentPage());
                }

                @Override
                public void onPageReachBottom() {
                    mJsThread.postPageReachBottom(getCurrentPage());
                }

                @Override
                public void onPageScroll(int scrollTop) {
                    mJsThread.postPageScroll(getCurrentPage(), scrollTop);
                }
            };
    private boolean mFirstRenderActionReceived = false;
    private InnerPageEnterListener mPageEnterListener = new InnerPageEnterListener();

    public RootView(Context context) {
        this(context, null);
    }

    public RootView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // onConfigurationChanged无法监听180度翻转，并且会受其他config改变的影响。
        // OrientationEventListener受制于g-sensor，无法监听代码中手动setRequestedOrientation的情况
        // 使用DisplayManager，只监听默认Display
        mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    public static void onHandleSkeletonHide(String source, VDocument document) {
        if (document == null) {
            return;
        }
        DocComponent component = document.getComponent();
        if (!(component.getInnerView() instanceof DecorLayout)) {
            return;
        }
        DecorLayout decorLayout = (DecorLayout) component.getInnerView();
        SkeletonSvgView skeletonView = decorLayout.findViewById(R.id.skeleton);
        if (skeletonView != null) {
            if (SkeletonProvider.HIDE_SOURCE_NATIVE.equals(source) && !skeletonView.isAutoHide()) {
                // If CP set the skeleton that is not automatically hidden, jump out of the logic
                return;
            }
            decorLayout.removeView(skeletonView);
            Log.i(TAG, "LOG_SKELETON onHandleSkeletonHide remove skeleton, source = " + source);
        }
    }

    public void onAppLoadEnd() {
        Log.i(TAG, "onRenderSuccess");
        onRenderSuccess();
    }

    public void setWaitDevTools(boolean waitDevTools) {
        mWaitDevTools = waitDevTools;
    }

    public void setDirectBack(boolean directBack) {
        mDirectBack = directBack;
    }

    public JsThread getJsThread() {
        return mJsThread;
    }

    @Nullable
    public Page getCurrentPage() {
        if (mPageManager != null) {
            return mPageManager.getCurrPage();
        } else {
            return null;
        }
    }

    public PageManager getPageManager() {
        return mPageManager;
    }

    public AutoplayManager getAutoplayManager() {
        if (mAutoplayManager == null) {
            mAutoplayManager = new AutoplayManager();
        }
        return mAutoplayManager;
    }

    public AppInfo getAppInfo() {
        return mAppInfo;
    }

    public VDocument getDocument() {
        return mDocument;
    }

    public String getUrl() {
        return mUrl;
    }

    public void load(String url) {
        load(url, true);
    }

    private void load(String url, boolean fromExternal) {
        if (DebugUtils.DBG) {
            DebugUtils.record("load");
        }

        if (mIsDestroyed) {
            throw new IllegalStateException("Can't load when the RootView is destroyed.");
        }

        if (mAndroidViewClient != null && mAndroidViewClient.shouldOverrideUrlLoading(this, url)) {
            return;
        }

        mUrl = url;
        HybridRequest request =
                new HybridRequest.Builder().pkg(mPackage).uri(url).fromExternal(fromExternal)
                        .build();
        if (mPackage == null) {
            if (!(request instanceof HybridRequest.HapRequest)) {
                throw new IllegalArgumentException("url is invalid: " + url);
            }
            launchApp((HybridRequest.HapRequest) request);
        } else {
            if (!routerPage(request)) {
                onRenderFailed(IRenderListener.ErrorCode.ERROR_PAGE_NOT_FOUND, "Page not found");
            }
        }
    }

    public void reloadPackage() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Can't load when the RootView is destroyed.");
        }

        if (mPageManager == null) {
            Log.w(TAG, "mPageManager has not been initialized.");
            return;
        }

        mPageManager
                .setAppInfo(HapEngine.getInstance(mPackage).getApplicationContext().getAppInfo());
        try {
            mPageManager.reload();
        } catch (PageNotFoundException e) {
            mJsThread.processV8Exception(e);
        }
    }

    public void reloadCurrentPage() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Can't load when the RootView is destroyed.");
        }
        Page oldCurrentPage = getCurrentPage();
        if (oldCurrentPage != null) {
            RouterUtils.replace(mPageManager, oldCurrentPage.getRequest());
        }
    }

    private void launchApp(HybridRequest.HapRequest request) {
        mPackage = request.getPackage();
        RuntimeLogManager.getDefault().logAppLoadStart(mPackage);
        // DisplayUtil增加版本兼容
        DisplayUtil.setHapEngine(HapEngine.getInstance(mPackage));
        loadAppInfo(request);

        if (!HapEngine.getInstance(mPackage).isCardMode()
                && !HapEngine.getInstance(mPackage).isInsetMode()) {
            Display.initSystemUI(((Activity) getContext()).getWindow(), this);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mDocument != null) {
            if (!changed && mInitialized) {
                return;
            }
            mInitialized = true;
            DecorLayout mDecorLayout = (DecorLayout) mDocument.getComponent().getInnerView();
            if (mDecorLayout != null) {
                int windowWidth = mDecorLayout.getMeasuredWidth();
                int windowHeight =
                        mDecorLayout.getMeasuredHeight() - mDecorLayout.getContentInsets().top;
                DisplayUtil.setViewPortWidth(windowWidth);
                DisplayUtil.setViewPortHeight(windowHeight);
            }
        }
    }

    void setOnFitSystemWindowsListener(FitWindowsViewGroup.OnFitSystemWindowsListener listener) {
        mListener = listener;
    }

    @Override
    public boolean fitSystemWindows(Rect insets) {
        if (mListener != null) {
            mListener.onFitSystemWindows(insets);
        }
        return super.fitSystemWindows(insets);
    }

    protected boolean routerPage(HybridRequest request) {
        return RouterUtils.router(getContext(), mPageManager, request);
    }

    protected boolean pushPage(PageManager pageManager, HybridRequest request)
            throws PageNotFoundException {
        return RouterUtils.push(pageManager, request);
    }

    @Override
    protected void onAttachedToWindow() {
        if (mIsDestroyed) {
            throw new IllegalStateException("Can't reuse a RootView");
        }
        super.onAttachedToWindow();
        if (mDisplayListener == null) {
            mDisplayListener =
                    new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {
                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {
                        }

                        @Override
                        public void onDisplayChanged(int displayId) {
                            WindowManager windowManager =
                                    (WindowManager) getContext()
                                            .getSystemService(Context.WINDOW_SERVICE);
                            android.view.Display defaultDisplay = windowManager.getDefaultDisplay();
                            // 只处理默认display
                            if (defaultDisplay == null
                                    || defaultDisplay.getDisplayId() != displayId) {
                                return;
                            }
                            int rotation = defaultDisplay.getRotation();
                            ScreenOrientation screenOrientation = new ScreenOrientation();
                            switch (rotation) {
                                case Surface.ROTATION_90:
                                    screenOrientation.setOrientation(
                                            ScreenOrientation.ORIENTATION_LANDSCAPE_SECONDARY);
                                    screenOrientation.setAngel(360f - 90f);
                                    break;
                                case Surface.ROTATION_180:
                                    screenOrientation.setOrientation(
                                            ScreenOrientation.ORIENTATION_PORTRAIT_SECONDARY);
                                    screenOrientation.setAngel(360f - 180f);
                                    break;
                                case Surface.ROTATION_270:
                                    screenOrientation.setOrientation(
                                            ScreenOrientation.ORIENTATION_LANDSCAPE_PRIMARY);
                                    screenOrientation.setAngel(360f - 270f);
                                    break;
                                case Surface.ROTATION_0:
                                    // no break
                                default:
                                    screenOrientation.setOrientation(
                                            ScreenOrientation.ORIENTATION_PORTRAIT_PRIMARY);
                                    screenOrientation.setAngel(0);
                                    break;
                            }
                            onOrientationChanged(screenOrientation);
                        }
                    };
        }
        mDisplayManager.registerDisplayListener(mDisplayListener, null);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            onVisibilityChanged(this, getVisibility());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
        if (mOnDetachedListener != null) {
            mOnDetachedListener.onDetached();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mDocument == null || mDocument.getComponent().isOpenWithAnimation()) {
            Log.i(TAG, "Ignore all key event in page animation");
            return true;
        }
        return KeyEventManager.getInstance().onDispatchKeyEvent(event)
                || super.dispatchKeyEvent(event);
    }

    public void release() {
        ConfigurationManager.getInstance().removeListener(mConfigurationListener);
        onAllPagesRemoved();
        destroy(false);
    }

    public void destroy(boolean immediately) {
        Log.d(TAG, "destroy: this=" + this + ", js=" + mJsThread + ", immediately=" + immediately);

        if (mIsDestroyed) {
            if (immediately && mJsThread != null && mJsThread.isAlive()) {
                mJsThread.shutdown(0);
            }
            return;
        }

        if (mDocument != null) {
            mDocument.destroy();
            mDocument = null;
        }

        mIsDestroyed = true;
        if (!TextUtils.isEmpty(mPackage)) {
            RuntimeLogManager.getDefault().logAppDiskUsage(mPackage);
        }
        if (TextUtils.isEmpty(mPackage) || HapEngine.getInstance(mPackage).isCardMode()) {
            if (mJsThread != null) {
                mJsThread.shutdown(immediately ? 0 : BLOCK_JS_THREAD_DELAY_TIME);
            }
        } else {
            mResidentManager.postDestroy(immediately);
        }
        dismissDialog();
        Context context = getContext();
        if (context instanceof Activity) {
            if (!((Activity) context).isDestroyed()) {
                Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
                for (; iterator.hasNext(); ) {
                    ActivityStateListener listener = iterator.next();
                    listener.onActivityDestroy();
                }
            }
        }
        mActivityStateListeners.clear();
        GestureDispatcher.remove(mRenderEventCallback);
        ResizeEventDispatcher.destroyInstance(mRenderEventCallback);
    }

    public boolean canGoBack() {
        if (onBackPressedFeatureListeners != null && onBackPressedFeatureListeners.size() > 0) {
            return true;
        }
        if (mJsThread == null) {
            return false;
        }
        return !mDirectBack || (mPageManager != null && mPageManager.getCurrIndex() > 0);
    }

    public void goBack() {
        if (onBackPressedFeatureListeners != null && onBackPressedFeatureListeners.size() > 0) {
            boolean isIntercepted = false;
            for (OnBackPressedFeatureListener listener : onBackPressedFeatureListeners) {
                isIntercepted = isIntercepted || listener.onBackPress();
            }
            if (isIntercepted) {
                return;
            }
        }
        if (mJsThread != null) {
            Page page = mPageManager.getCurrPage();
            mJsThread.postBackPress(page);
        }
    }

    public void menuButtonPressPage(HybridView.OnKeyUpListener onKeyUpIsConsumption) {
        if (mJsThread != null) {
            Page page = mPageManager.getCurrPage();
            mJsThread.postOnMenuButtonPress(page, onKeyUpIsConsumption);
        }
    }

    public void showMenu() {
        if (mJsThread != null) {
            Page page = mPageManager.getCurrPage();
            mJsThread.postMenuPress(page);
        }
    }

    public void onOrientationChanged(ScreenOrientation screenOrientation) {
        if (mJsThread == null) {
            return;
        }
        Page page = mPageManager.getCurrPage();
        mJsThread.postOrientationChange(page, screenOrientation);
    }

    private void showIncompatibleAppDialog() {
        if (mDialogManager == null) {
            mDialogManager = new RootViewDialogManager((Activity) getThemeContext(), mAppInfo);
        }
        mDialogManager.showIncompatibleAppDialog();
    }

    protected Source getJsAppSource() {
        return new RpkSource(getContext(), getPackage(), "app.js");
    }

    private void processUserException(Exception exception) {
        recordPageError(exception);
        showUserException(exception);
    }

    protected void showUserException(Exception exception) {
        if (mDialogManager == null) {
            mDialogManager = new RootViewDialogManager((Activity) getThemeContext(), mAppInfo);
        }
        mDialogManager.showExceptionDialog(exception);
    }

    private void recordPageError(Exception exception) {
        String pageName = null;
        if (mPageManager != null) {
            Page curPage = mPageManager.getCurrPage();
            if (curPage != null) {
                pageName = curPage.getName();
            }
        }
        RuntimeLogManager.getDefault().logPageError(mPackage, pageName, exception);
    }

    private void dismissDialog() {
        if (mDialogManager != null) {
            mDialogManager.dismissDialog();
            mDialogManager = null;
        }
    }

    public void showSystemMenu() {
        if (!isConsumeMenu()) {
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (provider != null) {
                provider.showSystemMenu(getContext(), mAppInfo);
            }
        }
    }

    private boolean isConsumeMenu() {
        boolean isConsume = false;
        if (null == mDocument) {
            return isConsume;
        }
        DocComponent docComponent = mDocument.getComponent();
        if (null == docComponent) {
            return isConsume;
        }
        ViewGroup viewGroup = docComponent.getInnerView();
        DecorLayout mDecorLayout = null;
        Display display = null;
        if (viewGroup instanceof DecorLayout) {
            mDecorLayout = (DecorLayout) viewGroup;
        }
        if (null != mDecorLayout) {
            display = mDecorLayout.getDecorLayoutDisPlay();
        }
        if (null != display) {
            isConsume = display.initShowMenubarDialog();
        }
        return isConsume;
    }

    /**
     * Called by V8Inspector to resume app executing
     */
    public void startJsApp() {
        if (mRequest != null) {
            loadAppInfo(mRequest);
        }
    }

    private void loadAppInfo(final HybridRequest request) {
        Executors.io()
                .execute(
                        new AbsTask<LoadResult>() {
                            @Override
                            protected LoadResult doInBackground() {
                                RuntimeLogManager.getDefault()
                                        .logAsyncThreadTaskStart(mPackage, "loadAppInfo");
                                Log.i(TAG, "loadAppInfo " + String.valueOf(request.getPackage()));
                                ApplicationContext appContext =
                                        HapEngine.getInstance(request.getPackage())
                                                .getApplicationContext();
                                mAppInfo = appContext.getAppInfo(false);
                                if (mAppInfo == null) {
                                    return LoadResult.APP_INFO_NULL;
                                } else if (mAppInfo.getMinPlatformVersion()
                                        > BuildConfig.platformVersion) {
                                    return LoadResult.INCOMPATIBLE_APP;
                                }

                                UserAgentHelper.setAppInfo(mAppInfo.getPackage(),
                                        mAppInfo.getVersionName());
                                NetworkConfig networkConfig =
                                        mAppInfo.getConfigInfo().getNetworkConfig();
                                if (networkConfig != null) {
                                    HttpConfig.get().onConfigChange(networkConfig);
                                }

                                final DisplayInfo displayInfo = mAppInfo.getDisplayInfo();
                                if (HapEngine.getInstance(mPackage).getMode() == HapEngine.Mode.APP
                                        && displayInfo != null
                                        && DarkThemeUtil
                                        .needChangeDefaultNightMode(displayInfo.getThemeMode())) {
                                    post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    AppCompatDelegate.setDefaultNightMode(
                                                            DarkThemeUtil.convertThemeMode(
                                                                    displayInfo.getThemeMode()));
                                                    Context context = getContext();
                                                    if (context instanceof Activity
                                                            && DarkThemeUtil.needRecreate(
                                                            displayInfo.getThemeMode())) {
                                                        Activity activity = (Activity) context;
                                                        activity.recreate();
                                                    }
                                                }
                                            });
                                }

                                EventManager.getInstance()
                                        .invoke(new ApplicationLaunchEvent(mAppInfo.getPackage()));
                                mRuntimeLifecycleCallback = new RuntimeLifecycleCallbackImpl();
                                mPageManager = new PageManager(RootView.this, mAppInfo);
                                // 在调试模式下, 等待Devtools连接会导致loadAppInfo被中断.当Devtools就绪会重新loadAppInfo,
                                // 此时需要保持V8状态,不能重新初始化JsThread.
                                if (mWaitDevTools && mRequest != null) {
                                    mRequest = null;
                                } else {
                                    mJsThread = JsThreadFactory.getInstance().create(getContext());
                                }
                                mJsThread.getJsChunksManager().initialize(mAppInfo);

                                // 当点击"开始调试",若Inspector没有销毁,则与DevTools的连接仍然保持,
                                // 此时需要通过isInspectorReady方法知晓此状态,直接进入启动流程.
                                try {
                                    if (mWaitDevTools
                                            && !InspectorManager.getInspector().isInspectorReady()) {
                                        mRequest = request;
                                        return LoadResult.INSPECTOR_UNREADY;
                                    }
                                } catch (AbstractMethodError e) {
                                    Log.e(TAG, "Inspector call isInspectorReady error", e);
                                }

                                if (!HapEngine.getInstance(mPackage).isCardMode()) {
                                    mResidentManager.init(getContext(), mAppInfo, mJsThread);
                                }
                                mJsThread.attach(
                                        mHandler, mAppInfo, RootView.this,
                                        mRuntimeLifecycleCallback, mPageManager);

                                // init configuration before create application
                                initConfiguration(appContext);
                                mJsThread.getJsChunksManager().registerAppChunks();

                                RuntimeLogManager.getDefault().logAppJsLoadStart(mPackage);
                                String content = JavascriptReader.get().read(getJsAppSource());
                                if (TextUtils.isEmpty(content)) {
                                    return LoadResult.APP_JS_EMPTY;
                                }

                                Source cssSource =
                                        new RpkSource(getContext(), getPackage(), "app.css.json");
                                String css = TextReader.get().read(cssSource);
                                RuntimeLogManager.getDefault().logAppJsLoadEnd(mPackage);
                                mJsThread.postCreateApplication(content, css, request);
                                mHasAppCreated.set(true);

                                if (mAndroidViewClient != null) {
                                    mAndroidViewClient.onApplicationCreate(RootView.this, mAppInfo);
                                }
                                if (mOnRequestWait.compareAndSet(true, false)) {
                                    mJsThread.postOnRequestApplication();
                                }
                                if (mOnShowWait.compareAndSet(true, false)) {
                                    mJsThread.postOnShowApplication();
                                }
                                if (mOnHideWait.compareAndSet(true, false)) {
                                    mJsThread.postOnHideApplication();
                                }

                                try {
                                    pushPage(mPageManager, request);
                                } catch (PageNotFoundException ex) {
                                    mJsThread.processV8Exception(ex);
                                    return LoadResult.PAGE_NOT_FOUND;
                                } finally {
                                    mHandler.sendEmptyMessage(MSG_CHECK_IS_SHOW);
                                }

                                return LoadResult.SUCCESS;
                            }

                            @Override
                            protected void onPostExecute(LoadResult result) {
                                RuntimeLogManager.getDefault()
                                        .logAsyncThreadTaskEnd(mPackage, "loadAppInfo");
                                if (mIsDestroyed) {
                                    onRenderFailed(IRenderListener.ErrorCode.ERROR_UNKNOWN,
                                            "RootView has destroy");
                                    return;
                                }

                                boolean handled = false;
                                switch (result) {
                                    case SUCCESS:
                                        // skip
                                        break;
                                    case APP_INFO_NULL:
                                        onRenderFailed(
                                                IRenderListener.ErrorCode.ERROR_FILE_NOT_FOUND,
                                                "Package resource not found");
                                        break;
                                    case PAGE_NOT_FOUND:
                                        onRenderFailed(
                                                IRenderListener.ErrorCode.ERROR_PAGE_NOT_FOUND,
                                                "Page not found");
                                        break;
                                    case INCOMPATIBLE_APP:
                                        handled =
                                                onRenderFailed(
                                                        IRenderListener.ErrorCode.ERROR_INCOMPATIBLE,
                                                        "App is incompatible with platform");
                                        if (!handled) {
                                            showIncompatibleAppDialog();
                                        }
                                        break;
                                    case INSPECTOR_UNREADY:
                                        handled =
                                                onRenderFailed(
                                                        IRenderListener.ErrorCode.ERROR_INSPECTOR_UNREADY,
                                                        "Inspector is not ready");
                                        if (!handled) {
                                            Toast.makeText(getContext(), R.string.inspector_unready,
                                                    Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                        break;
                                    default:
                                        onRenderFailed(IRenderListener.ErrorCode.ERROR_UNKNOWN,
                                                result.toString());
                                        break;
                                }
                            }
                        });
    }

    private void initConfiguration(ApplicationContext appContext) {
        ConfigurationManager.getInstance().init(appContext);
        Locale initialLocale = ConfigurationManager.getInstance().getCurrentLocale();
        mJsThread.postUpdateLocale(
                initialLocale,
                LocaleResourcesParser.getInstance()
                        .resolveLocaleResources(mPackage, initialLocale));
        if (mConfigurationListener == null) {
            mConfigurationListener = new RootViewConfigurationListener(this);
        }
        ConfigurationManager.getInstance().addListener(mConfigurationListener);
    }

    private void handleConfigurationChange(Page currentPage, HapConfiguration config) {
        if (currentPage == null || config == null) {
            return;
        }

        HapConfiguration curConfig = currentPage.getHapConfiguration();

        HapConfiguration newConfig = config.obtain();

        // handle locale change.
        boolean updateTitleBar = false;
        Locale newLocale = newConfig.getLocale();
        if (curConfig == null || !curConfig.getLocale().equals(newLocale)) {
            updateTitleBar = true;
            if (mJsThread != null) {
                mJsThread.postUpdateLocale(
                        newLocale,
                        LocaleResourcesParser.getInstance()
                                .resolveLocaleResources(mPackage, newLocale));
                mJsThread.postNotifyConfigurationChanged(currentPage,
                        JsThread.CONFIGURATION_TYPE_LOCALE);
            }
            newConfig.setLocale(newLocale);
        }

        // handle theme mode change.
        if (curConfig == null || curConfig.getLastUiMode() != newConfig.getUiMode()) {
            mJsThread.postNotifyConfigurationChanged(currentPage,
                    JsThread.CONFIGURATION_TYPE_THEME_MODE);
            mJsThread.getRenderActionManager().updateMediaPropertyInfo(currentPage);
            newConfig.setLastUiMode(newConfig.getUiMode());
        }

        // update config to current page.
        currentPage.setHapConfiguration(newConfig);

        // update titlebar
        if (updateTitleBar) {
            Executors.ui()
                    .execute(
                            () -> {
                                if (mDocument != null) {
                                    mDocument
                                            .getComponent()
                                            .updateTitleBar(Collections.emptyMap(),
                                                    currentPage.pageId);
                                }
                            });
        }
    }

    void onHandleRenderActionsBuffer() {
        if (mRenderActionPackagesBuffer.size() > 0) {
            RenderActionPackage renderActionPackage;
            while ((renderActionPackage = mRenderActionPackagesBuffer.poll()) != null) {
                onSendRenderActionsInMainThread(renderActionPackage);
            }
        }
    }

    void onSendRenderActionsInMainThread(RenderActionPackage renderActionPackage) {
        if (mIsDestroyed || mExceptionCaught) {
            return;
        }

        Page page = mPageManager.getPageById(renderActionPackage.pageId);
        if (page == null) {
            return;
        }

        for (RenderAction action : renderActionPackage.renderActionList) {
            page.pushRenderAction(action);
        }

        applyActions();
    }

    public void applyActions() {
        if (mDocument == null) {
            return;
        }
        Page currentPage = mPageManager.getCurrPage();
        if (currentPage == null) {
            return;
        }

        RuntimeLogManager.getDefault().logUIThreadTaskStart(mPackage, "applyActions");
        RenderAction action = currentPage.pollRenderAction();
        while (action != null) {
            applyAction(action);
            action = currentPage.pollRenderAction();
        }
        RuntimeLogManager.getDefault().logUIThreadTaskEnd(mPackage, "applyActions");
    }

    public void applyAction(RenderAction action) {
        try {

            HapEngine hapEngine = HapEngine.getInstance(mPackage);
            if (action instanceof VDomChangeAction) {
                mVdomActionApplier.applyChangeAction(
                        hapEngine,
                        getThemeContext(),
                        mJsThread,
                        (VDomChangeAction) action,
                        mDocument,
                        mRenderEventCallback);
            } else if (action instanceof ComponentAction) {
                mCallingComponent.applyComponentAction((ComponentAction) action, mDocument);
            }

        } catch (Exception ex) {
            // fix bug: when ex.getMessage return null, log.e(tag, msg) will crash
            // eg. java.lang.NullPointerException
            //           at com.xx.xx
            Log.e(TAG, "Send render actions failed", ex);
            mExceptionCaught = true;
            mJsThread.processV8Exception(ex);
        }
    }

    /* start implement JsBridgeCallback */
    @Override
    public void onSendRenderActions(final RenderActionPackage renderActionPackage) {
        mRenderActionPackagesBuffer.offer(renderActionPackage);
        Message message = Message.obtain(mHandler, MSG_RENDER_ACTIONS);
        mHandler.sendMessageAtFrontOfQueue(message);

        if (!mFirstRenderActionReceived
                && renderActionPackage.type != RenderActionPackage.TYPE_PRE_CREATE_BODY) {
            mFirstRenderActionReceived = true;
            EventBus.getDefault().post(new FirstRenderActionEvent());
            Profiler.recordFirstFrameRendered(System.nanoTime(), mJsThread.getId());
        }
    }

    @Override
    public void onRenderSkeleton(String packageName, org.json.JSONObject parseResult) {
        if (mDocument == null || parseResult == null || TextUtils.isEmpty(packageName)) {
            return;
        }
        Component component = mDocument.getComponent();
        if (component != null) {
            DecorLayout decorLayout = (DecorLayout) ((Container) component).getInnerView();
            if (decorLayout == null) {
                Log.i(TAG, "LOG_SKELETON do not render skeleton because decorLayout is null ");
                return;
            }
            Rect insets = decorLayout.getContentInsets();
            if (insets == null) {
                Log.i(TAG,
                        "LOG_SKELETON do not render skeleton because decorLayout insets is null ");
                return;
            }
            int skeletonViewWidth = getWidth() - insets.left;
            int skeletonViewHeight = getHeight() - insets.top;
            if (skeletonViewWidth <= 0 || skeletonViewHeight <= 0) {
                Log.i(
                        TAG,
                        "LOG_SKELETON do not render skeleton because skeletonSvgView size has not yet determined ");
                return;
            }
            SkeletonSvgView skeletonSvgView = new SkeletonSvgView(getContext(), packageName);
            RelativeLayout.LayoutParams layoutParams =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT);
            layoutParams.topMargin += insets.top;
            layoutParams.leftMargin += insets.left;
            skeletonSvgView.setLayoutParams(layoutParams);
            skeletonSvgView.setId(R.id.skeleton);
            skeletonSvgView.setClickable(true);
            Page currentPage = getCurrentPage();
            if (currentPage != null) {
                skeletonSvgView.setBackgroundColor(currentPage.getBackgroundColor());
            } else {
                skeletonSvgView.setBackgroundColor(Color.WHITE);
            }
            try {
                skeletonSvgView.setup(parseResult, skeletonViewWidth, skeletonViewHeight);
                // Before set and draw skeleton, check whether it has been called to hide
                boolean isCpHideSkeleton = mDocument.isCpHideSkeleton();
                boolean isCreateFinish = mDocument.isCreateFinish();
                if (isCreateFinish || (isCpHideSkeleton && !skeletonSvgView.isAutoHide())) {
                    // two cases:
                    // 1. createFinish comes earlier
                    // 2. It has been set to not automatically hide but the API call of CP comes earlier
                    Log.i(TAG, "LOG_SKELETON prevent adding skeleton screen because Cp "
                            + "call hide api earlier or createFinish comes earlier");
                    return;
                }
                decorLayout.addView(skeletonSvgView);
                skeletonSvgView.start();
                Log.i(TAG, "LOG_SKELETON render skeleton success");
            } catch (Exception e) {
                Log.e(TAG, "LOG_SKELETON render skeleton fail, ", e);
            }
        }
    }

    /* start implement PageManager.PageChangedListener */
    @Override
    public void onPagePreChange(int oldIndex, int newIndex, Page oldPage, Page newPage) {
        if (BuildConfig.DEBUG) {
            ThreadUtils.checkInMainThread();
        }

        if (mIsDestroyed) {
            return;
        }

        if (newIndex < oldIndex) {
            // when back, we should call onConfigurationChanged() before onShow()
            handleConfigurationChange(newPage, ConfigurationManager.getInstance().getCurrent());
        }
        mJsThread.postChangeVisiblePage(oldPage, false);
        InspectorManager.getInspector().onPagePreChange(oldIndex, newIndex, oldPage, newPage);
    }
    /* end implement JsBridgeCallback */

    @Override
    public void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage) {
        if (BuildConfig.DEBUG) {
            ThreadUtils.checkInMainThread();
        }

        if (mIsDestroyed) {
            return;
        }

        RuntimeLogManager.getDefault().logPageChanged(newPage);

        if (newPage != null && newPage.getReferrer() == null && newIndex >= oldIndex) {
            newPage.setReferrer(oldPage);
        }

        onPageChangedInMainThread(oldIndex, newIndex, oldPage, newPage);
        InspectorManager.getInspector().onPageChanged(oldIndex, newIndex, oldPage, newPage);
    }

    private void onPageRemoved(int pageId) {
        if (mRemoveActionListenerList == null || mRemoveActionListenerList.size() <= 0) {
            return;
        }

        Iterator<PageRemoveActionListener> iterator = mRemoveActionListenerList.iterator();
        while (iterator.hasNext()) {
            PageRemoveActionListener listener = iterator.next();
            if (listener != null && listener.onPageRemoved(pageId)) {
                mRemoveActionListenerList.remove(listener);
            }
        }
    }

    @Override
    public void onPageRemoved(int index, Page page) {
        if (BuildConfig.DEBUG) {
            ThreadUtils.checkInMainThread();
        }

        if (mIsDestroyed) {
            return;
        }

        if (page != null && !page.shouldCache()) {
            onPageRemoved(page.pageId);
            page.clearCache();
            mJsThread.postDestroyPage(page);
        }
        InspectorManager.getInspector().onPageRemoved(index, page);
    }

    void onPageChangedInMainThread(int oldIndex, int newIndex, Page oldPage, Page currPage) {
        if (mIsDestroyed) {
            return;
        }

        if (newIndex < 0 || currPage == null) {
            ((Activity) getContext()).onBackPressed();
            return;
        }

        if (currPage.isPageNotFound()) {
            mJsThread.postPageNotFound(currPage);
        }

        boolean refresh = currPage.shouldRefresh();
        if (oldPage == currPage) {
            mJsThread.postChangeVisiblePage(currPage, true);
            if (refresh) {
                mJsThread.postRefreshPage(currPage);
            }
            currPage.setShouldRefresh(false);
            return;
        }

        ApplicationContext applicationContext =
                HapEngine.getInstance(mPackage).getApplicationContext();

        VDocument oldDocument = null;
        if (mDocument != null) {
            oldDocument = mDocument;
            JSONObject oldPageAnimateSettingObj = getPageAnimationJsonFromParams(oldPage);
            int animType =
                    newIndex >= oldIndex
                            ? Attributes.getPageOpenExitAnimation(
                            oldPageAnimateSettingObj, DocAnimator.TYPE_PAGE_OPEN_EXIT)
                            : Attributes.getPageCloseExitAnimation(
                            oldPageAnimateSettingObj, DocAnimator.TYPE_PAGE_CLOSE_EXIT);
            mDocument.detachChildren(
                    animType,
                    new InnerPageExitListener(mDocument, oldPage, newIndex > oldIndex),
                    newIndex > oldIndex);
            applicationContext.dispatchPageStop(oldPage);

            if (newIndex <= oldIndex) { // 返回操作
                applicationContext.dispatchPageDestroy(oldPage);
            }
        }

        applicationContext.dispatchPageStart(currPage);

        RoutableInfo routableInfo = currPage.getRoutableInfo();
        if (mAndroidViewClient != null) {
            HybridRequest request =
                    new HybridRequest.Builder().pkg(mPackage).uri(routableInfo.getPath()).build();
            mAndroidViewClient.onPageStarted(RootView.this, request.getUri());
        }
        JSONObject currPageAnimateSettingObj = getPageAnimationJsonFromParams(currPage);
        if (newIndex >= oldIndex) {
            forward(newIndex, currPage, currPageAnimateSettingObj);
        } else {
            backward(currPage, currPageAnimateSettingObj);
        }
        DocComponent oldComponent = oldDocument == null ? null : oldDocument.getComponent();
        DocComponent newComponent = mDocument.getComponent();
        if (newIndex >= oldIndex) {
            onPageEnter(oldComponent, newComponent);
        } else {
            onPageExit(oldComponent, newComponent);
        }
        currPage.setShouldRefresh(false);
    }

    private void backward(Page currPage, JSONObject currPageAnimateSettingObj) {
        boolean refresh = currPage.shouldRefresh();
        VDocument cacheDoc = currPage.getCacheDoc();
        boolean hasWeb = cacheDoc != null && cacheDoc.hasWebComponent();
        boolean darkModeChanged = DarkThemeUtil.isDarkModeChanged(getThemeContext(), cacheDoc);
        if (cacheDoc != null && !(hasWeb && darkModeChanged)) {
            RuntimeLogManager.getDefault()
                    .logPageCacheRenderStart(mAppInfo.getPackage(), currPage.getName());
            mDocument = currPage.getCacheDoc();
            if (currPage.hasRenderActions()) {
                applyActions();
            }
            mDocument.attachChildren(
                    false,
                    Attributes.getPageCloseEnterAnimation(
                            currPageAnimateSettingObj, DocAnimator.TYPE_PAGE_CLOSE_ENTER),
                    mPageEnterListener);
            mJsThread.postChangeVisiblePage(currPage, true);
            if (refresh) {
                mJsThread.postRefreshPage(currPage);
            }
        } else {
            mJsThread.postRecreatePage(currPage);
            RuntimeLogManager.getDefault()
                    .logPageRecreateRenderStart(mAppInfo.getPackage(), currPage.getName());
            mDocument = new VDocument(createDocComponent(currPage.pageId));
            mDocument.attachChildren(
                    false,
                    Attributes.getPageCloseEnterAnimation(
                            currPageAnimateSettingObj, DocAnimator.TYPE_PAGE_CLOSE_ENTER),
                    mPageEnterListener);
            currPage.setDisplayInfo(mDocument);
        }
    }
    /* end implement PageManager.PageChangedListener */

    private void forward(int newIndex, Page currPage, JSONObject currPageAnimateSettingObj) {
        if (currPage.getCacheDoc() != null) {
            mDocument = currPage.getCacheDoc();
            mDocument.attachChildren(
                    true,
                    newIndex == 0
                            ? 0
                            : Attributes.getPageOpenEnterAnimation(
                            currPageAnimateSettingObj, DocAnimator.TYPE_PAGE_OPEN_ENTER),
                    mPageEnterListener);
            mJsThread.postChangeVisiblePage(currPage, true);
        } else {
            mJsThread.loadPage(currPage);
            RuntimeLogManager.getDefault()
                    .logPageCreateRenderStart(mAppInfo.getPackage(), currPage.getName());
            mDocument = new VDocument(createDocComponent(currPage.pageId));
            mDocument.attachChildren(
                    true,
                    newIndex == 0
                            ? 0
                            : Attributes.getPageOpenEnterAnimation(
                            currPageAnimateSettingObj, DocAnimator.TYPE_PAGE_OPEN_ENTER),
                    mPageEnterListener);
            currPage.setCacheDoc(mDocument);
            currPage.setDisplayInfo(mDocument);
        }
    }

    protected DocComponent createDocComponent(int pageId) {
        return new DocComponent(
                HapEngine.getInstance(mPackage),
                getThemeContext(),
                pageId,
                mRenderEventCallback,
                this,
                mAppInfo);
    }

    protected void onPageEnter(DocComponent oldComponent, DocComponent newComponent) {
    }

    protected void onPageExit(DocComponent oldComponent, DocComponent newComponent) {
    }

    public void onActivityCreate() {
        Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
        for (; iterator.hasNext(); ) {
            ActivityStateListener listener = iterator.next();
            listener.onActivityCreate();
        }
    }

    public void onActivityRequest() {
        if (mIsDestroyed) {
            Log.w(TAG, "RootView is destroyed, skip onRequest");
            return;
        }
        if (mJsThread != null && mHasAppCreated.get()) {
            mJsThread.postOnRequestApplication();
        } else {
            mOnRequestWait.set(true);
        }
    }

    public void onActivityStart() {
        Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
        for (; iterator.hasNext(); ) {
            ActivityStateListener listener = iterator.next();
            listener.onActivityStart();
        }
        start();
    }

    public void onActivityResume() {
        Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
        for (; iterator.hasNext(); ) {
            ActivityStateListener listener = iterator.next();
            listener.onActivityResume();
        }
        resume();
    }

    public void onActivityPause() {
        Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
        for (; iterator.hasNext(); ) {
            ActivityStateListener listener = iterator.next();
            listener.onActivityPause();
        }
        pause();
    }

    public void onActivityStop() {
        Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
        for (; iterator.hasNext(); ) {
            ActivityStateListener listener = iterator.next();
            listener.onActivityStop();
        }
        stop();
    }

    public void onActivityDestroy() {
        if (getPageManager() != null) {
            getPageManager().clearCachePage();
        }
        dismissDialog();
        Iterator<ActivityStateListener> iterator = mActivityStateListeners.iterator();
        for (; iterator.hasNext(); ) {
            ActivityStateListener listener = iterator.next();
            listener.onActivityDestroy();
        }
        BrightnessUtils.unregisterAllObervers(getContext());
        if (!TextUtils.isEmpty(mPackage)) {
            MediaUtils.clearExpiredTempFile(getContext().getApplicationContext(), mPackage);
        }
        ConfigurationManager.getInstance().reset(getContext());
    }

    protected void start() {
        if (!TextUtils.isEmpty(mPackage)) {
            if (HapEngine.getInstance(mPackage).isCardMode()) {
                if (mJsThread != null) {
                    mJsThread.unblock();
                }
            } else {
                mResidentManager.postRunInForeground();
            }
        }
    }

    protected void resume() {
        if (mJsThread != null && mHasAppCreated.get()) {
            mJsThread.postOnShowApplication();
        } else {
            mOnShowWait.set(true);
        }
        if (!TextUtils.isEmpty(mPackage)) {
            if (HapEngine.getInstance(mPackage).isCardMode()) {
                setCurrentPageVisible(true);
            } else {
                // must unblock jsthread before invoking setCurrentPageVisible
                mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                setCurrentPageVisible(true);
                            }
                        });
            }
        }
    }

    protected void pause() {
        setCurrentPageVisible(false);
        if (mJsThread != null && mHasAppCreated.get()) {
            mJsThread.postOnHideApplication();
        } else {
            mOnHideWait.set(true);
        }
    }

    protected void stop() {
        if (!TextUtils.isEmpty(mPackage)) {
            if (HapEngine.getInstance(mPackage).isCardMode()) {
                if (mJsThread != null) {
                    mJsThread.block(BLOCK_JS_THREAD_DELAY_TIME);
                }
            } else {
                mResidentManager.postRunInBackground();
            }
        }
    }

    protected void setCurrentPageVisible(boolean visible) {
        if (mPageManager != null && mJsThread != null) {
            Page page = mPageManager.getCurrPage();
            if (page != null) {
                if (!visible && page.getState() == Page.STATE_VISIBLE) {
                    mJsThread.postChangeVisiblePage(page, false);
                } else if (visible && page.getState() == Page.STATE_INITIALIZED) {
                    mJsThread.postChangeVisiblePage(page, true);
                }
            }
        }
    }

    public String getPackage() {
        return mPackage;
    }

    public void setAndroidViewClient(AndroidViewClient client) {
        mAndroidViewClient = client;
    }

    public void setLoadPageJsListener(Page.LoadPageJsListener loadPageJsListener) {
        mLoadPageJsListener = loadPageJsListener;
    }

    public void setResidentManager(ResidentManager residentManager) {
        mResidentManager = residentManager;
    }

    public boolean isInMultiWindowMode() {
        return mIsInMultiWindowMode;
    }

    public void setInMultiWindowMode(boolean isInMultiWindowMode) {
        mIsInMultiWindowMode = isInMultiWindowMode;
    }

    public ApplicationContext getAppContext() {
        return HapEngine.getInstance(mPackage).getApplicationContext();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        GestureDispatcher dispatcher = null;
        if (!mIsDestroyed) {
            dispatcher = GestureDispatcher.createInstanceIfNecessary(mRenderEventCallback);
        }
        boolean result = true;
        try {
            result = super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            Log.e(TAG, "Fail to dispatchTouchEvent: ", e);
        }
        // 事件分发完后统一通过GestureDispatcher将事件下发js
        if (dispatcher != null) {
            dispatcher.flush();
        }
        return result;
    }

    protected Context getThemeContext() {
        return getContext();
    }

    @Override
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (mVisibilityChangedListener != null) {
            mVisibilityChangedListener.onVisibilityChanged(isShown());
        }
    }

    public void setOnVisibilityChangedListener(HybridView.OnVisibilityChangedListener l) {
        mVisibilityChangedListener = l;
    }

    public void addPageRemoveActionListener(PageRemoveActionListener listener) {
        if (listener != null) {
            if (mRemoveActionListenerList == null) {
                mRemoveActionListenerList = new CopyOnWriteArrayList<>();
            }
            mRemoveActionListenerList.add(listener);
        }
    }

    public void addOnBackPressedFeatureListener(OnBackPressedFeatureListener listener) {
        onBackPressedFeatureListeners.add(listener);
    }

    public void removeOnBackPressedFeatureListener(OnBackPressedFeatureListener listener) {
        onBackPressedFeatureListeners.remove(listener);
    }

    private void onAllPagesRemoved() {
        if (mRemoveActionListenerList != null && mRemoveActionListenerList.size() > 0) {
            Iterator<PageRemoveActionListener> iterator = mRemoveActionListenerList.iterator();
            while (iterator.hasNext()) {
                PageRemoveActionListener listener = iterator.next();
                if (listener != null) {
                    listener.onAllPagesRemoved();
                }
            }
            mRemoveActionListenerList.clear();
        }
    }

    public int getMenubarStatus() {
        return mCurMenubarStatus;
    }

    public void setMenubarStatus(int status) {
        mCurMenubarStatus = status;
    }

    protected void onRenderSuccess() {
    }

    protected boolean onRenderFailed(int errorCode, String message) {
        return false;
    }

    protected boolean onRenderProgress() {
        return false;
    }

    private JSONObject getPageAnimationJsonFromParams(Page page) {
        JSONObject pageAnimateSettingObj = null;
        if (page != null && page.params != null && page.params.size() > 0) {
            Object obj = page.params.get(HybridRequest.PARAM_PAGE_ANIMATION);
            if (obj instanceof String) {
                String animationStr = obj.toString().trim();
                try {
                    pageAnimateSettingObj = new JSONObject(animationStr);
                } catch (JSONException e) {
                    Log.e(TAG, "onPageChangedInMainThread: ", e);
                }
            }
        }
        return pageAnimateSettingObj;
    }

    protected void onPageInitialized(Page page) {
        if (page == getCurrentPage()) {
            setCurrentPageVisible(true);
            onHandleSkeletonHide(SkeletonProvider.HIDE_SOURCE_NATIVE, mDocument);
        } else {
            Log.d(TAG, "not current page. skip page=" + page);
        }
    }

    public void setOnDetachedListener(OnDetachedListener listener) {
        mOnDetachedListener = listener;
    }

    enum LoadResult {
        SUCCESS,
        APP_INFO_NULL,
        INCOMPATIBLE_APP,
        INSPECTOR_UNREADY,
        APP_JS_EMPTY,
        PAGE_NOT_FOUND
    }

    public interface PageRemoveActionListener {
        void onAllPagesRemoved();

        boolean onPageRemoved(int pageId);
    }

    public interface OnBackPressedFeatureListener {
        boolean onBackPress();
    }

    public interface OnDetachedListener {
        void onDetached();
    }

    private static class RootViewConfigurationListener
            implements ConfigurationManager.ConfigurationListener {
        private WeakReference<RootView> mReference;

        public RootViewConfigurationListener(RootView rootView) {
            mReference = new WeakReference<>(rootView);
        }

        @Override
        public void onConfigurationChanged(HapConfiguration newConfig) {
            if (mReference == null) {
                return;
            }
            RootView rootView = mReference.get();
            if (rootView == null) {
                return;
            }
            Page currentPage = rootView.mPageManager.getCurrPage();
            rootView.handleConfigurationChange(currentPage, newConfig);
        }
    }

    class H extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RENDER_ACTIONS: {
                    onHandleRenderActionsBuffer();
                    break;
                }
                case MSG_BACK_PRESS: {
                    RouterUtils.back(getContext(), mPageManager);
                    break;
                }
                case MSG_USER_EXCEPTION: {
                    processUserException((Exception) msg.obj);
                    break;
                }
                case MSG_MENU_PRESS: {
                    showSystemMenu();
                    break;
                }
                case MSG_APP_LOAD_END: {
                    onAppLoadEnd();
                    break;
                }
                case MSG_PAGE_CLEAR_CACHE: {
                    Page page = (Page) msg.obj;
                    if (page != null) {
                        page.clearCache();
                    }
                    break;
                }
                case MSG_LOAD_PAGE_JS_START: {
                    Page page = (Page) msg.obj;
                    if (page != null && mLoadPageJsListener != null) {
                        mLoadPageJsListener.onLoadStart(page);
                    }
                    break;
                }
                case MSG_LOAD_PAGE_JS_FINISH: {
                    Page page = (Page) msg.obj;
                    if (page != null && mLoadPageJsListener != null) {
                        mLoadPageJsListener.onLoadFinish(page);
                    }
                    break;
                }
                case MSG_PAGE_INITIALIZED: {
                    Page page = (Page) msg.obj;
                    onPageInitialized(page);
                    break;
                }
                case MSG_CHECK_IS_SHOW: {
                    if (!isShown()) {
                        stop();
                    }
                    break;
                }
                default: {
                    super.handleMessage(msg);
                    break;
                }
            }
        }
    }

    private class InnerPageEnterListener implements DocComponent.PageEnterListener {

        @Override
        public void onStart() {
        }

        @Override
        public void onEnd() {
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            applyActions();
                        }
                    });
        }
    }

    private class InnerPageExitListener implements DocComponent.PageExitListener {
        private VDocument mDocument;
        private Page mPage;
        private boolean mIsOpen;

        InnerPageExitListener(VDocument document, Page page, boolean isOpen) {
            mDocument = document;
            mPage = page;
            mIsOpen = isOpen;
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onEnd() {
            if (mIsOpen) {
                if (mPageManager.getPageCount() > PAGE_CACHE_NUM_MAX) {
                    Page page = mPageManager
                            .getPage(mPageManager.getPageCount() - PAGE_CACHE_NUM_MAX - 1);
                    if (page != null && !page.shouldCache()) {
                        page.clearCache();
                        page.setState(Page.STATE_CREATED);
                    }
                }
            } else {
                // replace current page or back to previous one
                VDocument vDocument = mDocument;
                if (vDocument != null && !mPage.shouldCache()) {
                    vDocument.destroy();
                } else {
                    mPage.touchCacheUsedTime();
                }
            }

            mDocument = null;
            mPage = null;
        }
    }

    private class RuntimeLifecycleCallbackImpl implements JsThread.LifecycleCallback {
        @Override
        public void onRuntimeCreate() {
            if (mAndroidViewClient != null) {
                mAndroidViewClient.onRuntimeCreate(RootView.this);
            }
        }

        @Override
        public void onRuntimeDestroy() {
            if (mAndroidViewClient != null) {
                mAndroidViewClient.onRuntimeDestroy(RootView.this);
            }
        }
    }
}
