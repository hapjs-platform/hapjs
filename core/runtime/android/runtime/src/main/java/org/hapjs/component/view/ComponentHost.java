/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import org.hapjs.component.Component;

public interface ComponentHost {
    Component getComponent();

    void setComponent(Component component);
}
