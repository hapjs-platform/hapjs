/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.hackres;

import android.content.res.Resources;
import android.content.res.ResourcesImpl;

public class HackResources extends Resources {
    public HackResources(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public void setImpl(ResourcesImpl impl) {
    }
}
