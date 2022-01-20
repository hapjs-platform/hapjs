/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import java.util.List;
import java.util.Map;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Response;
import org.hapjs.features.ad.BaseAd;
import org.hapjs.features.ad.NativeAd;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseNativeAdInstance extends BaseAdInstance
        implements IAdInstance.INativeAdInstance {
    public static final String AD_LIST = "adList";
    public static final String AD_ID = "adId";
    public static final String TITLE = "title";
    public static final String DESC = "desc";
    public static final String ICON = "icon";
    public static final String IMG_URL_LIST = "imgUrlList";
    public static final String LOGO_URL = "logoUrl";
    public static final String CLICK_BTN_TEXT = "clickBtnTxt";
    public static final String CREATIVE_TYPE = "creativeType";
    public static final String INTERACTION_TYPE = "interactionType";

    protected void onLoad(List<NativeAdEntity> list) {
        JSONObject result = new JSONObject();
        JSONArray adInfoArray = new JSONArray();
        try {
            result.put(AD_LIST, adInfoArray);
            for (NativeAdEntity nativeAdEntity : list) {
                JSONObject adInfo = new JSONObject();
                adInfo.put(AD_ID, nativeAdEntity.getAdId());
                adInfo.put(TITLE, nativeAdEntity.getTitle());
                adInfo.put(DESC, nativeAdEntity.getDesc());
                adInfo.put(ICON, nativeAdEntity.getIcon());

                List<String> imgUrlList = nativeAdEntity.getImgUrlList();
                if (imgUrlList != null) {
                    JSONArray imgUrlArray = new JSONArray();
                    for (String imgUrl : imgUrlList) {
                        imgUrlArray.put(imgUrl);
                    }
                    adInfo.put(IMG_URL_LIST, imgUrlArray);
                }

                adInfo.put(LOGO_URL, nativeAdEntity.getLogoUrl());
                adInfo.put(CLICK_BTN_TEXT, nativeAdEntity.getClickBtnTxt());
                adInfo.put(CREATIVE_TYPE, nativeAdEntity.getCreativeType());
                adInfo.put(INTERACTION_TYPE, nativeAdEntity.getInteractionType());

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

    public static class NativeAdEntity {
        private String adId;
        private String title;
        private String desc;
        private String icon;
        private List<String> imgUrlList;
        private String logoUrl;
        private String clickBtnTxt;
        private int creativeType;
        private int interactionType;

        public String getAdId() {
            return adId;
        }

        public void setAdId(String adId) {
            this.adId = adId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public List<String> getImgUrlList() {
            return imgUrlList;
        }

        public void setImgUrlList(List<String> imgUrlList) {
            this.imgUrlList = imgUrlList;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        public String getClickBtnTxt() {
            return clickBtnTxt;
        }

        public void setClickBtnTxt(String clickBtnTxt) {
            this.clickBtnTxt = clickBtnTxt;
        }

        public int getCreativeType() {
            return creativeType;
        }

        public void setCreativeType(int creativeType) {
            this.creativeType = creativeType;
        }

        public int getInteractionType() {
            return interactionType;
        }

        public void setInteractionType(int interactionType) {
            this.interactionType = interactionType;
        }

        @Override
        public String toString() {
            return "NativeAdEntity{"
                    + "adId='"
                    + adId
                    + '\''
                    + ", title='"
                    + title
                    + '\''
                    + ", desc='"
                    + desc
                    + '\''
                    + ", icon='"
                    + icon
                    + '\''
                    + ", imgUrlList="
                    + imgUrlList
                    + ", logoUrl='"
                    + logoUrl
                    + '\''
                    + ", clickBtnTxt='"
                    + clickBtnTxt
                    + '\''
                    + ", creativeType="
                    + creativeType
                    + ", interactionType="
                    + interactionType
                    + '}';
        }
    }
}
