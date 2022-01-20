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
import org.hapjs.features.ad.instance.BaseBannerAdInstance;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = BannerAd.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = BannerAd.ACTION_SHOW, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = BannerAd.ACTION_HIDE, mode = Extension.Mode.ASYNC),
                @ActionAnnotation(name = BannerAd.ACTION_DESTROY, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = BannerAd.ACTION_ON_LOAD,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_ON_CLOSE,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_ON_ERROR,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_ON_RESIZE,
                        mode = Extension.Mode.CALLBACK,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_OFF_LOAD,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_OFF_CLOSE,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_OFF_ERROR,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ACTION_OFF_RESIZE,
                        mode = Extension.Mode.SYNC,
                        multiple = Extension.Multiple.MULTI),
                @ActionAnnotation(
                        name = BannerAd.ATTR_GET_STYLE,
                        mode = Extension.Mode.SYNC,
                        type = Extension.Type.ATTRIBUTE,
                        access = Extension.Access.READ,
                        alias = BannerAd.ATTR_STYLE_ALIAS,
                        subAttrs = {
                                BannerAd.PARAMS_KEY_LEFT,
                                BannerAd.PARAMS_KEY_TOP,
                                BannerAd.PARAMS_KEY_WIDTH,
                                BannerAd.PARAMS_KEY_HEIGHT,
                                BannerAd.PARAMS_KEY_REAL_WIDTH,
                                BannerAd.PARAMS_KEY_REAL_HEIGHT
                        }),
                @ActionAnnotation(
                        name = BannerAd.ATTR_SET_STYLE,
                        mode = Extension.Mode.SYNC,
                        type = Extension.Type.ATTRIBUTE,
                        access = Extension.Access.WRITE,
                        alias = BannerAd.ATTR_STYLE_ALIAS,
                        subAttrs = {
                                BannerAd.PARAMS_KEY_LEFT,
                                BannerAd.PARAMS_KEY_TOP,
                                BannerAd.PARAMS_KEY_WIDTH,
                                BannerAd.PARAMS_KEY_HEIGHT
                        }),
        })
public class BannerAd extends BaseAd {
    public static final String FEATURE_NAME = "service.ad.banner";
    public static final String ACTION_ON_RESIZE = "onResize";
    public static final String ACTION_OFF_RESIZE = "offResize";
    public static final String PARAMS_KEY_STYLE = "style";
    public static final String PARAMS_KEY_LEFT = "left";
    public static final String PARAMS_KEY_TOP = "top";
    public static final String PARAMS_KEY_WIDTH = "width";
    public static final String PARAMS_KEY_HEIGHT = "height";
    public static final String PARAMS_KEY_REAL_WIDTH = "realWidth";
    public static final String PARAMS_KEY_REAL_HEIGHT = "realHeight";
    protected static final String ACTION_HIDE = "hide";
    protected static final String ATTR_STYLE_ALIAS = "style";
    protected static final String ATTR_SET_STYLE = "__setStyle";
    protected static final String ATTR_GET_STYLE = "__getStyle";
    private static final String TAG = "BannerAd";
    private static final String ATTR_DEFAULT_PARAMS_KEY = "value";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        BaseBannerAdInstance bannerAdInstance =
                (BaseBannerAdInstance) InstanceManager.getInstance()
                        .getInstance(request.getInstanceId());
        if (bannerAdInstance == null) {
            return new Response(Response.CODE_SERVICE_UNAVAILABLE, "no such bannerAd instance");
        }
        String action = request.getAction();
        switch (action) {
            case ACTION_SHOW:
                bannerAdInstance.show(request.getCallback());
                break;
            case ACTION_HIDE:
                bannerAdInstance.hide(request.getCallback());
                break;
            case ACTION_DESTROY:
                bannerAdInstance.destroy();
                break;
            case ACTION_ON_LOAD:
            case ACTION_ON_CLOSE:
            case ACTION_ON_ERROR:
            case ACTION_ON_RESIZE:
                bannerAdInstance.addListener(request);
                break;
            case ACTION_OFF_LOAD:
            case ACTION_OFF_CLOSE:
            case ACTION_OFF_ERROR:
            case ACTION_OFF_RESIZE:
                bannerAdInstance.removeListener(request);
                break;
            case ATTR_GET_STYLE:
                return getStyle(request, bannerAdInstance);
            case ATTR_SET_STYLE:
                setStyle(request, bannerAdInstance);
                break;
            default:
                break;
        }
        return Response.SUCCESS;
    }

    protected void setStyle(Request request, BaseBannerAdInstance bannerAdInstance)
            throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        JSONObject styleParams = jsonParams.optJSONObject(ATTR_DEFAULT_PARAMS_KEY);
        if (styleParams == null) {
            Log.w(TAG, "setStyle: style can not be empty");
            return;
        }

        int designWidth = request.getHapEngine().getDesignWidth();
        BaseBannerAdInstance.Style style =
                BaseBannerAdInstance.jsonToStyle(styleParams, designWidth);
        bannerAdInstance.setStyle(style);
    }

    protected Response getStyle(Request request, BaseBannerAdInstance bannerAdInstance)
            throws JSONException {
        BaseBannerAdInstance.Style style = bannerAdInstance.getStyle();
        if (style == null) {
            return null;
        }
        int designWidth = request.getHapEngine().getDesignWidth();
        return new Response(style.toJSON(designWidth));
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
