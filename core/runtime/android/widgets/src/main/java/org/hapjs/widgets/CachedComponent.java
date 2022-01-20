/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import org.hapjs.component.Component;

public class CachedComponent {

    public final Component component;
    public final boolean added;
    public final int index;

    public CachedComponent(Component component, boolean added, int index) {
        this.component = component;
        this.added = added;
        this.index = index;
    }
}
