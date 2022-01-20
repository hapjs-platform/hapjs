/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.utils.map;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * SharedMap 保存着相关 CombinedMap 的所有 key 使用 {@link SharedValue#isSame} 获得 value 是否和单个 CombinedMap
 * 相同/不同
 */
public class SharedMap<K, V> {

    private Map<K, SharedValue<V>> mSharedMap = new LinkedHashMap<>();
    private boolean mIsInit = false;
    private V mDefaultValue;

    public SharedMap(V defaultValue) {
        mDefaultValue = defaultValue;
    }

    int size() {
        return mSharedMap.size();
    }

    V get(Object key) {
        SharedValue<V> sharedValue = mSharedMap.get(key);
        if (sharedValue == null) {
            return null;
        }
        return sharedValue.get();
    }

    Set<K> keySet() {
        return mSharedMap.keySet();
    }

    boolean containsKey(K key) {
        return mSharedMap.containsKey(key);
    }

    SharedValue<V> getSharedValue(K key) {
        return mSharedMap.get(key);
    }

    void put(K key, V value, int id, boolean isSame) {
        mSharedMap.put(key, new SharedValue<>(value, id, isSame));
    }

    void setIsInit(boolean isInit) {
        mIsInit = isInit;
    }

    boolean isInit() {
        return mIsInit;
    }

    V getDefaultValue() {
        return mDefaultValue;
    }

    Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    Set<Map.Entry<K, V>> sameMapEntrySet(int id) {
        return new SameEntrySet(id);
    }

    Set<Map.Entry<K, V>> diffMapEntrySet() {
        return new DiffEntrySet();
    }

    static class SharedValue<V> {
        V value;
        Set<Integer> diffSet = new ArraySet<>();

        SharedValue(V value, int id, boolean isSame) {
            this.value = value;
            setSame(id, isSame);
        }

        V get() {
            return value;
        }

        boolean isSame(int id) {
            return !diffSet.contains(id);
        }

        boolean isAllSame() {
            return diffSet.isEmpty();
        }

        void setSame(int id, boolean isSame) {
            if (!isSame) {
                this.diffSet.add(id);
            } else {
                this.diffSet.remove(id);
            }
        }
    }

    final class SameEntrySet extends EntrySet {
        int id;

        SameEntrySet(int id) {
            this.id = id;
        }

        @Override
        boolean isValid(Map.Entry<K, SharedValue<V>> entry) {
            return entry.getValue().isSame(id);
        }
    }

    final class DiffEntrySet extends EntrySet {

        @Override
        boolean isValid(Map.Entry<K, SharedValue<V>> entry) {
            return !entry.getValue().isAllSame();
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @NonNull
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator() {
                @Override
                boolean isValid(Map.Entry<K, SharedValue<V>> entry) {
                    return EntrySet.this.isValid(entry);
                }
            };
        }

        @Override
        public int size() { // TODO: optimize
            int size = 0;
            for (Map.Entry<K, SharedValue<V>> entry : mSharedMap.entrySet()) {
                if (isValid(entry)) {
                    size++;
                }
            }
            return size;
        }

        boolean isValid(Map.Entry<K, SharedValue<V>> entry) {
            return true;
        }
    }

    abstract class EntryIterator implements Iterator<Map.Entry<K, V>>, Map.Entry<K, V> {
        private Iterator<Map.Entry<K, SharedValue<V>>> it = mSharedMap.entrySet().iterator();
        private Map.Entry<K, SharedValue<V>> currEntry = null;
        private Map.Entry<K, SharedValue<V>> validNext = null;

        @Override
        public boolean hasNext() {
            return hasValidNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasValidNext()) {
                throw new IndexOutOfBoundsException();
            }

            currEntry = validNext;
            validNext = null;
            return this;
        }

        private boolean hasValidNext() {
            if (validNext != null) {
                return true;
            }

            while (it.hasNext()) {
                Map.Entry<K, SharedValue<V>> next = it.next();
                if (isValid(next)) {
                    this.validNext = next;
                    return true;
                }
            }

            return false;
        }

        abstract boolean isValid(Map.Entry<K, SharedValue<V>> entry);

        @Override
        public K getKey() {
            return currEntry.getKey();
        }

        @Override
        public V getValue() {
            return currEntry.getValue().get();
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
