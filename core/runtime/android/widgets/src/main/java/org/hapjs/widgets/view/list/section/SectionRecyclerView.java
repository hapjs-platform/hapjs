/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.list.section;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.NestedRecyclerView;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;

public class SectionRecyclerView extends NestedRecyclerView implements ComponentHost {

    private Component mComponent;

    public SectionRecyclerView(@NonNull Context context) {
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        if (layoutManager instanceof LinearLayoutManager) {
            int orientation = ((LinearLayoutManager) layoutManager).getOrientation();
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (orientation == VERTICAL && !mComponent.isHeightDefined()) {
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (orientation == HORIZONTAL && !mComponent.isWidthDefined()) {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            requestLayout();
        }
    }
}
