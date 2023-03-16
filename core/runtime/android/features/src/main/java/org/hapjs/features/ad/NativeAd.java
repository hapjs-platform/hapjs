/*
 * Copyright (c) 2021-2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad;

import org.hapjs.ad.NativeAdEntity;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.features.ad.instance.BaseNativeAdInstance;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = NativeAd.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = NativeAd.ACTION_LOAD, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = NativeAd.ACTION_REPORT_AD_SHOW, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = NativeAd.ACTION_REPORT_AD_CLICK, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = NativeAd.ACTION_DESTROY, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = NativeAd.ACTION_ON_LOAD,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = NativeAd.ACTION_ON_ERROR,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = NativeAd.ACTION_OFF_LOAD,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = NativeAd.ACTION_OFF_ERROR,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
        })
public class NativeAd extends BaseAd {
    public static final String FEATURE_NAME = "service.ad.native";
    protected static final String ACTION_LOAD = "load";
    protected static final String ACTION_REPORT_AD_SHOW = "reportAdShow";
    protected static final String ACTION_REPORT_AD_CLICK = "reportAdClick";
    private static final String TAG = "NativeAd";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        BaseNativeAdInstance nativeAdInstance =
                (BaseNativeAdInstance) InstanceManager.getInstance()
                        .getInstance(request.getInstanceId());
        if (nativeAdInstance == null) {
            return new Response(Response.CODE_SERVICE_UNAVAILABLE, "no such nativeAd instance");
        }

        String action = request.getAction();
        JSONObject jsonObject = request.getJSONParams();
        String adId = jsonObject.optString(NativeAdEntity.AD_ID);

        switch (action) {
            case ACTION_LOAD:
                nativeAdInstance.load();
                break;
            case ACTION_REPORT_AD_SHOW:
                nativeAdInstance.reportAdShow(adId);
                break;
            case ACTION_REPORT_AD_CLICK:
                nativeAdInstance.reportAdClick(adId);
                break;
            case ACTION_DESTROY:
                nativeAdInstance.destroy();
                break;
            case ACTION_ON_LOAD:
            case ACTION_ON_ERROR:
                nativeAdInstance.addListener(request);
                break;
            case ACTION_OFF_LOAD:
            case ACTION_OFF_ERROR:
                nativeAdInstance.removeListener(request);
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
