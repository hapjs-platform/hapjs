/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

public class InstanceV8Object extends V8Object {
    private int id;

    public InstanceV8Object(V8 v8, int id) {
        super(v8, null);
        this.id = id;
    }

    @Override
    public void release() {
        // 调用对象release方法
        InstanceManager.IInstance instance = InstanceManager.getInstance().getInstance(id);
        if (null != instance) {
            instance.release();
        }
        // 容器删除java对象
        InstanceManager.getInstance().removeInstance(id);
        super.release();
    }
}
