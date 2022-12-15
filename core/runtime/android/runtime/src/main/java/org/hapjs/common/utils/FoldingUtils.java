/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import org.hapjs.model.DisplayInfo;

public class FoldingUtils {
    public static String mRpkWideScreenFitMode;

    public static String getRpkWideScreenFitMode() {
        return mRpkWideScreenFitMode;
    }

    public static void setRpkWideScreenFitMode(String rpkWideScreenFitMode) {
        mRpkWideScreenFitMode = rpkWideScreenFitMode;
    }

    public static boolean isFoldFullScreenMode() {
        return isFoldFullScreenMode(mRpkWideScreenFitMode);
    }

    public static boolean isFoldFullScreenMode(String fitMode) {
        if (!DisplayInfo.MODE_FIT_SCREEN.equals(fitMode)) {
            return true;
        }
        return false;
    }

    public static boolean isMultiWindowMode() {
        return DisplayInfo.MODE_MULTI_WINDOW.equals(mRpkWideScreenFitMode);
    }

    public static boolean isAdaptiveScreenMode() {
        return DisplayInfo.MODE_ADAPTIVE_SCREEN.equals(mRpkWideScreenFitMode);
    }

}
