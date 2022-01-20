/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import org.hapjs.runtime.R;

public class ViewIdUtils {

    private static final int VIEW_ID_TYPE = 0x00ff0000;
    private static final int VIEW_ID_NONE = -1;

    private static int mViewIdBase = VIEW_ID_NONE;

    public static int getViewId(int ref) {
        return getViewIdBase() + ref;
    }

    public static int getViewIdBase() {
        if (mViewIdBase == VIEW_ID_NONE) {
            int packageId = R.layout.hybrid_main & 0xff000000;
            mViewIdBase = packageId + VIEW_ID_TYPE;
        }
        return mViewIdBase;
    }
}
