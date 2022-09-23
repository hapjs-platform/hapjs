/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.signutils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.hapjs.debugger.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class SignatureUtils {
    private static final String TAG = "SignatureUtils";
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final String MARK_STRING = "---";

    public static String getMd5(byte[] data) {
        return getDigest(data, "MD5");
    }

    public static String getSha256(byte[] data) {
        return getDigest(data, "SHA-256");
    }

    private static String getDigest(byte[] data, String algorithm) {
        String hashString = "";
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(data);
            return byte2HexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Md5 algorithm NOT found.", e);
        }
        return hashString.toLowerCase();
    }

    public static String getSMSHash(String packageName, byte[] data) {
        if (TextUtils.isEmpty(packageName) || data == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((packageName + " ").getBytes());
            md.update(data);
            String base64Hash = Base64.encodeToString(md.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
            base64Hash = base64Hash.substring(0, 11);
            return base64Hash;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getSMSHash", e);
        }
        return "";
    }

    private static String byte2HexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static PackageInfo getNativePackageInfo(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "package not found: " + pkg, e);
        }
        return null;
    }

    public static PackageInfo getNativePackageInfo(Context context, Uri uri) {
        File apk = saveToFile(context, uri);
        if (apk != null) {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
        }
        return null;
    }

    public static Pair<org.hapjs.debugger.pm.PackageInfo, File> getHybridPackageInfo(Context context, Uri uri) {
        File archive = saveToFile(context, uri);
        File rpk = null;
        try {
            if (archive != null) {
                rpk = getSignedRpk(context, archive);
            }
            if (rpk != null) {
                org.hapjs.debugger.pm.PackageInfo pi = org.hapjs.debugger.pm.PackageManager.getPackageInfo(rpk.getAbsolutePath());
                pi.setSignature(getSignatureFromRpk(rpk));
                File iconFile = org.hapjs.debugger.pm.PackageManager.getIconFile(rpk.getAbsolutePath(), pi.getIconPath());
                return new Pair<>(pi, iconFile);
            }
        } finally {
            if (archive != null) {
                FileUtils.rmdirs(archive);
            }
            if (rpk != null) {
                FileUtils.rmdirs(rpk);
            }
        }
        return null;
    }

    public static byte[] getSignatureFromPem(Context context, Uri uri) {
        File pem = saveToFile(context, uri);
        if (pem != null) {
            try {
                String content = FileUtils.readFileAsString(pem.getAbsolutePath());
                return getSignatureFromPem(content);
            } catch (Throwable e) {
                Log.e(TAG, "getSignatureFromPem failed, uri=" + uri, e);
            }
        }
        return null;
    }

    private static byte[] getSignatureFromPem(String cerContent) {
        String[] lines = cerContent.split("\n");
        if (lines == null && lines.length == 0) {
            return null;
        }

        int beginLineIndex = 0;
        int lineCount = lines.length;
        String firstLine = lines[0];
        if (firstLine.contains(MARK_STRING)) {
            beginLineIndex = 1;
            lineCount--;
        }
        String endLine = lines[lines.length - 1];
        if (endLine.contains(MARK_STRING)) {
            lineCount--;
        }

        if (lineCount <= 0) {
            return null;
        }

        StringBuilder base64SignBuilder = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            base64SignBuilder.append(lines[i + beginLineIndex]);
        }
        return Base64.decode(base64SignBuilder.toString(), 0);
    }

    private static byte[] getSignatureFromRpk(File rpk) {
        try {
            X509Certificate[][] cert = SignatureVerifier.verify(rpk.getAbsolutePath());
            if (cert != null) {
                return cert[0][0].getEncoded();
            } else {
                Log.e(TAG, "SignatureVerifier.verify failed, rpk=" + rpk.getAbsolutePath());
            }
        } catch (SignatureVerifier.SignatureNotFoundException | CertificateEncodingException | IOException e) {
            Log.e(TAG, "SignatureVerifier.verify failed, rpk=" + rpk.getAbsolutePath(), e);
        }
        return null;
    }

    private static File getSignedRpk(Context context, File archive) {
        File tmpDir = context.getDir("temp", Context.MODE_PRIVATE);
        try {
            if (ZipUtils.unzip(archive, tmpDir)) {
                File[] files = tmpDir.listFiles();
                if (files != null) {
                    boolean hasManifest = false;
                    File rpkFile = null;
                    for (File file : files) {
                        if (file.getName().equals("manifest.json")) {
                            hasManifest = true;
                            break;
                        } else if (file.getName().endsWith(".rpk")) {
                            rpkFile = file;
                            break;
                        }
                    }

                    if (hasManifest) {
                        return archive;
                    } else if (rpkFile != null) {
                        try {
                            File tmpFile = File.createTempFile("rpkFile", "");
                            if (FileUtils.copyFile(rpkFile, tmpFile)) {
                                return tmpFile;
                            }
                        } catch (IOException e) {
                            Log.i(TAG, "retrive manifest.json fail", e);
                        }
                    }
                }
            }
        } finally {
            FileUtils.rmdirs(tmpDir);
        }
        return null;
    }

    private static File saveToFile(Context context, Uri uri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            File tmpFile = File.createTempFile("archive", "");
            if (FileUtils.saveToFile(input, tmpFile)) {
                return tmpFile;
            } else {
                Log.e(TAG, "saveToFile failed");
            }
        } catch (IOException e) {
            Log.e(TAG, "saveToFile failed, uri=" + uri, e);
        }
        return null;
    }

}
