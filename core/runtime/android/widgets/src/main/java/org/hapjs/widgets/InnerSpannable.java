/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.text.Spannable;
import java.util.List;

public interface InnerSpannable {

    int STYLE_UNDEFINED = -1;

    void applySpannable();

    Spannable getSpannable();

    List<Spannable> getChildrenSpannable();
}
