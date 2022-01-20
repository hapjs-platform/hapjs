/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.util.Log;
import java.util.List;
import org.hapjs.model.SubpackageInfo;

public class AppDistributionMeta {
    private static final String TAG = "AppDistributionMeta";

    private String mPackage;
    private int mVersion;
    private String mDownloadUrl;
    private List<SubpackageInfo> mSubpackageInfos;
    private List<SubpackageInfo> mNeedUpdateSubpackages;
    private long mTotalSize;

    public AppDistributionMeta(String pkg) {
        this(pkg, -1);
    }

    public AppDistributionMeta(String pkg, int version) {
        mPackage = pkg;
        mVersion = version;
    }

    public AppDistributionMeta(
            String pkg,
            int version,
            String downloadUrl,
            List<SubpackageInfo> subpackageInfos,
            List<SubpackageInfo> needUpdateSubpackages) {
        this(pkg, version, downloadUrl, subpackageInfos, needUpdateSubpackages, 0);
    }

    public AppDistributionMeta(
            String pkg,
            int version,
            String downloadUrl,
            List<SubpackageInfo> subpackageInfos,
            List<SubpackageInfo> needUpdateSubpackages,
            long size) {
        mPackage = pkg;
        mVersion = version;
        mDownloadUrl = downloadUrl;
        mSubpackageInfos = subpackageInfos;
        mNeedUpdateSubpackages = needUpdateSubpackages;
        mTotalSize = size;
    }

    public String getPackage() {
        return mPackage;
    }

    public int getVersion() {
        return mVersion;
    }

    public String getDownloadUrl(String subpackageName) {
        if (subpackageName == null) {
            return mDownloadUrl;
        }

        if (mSubpackageInfos != null) {
            for (SubpackageInfo info : mSubpackageInfos) {
                if (subpackageName.equals(info.getName())) {
                    return info.getSrc();
                }
            }
        }

        Log.w(TAG,
                "no subpackage info for package: " + mPackage + ", subpackage: " + subpackageName);

        return null;
    }

    public SubpackageInfo getSubpackageInfo(String subpackageName) {
        if (mSubpackageInfos != null) {
            for (SubpackageInfo info : mSubpackageInfos) {
                if (subpackageName.equals(info.getName())) {
                    return info;
                }
            }
        }
        return null;
    }

    public List<SubpackageInfo> getSubpackageInfos() {
        return mSubpackageInfos;
    }

    public List<SubpackageInfo> getNeedUpdateSubpackages() {
        return mNeedUpdateSubpackages;
    }

    public long getSize() {
        return mTotalSize;
    }

    @Override
    public String toString() {
        return "mPackage:"
                + mPackage
                + ", mVersion="
                + mVersion
                + ", mDownloadUrl="
                + mDownloadUrl
                + ", mSubpackageInfos:"
                + mSubpackageInfos
                + ", mNeedUpdateSubpackages:"
                + mNeedUpdateSubpackages
                + ", mTotalSize:"
                + mTotalSize;
    }
}
