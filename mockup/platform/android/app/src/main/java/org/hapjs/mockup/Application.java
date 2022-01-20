/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup;

import android.content.Context;
import androidx.multidex.MultiDex;
import org.hapjs.common.utils.hiddenapi.Reflection;
import org.hapjs.runtime.RuntimeApplication;

public class Application extends RuntimeApplication {

    @Override
    protected void attachBaseContext(Context base) {
        // Install multi-dex before any other code
        MultiDex.install(base);
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }
}
