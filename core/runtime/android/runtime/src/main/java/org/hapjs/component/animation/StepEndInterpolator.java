/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

/**
 * StepEnd 步进插值器
 *
 * @author xiaopengfei
 * @date 2020/4/8
 */
public class StepEndInterpolator extends StepInterpolator {
    public StepEndInterpolator() {
        super(1, StepInterpolator.END);
    }
}
