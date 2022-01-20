/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.utils.map;

import androidx.annotation.NonNull;
import java.util.AbstractMap;
import java.util.Set;

/**
 * CombinedMap 与其他 CombinedMap 共享的数据部分, 只读, 不可修改
 */
public class SharedSameMap<K, V> extends AbstractMap<K, V> {

    private SharedMap<K, V> mSharedMap;
    private int mId;

    SharedSameMap(SharedMap<K, V> sharedMap, int id) {
        mSharedMap = sharedMap;
        mId = id;
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return mSharedMap.sameMapEntrySet(mId);
    }
}
