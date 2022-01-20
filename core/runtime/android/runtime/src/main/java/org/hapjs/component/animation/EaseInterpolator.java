/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

public class EaseInterpolator extends CubicBezierInterpolator {

    public EaseInterpolator() {
        super(0.25f, 0.1f, 0.25f, 1f);
    }
}
