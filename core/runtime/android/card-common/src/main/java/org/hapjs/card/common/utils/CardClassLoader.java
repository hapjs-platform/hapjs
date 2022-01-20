/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.common.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CardClassLoader extends DexClassLoader {
    private static final String TAG = "CardClassLoader";

    private PackageLoadRulesHelper mPackageLoadRulesHelper;

    public CardClassLoader(
            Context context,
            String platform,
            String dexPath,
            String optimizedDirectory,
            String librarySearchPath,
            ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);

        mPackageLoadRulesHelper = new PackageLoadRulesHelper(context, platform);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz == null) {
            if (mPackageLoadRulesHelper.isOurPackage(name)) {
                try {
                    clazz = findClass(name);
                } catch (Exception ex) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }
            }
            if (clazz == null) {
                clazz = super.loadClass(name);
            }
        }
        return clazz;
    }

    private static class PackageLoadRulesHelper {
        private static final String PACKAGE_LOAD_RULES = "hap/package_load_rules.json";
        private static final String KEY_PLATFORM_PREFERRED_PKGS = "platformPreferredPkgs";
        private static final String KEY_HOST_PREFERRED_PKGS = "hostPreferredPkgs";
        private static final String KEY_DEFAULT = "default";
        private static final String PLATFORM = "platform";

        private Map<String, List<String>> mRulesMap = new HashMap<>();
        private boolean mPlatformByDefault = true;

        public PackageLoadRulesHelper(Context context, String platformPkg) {
            try {
                Context platformContext = context.createPackageContext(platformPkg, 0);
                boolean result = initRules(platformContext);
                if (result) {
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "failed to update rules", e);
            }

            initRules(context);
        }

        private boolean initRules(Context context) {
            try {
                InputStream in = context.getResources().getAssets().open(PACKAGE_LOAD_RULES);
                String text = FileUtils.readStreamAsString(in, true);
                JSONObject jsonObject = new JSONObject(text);

                JSONArray platformPreferredPkgs =
                        jsonObject.optJSONArray(KEY_PLATFORM_PREFERRED_PKGS);
                if (platformPreferredPkgs != null) {
                    ArrayList<String> list = new ArrayList<>(platformPreferredPkgs.length());
                    for (int i = 0; i < platformPreferredPkgs.length(); i++) {
                        list.add(platformPreferredPkgs.getString(i));
                    }
                    mRulesMap.put(KEY_PLATFORM_PREFERRED_PKGS, list);
                }

                JSONArray hostPreferredPkgs = jsonObject.optJSONArray(KEY_HOST_PREFERRED_PKGS);
                if (hostPreferredPkgs != null) {
                    ArrayList<String> list = new ArrayList<>(hostPreferredPkgs.length());
                    for (int i = 0; i < hostPreferredPkgs.length(); i++) {
                        list.add(hostPreferredPkgs.getString(i));
                    }
                    mRulesMap.put(KEY_HOST_PREFERRED_PKGS, list);
                }

                mPlatformByDefault =
                        TextUtils.equals(PLATFORM, jsonObject.optString(KEY_DEFAULT, PLATFORM));
                return true;
            } catch (IOException | JSONException e) {
                Log.e(TAG, "fail to init rules", e);
                return false;
            }
        }

        private boolean isOurPackage(String name) {
            List<String> platformPreferredPkgs = mRulesMap.get(KEY_PLATFORM_PREFERRED_PKGS);
            if (platformPreferredPkgs != null) {
                for (String pkg : platformPreferredPkgs) {
                    if (name.startsWith(pkg)) {
                        return true;
                    }
                }
            }

            List<String> hostPreferredPkgs = mRulesMap.get(KEY_HOST_PREFERRED_PKGS);
            if (hostPreferredPkgs != null) {
                for (String pkg : hostPreferredPkgs) {
                    if (name.startsWith(pkg)) {
                        return false;
                    }
                }
            }

            return mPlatformByDefault;
        }
    }
}
