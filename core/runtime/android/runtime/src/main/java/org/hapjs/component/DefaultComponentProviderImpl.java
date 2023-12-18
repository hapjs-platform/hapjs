/*
 * Copyright (c) 2023-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;

public class DefaultComponentProviderImpl implements ComponentProvider {
    @Override
    public boolean isDefaultRgb565EnableBelowAndroidO(Context context) {
        return true;
    }

    @Override
    public boolean isSysShowSizeChange() {
        return false;
    }
}