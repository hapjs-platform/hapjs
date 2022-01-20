/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist.model;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.RecyclerItemList;
import org.hapjs.component.utils.map.CombinedMap;
import org.hapjs.widgets.sectionlist.SectionGroup;
import org.hapjs.widgets.sectionlist.SectionHeader;
import org.hapjs.widgets.sectionlist.SectionItem;
import org.hapjs.widgets.sectionlist.SectionListAdapter;

public class ItemGroup extends Item {

    private static final String TAG = "ItemGroup";

    private ItemHeader mHeader;
    private List<Item> mChildren;
    private boolean mExpand;
    private OnExpandStateChangeListener mExpandStateChangeListener;
    private boolean mBlock;

    public ItemGroup(SectionGroup.RecyclerItem recyclerItem) {
        super(recyclerItem);
        mChildren = initChild();
        SectionHeader.RecyclerItem headerRecyclerItem = recyclerItem.getHeaderRecyclerItem();
        if (headerRecyclerItem != null) {
            mHeader = (ItemHeader) headerRecyclerItem.getItem();
            mHeader.setParent(this);
        }
    }

    private List<Item> initChild() {
        List<Item> childItems = new ArrayList<>();
        SectionGroup.RecyclerItem recyclerItem = getData();
        RecyclerItemList children = recyclerItem.getChildren();
        int size = children.size();
        for (int i = 0; i < size; i++) {
            RecyclerDataItem child = children.get(i);
            if (child instanceof SectionItem.RecyclerItem
                    && !(child instanceof SectionHeader.RecyclerItem)) {
                childItems.add(((SectionItem.RecyclerItem) child).getItem());
                ((SectionItem.RecyclerItem) child).getItem().setParent(this);
            }
        }
        return childItems;
    }

    public ItemHeader getHeaderItem() {
        return mHeader;
    }

    public void onHeadAdd(@NonNull SectionHeader.RecyclerItem headerItem) {
        if (Objects.equals(mHeader, headerItem.getItem())) {
            return;
        }

        mHeader = (ItemHeader) headerItem.getItem();
        // header的parent为SectionGroup，但是SectionGroup的children里面不包含header。
        mHeader.setParent(this);

        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        adapter.post(
                () -> {
                    int itemPosition = adapter.getItemPosition(ItemGroup.this);
                    if (itemPosition >= 0) {
                        adapter.addItem(itemPosition + 1, mHeader);
                    }
                });
    }

    public void onChildAdd(int index, SectionItem.RecyclerItem recyclerDataItem) {
        if (index < 0 || index > getChildCount()) {
            Log.e(
                    TAG,
                    "onChildAdd error,index is out of array index,current index:"
                            + index
                            + "  totalSize:"
                            + getChildCount());
            return;
        }

        Item item = recyclerDataItem.getItem();
        mChildren.add(index, item);
        item.setParent(this);

        if (!isExpand()) {
            // 没有展开，不需要显示child
            return;
        }

        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        adapter.post(
                () -> {
                    int previousItemPosition = findPreviousItemPosition(item, adapter);
                    if (previousItemPosition >= 0) {
                        adapter.addItem(previousItemPosition + 1, item);
                    } else {
                        Log.e(TAG, "onChildAdd fail,cannot previous item's position,itemInfo: "
                                + item);
                    }
                });
    }

    private int findPreviousItemPosition(Item item, SectionListAdapter adapter) {
        Item beginItem = mHeader != null ? mHeader : this;
        int beginPosition = adapter.getItemPosition(beginItem);
        if (beginPosition < 0) {
            return -1;
        }
        int index = mChildren.indexOf(item);
        if (index > 0) {
            // children顺序中的前一个item
            Item previousItem = mChildren.get(index - 1);
            if (previousItem instanceof ItemGroup) {
                if (previousItem.isExpand()) {
                    List<Item> expandChildren = ((ItemGroup) previousItem).getExpandChildren();
                    if (expandChildren.size() > 0) {
                        // SectionGroup展开了，前一个item就是展开的child的最后一个item
                        previousItem = expandChildren.get(expandChildren.size() - 1);
                    } else if (((ItemGroup) previousItem).getHeaderItem() != null) {
                        // SectionGroup展开，但是没有child，那么前一个item就是header
                        previousItem = ((ItemGroup) previousItem).getHeaderItem();
                    }
                } else if (((ItemGroup) previousItem).getHeaderItem() != null) {
                    // SectionGroup没有展开，那么前一个item就是header
                    previousItem = ((ItemGroup) previousItem).getHeaderItem();
                }
            }
            beginPosition = adapter.getItemPosition(previousItem);
        }
        return beginPosition;
    }

    public void onHeadRemoved(SectionHeader.RecyclerItem headerItem) {
        if (mHeader == null) {
            return;
        }
        if (!Objects.equals(mHeader, headerItem.getItem())) {
            return;
        }

        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        adapter.removeItem(mHeader);
        mHeader.setParent(null);
        mHeader = null;
    }

    public void onChildRemove(SectionItem.RecyclerItem recyclerDataItem) {
        Item item = recyclerDataItem.getItem();
        mChildren.remove(item);
        // 如果没有展开，该item没有被添加到adapter里面去
        if (!isExpand()) {
            return;
        }

        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        if (item instanceof ItemGroup) {
            List<Item> deleteNodes = ((ItemGroup) item).getExpandChildren();
            ItemHeader headerItem = ((ItemGroup) item).getHeaderItem();
            if (headerItem != null) {
                deleteNodes.add(0, headerItem);
            }
            deleteNodes.add(0, item);
            adapter.removeItems(deleteNodes);
        } else {
            adapter.removeItem(item);
        }
    }

    public List<Item> getChildren() {
        return mChildren;
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public Item getChildAt(int index) {
        int size = mChildren.size();
        if (index < 0 || index >= size) {
            return null;
        }
        return mChildren.get(index);
    }

    public int indexOf(Item item) {
        return mChildren.indexOf(item);
    }

    @Override
    public boolean isExpand() {
        return mExpand;
    }

    public void setExpand(boolean expand) {
        if (mExpand == expand) {
            return;
        }
        notifyStateChanged(expand);

        ItemGroup parent = getParent();
        if (parent != null && !parent.isExpand()) {
            // parent折叠，那么自己以及child都将不能展开显示，处于冻结状态
            mBlock = true;
            return;
        }

        mBlock = false;

        if (expand) {
            onExpand();
        } else {
            onCollapse();
        }
    }

    public void setExpandStateChangeListener(
            OnExpandStateChangeListener expandStateChangeListener) {
        mExpandStateChangeListener = expandStateChangeListener;
    }

    private void notifyStateChanged(boolean expand) {
        if (mExpand == expand) {
            return;
        }
        mExpand = expand;
        CombinedMap<String, Object> attrsDomData = getData().getAttrsDomData();
        if (attrsDomData != null) {
            attrsDomData.put(SectionGroup.ATTR_EXPAND, expand);
        }
        if (mExpandStateChangeListener != null) {
            mExpandStateChangeListener.onStateChanged(mExpand);
        }
    }

    /**
     * 获取展开的child
     *
     * @return
     */
    @NonNull
    public List<Item> getExpandChildren() {
        List<Item> expandList = new ArrayList<>();
        List<Item> children = getChildren();
        for (Item child : children) {
            expandList.add(child);
            if (child instanceof ItemGroup) {
                ItemHeader headerItem = ((ItemGroup) child).getHeaderItem();
                if (headerItem != null) {
                    // header作为group显示
                    expandList.add(headerItem);
                }
                if (child.isExpand() && !isBlock()) {
                    // 处于block状态，没有在adapter中显示
                    expandList.addAll(((ItemGroup) child).getExpandChildren());
                }
            }
        }
        return expandList;
    }

    private boolean isBlock() {
        ItemGroup parent = getParent();
        if (parent == null) {
            return false;
        }
        if (!parent.mBlock) {
            mBlock = false;
            return false;
        }
        return mBlock;
    }

    private void onExpand() {
        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        List<Item> children = getExpandChildren();
        if (children.size() <= 0) {
            return;
        }

        int position = -1;
        Item item = getHeaderItem();
        if (item != null) {
            // header存在
            position = adapter.getItemPosition(item) + 1;
        } else {
            position = adapter.getItemPosition(this) + 1;
        }

        if (position >= 0) {
            adapter.addItems(position, children);
        }
    }

    private void onCollapse() {
        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        List<Item> children = getExpandChildren();
        if (children.size() <= 0) {
            return;
        }

        adapter.removeItems(children);
    }

    public void scrollTo(int position, boolean smooth) {
        if (!isExpand()) {
            // 处于折叠状态，无法滚动
            return;
        }

        int childCount = getChildCount();
        if (position < 0 || position >= childCount) {
            return;
        }

        SectionListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        Item item = getChildren().get(position);
        int itemPosition = adapter.getItemPosition(item);
        if (itemPosition < 0) {
            return;
        }
        adapter.scrollTo(itemPosition, smooth);
    }

    public interface OnExpandStateChangeListener {
        void onStateChanged(boolean isExpand);
    }
}
