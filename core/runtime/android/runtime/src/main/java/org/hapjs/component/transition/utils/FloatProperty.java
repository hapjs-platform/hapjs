/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition.utils;

import android.util.Property;
import androidx.annotation.NonNull;

/**
 * {@link android.util.Property} 的一种实现, 特定用于 <code>float</code> 类型字段. 该特定类型子类允许调用带有 原始 <code>float
 * </code> 类型参数的 {@link #setValue(Object, float) setValue()} 方法，避免自动装箱及其他 与 <code>Float</code>
 * 类相关的开销, 有益性能.
 */
public abstract class FloatProperty<T> extends Property<T, Float> {

    public FloatProperty() {
        super(Float.class, null);
    }

    /**
     * {@link #set(Object, Float)} 方法的一种特定类型变种, 当处理 <code>float</code> 类型字段时更快.
     *
     * @param object 带有该属性的对象
     * @param value  属性值
     */
    public abstract void setValue(@NonNull T object, float value);

    @Override
    public final void set(@NonNull T object, Float value) {
        setValue(object, value);
    }

    /**
     * 缺省实现. 一些属性可以没有 getter. 覆盖此方法以实现真实的 getter.
     */
    @Override
    public Float get(@NonNull T object) {
        return 0f;
    }
}
