/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import android.util.Log;

import org.hapjs.common.utils.IntegerUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrayModeConfig {
    private static final String TAG = "GrayModeConfig";

    private static final String PATTERN_REGULAR_DATE_STR = "(\\d{1,2})/(\\d{1,2})";
    private static final String PATTERN_TEMPORARY_DATE_STR = "(\\d{4})/(\\d{1,2})/(\\d{1,2})";

    private static final String KEY_GRAY_MODE_CONFIG_ENABLE = "enable";
    private static final String KEY_GRAY_MODE_CONFIG_DURATION = "duration";
    private static final String KEY_GRAY_MODE_CONFIG_DURATION_REGULAR = "regular";
    private static final String KEY_GRAY_MODE_CONFIG_DURATION_TEMPORARY = "temporary";
    private static final String KEY_GRAY_MODE_CONFIG_EXCLUDED_PAGES = "excludedPages";

    private boolean mEnable;
    private List<String> mRegularList;
    private List<String> mTemporaryList;
    private List<String> mExcludedPageList;

    private GrayModeConfig() {
    }

    public static GrayModeConfig fromJSONObject(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        GrayModeConfig grayModeConfig = new GrayModeConfig();
        grayModeConfig.mEnable = jsonObject.optBoolean(KEY_GRAY_MODE_CONFIG_ENABLE);
        JSONObject durationJsonObj = jsonObject.optJSONObject(KEY_GRAY_MODE_CONFIG_DURATION);
        if (durationJsonObj != null) {
            JSONArray regularJsonArray = durationJsonObj.optJSONArray(KEY_GRAY_MODE_CONFIG_DURATION_REGULAR);
            JSONArray temporaryJsonArray = durationJsonObj.optJSONArray(KEY_GRAY_MODE_CONFIG_DURATION_TEMPORARY);
            grayModeConfig.mRegularList = new ArrayList<>();
            if (regularJsonArray != null) {
                for (int i = 0; i < regularJsonArray.length(); i++) {
                    grayModeConfig.mRegularList.add(regularJsonArray.optString(i));
                }
            }
            grayModeConfig.mTemporaryList = new ArrayList<>();
            if (temporaryJsonArray != null) {
                for (int i = 0; i < temporaryJsonArray.length(); i++) {
                    grayModeConfig.mTemporaryList.add(temporaryJsonArray.optString(i));
                }
            }
        }
        JSONArray excludedPageJsonArray = jsonObject.optJSONArray(KEY_GRAY_MODE_CONFIG_EXCLUDED_PAGES);
        grayModeConfig.mExcludedPageList = new ArrayList<>();
        if (excludedPageJsonArray != null) {
            for (int i = 0; i < excludedPageJsonArray.length(); i++) {
                grayModeConfig.mExcludedPageList.add(excludedPageJsonArray.optString(i));
            }
        }
        return grayModeConfig;
    }

    public JSONObject toJsonObject() {
        try {
            JSONArray regularJsonArray = new JSONArray();
            for (int i = 0; i < mRegularList.size(); i++) {
                regularJsonArray.put(mRegularList.get(i));
            }
            JSONArray temporaryJsonArray = new JSONArray();
            for (int i = 0; i < mTemporaryList.size(); i++) {
                temporaryJsonArray.put(mTemporaryList.get(i));
            }
            JSONObject durationJsonObj = new JSONObject();
            durationJsonObj.put(KEY_GRAY_MODE_CONFIG_DURATION_REGULAR, regularJsonArray);
            durationJsonObj.put(KEY_GRAY_MODE_CONFIG_DURATION_TEMPORARY, temporaryJsonArray);

            JSONArray excludedPagesJsonArray = new JSONArray();
            for (int i = 0; i < mExcludedPageList.size(); i++) {
                excludedPagesJsonArray.put(mExcludedPageList.get(i));
            }

            JSONObject configJsonObj = new JSONObject();
            configJsonObj.put(KEY_GRAY_MODE_CONFIG_ENABLE, mEnable);
            configJsonObj.put(KEY_GRAY_MODE_CONFIG_DURATION, durationJsonObj);
            configJsonObj.put(KEY_GRAY_MODE_CONFIG_EXCLUDED_PAGES, excludedPagesJsonArray);

            return configJsonObj;
        } catch (JSONException e) {
            Log.e(TAG, "toJsonObject: JSONException.", e);
            return new JSONObject();
        }
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public boolean isBelongGrayModeDuration() {
        if (mRegularList != null) {
            for (String regularDate : mRegularList) {
                if (isBelongRegularDuration(regularDate)) {
                    return true;
                }
            }
        }
        if (mTemporaryList != null) {
            for (String temporaryDate : mTemporaryList) {
                if (isBelongTemporaryDate(temporaryDate)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPagePathExcluded(String pathPath) {
        if (mExcludedPageList == null || mExcludedPageList.isEmpty()) {
            return false;
        }
        for (int i = 0; i < mExcludedPageList.size(); i++) {
            String excludedPagePath = mExcludedPageList.get(i);
            if (TextUtils.equals(pathPath, excludedPagePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBelongRegularDuration(String regularDateStr) {
        if (TextUtils.isEmpty(regularDateStr)) {
            return false;
        }
        Pattern r = Pattern.compile(PATTERN_REGULAR_DATE_STR);
        Matcher m = r.matcher(regularDateStr);
        if (m.find()) {
            int month = IntegerUtil.parse(m.group(1));
            int dayOfMonth = IntegerUtil.parse(m.group(2));
            Calendar current = Calendar.getInstance();
            return current.get(Calendar.MONTH) + 1 == month
                    && current.get(Calendar.DAY_OF_MONTH) == dayOfMonth;
        }
        return false;
    }

    private boolean isBelongTemporaryDate(String temporaryDateStr) {
        if (TextUtils.isEmpty(temporaryDateStr)) {
            return false;
        }
        Pattern r = Pattern.compile(PATTERN_TEMPORARY_DATE_STR);
        Matcher m = r.matcher(temporaryDateStr);
        if (m.find()) {
            int year = IntegerUtil.parse(m.group(1));
            int month = IntegerUtil.parse(m.group(2));
            int dayOfMonth = IntegerUtil.parse(m.group(3));
            Calendar current = Calendar.getInstance();
            return current.get(Calendar.YEAR) == year
                    && current.get(Calendar.MONTH) + 1 == month
                    && current.get(Calendar.DAY_OF_MONTH) == dayOfMonth;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GrayModeConfig other = (GrayModeConfig) obj;
        return this.mEnable == other.mEnable
                && isContentEquals(this.mRegularList, other.mRegularList)
                && isContentEquals(this.mTemporaryList, other.mTemporaryList)
                && isContentEquals(this.mExcludedPageList, other.mExcludedPageList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEnable, mRegularList, mTemporaryList, mExcludedPageList);
    }

    private boolean isContentEquals(List<String> list1, List<String> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        String[] arr1 = list1.toArray(new String[]{});
        String[] arr2 = list2.toArray(new String[]{});
        Arrays.sort(arr1);
        Arrays.sort(arr1);
        return Arrays.equals(arr1,arr2);
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }
}
