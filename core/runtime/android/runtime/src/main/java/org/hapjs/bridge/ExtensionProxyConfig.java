/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.runtime.Runtime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExtensionProxyConfig {
    private static final String TAG = "ExtensionProxyConfig";

    private static final String PROXY_CONFIG_NAME = "proxy.json";

    private static final String KEY_FEATURES = "features";
    private static final String KEY_NAME = "name";
    private static final String KEY_METHODS = "methods";
    private static final String KEY_PROXY = "proxy";
    private static final String KEY_MODE = "mode";
    private Map<String, List<Method>> mProxyInfo = new HashMap<>();

    public static ExtensionProxyConfig getInstance() {
        return Holder.INSTANCE;
    }

    private static ExtensionProxyConfig loadProxyConfig(Context context) {
        try {
            InputStream in = context.getResources().getAssets().open(PROXY_CONFIG_NAME);
            String text = FileUtils.readStreamAsString(in, true);
            JSONObject data = new JSONObject(text);
            return ExtensionProxyConfig.parse(data);
        } catch (IOException e) {
            Log.e(TAG, "fail to load proxy config", e);
        } catch (JSONException e) {
            Log.e(TAG, "fail to load proxy config", e);
        }
        return new ExtensionProxyConfig();
    }

    public static ExtensionProxyConfig parse(JSONObject data) throws JSONException {
        ExtensionProxyConfig config = new ExtensionProxyConfig();
        Map<String, List<Method>> proxyInfo = new HashMap<>();
        JSONArray features = data.getJSONArray(KEY_FEATURES);
        for (int i = 0; i < features.length(); i++) {
            JSONObject featureJson = features.getJSONObject(i);
            String featureName = featureJson.getString(KEY_NAME);
            JSONArray methodsJsonArray = featureJson.getJSONArray(KEY_METHODS);
            for (int j = 0; j < methodsJsonArray.length(); j++) {
                JSONObject methodJson = methodsJsonArray.getJSONObject(j);
                Method method =
                        new Method(
                                methodJson.getString(KEY_NAME),
                                methodJson.getString(KEY_PROXY),
                                Extension.Mode.valueOf(methodJson
                                        .optString(KEY_MODE, Extension.Mode.SYNC.name())));
                List<Method> methodProxyList = proxyInfo.get(featureName);
                if (methodProxyList == null) {
                    methodProxyList = new ArrayList<>();
                    proxyInfo.put(featureName, methodProxyList);
                }
                methodProxyList.add(method);
            }
        }
        config.mProxyInfo = proxyInfo;
        Log.d(TAG, "parse extension proxy config:  " + proxyInfo);
        return config;
    }

    public String getProxyAction(String feature, String action) {
        List<Method> methods = mProxyInfo.get(feature);
        if (methods != null) {
            for (Method method : methods) {
                if (TextUtils.equals(action, method.name)) {
                    return method.proxyName;
                }
            }
        }
        return null;
    }

    public void configProxyMethod() {
        for (Map.Entry<String, List<Method>> featureEntry : mProxyInfo.entrySet()) {
            String featureName = featureEntry.getKey();
            ExtensionMetaData metaData = FeatureBridge.getFeatureMap().get(featureName);
            if (metaData != null) {
                for (Method method : featureEntry.getValue()) {
                    metaData.addProxyMethod(method.name, method.proxyName, method.mode);
                }
            }
        }
    }

    private static class Holder {
        static ExtensionProxyConfig INSTANCE = loadProxyConfig(Runtime.getInstance().getContext());
    }

    private static class Method {
        final String name;
        final String proxyName;
        final Extension.Mode mode;

        public Method(String name, String proxyName, Extension.Mode mode) {
            this.name = name;
            this.proxyName = proxyName;
            this.mode = mode;
        }

        @Override
        public String toString() {
            return "Method(" + "name=" + name + ", proxyName=" + proxyName + ", mode=" + mode + ")";
        }
    }
}
