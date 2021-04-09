/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils.lrucache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LruCache<K, V> implements Cache<K, V> {

    private static final int DEFAULT_CAPACITY = 10;

    private final LruHashMap<K, V> mMap;
    // 冷热端分离
    private final Map<K, Integer> mCountMap;

    private final int mMaxMemorySize;

    private int mMemorySize;

    public LruCache() {
        this(DEFAULT_CAPACITY);
    }

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        mMap = new LruHashMap<>(capacity);
        mCountMap = new LinkedHashMap<K, Integer>();
        mMaxMemorySize = capacity;
    }

    @Override
    public final V get(K key) {
        Objects.requireNonNull(key, "key == null");
        synchronized (this) {
            V value = mMap.get(key);
            mCountMap.put(key, mCountMap.get(key) != null ? (mCountMap.get(key) + 1) : 1);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public final V put(K key, V value) {
        Objects.requireNonNull(key, "key == null");
        Objects.requireNonNull(value, "value == null");
        V previous;
        synchronized (this) {
            previous = mMap.put(key, value);
            mMemorySize += getValueSize(value);
            if (previous != null) {
                // 已经存在则size不需要增加
                mMemorySize -= getValueSize(previous);
            }
            mCountMap.put(key, 1);
            trimToSize(mMaxMemorySize);
        }
        return previous;
    }

    @Override
    public final V remove(K key) {
        Objects.requireNonNull(key, "key == null");
        V previous;
        synchronized (this) {
            previous = mMap.remove(key);
            mCountMap.remove(key);
            if (previous != null) {
                mMemorySize -= getValueSize(previous);
                onRemoved(key, previous);
            }
        }
        return previous;
    }

    @Override
    public final synchronized void clear() {
        if (mMap == null || mMap.size() == 0) {
            return;
        }
        Iterator iterator = mMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> toRemove = (Map.Entry<K, V>) iterator.next();
            onRemoved(toRemove.getKey(), toRemove.getValue());
            iterator.remove();
            mCountMap.remove(toRemove.getKey());
            mMemorySize -= getValueSize(toRemove.getValue());
        }
    }

    @Override
    public final synchronized int getMaxMemorySize() {
        return mMaxMemorySize;
    }

    @Override
    public final synchronized int getMemorySize() {
        return mMemorySize;
    }

    /**
     * Returns a copy of the current contents of the cache.
     */
    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap<>(mMap);
    }

    /**
     * Returns the class name.
     *
     * <p>This method should be overridden to debug exactly.
     *
     * @return class name.
     */
    protected String getClassName() {
        return LruCache.class.getName();
    }

    /**
     * Returns the size of the entry.
     *
     * <p>The default implementation returns 1 so that max size is the maximum number of entries.
     *
     * <p><em>Note:</em> This method should be overridden if you control memory size correctly.
     *
     * @param value value
     * @return the size of the entry.
     */
    protected int getValueSize(V value) {
        return 1;
    }

    /**
     * Remove the eldest entries.
     *
     * <p><em>Note:</em> This method has to be called in synchronized block.
     *
     * @param maxSize max size
     */
    private void trimToSize(int maxSize) {
        boolean allCacheInHotSide = true;
        while (true) {
            if (mMemorySize <= maxSize || mMap.isEmpty()) {
                allCacheInHotSide = false;
                break;
            }
            if (mMemorySize < 0 || (mMap.isEmpty() && mMemorySize != 0)) {
                throw new IllegalStateException(
                        getClassName() + ".getValueSize() is reporting inconsistent results");
            }

            Map.Entry<K, V> toRemove = mMap.entrySet().iterator().next();
            if (mCountMap.get(toRemove.getKey()) != null && mCountMap.get(toRemove.getKey()) > 1) {
                mCountMap.put(toRemove.getKey(), 1);
                get(toRemove.getKey());
            } else {
                onRemoved(toRemove.getKey(), toRemove.getValue());
                mMap.remove(toRemove.getKey());
                mCountMap.remove(toRemove.getKey());
                mMemorySize -= getValueSize(toRemove.getValue());
                allCacheInHotSide = false;
            }
        }
        // 如果所有的页面都是成了热页面，则把所有的页面初始化成冷页面后再采用lru淘汰策略
        if (allCacheInHotSide) {
            Iterator<Map.Entry<K, Integer>> iterator = mCountMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, Integer> countMap = iterator.next();
                mCountMap.put(countMap.getKey(), 1);
            }
            trimToSize(maxSize);
        }
    }

    public void onRemoved(K key, V value) {
    }

    @Override
    public final synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : mMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(",");
        }
        sb.append("maxMemory=")
                .append(mMaxMemorySize)
                .append(",")
                .append("memorySize=")
                .append(mMemorySize);
        return sb.toString();
    }
}
