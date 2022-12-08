/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.list;

import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.FlexRecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.HapStaggeredGridLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.AbstractScrollable;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.Recycler;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.RecyclerDataTemplate;
import org.hapjs.component.RecyclerItemList;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.appearance.RecycleAppearanceManager;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.ScrollView;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;
import org.hapjs.widgets.RecyclerDataItemFactory;
import org.hapjs.widgets.view.list.FlexGridLayoutManager;
import org.hapjs.widgets.view.list.FlexLayoutManager;
import org.hapjs.widgets.view.list.FlexStaggeredGridLayoutManager;
import org.hapjs.widgets.view.list.RecyclerViewAdapter;

@WidgetAnnotation(
        name = List.WIDGET_NAME,
        methods = {
                List.METHOD_SCROLL_TO,
                List.METHOD_SCROLL_BY,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class List extends AbstractScrollable<RecyclerView> implements Recycler, SwipeObserver {
    protected static final String WIDGET_NAME = "list";
    protected static final String METHOD_SCROLL_TO = "scrollTo";
    protected static final String METHOD_PARAM_INDEX = "index";
    protected static final String METHOD_PARAM_SMOOTH = "smooth";
    protected static final String METHOD_SCROLL_BY = "scrollBy";
    protected static final String METHOD_PARAM_TOP = "top";
    protected static final String METHOD_PARAM_LEFT = "left";
    protected static final String METHOD_PARAM_BEHAVIOR = "behavior";
    protected static final String BEHAVIOR_SMOOTH = "smooth";
    private static final String TAG = "List";
    private static final String LIST_LAYOUT_TYPE = "layoutType";
    private static final String LIST_GRID_TYPE = "grid";
    private static final String LIST_STAGGER_TYPE = "stagger";
    private String mCurrentLayoutType;
    private Adapter mAdapter;
    private RecyclerView mRecyclerView;
    private ScrollListener mScrollListener;
    private ScrollBottomListener mScrollBottomListener;
    private ScrollTopListener mScrollTopListener;
    private ScrollEndListener mScrollEndListener;
    private ScrollTouchUpListener mScrollTouchUpListener;
    private FlexLayoutManager mFlexLayoutManager;
    protected RecyclerViewAdapter mRecyclerViewImpl;

    private RecyclerItem mRecyclerItem;
    private int mPreviousScrollPosition = -1;
    private int mPreviousScrollOffset = 0;
    private int mColumnCount = FlexLayoutManager.DEFAULT_COLUMN_COUNT;
    private boolean mIsScrollPage = false;
    private int mOrientation = OrientationHelper.VERTICAL;
    private boolean mIsReverse = false;
    private boolean mIsEnableTalkBack;

    public List(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int elId,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, elId, callback, savedState);
        mIsEnableTalkBack = isEnableTalkBack();
    }

    @Override
    protected RecyclerView createViewImpl() {
        mRecyclerViewImpl = createRecyclerViewInner();
        mRecyclerViewImpl.setComponent(this);

        mRecyclerView = mRecyclerViewImpl.getActualRecyclerView();
        ViewGroup.LayoutParams params = generateDefaultLayoutParams();
        mRecyclerView.setLayoutParams(params);
        initFlexLayoutManager();
        mRecyclerView.setItemAnimator(null);

        mAdapter = new Adapter();
        mRecyclerView.setAdapter(mAdapter);
        if (getRecyclerItem() != null) {
            mAdapter.setData(getRecyclerItem());
        }

        mRecyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    private int mScrolledX;
                    private int mScrolledY;
                    private int mState = RecyclerView.SCROLL_STATE_IDLE;

                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        mState = newState;

                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (mFlexLayoutManager instanceof FlexStaggeredGridLayoutManager) {
                                configPreviousScrollPositionAndOffset();
                            }
                            if (mScrollEndListener != null) {
                                mScrollEndListener.onScrollEnd();
                            }
                            if (mScrollListener != null) {
                                mScrollListener.onScroll(0, 0, mState);
                            }
                            if (mFlexLayoutManager == null) {
                                Log.w(TAG, "onScrollStateChanged: mFlexLayoutManager is null");
                                return;
                            }
                            if (mScrollBottomListener != null
                                    && (mFlexLayoutManager.getFlexItemCount() - 1)
                                    == mFlexLayoutManager
                                    .findFlexLastCompletelyVisibleItemPosition()) {
                                if (mFlexLayoutManager.canFlexScrollHorizontally()) {
                                    if (Math.abs(mScrolledX) >= 1) {
                                        mScrollBottomListener.onScrollBottom();
                                        mScrolledX = 0;
                                    }
                                } else if (mFlexLayoutManager.canFlexScrollVertically()) {
                                    if (Math.abs(mScrolledY) >= 1) {
                                        mScrollBottomListener.onScrollBottom();
                                        mScrolledY = 0;
                                    }
                                }
                            }
                            if (mScrollTopListener != null
                                    && mFlexLayoutManager
                                    .findFlexFirstCompletelyVisibleItemPosition() == 0) {
                                if (mFlexLayoutManager.canFlexScrollHorizontally()) {
                                    if (Math.abs(mScrolledX) >= 1) {
                                        mScrollTopListener.onScrollTop();
                                        mScrolledX = 0;
                                    }
                                } else if (mFlexLayoutManager.canFlexScrollVertically()) {
                                    if (Math.abs(mScrolledY) >= 1) {
                                        mScrollTopListener.onScrollTop();
                                        mScrolledY = 0;
                                    }
                                }
                            }
                        } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                            // 滑动中手指抬起
                            if (mScrollTouchUpListener != null) {
                                mScrollTouchUpListener.onScrollTouchUp();
                            }
                        }
                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);

                        mScrolledX = dx;
                        mScrolledY = dy;

                        if (mScrollListener != null) {
                            mScrollListener.onScroll(dx, dy, mState);
                        }
                        processAppearanceEvent();
                    }
                });

        mRecyclerView.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        // attach恢复位置
                        if (mFlexLayoutManager instanceof FlexStaggeredGridLayoutManager) {
                            scrollToPosition(mPreviousScrollPosition, mPreviousScrollOffset, false);
                        }
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                    }
                });

        return mRecyclerView;
    }

    private int configPreviousScrollPositionAndOffset() {
        if (mFlexLayoutManager == null) {
            return 0;
        }
        View firtVisibleView = mFlexLayoutManager.getFlexChildAt(0);
        if (firtVisibleView != null) {
            // FIXME: 2020/6/1 mIsReverse场景未适配
            mPreviousScrollPosition = mFlexLayoutManager.getFlexChildPosition(firtVisibleView);
            if (mOrientation == OrientationHelper.VERTICAL) {
                mPreviousScrollOffset = firtVisibleView.getTop() - firtVisibleView.getPaddingTop();
            } else {
                mPreviousScrollOffset =
                        firtVisibleView.getLeft() - firtVisibleView.getPaddingLeft();
            }

            return mPreviousScrollOffset;
        }
        return 0;
    }

    private void initFlexLayoutManager() {
        if (mAttrsDomData != null) {
            Object typeObj = mAttrsDomData.get(LIST_LAYOUT_TYPE);
            if (typeObj instanceof String) {
                setFlexLayoutManager((String) typeObj);
                return;
            }
        }
        setFlexLayoutManager(LIST_GRID_TYPE);
    }

    protected FlexLayoutManager createLayoutManagerInner() {
        return LIST_STAGGER_TYPE.equals(mCurrentLayoutType) ? new FlexStaggeredGridLayoutManager(OrientationHelper.VERTICAL) :
                new FlexGridLayoutManager(mContext, mRecyclerViewImpl);
    }

    protected RecyclerViewAdapter createRecyclerViewInner() {
        return new FlexRecyclerView(mContext);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);
        if (METHOD_SCROLL_TO.equals(methodName)) {
            int position = 0;
            Object posObj = args.get(METHOD_PARAM_INDEX);
            if (posObj instanceof Integer) {
                position = (int) posObj;
            } else if (posObj != null) {
                mCallback.onJsException(
                        new IllegalAccessException(
                                "the index param of scrollTo function must be number"));
            }
            boolean isSmoothScroll = false;
            Object behaviorObj = args.get(METHOD_PARAM_BEHAVIOR); // 1070新增 behavior 参数
            if (behaviorObj != null) {
                isSmoothScroll = BEHAVIOR_SMOOTH.equals(behaviorObj.toString());
            } else {
                Object smoothObject = args.get(METHOD_PARAM_SMOOTH);
                if (smoothObject != null) {
                    try {
                        isSmoothScroll = Boolean.valueOf(smoothObject.toString());
                    } catch (Exception e) {
                        mCallback.onJsException(
                                new IllegalAccessException(
                                        "the smooth param of scrollBy function must be boolean"));
                    }
                }
            }
            scrollToPosition(position, isSmoothScroll);
        } else if (METHOD_SCROLL_BY.equals(methodName)) {
            float top = 0;
            Object topObj = args.get(METHOD_PARAM_TOP);
            if (topObj != null) {
                try {
                    top = Float.valueOf(topObj.toString());
                } catch (NumberFormatException e) {
                    mCallback.onJsException(
                            new IllegalAccessException(
                                    "the top param of scrollBy function must be number"));
                }
            }
            float left = 0;
            Object leftObj = args.get(METHOD_PARAM_LEFT);
            if (leftObj != null) {
                try {
                    left = Float.valueOf(leftObj.toString());
                } catch (NumberFormatException e) {
                    mCallback.onJsException(
                            new IllegalAccessException(
                                    "the left param of scrollBy function must be number"));
                }
            }
            boolean isSmoothScroll = false;
            Object behaviorObj = args.get(METHOD_PARAM_BEHAVIOR);
            if (behaviorObj != null) {
                isSmoothScroll = BEHAVIOR_SMOOTH.equals(behaviorObj.toString());
            } else {
                // 兼容 smooth 参数
                Object smoothObject = args.get(METHOD_PARAM_SMOOTH);
                if (smoothObject != null) {
                    try {
                        isSmoothScroll = Boolean.valueOf(smoothObject.toString());
                    } catch (Exception e) {
                        mCallback.onJsException(
                                new IllegalAccessException(
                                        "the smooth param of scrollBy function must be boolean"));
                    }
                }
            }
            scrollBy(left, top, isSmoothScroll);
        }
    }

    private void scrollBy(float left, float top, boolean isSmoothScroll) {
        if (mHost == null) {
            return;
        }
        float leftRealPx = DisplayUtil.getRealPxByWidth(left, mHapEngine.getDesignWidth());
        float topRealPx = DisplayUtil.getRealPxByWidth(top, mHapEngine.getDesignWidth());
        int leftInt = Math.round(leftRealPx);
        int topInt = Math.round(topRealPx);
        if (isSmoothScroll) {
            mHost.smoothScrollBy(leftInt, topInt);
        } else {
            mHost.scrollBy(leftInt, topInt);
        }
    }

    private void scrollToPosition(int position, boolean isSmoothScroll) {
        scrollToPosition(position, 0, isSmoothScroll);
    }

    private void scrollToPosition(int position, int offset, boolean isSmoothScroll) {
        mPreviousScrollPosition = position;
        mPreviousScrollOffset = offset;
        if (position > mAdapter.getItemCount() - 1) {
            position = mAdapter.getItemCount() - 1;
        }
        if (mFlexLayoutManager == null || mHost == null) {
            return;
        }

        if (position < 0) {
            position = 0;
        }
        if (isSmoothScroll) {
            mHost.smoothScrollToPosition(position);
        } else {
            mHost.stopScroll();
            int finalPosition = position;
            Handler hostHandler = mHost.getHandler();
            if (hostHandler != null) {
                hostHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                scrollToFlexPositionWithOffset(finalPosition, offset);
                            }
                        });
            } else {
                scrollToFlexPositionWithOffset(finalPosition, offset);
            }
        }
    }

    private void scrollToFlexPositionWithOffset(int position, int offset) {
        if (mFlexLayoutManager != null) {
            mFlexLayoutManager.scrollToFlexPositionWithOffset(position, offset);
            mPreviousScrollPosition = position;
            mPreviousScrollOffset = offset;
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.SCROLL_PAGE:
                mIsScrollPage = Attributes.getBoolean(attribute, false);
                setScrollPage(mIsScrollPage);
                return true;
            case Attributes.Style.COLUMNS:
                mColumnCount =
                        Attributes.getInt(mHapEngine, attribute,
                                FlexLayoutManager.DEFAULT_COLUMN_COUNT);
                if (mFlexLayoutManager != null) {
                    mFlexLayoutManager.setFlexSpanCount(mColumnCount);
                }
                return true;
            case LIST_LAYOUT_TYPE:
                String layoutType = Attributes.getString(attribute, mCurrentLayoutType);
                if (TextUtils.isEmpty(layoutType)) {
                    setFlexLayoutManager(LIST_GRID_TYPE);
                } else if (!layoutType.trim().equalsIgnoreCase(mCurrentLayoutType)) {
                    setFlexLayoutManager(layoutType);
                }
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    private void setFlexLayoutManager(String typeObj) {
        if (TextUtils.isEmpty(typeObj) || typeObj.trim().equalsIgnoreCase(mCurrentLayoutType)) {
            return;
        }
        if (LIST_STAGGER_TYPE.equalsIgnoreCase(typeObj.trim())) {
            mCurrentLayoutType = LIST_STAGGER_TYPE;
            mFlexLayoutManager = createLayoutManagerInner();
        } else if (LIST_GRID_TYPE.equalsIgnoreCase(typeObj.trim())) {
            mCurrentLayoutType = LIST_GRID_TYPE;
            mFlexLayoutManager = createLayoutManagerInner();
            mFlexLayoutManager.setSpanSizeLookup(new SpanSizeLookup());
        } else {
            mCallback.onJsException(
                    new IllegalAccessException("the layout-type of list must be grid or stagger"));
            return;
        }
        mFlexLayoutManager.setFlexRecyclerView(mRecyclerViewImpl);
        mFlexLayoutManager.setFlexSpanCount(mColumnCount);
        setScrollPage(mIsScrollPage);
        mRecyclerView.setLayoutManager(mFlexLayoutManager.getRealLayoutManager());
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.SCROLL.equals(event)) {
            mScrollListener =
                    new ScrollListener() {
                        @Override
                        public void onScroll(int dx, int dy, int state) {
                            Map<String, Object> params = new HashMap<>();
                            params.put(
                                    "scrollX", DisplayUtil
                                            .getDesignPxByWidth(dx, mHapEngine.getDesignWidth()));
                            params.put(
                                    "scrollY", DisplayUtil
                                            .getDesignPxByWidth(dy, mHapEngine.getDesignWidth()));
                            params.put("scrollState", state);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.SCROLL, List.this, params,
                                    null);
                        }
                    };
            return true;
        } else if (Attributes.Event.SCROLL_TOP.equals(event)) {
            mScrollTopListener =
                    new ScrollTopListener() {
                        @Override
                        public void onScrollTop() {
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.SCROLL_TOP, List.this, null,
                                    null);
                        }
                    };
            return true;
        } else if (Attributes.Event.SCROLL_BOTTOM.equals(event)) {
            mScrollBottomListener =
                    new ScrollBottomListener() {
                        @Override
                        public void onScrollBottom() {
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.SCROLL_BOTTOM, List.this,
                                    null, null);
                        }
                    };
            return true;
        } else if (Attributes.Event.SCROLL_END.equals(event)) {
            // 滑动结束
            mScrollEndListener =
                    new ScrollEndListener() {
                        @Override
                        public void onScrollEnd() {
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.SCROLL_END, List.this, null,
                                    null);
                        }
                    };
            return true;
        } else if (Attributes.Event.SCROLL_TOUCH_UP.equals(event)) {
            // 手指抬起
            mScrollTouchUpListener =
                    new ScrollTouchUpListener() {
                        @Override
                        public void onScrollTouchUp() {
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.SCROLL_TOUCH_UP, List.this,
                                    null, null);
                        }
                    };
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.SCROLL.equals(event)) {
            mScrollListener = null;
            return true;
        } else if (Attributes.Event.SCROLL_TOP.equals(event)) {
            mScrollTopListener = null;
            return true;
        } else if (Attributes.Event.SCROLL_BOTTOM.equals(event)) {
            mScrollBottomListener = null;
            return true;
        } else if (Attributes.Event.SCROLL_END.equals(event)) {
            // 滑动结束
            mScrollEndListener = null;
            return true;
        } else if (Attributes.Event.SCROLL_TOUCH_UP.equals(event)) {
            // 手指抬起
            mScrollTouchUpListener = null;
            return true;
        }

        return super.removeEvent(event);
    }

    @Override
    public void addChild(Component child, int index) {
        throw new IllegalArgumentException("will not come here");
    }

    @Override
    public void removeChild(Component child) {
        throw new IllegalArgumentException("will not come here");
    }

    @Override
    public void setFlexDirection(String flexDirectionStr) {
        if (TextUtils.isEmpty(flexDirectionStr)) {
            return;
        }
        if ("row".equals(flexDirectionStr)) {
            mOrientation = OrientationHelper.HORIZONTAL;
        } else if ("row-reverse".equals(flexDirectionStr)) {
            mOrientation = OrientationHelper.HORIZONTAL;
            mIsReverse = true;
        } else if ("column-reverse".equals(flexDirectionStr)) {
            mIsReverse = true;
        }
        mFlexLayoutManager.setFlexOrientation(mOrientation);
        mFlexLayoutManager.setFlexReverseLayout(mIsReverse);
    }

    public boolean isHorizontal() {
        return mFlexLayoutManager.getFlexOrientation() == OrientationHelper.HORIZONTAL;
    }

    private void setScrollPage(boolean scrollPage) {
        if (isHorizontal() || mHost == null || mFlexLayoutManager == null) {
            return;
        }

        mRecyclerViewImpl.setScrollPage(scrollPage);
        mFlexLayoutManager.setScrollPage(scrollPage);
    }

    @Override
    public void destroy() {
        super.destroy();
        mChildren.clear();
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        YogaNode node = YogaUtil.getYogaNode(mHost);
        if (node != null && mHost.getParent().getParent() instanceof ScrollView) {
            // set flex-grow to 1 because ScrollView use [0, UNSPECIFIED] to measure child view.
            int orientation = mFlexLayoutManager.getFlexOrientation();
            if ((orientation == OrientationHelper.HORIZONTAL && !isWidthDefined())
                    || (orientation == OrientationHelper.VERTICAL && !isHeightDefined())) {
                node.setFlexGrow(1f);
            }
        }

        super.onHostViewAttached(parent);
    }

    @Override
    public void ensureAppearanceManager() {
        if (mAppearanceManager == null) {
            mAppearanceManager = new RecycleAppearanceManager();
        }
    }

    private Component getParentListItem(Component component) {
        Component parentListItem = component;
        while (parentListItem != null) {
            if (parentListItem instanceof ListItem) {
                break;
            }
            parentListItem = parentListItem.getParent();
        }
        return parentListItem;
    }

    @Override
    public void setAppearanceWatch(Component component, int event, boolean isWatch) {
        component.setWatchAppearance(event, isWatch);
        if (isWatch) {
            ensureAppearanceManager();
            Component listItem = getParentListItem(component);
            if (null != listItem
                    && null != listItem.getHostView()
                    && listItem.getHostView().isAttachedToWindow()) {
                mAppearanceManager.bindAppearanceEvent(component);
            }
        } else if (mAppearanceManager != null) {
            mAppearanceManager.unbindAppearanceEvent(component);
        }
        processAppearanceOneEvent(component);
    }

    private void notifyItemChanged(int index) {
        if (mAdapter != null) {
            mAdapter.notifyItemChanged(index);
        }
    }

    private void notifyItemInserted(int index) {
        if (mAdapter != null) {
            mAdapter.notifyItemInserted(index);
            if (index == 0) {
                mHost.scrollToPosition(0);
            }
        }
    }

    private void notifyItemRemoved(int index) {
        if (mAdapter != null) {
            mAdapter.notifyItemRemoved(index);
        }
    }

    private void setRecyclerData(RecyclerItem recyclerItem) {
        mRecyclerItem = recyclerItem;
        if (mAdapter != null) {
            mAdapter.setData(recyclerItem);
        }
    }

    private RecyclerItem getRecyclerItem() {
        return mRecyclerItem;
    }

    @Override
    public RecyclerDataItem.Creator getRecyclerDataItemCreator() {
        return RecyclerDataItemFactory.getInstance();
    }

    private interface ScrollListener {
        void onScroll(int dx, int dy, int state);
    }

    private interface ScrollBottomListener {
        void onScrollBottom();
    }

    private interface ScrollTopListener {
        void onScrollTop();
    }

    private interface ScrollEndListener {
        void onScrollEnd();
    }

    private interface ScrollTouchUpListener {
        void onScrollTouchUp();
    }

    public static class RecyclerItem extends Container.RecyclerItem {
        private SparseArray<RecyclerDataTemplate> mListItemTemplates = new SparseArray<>();
        private Parcelable mInstanceState;

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        protected void onApplyDataToComponent(Component recycle) {
            super.onApplyDataToComponent(recycle);
            ((List) recycle).setRecyclerData(this);
            RecyclerView.LayoutManager layoutManager =
                    ((RecyclerView) recycle.getHostView()).getLayoutManager();
            if (layoutManager == null) {
                Log.e(TAG, "onApplyDataToComponent: layoutManager is null");
                return;
            }
            if (mInstanceState != null) {
                layoutManager.onRestoreInstanceState(mInstanceState);
            } else {
                layoutManager.scrollToPosition(0);
            }
        }

        @Override
        public void unbindComponent() {
            if (getBoundComponent() != null) {
                RecyclerView.LayoutManager layoutManager =
                        ((RecyclerView) getBoundComponent().getHostView()).getLayoutManager();
                if (layoutManager != null) {
                    mInstanceState = layoutManager.onSaveInstanceState();
                }
                ((List) getBoundComponent()).setRecyclerData(null);
            }
            super.unbindComponent();
        }

        void notifyItemChanged(ListItem.RecyclerItem item) {
            if (getBoundComponent() != null) {
                ((List) getBoundComponent()).notifyItemChanged(getChildren().indexOf(item));
            }
        }

        @Override
        public void onChildAdded(RecyclerDataItem child, int index) {
            super.onChildAdded(child, index);
            if (getBoundComponent() != null) {
                ((List) getBoundComponent()).notifyItemInserted(index);
            }
        }

        @Override
        public void onChildRemoved(RecyclerDataItem child, int index) {
            super.onChildRemoved(child, index);
            if (getBoundComponent() != null) {
                ((List) getBoundComponent()).notifyItemRemoved(index);
            }
        }

        void attachToTemplate(ListItem.RecyclerItem item) {
            RecyclerDataTemplate template = mListItemTemplates.get(item.getViewType());

            if (template == null) {
                template = new RecyclerDataTemplate(item);
                setListItemTemplate(item.getViewType(), template);
            }

            item.attachToTemplate(template);
        }

        private void setListItemTemplate(int type, RecyclerDataTemplate template) {
            mListItemTemplates.put(type, template);
        }

        @Override
        public boolean isSupportTemplate() {
            return true;
        }
    }

    private class Holder extends RecyclerView.ViewHolder {
        private Component mRecycleComponent;
        private RecyclerDataItem mItem;

        public void setContentDescription(String description) {
            if (!TextUtils.isEmpty(description) && null != mRecycleComponent) {
                View hostView = mRecycleComponent.getHostView();
                if (null != hostView) {
                    hostView.setContentDescription(description);
                }
            }
        }

        Holder(Component instance) {
            super(instance.getHostView());
            mRecycleComponent = instance;
        }

        void bind(RecyclerDataItem item) {
            mItem = item;
            item.dispatchBindComponent(mRecycleComponent);
        }

        void unbind() {
            mItem.dispatchUnbindComponent();
            mItem.destroy();
            mItem = null;
        }

        Component getRecycleComponent() {
            return mRecycleComponent;
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        private RecyclerItemList mData;
        private List.RecyclerItem mRecyclerItem;
        private int mCreateViewPosition = 0;

        Adapter() {
            setHasStableIds(true);
        }

        public void setData(List.RecyclerItem recyclerItem) {
            mRecyclerItem = recyclerItem;
            if (recyclerItem == null) {
                mData = null;
            } else {
                mData = recyclerItem.getChildren();
            }
            if (mRecyclerView.isComputingLayout()) {
                Handler handler = mRecyclerView.getHandler();
                if (handler != null) {
                    handler.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                }
            } else {
                notifyDataSetChanged();
            }
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            ListItem.RecyclerItem item = getItem(mCreateViewPosition);
            if (item.getViewType() != viewType) {
                throw new IllegalStateException("will not be here");
            }
            mRecyclerItem.attachToTemplate(item);
            Component recycle = item.createRecycleComponent(List.this);
            recycle.createView();
            mChildren.add(recycle); // so: it will be destroy when list destroy
            Holder holder = new Holder(recycle);
            if (mRecyclerView.getLayoutManager() instanceof FlexStaggeredGridLayoutManager
                    && item.getColumnSpan() == mFlexLayoutManager.getFlexSpanCount()) {
                HapStaggeredGridLayoutManager.LayoutParams layoutParams =
                        new HapStaggeredGridLayoutManager.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setFullSpan(true);
                holder.itemView.setLayoutParams(layoutParams);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            ListItem.RecyclerItem item = getItem(position);
            mRecyclerItem.attachToTemplate(item);
            if (mIsEnableTalkBack) {
                String description = item.getViewDescription();
                if (TextUtils.isEmpty(description)) {
                    if (null != mContext) {
                        holder.setContentDescription(mContext.getResources().getString(R.string.talkback_notitle_defaultstr));
                    }
                } else {
                    holder.setContentDescription(description);
                }
            }
            holder.bind(item);
            Component component = holder.getRecycleComponent();
            lazySetAppearanceWatch(component);
        }

        @Override
        public void onViewRecycled(Holder holder) {
            Component component = holder.getRecycleComponent();
            if (mAppearanceManager instanceof RecycleAppearanceManager) {
                ((RecycleAppearanceManager) mAppearanceManager).recycleHelper(component);
            }
            holder.unbind();
            mChildren.remove(component);
        }

        @Override
        public void onViewAttachedToWindow(Holder holder) {
            Component component = holder.getRecycleComponent();
            component.onHostViewAttached(mHost); // For ListItem setMargin
        }

        @Override
        public void onViewDetachedFromWindow(Holder holder) {
            processAppearanceEvent();
        }

        @Override
        public int getItemCount() {
            return mData == null ? 0 : mData.size();
        }

        @Override
        public int getItemViewType(int position) {
            int viewType = getItem(position).getViewType();
            mCreateViewPosition = position;
            return viewType;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        ListItem.RecyclerItem getItem(int position) {
            // TODO mCallback.onJsException(new Exception("list child component must be list-item"));
            RecyclerDataItem item = mData.get(position);
            if (item instanceof ListItem.RecyclerItem) {
                return (ListItem.RecyclerItem) item;
            }
            if (mCallback != null) {
                mCallback.onJsException(new Exception("list child component must be list-item"));
            }
            throw new IllegalStateException("list child component must be list-item");
        }
    }

    private class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

        @Override
        public int getSpanSize(int position) {
            ListItem.RecyclerItem item = mAdapter.getItem(position);
            int columnSpan = item.getColumnSpan();
            if (columnSpan > mColumnCount) {
                String exceptionStr =
                        "list-item at position "
                                + position
                                + " requires "
                                + columnSpan
                                + " spans but list has only "
                                + mColumnCount
                                + " columns.";
                if (mCallback != null) {
                    mCallback.onJsException(new IllegalArgumentException(exceptionStr));
                } else {
                    Log.e(TAG, "getSpanSize: " + exceptionStr);
                }
                return FlexLayoutManager.DEFAULT_COLUMN_COUNT;
            }
            return columnSpan;
        }
    }
}
