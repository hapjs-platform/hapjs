/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.ThemeProvider;

public class ThemeUtils {

    public static int getAlertDialogTheme() {
        ThemeProvider provider = ProviderManager.getDefault().getProvider(ThemeProvider.NAME);
        return provider != null ? provider.getAlertDialogTheme() : 0;
    }
}
