/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import androidx.annotation.NonNull;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OrderedConcurrentHashMap<K, V> implements Iterable<K> {
    protected ConcurrentHashMap<K, V> mEntriesMap = new ConcurrentHashMap<>();
    protected ConcurrentLinkedQueue<K> mEntryKeyQueue = new ConcurrentLinkedQueue<>();

    public void add(@NonNull K key, V value) {
        mEntriesMap.put(key, value);
        if (!mEntryKeyQueue.contains(key)) {
            mEntryKeyQueue.add(key);
        }
    }

    public V get(@NonNull K key) {
        return mEntriesMap.get(key);
    }

    public boolean remove(@NonNull K key) {
        mEntriesMap.remove(key);
        return mEntryKeyQueue.remove(key);
    }

    public void clear() {
        mEntriesMap.clear();
        mEntryKeyQueue.clear();
    }

    @NonNull
    @Override
    public Iterator<K> iterator() {
        return mEntryKeyQueue.iterator();
    }
}
