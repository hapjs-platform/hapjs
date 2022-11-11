/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = ConfigurationModule.NAME,
        actions = {
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_LOCALE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_SET_LOCALE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_THEME_MODE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_SET_GRAY_MODE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_FOLDABLE_STATE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_SCREEN_ORIENTATION, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_SHOWSIZE_LEVEL, mode = Extension.Mode.SYNC)
        })
public class ConfigurationModule extends ModuleExtension {

    protected static final String NAME = "system.configuration";
    protected static final String ACTION_GET_LOCALE = "getLocale";
    protected static final String ACTION_SET_LOCALE = "setLocale";
    protected static final String ACTION_GET_THEME_MODE = "getThemeMode";
    protected static final String ACTION_SET_GRAY_MODE = "setGrayMode";
    protected static final String ACTION_GET_FOLDABLE_STATE = "getFoldableState";
    protected static final String ACTION_GET_SCREEN_ORIENTATION = "getScreenOrientation";

    private static final String PARAM_LANG = "language";
    private static final String PARAM_COUNTRY_REGION = "countryOrRegion";
    private static final String CONFIGURATION_ORIENTATION_UNDEFINED = "undefined";
    private static final String CONFIGURATION_ORIENTATION_PORTRAIT = "portrait";
    private static final String CONFIGURATION_ORIENTATION_LANDSCAPE = "landscape";
    protected static final int FOLDABLE_SCREEN_EXPAND = 1;
    protected static final int FOLDABLE_SCREEN_COLLAPSE = 2;
    protected static final int FOLDABLE_SCREEN_UNKNOWN = 0;
    protected static final int FOLDABLE_SCREEN_HALF_COLLAPSE = 3;
    protected static final String ACTION_GET_SHOWSIZE_LEVEL = "getShowLevel";

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_LOCALE.equals(action)) {
            return getLocale(request);
        } else if (ACTION_SET_LOCALE.equals(action)) {
            return setLocale(request);
        } else if (ACTION_GET_THEME_MODE.equals(action)) {
            return getThemeMode(request);
        } else if (ACTION_SET_GRAY_MODE.equals(action)) {
            return setGrayMode(request);
        } else if (ACTION_GET_SCREEN_ORIENTATION.equals(action)) {
            return getScreenOrientation(request);
        } else if (ACTION_GET_FOLDABLE_STATE.equals(action)) {
            return getFoldableState(request);
        } else if (ACTION_GET_SHOWSIZE_LEVEL.equals(action)) {
            return getShowLevel(request);
        }
        return null;
    }

    private Response getScreenOrientation(Request request) {
        Activity activity = request.getNativeInterface().getActivity();
        Resources resources = activity.getResources();
        if (activity == null) {
            return new Response(CONFIGURATION_ORIENTATION_UNDEFINED);
        }
        Configuration configuration = resources.getConfiguration();
        if (configuration == null) {
            return new Response(CONFIGURATION_ORIENTATION_UNDEFINED);
        }
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            return new Response(CONFIGURATION_ORIENTATION_PORTRAIT);
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return new Response(CONFIGURATION_ORIENTATION_LANDSCAPE);
        } else {
            return new Response(CONFIGURATION_ORIENTATION_UNDEFINED);
        }
    }

    protected Response getShowLevel(Request request) {
        boolean isDefaultSysShowSize = true;
        float showSizeLevel = 1.0f;
        Context context = request.getApplicationContext().getContext();
        if (null == context) {
            return new Response(Response.CODE_SERVICE_UNAVAILABLE, showSizeLevel + "");
        }
        SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        if (null != sysOpProvider) {
            isDefaultSysShowSize = !sysOpProvider.isSysShowLevelChange(context);
        }
        if (isDefaultSysShowSize) {
            return new Response(Response.CODE_SUCCESS, showSizeLevel + "");
        }
        if (null != sysOpProvider) {
            showSizeLevel = sysOpProvider.getScaleShowLevel(context);
        }
        return new Response(Response.CODE_SUCCESS, showSizeLevel + "");
    }

    private Response getThemeMode(Request request) {
        return new Response(Response.CODE_SUCCESS, DarkThemeUtil.isDarkMode() ? 1 : 0);
    }

    private Response getLocale(Request request) throws JSONException {
        Locale currentLocale = ConfigurationManager.getInstance().getCurrentLocale();
        if (currentLocale != null) {
            JSONObject locale = new JSONObject();
            locale.put(PARAM_LANG, currentLocale.getLanguage());
            locale.put(PARAM_COUNTRY_REGION, currentLocale.getCountry());
            return new Response(locale);
        } else {
            return Response.ERROR;
        }
    }

    protected Response getFoldableState(Request request) {
        return new Response(FOLDABLE_SCREEN_UNKNOWN);
    }

    private Response setLocale(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        Locale locale =
                new Locale(params.optString(PARAM_LANG), params.optString(PARAM_COUNTRY_REGION));
        Configuration configuration =
                request.getNativeInterface().getActivity().getResources().getConfiguration();
        configuration.setLocale(locale);
        ConfigurationManager.getInstance()
                .update(request.getNativeInterface().getActivity(), configuration);
        return Response.SUCCESS;
    }

    private Response setGrayMode(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        String pkg = request.getHapEngine().getPackage();
        GrayModeManager grayModeManager = GrayModeManager.getInstance();
        grayModeManager.recordGrayModeConfig(request.getApplicationContext().getContext(), params, request.getHapEngine().getVersionCode());
        if (grayModeManager.isNeedRecreate()) {
            Executors.ui().execute(() -> request.getNativeInterface().getActivity().recreate());
            grayModeManager.setNeedRecreate(false);
        }
        return Response.SUCCESS;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
