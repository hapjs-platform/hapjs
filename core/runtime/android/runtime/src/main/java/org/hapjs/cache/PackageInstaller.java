/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import java.io.File;
import org.hapjs.model.SubpackageInfo;

public abstract class PackageInstaller {

    protected String mPackageName;
    protected Context mContext;
    protected SubpackageInfo mSubpackageInfo;
    protected boolean mIsUpdate;

    public PackageInstaller(Context context, String pkg) {
        this.mContext = context.getApplicationContext();
        this.mPackageName = pkg;
    }

    public PackageInstaller(Context context, String packageName, boolean isUpdate) {
        mPackageName = packageName;
        mContext = context;
        mIsUpdate = isUpdate;
    }

    public PackageInstaller(
            Context context, String packageName, SubpackageInfo subpackageInfo, boolean isUpdate) {
        mPackageName = packageName;
        mContext = context;
        mSubpackageInfo = subpackageInfo;
        mIsUpdate = isUpdate;
    }

    private static File getTempResourceRootDir1(Context context) {
        File dir = new File(context.getCacheDir(), "temp_resource_1");
        dir.mkdirs();
        return dir;
    }

    private static File getTempResourceRootDir2(Context context) {
        File dir = new File(context.getCacheDir(), "temp_resource_2");
        dir.mkdirs();
        return dir;
    }

    protected static File getTempResourceDir1(Context context, String pkg) {
        return new File(getTempResourceRootDir1(context), pkg);
    }

    protected static File getTempResourceDir2(Context context, String pkg) {
        return new File(getTempResourceRootDir2(context), pkg);
    }

    public String getPackage() {
        return mPackageName;
    }

    public abstract int getVersionCode();

    public SubpackageInfo getSubpackageInfo() {
        return mSubpackageInfo;
    }

    public boolean isSubpackage() {
        return mSubpackageInfo != null;
    }

    public boolean isUpdate() {
        return mIsUpdate;
    }

    public void setUpdate(boolean update) {
        mIsUpdate = update;
    }

    public boolean isAllSuccess() {
        return true;
    }

    public abstract void install(File resourceDir, File signatureFile) throws CacheException;
}
