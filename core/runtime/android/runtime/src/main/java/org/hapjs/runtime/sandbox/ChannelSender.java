/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// The channel end for sending request from the corresponding receiver end.
public class ChannelSender extends SandboxChannel {
    private static final String TAG = "ChannelSender";

    private SandboxLogHelper.PositiveChannelStatHelper mSandboxStatHelper;
    private Handler mHandler;
    private String mQuickAppPkg;

    public ChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler) {
        super(readSide, writeSide);
        mHandler = handler;
        mSandboxStatHelper = new SandboxLogHelper.PositiveChannelStatHelper(this, handler);
    }

    public void setQuickAppPkg(String pkg) {
        mQuickAppPkg = pkg;
    }

    public String getQuickAppPkg() {
        return mQuickAppPkg;
    }

    protected <T> T invokeSync(String method, Class<T> retClazz, Object... args) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            throw new RuntimeException("channel can only be accessed from one single thread");
        }

        try {
            debugLog(TAG, "start to scheduleHeartBeat");
            mSandboxStatHelper.scheduleHeartBeat(getQuickAppPkg());

            debugLog(TAG, "start to write sync, method=" + method);
            write(serialize(true, method, args));

            debugLog(TAG, "start to read response, method=" + method);
            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            read(responseBuffer);

            debugLog(TAG, "end to read response, method=" + method);
            SerializeHelper.Deserializer deserializer = SerializeHelper.getInstance().createDeserializer(responseBuffer.toByteArray());
            boolean isNull = deserializer.readObject(boolean.class);
            T response = isNull ? null : deserializer.readObject(retClazz);

            debugLog(TAG, "end to write sync, method=" + method);
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void invokeAsync(String method, Object... args) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            throw new RuntimeException("channel can only be accessed from one single thread");
        }

        try {
            debugLog(TAG, "start to scheduleHeartBeat");
            mSandboxStatHelper.scheduleHeartBeat(getQuickAppPkg());

            debugLog(TAG, "start to write async, method=" + method);
            write(serialize(false, method, args));
            Log.d(TAG, "end to write response, method=" + method);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] serialize(boolean sync, String method, Object... args) throws IOException {
        debugLog(TAG, "start to serialize, method=" + method);

        SerializeHelper.Serializer serializer = SerializeHelper.getInstance().createSerializer();
        serializer.writeObject(sync);
        serializer.writeObject(method);
        serializer.writeObject(args);
        serializer.writeObject(System.currentTimeMillis());
        byte[] data = serializer.closeAndGetBytes();

        debugLog(TAG, "end to serialize, dataSize=" + data.length);
        return data;
    }
}
