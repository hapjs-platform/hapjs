/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.list;

import android.content.Context;
import android.view.ViewGroup;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.component.view.state.State;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.list.FlexLayoutManager;

@WidgetAnnotation(
        name = ListItem.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT
        })
public class ListItem extends Container<PercentFlexboxLayout> {

    protected static final String WIDGET_NAME = "list-item";

    public ListItem(
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
        PercentFlexboxLayout flexboxLayout = new PercentFlexboxLayout(mContext);
        flexboxLayout.setComponent(this);
        mNode = flexboxLayout.getYogaNode();

        ViewGroup.LayoutParams lp = generateDefaultLayoutParams();
        if (!(mParent instanceof List)) {
            throw new IllegalArgumentException("list-item must be added in list");
        }
        if (((List) mParent).isHorizontal()) {
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            setHeightDefined(true);
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            setWidthDefined(true);
        }
        flexboxLayout.setLayoutParams(lp);

        return flexboxLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key){
            case Attributes.EventDispatch.DISALLOW_INTERCEPT:
                boolean isDisallow = Attributes.getBoolean(attribute, false);
                disallowIntercept(isDisallow);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    private void disallowIntercept(boolean disallow) {
        if (mHost != null) {
            mHost.setDisallowIntercept(disallow);
        }
    }

    public static class RecyclerItem extends Container.RecyclerItem {
        private int mAttrType = -1;

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        public void bindAttrs(Map attrs) {
            super.bindAttrs(attrs);

            Object type = getAttrsDomData().get(Attributes.Style.TYPE);
            if (type != null) {
                int attrType = type.toString().trim().hashCode();
                if (attrType != mAttrType) {
                    mAttrType = attrType;
                    notifyTypeChanged();
                }
            }
        }

        int getViewType() {
            // list recyclerItem in list recyclerItem, share same list component
            // 所以共用了 RecyclerAdapter, 共用了 ViewHolder 缓存池
            // 这里对 type 处理, 来使不同的 list 内的 list-item 不要复用组件, 从而避免错误
            return mAttrType + getParent().hashCode();
        }

        int getColumnSpan() {
            if (getBoundComponent() != null) {
                return getBoundComponent()
                        .getCurStateStyleInt(
                                Attributes.Style.COLUMN_SPAN,
                                FlexLayoutManager.DEFAULT_COLUMN_COUNT);
            }

            CSSValues style = getStyleAttribute(Attributes.Style.COLUMN_SPAN);
            if (style == null) {
                return FlexLayoutManager.DEFAULT_COLUMN_COUNT;
            }
            return Attributes.getInt(
                    null, style.get(State.NORMAL), FlexLayoutManager.DEFAULT_COLUMN_COUNT);
        }

        private void notifyTypeChanged() {
            notifyItemChanged();
        }

        private void notifyItemChanged() {
            if (getAttachedTemplate() != null) {
                dispatchDetachFromTemplate();
            }
            if (getBoundComponent() != null) {
                dispatchUnbindComponent();
            }
            if (getParent() != null) {
                ((List.RecyclerItem) getParent()).notifyItemChanged(this);
            }
        }

        @Override
        protected void requestBindTemplate() {
            notifyItemChanged();
        }
    }
}
