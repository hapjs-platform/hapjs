/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.tv.list;

import android.content.Context;
import android.text.TextUtils;

import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.GridViewAdapterImpl;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.bridge.annotation.InheritedAnnotation;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.list.FlexLayoutManager;
import org.hapjs.widgets.view.list.RecyclerViewAdapter;

import java.util.HashMap;
import java.util.Map;

@InheritedAnnotation
public class List extends org.hapjs.widgets.list.List {

    private ItemSelectedListener mSelectedListener;

    public List(HapEngine hapEngine, Context context, Container parent, int elId, RenderEventCallback callback,
                Map<String, Object> savedState) {
        super(hapEngine, context, parent, elId, callback, savedState);
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.FOCUS_BEHAVIOR:
                String behavior = Attributes.getString(attribute);
                int focusBehavior = GridViewAdapterImpl.FOCUS_SCROLL_ALIGNED;
                if (Attributes.FocusBehavior.EDGED.equals(behavior)) {
                    focusBehavior = GridViewAdapterImpl.FOCUS_SCROLL_ITEM;
                }
                ((GridViewAdapterImpl) mHost).setFocusScrollStrategy(focusBehavior);
                return true;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.SELECTED.equals(event)) {
            mSelectedListener = new ItemSelectedListener() {
                @Override
                public void onSelected(int position) {
                    Map<String, Object> params = new HashMap<>(1);
                    params.put("position", position);
                    mCallback.onJsEventCallback(getPageId(), mRef, Attributes.Event.SELECTED, List.this, params, null);
                }
            };
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (Attributes.Event.SELECTED.equals(event)) {
            mSelectedListener = null;
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    protected RecyclerViewAdapter createRecyclerViewInner() {
        BaseGridView baseGridView = new GridViewAdapterImpl(mContext);
        baseGridView.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                super.onChildViewHolderSelected(parent, child, position, subposition);
                if (mSelectedListener != null) {
                    mSelectedListener.onSelected(position);
                }
            }
        });
        return (RecyclerViewAdapter) baseGridView;
    }

    @Override
    protected FlexLayoutManager createLayoutManagerInner() {
        return ((GridViewAdapterImpl) mRecyclerViewImpl.getActualRecyclerView()).getGridLayoutManagerAdapter();
    }

    private interface ItemSelectedListener {
        void onSelected(int position);
    }

}
