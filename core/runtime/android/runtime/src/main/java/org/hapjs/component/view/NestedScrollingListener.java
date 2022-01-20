/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

public interface NestedScrollingListener {

    void onFling(int velocityX, int velocityY);

    void onOverScrolled(int dx, int dy);
}
