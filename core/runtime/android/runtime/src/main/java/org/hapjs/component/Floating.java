/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.view.View;

public interface Floating {
    void show(View anchor);

    void dismiss();
}
