/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackageHelper {
    private static final String TAG = "PackageHelper";

    public static void install(
            Context context,
            CacheStorage cacheStorage,
            String packageName,
            String manifest,
            String[] files,
            String[] data)
            throws Exception {
        File destFile = new File(context.getCacheDir(), packageName);
        zip(manifest, files, data, destFile.getAbsolutePath());
        cacheStorage.install(packageName, destFile.getAbsolutePath());
    }

    private static void zip(String manifest, String[] files, String[] data, String destFile)
            throws Exception {
        FileOutputStream dest = new FileOutputStream(destFile);

        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
        ZipEntry manifestEntry = new ZipEntry("manifest.json");
        out.putNextEntry(manifestEntry);
        out.write(manifest.getBytes("utf-8"));
        for (int i = 0; i < files.length; i++) {
            ZipEntry entry = new ZipEntry(files[i]);
            out.putNextEntry(entry);
            out.write(data[i].getBytes("utf-8"));
        }
        out.close();
    }
}
