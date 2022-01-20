/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.utils.map;

import androidx.annotation.NonNull;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * CombinedMap 与其他 CombinedMap 不同的部分, 只读, 不可修改
 */
public class CombinedDiffMap<K, V> extends AbstractMap<K, V> {
    private Set<Entry<K, V>> entrySet;

    private SharedMap<K, V> mSharedMap;
    private CombinedMap<K, V> mCombinedMap;

    CombinedDiffMap(CombinedMap<K, V> map, SharedMap<K, V> sharedMap) {
        mCombinedMap = map;
        mSharedMap = sharedMap;
    }

    @Override
    public V get(Object o) {
        return mCombinedMap.get(o);
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return mSharedMap.diffMapEntrySet().size();
        }
    }

    class EntryIterator implements Iterator<Entry<K, V>>, Entry<K, V> {
        private Iterator<Entry<K, V>> it = mSharedMap.diffMapEntrySet().iterator();
        private Map.Entry<K, V> currSharedEntry;

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            currSharedEntry = it.next();
            return this;
        }

        @Override
        public K getKey() {
            return currSharedEntry.getKey();
        }

        @Override
        public V getValue() {
            return mCombinedMap.getFromInnerMap(
                    getKey(), currSharedEntry.getValue()); // TODO: optimize time cost
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("setValue");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
