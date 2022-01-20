/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeatureInfo {
    private static final String TAG = "FeatureInfo";

    private static final String KEY_NAME = "name";
    private static final String KEY_PARAMS = "params";
    private String mName;
    private Map<String, String> mParams;

    private FeatureInfo(String name, Map<String, String> params) {
        mName = name;
        mParams = params;
    }

    public static List<FeatureInfo> parse(JSONArray featureInfosJSON) {
        List<FeatureInfo> featureInfos = new ArrayList<>();
        if (featureInfosJSON != null) {
            for (int i = 0; i < featureInfosJSON.length(); i++) {
                try {
                    JSONObject featureInfoJSON = featureInfosJSON.getJSONObject(i);
                    FeatureInfo featureInfo = parse(featureInfoJSON);
                    if (featureInfo != null) {
                        featureInfos.add(featureInfo);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Fail to parse feature info", e);
                }
            }
        }
        return featureInfos;
    }

    private static FeatureInfo parse(JSONObject featureJSON) {
        try {
            String name = featureJSON.getString(KEY_NAME);
            JSONObject paramsJSON = featureJSON.optJSONObject(KEY_PARAMS);
            Map<String, String> params = parseFeatureParams(paramsJSON);
            return new FeatureInfo(name, params);
        } catch (JSONException e) {
            Log.e(TAG, "Fail to parse feature info", e);
            return null;
        }
    }

    private static Map<String, String> parseFeatureParams(JSONObject paramsJSON)
            throws JSONException {
        Map<String, String> params = null;
        if (paramsJSON != null && paramsJSON.length() > 0) {
            params = new HashMap<>();
            Iterator<String> keys = paramsJSON.keys();
            while (keys.hasNext()) {
                String paramName = keys.next();
                String paramValue = paramsJSON.getString(paramName);
                params.put(paramName, paramValue);
            }
        }
        return params;
    }

    public String getName() {
        return mName;
    }

    public Map<String, String> getParams() {
        return mParams;
    }
}
