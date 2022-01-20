/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.pm;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.hapjs.debugger.utils.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class PackageManager {
    private static final String TAG = "PackageManager";

    public static PackageInfo getPackageInfo(String packagePath) {
        Log.d(TAG, "getPackageInfo packagePath=" + packagePath);
        PackageInfo info = getPackageInfo(new File(packagePath));
        if (info == null) {
            Log.d(TAG, "no manifest.json. try as split-rpk");
            File baseSubpackage = retrieveBaseSubpackage(packagePath);
            if (baseSubpackage != null) {
                info = getPackageInfo(baseSubpackage);
                baseSubpackage.delete();
            }
        }
        return info;
    }

    private static PackageInfo getPackageInfo(File packageFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(packageFile);
            ZipEntry zipEntry = zipFile.getEntry("manifest.json");
            if (zipEntry != null) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                String manifestText = FileUtils.readStreamAsString(inputStream, true);
                JSONObject manifestJson = new JSONObject(manifestText);
                return PackageInfo.parse(manifestJson);
            } else {
                Log.w(TAG, "manifest.json not found");
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to read manifest.json", e);
        } catch (JSONException e) {
            Log.e(TAG, "Fail to parse manifest.json", e);
        } finally {
            FileUtils.closeQuietly(zipFile);
        }
        return null;
    }

    private static File retrieveBaseSubpackage(String packagePath) {
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(packagePath)));
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                String filename = ze.getName();
                if (!filename.contains(File.separator) && !ze.isDirectory()
                        && (filename.endsWith(".base.srpk") || filename.equals("main.rpk"))) {
                    File tmpBaseSubpackage = File.createTempFile(filename, null);
                    byte[] buffer = new byte[1024];
                    int count;
                    fos = new FileOutputStream(tmpBaseSubpackage);
                    while ((count = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                    return tmpBaseSubpackage;
                }
            }
            Log.w(TAG, "base subpackage not found");
        } catch (IOException e) {
            Log.e(TAG, "failed to retrieve", e);
        } finally {
            FileUtils.closeQuietly(zis);
            FileUtils.closeQuietly(fos);
        }
        return null;
    }
}
