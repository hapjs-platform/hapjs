/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;

public interface HybridDialogProvider {
    String NAME = "HybridDialogProvider";

    HybridDialog createAlertDialog(Context context, int themeResId);

    HybridDialog createDialogWithButtonColors(Context context, int themeResId, String[] colorArray);
}
