/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import org.hapjs.common.utils.FileUtils;

public class TeeZipExtractor extends ZipExtractor {
    private static final String TAG = "TeeZipExtractor";
    protected File mArchiveFile;
    private ProgressInputStream mProgressStream;
    private TeeInputStream mTeeStream;

    public TeeZipExtractor(
            ZipInputStream stream,
            TeeInputStream teeStream,
            ProgressInputStream progressStream,
            File archiveFile) {
        super(stream, false);
        mTeeStream = teeStream;
        mProgressStream = progressStream;
        mArchiveFile = archiveFile;
    }

    @Override
    public void extract(File outDir) throws IOException, CacheException {
        try {
            extractInner(outDir);
        } finally {
            FileUtils.closeQuietly(mStream, mTeeStream, mProgressStream);
        }
        Log.i(TAG, "mArchiveFile.size=" + mArchiveFile.length());
    }

    protected void extractInner(File outDir) throws IOException, CacheException {
        super.extract(outDir);
        mTeeStream.skipFully();
    }

    public void cancel() throws CacheException {
        try {
            mTeeStream.skipFully();
        } catch (IOException e) {
            throw new CacheException(CacheErrorCode.UNKNOWN, "Fail to cancel install", e);
        } finally {
            FileUtils.closeQuietly(mTeeStream);
            super.cancel();
        }
    }

    public void setStreamProgressListener(
            ProgressInputStream.StreamProgressListener streamProgressListener) {
        mProgressStream.setStreamProgressListener(streamProgressListener);
    }
}
