/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.text.TextUtils;

public enum Platform {
    SYSTEM(R.string.share_more_name, R.drawable.feature_share_more);

    public int mName;
    public int mIcon;

    private Platform(int name, int icon) {
        this.mName = name;
        this.mIcon = icon;
    }

    public static Platform convertToEmun(String media) {
        if (TextUtils.isEmpty(media)) {
            return null;
        }
        //compatible for old platforms constant: MORE
        if ("MORE".equals(media)) {
            return SYSTEM;
        }
        return Platform.valueOf(media);
    }
}