/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.model.AppInfo;
import org.hapjs.model.ConfigInfo;
import org.hapjs.runtime.resource.ResourceManager;
import org.hapjs.runtime.resource.ResourceManagerFactory;
import org.hapjs.system.SysOpProvider;

public class HapEngine {
    private static final String TAG = "HapEngine";
    private static final ConcurrentHashMap<String, HapEngine> sEngines = new ConcurrentHashMap();
    private Context mContext;
    private String mPackage;
    private Mode mMode;
    private ApplicationContext mApplicationContext;
    private ResourceManager mResourceManager;

    private HapEngine(Context context, String pkg) {
        mContext = context;
        mPackage = pkg;
        mMode = Mode.APP;
    }

    public static HapEngine getInstance(String pkg) {
        HapEngine engine = sEngines.get(pkg);
        if (engine == null) {
            engine = new HapEngine(Runtime.getInstance().getContext(), pkg);
            HapEngine oldEngine = sEngines.putIfAbsent(pkg, engine);
            if (oldEngine != null) {
                engine = oldEngine;
            }
        }
        return engine;
    }

    public Context getContext() {
        return mContext;
    }

    public String getPackage() {
        return mPackage;
    }

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        mMode = mode;
    }

    public boolean isCardMode() {
        return mMode == Mode.CARD;
    }

    public boolean isInsetMode() {
        return mMode == Mode.INSET;
    }

    public ApplicationContext getApplicationContext() {
        if (mApplicationContext == null) {
            mApplicationContext = new ApplicationContext(mContext, mPackage);
        }
        return mApplicationContext;
    }

    public ResourceManager getResourceManager() {
        if (mResourceManager == null) {
            mResourceManager = ResourceManagerFactory.getResourceManager(mContext, mPackage);
        }
        return mResourceManager;
    }

    public int getDesignWidth() {
        AppInfo appInfo = getApplicationContext().getAppInfo();
        if (appInfo == null) return ConfigInfo.DEFAULT_DESIGN_WIDTH;

        SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        return provider.getDesignWidth(mContext, appInfo);
    }

    public int getMinPlatformVersion() {
        AppInfo appInfo = getApplicationContext().getAppInfo();
        if (appInfo != null) {
            return appInfo.getMinPlatformVersion();
        }
        return -1;
    }

    public int getVersionCode() {
        AppInfo appInfo = getApplicationContext().getAppInfo();
        if (appInfo != null) {
            return appInfo.getVersionCode();
        }
        return -1;
    }

    public static class Mode {

        public static final Mode APP = new Mode(0, "APP");
        public static final Mode CARD = new Mode(1, "CARD");
        public static final Mode INSET = new Mode(2, "INSET");

        int value;
        String name;

        public Mode(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int value() {
            return value;
        }

        public String name() {
            return name;
        }
    }
}
