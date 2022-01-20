/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.gesture;

public interface GestureHost {
    IGesture getGesture();

    void setGesture(IGesture gestureDelegate);
}
