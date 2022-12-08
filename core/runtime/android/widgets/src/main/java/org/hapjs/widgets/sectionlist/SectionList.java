/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist;

import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.RecyclerDataItemFactory;
import org.hapjs.widgets.sectionlist.model.Item;
import org.hapjs.widgets.sectionlist.model.ItemList;
import org.hapjs.widgets.view.list.section.SectionListLayoutManager;
import org.hapjs.widgets.view.list.section.SectionRecyclerView;

@WidgetAnnotation(
        name = SectionList.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                SectionList.METHOD_SCROLL_TO,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class SectionList extends AbstractScrollable<SectionRecyclerView>
        implements Recycler, SwipeObserver {

    public static final String WIDGET_NAME = "section-list";
    protected static final String METHOD_SCROLL_TO = "scrollTo";
    private static final String EVENT_SCROLL = "scroll";
    private static final String EVENT_SCROLL_END = "scrollend";
    private static final String EVENT_SCROLL_TOUCH_UP = "scrolltouchup";
    private static final String EVENT_SCROLL_TOP = "scrolltop";
    private static final String EVENT_SCROLL_BOTTOM = "scrollbottom";
    private static final String METHOD_PARAM_INDEX = "index";
    private static final String METHOD_PARAM_BEHAVIOR = "behavior";
    private static final String BEHAVIOR_SMOOTH = "smooth";

    private SectionRecyclerView mRecyclerView;
    private Set<String> mRegisterEvents = new HashSet<>();
    private SectionListLayoutManager mLayoutManager;
    private SectionListAdapter mAdapter;
    private ItemList mItemList;

    public SectionList(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected SectionRecyclerView createViewImpl() {
        Context context = mContext;
        mRecyclerView = new SectionRecyclerView(context);
        mRecyclerView.setComponent(this);
        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mRecyclerView.setLayoutParams(params);

        mLayoutManager = new SectionListLayoutManager(context, mRecyclerView);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setItemAnimator(null);
        mAdapter = new SectionListAdapter(this);
        mAdapter.bindRecyclerView(mRecyclerView);

        mRecyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    private int mState = RecyclerView.SCROLL_STATE_IDLE;

                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView,
                                                     int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        mState = newState;

                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                            if (mRegisterEvents.contains(EVENT_SCROLL_END)) {
                                notifyEventScrollEnd();
                            }

                            if (mRegisterEvents.contains(EVENT_SCROLL)) {
                                notifyEventScroll(0, 0, newState);
                            }

                            if (mRegisterEvents.contains(EVENT_SCROLL_BOTTOM)) {
                                int lastVisibleItemPosition = mLayoutManager.findLastCompletelyVisibleItemPosition();
                                if (lastVisibleItemPosition == mAdapter.getItemCount() - 1) {
                                    notifyEventScrollBottom();
                                }
                            }

                            if (mRegisterEvents.contains(EVENT_SCROLL_TOP)) {
                                int firstVisibleItemPosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                                if (firstVisibleItemPosition == 0) {
                                    notifyEventScrollTop();
                                }
                            }
                        } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                            // 滑动中手指抬起
                            if (mRegisterEvents.contains(EVENT_SCROLL_TOUCH_UP)) {
                                notifyEventTouchUp();
                            }
                        }
                    }

                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (mRegisterEvents.contains(EVENT_SCROLL)) {
                            notifyEventScroll(dx, dy, mState);
                        }
                        processAppearanceEvent();
                    }
                });
        return mRecyclerView;
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event)) {
            return false;
        }
        if (isScrollEvent(event)) {
            mRegisterEvents.add(event);
            return true;
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (mRegisterEvents.remove(event)) {
            return true;
        }
        return super.removeEvent(event);
    }

    private void notifyEventScroll(float dx, float dy, int state) {
        Map<String, Object> params = new HashMap<>();
        params.put("scrollX", DisplayUtil.getDesignPxByWidth(dx, mHapEngine.getDesignWidth()));
        params.put("scrollY", DisplayUtil.getDesignPxByWidth(dy, mHapEngine.getDesignWidth()));
        params.put("scrollState", state);
        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_SCROLL, this, params, null);
    }

    private void notifyEventScrollTop() {
        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_SCROLL_TOP, this, null, null);
    }

    private void notifyEventScrollBottom() {
        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_SCROLL_BOTTOM, this, null, null);
    }

    private void notifyEventScrollEnd() {
        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_SCROLL_END, this, null, null);
    }

    private void notifyEventTouchUp() {
        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_SCROLL_TOUCH_UP, this, null, null);
    }

    protected boolean isScrollEvent(String event) {
        return TextUtils.equals(EVENT_SCROLL, event)
                || TextUtils.equals(EVENT_SCROLL_BOTTOM, event)
                || TextUtils.equals(EVENT_SCROLL_END, event)
                || TextUtils.equals(EVENT_SCROLL_TOP, event)
                || TextUtils.equals(EVENT_SCROLL_TOUCH_UP, event);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (METHOD_SCROLL_TO.equals(methodName)) {
            int position = 0;
            Object index = args.get(METHOD_PARAM_INDEX);
            if (index instanceof Integer) {
                position = (int) index;
            } else if (index != null) {
                mCallback.onJsException(
                        new IllegalAccessException(
                                "the index param of scrollTo function must be number"));
                return;
            }

            Object behavior = args.get(METHOD_PARAM_BEHAVIOR);
            boolean smooth = false;
            if (behavior != null) {
                smooth = BEHAVIOR_SMOOTH.equalsIgnoreCase(behavior.toString());
            }
            scrollTo(position, smooth);
            return;
        }
        super.invokeMethod(methodName, args);
    }

    private void scrollTo(int position, boolean smooth) {
        if (mRecyclerView == null || mItemList == null) {
            return;
        }

        int childCount = mItemList.getChildCount();
        if (position < 0 || position >= childCount) {
            return;
        }

        Item item = mItemList.getChildren().get(position);
        int itemPosition = mAdapter.getItemPosition(item);
        if (itemPosition < 0) {
            return;
        }

        if (smooth) {
            mRecyclerView.smoothScrollToPosition(itemPosition);
            return;
        }

        mRecyclerView.scrollToPosition(itemPosition);
    }

    @Override
    public RecyclerDataItem.Creator getRecyclerDataItemCreator() {
        return RecyclerDataItemFactory.getInstance();
    }

    private void setItemList(ItemList itemList) {
        if (itemList != null) {
            itemList.setAdapter(mAdapter);
        } else if (mItemList != null) {
            mItemList.setAdapter(null);
        }
        mItemList = itemList;
    }

    public static class RecyclerItem extends Container.RecyclerItem {

        private ItemList mItemList;
        private Parcelable mInstanceState;
        private SparseArray<RecyclerDataTemplate> mListItemTemplates = new SparseArray<>();
        private ArrayList<RecyclerDataItem> mSkipItems = new ArrayList<>();

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
            mItemList = new ItemList(this);
        }

        @Override
        protected void onApplyDataToComponent(Component recycle) {
            super.onApplyDataToComponent(recycle);
            ((SectionList) recycle).setItemList(mItemList);
            RecyclerView.LayoutManager layoutManager = ((SectionList) recycle).mLayoutManager;
            if (layoutManager != null && mInstanceState != null) {
                layoutManager.onRestoreInstanceState(mInstanceState);
            }
        }

        @Override
        public void unbindComponent() {
            SectionList sectionList = getBoundComponent();
            if (sectionList != null) {
                SectionListLayoutManager layoutManager = sectionList.mLayoutManager;
                mInstanceState = layoutManager.onSaveInstanceState();
                sectionList.setItemList(null);
            }
            super.unbindComponent();
        }

        public int getValidChildPosition(RecyclerDataItem child) {
            RecyclerItemList children = getChildren();
            int index = children.indexOf(child);
            if (index < 0) {
                // 不包含该child
                return -1;
            }

            int skip = 0;
            for (RecyclerDataItem skipItem : mSkipItems) {
                int skipIndex = children.indexOf(skipItem);
                if (skipIndex < index) {
                    skip++;
                }
            }
            return index - skip;
        }

        @Override
        public void onChildAdded(RecyclerDataItem child, int index) {
            super.onChildAdded(child, index);

            if (!isSupportDataType(child)) {
                mSkipItems.add(child);
                return;
            }
            int position = getValidChildPosition(child);
            mItemList.addChild(position, (SectionItem.RecyclerItem) child);
        }

        @Override
        public void onChildRemoved(RecyclerDataItem child, int index) {
            super.onChildRemoved(child, index);
            if (!isSupportDataType(child)) {
                mSkipItems.remove(child);
                return;
            }
            mItemList.removeChild((SectionItem.RecyclerItem) child);
        }

        @Override
        public boolean isSupportTemplate() {
            return true;
        }

        public void attachTemplate(RecyclerDataItem item) {
            int type = getItemViewType(item);
            RecyclerDataTemplate template = mListItemTemplates.get(type);

            if (template == null) {
                template = new RecyclerDataTemplate(item);
                mListItemTemplates.put(type, template);
            }

            item.attachToTemplate(template);
        }

        int getItemViewType(RecyclerDataItem dataItem) {
            if (dataItem instanceof SectionItem.RecyclerItem) {
                return ((SectionItem.RecyclerItem) dataItem).getViewType();
            }
            return dataItem.hashCode();
        }

        private boolean isSupportDataType(RecyclerDataItem dataItem) {
            if (dataItem instanceof SectionHeader.RecyclerItem) {
                // section-list不支持直接section-header
                return false;
            }
            return dataItem instanceof SectionItem.RecyclerItem;
        }
    }
}
