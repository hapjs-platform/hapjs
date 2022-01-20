/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.text.TextUtils;
import android.util.Log;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hapjs.distribution.AppDistributionMeta;
import org.hapjs.model.SubpackageInfo;

public class PackageFilesValidator {

    private static final String TAG = "PackageFilesValidator";
    // pkg - AppDistributionMeta
    private static ConcurrentMap<String, AppDistributionMeta> sAppDistributionMetas =
            new ConcurrentHashMap<>();
    // pkg-subpackage - Files
    private static ConcurrentMap<String, Set<String>> sInstallingAppFiles =
            new ConcurrentHashMap<>();

    public static void addAppDistributionMeta(String pkg, AppDistributionMeta meta) {
        sAppDistributionMetas.put(pkg, meta);
    }

    public static void addAppFiles(String pkg, String subpackage, Set<String> files) {
        sInstallingAppFiles.put(getAppKey(pkg, subpackage), files);
    }

    public static boolean isInvalidResource(String pkg, String resPath) {
        return isInvalidResource(pkg, null, resPath);
    }

    /**
     * 校验加载的资源是否为无效资源
     *
     * @param pkg        应用包名
     * @param subpackage 分包包名
     * @param resPath    资源地址。eg: /common/logo.png
     * @return
     */
    public static boolean isInvalidResource(String pkg, String subpackage, String resPath) {
        if (TextUtils.isEmpty(resPath)) {
            return true;
        }
        AppDistributionMeta meta = sAppDistributionMetas.get(pkg);
        if (meta == null) {
            Log.w(TAG, "isInvalidResource: AppDistributionMeta is null");
            return false;
        }
        resPath = resPath.startsWith("/") ? resPath.substring(1) : resPath;
        if (TextUtils.isEmpty(subpackage)) {
            List<SubpackageInfo> subpackageInfos = meta.getSubpackageInfos();
            if (subpackageInfos == null || subpackageInfos.isEmpty()) { // 非分包安装
                return isInvalidResourceInternal(pkg, null, resPath);
            } else { // 分包
                List<SubpackageInfo> needUpdateSubpackages = meta.getNeedUpdateSubpackages();
                SubpackageInfo base = null;
                for (SubpackageInfo subpackageInfo : subpackageInfos) {
                    if (subpackageInfo.isBase()) { // base 包无法通过 resource 去校验
                        base = subpackageInfo;
                    } else if (subpackageInfo.containResource(resPath)) { // 检查 resource 在哪个分包中
                        boolean needUpdate = false;
                        for (SubpackageInfo needUpdateSubpackage : needUpdateSubpackages) {
                            if (needUpdateSubpackage.getName().equals(subpackageInfo.getName())) {
                                needUpdate = true;
                                break;
                            }
                        }
                        if (needUpdate) { // 检查分包是否已经安装
                            return isInvalidResourceInternal(pkg, subpackageInfo.getName(),
                                    resPath);
                        } else { // 已安装时直接认为文件无效
                            return true;
                        }
                    }
                }
                if (base != null) {
                    return isInvalidResourceInternal(pkg, base.getName(), resPath);
                }
            }
        } else { // 某个分包
            SubpackageInfo subpackageInfo = meta.getSubpackageInfo(subpackage);
            if (subpackageInfo != null) {
                if (subpackageInfo.isBase() || subpackageInfo.containResource(resPath)) {
                    return isInvalidResourceInternal(pkg, subpackage, resPath);
                }
            }
        }
        return false;
    }

    private static boolean isInvalidResourceInternal(String pkg, String subpackage,
                                                     String resPath) {
        Set<String> files = sInstallingAppFiles.get(getAppKey(pkg, subpackage));
        return files != null && !files.contains(resPath);
    }

    public static void clear(String pkg) {
        sAppDistributionMetas.remove(pkg);
        Set<String> keys = sInstallingAppFiles.keySet();
        if (keys != null && keys.isEmpty()) {
            for (String key : keys) {
                if (key.equals(pkg) || key.startsWith(pkg + "-")) {
                    sInstallingAppFiles.remove(key);
                }
            }
        }
    }

    private static String getAppKey(String pkg, String subpackage) {
        if (TextUtils.isEmpty(subpackage)) {
            return pkg;
        } else {
            return pkg + "-" + subpackage;
        }
    }
}
