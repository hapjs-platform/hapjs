/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;

import org.hapjs.render.jsruntime.SandboxProvider;
import org.hapjs.runtime.ProviderManager;

// A channel has two ends. One is for sending requests, the other is for receiving request and may send back response.
public class SandboxChannel {
    private static final String TAG = "SandboxChannel";
    private ParcelFileDescriptor.AutoCloseInputStream mInput;
    private ParcelFileDescriptor.AutoCloseOutputStream mOutput;
    private byte[] mBuffer = new byte[64 * 1024];
    private ReentrantLock mLock = new ReentrantLock(true);

    public SandboxChannel(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide) {
        mInput = new ParcelFileDescriptor.AutoCloseInputStream(readSide);
        mOutput = new ParcelFileDescriptor.AutoCloseOutputStream(writeSide);
    }

    protected void read(ByteArrayOutputStream buff) throws IOException {
        int lenNeeded = readInt();
        read(mInput, buff, lenNeeded);
    }

    private int readInt() throws IOException {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        read(mInput, buff, 4);
        byte[] array = buff.toByteArray();
        return ((array[0] & 0xff) << 24) | ((array[1] & 0xff) << 16) | ((array[2] & 0xff) << 8) | (array[3] & 0xff);
    }

    private void read(InputStream input, OutputStream output, int lenNeeded) throws IOException {
        int len, lenLeft = lenNeeded;
        while ((len = input.read(mBuffer, 0, Math.min(mBuffer.length, lenLeft))) > 0) {
            output.write(mBuffer, 0, len);
            lenLeft -= len;
        }

        if (len == -1) {
            throw new IOException("stream closed.");
        } else if (lenLeft != 0) {
            throw new IOException("want " + lenNeeded + ", but get " + (lenNeeded - lenLeft));
        }
    }

    protected void write(byte[] data) throws IOException {
        writeInt(data.length);
        mOutput.write(data, 0, data.length);
    }

    private void writeInt(int val) throws IOException {
        mBuffer[0] = (byte) ((val >> 24) & 0xff);
        mBuffer[1] = (byte) ((val >> 16) & 0xff);
        mBuffer[2] = (byte) ((val >> 8) & 0xff);
        mBuffer[3] = (byte) (val & 0xff);

        mOutput.write(mBuffer, 0, 4);
    }

    protected static void debugLog(String tag, String msg) {
        SandboxProvider sandboxProvider = ProviderManager.getDefault().getProvider(SandboxProvider.NAME);
        if (sandboxProvider != null && sandboxProvider.isDebugLogEnabled()) {
            Log.d(tag, msg);
        }
    }
}
