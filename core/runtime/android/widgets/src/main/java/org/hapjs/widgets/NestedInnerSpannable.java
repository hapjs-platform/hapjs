/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.text.Spannable;

import java.util.List;

public interface NestedInnerSpannable extends InnerSpannable{
    List<Spannable> getChildrenSpannable();
}
