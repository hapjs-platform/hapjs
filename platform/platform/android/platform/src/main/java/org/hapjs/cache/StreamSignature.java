/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONException;
import org.json.JSONObject;

class StreamSignature {
    private static final String TAG = "StreamSignature";

    private static final String FILENAME_HASH_JSON = "hash.json";
    private static final String KEY_ALGORITHM = "algorithm";
    private static final String KEY_DIGESTS = "digests";
    private String mPkg;
    private File mStreamCertFile;
    private String mAlgorithm;
    private Map<String, String> mDigests;

    private StreamSignature(String pkg, File streamCertFile) {
        this.mPkg = pkg;
        this.mStreamCertFile = streamCertFile;
    }

    public static StreamSignature parse(String pkg, File file) {
        return new StreamSignature(pkg, file);
    }

    public void verifySignature(Context context, File certFile) throws CacheException {
        InstallInterceptProvider installProvider =
                ProviderManager.getDefault().getProvider(InstallInterceptProvider.NAME);
        installProvider.onSignatureVerify(context, mStreamCertFile, certFile, mPkg);

        JSONObject hashJson = loadHashJson(mStreamCertFile);
        if (hashJson != null) {
            mAlgorithm = getAlgorithm(hashJson);
            mDigests = getFileDigests(hashJson);
        }
        mStreamCertFile.delete();
        if (mAlgorithm == null || mDigests == null) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_PARSE_CERTIFICATE_FAILED, "cert file is invalid");
        }
    }

    public Map<String, String> getDigests() {
        return mDigests;
    }

    private JSONObject loadHashJson(File zipFile) {
        InputStream in = null;
        ZipInputStream zis = null;
        try {
            in = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(in));
            ZipEntry ze = zis.getNextEntry();
            if (ze != null && FILENAME_HASH_JSON.equals(ze.getName())) {
                String hashJson = FileUtils.readStreamAsString(zis, true);
                return new JSONObject(hashJson);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fail to find CERT", e);
        } catch (IOException e) {
            Log.e(TAG, "Fail to read hash.json", e);
        } catch (JSONException e) {
            Log.e(TAG, "Fail to parse hash.json", e);
        } finally {
            FileUtils.closeQuietly(zis);
            FileUtils.closeQuietly(in);
        }
        return null;
    }

    public String getAlgorithm() {
        return mAlgorithm;
    }

    private String getAlgorithm(JSONObject hashJson) {
        return hashJson.optString(KEY_ALGORITHM, "SHA-256");
    }

    private Map<String, String> getFileDigests(JSONObject hashJson) throws CacheException {
        JSONObject files = hashJson.optJSONObject(KEY_DIGESTS);
        if (files != null) {
            Map<String, String> digests = new HashMap<>();
            Iterator<String> iterator = files.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                digests.put(key, files.optString(key));
            }
            return digests;
        } else {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_PARSE_CERTIFICATE_FAILED, "digests is invalid");
        }
    }
}
