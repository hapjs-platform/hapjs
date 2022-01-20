/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.hapjs.common.utils.StringUtils;

public class StreamZipExtractor extends TeeZipExtractor {
    private static final String TAG = "StreamZipExtractor";

    private StreamSignature mStreamSignature;
    private MessageDigest mDigester;
    private Map<String, String> mDigests;
    private String mTmpFileSuffix = "";

    public StreamZipExtractor(
            ZipInputStream stream,
            TeeInputStream teeStream,
            ProgressInputStream progressStream,
            File archiveFile,
            StreamSignature streamSignature,
            String tmpFileSuffix) {
        super(stream, teeStream, progressStream, archiveFile);
        mStreamSignature = streamSignature;
        if (tmpFileSuffix != null) {
            mTmpFileSuffix = tmpFileSuffix;
        }
    }

    @Override
    protected void extractInner(File outDir) throws IOException, CacheException {
        try {
            mDigester = MessageDigest.getInstance(mStreamSignature.getAlgorithm());
            mDigests = mStreamSignature.getDigests();
            super.extractInner(outDir);
        } catch (NoSuchAlgorithmException e) {
            throw new CacheException(
                    CacheErrorCode.PACKAGE_PARSE_CERTIFICATE_FAILED, "algorithm is invalid", e);
        } catch (DigestNotMatchException e) {
            throw new CacheException(CacheErrorCode.PACKAGE_VERIFY_DIGEST_FAILED,
                    "digest not match", e);
        }
    }

    @Override
    protected ByteArrayOutputStream readFile(InputStream stream, String fileName, long size)
            throws IOException {
        mDigester.reset();
        InputStream teeStream = new TeeInputStream(stream, new DigestOutputStream(mDigester));
        ByteArrayOutputStream out = super.readFile(teeStream, fileName, size);
        String digest = StringUtils.byte2HexString(mDigester.digest());
        if (!digest.equalsIgnoreCase(mDigests.get(fileName))) {
            throw new DigestNotMatchException("Fail to verify digest");
        }
        return out;
    }

    @Override
    protected SaveFileTask generateSaveFileTask(byte[] content, File file) {
        return new SaveFileTask(content, file, mTmpFileSuffix);
    }

    public static class DigestNotMatchException extends IOException {
        public DigestNotMatchException(String message) {
            super(message);
        }
    }
}
