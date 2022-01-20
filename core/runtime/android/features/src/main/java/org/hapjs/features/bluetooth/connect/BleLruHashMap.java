/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.connect;

import java.util.LinkedHashMap;

public class BleLruHashMap<K, V> extends LinkedHashMap<K, V> {

    private final int mMaxSize;

    public BleLruHashMap(int maxSize, boolean accessOrder) {
        super(5, 0.75f, accessOrder);
        mMaxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry eldest) {
        if (size() > mMaxSize && eldest.getValue() instanceof BleConnector) {
            ((BleConnector) eldest.getValue()).disconnect();
        }
        return size() > mMaxSize;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
