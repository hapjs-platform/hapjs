/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad;

import org.hapjs.bridge.Extension;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.ad.instance.BaseInterstitialAdInstance;

@FeatureExtensionAnnotation(
        name = InterstitialAd.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = InterstitialAd.ACTION_SHOW, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = InterstitialAd.ACTION_DESTROY, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = InterstitialAd.ACTION_ON_LOAD,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = InterstitialAd.ACTION_ON_CLOSE,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = InterstitialAd.ACTION_ON_ERROR,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = InterstitialAd.ACTION_OFF_LOAD,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = InterstitialAd.ACTION_OFF_CLOSE,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = InterstitialAd.ACTION_OFF_ERROR,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
        })
public class InterstitialAd extends BaseAd {
    public static final String FEATURE_NAME = "service.ad.interstitial";
    private static final String TAG = "InterstitialAd";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        BaseInterstitialAdInstance interstitialAdInstance =
                (BaseInterstitialAdInstance)
                        InstanceManager.getInstance().getInstance(request.getInstanceId());
        if (interstitialAdInstance == null) {
            return new Response(Response.CODE_SERVICE_UNAVAILABLE,
                    "no such interstitialAd instance");
        }
        switch (action) {
            case ACTION_SHOW:
                interstitialAdInstance.show(request.getCallback());
                break;
            case ACTION_DESTROY:
                interstitialAdInstance.destroy();
                break;
            case ACTION_ON_LOAD:
            case ACTION_ON_CLOSE:
            case ACTION_ON_ERROR:
                interstitialAdInstance.addListener(request);
                break;
            case ACTION_OFF_LOAD:
            case ACTION_OFF_CLOSE:
            case ACTION_OFF_ERROR:
                interstitialAdInstance.removeListener(request);
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
