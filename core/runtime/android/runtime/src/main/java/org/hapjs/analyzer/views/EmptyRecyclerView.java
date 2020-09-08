/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class EmptyRecyclerView extends SlideMonitoredRecyclerView {

    private OnDataSizeChangedCallback mDataSizeChangedCallback;

    public EmptyRecyclerView(@NonNull Context context) {
        super(context);
    }

    public EmptyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mDataObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mDataObserver);
        }
        checkIfEmpty();
    }


    private AdapterDataObserver mDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };

    private void checkIfEmpty() {
        if (getAdapter() != null) {
            int itemCount = getAdapter().getItemCount();
            boolean emptyViewVisible = itemCount == 0;
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
            if (mDataSizeChangedCallback != null) {
                mDataSizeChangedCallback.onDataSize(itemCount);
            }
        }
    }

    public void setDataSizeChangedCallback(OnDataSizeChangedCallback dataSizeChangedCallback) {
        mDataSizeChangedCallback = dataSizeChangedCallback;
        checkIfEmpty();
    }

    public interface OnDataSizeChangedCallback {
        void onDataSize(int size);
    }
}
