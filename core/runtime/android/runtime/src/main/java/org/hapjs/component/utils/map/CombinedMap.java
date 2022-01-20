/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.utils.map;

import androidx.annotation.NonNull;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 一个 CombinedMap 包含一个内部数据 {@link #mInnerMap} 和一个模板数据 {@link #mSharedMap}. 多个 CombinedMap 共用同一个模板
 * mSharedMap.
 *
 * <p>获取 value 的时候, 会先从 mInnerMap 获取, 如果 mInnerMap 内没有, 再从 mSharedMap 获取.
 *
 * <p>SharedSameMap, CombinedDiffMap 表示此 mInnerMap 和模板 mSharedMap 相同/不同部分的数据.
 */
public class CombinedMap<K, V> extends AbstractMap<K, V> {
    private Set<Entry<K, V>> entrySet;

    private int mRef;
    private Map<K, V> mInnerMap;
    private SharedMap<K, V> mSharedMap;

    public CombinedMap() {
        mInnerMap = new LinkedHashMap<>();
    }

    public CombinedMap(Map<K, V> map) {
        mInnerMap = map;
    }

    /**
     * 设置该 CombinedMap 对象的唯一 ID
     *
     * @param id 一般为 CombinedMap 对象对应的 RecyclerDataItem 对象的 mRef; 也可为自定义的唯一 ID
     */
    public void setId(int id) {
        mRef = id;
    }

    /**
     * 设置 mSharedMap, 多个 CombinedMap 可以设置同一个 mSharedMap
     */
    public void setSharedMap(SharedMap<K, V> sharedMap) {
        if (mSharedMap != null) {
            removeSharedMap();
        }

        initOrCompareSharedMap(sharedMap);
        mSharedMap = sharedMap;
    }

    /**
     * 移除 sharedMap
     */
    public void removeSharedMap() {
        if (mSharedMap == null) {
            return;
        }

        for (Entry<? extends K, ? extends V> entry : getSameMap().entrySet()) {
            mInnerMap.put(entry.getKey(), entry.getValue());
        }
        mSharedMap = null;
    }

    private void init(SharedMap<K, V> sharedMap) {
        for (Map.Entry<? extends K, ? extends V> entry : mInnerMap.entrySet()) {
            sharedMap.put(entry.getKey(), entry.getValue(), mRef, true);
        }
        mInnerMap.clear();
        sharedMap.setIsInit(true);
    }

    /**
     * 初始化或对照模板 {@link SharedMap} 如果属性 value 和 创建的模板 SharedMap 中的属性 value 相同，则可将此 value 从此 CombinedMap
     * 中移除 如果此 {@link CombinedMap#mInnerMap} 不包含某些 key，则给 mInnerMap 设置一个默认值，标记为不同 如果模板 {@link
     * SharedMap} 不包含某些 key，则给 sharedMap 设置一个默认值，标记为不同
     *
     * @param sharedMap 被初始化或被对照的模板对象
     */
    private void initOrCompareSharedMap(SharedMap<K, V> sharedMap) {
        if (!sharedMap.isInit()) {
            init(sharedMap);
            return;
        }

        for (K key : mInnerMap.keySet()) {
            if (!sharedMap.containsKey(key)) {
                // 模板不含这个属性 key, 添加该属性值 value 到 sharedMap, 模板增加了默认属性值
                sharedMap.put(key, sharedMap.getDefaultValue(), mRef, false);
            }
        }

        for (K key : sharedMap.keySet()) {
            SharedMap.SharedValue<V> sharedValue = sharedMap.getSharedValue(key);

            if (!mInnerMap.containsKey(key)) {
                // item 不含这个属性 key, 向 item 添加一个对应的默认值
                mInnerMap.put(key, sharedMap.getDefaultValue());
                // 标记模板 sharedMap 中这个 key 对应值不相同
                sharedValue.setSame(mRef, false);
                continue;
            }

            if (Objects.equals(sharedValue.get(), mInnerMap.get(key))) {
                // 确保 isSame 被设为 false 之后不会逆转，永久为 false
                if (sharedValue.isSame(mRef)) {
                    // 模板和 item 这个属性 key 对应值相同, 可以从 item 中移除
                    mInnerMap.remove(key);
                }
            } else {
                sharedValue.setSame(mRef, false);
            }
        }
    }

    /**
     * 获得和其他 CombinedMap 相同部分的数据
     */
    public Map<K, V> getSameMap() {
        if (mSharedMap == null) {
            return mInnerMap;
        }
        return new SharedSameMap<>(mSharedMap, mRef);
    }

    /**
     * 获得和其他 CombinedMap 不同部分的数据
     */
    public Map<K, V> getDiffMap() {
        if (mSharedMap == null) {
            return mInnerMap;
        }
        return new CombinedDiffMap<>(this, mSharedMap);
    }

    /**
     * 先从 mInnerMap 获取, 如果 mInnerMap 内没有, 再从 mSharedMap 获取.
     */
    @Override
    public V get(Object key) {
        if (mSharedMap == null) {
            return mInnerMap.get(key);
        }

        if (mInnerMap.containsKey(key)) { // TODO: optimize, can we remove containsKey check
            return mInnerMap.get(key);
        }

        return mSharedMap.get(key);
    }

    V getFromInnerMap(Object key, V fullback) {
        if (mInnerMap.containsKey(key)) {
            return mInnerMap.get(key);
        }

        return fullback;
    }

    /**
     * 动态修改数据, 会同步到 mSharedMap
     */
    @Override
    public V put(K k, V v) {
        if (mSharedMap == null) {
            return mInnerMap.put(k, v);
        }

        V oldValue = get(k);
        internalPut(k, v);
        return oldValue;
    }

    /**
     * 动态修改数据, 会同步到 mSharedMap
     */
    @Override
    public V remove(Object key) {
        if (mSharedMap == null) {
            return mInnerMap.remove(key);
        }

        V oldValue = get(key);
        internalRemove(key);
        return oldValue;
    }

    /**
     * 动态修改一个 CombinedMap 的值，将键值对添加到模板 mSharedMap 中
     */
    private void internalPut(K key, V value) {
        SharedMap.SharedValue<V> sharedValue = mSharedMap.getSharedValue(key);
        if (sharedValue == null) {
            mInnerMap.put(key, value);
            mSharedMap.put(key, mSharedMap.getDefaultValue(), mRef, false);
            return;
        }

        if (Objects.equals(sharedValue.get(), value)) {
            // 确保 isSame 被设为 false 之后不会逆转，永久为 false
            if (sharedValue.isSame(mRef)) {
                mInnerMap.remove(key);
            } else {
                mInnerMap.put(key, value);
            }
            return;
        }

        sharedValue.setSame(mRef, false);
        mInnerMap.put(key, value);
    }

    /**
     * 动态删除一个 CombinedMap 的值, 删除就等于设置一个默认值
     */
    private void internalRemove(Object key) {
        internalPut((K) key, mSharedMap.getDefaultValue());
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        if (mSharedMap == null) {
            return mInnerMap.entrySet();
        }

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
            return mSharedMap.size();
        }
    }

    class EntryIterator
            implements Iterator<Entry<K, V>>, Entry<K, V> { // like MapCollections#MapIterator
        private Iterator<Map.Entry<K, V>> it = mSharedMap.entrySet().iterator();
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
        public void remove() {
            CombinedMap.this.remove(getKey());
        }

        @Override
        public K getKey() {
            return currSharedEntry.getKey();
        }

        @Override
        public V getValue() {
            return getFromInnerMap(getKey(),
                    currSharedEntry.getValue()); // TODO: optimize time cost
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("setValue");
        }
    }
}
