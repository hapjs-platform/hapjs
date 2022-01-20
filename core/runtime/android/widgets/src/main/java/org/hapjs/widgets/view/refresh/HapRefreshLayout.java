/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.content.Context;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;

public class HapRefreshLayout extends RefreshLayout implements ComponentHost {

    private Component mComponent;

    public HapRefreshLayout(Context context) {
        super(context);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }
}
