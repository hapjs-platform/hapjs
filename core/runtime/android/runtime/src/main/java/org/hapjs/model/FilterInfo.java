/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.hapjs.bridge.HybridRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FilterInfo {
    private static final String TAG = "FilterInfo";

    private static final String KEY_URI = "uri";
    private String mAction;
    private List<ActionFilter> mFilters;

    private FilterInfo(String action) {
        mAction = action;
        mFilters = new ArrayList<>();
    }

    public static Map<String, FilterInfo> parse(JSONObject filterJSON) throws JSONException {
        Map<String, FilterInfo> filterMap = new HashMap<>();
        Iterator<String> keyIterator = filterJSON.keys();
        while (keyIterator.hasNext()) {
            String action = keyIterator.next();
            Object filterObject = filterJSON.get(action);
            FilterInfo filterInfo = parseFilterInfo(action, filterObject);
            filterMap.put(action, filterInfo);
        }
        return filterMap;
    }

    private static FilterInfo parseFilterInfo(String action, Object filterObject)
            throws JSONException {
        FilterInfo filterInfo = new FilterInfo(action);
        if (filterObject instanceof JSONArray) {
            JSONArray filterList = (JSONArray) filterObject;
            for (int i = 0; i < filterList.length(); ++i) {
                JSONObject filterJSON = filterList.getJSONObject(i);
                addActionFilter(filterInfo, filterJSON);
            }
        } else if (filterObject instanceof JSONObject) {
            addActionFilter(filterInfo, (JSONObject) filterObject);
        } else {
            Log.e(TAG, "Fail to parse filterObject");
        }
        return filterInfo;
    }

    private static void addActionFilter(FilterInfo filterInfo, JSONObject filterJSON) {
        ActionFilter actionFilter = parseActionFilter(filterJSON);
        if (actionFilter != null) {
            filterInfo.mFilters.add(actionFilter);
        }
    }

    private static ActionFilter parseActionFilter(JSONObject filterJSON) {
        String uriPatternText = filterJSON.optString(KEY_URI);
        if (uriPatternText != null) {
            return ActionFilter.create(uriPatternText);
        }
        return null;
    }

    public boolean match(HybridRequest request) {
        if (TextUtils.equals(mAction, request.getAction())) {
            for (ActionFilter filter : mFilters) {
                if (filter.match(request)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class ActionFilter {
        private final Pattern uriPattern;

        private ActionFilter(Pattern uriPattern) {
            this.uriPattern = uriPattern;
        }

        public static ActionFilter create(String uriPatternText) {
            Pattern uriPattern = Pattern.compile(uriPatternText);
            return new ActionFilter(uriPattern);
        }

        public boolean match(HybridRequest request) {
            String uri = request.getUri();
            return uri != null && uriPattern.matcher(uri).find();
        }
    }
}
