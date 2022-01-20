/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.skeleton;

import android.content.Context;
import android.text.TextUtils;

public class DefaultSkeletonProviderImpl implements SkeletonProvider {
    private Context mContext;

    public DefaultSkeletonProviderImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isSkeletonEnable(String packageName) {
        if (mContext != null && !TextUtils.isEmpty(packageName)) {
            return true;
        }
        return false;
    }
}
