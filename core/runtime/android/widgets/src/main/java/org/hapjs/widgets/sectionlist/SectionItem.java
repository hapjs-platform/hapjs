/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.sectionlist.model.Item;

@WidgetAnnotation(
        name = SectionItem.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class SectionItem extends Container<PercentFlexboxLayout> {

    public static final String WIDGET_NAME = "section-item";

    public SectionItem(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected PercentFlexboxLayout createViewImpl() {
        PercentFlexboxLayout percentFlexboxLayout = new PercentFlexboxLayout(mContext);
        percentFlexboxLayout.setComponent(this);
        mNode = percentFlexboxLayout.getYogaNode();
        return percentFlexboxLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        if (Attributes.Style.POSITION.equals(key)) {
            // unsupport position
            return true;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    public void addChild(Component child, int index) {
        if (child instanceof SectionList
                || child instanceof SectionHeader
                || child instanceof SectionGroup) {
            return;
        }
        super.addChild(child, index);
    }

    public static class RecyclerItem extends Container.RecyclerItem {

        private Item mItem;
        private int mViewType = getComponentClass().hashCode();

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
            mItem = createItem();
        }

        @NonNull
        protected Item createItem() {
            return new Item(this);
        }

        @NonNull
        public Item getItem() {
            return mItem;
        }

        @Override
        protected void requestBindTemplate() {
            if (getAttachedTemplate() != null) {
                // dom变化后template需要重新绑定新的
                dispatchDetachFromTemplate();
            }

            if (getBoundComponent() != null) {
                dispatchUnbindComponent();
            }

            // dom变化，重新计算新的viewType
            updateViewType();
        }

        public int getViewType() {
            return mViewType;
        }

        protected void updateViewType() {
            int oldType = mViewType;
            mViewType = calculateViewType();
            if (oldType == mViewType) {
                return;
            }
            mItem.onItemChanged();
        }

        protected int calculateViewType() {
            return identity();
        }
    }
}
