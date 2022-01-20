/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad;

import android.app.Activity;
import android.text.TextUtils;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.ad.impl.BannerAdInstance;
import org.hapjs.features.ad.impl.InterstitialAdInstance;
import org.hapjs.features.ad.impl.NativeAdInstance;
import org.hapjs.features.ad.impl.RewardedVideoAdInstance;
import org.hapjs.features.ad.instance.BaseBannerAdInstance;
import org.hapjs.features.ad.instance.IAdInstance;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Ad.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Ad.ACTION_CREATE_BANNER_AD, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = Ad.ACTION_CREATE_INTERSTITIAL_AD, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = Ad.ACTION_CREATE_NATIVE_AD, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = Ad.ACTION_CREATE_REWARDED_VIDEO_AD, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = Ad.ACTION_GET_PROVIDER, mode = Extension.Mode.SYNC)
        })
public class Ad extends FeatureExtension {
    protected static final String FEATURE_NAME = "service.ad";
    protected static final String ACTION_CREATE_BANNER_AD = "createBannerAd";
    protected static final String ACTION_CREATE_INTERSTITIAL_AD = "createInterstitialAd";
    protected static final String ACTION_CREATE_NATIVE_AD = "createNativeAd";
    protected static final String ACTION_CREATE_REWARDED_VIDEO_AD = "createRewardedVideoAd";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    protected static final String PARAMS_KEY_AD_UNIT_ID = "adUnitId";
    private static final String TAG = "Ad";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_PROVIDER.equals(action)) {
            return getProvider();
        }

        JSONObject jsonParams = request.getJSONParams();
        String adUnitId = jsonParams.optString(PARAMS_KEY_AD_UNIT_ID);
        if (TextUtils.isEmpty(adUnitId)) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "adUnitId can not be empty");
        }
        Activity activity = request.getNativeInterface().getActivity();

        IAdInstance adInstance = null;
        if (ACTION_CREATE_BANNER_AD.equals(action)) {
            int designWidth = request.getHapEngine().getDesignWidth();
            BaseBannerAdInstance.Style style = null;
            JSONObject styleParams = jsonParams.optJSONObject(BannerAd.PARAMS_KEY_STYLE);
            if (styleParams != null) {
                style = BaseBannerAdInstance.jsonToStyle(styleParams, designWidth);
            }
            adInstance = createBannerAd(activity, adUnitId, style, designWidth);
        } else if (ACTION_CREATE_INTERSTITIAL_AD.equals(action)) {
            adInstance = createInterstitialAd(activity, adUnitId);
        } else if (ACTION_CREATE_NATIVE_AD.equals(action)) {
            adInstance = createNativeAd(activity, adUnitId);
        } else if (ACTION_CREATE_REWARDED_VIDEO_AD.equals(action)) {
            adInstance = createRewardedVideoAd(activity, adUnitId);
        }

        if (adInstance != null) {
            HybridManager hybridManager = request.getView().getHybridManager();
            return new Response(
                    InstanceManager.getInstance().createInstance(hybridManager, adInstance));
        }
        return Response.NO_ACTION;
    }

    protected IAdInstance createBannerAd(
            Activity activity, String adUnitId, BaseBannerAdInstance.Style style, int designWidth) {
        return new BannerAdInstance(activity, adUnitId, style, designWidth);
    }

    protected IAdInstance createInterstitialAd(Activity activity, String adUnitId) {
        return new InterstitialAdInstance(activity, adUnitId);
    }

    protected IAdInstance createNativeAd(Activity activity, String adUnitId) {
        return new NativeAdInstance(activity, adUnitId);
    }

    protected IAdInstance createRewardedVideoAd(Activity activity, String adUnitId) {
        return new RewardedVideoAdInstance(activity, adUnitId);
    }

    protected Response getProvider() {
        return new Response("");
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
