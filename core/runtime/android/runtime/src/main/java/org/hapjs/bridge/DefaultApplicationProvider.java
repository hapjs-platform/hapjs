/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.WebViewUtils;

public class DefaultApplicationProvider implements ApplicationProvider {

    protected static final String DIR_DATABASE = "database";
    protected static final String DIR_SHARED_PREF = "pref";

    @Override
    public File getCacheDir(Context context, String pkg) {
        File dir = new File(context.getCacheDir(), pkg);
        return ensureDir(dir);
    }

    @Override
    public File getFilesDir(Context context, String pkg) {
        File dir = new File(context.getFilesDir(), pkg);
        return ensureDir(dir);
    }

    @Override
    public File getMassDir(Context context, String pkg) {
        File dir = new File(context.getExternalFilesDir(null), pkg);
        return ensureDir(dir);
    }

    @Override
    public File getDatabaseDir(Context context, String pkg) {
        File dir = new File(context.getDir(DIR_DATABASE, Context.MODE_PRIVATE), pkg);
        return ensureDir(dir);
    }

    @Override
    public File getSharedPrefDir(Context context, String pkg) {
        File dir = new File(context.getDir(DIR_SHARED_PREF, Context.MODE_PRIVATE), pkg);
        return ensureDir(dir);
    }

    @Override
    public long getDiskUsage(Context context, String pkg) {
        long size = 0;
        for (File f : getFiles(context, pkg)) {
            size += FileUtils.getDiskUsage(f);
        }
        return size;
    }

    @Override
    public void clearData(Context context, String pkg) {
        for (File f : getFiles(context, pkg)) {
            FileUtils.rmRF(f);
        }
    }

    protected List<File> getFiles(Context context, String pkg) {
        ArrayList<File> list = new ArrayList<>();
        list.add(getCacheDir(context, pkg));
        list.add(getFilesDir(context, pkg));
        list.add(getDatabaseDir(context, pkg));
        list.add(getSharedPrefDir(context, pkg));

        File massDir = getMassDir(context, pkg);
        if (massDir != null) {
            list.add(massDir);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            list.add(WebViewUtils.getWebViewData(context, pkg));
            list.add(WebViewUtils.getWebViewCache(context, pkg));
        }
        return list;
    }

    protected File ensureDir(File dir) {
        return FileUtils.mkdirs(dir) ? dir : null;
    }
}
