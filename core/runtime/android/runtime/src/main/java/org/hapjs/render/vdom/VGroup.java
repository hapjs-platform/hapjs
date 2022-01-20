/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.vdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hapjs.component.Component;
import org.hapjs.component.ComponentDataHolder;
import org.hapjs.component.Container;
import org.hapjs.component.Recycler;
import org.hapjs.component.RecyclerDataItem;

public class VGroup extends VElement {

    private static final String TAG = "VGroup";
    private List<VElement> mChildren = new ArrayList<>();

    public VGroup(
            VDocument doc,
            int id,
            String tagName,
            Recycler recycler,
            Container.RecyclerItem recyclerItem) {
        super(doc, id, tagName, recyclerItem);

        recyclerItem.setChildrenHolder(Collections.unmodifiableList(getChildren()));
        recyclerItem.bindComponent((Component) recycler);
    }

    public VGroup(VDocument doc, int id, String tagName, ComponentDataHolder dataHolder) {
        super(doc, id, tagName, dataHolder);

        if (dataHolder instanceof Container.RecyclerItem) {
            ((Container.RecyclerItem) dataHolder)
                    .setChildrenHolder(Collections.unmodifiableList(getChildren()));
        }
    }

    public List<VElement> getChildren() {
        return mChildren;
    }

    public void addChild(VElement ele) {
        addChild(ele, mChildren.size());
    }

    public void addChild(VElement ele, int index) {
        if (index < 0 || index >= mChildren.size()) {
            mChildren.add(ele);
        } else {
            mChildren.add(index, ele);
        }

        ele.mParent = this;

        onChildEleAdded(ele, index);
        mDoc.onAddElement(ele);
    }

    private void onChildEleAdded(VElement ele, int index) {
        if (mDataHolder instanceof Container) {
            ((Container) mDataHolder).addChild(ele.getComponent(), index);
        } else {
            ((Container.RecyclerItem) mDataHolder)
                    .onChildAdded((RecyclerDataItem) ele.mDataHolder, getChildren().indexOf(ele));
        }
    }

    private void onChildEleRemoved(VElement ele, int index) {
        if (mDataHolder instanceof Container) {
            ((Container) mDataHolder).removeChild(ele.getComponent());
        } else {
            ((Container.RecyclerItem) mDataHolder)
                    .onChildRemoved((RecyclerDataItem) ele.mDataHolder, index);
        }
    }

    public void removeChild(VElement ele) {
        int index = mChildren.indexOf(ele);
        mChildren.remove(ele);
        ele.mParent = null;

        onChildEleRemoved(ele, index);
        mDoc.onDeleteElement(ele);
    }
}
