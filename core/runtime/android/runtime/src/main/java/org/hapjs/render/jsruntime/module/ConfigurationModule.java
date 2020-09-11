/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.content.res.Configuration;
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
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = ConfigurationModule.NAME,
        actions = {
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_LOCALE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_SET_LOCALE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_GET_THEME_MODE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ConfigurationModule.ACTION_SET_GRAY_MODE, mode = Extension.Mode.SYNC)
        })
public class ConfigurationModule extends ModuleExtension {

    protected static final String NAME = "system.configuration";
    protected static final String ACTION_GET_LOCALE = "getLocale";
    protected static final String ACTION_SET_LOCALE = "setLocale";
    protected static final String ACTION_GET_THEME_MODE = "getThemeMode";
    protected static final String ACTION_SET_GRAY_MODE = "setGrayMode";

    private static final String PARAM_LANG = "language";
    private static final String PARAM_COUNTRY_REGION = "countryOrRegion";

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
        }
        return null;
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
