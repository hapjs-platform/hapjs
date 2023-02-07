/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDisplay;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaUnit;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.FoldingUtils;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.common.utils.SnapshotUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.common.utils.ViewIdUtils;
import org.hapjs.component.animation.Animation;
import org.hapjs.component.animation.AnimationParser;
import org.hapjs.component.animation.AnimatorListenerBridge;
import org.hapjs.component.animation.CSSAnimatorSet;
import org.hapjs.component.animation.Origin;
import org.hapjs.component.animation.TimingFactory;
import org.hapjs.component.animation.Transform;
import org.hapjs.component.appearance.AppearanceHelper;
import org.hapjs.component.bridge.ActivityStateListener;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Corner;
import org.hapjs.component.constants.Edge;
import org.hapjs.component.constants.Spacing;
import org.hapjs.component.transition.CSSTransitionSet;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.SwipeDelegate;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.GestureDispatcher;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.component.view.metrics.Metrics;
import org.hapjs.component.view.state.State;
import org.hapjs.model.AppInfo;
import org.hapjs.model.DisplayInfo;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.IHybridViewHolder;
import org.hapjs.render.Page;
import org.hapjs.render.css.Node;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.system.SysOpProvider;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class Component<T extends View>
        implements ActivityStateListener, ComponentDataHolder, IHybridViewHolder {

    public static final int INVALID_PAGE_ID = -1;
    public static final String METHOD_FOCUS = "focus";
    public static final String METHOD_ANIMATE = "animate";
    public static final String METHOD_REQUEST_FULLSCREEN = "requestFullscreen";
    public static final String METHOD_GET_BOUNDING_CLIENT_RECT = "getBoundingClientRect";
    public static final String METHOD_TO_TEMP_FILE_PATH = "toTempFilePath";

    public static final String KEY_FILE_TYPE = "fileType";
    public static final String KEY_QUALITY = "quality";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_FAIL = "fail";
    public static final String KEY_COMPLETE = "complete";

    public static final String TEMP_FILE_PATH = "tempFilePath";
    public static final String PSEUDO_STATE = "pseudo";
    public static final int TOUCH_TYPE_FLOATING = 0;
    public static final int TOUCH_TYPE_SWIPE = 1;
    public static final int TOUCH_TYPE_ACTIVE = 2;
    private static final String TAG = "Component";
    private static final String LAYOUT_DATA = "layout_data";
    private static final String CALLBACK_KEY_SUCCESS = "success";
    private static final String CALLBACK_KEY_FAIL = "fail";
    private static final String CALLBACK_KEY_COMPLETE = "complete";
    private static final int MIN_DISPLAY_SHOW_PLATFORM_VERSION = 1080;
    private static SysOpProvider sSysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
    protected Context mContext;
    protected Container mParent;
    protected int mRef;
    protected Node mCssNode;
    protected RenderEventCallback mCallback;
    protected YogaNode mNode;
    protected T mHost;
    protected Position mPosition;
    protected String mId;
    protected boolean mLazyCreate; // TODO remove mLazyCreate
    protected Map<String, CSSValues> mStyleDomData;
    protected Map<String, Object> mAttrsDomData;
    protected Set<String> mEventDomData;
    protected Map<String, Boolean> mStateAttrs;
    protected List<OnDomDataChangeListener> mDomDataChangeListeners;
    protected List<OnDomTreeChangeListener> mDomTreeChangeListeners;
    protected HapEngine mHapEngine;
    protected ComponentBackgroundComposer mBackgroundComposer;
    protected int mMinPlatformVersion;
    protected Canvas mSnapshotCanvas;
    protected int mAdaptiveBeforeWidth;
    protected boolean mShow = true;
    protected boolean mShowAttrInitialized = false;
    private boolean mIsFixPositionDisabled = false;
    private Spacing mPadding;
    private Metrics mSizeLimitMetrics;
    private boolean[] mPaddingExist = new boolean[4];
    private boolean[] mWatchAppearance = new boolean[2];
    private List<View.OnFocusChangeListener> mFocusChangeListeners;
    private View.OnFocusChangeListener mFocusChangeListener;
    private View.OnFocusChangeListener mBlurChangeListener;
    private ResizeListener mResizeListener;
    private SwipeDelegate mSwipeDelegate;
    private AnimatorListenerBridge.AnimatorEventListener mAnimatorEventListener;
    private Map<String, String> mStylesApplyed;
    private Map<String, Object> mSavedState;
    private List<String> mHookData;
    private Transform mTransform;
    private CSSAnimatorSet mAnimatorSet;
    private CSSTransitionSet mTransitionSet;
    private Component mSceneRootComponent;
    private boolean isTransitionInitialized = false;
    private boolean mWidthDefined = false;
    private boolean mHeightDefined = false;
    private float mPercentWidth = -1;
    private float mPercentHeight = -1;
    private View.OnTouchListener mOnTouchListener;
    private SparseArray<View.OnTouchListener> mTouchListeners;
    private FloatingTouchListener mFloatingTouchListener;
    private HashMap<String, Animation> mAnimations;
    private boolean mKeyEventRegister;
    private ViewTreeObserver mViewTreeObserver;
    private ViewTreeObserver.OnGlobalLayoutListener mTransformLayoutListener;
    private RecyclerDataItem mBoundRecyclerItem;
    private boolean mApplyedPseudoStyle;
    private Map<String, Boolean> mPendingStates;
    private Set<String> mCachedAttrsSet = new HashSet<>();
    private ComponentPreDrawListener mReadyPreDrawListener;
    private ComponentPreDrawListener mUnReadyPreDrawListener;
    private boolean mRegisterClickEvent;

    protected boolean mIsAdMaterial = false;
    protected boolean mIsUseInList = false;

    private View mFullScreenView;

    public Component(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback,
                     Map<String, Object> savedState) {
        mHapEngine = hapEngine;
        mContext = context;
        mParent = parent;
        mRef = ref;
        mCallback = callback;
        mSavedState = savedState;

        mStyleDomData = new LinkedHashMap<>();
        mAttrsDomData = new ArrayMap<>();
        mEventDomData = new ArraySet<>();
        mStateAttrs = new ArrayMap<>();
        mStylesApplyed = new ArrayMap<>();

        mDomDataChangeListeners = new ArrayList<>();
        mDomTreeChangeListeners = new ArrayList<>();

        mAnimations = new HashMap<>();
        mHookData = new ArrayList<>();

        if (parent != null) {
            mLazyCreate = parent.mLazyCreate;
        }

        if (hapEngine != null) {
            AppInfo appInfo = hapEngine.getApplicationContext().getAppInfo();
            if (appInfo != null) {
                mMinPlatformVersion = appInfo.getMinPlatformVersion();
            }
        }
        if (mParent != null && mParent.isAdMaterial()) {
            mIsAdMaterial = true;
        }
        if (mParent != null && mParent.isUseInList()) {
            mIsUseInList = true;
        }
    }

    public T createView() {
        if (!mLazyCreate) {
            mHost = createViewImpl();
            applyAttrs(mAttrsDomData);
            applyStyles(mStyleDomData);
            applyEvents(mEventDomData);
            onRestoreInstanceState(mSavedState);

            configBubbleEventAbove1040(false);
        }
        updateViewId();

        return mHost;
    }

    public T lazyCreateView() {
        mHost = createViewImpl();
        configBubbleEventAbove1040(false);
        if (mNode != null) {
            // 如果多次lazyCreate，YogaNode需要reset。
            mNode = null;
            // 在lazyApplyData之前，如果有onStateChanged，将出现NullPointerException
            initYogaNodeFromHost();
        }
        updateViewId();
        invalidBackground();
        return mHost;
    }

    public void setLazyCreate(boolean lazyCreate) {
        mLazyCreate = lazyCreate;
    }

    public void lazyApplyData() {
        applyAttrs(mAttrsDomData, true);
        applyStyles(mStyleDomData, true);
        applyEvents(mEventDomData);
        applyCache();
    }

    @Override
    public Map<String, CSSValues> getStyleDomData() {
        return mStyleDomData;
    }

    @Override
    public Map<String, Object> getAttrsDomData() {
        return mAttrsDomData;
    }

    protected abstract T createViewImpl();

    public T getHostView() {
        return mHost;
    }

    public void setHostView(View view) {
        if (view instanceof ComponentHost) {
            ((ComponentHost) view).setComponent(this);
        }
        mHost = (T) view;
        invalidBackground();
        configBubbleEventAbove1040(true);
        initYogaNodeFromHost();
        setFullScreenView(mHost);
    }

    public int getMinPlatformVersion() {
        return mMinPlatformVersion;
    }

    public void addOnDomDataChangeListener(OnDomDataChangeListener listener) {
        mDomDataChangeListeners.add(listener);
    }

    public void addOnDomTreeChangeListener(OnDomTreeChangeListener listener) {
        mDomTreeChangeListeners.add(listener);
    }

    public void addOnFocusChangeListener(View.OnFocusChangeListener listener) {
        if (mHost == null) {
            return;
        }
        if (mFocusChangeListeners == null) {
            mFocusChangeListeners = new ArrayList<>();
            mHost.setOnFocusChangeListener(
                    new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            for (View.OnFocusChangeListener listener : mFocusChangeListeners) {
                                if (listener != null) {
                                    listener.onFocusChange(v, hasFocus);
                                }
                            }
                        }
                    });
        }
        mFocusChangeListeners.add(listener);
    }

    public void removeOnFocusChangeListener(View.OnFocusChangeListener listener) {
        if (mHost == null) {
            return;
        }
        if (mFocusChangeListeners != null) {
            mFocusChangeListeners.remove(listener);
            if (mFocusChangeListeners.isEmpty()) {
                mFocusChangeListeners = null;
                mHost.setOnFocusChangeListener(null);
            }
        }
    }

    public void setCssNode(Node cssNode) {
        mCssNode = cssNode;
    }

    /**
     * bind attributes.
     *
     * @param attrs attribute key and value.
     */
    @Override
    public void bindAttrs(Map<String, Object> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return;
        }

        mAttrsDomData.putAll(attrs);

        if (mHost != null) {
            applyAttrs(attrs);

            for (OnDomDataChangeListener listener : mDomDataChangeListeners) {
                listener.onAttrsChange(this, attrs);
            }
        }
    }

    public void applyAttrs(Map<String, Object> attrs) {
        applyAttrs(attrs, false);
    }

    public void applyAttrs(Map<String, Object> attrs, boolean force) {

        if (attrs == null || (mLazyCreate && !force)) {
            return;
        }

        if (mNode == null) {
            initYogaNodeFromHost();
        }

        Map<String, Boolean> stateChangedAttrs = new ArrayMap<>();
        for (String key : attrs.keySet()) {
            Object attribute = attrs.get(key);
            setAttribute(key, attribute);
            if (attribute != null) {
                String attributeStr = attribute.toString();
                if ("true".equals(attributeStr) || "false".equals(attributeStr)) {
                    Boolean inValue = mStateAttrs.get(key);
                    Boolean outValue = Attributes.getBoolean(attribute, false);
                    if (inValue == null || inValue != outValue) {
                        mStateAttrs.put(key, outValue);
                        stateChangedAttrs.put(key, outValue);
                    }
                }
            }
        }
        processStateChanged(stateChangedAttrs);

        invalidateYogaLayout();
    }

    private void processStateChanged(Map<String, Boolean> stateChangedAttrs) {
        if (stateChangedAttrs == null || stateChangedAttrs.isEmpty()) {
            return;
        }
        for (String key : mStylesApplyed.keySet()) {
            for (String state : stateChangedAttrs.keySet()) {
                CSSValues attributeMap = mStyleDomData.get(key);
                if (attributeMap == null) {
                    Log.w(TAG, "processStateChanged: attributeMap is null");
                    continue;
                }
                Boolean isChangeObj = stateChangedAttrs.get(state);
                if (isChangeObj != null && isChangeObj) {
                    Object attribute = attributeMap.get(state);
                    if (attribute != null) {
                        setAttribute(key, attribute);
                        mStylesApplyed.put(key, state);
                    }
                } else {
                    String applyedState = mStylesApplyed.get(key);
                    if (applyedState != null && applyedState.equals(state)) {
                        String fallbackState = State.NORMAL;
                        for (String fallback : mStateAttrs.keySet()) {
                            Boolean stateAttr = mStateAttrs.get(fallback);
                            if (stateAttr != null && stateAttr) {
                                Object attribute = attributeMap.get(fallback);
                                if (attribute != null) {
                                    fallbackState = fallback;
                                    break;
                                }
                            }
                        }
                        setAttribute(key, attributeMap.get(fallbackState));
                        mStylesApplyed.put(key, fallbackState);
                    }
                }
            }
        }

        setRealPadding();

        if (mPosition != null) {
            mPosition.applyPosition();
        }

        applyBackground();
    }

    /**
     * bind styles.
     *
     * @param attrs attribute key and value.
     */
    @Override
    public void bindStyles(Map<String, ? extends CSSValues> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return;
        }

        mStyleDomData.putAll(attrs);

        if (mHost != null) {
            applyStyles(attrs);

            for (OnDomDataChangeListener listener : mDomDataChangeListeners) {
                listener.onStylesChange(this, attrs);
            }
        }
    }

    public void applyStyles(Map<String, ? extends CSSValues> attrs) {
        applyStyles(attrs, false);
    }

    public void applyStyles(Map<String, ? extends CSSValues> attrs, boolean force) {

        if (attrs == null || (mLazyCreate && !force)) {
            return;
        }

        if (mNode == null) {
            initYogaNodeFromHost();
        }

        setPaddingExist(Attributes.Style.PADDING, false);

        for (String key : attrs.keySet()) {
            CSSValues attributeMap = attrs.get(key);
            if (attributeMap == null) {
                Log.w(TAG, "applyStyles: attributeMap is null");
                continue;
            }
            String applyState = getState(key);
            Object attribute = attributeMap.get(applyState);
            setAttribute(key, attribute);

            if (mHost != null) {
                mStylesApplyed.put(key, applyState);
            }
        }

        setRealPadding();

        if (mHost == null) {
            return;
        }

        handleStyles();
    }

    private void handleStyles() {
        if (mPosition != null) {
            mPosition.applyPosition();
        }

        applyBackground();

        if (mAnimatorSet != null) {
            if (!mAnimatorSet.isReady() && !TextUtils.isEmpty(mAnimatorSet.getAttr())) {
                addUnReadyPreDrawListener();
            } else if (mAnimatorSet.isDirty()) {
                if (mAnimatorSet.isRunning()) {
                    mAnimatorSet.cancel();
                }
                if (mHost.isAttachedToWindow()) {
                    mAnimatorSet.start();
                } else {
                    addReadyPreDrawListener();
                }
            } else if (mAnimatorSet.isRunning() && mAnimatorSet.isPropertyUpdated()) {
                mAnimatorSet.cancel();
                CSSAnimatorSet temp = mAnimatorSet.parseAndStart();
                if (temp != null) {
                    temp.setIsPropertyUpdated(false);
                    setAnimatorSet(temp);
                }
            } else {
                Log.i(TAG, "applyStyles: animation style applying when it is not running.");
            }
        }

        if (mTransform != null
                && (!Float.isNaN(mTransform.getTranslationXPercent())
                || !Float.isNaN(mTransform.getTranslationYPercent())
                || !Float.isNaN(mTransform.getTranslationZPercent()))) {
            addGlobalLayoutListener();
        } else if (mTransformLayoutListener != null) {
            removeGlobalLayoutListener();
        }

        invalidateYogaLayout();
    }

    public boolean isApplyedPseudoStyle() {
        return mApplyedPseudoStyle;
    }

    public void setApplyedPseudoStyle(boolean applyedPseudoStyle) {
        if (DebugUtils.DBG) {
            Log.d(
                    TAG,
                    "setApplyedPseudoStyle: "
                            + applyedPseudoStyle
                            + " component: "
                            + this
                            + " cssnode: "
                            + mCssNode);
        }
        mApplyedPseudoStyle = applyedPseudoStyle;
    }

    public void restoreStyles() {
        if (DebugUtils.DBG) {
            Log.d(TAG, "restoreStyles component: " + this + " node: " + mCssNode);
        }
        if (isApplyedPseudoStyle()) {
            Map<String, CSSValues> cssValuesMap = new HashMap<>();
            for (String key : mCachedAttrsSet) {
                cssValuesMap.put(key, mStyleDomData.get(key));
            }
            applyStyles(cssValuesMap);
            setApplyedPseudoStyle(false);
            mCachedAttrsSet.clear();
        }
    }

    public void applyPseoudoStyles(String cssValuesKey, Map<String, ? extends CSSValues> attrs) {

        if (mNode == null) {
            initYogaNodeFromHost();
        }

        setPaddingExist(Attributes.Style.PADDING, false);

        for (String key : mCachedAttrsSet) {
            CSSValues attributeMap = mStyleDomData.get(key);
            if (attributeMap == null) {
                Log.w(TAG, "applyStyles: attributeMap is null");
                continue;
            }
            String applyState = getState(key);
            Object attribute = attributeMap.get(applyState);
            setAttribute(key, attribute);

            if (mHost != null) {
                mStylesApplyed.put(key, applyState);
            }
        }

        if (attrs == null) {
            return;
        }

        if (mNode == null) {
            initYogaNodeFromHost();
        }

        setPaddingExist(Attributes.Style.PADDING, false);

        for (String key : attrs.keySet()) {
            CSSValues attributeMap = attrs.get(key);
            if (attributeMap == null) {
                Log.w(TAG, "applyStyles: attributeMap is null");
                continue;
            }
            Object attribute = attributeMap.get(PSEUDO_STATE + "+" + cssValuesKey);
            setAttribute(key, attribute);
        }

        setRealPadding();

        if (mHost == null) {
            return;
        }

        handleStyles();

        setApplyedPseudoStyle(true);
        mCachedAttrsSet.clear();
        mCachedAttrsSet.addAll(attrs.keySet());
    }

    private void clearUnReadyPreDrawListener() {
        if (mHost != null) {
            ViewTreeObserver viewTreeObserver = mHost.getViewTreeObserver();
            if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
                if (mUnReadyPreDrawListener != null) {
                    viewTreeObserver.removeOnPreDrawListener(mUnReadyPreDrawListener);
                }
            }
        }
    }

    private void addUnReadyPreDrawListener() {
        if (mHost != null) {
            clearUnReadyPreDrawListener();
            ViewTreeObserver viewTreeObserver = mHost.getViewTreeObserver();
            if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
                mUnReadyPreDrawListener =
                        new ComponentPreDrawListener(this, mHost, mAnimatorSet, false);
                viewTreeObserver.addOnPreDrawListener(mUnReadyPreDrawListener);
            }
        }
    }

    private void clearReadyPreDrawListener() {
        if (mHost != null) {
            ViewTreeObserver viewTreeObserver = mHost.getViewTreeObserver();
            if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
                if (mReadyPreDrawListener != null) {
                    viewTreeObserver.removeOnPreDrawListener(mReadyPreDrawListener);
                }
            }
        }
    }

    private void addReadyPreDrawListener() {
        if (mHost != null) {
            clearReadyPreDrawListener();
            ViewTreeObserver viewTreeObserver = mHost.getViewTreeObserver();
            if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
                mReadyPreDrawListener =
                        new ComponentPreDrawListener(this, mHost, mAnimatorSet, true);
                viewTreeObserver.addOnPreDrawListener(mReadyPreDrawListener);
            }
        }
    }

    private void clearPreDrawListener() {
        clearReadyPreDrawListener();
        clearUnReadyPreDrawListener();
    }

    private void addGlobalLayoutListener() {
        if (mTransformLayoutListener == null) {
            mTransformLayoutListener = new ComponentOnGlobalLayoutListener(this);
        }
        // mHost.getViewTreeObserver可能会出现返回值不一致的情况，mViewTreeObserver用于缓存当前添加Listener对象
        // 每次addGlobalLayoutListener时需要将原来Listener进行移除
        removeGlobalLayoutListener();
        if (mHost != null) {
            mViewTreeObserver = mHost.getViewTreeObserver();
            if (mViewTreeObserver != null && mViewTreeObserver.isAlive()) {
                mViewTreeObserver.addOnGlobalLayoutListener(mTransformLayoutListener);
            } else {
                Log.e(TAG, "addGlobalLayoutListener fail: view tree observer not alive");
            }
        } else {
            Log.e(TAG, "addGlobalLayoutListener fail: mHost is null");
        }
    }

    private void removeGlobalLayoutListener() {
        if (mTransformLayoutListener != null) {
            if (mViewTreeObserver != null && mViewTreeObserver.isAlive()) {
                mViewTreeObserver.removeOnGlobalLayoutListener(mTransformLayoutListener);
            } else {
                Log.e(
                        TAG,
                        "removeGlobalLayoutListener fail: mViewTreeObserver is null or mViewTreeObserver not alive");
            }
        }
    }

    public Map<String, Boolean> getStateMap() {
        return mStateAttrs;
    }

    @Override
    public HybridView getHybridView() {
        Context context = mContext;
        if (context instanceof RuntimeActivity) {
            return ((RuntimeActivity) context).getHybridView();
        } else if (mHapEngine != null && (mHapEngine.isCardMode() || mHapEngine.isInsetMode())) {
            View hostView = getRootComponent().getHostView();
            if (hostView instanceof IHybridViewHolder) {
                return ((IHybridViewHolder) hostView).getHybridView();
            }
        }
        Log.e(TAG, "Unable to get hybrid view");
        return null;
    }

    public void setRealPadding() {

        int leftPadding = Math.round(getPadding(Spacing.LEFT) + getBorder(Spacing.LEFT));
        int topPadding = Math.round(getPadding(Spacing.TOP) + getBorder(Spacing.TOP));
        int rightPadding = Math.round(getPadding(Spacing.RIGHT) + getBorder(Spacing.RIGHT));
        int bottomPadding = Math.round(getPadding(Spacing.BOTTOM) + getBorder(Spacing.BOTTOM));

        if (mHost instanceof YogaLayout) {
            mNode.setPadding(YogaEdge.LEFT, leftPadding);
            mNode.setPadding(YogaEdge.TOP, topPadding);
            mNode.setPadding(YogaEdge.RIGHT, rightPadding);
            mNode.setPadding(YogaEdge.BOTTOM, bottomPadding);
        } else if (mHost != null) {
            int oldLeftPadding = mHost.getPaddingLeft();
            int oldTopPadding = mHost.getPaddingTop();
            int oldRightPadding = mHost.getPaddingRight();
            int oldBottomPadding = mHost.getPaddingBottom();
            // Bug fix:Android 5.0及以下长按时调用setPadding会导致复制粘贴框无法显示
            if (oldLeftPadding != leftPadding
                    || oldTopPadding != topPadding
                    || oldRightPadding != rightPadding
                    || oldBottomPadding != bottomPadding) {
                mHost.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
            }
        }
    }

    public String getState(String key) {
        CSSValues attributeMap = mStyleDomData.get(key);
        String applyState = State.NORMAL;
        if (attributeMap != null) {
            for (String state : mStateAttrs.keySet()) {
                Boolean stateAttr = mStateAttrs.get(state);
                if (stateAttr != null && stateAttr) {
                    Object attribute = attributeMap.get(state);
                    if (attribute != null) {
                        applyState = state;
                        break;
                    }
                }
            }
        }

        return applyState;
    }

    private ViewGroup.LayoutParams getLayoutParams() {
        if (mHost == null) {
            return null;
        }

        ViewGroup.LayoutParams lp = mHost.getLayoutParams();
        if (lp == null) {
            lp = generateDefaultLayoutParams();
        }

        return lp;
    }

    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new YogaLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public float getBorder(int edge) {
        if (mBackgroundComposer == null) {
            return 0f;
        }

        if (!FloatUtil.isUndefined(mBackgroundComposer.getBorderWidth(edge))) {
            return mBackgroundComposer.getBorderWidth(edge);
        }

        if (!FloatUtil.isUndefined(mBackgroundComposer.getBorderWidth(Spacing.ALL))) {
            return mBackgroundComposer.getBorderWidth(Spacing.ALL);
        }

        return 0f;
    }

    protected boolean setAttribute(String key, Object attribute) {
        tryTransit(key);
        switch (key) {
            case Attributes.Style.ID:
                parseId(attribute);
                return true;
            case Attributes.Style.WIDTH:
                String width = Attributes.getString(attribute, "");
                setWidth(width);
                return true;
            case Attributes.Style.HEIGHT:
                String height = Attributes.getString(attribute, "");
                setHeight(height);
                return true;
            case Attributes.Style.MIN_WIDTH:
            case Attributes.Style.MIN_HEIGHT:
            case Attributes.Style.MAX_WIDTH:
            case Attributes.Style.MAX_HEIGHT:
                String limitStr = Attributes.getString(attribute, "");
                setMinMaxWidthHeight(key, limitStr);
                return true;
            case Attributes.Style.PADDING:
            case Attributes.Style.PADDING_LEFT:
            case Attributes.Style.PADDING_TOP:
            case Attributes.Style.PADDING_RIGHT:
            case Attributes.Style.PADDING_BOTTOM:
                float padding = Attributes.getFloat(mHapEngine, attribute, 0);
                setPadding(key, padding);
                setPaddingExist(key, true);
                return true;
            case Attributes.Style.MARGIN:
            case Attributes.Style.MARGIN_LEFT:
            case Attributes.Style.MARGIN_TOP:
            case Attributes.Style.MARGIN_RIGHT:
            case Attributes.Style.MARGIN_BOTTOM:
                setMargin(key, attribute);
                return true;
            case Attributes.Style.FLEX:
                float flex = Attributes.getFloat(mHapEngine, attribute, 0);
                setFlex(flex);
                return true;
            case Attributes.Style.FLEX_GROW:
                float flexGrow = Attributes.getFloat(mHapEngine, attribute, 0);
                setFlexGrow(flexGrow);
                return true;
            case Attributes.Style.FLEX_SHRINK:
                float flexShrink = Attributes.getFloat(mHapEngine, attribute, 1);
                setFlexShrink(flexShrink);
                return true;
            case Attributes.Style.FLEX_BASIS:
                float flexBasis = Attributes.getFloat(mHapEngine, attribute, -1);
                setFlexBasis(flexBasis);
                return true;
            case Attributes.Style.ALIGN_SELF:
                String alignSelf = Attributes.getString(attribute, "auto");
                setAlignSelf(alignSelf);
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                String colorStr = Attributes.getString(attribute, "transparent");
                setBackgroundColor(colorStr);
                return true;
            case Attributes.Style.BACKGROUND_IMAGE:
                String backgroundImage = Attributes.getString(attribute, "transparent");
                setBackgroundImage(backgroundImage);
                return true;
            case Attributes.Style.BACKGROUND_SIZE:
                String backgroundSize = Attributes.getString(attribute, Attributes.ImageMode.NONE);
                setBackgroundSize(backgroundSize);
                return true;
            case Attributes.Style.BACKGROUND_REPEAT:
                String backgroundRepeatMode =
                        Attributes.getString(attribute, Attributes.RepeatMode.REPEAT);
                setBackgroundRepeat(backgroundRepeatMode);
                return true;
            case Attributes.Style.BACKGROUND_POSITION:
                String backgroundPositionMode =
                        Attributes.getString(attribute, Attributes.PositionMode.TOP_LEFT);
                setBackgroundPosition(backgroundPositionMode);
                return true;
            case Attributes.Style.BACKGROUND:
                String backgroundStr = Attributes.getString(attribute, "transparent");
                setBackground(backgroundStr);
                return true;
            case Attributes.Style.OPACITY:
                float opacity = Attributes.getFloat(mHapEngine, attribute, 1f);
                setOpacity(opacity);
                return true;
            case Attributes.Style.DISPLAY:
                String display = Attributes.getString(attribute, Attributes.Display.FLEX);
                setDisplay(display);
                return true;
            case Attributes.Style.SHOW:
                mShow = parseShowAttribute(attribute);
                mShowAttrInitialized = true;
                show(mShow);
                return true;
            case Attributes.Style.VISIBILITY:
                String visibility = Attributes.getString(attribute, Attributes.Visibility.VISIBLE);
                setVisibility(visibility);
                return true;
            case Attributes.Style.POSITION:
                String positionMode = Attributes.getString(attribute, Attributes.Position.RELATIVE);
                setPositionMode(positionMode);
                return true;
            case Attributes.Style.LEFT:
            case Attributes.Style.TOP:
            case Attributes.Style.RIGHT:
            case Attributes.Style.BOTTOM:
                float position = Attributes.getFloat(mHapEngine, attribute, FloatUtil.UNDEFINED);
                setPosition(key, position);
                return true;
            case Attributes.Style.BORDER_WIDTH:
            case Attributes.Style.BORDER_LEFT_WIDTH:
            case Attributes.Style.BORDER_TOP_WIDTH:
            case Attributes.Style.BORDER_RIGHT_WIDTH:
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
                float borderWidth = Attributes.getFloat(mHapEngine, attribute, 0f);
                setBorderWidth(key, borderWidth);
                return true;
            case Attributes.Style.BORDER_COLOR:
            case Attributes.Style.BORDER_LEFT_COLOR:
            case Attributes.Style.BORDER_TOP_COLOR:
            case Attributes.Style.BORDER_RIGHT_COLOR:
            case Attributes.Style.BORDER_BOTTOM_COLOR:
                String borderColorStr = Attributes.getString(attribute, "black");
                setBorderColor(key, borderColorStr);
                return true;
            case Attributes.Style.BORDER_STYLE:
                String borderStyle = Attributes.getString(attribute, "SOLID");
                setBorderStyle(borderStyle);
                return true;
            case Attributes.Style.BORDER_RADIUS:
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                String borderRadiusStr = Attributes.getString(attribute, "");
                setBorderRadius(key, borderRadiusStr);
                return true;
            case Attributes.Style.DISABLED:
                boolean disabled = Attributes.getBoolean(attribute, false);
                setDisabled(disabled);
                return true;
            case Attributes.Style.FOCUSABLE:
                boolean focusable = Attributes.getBoolean(attribute, false);
                setFocusable(focusable);
                return true;
            case Attributes.Style.ARIA_LABEL:
                String ariaLabel = Attributes.getString(attribute);
                setAriaLabel(ariaLabel);
                return true;
            case Attributes.Style.ARIA_UNFOCUSABLE:
                boolean ariaUnfocusable = Attributes.getBoolean(attribute, true);
                setAriaUnfocusable(ariaUnfocusable);
                return true;
            case Attributes.Style.TRANSFORM:
                mTransform = Transform.parse(mHapEngine, attribute);
                if (mTransform == null) {
                    // Parse error,apply default Transform.
                    mTransform = new Transform();
                }
                Transform.applyTransform(mTransform, mHost);
                return true;
            case Attributes.Style.TRANSFORM_ORIGIN:
                String originStr = Attributes.getString(attribute, "50% 50% 0");
                setTransformOrigin(originStr);
                return true;
            // animation start:
            case Attributes.Style.ANIMATION_DURATION:
                int duration = AnimationParser.getTime(Attributes.getString(attribute));
                getOrCreateCSSAnimatorSet().setDuration(duration);
                return true;
            case Attributes.Style.ANIMATION_TIMING_FUNCTION:
                String timing = Attributes.getString(attribute, "ease");
                getOrCreateCSSAnimatorSet()
                        .setKeyFrameInterpolator(TimingFactory.getTiming(timing));
                return true;
            case Attributes.Style.ANIMATION_DELAY:
                int delay = AnimationParser.getTime(Attributes.getString(attribute));
                getOrCreateCSSAnimatorSet().setDelay(delay);
                return true;
            case Attributes.Style.ANIMATION_ITERATION_COUNT:
                int repeatCount = Attributes.getInt(mHapEngine, attribute, 0);
                getOrCreateCSSAnimatorSet().setRepeatCount(repeatCount);
                return true;
            case Attributes.Style.ANIMATION_FILL_MODE:
                String fillMode = Attributes.getString(attribute, CSSAnimatorSet.FillMode.NONE);
                getOrCreateCSSAnimatorSet().setFillMode(fillMode);
                return true;
            case Attributes.Style.ANIMATION_KEYFRAMES:
                CSSAnimatorSet animatorSet =
                        AnimationParser.parse(
                                mHapEngine, mAnimatorSet, Attributes.getString(attribute, ""),
                                this);

                if (animatorSet != null && animatorSet.isReady()) {
                    mAnimatorSet = animatorSet;
                } else if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
                    mAnimatorSet.cancel();
                    mAnimatorSet.setDirty(false);
                }
                getOrCreateCSSAnimatorSet().setAttr(attribute);
                return true;
            case Attributes.Style.ANIMATION_DIRECTION:
                String direction = Attributes.getString(attribute, CSSAnimatorSet.Direction.NORMAL);
                getOrCreateCSSAnimatorSet().setDirection(direction);
                return true;
            // animation end.
            // transition start:
            case Attributes.Style.TRANSITION_DURATION:
                String dur = Attributes.getString(attribute, "0s");
                setTransitionDuration(dur);
                return true;
            case Attributes.Style.TRANSITION_TIMING_FUNCTION:
                String timingFunction = Attributes.getString(attribute, "ease");
                setTransitionTimingFunction(timingFunction);
                return true;
            case Attributes.Style.TRANSITION_DELAY:
                String startDelay = Attributes.getString(attribute, "0s");
                setTransitionDelay(startDelay);
                return true;
            case Attributes.Style.TRANSITION_PROPERTY:
                String property = Attributes.getString(attribute, "all");
                setTransitionProperty(property);
                return true;
            // transition end.
            case Attributes.Style.FORCE_DARK:
                boolean forceDark = Attributes.getBoolean(attribute, true);
                setForceDark(forceDark);
                return true;
            case Attributes.Style.AUTO_FOCUS:
                boolean autoFocus = Attributes.getBoolean(attribute, false);
                focus(autoFocus);
                return true;
            case Attributes.Style.OVERFLOW:
                String overflowValue =
                        Attributes.getString(attribute, Attributes.OverflowType.HIDDEN);
                setOverflow(overflowValue);
                return true;
            default:
                return false;
        }
    }

    private void setOverflow(String overflowValue) {
        if (TextUtils.isEmpty(overflowValue)) {
            return;
        }
        Container parent = getParent();
        if (parent == null || (parent = parent.getParent()) == null) {
            return;
        }
        boolean clipChildren = !Attributes.OverflowType.VISIBLE.equals(overflowValue);
        parent.setClipChildren(clipChildren);
    }

    private void setForceDark(boolean forceDark) {
        if (mHost == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mHost.setForceDarkAllowed(forceDark);
        }
    }

    private CSSAnimatorSet getOrCreateCSSAnimatorSet() {
        if (mAnimatorSet == null) {
            mAnimatorSet = new CSSAnimatorSet(mHapEngine, this);
        }
        return mAnimatorSet;
    }

    public HapEngine getHapEngine() {
        return mHapEngine;
    }

    public CSSTransitionSet getOrCreateTransitionSet() {
        if (mTransitionSet == null) {
            mTransitionSet = new CSSTransitionSet();
        }
        return mTransitionSet;
    }

    protected Component resolveSceneRootComponent() {
        if (mHost instanceof ViewGroup) {
            return this;
        } else if (mParent != null) {
            return mParent.resolveSceneRootComponent();
        } else {
            return null;
        }
    }

    public Component getSceneRootComponent() {
        return mSceneRootComponent;
    }

    protected void setTransitionProperty(String property) {
        if (mHost == null || TextUtils.isEmpty(property)) {
            return;
        }
        if (mSceneRootComponent == null) {
            mSceneRootComponent = resolveSceneRootComponent();
        }
        if (mSceneRootComponent != null) {
            boolean success =
                    mSceneRootComponent.getOrCreateTransitionSet().setProperties(mHost, property);
            isTransitionInitialized |= success;
        }
    }

    public void setTransitionDuration(String duration) {
        if (mHost == null || TextUtils.isEmpty(duration)) {
            return;
        }
        if (mSceneRootComponent == null) {
            mSceneRootComponent = resolveSceneRootComponent();
        }
        if (mSceneRootComponent != null) {
            mSceneRootComponent.getOrCreateTransitionSet().setDurations(mHost, duration);
        }
    }

    public void setTransitionDelay(String delay) {
        if (mHost == null || TextUtils.isEmpty(delay)) {
            return;
        }
        if (mSceneRootComponent == null) {
            mSceneRootComponent = resolveSceneRootComponent();
        }
        if (mSceneRootComponent != null) {
            mSceneRootComponent.getOrCreateTransitionSet().setDelays(mHost, delay);
        }
    }

    public void setTransitionTimingFunction(String timingFunction) {
        if (mHost == null || TextUtils.isEmpty(timingFunction)) {
            return;
        }
        if (mSceneRootComponent == null) {
            mSceneRootComponent = resolveSceneRootComponent();
        }
        if (mSceneRootComponent != null) {
            mSceneRootComponent.getOrCreateTransitionSet()
                    .setTimingFunctions(mHost, timingFunction);
        }
    }

    protected void tryTransit(String property) {
        if (!isTransitionInitialized || mSceneRootComponent == null || mHost == null) {
            return;
        }
        CSSTransitionSet transitionSet = mSceneRootComponent.getOrCreateTransitionSet();
        if (!transitionSet.isTransitionProperty(mHost, property)) {
            return;
        }
        ViewGroup sceneRootView = (ViewGroup) mSceneRootComponent.getHostView();
        if (sceneRootView == null) {
            return;
        }
        transitionSet.prepareProperty(property, mHost, sceneRootView);
    }

    private void setPaddingExist(String key, boolean isExist) {

        switch (key) {
            case Attributes.Style.PADDING:
                mPaddingExist[Spacing.LEFT] = isExist;
                mPaddingExist[Spacing.TOP] = isExist;
                mPaddingExist[Spacing.RIGHT] = isExist;
                mPaddingExist[Spacing.BOTTOM] = isExist;
                break;
            case Attributes.Style.PADDING_LEFT:
                mPaddingExist[Spacing.LEFT] = isExist;
                break;
            case Attributes.Style.PADDING_TOP:
                mPaddingExist[Spacing.TOP] = isExist;
                break;
            case Attributes.Style.PADDING_RIGHT:
                mPaddingExist[Spacing.RIGHT] = isExist;
                break;
            case Attributes.Style.PADDING_BOTTOM:
                mPaddingExist[Spacing.BOTTOM] = isExist;
                break;
            default:
                // Never get here.
                Log.e(TAG, "setPaddingExist: Wrong key.");
        }
    }

    protected Object getAttribute(String key) {
        switch (key) {
            case Attributes.Style.WIDTH:
                return getWidth();
            case Attributes.Style.HEIGHT:
                return getHeight();
            // padding
            case Attributes.Style.PADDING:
            case Attributes.Style.PADDING_LEFT:
            case Attributes.Style.PADDING_TOP:
            case Attributes.Style.PADDING_RIGHT:
            case Attributes.Style.PADDING_BOTTOM:
                return getPadding(key);

            // margin
            case Attributes.Style.MARGIN:
                int[] listM = {
                        getMargin(Attributes.Style.MARGIN_TOP),
                        getMargin(Attributes.Style.MARGIN_RIGHT),
                        getMargin(Attributes.Style.MARGIN_BOTTOM),
                        getMargin(Attributes.Style.MARGIN_LEFT)
                };
                return listM;
            case Attributes.Style.MARGIN_LEFT:
            case Attributes.Style.MARGIN_TOP:
            case Attributes.Style.MARGIN_RIGHT:
            case Attributes.Style.MARGIN_BOTTOM:
                return getMargin(key);

            // flex
            case Attributes.Style.FLEX:
                float[] listF = {getFlexGrow(), getFlexShrink(), getFlexBasis()};
                return listF;
            case Attributes.Style.FLEX_GROW:
                return getFlexGrow();
            case Attributes.Style.FLEX_SHRINK:
                return getFlexShrink();
            case Attributes.Style.FLEX_BASIS:
                return getFlexBasis();
            case Attributes.Style.ALIGN_SELF:
                return getAlignSelf();

            // background
            case Attributes.Style.BACKGROUND_COLOR:
                return getBackgroundColor();
            case Attributes.Style.BACKGROUND_IMAGE:
                return null;
            case Attributes.Style.OPACITY:
                return getOpacity();
            case Attributes.Style.DISPLAY:
                return getDisplay();
            case Attributes.Style.SHOW:
                return null;
            case Attributes.Style.VISIBILITY:
                return null;
            case Attributes.Style.POSITION:
                return null;
            case Attributes.Style.LEFT:
            case Attributes.Style.TOP:
            case Attributes.Style.RIGHT:
            case Attributes.Style.BOTTOM:
                return null;
            case Attributes.Style.BORDER_WIDTH:
                float[] listBW = {
                        getBorderWidth(Attributes.Style.BORDER_TOP_WIDTH),
                        getBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH),
                        getBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH),
                        getBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH)
                };
                return listBW;
            case Attributes.Style.BORDER_LEFT_WIDTH:
            case Attributes.Style.BORDER_TOP_WIDTH:
            case Attributes.Style.BORDER_RIGHT_WIDTH:
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
                return getBorderWidth(key);
            case Attributes.Style.BORDER_COLOR:
                String[] listBC = {
                        getBorderColor(Attributes.Style.BORDER_TOP_COLOR),
                        getBorderColor(Attributes.Style.BORDER_RIGHT_COLOR),
                        getBorderColor(Attributes.Style.BORDER_BOTTOM_COLOR),
                        getBorderColor(Attributes.Style.BORDER_LEFT_COLOR)
                };
                return listBC;
            case Attributes.Style.BORDER_LEFT_COLOR:
            case Attributes.Style.BORDER_TOP_COLOR:
            case Attributes.Style.BORDER_RIGHT_COLOR:
            case Attributes.Style.BORDER_BOTTOM_COLOR:
                return getBorderColor(key);
            case Attributes.Style.BORDER_STYLE:
                return getBorderStyle();
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                return null;
            case Attributes.Style.DISABLED:
                return isDisabled();
            // animation start:
            case Attributes.Style.ANIMATION_DURATION:
                return null;
            case Attributes.Style.ANIMATION_TIMING_FUNCTION:
                return null;
            case Attributes.Style.ANIMATION_DELAY:
                return null;
            case Attributes.Style.ANIMATION_ITERATION_COUNT:
                return null;
            case Attributes.Style.ANIMATION_FILL_MODE:
                return null;
            case Attributes.Style.ANIMATION_KEYFRAMES:
                return null;
            case Attributes.Style.ANIMATION_DIRECTION:
                return null;
            case Attributes.Style.TRANSFORM:
                return null;
            case Attributes.Style.TRANSFORM_ORIGIN:
                return null;
            // animation end.
            default:
                return null;
        }
    }

    public void bindEvents(Map<String, Boolean> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Boolean> entry : events.entrySet()) {
            String event = entry.getKey();
            boolean enable = entry.getValue();

            if (enable) {
                if (!mEventDomData.contains(event)) {
                    mEventDomData.add(event);
                    addEvent(event);
                }
            } else {
                if (mEventDomData.contains(event)) {
                    mEventDomData.remove(event);
                    removeEvent(event);
                }
            }
        }
    }

    @Override
    public void bindEvents(Set<String> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        mEventDomData.addAll(events);

        if (mHost != null) {
            applyEvents(events);

            for (OnDomDataChangeListener listener : mDomDataChangeListeners) {
                listener.onEventsChange(this, events, true);
            }
        }
    }

    public void applyEvents(Set<String> events) {
        if (events == null) {
            return;
        }

        for (String event : events) {
            addEvent(event);
        }
    }

    @Override
    public Set<String> getDomEvents() {
        return mEventDomData;
    }

    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.FOCUS.equals(event) ||
                Attributes.Event.BLUR.equals(event) ||
                Attributes.Event.CLICK.equals(event)) {
            mHost.setFocusable(true);
        }

        boolean result = false;
        if (Attributes.Event.FOCUS.equals(event) || Attributes.Event.BLUR.equals(event)) {
            if (Attributes.Event.FOCUS.equals(event)) {
                mFocusChangeListener =
                        new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    mCallback.onJsEventCallback(
                                            getPageId(), mRef, Attributes.Event.FOCUS,
                                            Component.this, null, null);
                                }
                            }
                        };
                addOnFocusChangeListener(mFocusChangeListener);
            }
            if (Attributes.Event.BLUR.equals(event)) {
                mBlurChangeListener =
                        new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (!hasFocus) {
                                    mCallback.onJsEventCallback(
                                            getPageId(), mRef, Attributes.Event.BLUR,
                                            Component.this, null, null);
                                }
                            }
                        };
                addOnFocusChangeListener(mBlurChangeListener);
            }
            result = true;
        } else if (Attributes.Event.APPEAR.equals(event)
                || Attributes.Event.DISAPPEAR.equals(event)) {
            Scrollable scroller = getParentScroller();
            if (Attributes.Event.APPEAR.equals(event) && scroller != null) {
                scroller.bindAppearEvent(this);
            }
            if (Attributes.Event.DISAPPEAR.equals(event) && scroller != null) {
                scroller.bindDisappearEvent(this);
            }
            result = true;
        } else if (Attributes.Event.SWIPE.equals(event)) {
            if (mSwipeDelegate == null) {
                mSwipeDelegate =
                        new SwipeDelegate(mHapEngine) {
                            @Override
                            public void onSwipe(Map<String, Object> direction) {
                                RenderEventCallback callback = mCallback;
                                if (callback != null) {
                                    callback.onJsEventCallback(
                                            getPageId(),
                                            getRef(),
                                            Attributes.Event.SWIPE,
                                            Component.this,
                                            direction,
                                            null);
                                }
                            }
                        };
            }
            addTouchListener(TOUCH_TYPE_SWIPE, mSwipeDelegate);
            result = true;
        } else if (isGestureEvent(event) && mHost instanceof GestureHost) {
            GestureHost gestureHost = (GestureHost) mHost;
            if (gestureHost.getGesture() == null) {
                gestureHost.setGesture(new GestureDelegate(mHapEngine, this, mContext));
            }
            if (Attributes.Event.LONGPRESS.equals(event)) {
                gestureHost.getGesture().setIsWatchingLongPress(true);
            }
            if (Attributes.Event.CLICK.equals(event)) {
                gestureHost.getGesture().setIsWatchingClick(true);
                setRegisterClickEvent(true);
            }
            if (mMinPlatformVersion < GestureDispatcher.MIN_BUBBLE_PLATFORM_VERSION) {
                // fix bug：list-item被回收再复用后，component没有刷新，保存的是旧的
                gestureHost.getGesture().updateComponent(this);
                gestureHost.getGesture().registerEvent(event);
                result = true;
            }
        } else if (Attributes.Event.RESIZE.equals(event)) {
            if (mResizeListener == null) {
                mResizeListener = new ResizeListener();
            }
            removeOnLayoutChangeListener(mResizeListener);
            addOnLayoutChangeListener(mResizeListener);
            result = true;
        } else if (Attributes.Event.KEY_EVENT.equals(event)) {
            mKeyEventRegister = true;
        } else if (isAnimationEvent(event)) {
            getOrCreateAnimatorEventListener().registerEvent(event);
            result = true;
        }

        return result;
    }

    public void applyHook(List<String> hooks){
        if (hooks == null || hooks.isEmpty()) {
            return;
        }
        mHookData.addAll(hooks);
    }

    public List<String> getHook(){
        return mHookData;
    }

    private boolean isListenFullscreenChange() {
        return getDomEvents().contains(Attributes.Event.FULLSCREEN_CHANGE);
    }

    public void onFullscreenChange(boolean fullscreen) {
        if (!isListenFullscreenChange()) {
            return;
        }
        Map<String, Object> params = new HashMap();
        params.put("fullscreen", fullscreen);
        mCallback.onJsEventCallback(
                getPageId(), mRef, Attributes.Event.FULLSCREEN_CHANGE, this, params, null);
    }

    private boolean isAnimationEvent(String event) {
        return Attributes.Event.ANIMATION_END.equals(event)
                || Attributes.Event.ANIMATION_START.equals(event)
                || Attributes.Event.ANIMATION_ITERATION.equals(event);
    }

    private boolean isGestureEvent(String event) {

        return Attributes.Event.TOUCH_START.equals(event)
                || Attributes.Event.TOUCH_MOVE.equals(event)
                || Attributes.Event.TOUCH_END.equals(event)
                || Attributes.Event.TOUCH_CANCEL.equals(event)
                || Attributes.Event.TOUCH_CLICK.equals(event)
                || Attributes.Event.TOUCH_LONG_PRESS.equals(event);
    }

    public void freezeEvent(String event) {
        if (!TextUtils.isEmpty(event) && mHost instanceof GestureHost) {
            GestureHost gestureHost = (GestureHost) mHost;
            if (gestureHost.getGesture() != null) {
                gestureHost.getGesture().addFrozenEvent(event);
            }
        }
    }

    public void unfreezeEvent(String event) {
        if (!TextUtils.isEmpty(event) && mHost instanceof GestureHost) {
            GestureHost gestureHost = (GestureHost) mHost;
            if (gestureHost.getGesture() != null) {
                gestureHost.getGesture().removeFrozenEvent(event);
            }
        }
    }

    @Override
    public void removeEvents(Set<String> events) {
        if (events == null) {
            return;
        }

        mEventDomData.removeAll(events);

        for (String event : events) {
            removeEvent(event);
        }

        for (OnDomDataChangeListener listener : mDomDataChangeListeners) {
            listener.onEventsChange(this, events, false);
        }
    }

    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        boolean result = false;
        if (Attributes.Event.CLICK.equals(event)) {
            mHost.setOnClickListener(null);
            result = true;
        } else if (Attributes.Event.FOCUS.equals(event) || Attributes.Event.BLUR.equals(event)) {
            if (Attributes.Event.FOCUS.equals(event) && mFocusChangeListener != null) {
                removeOnFocusChangeListener(mFocusChangeListener);
                mFocusChangeListener = null;
            }
            if (Attributes.Event.BLUR.equals(event) && mBlurChangeListener != null) {
                removeOnFocusChangeListener(mBlurChangeListener);
                mBlurChangeListener = null;
            }
            result = true;
        } else if (Attributes.Event.APPEAR.equals(event)
                || Attributes.Event.DISAPPEAR.equals(event)) {
            Scrollable scroller = getParentScroller();
            if (Attributes.Event.APPEAR.equals(event) && scroller != null) {
                scroller.unbindAppearEvent(this);
            }
            if (Attributes.Event.DISAPPEAR.equals(event) && scroller != null) {
                scroller.unbindDisappearEvent(this);
            }
            result = true;
        } else if (Attributes.Event.SWIPE.equals(event)) {
            removeTouchListener(TOUCH_TYPE_SWIPE);
            mSwipeDelegate = null;
            result = true;
        } else if (isGestureEvent(event)) {
            if (mHost instanceof GestureHost) {
                GestureHost gestureHost = (GestureHost) mHost;
                IGesture gesture = gestureHost.getGesture();
                if (gesture != null) {
                    gesture.removeEvent(event);
                    if (Attributes.Event.LONGPRESS.equals(event)) {
                        gesture.setIsWatchingLongPress(false);
                    } else if (Attributes.Event.CLICK.equals(event)) {
                        gesture.setIsWatchingClick(false);
                        setRegisterClickEvent(false);
                    }
                }
            }
            result = true;
        } else if (Attributes.Event.RESIZE.equals(event)) {
            removeOnLayoutChangeListener(mResizeListener);
            mResizeListener = null;
            result = true;
        } else if (isAnimationEvent(event)) {
            if (mAnimatorEventListener != null) {
                mAnimatorEventListener.unregisterEvent(event);
            }
            result = true;
        }

        return result;
    }

    public final void performSaveInstanceState(Map<String, Object> outState) {
        onSaveInstanceState(outState);
    }

    public final void performRestoreInstanceState(Map<String, Object> savedState) {
        onRestoreInstanceState(savedState);
    }

    public void destroy() {
        mBoundRecyclerItem = null;
        if (mBackgroundComposer != null) {
            mBackgroundComposer.destroy();
        }
        if (mCssNode != null) {
            mCssNode.clearPseudoListener();
        }
        mCssNode = null;
        if (!mAnimations.isEmpty()) {
            for (String key : mAnimations.keySet()) {
                Animation animation = mAnimations.get(key);
                if (animation != null) {
                    animation.onDestroy();
                }
            }
        }
        clearPreDrawListener();
        if(mHookData != null){
            mHookData.clear();
        }

        if (mAnimatorEventListener != null) {
            mAnimatorEventListener.unregisterAllEvents();
            mAnimatorEventListener = null;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.destroy();
            mAnimatorSet = null;
        }
        if (mTransitionSet != null) {
            mTransitionSet.destroy();
            mTransitionSet = null;
            mSceneRootComponent = null;
        }
        Scrollable scroller = getParentScroller();
        if (scroller != null) {
            scroller.unbindAppearEvent(this);
            scroller.unbindDisappearEvent(this);
        }
        removeGlobalLayoutListener();
        if (mTouchListeners != null) {
            mTouchListeners.clear();
        }
    }

    protected void onSaveInstanceState(Map<String, Object> outState) {
        if (outState == null) {
            return;
        }
        if (mResizeListener != null) {
            outState.put(LAYOUT_DATA, mResizeListener.getLayout());
        }
    }

    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        // restore;
        if (savedState == null) {
            return;
        }
        if (mResizeListener != null) {
            Rect layout = (Rect) savedState.get(LAYOUT_DATA);
            mResizeListener.setLayout(layout);
        }
        savedState.clear();
    }

    public boolean isComponentAdaptiveEnable() {
        if (mContext == null || sSysOpProvider == null) {
            return false;
        }
        return sSysOpProvider.isFoldableDevice(mContext)
                && !sSysOpProvider.isFoldStatusByDisplay(mContext) && FoldingUtils.isAdaptiveScreenMode();
    }

    public int getWidth() {
        if (mHost == null || mHost.getLayoutParams() == null) {
            return IntegerUtil.UNDEFINED;
        }
        return mHost.getLayoutParams().width;
    }

    public void setWidth(String widthStr) {

        if (mHost == null) {
            return;
        }

        ViewGroup.LayoutParams lp = getLayoutParams();
        mHost.setLayoutParams(lp);

        if (TextUtils.isEmpty(widthStr)) {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            mWidthDefined = false;
            if (mNode != null) {
                mNode.setWidth(YogaConstants.UNDEFINED);
            }
            return;
        }

        if (widthStr.endsWith(Metrics.PERCENT)) {
            String temp = widthStr.trim();
            temp = temp.substring(0, temp.indexOf(Metrics.PERCENT));
            mPercentWidth = FloatUtil.parse(temp) / 100f;
            if (FloatUtil.isUndefined(mPercentWidth)) {
                Log.e(TAG, "set width value is error：" + widthStr);
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mWidthDefined = false;
                if (mNode != null) {
                    mNode.setWidth(YogaConstants.UNDEFINED);
                }
                return;
            }

            if (mNode != null) {
                mNode.setWidthPercent(mPercentWidth * 100);
            }

            // 如果parent不是yogalayout，yoganode设置百分比没有意义
            if (FloatUtil.floatsEqual(mPercentWidth, 1) && !(mParent.mHost instanceof YogaLayout)) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        } else {
            mPercentWidth = -1;
            int width =
                    Attributes.getInt(mHapEngine, widthStr, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (isComponentAdaptiveEnable()) {
                mAdaptiveBeforeWidth = width;
                width = getAdapterWidth(width);
            }
            lp.width = width;
            if (mNode != null) {
                mNode.setWidth(width);
            }
        }
        mWidthDefined = true;
    }

    private int getAdapterWidth(int width) {
        int foldWidth = sSysOpProvider.getFoldDisplayWidth(mContext.getApplicationContext());
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        if (width > (foldWidth / 2)) {
            int adapterMargin = foldWidth - width;
            boolean isLandscapeMode = DisplayUtil.isLandscapeMode(mContext);
            if (isLandscapeMode) {
                return displayMetrics.widthPixels + sSysOpProvider.getSafeAreaWidth(mContext) - adapterMargin;
            } else {
                return displayMetrics.widthPixels - adapterMargin;
            }
        }
        return width;
    }

    public int getHeight() {
        if (mHost == null || mHost.getLayoutParams() == null) {
            return IntegerUtil.UNDEFINED;
        }
        return mHost.getLayoutParams().height;
    }

    public void setHeight(String heightStr) {
        if (mHost == null) {
            return;
        }

        ViewGroup.LayoutParams lp = getLayoutParams();
        mHost.setLayoutParams(lp);

        if (TextUtils.isEmpty(heightStr)) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mHeightDefined = false;
            if (mNode != null) {
                mNode.setHeight(YogaConstants.UNDEFINED);
            }
            return;
        }

        if (heightStr.endsWith(Metrics.PERCENT)) {
            String temp = heightStr.trim();
            temp = temp.substring(0, temp.indexOf(Metrics.PERCENT));
            mPercentHeight = FloatUtil.parse(temp) / 100f;
            if (FloatUtil.isUndefined(mPercentHeight)) {
                Log.e(TAG, "set height value is error: " + heightStr);
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mHeightDefined = false;
                if (mNode != null) {
                    mNode.setHeight(YogaConstants.UNDEFINED);
                }
                return;
            }
            if (mNode != null) {
                mNode.setHeightPercent(mPercentHeight * 100);
            }
            // 如果parent不是yogalayout，yoganode设置百分比没有意义
            if (FloatUtil.floatsEqual(mPercentHeight, 1)
                    && !(mParent.mHost instanceof YogaLayout)) {
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        } else {
            mPercentHeight = -1;
            int height =
                    Attributes.getInt(mHapEngine, heightStr, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.height = height;
            if (mNode != null) {
                mNode.setHeight(height);
            }
        }
        mHeightDefined = true;
    }

    public void setMinMaxWidthHeight(String key, String limitStr) {
        if (mSizeLimitMetrics == null) {
            mSizeLimitMetrics = new Metrics();
        }
        mSizeLimitMetrics.applyMetrics(mHapEngine, key, limitStr, mNode);
    }

    public void setPadding(String position, float padding) {

        if (padding < 0) {
            return;
        }
        if (mPadding == null) {
            mPadding = new Spacing(FloatUtil.UNDEFINED);
        }

        switch (position) {
            case Attributes.Style.PADDING:
                mPadding.set(Spacing.ALL, padding);
                break;
            case Attributes.Style.PADDING_LEFT:
                mPadding.set(Spacing.LEFT, padding);
                break;
            case Attributes.Style.PADDING_TOP:
                mPadding.set(Spacing.TOP, padding);
                break;
            case Attributes.Style.PADDING_RIGHT:
                mPadding.set(Spacing.RIGHT, padding);
                break;
            case Attributes.Style.PADDING_BOTTOM:
                mPadding.set(Spacing.BOTTOM, padding);
                break;
            default:
                break;
        }
    }

    public void setPadding(int edge, float padding) {
        if (padding < 0) {
            return;
        }
        if (mPadding == null) {
            mPadding = new Spacing(FloatUtil.UNDEFINED);
        }
        switch (edge) {
            case Spacing.ALL:
            case Spacing.LEFT:
            case Spacing.TOP:
            case Spacing.RIGHT:
            case Spacing.BOTTOM:
                mPadding.set(edge, padding);
                break;
            default:
                break;
        }
    }

    public float getPadding(String position) {
        if (mHost == null) {
            return FloatUtil.UNDEFINED;
        }

        int spacing;
        switch (position) {
            case Attributes.Style.PADDING:
                spacing = Spacing.ALL;
                break;
            case Attributes.Style.PADDING_LEFT:
                spacing = Spacing.LEFT;
                break;
            case Attributes.Style.PADDING_TOP:
                spacing = Spacing.TOP;
                break;
            case Attributes.Style.PADDING_RIGHT:
                spacing = Spacing.RIGHT;
                break;
            case Attributes.Style.PADDING_BOTTOM:
                spacing = Spacing.BOTTOM;
                break;
            default:
                spacing = IntegerUtil.UNDEFINED;
        }

        return getPadding(spacing);
    }

    public float getPadding(int edge) {
        if (mPadding != null) {
            if (!FloatUtil.isUndefined(mPadding.get(edge))) {
                return mPadding.get(edge);
            }

            if (!FloatUtil.isUndefined(mPadding.get(Spacing.ALL))) {
                return mPadding.get(Spacing.ALL);
            }
        }

        return 0;
    }

    public void setMargin(String position, Object marginAttr) {
        if (mHost == null || mNode == null) {
            return;
        }

        if (mPosition == null) {
            mPosition = new Position();
        }
        mPosition.setMargin(position, marginAttr);

        if (!tryInstallMarginLayoutParams()) {
            // not support margin
            Log.d(TAG, "setMargin failed, not supported");
            return;
        }

        String display = getCurStateStyleString(Attributes.Style.DISPLAY, Attributes.Display.FLEX);

        final boolean isParentYogaLayout = isParentYogaLayout();
        boolean marginAuto = false;
        int margin = 0;
        if (Attributes.Style.MARGIN_AUTO.equals(marginAttr) && isParentYogaLayout) {
            marginAuto = true;
        } else {
            margin = Attributes.getInt(mHapEngine, marginAttr, 0);
        }

        switch (position) {
            case Attributes.Style.MARGIN:
                if (isParentYogaLayout) {
                    if (!Attributes.Display.NONE.equals(display)) {
                        if (marginAuto) {
                            mNode.setMarginAuto(YogaEdge.ALL);
                        } else {
                            mNode.setMargin(YogaEdge.ALL, margin);
                        }
                    }
                } else {
                    ViewGroup.MarginLayoutParams marginLayoutParams =
                            (ViewGroup.MarginLayoutParams) getLayoutParams();
                    if (marginLayoutParams != null) {
                        marginLayoutParams.setMargins(margin, margin, margin, margin);
                    }
                }
                break;
            case Attributes.Style.MARGIN_LEFT:
                if (isParentYogaLayout) {
                    if (!Attributes.Display.NONE.equals(display)) {
                        if (marginAuto) {
                            mNode.setMarginAuto(YogaEdge.LEFT);
                        } else {
                            mNode.setMargin(YogaEdge.LEFT, margin);
                        }
                    }
                } else {
                    ViewGroup.MarginLayoutParams marginLayoutParams =
                            (ViewGroup.MarginLayoutParams) getLayoutParams();
                    if (marginLayoutParams != null) {
                        marginLayoutParams.leftMargin = margin;
                    }
                }
                break;
            case Attributes.Style.MARGIN_TOP:
                if (isParentYogaLayout) {
                    if (!Attributes.Display.NONE.equals(display)) {
                        if (marginAuto) {
                            mNode.setMarginAuto(YogaEdge.TOP);
                        } else {
                            mNode.setMargin(YogaEdge.TOP, margin);
                        }
                    }
                } else {
                    ViewGroup.MarginLayoutParams marginLayoutParams =
                            (ViewGroup.MarginLayoutParams) getLayoutParams();
                    if (marginLayoutParams != null) {
                        marginLayoutParams.topMargin = margin;
                    }
                }
                break;
            case Attributes.Style.MARGIN_RIGHT:
                if (isParentYogaLayout) {
                    if (!Attributes.Display.NONE.equals(display)) {
                        if (marginAuto) {
                            mNode.setMarginAuto(YogaEdge.RIGHT);
                        } else {
                            mNode.setMargin(YogaEdge.RIGHT, margin);
                        }
                    }
                } else {
                    ViewGroup.MarginLayoutParams marginLayoutParams =
                            (ViewGroup.MarginLayoutParams) getLayoutParams();
                    if (marginLayoutParams != null) {
                        marginLayoutParams.rightMargin = margin;
                    }
                }
                break;
            case Attributes.Style.MARGIN_BOTTOM:
                if (isParentYogaLayout) {
                    if (!Attributes.Display.NONE.equals(display)) {
                        if (marginAuto) {
                            mNode.setMarginAuto(YogaEdge.BOTTOM);
                        } else {
                            mNode.setMargin(YogaEdge.BOTTOM, margin);
                        }
                    }
                } else {
                    ViewGroup.MarginLayoutParams marginLayoutParams =
                            (ViewGroup.MarginLayoutParams) getLayoutParams();
                    if (marginLayoutParams != null) {
                        marginLayoutParams.bottomMargin = margin;
                    }
                }
                break;
            default:
                break;
        }

        mHost.setLayoutParams(getLayoutParams());

        // yoga bug fix: align-self should not be AUTO or STRETCH when margin is auto in cross size.
        if (isParentYogaLayout) {
            YogaNode parentNode = mNode.getParent();
            if (parentNode == null) {
                return;
            }
            YogaFlexDirection direction = parentNode.getFlexDirection();
            if (mNode.getMargin(YogaEdge.ALL).unit == YogaUnit.AUTO
                    || ((mNode.getMargin(YogaEdge.LEFT).unit == YogaUnit.AUTO
                    || mNode.getMargin(YogaEdge.RIGHT).unit == YogaUnit.AUTO
                    || mNode.getMargin(YogaEdge.START).unit == YogaUnit.AUTO
                    || mNode.getMargin(YogaEdge.END).unit == YogaUnit.AUTO)
                    && (direction == YogaFlexDirection.COLUMN
                    || direction == YogaFlexDirection.COLUMN_REVERSE))
                    || ((mNode.getMargin(YogaEdge.TOP).unit == YogaUnit.AUTO
                    || mNode.getMargin(YogaEdge.BOTTOM).unit == YogaUnit.AUTO)
                    && (direction == YogaFlexDirection.ROW
                    || direction == YogaFlexDirection.ROW_REVERSE))) {
                mNode.setAlignSelf(YogaAlign.FLEX_START);
            } else {
                setAlignSelf(
                        getCurStateStyleString(Attributes.Style.ALIGN_SELF, Attributes.Align.AUTO));
            }
        }
    }

    public int getMargin(String position) {
        if (mHost == null || mHost.getLayoutParams() == null) {
            return IntegerUtil.UNDEFINED;
        }

        ViewGroup.LayoutParams lp = getLayoutParams();
        boolean isMarginLp = (lp instanceof ViewGroup.MarginLayoutParams);
        switch (position) {
            case Attributes.Style.MARGIN_LEFT:
                if (isParentYogaLayout()) {
                    return Math.round(mNode.getMargin(YogaEdge.LEFT).value);
                } else if (isMarginLp) {
                    return ((ViewGroup.MarginLayoutParams) lp).leftMargin;
                } else {
                    return 0;
                }
            case Attributes.Style.MARGIN_TOP:
                if (isParentYogaLayout()) {
                    return Math.round(mNode.getMargin(YogaEdge.TOP).value);
                } else if (isMarginLp) {
                    return ((ViewGroup.MarginLayoutParams) lp).topMargin;
                } else {
                    return 0;
                }
            case Attributes.Style.MARGIN_RIGHT:
                if (isParentYogaLayout()) {
                    return Math.round(mNode.getMargin(YogaEdge.RIGHT).value);
                } else if (isMarginLp) {
                    return ((ViewGroup.MarginLayoutParams) lp).rightMargin;
                } else {
                    return 0;
                }
            case Attributes.Style.MARGIN_BOTTOM:
                if (isParentYogaLayout()) {
                    return Math.round(mNode.getMargin(YogaEdge.BOTTOM).value);
                } else if (isMarginLp) {
                    return ((ViewGroup.MarginLayoutParams) lp).bottomMargin;
                } else {
                    return 0;
                }
            default:
                return IntegerUtil.UNDEFINED;
        }
    }

    public void setFlex(float flex) {
        if (mHost == null || mNode == null) {
            return;
        }
        setFlexGrow(flex);
        setFlexShrink(1);
        setFlexBasis(0);
    }

    public float getFlexGrow() {
        if (mHost == null || mNode == null) {
            return FloatUtil.UNDEFINED;
        }
        return mNode.getFlexGrow();
    }

    public void setFlexGrow(float flexGrow) {
        if (mHost == null || mNode == null) {
            return;
        }
        mNode.setFlexGrow(flexGrow);
    }

    public float getFlexShrink() {
        if (mHost == null || mNode == null) {
            return FloatUtil.UNDEFINED;
        }
        return mNode.getFlexShrink();
    }

    public void setFlexShrink(float flexShrink) {
        if (mHost == null || mNode == null) {
            return;
        }
        mNode.setFlexShrink(flexShrink);
    }

    public float getFlexBasis() {
        if (mHost == null || mNode == null) {
            return FloatUtil.UNDEFINED;
        }
        return mNode.getFlexBasis().value;
    }

    public void setFlexBasis(float flexBasis) {
        if (mHost == null || mNode == null) {
            return;
        }

        boolean flexBasisUnnecessary = isFlexBasisUnnecessary();
        if (flexBasisUnnecessary) {
            mNode.setFlexBasis(YogaConstants.UNDEFINED);
            return;
        }

        mNode.setFlexBasis(flexBasis);
    }

    public String getAlignSelf() {
        if (mHost == null || mNode == null) {
            return null;
        }

        YogaAlign alignSelf = mNode.getAlignSelf();
        switch (alignSelf) {
            case FLEX_START:
                return Attributes.Align.FLEX_START;
            case CENTER:
                return Attributes.Align.CENTER;
            case FLEX_END:
                return Attributes.Align.FLEX_END;
            case STRETCH:
                return Attributes.Align.STRETCH;
            case BASELINE:
                return Attributes.Align.BASELINE;
            case AUTO:
            default:
                return Attributes.Align.AUTO;
        }
    }

    public void setAlignSelf(String alignSelfStr) {
        if (mHost == null || mNode == null) {
            return;
        }

        YogaAlign alignSelf;
        switch (alignSelfStr) {
            case Attributes.Align.FLEX_START:
                alignSelf = YogaAlign.FLEX_START;
                break;
            case Attributes.Align.CENTER:
                alignSelf = YogaAlign.CENTER;
                break;
            case Attributes.Align.FLEX_END:
                alignSelf = YogaAlign.FLEX_END;
                break;
            case Attributes.Align.STRETCH:
                alignSelf = YogaAlign.STRETCH;
                break;
            case Attributes.Align.BASELINE:
                alignSelf = YogaAlign.BASELINE;
                break;
            case Attributes.Align.AUTO:
            default:
                alignSelf = YogaAlign.AUTO;
                break;
        }
        mNode.setAlignSelf(alignSelf);
    }

    public ComponentBackgroundComposer getOrCreateBackgroundComposer() {
        if (mBackgroundComposer == null) {
            mBackgroundComposer = new ComponentBackgroundComposer(this);
        }
        return mBackgroundComposer;
    }

    public void invalidBackground() {
        if (mBackgroundComposer != null) {
            mBackgroundComposer.invalid();
        }
    }

    public void applyBackground() {
        if (mBackgroundComposer != null) {
            mBackgroundComposer.apply();
        }
    }

    public void setBackground(String background) {
        if (mHost == null) {
            return;
        }
        getOrCreateBackgroundComposer().setBackground(background);
    }

    public int getBackgroundColor() {
        if (mHost == null || mBackgroundComposer == null) {
            return IntegerUtil.UNDEFINED;
        }
        return mBackgroundComposer.getBackgroundColor();
    }

    public void setBackgroundColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }
        getOrCreateBackgroundComposer().setBackgroundColor(colorStr);
    }

    public void setBackgroundColor(int color) {
        if (mHost == null) {
            return;
        }
        getOrCreateBackgroundComposer().setBackgroundColor(color);
    }

    public void setBackgroundImage(String backgroundImage) {
        setBackgroundImage(backgroundImage, false);
    }

    public void setBackgroundImage(String backgroundImage, boolean setBlur) {
        getOrCreateBackgroundComposer().setBackgroundImage(backgroundImage, setBlur);
    }

    public void setBackgroundSize(String backgroundSize) {
        getOrCreateBackgroundComposer().setBackgroundSize(backgroundSize);
    }

    public void setBackgroundRepeat(String backgroundRepeat) {
        getOrCreateBackgroundComposer().setBackgroundRepeat(backgroundRepeat);
    }

    public void setBackgroundPosition(String backgroundPosition) {
        getOrCreateBackgroundComposer().setBackgroundPosition(backgroundPosition);
    }

    public void refreshPaddingFromBackground(NinePatchDrawable drawable) {
        Rect padding = new Rect();
        if (drawable.getPadding(padding)) {
            if (!mPaddingExist[Spacing.LEFT]) {
                setPadding(Attributes.Style.PADDING_LEFT, padding.left);
            }
            if (!mPaddingExist[Spacing.TOP]) {
                setPadding(Attributes.Style.PADDING_TOP, padding.top);
            }
            if (!mPaddingExist[Spacing.RIGHT]) {
                setPadding(Attributes.Style.PADDING_RIGHT, padding.right);
            }
            if (!mPaddingExist[Spacing.BOTTOM]) {
                setPadding(Attributes.Style.PADDING_BOTTOM, padding.bottom);
            }

            setRealPadding();
            invalidateYogaLayout();
        }
    }

    public float getOpacity() {
        if (mHost == null) {
            return FloatUtil.UNDEFINED;
        }
        return mHost.getAlpha();
    }

    public void setOpacity(float opacity) {
        if (FloatUtil.isUndefined(opacity)
                || opacity < 0
                || opacity > 1
                || mHost == null
                || FloatUtil.floatsEqual(mHost.getAlpha(), opacity)) {
            return;
        }
        mHost.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mHost.setAlpha(opacity);
    }

    public void setBorderWidth(String position, float width) {
        if (FloatUtil.isUndefined(width) || width < 0 || mHost == null) {
            return;
        }

        width = width > 0 && width < 1 ? 1 : width;

        ComponentBackgroundComposer backgroundComposer = getOrCreateBackgroundComposer();
        switch (position) {
            case Attributes.Style.BORDER_WIDTH:
                backgroundComposer.setBorderWidth(Spacing.ALL, width);
                break;
            case Attributes.Style.BORDER_LEFT_WIDTH:
                backgroundComposer.setBorderWidth(Spacing.LEFT, width);
                break;
            case Attributes.Style.BORDER_TOP_WIDTH:
                backgroundComposer.setBorderWidth(Spacing.TOP, width);
                break;
            case Attributes.Style.BORDER_RIGHT_WIDTH:
                backgroundComposer.setBorderWidth(Spacing.RIGHT, width);
                break;
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
                backgroundComposer.setBorderWidth(Spacing.BOTTOM, width);
                break;
            default:
                break;
        }
    }

    public float getBorderWidth(String position) {
        if (mHost == null || mBackgroundComposer == null) {
            return FloatUtil.UNDEFINED;
        }

        switch (position) {
            case Attributes.Style.BORDER_WIDTH:
                return mBackgroundComposer.getBorderWidth(Spacing.ALL);
            case Attributes.Style.BORDER_LEFT_WIDTH:
                return mBackgroundComposer.getBorderWidth(Spacing.LEFT);
            case Attributes.Style.BORDER_TOP_WIDTH:
                return mBackgroundComposer.getBorderWidth(Spacing.TOP);
            case Attributes.Style.BORDER_RIGHT_WIDTH:
                return mBackgroundComposer.getBorderWidth(Spacing.RIGHT);
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
                return mBackgroundComposer.getBorderWidth(Spacing.BOTTOM);
            default:
                return FloatUtil.UNDEFINED;
        }
    }

    public void setBorderColor(String position, String borderColorStr) {
        if (TextUtils.isEmpty(borderColorStr) || mHost == null) {
            return;
        }
        final int color = ColorUtil.getColor(borderColorStr);

        ComponentBackgroundComposer backgroundComposer = getOrCreateBackgroundComposer();
        switch (position) {
            case Attributes.Style.BORDER_COLOR:
                backgroundComposer.setBorderColor(Edge.ALL, color);
                break;
            case Attributes.Style.BORDER_LEFT_COLOR:
                backgroundComposer.setBorderColor(Edge.LEFT, color);
                break;
            case Attributes.Style.BORDER_TOP_COLOR:
                backgroundComposer.setBorderColor(Edge.TOP, color);
                break;
            case Attributes.Style.BORDER_RIGHT_COLOR:
                backgroundComposer.setBorderColor(Edge.RIGHT, color);
                break;
            case Attributes.Style.BORDER_BOTTOM_COLOR:
                backgroundComposer.setBorderColor(Edge.BOTTOM, color);
                break;
            default:
                break;
        }
    }

    public String getBorderColor(String position) {
        if (mHost == null || mBackgroundComposer == null) {
            return null;
        }
        int borderColor = 0;

        switch (position) {
            case Attributes.Style.BORDER_COLOR:
                borderColor = mBackgroundComposer.getBorderColor(Edge.ALL);
                break;
            case Attributes.Style.BORDER_LEFT_COLOR:
                borderColor = mBackgroundComposer.getBorderColor(Edge.LEFT);
                break;
            case Attributes.Style.BORDER_TOP_COLOR:
                borderColor = mBackgroundComposer.getBorderColor(Edge.TOP);
                break;
            case Attributes.Style.BORDER_RIGHT_COLOR:
                borderColor = mBackgroundComposer.getBorderColor(Edge.RIGHT);
                break;
            case Attributes.Style.BORDER_BOTTOM_COLOR:
                borderColor = mBackgroundComposer.getBorderColor(Edge.BOTTOM);
                break;
            default:
                break;
        }
        return ColorUtil.getColorStr(borderColor);
    }

    public String getBorderStyle() {
        if (mHost == null || mBackgroundComposer == null) {
            return null;
        }
        return mBackgroundComposer.getBorderStyle();
    }

    public void setBorderStyle(String borderStyle) {
        if (TextUtils.isEmpty(borderStyle) || mHost == null) {
            return;
        }
        getOrCreateBackgroundComposer().setBorderStyle(borderStyle);
    }

    public void setBorderRadiusPercent(String position, float borderRadiusPercent) {
        if (FloatUtil.isUndefined(borderRadiusPercent) || borderRadiusPercent < 0
                || mHost == null) {
            return;
        }

        ComponentBackgroundComposer backgroundComposer = getOrCreateBackgroundComposer();
        switch (position) {
            case Attributes.Style.BORDER_RADIUS:
                backgroundComposer.setBorderRadiusPercent(borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
                backgroundComposer
                        .setBorderCornerRadiiPercent(Corner.TOP_LEFT, borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
                backgroundComposer
                        .setBorderCornerRadiiPercent(Corner.TOP_RIGHT, borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
                backgroundComposer
                        .setBorderCornerRadiiPercent(Corner.BOTTOM_LEFT, borderRadiusPercent);
                break;
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                backgroundComposer
                        .setBorderCornerRadiiPercent(Corner.BOTTOM_RIGHT, borderRadiusPercent);
                break;
            default:
                break;
        }
    }

    public void setBorderRadius(String position, String borderRadiusStr) {
        // borderRadiusStr 为空串""时认为是0才对
        if (mHost == null || borderRadiusStr == null) {
            return;
        }

        String temp = borderRadiusStr.trim();
        if (borderRadiusStr.endsWith(Metrics.PERCENT)) {
            temp = temp.substring(0, temp.indexOf(Metrics.PERCENT));
            float borderRadiusPercent;
            try {
                borderRadiusPercent = Float.parseFloat(temp) / 100f;
            } catch (Exception e) {
                borderRadiusPercent = 0f;
            }
            setBorderRadiusPercent(position, borderRadiusPercent);
        } else {
            float borderRadius = Attributes.getFloat(mHapEngine, borderRadiusStr, 0f);
            setBorderRadius(position, borderRadius);
        }
    }

    public void setBorderRadius(String position, float borderRadius) {
        if (FloatUtil.isUndefined(borderRadius) || borderRadius < 0 || mHost == null) {
            return;
        }

        ComponentBackgroundComposer backgroundComposer = getOrCreateBackgroundComposer();
        switch (position) {
            case Attributes.Style.BORDER_RADIUS:
                backgroundComposer.setBorderRadius(borderRadius);
                break;
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
                backgroundComposer.setBorderCornerRadii(Corner.TOP_LEFT, borderRadius);
                break;
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
                backgroundComposer.setBorderCornerRadii(Corner.TOP_RIGHT, borderRadius);
                break;
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
                backgroundComposer.setBorderCornerRadii(Corner.BOTTOM_LEFT, borderRadius);
                break;
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                backgroundComposer.setBorderCornerRadii(Corner.BOTTOM_RIGHT, borderRadius);
                break;
            default:
                break;
        }
    }

    public float getBorderRadius() {
        CSSBackgroundDrawable cssBackground = getOrCreateCSSBackground();
        if (cssBackground != null) {
            return cssBackground.getRadius();
        } else {
            return 0;
        }
    }

    public void setFocusable(boolean focusable) {
        if (mHost == null) {
            return;
        }
        mHost.setFocusable(focusable);
        mHost.setFocusableInTouchMode(focusable);
    }

    public boolean isDisabled() {
        if (mHost == null) {
            Log.w(TAG, "isDisabled: mHost is null");
            return true;
        }
        return !mHost.isEnabled();
    }

    public void setDisabled(boolean disabled) {
        if (mHost == null) {
            return;
        }
        mHost.setEnabled(!disabled);
    }

    public void setAriaLabel(String ariaLabel) {
        if (mHost == null) {
            return;
        }

        mHost.setContentDescription(ariaLabel);
    }

    public void setAriaUnfocusable(boolean unfocusable) {
        if (mHost == null) {
            return;
        }
        int mode =
                unfocusable ? View.IMPORTANT_FOR_ACCESSIBILITY_NO :
                        View.IMPORTANT_FOR_ACCESSIBILITY_YES;
        mHost.setImportantForAccessibility(mode);
    }

    public void setTransformOrigin(String transformOrigin) {
        if (mHost == null) {
            return;
        }
        float originX = Origin.parseOrigin(transformOrigin, Origin.ORIGIN_X, mHost, mHapEngine);
        float originY = Origin.parseOrigin(transformOrigin, Origin.ORIGIN_Y, mHost, mHapEngine);
        if (!FloatUtil.isUndefined(originX)) {
            mHost.setPivotX(originX);
        }
        if (!FloatUtil.isUndefined(originY)) {
            mHost.setPivotY(originY);
        }
    }

    public void triggerActiveState(MotionEvent event) {
        if (isDisabled()) {
            return;
        }

        Component c = this;
        while (c != null && !(c instanceof DocComponent)) {
            StateHelper.onActiveStateChanged(c, event);
            c = c.getParent();
        }
    }

    public void onStateChanged(Map<String, Boolean> newStates) {
        for (Map.Entry<String, Boolean> entry : newStates.entrySet()) {
            String state = entry.getKey();
            Boolean stateValue = entry.getValue();
            mStateAttrs.put(state, stateValue);
        }

        processStateChanged(newStates);
        if (newStates.size() > 0) {
            if (DebugUtils.DBG) {
                Log.d(TAG, "onStateChanged: component: " + this + " cssnode: " + mCssNode);
            }
        }
        if (mCssNode != null
                && mCssNode.getPseudoListener() != null
                && mCssNode.getPseudoListener().size() > 0
                && newStates.size() > 0) {
            mCssNode.handleStateChanged(newStates);
            mPendingStates = null;
        } else {
            mPendingStates = newStates;
        }
    }

    public boolean getStateValue(String state) {
        Boolean value = mStateAttrs.get(state);
        if (value == null) {
            return false;
        }
        return value;
    }

    public DocComponent getRootComponent() {
        Component parent = this;
        while (parent.mParent != null) {
            parent = parent.mParent;
        }

        if (!(parent instanceof DocComponent)) {
            return null;
        }

        return (DocComponent) parent;
    }

    // 当前被点击的组件或其父组件在jsframework注册了click事件或者是Floating类型的组件,则返回true
    public boolean isRegisterClickEventComponent() {
        Component parent = this;
        while (parent != null) {
            if (parent.isRegisterClickEvent() || parent instanceof Floating) {
                return true;
            }
            parent = parent.mParent;
        }
        return false;
    }

    public SwipeDelegate getSwipeDelegate() {
        return mSwipeDelegate;
    }

    public final Component findComponentById(String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }

        return findComponentTraversal(id);
    }

    protected Component findComponentTraversal(String id) {
        if (id.equals(mId)) {
            return this;
        }

        return null;
    }

    public int getPageId() {
        DocComponent docComponent = getRootComponent();
        return docComponent == null ? INVALID_PAGE_ID : docComponent.getPageId();
    }

    public Page getPage() {
        DocComponent docComponent = getRootComponent();
        return docComponent == null ? null : docComponent.getPage();
    }

    public CSSBackgroundDrawable getOrCreateCSSBackground() {
        return getOrCreateBackgroundComposer().getBackgroundDrawable();
    }

    public String getDisplay() {
        if (mHost == null) {
            return null;
        }
        return mHost.getVisibility() == View.GONE ? Attributes.Display.NONE :
                Attributes.Display.FLEX;
    }

    public void setDisplay(String display) {
        if (TextUtils.isEmpty(display) || mHost == null) {
            return;
        }
        if (mMinPlatformVersion >= MIN_DISPLAY_SHOW_PLATFORM_VERSION) {
            show(mShowAttrInitialized ? mShow : !Attributes.Display.NONE.equals(display));
        } else {
            show(!Attributes.Display.NONE.equals(display));
        }
    }

    public void show(boolean isShow) {
        if (mHost == null) {
            return;
        }

        mHost.setVisibility(isShow ? View.VISIBLE : View.GONE);

        if (mNode == null) {
            return;
        }
        if (isShow) {
            mNode.setDisplay(YogaDisplay.FLEX);
        } else {
            mNode.setDisplay(YogaDisplay.NONE);
        }
    }

    public void setVisibility(String visibility) {
        if (TextUtils.isEmpty(visibility) || mHost == null) {
            return;
        }
        mHost.setVisibility(
                Attributes.Visibility.HIDDEN.equals(visibility) ? View.INVISIBLE : VISIBLE);
    }

    public void setPositionMode(String positionMode) {
        // disable fixed position mode when card mode
        if (getRootComponent() != null && getRootComponent().isCardMode()) {
            return;
        }

        if (TextUtils.isEmpty(positionMode)
                ||
                // disable fixed position mode when lazy create,  TODO: is there a better solution
                (mIsFixPositionDisabled && Attributes.Position.FIXED.equals(positionMode))) {
            return;
        }

        if (mPosition == null) {
            mPosition = new Position();
        }
        mPosition.setMode(positionMode);
    }

    private void disableFixPosition() {
        mIsFixPositionDisabled = true;
    }

    public void setPosition(String position, float value) {
        if (mPosition == null) {
            mPosition = new Position();
        }

        switch (position) {
            case Attributes.Style.LEFT:
                mPosition.setLeft(value);
                break;
            case Attributes.Style.TOP:
                mPosition.setTop(value);
                break;
            case Attributes.Style.RIGHT:
                mPosition.setRight(value);
                break;
            case Attributes.Style.BOTTOM:
                mPosition.setBottom(value);
                break;
            default:
                break;
        }
    }

    public float getPosition(String position) {
        if (mPosition == null || mNode == null) {
            return FloatUtil.UNDEFINED;
        }

        switch (position) {
            case Attributes.Style.LEFT:
                return mPosition.mLeft;
            case Attributes.Style.TOP:
                return mPosition.mTop;
            case Attributes.Style.RIGHT:
                return mPosition.mRight;
            case Attributes.Style.BOTTOM:
                return mPosition.mBottom;
            default:
                return FloatUtil.UNDEFINED;
        }
    }

    public Object getCurStateStyle(String key, Object defaultValue) {
        CSSValues dataMap = mStyleDomData.get(key);

        if (dataMap == null) {
            return defaultValue;
        }

        return dataMap.get(getState(key));
    }

    public String getCurStateStyleString(String key, String defaultValue) {
        CSSValues dataMap = mStyleDomData.get(key);

        if (dataMap == null) {
            return defaultValue;
        }

        Object attribute = dataMap.get(getState(key));

        return Attributes.getString(attribute, defaultValue);
    }

    public float getCurStateStyleFloat(String key, float defaultValue) {
        CSSValues dataMap = mStyleDomData.get(key);

        if (dataMap == null) {
            return defaultValue;
        }

        Object attribute = dataMap.get(getState(key));

        return Attributes.getFloat(mHapEngine, attribute, defaultValue);
    }

    public int getCurStateStyleInt(String key, int defaultValue) {
        CSSValues dataMap = mStyleDomData.get(key);

        if (dataMap == null) {
            return defaultValue;
        }

        Object attribute = dataMap.get(getState(key));

        return Attributes.getInt(mHapEngine, attribute, defaultValue);
    }

    @Override
    public void onActivityCreate() {
    }

    @Override
    public void onActivityStart() {
    }

    @Override
    public void onActivityResume() {
    }

    @Override
    public void onActivityPause() {
    }

    @Override
    public void onActivityStop() {
    }

    @Override
    public void onActivityDestroy() {
    }

    public void invokeMethod(String methodName, final Map<String, Object> args) {
        if (METHOD_FOCUS.equals(methodName)) {
            boolean focus = true;
            if (args != null && args.get("focus") != null) {
                focus = Attributes.getBoolean(args.get("focus"), true);
            }
            focus(focus);
        } else if (METHOD_REQUEST_FULLSCREEN.equals(methodName)) {
            int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            if (args != null) {
                Object value = args.get("screenOrientation");
                if (Page.ORIENTATION_PORTRAIT.equals(value)) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else if (Page.ORIENTATION_LANDSCAPE.equals(value)) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                }
            }
            DocComponent docComponent = getRootComponent();
            if (docComponent != null) {
                docComponent.enterFullscreen(this, screenOrientation, false);
            } else {
                Log.e(TAG, "invokeMethod: docComponent is null");
            }
        } else if (METHOD_GET_BOUNDING_CLIENT_RECT.equals(methodName)) {
            getBoundingClientRect(args);
        } else if (METHOD_TO_TEMP_FILE_PATH.equals(methodName)) {
            hostViewToTempFilePath(args);
        }
    }

    public Uri tryParseUri(String src) {
        Uri result = null;
        if (!TextUtils.isEmpty(src)) {
            result = UriUtils.computeUri(src);
            if (result == null) {
                result = mCallback.getCache(src);
            } else if (InternalUriUtils.isInternalUri(result)) {
                result = mCallback.getUnderlyingUri(src);
            }
        }
        return result;
    }

    protected boolean parseShowAttribute(Object attribute) {
        if (attribute == null) {
            return true;
        }

        String attributeStr = attribute.toString();
        if ("null".equalsIgnoreCase(attributeStr)
                || "none".equalsIgnoreCase(attributeStr)
                || "undefined".equalsIgnoreCase(attributeStr)
                || "0".equalsIgnoreCase(attributeStr)
                || "false".equalsIgnoreCase(attributeStr)) {
            return false;
        }
        return true;
    }

    @Override
    public int getRef() {
        return mRef;
    }

    private void setRef(int ref) {
        mRef = ref;
    }

    /**
     * list 中 绑定数据后，回调该方法
     */
    protected void afterApplyDataToComponent() {

    }

    public RenderEventCallback getCallback() {
        return mCallback;
    }

    public AnimatorListenerBridge.AnimatorEventListener getOrCreateAnimatorEventListener() {
        if (null == mAnimatorEventListener) {
            mAnimatorEventListener = new AnimatorBridgeListener();
        }
        return mAnimatorEventListener;
    }

    public float getPercentWidth() {
        return mPercentWidth;
    }

    public float getPercentHeight() {
        return mPercentHeight;
    }

    public Transform getTransform() {
        return mTransform;
    }

    public CSSAnimatorSet getAnimatorSet() {
        return mAnimatorSet;
    }

    public void setAnimatorSet(CSSAnimatorSet animatorSet) {
        if (animatorSet != null) {
            mAnimatorSet = animatorSet;
        }
    }

    public void focus(boolean focus) {
        if (mHost != null) {
            if (focus) {
                mHost.setFocusable(true);
                mHost.setFocusableInTouchMode(true);
                mHost.requestFocus();
            } else {
                mHost.clearFocus();
            }
        }
    }

    public void callOnClick() {
        if (mHost == null) {
            return;
        }

        mHost.callOnClick();
    }

    public void notifyAppearStateChange(String eventType) {
        if (Attributes.Event.APPEAR.equals(eventType)
                || Attributes.Event.DISAPPEAR.equals(eventType)) {
            Map<String, Object> params = new ArrayMap<>();
            mCallback.onJsEventCallback(getPageId(), mRef, eventType, this, params, null);
        }
    }

    public Container getParent() {
        return mParent;
    }

    /**
     * get Scroller components
     */
    public Scrollable getParentScroller() {
        Component component = this;
        Component container;
        for (; ; ) {
            container = component.getParent();
            if (container == null) {
                return null;
            }
            if (container instanceof Scrollable) {
                return (Scrollable) container;
            }
            if (container == getRootComponent()) {
                return null;
            }
            component = container;
        }
    }

    public boolean isWidthDefined() {
        return mWidthDefined;
    }

    public void setWidthDefined(boolean defined) {
        mWidthDefined = defined;
    }

    public boolean isHeightDefined() {
        return mHeightDefined;
    }

    public void setHeightDefined(boolean defined) {
        mHeightDefined = defined;
    }

    protected RecyclerDataItem getBoundRecyclerItem() {
        return mBoundRecyclerItem;
    }

    public void setBoundRecyclerItem(RecyclerDataItem recyclerItem) {
        mBoundRecyclerItem = recyclerItem;
    }

    public void applyCache() {
    }

    private void invalidateYogaLayout() {
        if (mHost != null && mNode != null) {
            if (mHost.isLayoutRequested()
                    && !mNode.isDirty()
                    && !isYogaLayout()
                    && isParentYogaLayout()) {
                mNode.dirty();
            }
            if (!mHost.isLayoutRequested() && mNode.isDirty()) {
                mHost.requestLayout();
            }
        }
    }

    public void onHostViewAttached(ViewGroup parent) {
        applyAttrs(mAttrsDomData, true);
        applyStyles(mStyleDomData, true);

        handlePendingState(this);
    }

    private void handlePendingState(Component component) {
        Node cssNode = component.mCssNode;
        if (component != null && cssNode != null) {
            if (component.mPendingStates != null
                    && component.mPendingStates.size() > 0
                    && cssNode != null
                    && mCssNode.getPseudoListener() != null
                    && mCssNode.getPseudoListener().size() > 0) {
                cssNode.handleStateChanged(component.mPendingStates);
            } else {
                if (PSEUDO_STATE.equals(mCssNode.getPendingState())) {
                    component.applyPseoudoStyles(
                            mCssNode.getPendingCssValuesKey(), mCssNode.getPendingCssValuesMap());
                } else {
                    component.restoreStyles();
                }
            }
            component.mPendingStates = null;
            cssNode.setPendingCssValuesMap(null);
            cssNode.setPendingCssValuesKey(null);
            cssNode.setPendingState(null);
        }
    }

    public boolean isYogaLayout() {
        return mHost instanceof YogaLayout;
    }

    public boolean isParentYogaLayout() {
        return (mHost != null) && (mHost.getParent() instanceof YogaLayout);
    }

    private void initYogaNodeFromHost() {
        if (mHost == null) {
            mNode = null;
        } else if (mHost instanceof YogaLayout) {
            mNode = ((YogaLayout) mHost).getYogaNode();
        } else if (mHost.getParent() instanceof YogaLayout) {
            YogaLayout parent = (YogaLayout) mHost.getParent();
            mNode = parent.getYogaNodeForView(mHost);
        } else {
            mNode = null;
        }
    }

    public YogaNode getYogaNode() {
        return mNode;
    }

    private boolean isFlexBasisUnnecessary() {
        if (mParent == null || mHost == null || !(mHost.getParent() instanceof YogaLayout)) {
            return false;
        }

        YogaNode parent = ((YogaLayout) mHost.getParent()).getYogaNode();
        if (parent.getFlexDirection() == YogaFlexDirection.ROW
                && (mParent.isWidthDefined() || parent.getFlexGrow() > 0)
                && mNode.getFlexGrow() > 0) {
            return false;
        }
        if (parent.getFlexDirection() == YogaFlexDirection.COLUMN
                && (mParent.isHeightDefined() || parent.getFlexGrow() > 0)
                && mNode.getFlexGrow() > 0) {
            return false;
        }

        if ((mHost.getParent().getParent() instanceof YogaLayout)) {
            YogaNode parentOfParent = ((YogaLayout) mHost.getParent().getParent()).getYogaNode();
            if (parent.getFlexDirection() != parentOfParent.getFlexDirection()
                    && parentOfParent.getAlignItems() == YogaAlign.STRETCH) {
                return false;
            }
        }

        return true;
    }

    /**
     * 对外暴露获取某个属性或者样式的值
     *
     * @param key
     * @return
     */
    public Object retrieveAttr(String key) {
        return getAttribute(key);
    }

    public boolean isFixed() {
        return mPosition != null && mPosition.isFixed();
    }

    public boolean isRelative() {
        return mPosition == null || mPosition.isRelative();
    }

    public boolean isAbsolute() {
        return mPosition != null && mPosition.isAbsolute();
    }

    // minPlatformVersion高于1040回调
    public boolean onTouch(View v, MotionEvent event) {
        if (mTouchListeners == null) {
            return false;
        }

        boolean result = false;
        for (int i = 0; i < mTouchListeners.size(); i++) {
            View.OnTouchListener l = mTouchListeners.valueAt(i);
            result |= l.onTouch(v, event);
        }
        return result;
    }

    public void addTouchListener(int type, @NonNull View.OnTouchListener l) {
        if (mTouchListeners == null) {
            mTouchListeners = new SparseArray<>();
        }
        mTouchListeners.put(type, l);

        if (mOnTouchListener == null) {
            mOnTouchListener = new TouchListenerDelegate();
        }
        // move to GestureDelegate when minPlatformVersion larger than 1040.
        if (mHapEngine.getMinPlatformVersion() < GestureDispatcher.MIN_BUBBLE_PLATFORM_VERSION
                && mHost != null) {
            mHost.setOnTouchListener(mOnTouchListener);
        }
    }

    public void removeTouchListener(int type) {
        if (mTouchListeners == null) {
            return;
        }
        mTouchListeners.remove(type);
        if (mTouchListeners.size() == 0 && mHost != null) {
            mHost.setOnTouchListener(null);
        }
    }

    private boolean tryInstallMarginLayoutParams() {
        if (mHost == null) {
            return false;
        }

        if (isParentYogaLayout()
                || (mHost.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return true;
        }
        try {
            ViewParent viewParent = mHost.getParent();
            if (!(viewParent instanceof ViewGroup)) {
                // impossible
                return false;
            }
            ViewGroup.MarginLayoutParams layoutParams =
                    new ViewGroup.MarginLayoutParams(getLayoutParams());

            // cause Exception if not support margin, checkLayoutParams error
            ((ViewGroup) viewParent).updateViewLayout(mHost, layoutParams);
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return false;
    }

    public Animation animate(String animId, String keyframes, String options) {
        if (TextUtils.isEmpty(keyframes)) {
            return null;
        }
        Animation animation = mAnimations.get(animId);
        CSSAnimatorSet tempAnimatorSet, animatorSet;

        if (animation != null && animation.getAnimatorSet() != null) {
            tempAnimatorSet = animation.getAnimatorSet();
        } else {
            tempAnimatorSet = new CSSAnimatorSet(mHapEngine, this);
        }

        try {
            JSONObject optionsObj = new JSONObject(options);
            for (Iterator keys = optionsObj.keys(); keys.hasNext(); ) {
                String key = (String) keys.next();
                Object attribute = optionsObj.get(key);
                if ("duration".equals(key)) {
                    int duration = AnimationParser.getTime(Attributes.getString(attribute));
                    tempAnimatorSet.setDuration(duration);
                } else if ("easing".equals(key)) {
                    String timing = Attributes.getString(attribute, "linear");
                    tempAnimatorSet.setKeyFrameInterpolator(TimingFactory.getTiming(timing));
                } else if ("delay".equals(key)) {
                    int delay = AnimationParser.getTime(Attributes.getString(attribute));
                    tempAnimatorSet.setDelay(delay);
                } else if ("iterations".equals(key)) {
                    int repeatCount = Attributes.getInt(mHapEngine, attribute, 0);
                    tempAnimatorSet.setRepeatCount(repeatCount);
                } else if ("fill".equals(key)) {
                    String fillMode = Attributes.getString(attribute, CSSAnimatorSet.FillMode.NONE);
                    tempAnimatorSet.setFillMode(fillMode);
                } else if ("direction".equals(key)) {
                    String direction =
                            Attributes.getString(attribute, CSSAnimatorSet.Direction.NORMAL);
                    tempAnimatorSet.setDirection(direction);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        animatorSet = AnimationParser.parse(mHapEngine, tempAnimatorSet, keyframes, this);
        if (animatorSet != null) {
            if (animation != null) {
                Log.i(TAG, "Animation ID " + animId + ", duplicate for reuse.");
                animatorSet.setAnimation(animation);
                animation.setAnimatorSet(animatorSet);
            } else {
                animation = new Animation(animatorSet);
            }
        }

        mAnimations.put(animId, animation);
        return animation;
    }

    // 在1040开始，native只下发target组件的事件给jsframework，在jsframework处实现捕获和冒泡处理
    // native则需要监听所有类型的touch事件下发。
    private void configBubbleEventAbove1040(boolean updateComponent) {
        if (mMinPlatformVersion >= GestureDispatcher.MIN_BUBBLE_PLATFORM_VERSION) {
            if (mHost instanceof GestureHost) {
                GestureHost gestureHost = (GestureHost) mHost;
                if (gestureHost.getGesture() == null) {
                    gestureHost.setGesture(new GestureDelegate(mHapEngine, this, mContext));
                }

                // fix bug：list-item被回收再复用后，component没有刷新，保存的是旧的
                if (updateComponent) {
                    gestureHost.getGesture().updateComponent(this);
                }
                gestureHost.getGesture().registerEvent(Attributes.Event.TOUCH_START);
                gestureHost.getGesture().registerEvent(Attributes.Event.TOUCH_MOVE);
                gestureHost.getGesture().registerEvent(Attributes.Event.TOUCH_END);
                gestureHost.getGesture().registerEvent(Attributes.Event.TOUCH_CANCEL);
                gestureHost.getGesture().registerEvent(Attributes.Event.TOUCH_CLICK);
                gestureHost.getGesture().registerEvent(Attributes.Event.TOUCH_LONG_PRESS);
            }
        }
    }

    public boolean isWatchAppearance() {
        return mWatchAppearance[AppearanceHelper.APPEAR]
                || mWatchAppearance[AppearanceHelper.DISAPPEAR];
    }

    public boolean isWatchAppearance(int eventType) {
        return (eventType == AppearanceHelper.APPEAR || eventType == AppearanceHelper.DISAPPEAR)
                && mWatchAppearance[eventType];
    }

    public void setWatchAppearance(int eventType, boolean isWatch) {
        if (eventType == AppearanceHelper.APPEAR || eventType == AppearanceHelper.DISAPPEAR) {
            mWatchAppearance[eventType] = isWatch;
        }
    }

    private void addOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
        if (mHost == null) {
            return;
        }
        // 组件和view一起复用，不会多次添加listener
        mHost.addOnLayoutChangeListener(listener);
    }

    private void removeOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
        if (mHost == null) {
            return;
        }
        mHost.removeOnLayoutChangeListener(listener);
    }

    private void callbackResizeEvent(Rect rect) {
        if (rect == null) {
            return;
        }

        Map<String, Object> param = new HashMap<>();
        int designWidth = mHapEngine.getDesignWidth();
        int left = rect.left;
        int top = rect.top;
        int right = rect.right;
        int bottom = rect.bottom;
        float offsetWidth = DisplayUtil.getDesignPxByWidth(right - left, designWidth);
        float offsetHeight = DisplayUtil.getDesignPxByWidth(bottom - top, designWidth);
        float offsetLeft = DisplayUtil.getDesignPxByWidth(left, designWidth);
        float offsetTop = DisplayUtil.getDesignPxByWidth(top, designWidth);

        param.put("offsetWidth", offsetWidth);
        param.put("offsetHeight", offsetHeight);
        param.put("offsetLeft", offsetLeft);
        param.put("offsetTop", offsetTop);
        RenderEventCallback.EventData eventData =
                new RenderEventCallback.EventData(getPageId(), mRef, Attributes.Event.RESIZE, param,
                        null);
        ResizeEventDispatcher.getInstance(this).put(eventData);
    }

    public void updateViewId() {
        if (mHost == null) {
            return;
        }
        mHost.setId(ViewIdUtils.getViewId(getRef()));
    }

    public void getBoundingClientRect(Map<String, Object> args) {
        if (mHost == null || args == null) {
            callback(args, CALLBACK_KEY_FAIL, null);
            callback(args, CALLBACK_KEY_COMPLETE, null);
            return;
        }
        if (mHost.getWidth() == 0 && mHost.getHeight() == 0) {
            callback(args, CALLBACK_KEY_FAIL, null);
            callback(args, CALLBACK_KEY_COMPLETE, null);
            return;
        }
        int designWidth = mHapEngine.getDesignWidth();
        Map<String, Object> object = new ArrayMap<>();
        int[] position = new int[2];
        mHost.getLocationInWindow(position);
        // 去除titlebar和statusbar的高度
        DocComponent rootComponent = getRootComponent();
        if (rootComponent != null) {
            DecorLayout decorLayout = (DecorLayout) rootComponent.getInnerView();
            Rect contentInsets = decorLayout.getContentInsets();
            object.put(
                    "top",
                    DisplayUtil.getDesignPxByWidth(position[1] - contentInsets.top, designWidth));
            object.put(
                    "bottom",
                    DisplayUtil.getDesignPxByWidth(
                            position[1] - contentInsets.top + mHost.getHeight(), designWidth));
        }
        object.put("left", DisplayUtil.getDesignPxByWidth(position[0], designWidth));
        object.put(
                "right",
                DisplayUtil.getDesignPxByWidth(position[0] + mHost.getWidth(), designWidth));
        object.put("width", DisplayUtil.getDesignPxByWidth(mHost.getWidth(), designWidth));
        object.put("height", DisplayUtil.getDesignPxByWidth(mHost.getHeight(), designWidth));
        callback(args, CALLBACK_KEY_SUCCESS, object);
        callback(args, CALLBACK_KEY_COMPLETE, null);
    }

    public void callback(Map<String, Object> args, String key, Object object) {
        if (args != null && args.containsKey(key)) {
            String callbackId = (String) args.get(key);
            mCallback.onJsMethodCallback(getPageId(), callbackId, object);
        }
    }

    protected void hostViewToTempFilePath(Map<String, Object> args) {
        Executors.io()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (args == null) {
                                    Log.e(TAG, "hostViewToTempFilePath failed: args is null");
                                    return;
                                }
                                String fileType =
                                        Attributes.getString(args.get(KEY_FILE_TYPE), "png");
                                double quality = Attributes.getDouble(args.get(KEY_QUALITY), 1.0);
                                String successCallbackId =
                                        Attributes.getString(args.get(KEY_SUCCESS));
                                String failCallbackId = Attributes.getString(args.get(KEY_FAIL));
                                String completeCallbackId =
                                        Attributes.getString(args.get(KEY_COMPLETE));

                                if (mSnapshotCanvas == null) {
                                    mSnapshotCanvas = new Canvas();
                                }
                                String bgColor =
                                        getRootComponent()
                                                .getAppInfo()
                                                .getDisplayInfo()
                                                .getDefaultStyle()
                                                .get(DisplayInfo.Style.KEY_BACKGROUND_COLOR);
                                int bgColorInteger =
                                        TextUtils.isEmpty(bgColor) ? Color.WHITE :
                                                ColorUtil.getColor(bgColor);
                                Bitmap snapshot =
                                        SnapshotUtils.createSnapshot(mHost, mSnapshotCanvas,
                                                bgColorInteger);
                                Uri snapshotUri =
                                        SnapshotUtils.saveSnapshot(
                                                getHybridView(), snapshot, getRef(), fileType,
                                                quality);

                                if (snapshotUri != null && !TextUtils.isEmpty(successCallbackId)) {
                                    String internalUri =
                                            getHybridView()
                                                    .getHybridManager()
                                                    .getApplicationContext()
                                                    .getInternalUri(snapshotUri);
                                    Map<String, Object> params = new HashMap<>();
                                    params.put(TEMP_FILE_PATH, internalUri);
                                    mCallback.onJsMethodCallback(getPageId(), successCallbackId,
                                            params);
                                } else if (!TextUtils.isEmpty(failCallbackId)) {
                                    mCallback.onJsMethodCallback(getPageId(), failCallbackId);
                                }
                                if (!TextUtils.isEmpty(completeCallbackId)) {
                                    mCallback.onJsMethodCallback(getPageId(), completeCallbackId);
                                }
                            }
                        });
    }

    protected void changeAttrDomData(String keyStr, Object changedData) {
        getAttrsDomData().put(keyStr, changedData);
        if (mBoundRecyclerItem != null) {
            mBoundRecyclerItem.getAttrsDomData().put(keyStr, changedData);
        }
    }

    public boolean onHostKey(int keyAction, int keyCode, KeyEvent event) {
        String eventName;
        if (mKeyEventRegister) {
            eventName = Attributes.Event.KEY_EVENT;
        } else {
            eventName = Attributes.Event.KEY_EVENT_PAGE;
        }
        return onHostKey(keyAction, keyCode, event, eventName);
    }

    public boolean onHostKey(int keyAction, int keyCode, KeyEvent event, String eventName) {
        Map<String, Object> params = new HashMap<>();
        params.put(KeyEventDelegate.KEY_CODE, keyCode);
        params.put(KeyEventDelegate.KEY_ACTION, keyAction);
        params.put(KeyEventDelegate.KEY_REPEAT, event.getRepeatCount());
        params.put(KeyEventDelegate.KEY_HASHCODE, event.hashCode());
        mCallback.onJsEventCallback(getPageId(), mRef, eventName, this, params, null);
        return true;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        if (mHost == null) {
            return;
        }
        mId = id;

        if (mFloatingTouchListener == null) {
            mFloatingTouchListener = new FloatingTouchListener();
        }
        addTouchListener(TOUCH_TYPE_FLOATING, mFloatingTouchListener);
    }

    protected void parseId(Object attribute) {
        String id = Attributes.getString(attribute);
        setId(id);
    }

    public boolean onHostKeyDown() {
        RenderEventCallback.EventData eventData =
                new RenderEventCallback.EventData(getPageId(), mRef, Attributes.Event.CLICK, null,
                        null);
        List<RenderEventCallback.EventData> eventDatas = new ArrayList<>();
        eventDatas.add(eventData);
        mCallback.onJsMultiEventCallback(getPageId(), eventDatas);
        return true;
    }

    private boolean isRegisterClickEvent() {
        return mRegisterClickEvent;
    }

    private void setRegisterClickEvent(boolean registerClickEvent) {
        mRegisterClickEvent = registerClickEvent;
    }

    private static class ComponentPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        private WeakReference<View> mViewRef;
        private WeakReference<Component> mComponentRef;
        private WeakReference<CSSAnimatorSet> mAnimationSetRef;
        private boolean mIsReady;

        public ComponentPreDrawListener(
                Component component, View view, CSSAnimatorSet animationSet, boolean isReady) {
            mComponentRef = new WeakReference<>(component);
            mViewRef = new WeakReference<>(view);
            mAnimationSetRef = new WeakReference<>(animationSet);
            mIsReady = isReady;
        }

        @Override
        public boolean onPreDraw() {
            final View view = mViewRef.get();
            if (view != null) {
                ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                viewTreeObserver.removeOnPreDrawListener(this);
            }
            final Component component = mComponentRef.get();
            final CSSAnimatorSet animatorSet = mAnimationSetRef.get();
            if (mIsReady) {
                if (animatorSet != null) {
                    animatorSet.start();
                }
            } else {
                if (animatorSet != null) {
                    CSSAnimatorSet temp = animatorSet.parseAndStart();
                    if (temp != null) {
                        temp.setIsReady(true);
                        if (component != null) {
                            component.setAnimatorSet(temp);
                        }
                    }
                }
            }
            return true;
        }
    }

    private static class ComponentOnGlobalLayoutListener
            implements ViewTreeObserver.OnGlobalLayoutListener {
        private WeakReference<Component> mReference;

        public ComponentOnGlobalLayoutListener(Component component) {
            mReference = new WeakReference<>(component);
        }

        @Override
        public void onGlobalLayout() {
            Component component = mReference.get();
            if (component == null) {
                return;
            }
            if (component.mTransform == null || component.mHost == null) {
                return;
            }
            if (!Float.isNaN(component.mTransform.getTranslationXPercent())) {
                float translationX =
                        component.mTransform.getTranslationXPercent() * component.mHost.getWidth();
                component.mTransform.setTranslationX(translationX);
                component.mHost.setTranslationX(translationX);
            }
            if (!Float.isNaN(component.mTransform.getTranslationYPercent())) {
                float translationY =
                        component.mTransform.getTranslationYPercent() * component.mHost.getHeight();
                component.mTransform.setTranslationY(translationY);
                component.mHost.setTranslationY(translationY);
            }
            if (!Float.isNaN(component.mTransform.getTranslationZPercent())) {
                float translationZ = component.mTransform.getTranslationZPercent() * 2; // 2dp thickness
                component.mTransform.setTranslationZ(translationZ);
                component.mHost.setTranslationZ(translationZ);
            }
        }
    }

    public boolean isAdMaterial() {
        return mIsAdMaterial;
    }

    public boolean isUseInList() {
        return mIsUseInList;
    }

    public static class RecyclerItem extends RecyclerDataItem {

        private Map<String, Object> mInstanceState = new HashMap<>();

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        public Component createRecycleComponent(Container parent) {
            Component recycle = getComponentCreator().createComponent(parent, getRef());

            if (mCssNode != null) {
                recycle.setCssNode(mCssNode);
                mCssNode.setComponent(recycle);
            }

            recycle.setLazyCreate(false);
            recycle.disableFixPosition();

            if (isUseWithTemplate()) {
                if (getAttachedTemplate() == null) {
                    throw new IllegalStateException(
                            "RecyclerItem under list must set Template, then create component");
                }

                recycle.bindAttrs(getAttrsDomData().getSameMap());
                recycle.bindStyles(getStyleDomData().getSameMap());
                recycle.bindEvents(getEventCombinedMap().getSameMap());
            }

            return recycle;
        }

        @Override
        public void unbindComponent() {
            if (getBoundComponent() != null) {
                getBoundComponent().performSaveInstanceState(mInstanceState);
            }
            super.unbindComponent();
        }

        @Override
        protected void onApplyDataToComponent(Component recycle) {
            recycle.setRef(getRef());
            recycle.updateViewId();

            if (getAttrsDomData().getDiffMap().size() > 0) {
                recycle.bindAttrs(getAttrsDomData().getDiffMap());
            }

            if (getStyleDomData().getDiffMap().size() > 0) {
                recycle.bindStyles(getStyleDomData().getDiffMap());
            }

            if (getEventCombinedMap().getDiffMap().size() > 0) {
                recycle.bindEvents(getEventCombinedMap().getDiffMap());
            }

            if (recycle.getHostView() == null) {
                // set template's data and recyclerItem's data, then create view and apply all data
                recycle.createView();
            } else {
                recycle.handlePendingState(recycle);
            }
            recycle.performRestoreInstanceState(mInstanceState);
            mInstanceState.clear();
            recycle.afterApplyDataToComponent();
        }

        public boolean isFixOrFloating() {
            if (Floating.class.isAssignableFrom(getComponentClass())) {
                return true;
            }

            CSSValues style = getStyleAttribute(Attributes.Style.POSITION);
            if (style != null) {
                String position =
                        Attributes.getString(style.get(State.NORMAL), Attributes.Position.RELATIVE);
                return Attributes.Position.FIXED.equals(position);
            }

            return false;
        }

        protected CSSValues getStyleAttribute(String key) {
            return getStyleDomData().get(key);
        }
    }

    protected class Position {
        float mLeft;
        float mTop;
        float mRight;
        float mBottom;
        float mMarginLeft;
        float mMarginTop;
        float mMarginRight;
        float mMarginBottom;

        String mPositionMode = Attributes.Position.RELATIVE;

        boolean mIsDirty = false;

        public Position() {
            mLeft = FloatUtil.UNDEFINED;
            mTop = FloatUtil.UNDEFINED;
            mRight = FloatUtil.UNDEFINED;
            mBottom = FloatUtil.UNDEFINED;
            mMarginLeft = FloatUtil.UNDEFINED;
            mMarginTop = FloatUtil.UNDEFINED;
            mMarginRight = FloatUtil.UNDEFINED;
            mMarginBottom = FloatUtil.UNDEFINED;
        }

        public boolean isFixed() {
            return TextUtils.equals(mPositionMode, Attributes.Position.FIXED);
        }

        public boolean isRelative() {
            return TextUtils.equals(mPositionMode, Attributes.Position.RELATIVE);
        }

        public boolean isAbsolute() {
            return TextUtils.equals(mPositionMode, Attributes.Position.ABSOLUTE);
        }

        public void setMode(String mode) {
            if (!(Attributes.Position.RELATIVE.equals(mode)
                    || Attributes.Position.ABSOLUTE.equals(mode)
                    || Attributes.Position.FIXED.equals(mode))) {
                mode = Attributes.Position.RELATIVE;
            }
            if (mHost == null || mNode == null || TextUtils.equals(mPositionMode, mode)) {
                return;
            }
            mIsDirty = true;
            mPositionMode = mode;
            switch (mPositionMode) {
                case Attributes.Position.RELATIVE:
                    mNode.setPositionType(YogaPositionType.RELATIVE);
                    if (mHost.getParent() instanceof PercentFlexboxLayout) {
                        PercentFlexboxLayout yogaLayout = (PercentFlexboxLayout) mHost.getParent();
                        yogaLayout.setChildrenDrawingOrderEnabled(true);
                    }
                    resetPositions();
                    break;
                case Attributes.Position.ABSOLUTE:
                    mNode.setPositionType(YogaPositionType.ABSOLUTE);
                    if (mHost.getParent() instanceof PercentFlexboxLayout) {
                        PercentFlexboxLayout yogaLayout = (PercentFlexboxLayout) mHost.getParent();
                        yogaLayout.setChildrenDrawingOrderEnabled(true);
                    }
                    resetPositions();
                    break;
                case Attributes.Position.FIXED:
                    clearPositions();
                    break;
                default:
                    break;
            }
        }

        public void setMargin(String key, Object marginAttr) {
            float margin;
            if (Attributes.Style.MARGIN_AUTO.equals(marginAttr)) {
                margin = 0.0f;
            } else {
                margin = Attributes.getFloat(mHapEngine, marginAttr, 0.0f);
            }
            if (FloatUtil.isUndefined(margin)) {
                margin = 0.0f;
            }
            mIsDirty = true;
            switch (key) {
                case Attributes.Style.MARGIN:
                    mMarginLeft = margin;
                    mMarginTop = margin;
                    mMarginRight = margin;
                    mMarginBottom = margin;
                    break;
                case Attributes.Style.MARGIN_LEFT:
                    mMarginLeft = margin;
                    break;
                case Attributes.Style.MARGIN_TOP:
                    mMarginTop = margin;
                    break;
                case Attributes.Style.MARGIN_RIGHT:
                    mMarginRight = margin;
                    break;
                case Attributes.Style.MARGIN_BOTTOM:
                    mMarginBottom = margin;
                    break;
                default:
                    break;
            }
        }

        public void setLeft(float left) {
            if (mNode == null) {
                return;
            }
            mIsDirty = true;
            mLeft = left;
            switch (mPositionMode) {
                case Attributes.Position.RELATIVE:
                case Attributes.Position.ABSOLUTE:
                    mNode.setPosition(YogaEdge.LEFT, mLeft);
                    break;
                default:
                    break;
            }
        }

        public void setTop(float top) {
            if (mNode == null) {
                return;
            }
            mIsDirty = true;
            mTop = top;
            switch (mPositionMode) {
                case Attributes.Position.RELATIVE:
                case Attributes.Position.ABSOLUTE:
                    mNode.setPosition(YogaEdge.TOP, mTop);
                    break;
                default:
                    break;
            }
        }

        public void setRight(float right) {
            if (mNode == null) {
                return;
            }
            mIsDirty = true;
            mRight = right;
            switch (mPositionMode) {
                case Attributes.Position.RELATIVE:
                case Attributes.Position.ABSOLUTE:
                    mNode.setPosition(YogaEdge.RIGHT, mRight);
                    break;
                default:
                    break;
            }
        }

        public void setBottom(float bottom) {
            if (mNode == null) {
                return;
            }
            mIsDirty = true;
            mBottom = bottom;
            switch (mPositionMode) {
                case Attributes.Position.RELATIVE:
                case Attributes.Position.ABSOLUTE:
                    mNode.setPosition(YogaEdge.BOTTOM, mBottom);
                    break;
                default:
                    break;
            }
        }

        public void applyPosition() {
            if (!mIsDirty || mHost == null) {
                return;
            }
            mIsDirty = false;
            if (isFixed()) {

                ViewGroup.LayoutParams defaultLp = getLayoutParams();
                DecorLayout.LayoutParams lp;
                if (defaultLp instanceof DecorLayout.LayoutParams) {
                    lp = (DecorLayout.LayoutParams) defaultLp;
                } else {
                    lp = new DecorLayout.LayoutParams(defaultLp);
                }

                if (FloatUtil.isUndefined(mTop) && !FloatUtil.isUndefined(mBottom)) {
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    lp.topMargin = 0;
                    lp.bottomMargin = Math.round(mBottom) + Math.round(mMarginBottom);
                } else {
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    lp.topMargin = Math.round(mTop) + Math.round(mMarginTop);
                }

                if (!isHeightDefined() && !FloatUtil.isUndefined(mTop)
                        && !FloatUtil.isUndefined(mBottom)) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    lp.topMargin = Math.round(mTop) + Math.round(mMarginTop);
                    lp.bottomMargin = Math.round(mBottom) + Math.round(mMarginBottom);
                }

                if (FloatUtil.isUndefined(mLeft) && !FloatUtil.isUndefined(mRight)) {
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    lp.rightMargin = Math.round(mRight) + Math.round(mMarginRight);
                } else {
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    lp.leftMargin = Math.round(mLeft) + Math.round(mMarginLeft);
                }

                if (!isWidthDefined() && !FloatUtil.isUndefined(mLeft)
                        && !FloatUtil.isUndefined(mRight)) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    lp.leftMargin = Math.round(mLeft) + Math.round(mMarginLeft);
                    lp.rightMargin = Math.round(mRight) + Math.round(mMarginRight);
                }

                lp.percentWidth = mPercentWidth;
                lp.percentHeight = mPercentHeight;

                ViewGroup parent = (ViewGroup) mHost.getParent();
                DocComponent rootComponent = getRootComponent();
                if (rootComponent != null) {
                    if (parent == null) {
                        mHost.setLayoutParams(lp);
                        rootComponent.addView(mHost, -1);
                    } else if (parent == rootComponent.getInnerView()) {
                        // only change the layout param since it is fixed view already.
                        Rect insets = ((DecorLayout) parent).getContentInsets();
                        lp.topMargin += insets.top;
                        mHost.setLayoutParams(lp);
                    } else {
                        parent.removeView(mHost);
                        mParent.addFixedChild(Component.this);
                        mHost.setLayoutParams(lp);
                        rootComponent.addView(mHost, -1);
                    }
                } else {
                    Log.e(TAG, "applyPosition: rootComponent is null");
                }

                if (mNode != null) {
                    YogaNode parentNode = mNode.getParent();
                    if (parentNode != null && parentNode.indexOf(mNode) > -1) {
                        parentNode.removeChildAt(parentNode.indexOf(mNode));
                    }
                }
            } else {
                DocComponent rootComponent = getRootComponent();
                if (rootComponent == null || mHost.getParent() != rootComponent.getInnerView()) {
                    return;
                }

                rootComponent.getInnerView().removeView(mHost);
                DecorLayout.LayoutParams lp = (DecorLayout.LayoutParams) mHost.getLayoutParams();
                mHost.setLayoutParams(lp.getSourceLayoutParams());
                int index = mParent.mChildren.indexOf(Component.this);

                YogaLayout parentView = (YogaLayout) mParent.getInnerView();
                if (parentView != null) {
                    int offsetIndex = mParent.offsetIndex(index);
                    if (mNode != null) {
                        parentView.addView(mHost, mNode, offsetIndex);
                    } else {
                        parentView.addView(mHost, offsetIndex);
                        // rearrange yoga node.
                        mNode = parentView.getYogaNodeForView(mHost);
                        lazyApplyData();
                    }
                    mParent.removeFixedChild(Component.this);
                }
            }
        }

        private void clearPositions() {
            mNode.setPosition(YogaEdge.LEFT, FloatUtil.UNDEFINED);
            mNode.setPosition(YogaEdge.TOP, FloatUtil.UNDEFINED);
            mNode.setPosition(YogaEdge.RIGHT, FloatUtil.UNDEFINED);
            mNode.setPosition(YogaEdge.BOTTOM, FloatUtil.UNDEFINED);
        }

        private void resetPositions() {
            mNode.setPosition(YogaEdge.LEFT, mLeft);
            mNode.setPosition(YogaEdge.TOP, mTop);
            mNode.setPosition(YogaEdge.RIGHT, mRight);
            mNode.setPosition(YogaEdge.BOTTOM, mBottom);
        }
    }

    // minPlatformVersion低于1040回调
    private class TouchListenerDelegate implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mMinPlatformVersion >= GestureDispatcher.MIN_BUBBLE_PLATFORM_VERSION) {
                return false;
            }
            if (mTouchListeners == null) {
                return false;
            }

            boolean result = false;
            for (int i = 0; i < mTouchListeners.size(); i++) {
                View.OnTouchListener l = mTouchListeners.valueAt(i);
                result |= l.onTouch(v, event);
            }

            return result;
        }
    }

    private class FloatingTouchListener implements View.OnTouchListener {

        private Floating mFloating;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (TextUtils.isEmpty(mId)) {
                        return false;
                    }
                    DocComponent rootComponent = getRootComponent();
                    if (rootComponent != null) {
                        FloatingHelper helper = rootComponent.getFloatingHelper();
                        mFloating = helper.get(mId);
                        if (mFloating == null) {
                            return false;
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mFloating != null) {
                        mFloating.show(mHost);
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    public class ResizeListener implements View.OnLayoutChangeListener {

        private Rect mLayout;

        public Rect getLayout() {
            return mLayout;
        }

        public void setLayout(Rect layout) {
            mLayout = layout;
            if (getDomEvents().contains(Attributes.Event.RESIZE)
                    && mHost != null
                    && !mHost.isInLayout()) {
                // 在list中，某些item对应的组件和view被复用后不会调用onLayout回调，导致无法
                // 触发 resize 事件，所以这里主动触发一下
                mHost.requestLayout();
            }
        }

        @Override
        public void onLayoutChange(
                View v,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
            handleResizeEvent(left, top, right, bottom);
        }

        private void handleResizeEvent(int left, int top, int right, int bottom) {
            // position为fixed时，top值返回的是到屏幕顶端的距离，我们需要返回到标题栏的距离，
            // 因此需要减去标题栏和状态栏的高度
            if (isFixed()) {
                DocComponent rootComponent = getRootComponent();
                if (rootComponent != null) {
                    DecorLayout decor = (DecorLayout) rootComponent.getInnerView();
                    top -= decor.getContentInsets().top;
                } else {
                    Log.e(TAG, "handleResizeEvent: rootComponent is null");
                }
            }

            // 数据没有改变时不触发回调
            if (mLayout != null && mLayout.width() == right - left
                    && mLayout.height() == bottom - top) {
                return;
            }

            if (mLayout == null) {
                mLayout = new Rect(left, top, right, bottom);
            } else {
                mLayout.left = left;
                mLayout.top = top;
                mLayout.right = right;
                mLayout.bottom = bottom;
            }
            callbackResizeEvent(mLayout);
        }
    }

    class AnimatorBridgeListener implements AnimatorListenerBridge.AnimatorEventListener {

        Set<String> registeredEventsSet = new ArraySet<>();

        @Override
        public void onAnimatorEvent(
                Animator animator,
                String eventName,
                Map<String, Object> params,
                Map<String, Object> attributes) {
            if (getCallback() != null && registeredEventsSet.contains(eventName)) {
                getCallback()
                        .onJsEventCallback(
                                getPageId(), getRef(), eventName, Component.this, params,
                                attributes);
            }
        }

        @Override
        public boolean registerEvent(String eventName) {
            return registeredEventsSet.add(eventName);
        }

        @Override
        public boolean unregisterEvent(String eventName) {
            return registeredEventsSet.remove(eventName);
        }

        @Override
        public void unregisterAllEvents() {
            registeredEventsSet.clear();
        }
    }

    public View getFullScreenView() {
        if (mFullScreenView == null) {
            return mHost;
        }
        return mFullScreenView;
    }

    public void setFullScreenView(View view) {
        mFullScreenView = view;
    }

    public boolean preConsumeEvent(String eventName, Map<String, Object> data, boolean immediately) {
        return false;
    }
}
