/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.cutout;

import android.os.Build;
import androidx.annotation.NonNull;
import org.hapjs.runtime.ProviderManager;

public class CutoutSupportFactory {
    private static final int VERSION = Build.VERSION.SDK_INT;

    @NonNull
    public static ICutoutSupport createCutoutSupport() {
        if (VERSION < Build.VERSION_CODES.O) {
            return new CommonCutoutSupport();
        } else if (VERSION < Build.VERSION_CODES.P) {
            CutoutProvider provider = ProviderManager.getDefault().getProvider(CutoutProvider.NAME);
            return new OCutoutSupport(provider);
        } else {
            return new PCutoutSupport();
        }
    }
}
