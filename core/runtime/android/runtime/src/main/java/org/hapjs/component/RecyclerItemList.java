/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import androidx.annotation.NonNull;
import java.util.Iterator;
import java.util.List;

public class RecyclerItemList implements Iterable<RecyclerDataItem> {
    private List<? extends RecyclerDataItem.Holder> mHolders;

    void setRecyclerItemHolders(List<? extends RecyclerDataItem.Holder> holders) {
        mHolders = holders;
    }

    public int size() {
        return mHolders == null ? 0 : mHolders.size();
    }

    public RecyclerDataItem get(int index) {
        return mHolders.get(index).getRecyclerItem();
    }

    public int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            if (get(i).equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public Iterator<RecyclerDataItem> iterator() {
        return new MyIterator();
    }

    private class MyIterator implements Iterator<RecyclerDataItem> {
        private int mIndex = 0;

        @Override
        public boolean hasNext() {
            return mIndex < size();
        }

        @Override
        public RecyclerDataItem next() {
            return get(mIndex++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
