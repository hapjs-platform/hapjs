/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.text;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.helper.StateHelper;

public class FlexTextView extends AppCompatTextView implements ComponentHost {
    private Component mComponent;

    public FlexTextView(Context context) {
        super(context);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
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
