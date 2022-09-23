/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.vdom;

import org.hapjs.component.Component;
import org.hapjs.component.ComponentDataHolder;
import org.hapjs.component.RecyclerDataItem;

public class VElement implements RecyclerDataItem.Holder {

    public static final int ID_DOC = -1;
    public static final int ID_BODY = -2;

    public static final String TAG_BODY = "scroller";

    private static final String TAG = "VElement";

    protected VDocument mDoc;
    protected VGroup mParent;
    protected int mVId;
    protected String mTagName;
    ComponentDataHolder mDataHolder;

    public VElement(VDocument doc, int id, String tagName, ComponentDataHolder componentDelegate) {
        mDoc = doc;
        mVId = id;
        mTagName = tagName;
        mDataHolder = componentDelegate;
    }

    public int getVId() {
        return mVId;
    }

    public String getTagName() {
        return mTagName;
    }

    public VGroup getParent() {
        return mParent;
    }

    public ComponentDataHolder getComponentDataHolder() {
        return mDataHolder;
    }

    public Component getComponent() {
        if (mDataHolder instanceof Component) {
            return (Component) mDataHolder;
        }
        return ((RecyclerDataItem) mDataHolder).getBoundComponent();
    }

    boolean isComponentClassMatch(Class clazz) {
        if (mDataHolder instanceof Component) {
            return mDataHolder.getClass() == clazz;
        }
        return ((RecyclerDataItem) mDataHolder).isComponentClassMatch(clazz);
    }

    public void destroy() {
        if (mDataHolder instanceof Component) {
            ((Component) mDataHolder).destroy();
        } else {
            ((RecyclerDataItem) mDataHolder).destroy();
        }
    }

    @Override
    public RecyclerDataItem getRecyclerItem() {
        return (RecyclerDataItem) mDataHolder;
    }
}
