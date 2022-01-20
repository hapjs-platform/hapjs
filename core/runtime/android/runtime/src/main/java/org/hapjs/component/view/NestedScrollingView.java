/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.view.ViewGroup;

public interface NestedScrollingView {

    boolean shouldScrollFirst(int dy, int velocityY);

    boolean nestedFling(int velocityX, int velocityY);

    NestedScrollingListener getNestedScrollingListener();

    void setNestedScrollingListener(NestedScrollingListener listener);

    ViewGroup getChildNestedScrollingView();
}
