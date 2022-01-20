/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.text.TextUtils;

public class ResourceConfig {
    private boolean mLoadFromLocal = true;
    private String mPlatform;
    private boolean mUseRemoteMode = false;

    private ResourceConfig() {
    }

    public static ResourceConfig getInstance() {
        return ResourceConfig.Holder.INSTANCE;
    }

    public void init(Context context, String platform) {
        mPlatform = platform;
        mLoadFromLocal = TextUtils.equals(platform, context.getPackageName());
    }

    public void setUseRemoteMode(boolean useRemote) {
        mUseRemoteMode = useRemote;
    }

    public boolean isLoadFromLocal() {
        return mLoadFromLocal && !mUseRemoteMode;
    }

    public String getPlatform() {
        if (TextUtils.isEmpty(mPlatform)) {
            mPlatform = Runtime.getInstance().getContext().getPackageName();
        }
        return mPlatform;
    }

    private static class Holder {
        static ResourceConfig INSTANCE = new ResourceConfig();
    }
}
