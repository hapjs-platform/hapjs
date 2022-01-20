/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition.utils;

import android.os.Build;
import android.util.Property;
import androidx.annotation.NonNull;

public abstract class IntProperty<T> extends Property<T, Integer> {

    public IntProperty() {
        super(Integer.class, null);
    }

    /**
     * 赋值给目标对象.
     *
     * @param object 带有该属性的对象
     * @param value  所赋值
     */
    public abstract void setValue(@NonNull T object, int value);

    @Override
    public final void set(@NonNull T object, @NonNull Integer value) {
        setValue(object, value);
    }

    /**
     * 缺省实现. 一些属性可以没有 getter. 覆盖此方法以实现真实的 getter.
     */
    @Override
    @NonNull
    public Integer get(T object) {
        return 0;
    }

    @NonNull
    public Property<T, Integer> optimize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return new android.util.IntProperty<T>(null) {
                @Override
                public void setValue(@NonNull T object, int value) {
                    IntProperty.this.setValue(object, value);
                }

                @Override
                @NonNull
                public Integer get(@NonNull T object) {
                    return IntProperty.this.get(object);
                }
            };
        } else {
            return this;
        }
    }
}
