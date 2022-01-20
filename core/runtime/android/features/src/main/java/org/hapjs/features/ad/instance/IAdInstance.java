/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import org.hapjs.bridge.Callback;
import org.hapjs.bridge.InstanceManager;

public interface IAdInstance extends InstanceManager.IInstance {

    void destroy();

    interface IBannerAdInstance extends IAdInstance {
        void show(Callback callback);

        void hide(Callback callback);
    }

    interface IInterstitialAdInstance extends IAdInstance {
        void show(Callback callback);
    }

    interface INativeAdInstance extends IAdInstance {
        void load();

        void reportAdShow(String adId);

        void reportAdClick(String adId);
    }

    interface IRewardedVideoAdInstance extends IAdInstance {
        void load(Callback callback);

        void show(Callback callback);
    }
}
