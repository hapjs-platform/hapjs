/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad;

import android.util.Log;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.ad.instance.BaseRewardedVideoAd;

@FeatureExtensionAnnotation(
        name = RewardedVideoAd.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = RewardedVideoAd.ACTION_LOAD, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = RewardedVideoAd.ACTION_SHOW, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = BannerAd.ACTION_DESTROY, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = RewardedVideoAd.ACTION_ON_LOAD,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = RewardedVideoAd.ACTION_ON_CLOSE,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = RewardedVideoAd.ACTION_ON_ERROR,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = RewardedVideoAd.ACTION_OFF_LOAD,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = RewardedVideoAd.ACTION_OFF_CLOSE,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = RewardedVideoAd.ACTION_OFF_ERROR,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
        })
public class RewardedVideoAd extends BaseAd {
    public static final String FEATURE_NAME = "service.ad.rewardedVideo";
    private static final String TAG = "RewardedVideoAd";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        BaseRewardedVideoAd ad =
                (BaseRewardedVideoAd) InstanceManager.getInstance()
                        .getInstance(request.getInstanceId());
        if (ad == null) {
            Log.d(TAG, "no such rewardedVideoAd instance");
            return new Response(Response.CODE_SERVICE_UNAVAILABLE,
                    "no such rewardedVideoAd instance");
        }
        String action = request.getAction();
        switch (action) {
            case ACTION_LOAD:
                ad.load(request.getCallback());
                break;
            case ACTION_SHOW:
                ad.show(request.getCallback());
                break;
            case ACTION_DESTROY:
                ad.destroy();
                break;
            case ACTION_ON_LOAD:
            case ACTION_ON_CLOSE:
            case ACTION_ON_ERROR:
                ad.addListener(request);
                break;
            case ACTION_OFF_LOAD:
            case ACTION_OFF_CLOSE:
            case ACTION_OFF_ERROR:
                ad.removeListener(request);
                break;
            default:
                break;
        }
        return Response.SUCCESS;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }
}
