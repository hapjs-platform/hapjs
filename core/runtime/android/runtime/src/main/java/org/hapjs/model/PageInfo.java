/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.HybridRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class PageInfo implements RoutableInfo {
    public static final String MODE_STANDARD = "standard";
    public static final String MODE_SINGLE_TASK = "singleTask";
    public static final String FLAG_CLEAR_TASK = "clearTask";
    private static final String KEY_COMPONENT = "component";
    private static final String KEY_PATH = "path";
    private static final String KEY_FILTER = "filter";
    private static final String KEY_LAUNCH_MODE = "launchMode";
    private String mName;
    private String mPath;
    private String mUri;
    private String mComponent;
    private Map<String, FilterInfo> mFilterMap;
    private String mLaunchMode;

    public PageInfo(
            String name,
            String path,
            String uri,
            String component,
            Map<String, FilterInfo> filterMap,
            String launchMode) {
        mName = name;
        mPath = path;
        mUri = uri;
        mComponent = component;
        mFilterMap = filterMap;
        if (mFilterMap == null) {
            mFilterMap = new HashMap<>();
        }
        mLaunchMode = launchMode;
    }

    public static PageInfo parse(String name, JSONObject pageObject) {
        String path = pageObject.optString(KEY_PATH, "/" + name);
        String component;
        try {
            component = pageObject.getString(KEY_COMPONENT);
        } catch (JSONException e) {
            throw new IllegalStateException("Component can't be empty, name=" + name);
        }
        String uri = name + "/" + component + ".js";
        Map<String, FilterInfo> filterMap = null;
        JSONObject filterJSON = pageObject.optJSONObject(KEY_FILTER);
        if (filterJSON != null) {
            try {
                filterMap = FilterInfo.parse(filterJSON);
            } catch (JSONException e) {
                throw new IllegalStateException("Illegal filter settings", e);
            }
        }
        String launchMode = pageObject.optString(KEY_LAUNCH_MODE, MODE_STANDARD);
        switch (launchMode) {
            case MODE_SINGLE_TASK:
                break;
            case MODE_STANDARD:
                break;
            default:
                launchMode = MODE_STANDARD;
                break;
        }
        return new PageInfo(name, path, uri, component, filterMap, launchMode);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getPath() {
        return mPath;
    }

    @Override
    public String getUri() {
        return mUri;
    }

    @Override
    public String getComponent() {
        return mComponent;
    }

    @Override
    public String getLaunchMode() {
        return mLaunchMode;
    }

    public boolean match(HybridRequest request) {
        FilterInfo filterInfo = mFilterMap.get(request.getAction());
        return filterInfo != null && filterInfo.match(request);
    }
}
