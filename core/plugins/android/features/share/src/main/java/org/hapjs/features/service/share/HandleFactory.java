/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.app.Activity;
import org.hapjs.features.service.share.impl.more.MoreShareApi;

public class HandleFactory {

    public static AbsShareApi createShareAPI(Activity activity,
                                             ShareContent shareContent, Platform platform) {
        AbsShareApi handler = null;
        switch (platform) {
            case SYSTEM:
                handler = new MoreShareApi(activity, shareContent, platform);
                break;
            default:
                break;
        }
        return handler;
    }
}