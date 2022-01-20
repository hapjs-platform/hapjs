/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class HybridInputStream extends InputStream {
    private InputStream mInputStream;
    private InputStream mInputFromFile;
    private boolean mFileFinished;
    private boolean mClosed;

    public HybridInputStream(File inputFile, InputStream inputStream) throws FileNotFoundException {
        mInputFromFile = new BufferedInputStream(new FileInputStream(inputFile));
        mInputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
        if (!mFileFinished) {
            int b = mInputFromFile.read();
            if (b >= 0) {
                return b;
            }
            mFileFinished = true;
        }
        return mInputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int byteCountFromFile = -1;
        if (!mFileFinished) {
            byteCountFromFile = mInputFromFile.read(b, off, len);
            if (byteCountFromFile == len) {
                return byteCountFromFile;
            } else if (byteCountFromFile == -1) {
                mFileFinished = true;
            }
        }

        int realByteCount = byteCountFromFile == -1 ? 0 : byteCountFromFile;
        int byteCountFromStream = mInputStream.read(b, off + realByteCount, len - realByteCount);
        if (byteCountFromFile == -1) {
            return byteCountFromStream;
        } else if (byteCountFromStream == -1) {
            return byteCountFromFile;
        } else {
            return byteCountFromFile + byteCountFromStream;
        }
    }

    @Override
    public int available() throws IOException {
        return mInputFromFile.available() + mInputStream.available();
    }

    @Override
    public void close() throws IOException {
        if (mClosed) {
            return;
        }
        mInputStream.close();
        mInputFromFile.close();
        mClosed = true;
    }
}
