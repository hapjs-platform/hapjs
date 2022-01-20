/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

public interface HandlerObject {
    /**
     * java传递js时会把当前对象替换为该方法返回的v8对象
     *
     * @return
     */
    V8Object toV8Object(V8 v8);
}
