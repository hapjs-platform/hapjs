/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

public class EaseInInterpolator extends CubicBezierInterpolator {

    public EaseInInterpolator() {
        super(0.42f, 0f, 1f, 1f);
    }
}
