/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad.instance;

import android.util.Log;
import java.util.Map;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Response;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.features.ad.BannerAd;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseBannerAdInstance extends BaseAdInstance
        implements IAdInstance.IBannerAdInstance {
    public static final float MIN_BANNER_AD_WIDTH_RADIO = 0.5f;
    private static final String TAG = "BaseBannerAdInstance";
    protected Style mStyle;
    private int mDesignWidth;

    public BaseBannerAdInstance(Style style, int designWidth) {
        if (style == null) {
            mStyle = new Style();
        } else {
            mStyle = style;
        }
        mDesignWidth = designWidth;
    }

    public static Style jsonToStyle(JSONObject jsonObject, int designWidth) throws JSONException {
        int leftPx = Style.DEFAULT_VALUE;
        int topPx = Style.DEFAULT_VALUE;
        int widthPx = Style.DEFAULT_VALUE;
        int heightPx = Style.DEFAULT_VALUE;
        if (jsonObject.has(BannerAd.PARAMS_KEY_LEFT)) {
            int left = jsonObject.getInt(BannerAd.PARAMS_KEY_LEFT);
            leftPx = (int) DisplayUtil.getRealPxByWidth(left, designWidth);
        }
        if (jsonObject.has(BannerAd.PARAMS_KEY_TOP)) {
            int top = jsonObject.getInt(BannerAd.PARAMS_KEY_TOP);
            topPx = (int) DisplayUtil.getRealPxByWidth(top, designWidth);
        }
        if (jsonObject.has(BannerAd.PARAMS_KEY_WIDTH)) {
            int width = jsonObject.getInt(BannerAd.PARAMS_KEY_WIDTH);
            widthPx = (int) DisplayUtil.getRealPxByWidth(width, designWidth);
        }
        if (jsonObject.has(BannerAd.PARAMS_KEY_HEIGHT)) {
            int height = jsonObject.getInt(BannerAd.PARAMS_KEY_HEIGHT);
            heightPx = (int) DisplayUtil.getRealPxByWidth(height, designWidth);
        }
        return new Style(leftPx, topPx, widthPx, heightPx);
    }

    public boolean setStyle(Style style) {
        boolean changed = false;
        if (style == null || mStyle == null) {
            return false;
        }
        if (style.getLeft() != Style.DEFAULT_VALUE && style.getLeft() != mStyle.getLeft()) {
            mStyle.setLeft(style.getLeft());
            changed = true;
        }
        if (style.getTop() != Style.DEFAULT_VALUE && style.getTop() != mStyle.getTop()) {
            mStyle.setTop(style.getTop());
            changed = true;
        }
        if (style.getWidth() != Style.DEFAULT_VALUE && style.getWidth() != mStyle.getWidth()) {
            mStyle.setWidth(style.getWidth());
            changed = true;
        }
        if (style.getHeight() != Style.DEFAULT_VALUE && style.getHeight() != mStyle.getHeight()) {
            mStyle.setHeight(style.getHeight());
            changed = true;
        }
        Log.d(TAG, "setStyle: " + mStyle + "-" + mDesignWidth);
        return changed;
    }

    public Style getStyle() {
        Log.d(TAG, "getStyle: " + mStyle + "-" + mDesignWidth);
        return mStyle;
    }

    protected void onResize(int width, int height) {
        JSONObject jsonObject = new JSONObject();
        Response response = null;
        try {
            jsonObject.put(
                    BannerAd.PARAMS_KEY_WIDTH,
                    (int) DisplayUtil.getDesignPxByWidth(width, mDesignWidth));
            jsonObject.put(
                    BannerAd.PARAMS_KEY_HEIGHT,
                    (int) DisplayUtil.getDesignPxByWidth(height, mDesignWidth));
            response = new Response(jsonObject);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "onError fail,JSONException occurred", e);
            response = new Response(Response.CODE_GENERIC_ERROR,
                    "onError fail,JSONException occurred");
        }
        Map<String, Callback> actionCallbacks = mCallbackMap.get(BannerAd.ACTION_ON_RESIZE);
        if (actionCallbacks == null) {
            cacheUnConsumeResponse(BannerAd.ACTION_ON_RESIZE, response);
            return;
        }
        for (Map.Entry<String, Callback> entry : actionCallbacks.entrySet()) {
            entry.getValue().callback(response);
        }
    }

    @Override
    public String getFeatureName() {
        return BannerAd.FEATURE_NAME;
    }

    public static class Style {
        public static final int DEFAULT_VALUE = Integer.MIN_VALUE;
        int left = DEFAULT_VALUE;
        int top = DEFAULT_VALUE;
        int width = DEFAULT_VALUE;
        int height = DEFAULT_VALUE;
        int realWidth = DEFAULT_VALUE;
        int realHeight = DEFAULT_VALUE;

        public Style() {
        }

        public Style(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getRealWidth() {
            return realWidth;
        }

        public void setRealWidth(int realWidth) {
            this.realWidth = realWidth;
        }

        public int getRealHeight() {
            return realHeight;
        }

        public void setRealHeight(int realHeight) {
            this.realHeight = realHeight;
        }

        public JSONObject toJSON(int designWidth) throws JSONException {
            JSONObject jsonObject = new JSONObject();
            int[] paramsArray = new int[] {left, top, width, height, realWidth, realHeight};
            String[] keyArray =
                    new String[] {
                            BannerAd.PARAMS_KEY_LEFT,
                            BannerAd.PARAMS_KEY_TOP,
                            BannerAd.PARAMS_KEY_WIDTH,
                            BannerAd.PARAMS_KEY_HEIGHT,
                            BannerAd.PARAMS_KEY_REAL_WIDTH,
                            BannerAd.PARAMS_KEY_REAL_HEIGHT
                    };
            for (int i = 0; i < paramsArray.length; i++) {
                if (paramsArray[i] != DEFAULT_VALUE) {
                    jsonObject.put(
                            keyArray[i],
                            (int) DisplayUtil.getDesignPxByWidth(paramsArray[i], designWidth));
                }
            }
            return jsonObject;
        }

        @Override
        public String toString() {
            return "Style{"
                    + "left="
                    + left
                    + ", top="
                    + top
                    + ", width="
                    + width
                    + ", height="
                    + height
                    + ", realWidth="
                    + realWidth
                    + ", realHeight="
                    + realHeight
                    + '}';
        }
    }
}
