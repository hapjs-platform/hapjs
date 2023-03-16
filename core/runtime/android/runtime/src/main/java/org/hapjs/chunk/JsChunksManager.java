/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.chunk;

import android.text.TextUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.render.Page;
import org.hapjs.render.AppResourcesLoader;
import org.hapjs.render.jsruntime.JsThread;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsChunksManager {
    private Set<String> mRegisteredSet = new HashSet<>();
    private List<SubpackageInfo> mSubpackageInfos;
    private JsThread mJsThread;
    private AppInfo mAppInfo;

    public JsChunksManager(JsThread jsThread) {
        mJsThread = jsThread;
    }

    public void initialize(AppInfo appInfo) {
        mAppInfo = appInfo;
        mSubpackageInfos = mAppInfo.getSubpackageInfos();
    }

    public void registerAppChunks() {
        String appChunks = AppResourcesLoader.getJsChunks(mAppInfo.getPackage(), AppResourcesLoader.APP_CHUNKS_JSON);
        if (!TextUtils.isEmpty(appChunks)) {
            mJsThread.postRegisterBundleChunks(appChunks);
        }
    }

    public void registerPageChunks(Page page) {
        if (mSubpackageInfos == null || mSubpackageInfos.isEmpty()) {
            if (!mRegisteredSet.contains(mAppInfo.getName())) {
                String pageChunks = AppResourcesLoader.getJsChunks(mAppInfo.getPackage(), AppResourcesLoader.PAGE_CHUNKS_JSON);
                if (!TextUtils.isEmpty(pageChunks)) {
                    mJsThread.postRegisterBundleChunks(pageChunks);
                    mRegisteredSet.add(mAppInfo.getName());
                }
            }
        } else {
            String path = page.getPath();
            for (SubpackageInfo subpackageInfo : mSubpackageInfos) {
                if (!mRegisteredSet.contains(subpackageInfo.getName())
                        && subpackageInfo.containPath(path)) {
                    String pageChunks = AppResourcesLoader.getJsChunks(mAppInfo.getPackage(),
                            subpackageInfo.getResource() + "/" + AppResourcesLoader.PAGE_CHUNKS_JSON);
                    if (!TextUtils.isEmpty(pageChunks)) {
                        mJsThread.postRegisterBundleChunks(pageChunks);
                        mRegisteredSet.add(subpackageInfo.getName());
                    }
                }
            }
        }
    }
}
