/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.ad;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;

import org.hapjs.ad.AdProvider;
import org.hapjs.ad.AdProxy;
import org.hapjs.ad.NativeAdEntity;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WidgetAnnotation(
        name = Ad.WIDGET_NAME
)
public class Ad extends Container<AdContainer> {
    private static final String TAG = "Ad";
    private static final String NATIVE = "native";
    protected static final String WIDGET_NAME = "ad";
    public static final int ERROR_UNKNOWN = 2000;
    public static final int ID_INVALID = -1;
    // attribute
    private static final String UNIT_ID = "adunitid";
    private static final String TYPE = "type";
    private static final String ID = "adid";
    //event
    private static final String ERROR = "error";
    private static final String LOAD = "load";
    private static final String AD_CLICK = "adclick";
    private static final String AD_SHOW = "adshow";

    private boolean mOnLoadRegistered;//是否注册加载监听
    private boolean mOnErrorRegistered;//是否注册错误监听
    private boolean mOnAdClickRegistered;
    private boolean mOnAdShowRegistered;
    private AdProxy mAdProxy;
    private String mUnitId;
    private String mType;
    private int mId = ID_INVALID;
    //key为广告id，用于缓存此id的缓存数据，当组件从屏幕划出再次划入时，防止重复拉取广告数据
    private SparseArray<NativeAdEntity> mCacheNativeAds;


    public Ad(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mCacheNativeAds = new SparseArray<>();
        initProxy(context);
    }

    @Override
    protected AdContainer createViewImpl() {
        AdContainer container = new AdContainer(mContext);
        container.setComponent(this);
        return container;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case UNIT_ID:
                String adId = Attributes.getString(attribute);
                if (TextUtils.isEmpty(adId)) {
                    mCallback.onJsException(new IllegalArgumentException("adid must be defined"));
                    return true;
                }
                setUnitId(adId);
                return true;
            case TYPE:
                String type = Attributes.getString(attribute);
                if (TextUtils.equals(type, NATIVE)) {
                    setType(type);
                } else {
                    //其他类型广告暂不支持，直接移除该组件
                    mParent.removeChild(this);
                }
                return true;
            case ID:
                String id = Attributes.getString(attribute);
                setAdId(id);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    private void setAdId(String idStr) {
        int id = 0;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }
        if (id != mId && mId != ID_INVALID) {
            //List中复用时，会触发此逻辑
            NativeAdEntity entity = mCacheNativeAds.get(id);
            if (entity != null) {
                onLoad(entity);
                return;
            }
            mAdProxy.loadAd(mUnitId, mType);
        }
        mId = id;
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (ERROR.equals(event)) {
            mOnErrorRegistered = true;
            return true;
        } else if (LOAD.equals(event)) {
            mOnLoadRegistered = true;
            return true;
        }else if (AD_CLICK.equals(event)) {
            mOnAdClickRegistered = true;
            return true;
        }else if (AD_SHOW.equals(event)) {
            mOnAdShowRegistered = true;
            return true;
        }
        return true;
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (ERROR.equals(event)) {
            mOnErrorRegistered = false;
            return true;
        } else if (LOAD.equals(event)) {
            mOnLoadRegistered = false;
            return true;
        }else if (AD_CLICK.equals(event)) {
            mOnAdClickRegistered = false;
            return true;
        }else if (AD_SHOW.equals(event)) {
            mOnAdShowRegistered = false;
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public boolean isAdMaterial() {
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
        mAdProxy.destroy();
    }

    public void onLoad(NativeAdEntity nativeAdEntity) {
        Log.d(TAG, "onLoad:" + nativeAdEntity);
        if (mId != ID_INVALID && mCacheNativeAds.get(mId) == null) {
            mCacheNativeAds.put(mId, nativeAdEntity);
        }
        mAdProxy.bindClickView(mUnitId, mType, nativeAdEntity);
        if (!mOnLoadRegistered) {
            return;
        }

        try {
            Map<String, Object> params = new HashMap<>(1);
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
            params.put("adData", adInfo);
            mCallback.onJsEventCallback(getPageId(), mRef, LOAD, Ad.this, params,
                    null);
        } catch (JSONException e) {
            e.printStackTrace();
            onError(ERROR_UNKNOWN, "parse data JSONException");
        }
    }

    public void onError(int errorCode, String errorMsg) {
        Log.e(TAG, "onError:errorCode=" + errorCode + ",errorMsg=" + errorMsg);
        if (!mOnErrorRegistered) {
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("errCode", errorCode);
        params.put("errMsg", errorMsg);
        mCallback.onJsEventCallback(getPageId(), mRef, ERROR, Ad.this, params,
                null);
    }

    public void onAdClick() {
        Log.w(TAG, "onClick");
        if (!mOnAdClickRegistered) {
            return;
        }
        mCallback.onJsEventCallback(getPageId(), mRef, AD_CLICK, Ad.this, null,
                null);
    }

    public void onAdShow() {
        Log.w(TAG, "onAdShow");
        if (!mOnAdShowRegistered) {
            return;
        }
        mCallback.onJsEventCallback(getPageId(), mRef, AD_SHOW, Ad.this, null,
                null);
    }

    public void setUnitId(String unitId) {
        if (!unitId.equals(mUnitId)) {
            mUnitId = unitId;
            if (!TextUtils.isEmpty(mType)) {
                mAdProxy.loadAd(mUnitId, mType);
            }
        }
    }

    public void setType(String type) {
        if (!TextUtils.equals(type, mType)) {
            mType = type;
            View adView = mAdProxy.initAdView(mType, mHost);
            if (adView != null && mHost != adView) {
                mHost.removeAllViews();
                mHost.addView(adView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            }
            mHost.setAdView(adView);
            if (!TextUtils.isEmpty(mUnitId)) {
                mAdProxy.loadAd(mUnitId, mType);
            }
        }
    }

    private void initProxy(Context context) {
        AdProvider adProvider = ProviderManager.getDefault().getProvider(AdProvider.NAME);
        if (adProvider != null) {
            mAdProxy = adProvider.createAdCustomProxy(context,this);
        }
        if (mAdProxy == null) {
            mAdProxy = new AdProxyImpl(this);
        }
    }
}
