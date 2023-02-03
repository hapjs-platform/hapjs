/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.adcustom;

import android.content.Context;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;

import java.util.HashMap;
import java.util.Map;

@WidgetAnnotation(
        name = AdCustom.WIDGET_NAME,
        methods = {
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
        }
)
public class AdCustom extends Component<AdCustomContainer> {
    protected static final String WIDGET_NAME = "ad-custom";
    private static final String TAG = "AdCustom";
    // attribute
    public static final String AD_UNIT_ID = "adunitid";

    public AdCustomDataProvider mAdCustomDataProvider;
    public String mAdUnitId;

    // event
    public static final String EVENT_LOAD = "load";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_SHOW = "show";
    public static final String EVENT_CLOSE = "close";
    public static final String EVENT_CLICK = "click";

    public AdCustom(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        initProxy();
    }

    protected void initProxy() {
        AdCustomDataProvider adCustomProvider = ProviderManager.getDefault().getProvider(AdCustomDataProvider.NAME);
        if (adCustomProvider == null) {
            mAdCustomDataProvider = new AdCustomDataProviderImpl();
        } else {
            mAdCustomDataProvider = adCustomProvider;
        }
        mAdCustomDataProvider.init(getHybridView());
    }

    @Override
    protected AdCustomContainer createViewImpl() {
        AdCustomContainer container = new AdCustomContainer(mContext);
        container.setLayoutParams(new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        container.setComponent(this);
        return container;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        if (mHost == null || mAdCustomDataProvider == null) {
            return true;
        }
        if (AD_UNIT_ID.equalsIgnoreCase(key)) {
            String adUnitId = Attributes.getString(attribute);
            if (TextUtils.isEmpty(adUnitId)) {
                mCallback.onJsException(new IllegalArgumentException("adUnitId must be defined"));
                return true;
            }
            if (!adUnitId.equals(mAdUnitId)) {
                mAdUnitId = adUnitId;
                mAdCustomDataProvider.loadCustomAd(mContext, mAdUnitId, mRef, this);
            }
            return true;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    public boolean isAdMaterial() {
        return true;
    }

    @Override
    protected void afterApplyDataToComponent() {
        if (isUseInList() && mAdCustomDataProvider != null) {
            mAdCustomDataProvider.loadCustomAd(mContext, mAdUnitId, mRef, this);
        }
    }

    @Override
    public void destroy() {
        if (mAdCustomDataProvider != null) {
            mAdCustomDataProvider.onComponentDestroy(mAdUnitId, mRef);
        }
        super.destroy();
    }

    public void onLoad(int ref) {
        if (mCallback == null) {
            return;
        }
        mCallback.onJsEventCallback(getPageId(), ref, EVENT_LOAD, this, null, null);
    }

    public void onError(int ref, int errorCode, String errorMsg) {
        if (mCallback == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("errCode", errorCode);
        params.put("errMsg", errorMsg);
        mCallback.onJsEventCallback(getPageId(), ref, EVENT_ERROR, this, params,
                null);
    }

    public void onShow(int ref) {
        if (mCallback == null) {
            return;
        }
        mCallback.onJsEventCallback(getPageId(), ref, EVENT_SHOW, this, null, null);
    }

    public void onClick(int ref) {
        if (mCallback == null) {
            return;
        }
        mCallback.onJsEventCallback(getPageId(), ref, EVENT_CLICK, this, null, null);
    }

    public void onClose(int ref) {
        if (mCallback == null) {
            return;
        }
        mCallback.onJsEventCallback(getPageId(), ref, EVENT_CLOSE, this, null, null);
    }

}
