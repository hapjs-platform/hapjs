/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.AbstractScrollable;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.RecyclerItemList;
import org.hapjs.component.appearance.RecycleAppearanceManager;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.sectionlist.model.Item;
import org.hapjs.widgets.sectionlist.model.ItemGroup;

@WidgetAnnotation(
        name = SectionGroup.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                SectionGroup.METHOD_EXPAND,
                SectionGroup.METHOD_SCROLL_TO,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class SectionGroup extends AbstractScrollable<FrameLayout> {

    public static final String WIDGET_NAME = "section-group";
    public static final String ATTR_EXPAND = "expand";
    protected static final String METHOD_EXPAND = "expand";
    protected static final String METHOD_SCROLL_TO = "scrollTo";
    private static final String TAG = "SectionGroup";
    private static final String METHOD_PARAM_EXPAND = "expand";
    private static final String METHOD_PARAM_INDEX = "index";
    private static final String METHOD_PARAM_BEHAVIOR = "behavior";
    private static final String BEHAVIOR_SMOOTH = "smooth";

    private static final String EVENT_CHANGE = "change";
    private static final int STATE_COLLAPSE = 1;
    private static final int STATE_EXPAND = 2;

    private RecyclerItem mRecyclerItem;
    private ItemGroup.OnExpandStateChangeListener mOnExpandStateChangeListener = null;

    public SectionGroup(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected FrameLayout createViewImpl() {
        // SectionGroup的child在adapter中做了扁平化处理，在View层面，所有的SctionGroup、SectionItem和
        // SectionHeader都是一层，SectionGroup对应的HostView的大小为0.
        FrameLayout view = new FrameLayout(mContext);
        ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(0, 0);
        view.setLayoutParams(p);
        return view;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        if (ATTR_EXPAND.equals(key)) {
            boolean expand = Attributes.getBoolean(attribute, false);
            expand(expand);
        }
        return true;
    }

    @Override
    public void addView(View childView, int index) {
        // un-support child view
    }

    private ItemGroup getItemGroup() {
        return mRecyclerItem != null ? (ItemGroup) mRecyclerItem.getItem() : null;
    }

    @Override
    protected boolean addEvent(String event) {
        if (EVENT_CHANGE.equals(event)) {
            if (mOnExpandStateChangeListener == null) {
                mOnExpandStateChangeListener = new ItemGroup.OnExpandStateChangeListener() {
                    @Override
                    public void onStateChanged(boolean isExpand) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("state", isExpand ? STATE_EXPAND : STATE_COLLAPSE);
                        mCallback.onJsEventCallback(getPageId(), getRef(), EVENT_CHANGE, SectionGroup.this, params, null);
                    }
                };
            }
            ItemGroup itemGroup = getItemGroup();
            if (itemGroup != null) {
                itemGroup.setExpandStateChangeListener(mOnExpandStateChangeListener);
            }
            return true;
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (EVENT_CHANGE.equals(event)) {
            ItemGroup itemGroup = getItemGroup();
            if (itemGroup != null) {
                itemGroup.setExpandStateChangeListener(null);
            }
            mOnExpandStateChangeListener = null;
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (TextUtils.equals(methodName, METHOD_EXPAND)) {
            if (mRecyclerItem != null) {
                mRecyclerItem.invokeMethod(methodName, args);
            }
            return;
        } else if (TextUtils.equals(methodName, METHOD_SCROLL_TO)) {
            if (mRecyclerItem != null) {
                mRecyclerItem.invokeMethod(methodName, args);
            }
            return;
        }
        super.invokeMethod(methodName, args);
    }

    private void expand(boolean expand) {
        if (mRecyclerItem == null) {
            return;
        }

        ItemGroup itemGroup = (ItemGroup) mRecyclerItem.getItem();
        itemGroup.setExpand(expand);
    }

    @Override
    public void addChild(Component child, int index) {
        if (child instanceof SectionHeader) {
            super.addChild(child, 0);
        }
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        YogaNode node = YogaUtil.getYogaNode(mHost);
        if (node != null && mHost.getParent() instanceof PercentFlexboxLayout) {
            // set flex-grow to 1 because ScrollView use [0, UNSPECIFIED] to measure child view.
            if (!isHeightDefined()) {
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

    private void onBind(RecyclerItem recyclerItem) {
        mRecyclerItem = recyclerItem;
        ItemGroup itemGroup = (ItemGroup) mRecyclerItem.getItem();
        if (mOnExpandStateChangeListener != null && itemGroup != null) {
            itemGroup.setExpandStateChangeListener(mOnExpandStateChangeListener);
        }
    }

    private void unBind() {
        ItemGroup itemGroup = (ItemGroup) mRecyclerItem.getItem();
        if (itemGroup != null) {
            itemGroup.setExpandStateChangeListener(null);
        }
        mRecyclerItem = null;
    }

    public static class RecyclerItem extends SectionItem.RecyclerItem {

        private SectionHeader.RecyclerItem mHeaderRecyclerItem;
        private ArrayList<RecyclerDataItem> mSkipItems = new ArrayList<>();
        private ArrayList<SectionHeader.RecyclerItem> mHeaderItems = new ArrayList<>();

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        protected Item createItem() {
            return new ItemGroup(this);
        }

        @Override
        public void bindAttrs(Map attrs) {
            super.bindAttrs(attrs);
            if (getBoundComponent() == null && attrs != null) {
                Object expand = attrs.get(ATTR_EXPAND);
                if (expand != null) {
                    try {
                        boolean e = Boolean.parseBoolean(expand.toString());
                        ((ItemGroup) getItem()).setExpand(e);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        @Override
        protected void onApplyDataToComponent(Component recycle) {
            SectionGroup sectionGroup = (SectionGroup) recycle;
            sectionGroup.onBind(this);
            super.onApplyDataToComponent(recycle);
        }

        @Override
        public void unbindComponent() {
            SectionGroup sectionGroup = getBoundComponent();
            if (sectionGroup != null) {
                sectionGroup.unBind();
            }
            super.unbindComponent();
        }

        private boolean isSupportChild(RecyclerDataItem child) {
            // 包含section-header section-item和section-
            if (!(child instanceof SectionItem.RecyclerItem)) {
                return false;
            }

            if (child instanceof SectionHeader.RecyclerItem
                    && mHeaderRecyclerItem != null
                    && !Objects.equals(mHeaderRecyclerItem, child)) {
                // 如果已经有header了，该child则无效
                return false;
            }
            return true;
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
            if (!mHeaderItems.isEmpty()) {
                for (SectionHeader.RecyclerItem headerItem : mHeaderItems) {
                    if (children.indexOf(headerItem) < index) {
                        skip++;
                    }
                }
            }
            return index - skip;
        }

        @Override
        public void onChildAdded(RecyclerDataItem child, int index) {
            super.onChildAdded(child, index);
            if (!isSupportChild(child)) {
                mSkipItems.add(child);
                return;
            }
            // header与其他的item会被分开，单独处理
            ItemGroup itemGroup = (ItemGroup) getItem();
            if (child instanceof SectionHeader.RecyclerItem) {
                SectionHeader.RecyclerItem headerItem = (SectionHeader.RecyclerItem) child;
                mHeaderItems.add((SectionHeader.RecyclerItem) child);
                if (mHeaderRecyclerItem == null) {
                    mHeaderRecyclerItem = headerItem;
                    itemGroup.onHeadAdd(headerItem);
                }
                return;
            }

            int position = getValidChildPosition(child);
            itemGroup.onChildAdd(position, (SectionItem.RecyclerItem) child);
        }

        @Override
        public void onChildRemoved(RecyclerDataItem child, int index) {
            super.onChildRemoved(child, index);
            if (!isSupportChild(child)) {
                mSkipItems.remove(child);
                return;
            }

            ItemGroup itemGroup = (ItemGroup) getItem();
            if (child instanceof SectionHeader.RecyclerItem) {
                SectionHeader.RecyclerItem headerItem = (SectionHeader.RecyclerItem) child;
                mHeaderItems.remove(headerItem);
                if (Objects.equals(mHeaderRecyclerItem, child)) {
                    itemGroup.onHeadRemoved(mHeaderRecyclerItem);
                    mHeaderRecyclerItem = null;
                }
                return;
            }

            itemGroup.onChildRemove((SectionItem.RecyclerItem) child);
        }

        @Override
        protected void requestBindTemplate() {
            // ItemGroup是个空壳子，啥都不做
        }

        public SectionHeader.RecyclerItem getHeaderRecyclerItem() {
            return mHeaderRecyclerItem;
        }

        @Override
        public void invokeMethod(String methodName, Map<String, Object> args) {
            if (TextUtils.equals(methodName, METHOD_EXPAND)) {
                Object o = args.get(METHOD_PARAM_EXPAND);
                if (o == null) {
                    return;
                }

                boolean expand = Boolean.parseBoolean(o.toString());
                ItemGroup itemGroup = (ItemGroup) getItem();
                itemGroup.setExpand(expand);
                return;
            } else if (TextUtils.equals(methodName, METHOD_SCROLL_TO)) {
                int position = 0;
                Object index = args.get(METHOD_PARAM_INDEX);
                if (index instanceof Integer) {
                    position = (int) index;
                } else if (index != null) {
                    Log.e(TAG, "call " + METHOD_SCROLL_TO + " fail,unknown index param:" + index);
                    return;
                }

                Object behavior = args.get(METHOD_PARAM_BEHAVIOR);
                boolean smooth = false;
                if (behavior != null) {
                    smooth = BEHAVIOR_SMOOTH.equalsIgnoreCase(behavior.toString());
                }
                ItemGroup itemGroup = (ItemGroup) getItem();
                itemGroup.scrollTo(position, smooth);
                return;
            }
            super.invokeMethod(methodName, args);
        }
    }
}
