/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;

public class DefaultHybridDialogProviderImpl implements HybridDialogProvider {
    @Override
    public HybridDialog createAlertDialog(Context context, int themeResId) {
        return new HybridDialogImpl(context, themeResId);
    }

    @Override
    public HybridDialog createDialogWithButtonColors(
            Context context, int themeResId, String[] colorArray) {
        return new HybridDialogImpl(context, themeResId, colorArray);
    }
}
