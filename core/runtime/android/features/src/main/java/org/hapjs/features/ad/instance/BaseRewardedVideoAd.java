/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import org.hapjs.features.ad.RewardedVideoAd;

public abstract class BaseRewardedVideoAd extends BaseAdInstance
        implements IAdInstance.IRewardedVideoAdInstance {
    @Override
    public String getFeatureName() {
        return RewardedVideoAd.FEATURE_NAME;
    }
}
