/*
 * Copyright (c) 2021-2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import org.hapjs.ad.NativeAdEntity;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Response;
import org.hapjs.features.ad.BaseAd;
import org.hapjs.features.ad.NativeAd;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public abstract class BaseNativeAdInstance extends BaseAdInstance implements IAdInstance.INativeAdInstance {

    protected void onLoad(List<NativeAdEntity> list) {
        JSONObject result = new JSONObject();
        JSONArray adInfoArray = new JSONArray();
        try {
            result.put(NativeAdEntity.AD_LIST, adInfoArray);
            for (NativeAdEntity nativeAdEntity : list) {
                JSONObject adInfo = new JSONObject();
                adInfo.put(NativeAdEntity.AD_ID, nativeAdEntity.getAdId());
                adInfo.put(NativeAdEntity.TITLE, nativeAdEntity.getTitle());
                adInfo.put(NativeAdEntity.DESC, nativeAdEntity.getDesc());
                adInfo.put(NativeAdEntity.ICON, nativeAdEntity.getIcon());

                List<String> imgUrlList = nativeAdEntity.getImgUrlList();
                if (imgUrlList != null) {
                    JSONArray imgUrlArray = new JSONArray();
                    for (String imgUrl : imgUrlList) {
                        imgUrlArray.put(imgUrl);
                    }
                    adInfo.put(NativeAdEntity.IMG_URL_LIST, imgUrlArray);
                }

                adInfo.put(NativeAdEntity.LOGO_URL, nativeAdEntity.getLogoUrl());
                adInfo.put(NativeAdEntity.CLICK_BTN_TEXT, nativeAdEntity.getClickBtnTxt());
                adInfo.put(NativeAdEntity.CREATIVE_TYPE, nativeAdEntity.getCreativeType());
                adInfo.put(NativeAdEntity.INTERACTION_TYPE, nativeAdEntity.getInteractionType());

                adInfoArray.put(adInfo);
            }
        } catch (JSONException e) {
            onError(AdConstants.ERROR_INTERNAL, "data error");
            e.printStackTrace();
            return;
        }
        Map<String, Callback> actionCallbacks = mCallbackMap.get(BaseAd.ACTION_ON_LOAD);
        if (actionCallbacks == null) {
            cacheUnConsumeResponse(BaseAd.ACTION_ON_LOAD, new Response(result));
            return;
        }
        for (Map.Entry<String, Callback> entry : actionCallbacks.entrySet()) {
            entry.getValue().callback(new Response(result));
        }
    }

    @Override
    public String getFeatureName() {
        return NativeAd.FEATURE_NAME;
    }
}
