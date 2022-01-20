/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.ExtensionBridge;
import org.hapjs.bridge.ExtensionMetaData;
import org.hapjs.bridge.MetaDataSet;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.runtime.CardConfig;
import org.json.JSONArray;
import org.json.JSONException;

public class ModuleBridge extends ExtensionBridge {
    static final Map<String, ExtensionMetaData> MODULE_MAP = new HashMap<>();
    private static final String TAG = "ModuleBridge";
    static boolean sCardModeEnabled = false;
    private RootView mRootView;
    private PageManager mPageManager;
    private AppInfo mAppInfo;

    public ModuleBridge(ClassLoader loader) {
        super(loader);
    }

    public static Map<String, ExtensionMetaData> getModuleMap() {
        if (MODULE_MAP.isEmpty()) {
            MODULE_MAP.putAll(MetaDataSet.getInstance().getModuleMetaDataMap());
        }
        return MODULE_MAP;
    }

    public static String getModuleMapJSONString() {
        return MetaDataSet.getInstance().getModuleMetaDataJSONString(sCardModeEnabled);
    }

    public static void configCardBlacklist() {
        sCardModeEnabled = true;
        Map<String, CardConfig.FeatureBlacklistItem> blacklistMap =
                CardConfig.getInstance().getFeatureBlacklistMap();
        for (String name : blacklistMap.keySet()) {
            if (!MODULE_MAP.containsKey(name)) {
                continue;
            }
            CardConfig.FeatureBlacklistItem blacklistItem = blacklistMap.get(name);
            if (blacklistItem == null) {
                Log.e(TAG,
                        "static initializer: value blacklistItem is null of which key is " + name);
            } else {
                List<String> blacklistMethods = blacklistItem.methods;
                if (blacklistMethods != null && !blacklistMethods.isEmpty()) {
                    ExtensionMetaData mataData = MODULE_MAP.get(name);
                    if (mataData != null) {
                        mataData.removeMethods(blacklistMethods);
                    }
                } else {
                    // No methods means remove feature completely
                    MODULE_MAP.remove(name);
                }
            }
        }
    }

    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mRootView = rootView;
        mPageManager = pageManager;
        mAppInfo = appInfo;
    }

    @Override
    protected ExtensionMetaData getExtensionMetaData(String module) {
        return getModuleMap().get(module);
    }

    @Override
    protected AbstractExtension createExtension(
            ClassLoader classLoader, ExtensionMetaData extensionMetaData) {
        AbstractExtension extension = super.createExtension(classLoader, extensionMetaData);
        if (extension instanceof ModuleExtension) {
            ((ModuleExtension) extension).attach(mRootView, mPageManager, mAppInfo);
        }
        return extension;
    }

    public JSONArray toJSON() {
        try {
            JSONArray bridgesJSON = new JSONArray();
            for (ExtensionMetaData extensionMetaData : getModuleMap().values()) {
                bridgesJSON.put(extensionMetaData.toJSON());
            }
            return bridgesJSON;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
