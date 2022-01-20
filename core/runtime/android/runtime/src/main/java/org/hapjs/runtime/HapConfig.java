/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.FeatureAliasRule;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.model.FeatureInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HapConfig {
    private static final String TAG = "HapConfig";

    private static final String SYSTEM_CONFIG_NAME = "hap.json";

    private static final String KEY_FEATURES = "features";
    private static final String KEY_FEATURE_ALIAS = "featureAlias";
    private static final String KEY_NAME = "name";
    private static final String KEY_TARGET = "target";
    private static final String KEY_REGEX = "regex";
    private List<FeatureInfo> mFeatureInfos;
    private List<FeatureAliasRule> mFeatureAliasRules;

    private HapConfig() {
        mFeatureAliasRules = new ArrayList<>();
        mFeatureInfos = new ArrayList<>();
    }

    public static HapConfig getInstance() {
        return Holder.INSTANCE;
    }

    private static HapConfig loadSystemConfig(Context context) {
        try {
            InputStream in = context.getResources().getAssets().open(SYSTEM_CONFIG_NAME);
            String text = FileUtils.readStreamAsString(in, true);
            JSONObject data = new JSONObject(text);
            return HapConfig.parse(data);
        } catch (IOException e) {
            Log.e(TAG, "fail to load system config", e);
        } catch (JSONException e) {
            Log.e(TAG, "fail to load system config", e);
        }

        return new HapConfig();
    }

    private static HapConfig parse(JSONObject data) {
        try {
            HapConfig config = new HapConfig();
            JSONArray featureInfosJSON = data.optJSONArray(KEY_FEATURES);
            if (featureInfosJSON != null) {
                config.mFeatureInfos = FeatureInfo.parse(featureInfosJSON);
            }
            JSONArray featureAliasesJSON = data.optJSONArray(KEY_FEATURE_ALIAS);
            if (featureAliasesJSON != null) {
                parseFeatureAlias(config, featureAliasesJSON);
            }
            return config;
        } catch (JSONException e) {
            Log.e(TAG, "Fail to parse config", e);
            return null;
        }
    }

    private static void parseFeatureAlias(HapConfig config, JSONArray featureAliasesJSON)
            throws JSONException {
        for (int i = 0; i < featureAliasesJSON.length(); i++) {
            JSONObject featureAliasJSON = featureAliasesJSON.getJSONObject(i);
            String name = featureAliasJSON.getString(KEY_NAME);
            String target = featureAliasJSON.getString(KEY_TARGET);
            boolean regex = featureAliasJSON.optBoolean(KEY_REGEX, false);
            FeatureAliasRule featureAliasRule = new FeatureAliasRule(name, target, regex);
            config.mFeatureAliasRules.add(featureAliasRule);
        }
    }

    public List<FeatureInfo> getFeatureInfos() {
        return mFeatureInfos;
    }

    public List<FeatureAliasRule> getFeatureAliasRules() {
        return mFeatureAliasRules;
    }

    public boolean isFeatureConfiged(String feature) {
        if (mFeatureInfos != null) {
            for (FeatureInfo info : mFeatureInfos) {
                if (feature.equals(info.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class Holder {
        static HapConfig INSTANCE = loadSystemConfig(Runtime.getInstance().getContext());
    }
}
