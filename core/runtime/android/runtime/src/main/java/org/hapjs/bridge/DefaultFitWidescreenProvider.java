/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.text.TextUtils;

import org.hapjs.model.DisplayInfo;

public class DefaultFitWidescreenProvider implements FitWidescreenProvider {
    @Override
    public String getFitMode(String packageName, String fitMode) {
        if (TextUtils.isEmpty(fitMode)) {
            return DisplayInfo.MODE_ORIGINAL;
        }
        return fitMode;
    }
}
