/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.ad;

import java.util.List;

public class NativeAdEntity {
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
        return "NativeAdEntity{" +
                "adId='" + adId + '\'' +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", icon='" + icon + '\'' +
                ", imgUrlList=" + imgUrlList +
                ", logoUrl='" + logoUrl + '\'' +
                ", clickBtnTxt='" + clickBtnTxt + '\'' +
                ", creativeType=" + creativeType +
                ", interactionType=" + interactionType +
                '}';
    }
}
