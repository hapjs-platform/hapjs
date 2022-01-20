/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils.lrucache;

public interface Cache<K, V> {
    V get(K key);

    V put(K key, V value);

    V remove(K key);

    void clear();

    int getMaxMemorySize();

    int getMemorySize();
}
