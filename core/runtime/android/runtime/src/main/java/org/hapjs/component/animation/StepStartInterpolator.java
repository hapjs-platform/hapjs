/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

/**
 * StepStart 步进插值器
 *
 * @author xiaopengfei
 * @date 2020/4/8
 */
public class StepStartInterpolator extends StepInterpolator {
    public StepStartInterpolator() {
        super(1, StepInterpolator.START);
    }
}
