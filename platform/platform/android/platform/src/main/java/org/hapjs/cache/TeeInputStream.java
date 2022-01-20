/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TeeInputStream extends InputStream {
    private static final int BUFFER_SIZE = 4096;

    private InputStream mIn;
    private OutputStream mOut;
    private boolean mClosed = false;

    public TeeInputStream(InputStream in, File file) throws FileNotFoundException {
        this(in, new BufferedOutputStream(new FileOutputStream(file)));
    }

    public TeeInputStream(InputStream in, OutputStream out) {
        mIn = in;
        mOut = out;
    }

    @Override
    public int read() throws IOException {
        int b = mIn.read();
        if (b >= 0) {
            mOut.write(b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int byteCount = mIn.read(b, off, len);
        if (byteCount > 0) {
            mOut.write(b, off, byteCount);
        }
        return byteCount;
    }

    @Override
    public int available() throws IOException {
        return mIn.available();
    }

    @Override
    public void close() throws IOException {
        if (mClosed) {
            return;
        }
        mIn.close();
        mOut.flush();
        mOut.close();
        mClosed = true;
    }

    public void skipFully() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int byteCount = read(buffer, 0, BUFFER_SIZE);
        while (byteCount >= 0) {
            byteCount = read(buffer, 0, BUFFER_SIZE);
        }
    }

    public void flush() throws IOException {
        mOut.flush();
    }
}
