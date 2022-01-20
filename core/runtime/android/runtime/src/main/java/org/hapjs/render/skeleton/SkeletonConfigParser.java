/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.skeleton;

import android.text.TextUtils;
import android.util.Log;
import org.hapjs.render.Page;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Parse the skeleton screen config file(page-skeleton mapping) One-to-one mapping format: {
 * "singleMap":{ "pageName":"skFileName.sk", // "页面名称":"sk文件" ... } } One-to-many mapping format
 * (Higher resolution priority): { "anchorMaps":[ { "page":"pageName", "anchor":"paramsName", //
 * name of the parameter being observed "skeletonMap":{ "value1":"value1.sk", // possible parameter
 * value "value2":"value2.sk", "else":"else.sk", // other valid parameter values "null":"null.sk" //
 * does not include the observed parameter } }, {} ... ] }
 */
public class SkeletonConfigParser {
    private static final String TAG = "SkeletonConfigParser";

    /**
     * Matching rules: If one-to-many mapping configuration is included, it will be preferred
     */
    public static String getSkeletonFileName(Page page, String configContent) {
        if (page == null || TextUtils.isEmpty(configContent)) {
            return null;
        }
        try {
            JSONObject configObject = new JSONObject(configContent);
            // One to many
            if (configObject.has("anchorMaps")) {
                JSONArray anchorMapsArray = configObject.getJSONArray("anchorMaps");
                for (int i = 0; i < anchorMapsArray.length(); i++) {
                    JSONObject anchorMapObj = anchorMapsArray.getJSONObject(i);
                    if (anchorMapObj.has("page")
                            && anchorMapObj.has("anchor")
                            && anchorMapObj.has("skeletonMap")) {
                        String pageName = anchorMapObj.getString("page");
                        if (!TextUtils.isEmpty(pageName) && pageName.equals(page.getName())) {
                            String anchorName = anchorMapObj.getString("anchor");
                            if (!TextUtils.isEmpty(anchorName)) {
                                JSONObject skeletonMap = anchorMapObj.getJSONObject("skeletonMap");
                                if (page.params != null) {
                                    if (page.params.containsKey(anchorName)) {
                                        String paramsValue = (String) page.params.get(anchorName);
                                        if (skeletonMap.has(paramsValue)) {
                                            return skeletonMap.getString(paramsValue);
                                        } else if (skeletonMap.has("else")) {
                                            return skeletonMap.getString("else");
                                        }
                                    } else {
                                        if (skeletonMap.has("null")) {
                                            return skeletonMap.getString("null");
                                        }
                                    }
                                }
                            }
                            return null;
                        }
                    } else {
                        Log.e(TAG,
                                "LOG_SKELETON，Illegal skeleton screen config configuration: one to many");
                    }
                }
            }
            // One to one
            if (configObject.has("singleMap")) {
                JSONObject singleMap = configObject.getJSONObject("singleMap");
                if (singleMap.has(page.getName())) {
                    return singleMap.getString(page.getName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "LOG_SKELETON fail to get skeleton file name from config: ", e);
        }
        return null;
    }
}
