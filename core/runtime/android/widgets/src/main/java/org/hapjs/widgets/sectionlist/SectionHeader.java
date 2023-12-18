/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
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
import org.hapjs.widgets.sectionlist.model.ItemGroup;
import org.hapjs.widgets.sectionlist.model.ItemHeader;

@WidgetAnnotation(
        name = SectionHeader.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class SectionHeader extends Container<PercentFlexboxLayout> {

    public static final String WIDGET_NAME = "section-header";
    private RecyclerItem mRecyclerItem;

    public SectionHeader(
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
        GestureDetector gestureDetector =
                new GestureDetector(
                        mContext,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDown(MotionEvent e) {
                                return mRecyclerItem != null;
                            }

                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                if (mRecyclerItem == null) {
                                    return false;
                                }
                                ItemGroup parent = mRecyclerItem.getItem().getParent();
                                if (parent != null) {
                                    parent.setExpand(!parent.isExpand());
                                }

                                return true;
                            }
                        });
        PercentFlexboxLayout percentFlexboxLayout =
                new PercentFlexboxLayout(mContext) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        // click事件可能被child消费
                        boolean consumed = gestureDetector.onTouchEvent(ev);
                        return super.dispatchTouchEvent(ev) || consumed;
                    }
                };
        percentFlexboxLayout.setComponent(this);
        mNode = percentFlexboxLayout.getYogaNode();
        return percentFlexboxLayout;
    }

    private void bindRecyclerDataItem(RecyclerItem recyclerItem) {
        mRecyclerItem = recyclerItem;
        freezeEvent(Attributes.Event.TOUCH_CLICK);
    }

    public static final class RecyclerItem extends SectionItem.RecyclerItem {

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @NonNull
        @Override
        protected Item createItem() {
            return new ItemHeader(this);
        }

        @Override
        protected void onApplyDataToComponent(Component recycle) {
            super.onApplyDataToComponent(recycle);
            SectionHeader sectionHeader = (SectionHeader) recycle;
            sectionHeader.bindRecyclerDataItem(this);
        }

        @Override
        public void unbindComponent() {
            SectionHeader sectionHeader = getBoundComponent();
            if (sectionHeader != null) {
                sectionHeader.bindRecyclerDataItem(null);
            }
            super.unbindComponent();
        }
    }
}
