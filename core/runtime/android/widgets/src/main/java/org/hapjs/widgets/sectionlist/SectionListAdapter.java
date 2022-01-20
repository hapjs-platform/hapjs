/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.widgets.sectionlist.model.Item;
import org.hapjs.widgets.sectionlist.model.ItemGroup;

public class SectionListAdapter extends BaseRecyclerViewAdapter<Item, SectionListAdapter.Holder> {
    private static final String TAG = "SectionListAdapter";
    private SectionList mSectionList;
    private int mCreateViewPosition = 0;
    private SectionList.RecyclerItem mRecyclerItem;

    public SectionListAdapter(SectionList sectionList) {
        mSectionList = sectionList;
    }

    public void setRecyclerDataItem(SectionList.RecyclerItem recyclerDataItem) {
        mRecyclerItem = recyclerDataItem;
    }

    public void scrollTo(int position, boolean smooth) {
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
            return;
        }

        if (smooth) {
            recyclerView.smoothScrollToPosition(position);
        } else {
            recyclerView.scrollToPosition(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        mCreateViewPosition = position;
        Item item = getItem(position);
        return item.getViewType();
    }

    @NonNull
    @Override
    protected Holder createHolder(@NonNull ViewGroup parent, int viewType) {
        Item item = getItem(mCreateViewPosition);
        SectionItem.RecyclerItem dataItem = item.getData();
        mRecyclerItem.attachTemplate(dataItem);
        ItemGroup parentNode = item.getParent();
        Component recycle;
        Container sectionGroup = null;
        // 在recycler模式下，通过component去获取component tree不可靠，因为只要滑出recyclerview范围后
        // component会被回收，甚至会对整个tree做渲染优化，最终打乱component的树结构，因此在Recycler情况下，通过
        // RecyclerDataItem来获取tree结构关系是最可靠的。
        if (parentNode != null
                && (sectionGroup = parentNode.getData().getBoundComponent()) != null) {
            // section-group可能滑出去被回收了，则将section-list作为其parent
            recycle = dataItem.createRecycleComponent(sectionGroup);
            sectionGroup.getChildren().add(recycle);
        } else {
            recycle = dataItem.createRecycleComponent(mSectionList);
            mSectionList.getChildren().add(recycle);
        }
        recycle.createView();
        return new Holder(recycle);
    }

    @Override
    protected void bindHolder(@NonNull Holder holder, Item item, int position) {
        SectionItem.RecyclerItem recyclerDataItem = item.getData();
        mRecyclerItem.attachTemplate(recyclerDataItem);
        holder.bind(recyclerDataItem);
        Component component = holder.getComponent();
        mSectionList.lazySetAppearanceWatch(component);
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        super.onViewRecycled(holder);
        Component component = holder.getComponent();
        holder.unbind();
        Container parent = component.getParent();
        parent.getChildren().remove(component);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull Holder holder) {
        super.onViewAttachedToWindow(holder);
        Component component = holder.getComponent();
        component.onHostViewAttached(mSectionList.getHostView());
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull Holder holder) {
        super.onViewDetachedFromWindow(holder);
        mSectionList.processAppearanceEvent();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private Component mComponent;
        private RecyclerDataItem mItem;

        Holder(Component instance) {
            super(instance.getHostView());
            mComponent = instance;
        }

        void bind(RecyclerDataItem item) {
            mItem = item;
            if (item instanceof SectionGroup.RecyclerItem) {
                // section-list对child做了扁平化处理，提高渲染效率
                // section-group在渲染层面将其所有的child全部移除，并渲染为RecyclerView中的每个item-view
                // 这里仅仅只需要让RecyclerItem与SectionGroup关联即可
                item.bindComponent(mComponent);
                return;
            }
            item.dispatchBindComponent(mComponent);
        }

        void unbind() {
            if (mItem instanceof SectionGroup.RecyclerItem
                    && ((SectionGroup.RecyclerItem) mItem).getChildren().size() > 0) {
                mItem.unbindComponent();
                return;
            }
            mItem.dispatchUnbindComponent();
            mItem.destroy();
            mItem = null;
        }

        Component getComponent() {
            return mComponent;
        }
    }
}
