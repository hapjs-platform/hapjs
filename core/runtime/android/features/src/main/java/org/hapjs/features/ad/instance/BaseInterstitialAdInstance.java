/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import org.hapjs.features.ad.InterstitialAd;

public abstract class BaseInterstitialAdInstance extends BaseAdInstance
        implements IAdInstance.IInterstitialAdInstance {

    @Override
    public String getFeatureName() {
        return InterstitialAd.FEATURE_NAME;
    }
}
