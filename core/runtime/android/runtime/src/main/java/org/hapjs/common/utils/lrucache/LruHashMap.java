/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils.lrucache;

import java.util.LinkedHashMap;

public class LruHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int mCapacity;

    public LruHashMap(int capacity) {
        super(capacity, 0.75f, true);
        mCapacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Entry entry) {
        return size() > mCapacity;
    }
}
