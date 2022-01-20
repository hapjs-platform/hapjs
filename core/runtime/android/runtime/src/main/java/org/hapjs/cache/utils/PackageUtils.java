/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.hapjs.bridge.AppInfoProvider;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.InstallInterceptProvider;
import org.hapjs.cache.PackageCheckProvider;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;
import org.hapjs.runtime.BuildConfig;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONException;
import org.json.JSONObject;

public class PackageUtils {
    public static final String FILENAME_MANIFEST = "manifest.json";
    public static final String FILENAME_MANIFEST_PHONE = "manifest-phone.json";
    public static final String FILENAME_MANIFEST_TV = "manifest-tv.json";
    public static final String FILENAME_APP_JS = "app.js";
    public static final String FILENAME_META_INFO = "META-INF";
    public static final String FILENAME_CERT = "CERT";
    private static final String TAG = "PackageUtils";

    public static void checkPackage(Context context, File zipFile, File certFile, String pkg)
            throws CacheException {
        checkPackage(context, null, zipFile, certFile, pkg);
    }

    public static void checkPackage(
            Context context, SubpackageInfo info, File zipFile, File certFile, String pkg)
            throws CacheException {
        InstallInterceptProvider installProvider =
                ProviderManager.getDefault().getProvider(InstallInterceptProvider.NAME);
        installProvider.onSignatureVerify(context, zipFile, certFile, pkg);

        if (info == null || info.isBase()) {
            checkRequiredFiles(zipFile, true);
        } else if (info.isStandalone()) {
            checkRequiredFiles(zipFile, false);
        }
    }

    public static void verify(Context context, File zipFile, File certFile, String pkg)
            throws CacheException {
        if (isVerifierDisabled(context)) {
            return;
        }

        byte[] certificateBytes = extractCertificate(zipFile);
        if (SignatureStore.exist(certFile)) {
            try {
                byte[] existCert = SignatureStore.load(certFile);
                if (!SignatureStore.match(existCert, certificateBytes)) {
                    throw new CacheException(
                            CacheErrorCode.PACKAGE_CERTIFICATE_CHANGED,
                            "Package file certificate changed");
                }
            } catch (IOException e) {
                Log.w(TAG, "verify signature failed", e);
                throw new CacheException(
                        CacheErrorCode.LOAD_EXISTED_CERTIFICATE_FAILED,
                        "Load existed package file certificate failed",
                        e);
            }
        } else {
            PackageCheckProvider provider =
                    ProviderManager.getDefault().getProvider(PackageCheckProvider.NAME);
            if (!provider.isValidCertificate(pkg, certificateBytes)) {
                throw new CacheException(
                        CacheErrorCode.PACKAGE_VERIFY_SIGNATURE_FAILED,
                        "Failed to verify the package file signature");
            }
            if (!SignatureStore.save(certificateBytes, certFile)) {
                throw new CacheException(
                        CacheErrorCode.SAVE_CERTIFICATE_FAILED,
                        "Save package file certificate failed");
            }
        }
    }

    public static byte[] extractCertificate(File zipFile) throws CacheException {
        Certificate[][] certificates;
        try {
            certificates = SignatureVerifier.verify(zipFile.getAbsolutePath());
        } catch (SignatureVerifier.SignatureNotFoundException e) {
            Log.w(TAG, "no signature", e);
            throw new CacheException(
                    CacheErrorCode.PACKAGE_HAS_NO_SIGNATURE, "Package file has no signature", e);
        } catch (Exception e) {
            Log.w(TAG, "verify signature failed", e);
            throw new CacheException(
                    CacheErrorCode.PACKAGE_VERIFY_SIGNATURE_FAILED,
                    "Failed to verify the package file signature",
                    e);
        }

        byte[] certificateBytes;
        try {
            certificateBytes = certificates[0][0].getEncoded();
        } catch (CertificateEncodingException e) {
            Log.w(TAG, "verify signature failed", e);
            throw new CacheException(
                    CacheErrorCode.PACKAGE_PARSE_CERTIFICATE_FAILED,
                    "Failed to parse the package file certificate",
                    e);
        }

        return certificateBytes;
    }

    private static void checkRequiredFiles(File zipFile, boolean needAppJs) throws CacheException {
        boolean hasManifestJson = false;
        boolean hasAppJs = false;
        InputStream in = null;
        try {
            in = new FileInputStream(zipFile);
            ZipInputStream zip = new ZipInputStream(new BufferedInputStream(in));
            ZipEntry entry = zip.getNextEntry();
            PackageCheckProvider provider =
                    ProviderManager.getDefault().getProvider(PackageCheckProvider.NAME);
            while (entry != null && (!hasManifestJson || !hasAppJs)) {
                String path = entry.getName();
                if (FILENAME_MANIFEST.equals(path)) {
                    hasManifestJson = true;
                } else if (provider.hasAppJs(path)) {
                    hasAppJs = true;
                }
                entry = zip.getNextEntry();
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Package file is not exists: " + zipFile.getPath(), e);
            throw new CacheException(
                    CacheErrorCode.PACKAGE_ARCHIVE_NOT_EXIST, "Package file does not exist", e);
        } catch (IOException e) {
            Log.w(TAG, "Package file is broken: " + zipFile.getPath(), e);
            throw new CacheException(CacheErrorCode.PACKAGE_READ_FAILED, "Package file read failed",
                    e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        if (!hasManifestJson) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_HAS_NO_MANIFEST_JSON,
                    "Package file has no manifest.json");
        }
        if (needAppJs && !hasAppJs) {
            throw new CacheException(CacheErrorCode.PACKAGE_HAS_NO_APP_JS,
                    "Package file has no app.js");
        }
    }

    public static boolean hasAppJs(String path) {
        return FILENAME_APP_JS.equals(path);
    }

    private static void checkManifest(String packageName, InputStream in)
            throws IOException, CacheException {
        String manifestText = FileUtils.readStreamAsString(in, false);
        JSONObject manifestJson;
        try {
            manifestJson = new JSONObject(manifestText);
        } catch (JSONException e) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_MANIFEST_JSON_INVALID, "Manifest.json is invalid", e);
        }

        AppInfoProvider provider = ProviderManager.getDefault().getProvider(AppInfoProvider.NAME);
        AppInfo appInfo = provider.parse(manifestJson);
        if (!TextUtils.equals(packageName, appInfo.getPackage())) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_NAME_CHANGED,
                    "Package name is different with requested");
        }
        if (appInfo.getMinPlatformVersion() > BuildConfig.platformVersion) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_INCOMPATIBLE, "Package is incompatible with platform");
        }
    }

    public static AppInfo getAppInfo(String packagePath) {
        return getAppInfo(new File(packagePath));
    }

    public static AppInfo getAppInfo(File packageFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(packageFile);
            ZipEntry zipEntry = zipFile.getEntry(FILENAME_MANIFEST);
            if (zipEntry != null) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                String manifestText = FileUtils.readStreamAsString(inputStream, true);
                JSONObject manifestJson = new JSONObject(manifestText);
                AppInfoProvider provider =
                        ProviderManager.getDefault().getProvider(AppInfoProvider.NAME);
                return provider.parse(manifestJson);
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

    public static boolean isVerifierDisabled(Context context) {
        Resources res = context.getResources();
        int id = res.getIdentifier("disable_verifier", "bool", context.getPackageName());
        boolean result = (id != 0 && res.getBoolean(id));
        Log.d(TAG, "isVerifierDisabled:" + result);
        return result;
    }
}
