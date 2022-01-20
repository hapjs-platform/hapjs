/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.value;

import android.util.ArrayMap;
import java.util.Map;

class CSSValueMap implements CSSValues {

    private Map<String, Object> mMap = new ArrayMap<>(1); // 通常只有一个 State.Normal

    void put(String state, Object value) {
        mMap.put(state, value);
    }

    public Object get(String state) {
        return mMap.get(state);
    }

    @Override
    public boolean equals(Object obj) { // RecyclerTemplate 需要对比样式数据
        if (obj instanceof CSSValueMap) {
            return mMap.equals(((CSSValueMap) obj).mMap);
        }
        return false;
    }
}
