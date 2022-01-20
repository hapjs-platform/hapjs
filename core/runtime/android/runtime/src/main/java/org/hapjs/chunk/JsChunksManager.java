/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.chunk;

import android.text.TextUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.common.executors.Executors;
import org.hapjs.io.RpkSource;
import org.hapjs.io.TextReader;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.render.Page;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.runtime.Runtime;

public class JsChunksManager {
    private static final String APP_CHUNKS_JSON = "app-chunks.json";
    private static final String PAGE_CHUNKS_JSON = "page-chunks.json";

    private ConcurrentHashMap<String, String> mJsChunksMap = new ConcurrentHashMap<>();
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
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(mAppInfo.getPackage(),
                                            "JsChunksManager#initialize");
                            putJsChunks(APP_CHUNKS_JSON, readJsChunks(APP_CHUNKS_JSON));
                            if (mSubpackageInfos == null || mSubpackageInfos.isEmpty()) {
                                putJsChunks(PAGE_CHUNKS_JSON, readJsChunks(PAGE_CHUNKS_JSON));
                            } else {
                                for (SubpackageInfo subpackageInfo : mSubpackageInfos) {
                                    String path =
                                            subpackageInfo.getResource() + "/" + PAGE_CHUNKS_JSON;
                                    putJsChunks(path, readJsChunks(path));
                                }
                            }
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(mAppInfo.getPackage(),
                                            "JsChunksManager#initialize");
                        });
    }

    private String getJsChunks(String path) {
        String jsonSource = mJsChunksMap.get(path);
        if (TextUtils.isEmpty(jsonSource)) {
            jsonSource = readJsChunks(path);
        }
        return jsonSource;
    }

    private String readJsChunks(String path) {
        RpkSource jsChunksSource =
                new RpkSource(Runtime.getInstance().getContext(), mAppInfo.getPackage(), path);
        return TextReader.get().read(jsChunksSource);
    }

    private void putJsChunks(String path, String jsChunks) {
        if (!TextUtils.isEmpty(jsChunks)) {
            mJsChunksMap.put(path, jsChunks);
        }
    }

    public void registerAppChunks() {
        String appChunks = getJsChunks(JsChunksManager.APP_CHUNKS_JSON);
        if (!TextUtils.isEmpty(appChunks)) {
            mJsThread.postRegisterBundleChunks(appChunks);
        }
    }

    public void registerPageChunks(Page page) {
        if (mSubpackageInfos == null || mSubpackageInfos.isEmpty()) {
            if (!mRegisteredSet.contains(mAppInfo.getName())) {
                String pageChunks = getJsChunks(PAGE_CHUNKS_JSON);
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
                    String pageChunks =
                            getJsChunks(subpackageInfo.getResource() + "/" + PAGE_CHUNKS_JSON);
                    if (!TextUtils.isEmpty(pageChunks)) {
                        mJsThread.postRegisterBundleChunks(pageChunks);
                        mRegisteredSet.add(subpackageInfo.getName());
                    }
                }
            }
        }
    }
}
