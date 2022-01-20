/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import java.util.HashMap;
import java.util.Map;

public class FloatingHelper {
    private Map<String, Floating> mFloatingMap;

    public FloatingHelper() {
        mFloatingMap = new HashMap<>();
    }

    public void put(String id, Floating floating) {
        mFloatingMap.put(id, floating);
    }

    public Floating get(String id) {
        return mFloatingMap.get(id);
    }

    public Floating getFloating(Component component) {
        if (component instanceof Floating) {
            return (Floating) component;
        }
        Component parent = component.getParent();
        while (parent != null) {
            if (parent instanceof Floating) {
                return (Floating) parent;
            }
            parent = parent.getParent();
        }

        return null;
    }
}
