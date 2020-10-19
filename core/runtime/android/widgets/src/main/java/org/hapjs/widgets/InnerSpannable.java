/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.text.Spannable;

public interface InnerSpannable {

    int STYLE_UNDEFINED = -1;

    void applySpannable();

    Spannable getSpannable();
}
