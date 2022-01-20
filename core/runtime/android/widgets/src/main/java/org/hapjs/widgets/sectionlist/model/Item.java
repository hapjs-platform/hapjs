/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist.model;

import androidx.annotation.Nullable;
import org.hapjs.widgets.sectionlist.SectionItem;
import org.hapjs.widgets.sectionlist.SectionListAdapter;

public class Item {
    private SectionItem.RecyclerItem mData;
    private ItemGroup mParent;
    private ItemList mRoot;

    public Item(SectionItem.RecyclerItem data) {
        mData = data;
    }

    @Nullable
    public SectionListAdapter getAdapter() {
        ItemList root = getRoot();
        if (root != null) {
            return root.getAdapter();
        }
        return null;
    }

    public <T extends SectionItem.RecyclerItem> T getData() {
        return (T) mData;
    }

    public ItemGroup getParent() {
        return mParent;
    }

    public void setParent(ItemGroup parent) {
        mParent = parent;
    }

    public ItemList getRoot() {
        if (mRoot != null) {
            return mRoot;
        }
        if (mParent != null) {
            return mParent.getRoot();
        }
        return null;
    }

    public void setRoot(ItemList root) {
        mRoot = root;
    }

    public int getViewType() {
        return mData.getViewType();
    }

    public boolean isExpand() {
        // 如果有parent，折叠/展开由parent决定
        if (mParent != null) {
            return mParent.isExpand();
        }
        // 如果没有parent，那么该item为第一层级
        ItemList root = getRoot();
        if (root == null) {
            // 没有在层级树中
            return false;
        }
        return true;
    }

    public void onItemChanged() {
        // 如果处于折叠状态，不需要触发刷新
        if (!isExpand()) {
            return;
        }

        // 刷新显示
        SectionListAdapter adapter = getAdapter();
        if (adapter != null) {
            int adapterPosition = adapter.getItemPosition(this);
            if (adapterPosition != -1) {
                adapter.notifyItemChanged(adapterPosition);
            }
        }
    }
}
