/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

public class EaseOutInterpolator extends CubicBezierInterpolator {

    public EaseOutInterpolator() {
        super(0f, 0f, 0.58f, 1f);
    }
}
