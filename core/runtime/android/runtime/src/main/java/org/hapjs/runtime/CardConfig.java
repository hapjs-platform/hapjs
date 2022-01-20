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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.common.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CardConfig {
    private static final String TAG = "CardConfig";

    private static final String CARD_CONFIG_NAME = "hap/card.json";

    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_NAME = "name";
    private static final String KEY_FEATURE_BLACKLIST = "featureBlacklist";
    private static final String KEY_COMPONENT_BLACKLIST = "componentBlacklist";
    private static final String KEY_METHODS = "methods";
    private static final String KEY_TYPES = "types";
    private String mPlatform;
    private Map<String, ComponentBlacklistItem> mComponentBlacklist;
    private Map<String, FeatureBlacklistItem> mFeatureBlacklist;

    private CardConfig(
            Map<String, ComponentBlacklistItem> componentBlacklist,
            Map<String, FeatureBlacklistItem> featureBlacklist,
            String platform) {
        mComponentBlacklist = componentBlacklist;
        mFeatureBlacklist = featureBlacklist;
        mPlatform = platform;
    }

    public static CardConfig getInstance() {
        return Holder.INSTANCE;
    }

    private static CardConfig loadCardConfig(Context context) {
        try {
            InputStream in = context.getResources().getAssets().open(CARD_CONFIG_NAME);
            String text = FileUtils.readStreamAsString(in, true);
            JSONObject data = new JSONObject(text);
            return CardConfig.parse(data);
        } catch (IOException e) {
            Log.e(TAG, "fail to load system config", e);
        } catch (JSONException e) {
            Log.e(TAG, "fail to load system config", e);
        }

        return new CardConfig(Collections.EMPTY_MAP, Collections.EMPTY_MAP, null);
    }

    private static CardConfig parse(JSONObject data) throws JSONException {
        String platform = data.optString(KEY_PLATFORM);
        Map<String, ComponentBlacklistItem> componentBlacklistMap = new HashMap<>();
        JSONArray componentBlacklistJSON = data.optJSONArray(KEY_COMPONENT_BLACKLIST);
        if (componentBlacklistJSON != null) {
            for (int i = 0; i < componentBlacklistJSON.length(); i++) {
                JSONObject object = componentBlacklistJSON.optJSONObject(i);
                String component = object.getString(KEY_NAME);
                JSONArray methodArray = object.optJSONArray(KEY_METHODS);
                List<String> methods = jsonArrayToList(methodArray);
                JSONArray typeArray = object.optJSONArray(KEY_TYPES);
                List<String> types = jsonArrayToList(typeArray);
                componentBlacklistMap
                        .put(component, new ComponentBlacklistItem(component, types, methods));
            }
        }

        Map<String, FeatureBlacklistItem> featureBlacklistMap = new HashMap<>();
        JSONArray featureBlacklistJSON = data.optJSONArray(KEY_FEATURE_BLACKLIST);
        if (featureBlacklistJSON != null) {
            for (int i = 0; i < featureBlacklistJSON.length(); i++) {
                JSONObject object = featureBlacklistJSON.optJSONObject(i);
                String feature = object.getString(KEY_NAME);
                JSONArray methodArray = object.optJSONArray(KEY_METHODS);
                List<String> methods = jsonArrayToList(methodArray);
                featureBlacklistMap.put(feature, new FeatureBlacklistItem(feature, methods));
            }
        }

        return new CardConfig(componentBlacklistMap, featureBlacklistMap, platform);
    }

    private static List<String> jsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<String> list = null;
        if (jsonArray != null) {
            list = new ArrayList<>();
            for (int j = 0; j < jsonArray.length(); j++) {
                list.add(jsonArray.getString(j));
            }
        }
        return list;
    }

    public Map<String, ComponentBlacklistItem> getComponentBlacklistMap() {
        return mComponentBlacklist;
    }

    public Map<String, FeatureBlacklistItem> getFeatureBlacklistMap() {
        return mFeatureBlacklist;
    }

    public String getPlatform() {
        return mPlatform;
    }

    private static class Holder {
        static CardConfig INSTANCE = loadCardConfig(Runtime.getInstance().getContext());
    }

    public static class ComponentBlacklistItem {
        public String name;
        public List<String> types;
        public List<String> methods;

        public ComponentBlacklistItem(String name, List<String> types, List<String> methods) {
            this.name = name;
            this.types = types;
            this.methods = methods;
        }
    }

    public static class FeatureBlacklistItem {
        public String name;
        public List<String> methods;

        public FeatureBlacklistItem(String name, List<String> methods) {
            this.name = name;
            this.methods = methods;
        }
    }
}
