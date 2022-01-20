/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

public class HintSpinnerAdapter implements SpinnerAdapter, ListAdapter {

    protected static final int PIVOT = 1;

    protected SpinnerAdapter mAdapter;

    protected Context mContext;

    protected int mHintLayout;

    protected int mHintDropdownLayout;

    protected LayoutInflater mLayoutInflater;

    public HintSpinnerAdapter(
            SpinnerAdapter spinnerAdapter,
            int hintLayout, Context context) {

        this(spinnerAdapter, hintLayout, -1, context);
    }

    public HintSpinnerAdapter(SpinnerAdapter spinnerAdapter,
                              int hintLayout, int hintDropdownLayout, Context context) {
        this.mAdapter = spinnerAdapter;
        this.mContext = context;
        this.mHintLayout = hintLayout;
        this.mHintDropdownLayout = hintDropdownLayout;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            return getHintView(parent);
        }
        return mAdapter.getView(position - PIVOT, null, parent);
    }

    protected View getHintView(ViewGroup parent) {
        return mLayoutInflater.inflate(mHintLayout, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Android BUG! http://code.google.com/p/android/issues/detail?id=17128 -
        // Spinner does not support multiple view types
        if (position == 0) {
            return mHintDropdownLayout == -1
                    ? new View(mContext) :
                    getHintDropdownView(parent);
        }

        return mAdapter.getDropDownView(position - PIVOT, null, parent);
    }

    protected View getHintDropdownView(ViewGroup parent) {
        return mLayoutInflater.inflate(mHintDropdownLayout, parent, false);
    }

    @Override
    public int getCount() {
        int count = mAdapter.getCount();
        return count == 0 ? 0 : count + PIVOT;
    }

    @Override
    public Object getItem(int position) {
        return position == 0 ? null : mAdapter.getItem(position - PIVOT);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position >= PIVOT ? mAdapter.getItemId(position - PIVOT) : position - PIVOT;
    }

    @Override
    public boolean hasStableIds() {
        return mAdapter.hasStableIds();
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != 0; // Don't allow the 'hint' item to be picked.
    }

}
