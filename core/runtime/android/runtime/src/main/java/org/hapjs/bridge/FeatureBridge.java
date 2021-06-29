/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.model.FeatureInfo;
import org.hapjs.runtime.CardConfig;
import org.hapjs.runtime.HapConfig;
import org.json.JSONArray;
import org.json.JSONException;

public class FeatureBridge extends ExtensionBridge {
    private static final String TAG = "FeatureBridge";

    private static boolean sCardModeEnabled = false;
    private ConcurrentHashMap<String, Map<String, String>> mFeatureParams;
    private Set<FeatureAliasRule> mFeatureAliasRules;

    public FeatureBridge(Context context, ClassLoader loader) {
        super(loader);
        mFeatureParams = new ConcurrentHashMap<>();
        mFeatureAliasRules =
                Collections.newSetFromMap(new ConcurrentHashMap<FeatureAliasRule, Boolean>());

        applyConfig();
        applyProxy();
    }

    private static class FeatureMapLoader {
        static final Map<String, ExtensionMetaData> FEATURE_MAP = new HashMap<>();

        static {
            FEATURE_MAP.putAll(MetaDataSet.getInstance().getFeatureMetaDataMap());
        }

        public static void configCardBlacklist() {
            Map<String, CardConfig.FeatureBlacklistItem> blacklist =
                    CardConfig.getInstance().getFeatureBlacklistMap();
            for (String name : blacklist.keySet()) {
                if (!FEATURE_MAP.containsKey(name)) {
                    continue;
                }
                CardConfig.FeatureBlacklistItem featureBlacklistItem = blacklist.get(name);
                if (featureBlacklistItem == null) {
                    FEATURE_MAP.remove(name);
                    continue;
                }
                List<String> blacklistMethods = featureBlacklistItem.methods;
                ExtensionMetaData extensionMetaData = FEATURE_MAP.get(name);
                if (extensionMetaData != null && blacklistMethods != null && !blacklistMethods.isEmpty()) {
                    extensionMetaData.removeMethods(blacklistMethods);
                } else {
                    // No methods means remove feature completely
                    FEATURE_MAP.remove(name);
                }
            }
        }
    }

    public static Map<String, ExtensionMetaData> getFeatureMap() {
        return FeatureMapLoader.FEATURE_MAP;
    }

    public static String getFeatureMapJSONString() {
        return MetaDataSet.getInstance().getFeatureMetaDataJSONString(sCardModeEnabled);
    }

    public static void configCardBlacklist() {
        sCardModeEnabled = true;
        FeatureMapLoader.configCardBlacklist();
    }

    public void addFeatures(List<FeatureInfo> featureInfos) {
        if (featureInfos != null) {
            for (FeatureInfo featureInfo : featureInfos) {
                String name = featureInfo.getName();
                Map<String, String> params = featureInfo.getParams();
                if (params != null) {
                    mFeatureParams.put(name, params);
                    FeatureExtension feature = (FeatureExtension) mExtensions.get(name);
                    if (feature != null) {
                        feature.setParams(params);
                    }
                }
            }
        }
    }

    @Override
    protected ExtensionMetaData getExtensionMetaData(String name) {
        ExtensionMetaData extensionMetaData = MetaDataSet.getInstance().getFeatureMetaData(name);
        if (extensionMetaData == null) {
            for (FeatureAliasRule featureAliasRule : mFeatureAliasRules) {
                String target = featureAliasRule.resolveAlias(name);
                if (target != null) {
                    extensionMetaData = MetaDataSet.getInstance().getFeatureMetaData(target);
                    break;
                }
            }
            if (extensionMetaData != null) {
                extensionMetaData = extensionMetaData.alias(name);
            }
        }
        return extensionMetaData;
    }

    @Override
    protected AbstractExtension createExtension(
            ClassLoader classLoader, ExtensionMetaData extensionMetaData) {
        AbstractExtension extension = super.createExtension(classLoader, extensionMetaData);
        if (extension instanceof FeatureExtension) {
            FeatureExtension feature = ((FeatureExtension) extension);
            feature.setParams(mFeatureParams.get(feature.getName()));
        }
        return extension;
    }

    public JSONArray toJSON() {
        try {
            Map<String, ExtensionMetaData> featureMap = getFeatureMap();
            JSONArray result = new JSONArray();
            for (ExtensionMetaData feature : featureMap.values()) {
                result.put(feature.toJSON());
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void dispose(boolean force) {
        for (AbstractExtension extension : mExtensions.values()) {
            ((FeatureExtension) extension).dispose(force);
        }
    }

    private void applyConfig() {
        HapConfig hapConfig = HapConfig.getInstance();
        addFeatures(hapConfig.getFeatureInfos());
        mFeatureAliasRules.addAll(hapConfig.getFeatureAliasRules());
    }

    private void applyProxy() {
        ExtensionProxyConfig.getInstance().configProxyMethod();
    }
}
