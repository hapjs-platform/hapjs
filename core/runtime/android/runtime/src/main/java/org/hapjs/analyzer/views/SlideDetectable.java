/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.views;

public interface SlideDetectable {
    void onSlideToBottom();

    boolean isSlideToBottom();

    interface OnSlideToBottomListener {
        void onSlideToBottom();
    }
}
