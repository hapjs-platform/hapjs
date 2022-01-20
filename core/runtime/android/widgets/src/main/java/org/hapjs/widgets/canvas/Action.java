/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

public abstract class Action {
    private String mAction;

    public Action(String action) {
        mAction = action;
    }

    public String getAction() {
        return mAction;
    }
}
