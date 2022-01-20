/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends InputStream {
    private static final int NOTIFY_SIZE_INTERVAL = 4096;

    private InputStream mIn;
    private StreamProgressListener mStreamProgressListener;
    private boolean mClosed = false;
    private long mBytesRead;
    private long mLastNotifySize;

    public ProgressInputStream(InputStream in) {
        mIn = in;
    }

    public void setStreamProgressListener(StreamProgressListener l) {
        mStreamProgressListener = l;
    }

    @Override
    public int read() throws IOException {
        int b = mIn.read();
        onRead(b >= 0 ? 1 : -1);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int byteCount = mIn.read(b, off, len);
        onRead(byteCount);
        return byteCount;
    }

    private void onRead(int byteCount) {
        if (byteCount > 0) {
            mBytesRead += byteCount;
        }
        if (mStreamProgressListener != null) {
            if (byteCount <= 0 || mBytesRead - mLastNotifySize >= NOTIFY_SIZE_INTERVAL) {
                mLastNotifySize = mBytesRead;
                mStreamProgressListener.onStreamProgress(mBytesRead);
            }
        }
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
        mClosed = true;
    }

    public interface StreamProgressListener {
        void onStreamProgress(long savedSize);
    }
}
