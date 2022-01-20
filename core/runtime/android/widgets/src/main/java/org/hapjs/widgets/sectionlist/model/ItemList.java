/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hapjs.widgets.sectionlist.SectionItem;
import org.hapjs.widgets.sectionlist.SectionList;
import org.hapjs.widgets.sectionlist.SectionListAdapter;

public class ItemList {

    private SectionList.RecyclerItem mRecyclerItem;
    private List<Item> mChildren;
    private SectionListAdapter mAdapter;

    public ItemList(SectionList.RecyclerItem recyclerItem) {
        mRecyclerItem = recyclerItem;
        mChildren = new ArrayList<>();
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public List<Item> getChildren() {
        return mChildren;
    }

    public SectionListAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(SectionListAdapter adapter) {
        if (Objects.equals(mAdapter, adapter)) {
            return;
        }
        if (mAdapter != null && adapter == null) {
            mAdapter.setDatas(null);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.setRecyclerDataItem(mRecyclerItem);
            List<Item> items = new ArrayList<>();
            for (Item child : mChildren) {
                items.add(child);
                if (child instanceof ItemGroup) {
                    ItemHeader headerItem = ((ItemGroup) child).getHeaderItem();
                    if (headerItem != null) {
                        items.add(headerItem);
                    }
                    items.addAll(((ItemGroup) child).getExpandChildren());
                }
            }
            mAdapter.setDatas(items);
        }
    }

    public void addChild(int index, SectionItem.RecyclerItem recyclerDataItem) {
        if (index < 0 || index > getChildCount()) {
            return;
        }

        Item item = recyclerDataItem.getItem();
        item.setRoot(this);
        mChildren.add(index, item);
        if (mAdapter == null) {
            return;
        }
        mAdapter.post(
                () -> {
                    int index1 = mChildren.indexOf(item);
                    int insertPos = 0;
                    if (index1 > 0) {
                        int previousItemPosition =
                                findPreviousItemPosition(mAdapter, recyclerDataItem.getItem());
                        if (previousItemPosition < 0) {
                            return;
                        }
                        insertPos = previousItemPosition + 1;
                    }
                    mAdapter.addItem(insertPos, item);
                });
    }

    private int findPreviousItemPosition(SectionListAdapter adapter, Item item) {
        int index = mChildren.indexOf(item);
        if (index == 0) {
            return -1;
        }
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
        return adapter.getItemPosition(previousItem);
    }

    public void removeChild(SectionItem.RecyclerItem recyclerDataItem) {
        mChildren.remove(recyclerDataItem.getItem());
        recyclerDataItem.getItem().setRoot(null);
        if (mAdapter == null) {
            return;
        }
        Item item = recyclerDataItem.getItem();
        if (!item.isExpand()) {
            // 没有展开，只需要删除自己即可
            mAdapter.removeItem(item);
            if (item instanceof ItemGroup) {
                ItemHeader headerItem = ((ItemGroup) item).getHeaderItem();
                if (headerItem != null) {
                    mAdapter.removeItem(headerItem);
                }
            }
            return;
        }

        if (item instanceof ItemGroup) {
            List<Item> expandChildren = ((ItemGroup) item).getExpandChildren();
            Item headerItem = ((ItemGroup) item).getHeaderItem();
            if (headerItem != null) {
                expandChildren.add(0, headerItem);
            }
            expandChildren.add(0, item);
            mAdapter.removeItems(expandChildren);
        } else {
            mAdapter.removeItem(item);
        }
    }
}
