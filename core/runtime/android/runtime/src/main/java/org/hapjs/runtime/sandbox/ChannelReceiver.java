/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.ParcelFileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// The channel end for receiving request from the corresponding sender end.
public abstract class ChannelReceiver extends SandboxChannel {
    private static final String TAG = "ChannelReceiver";

    public ChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, String name) {
        super(readSide, writeSide);

        new Thread(() -> startListen(), name).start();
    }

    private void startListen() {
        try {
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            while (true) {
                buff.reset();

                debugLog(TAG, "start to read channel");
                read(buff);

                debugLog(TAG, "start to deserialize");
                SerializeHelper.Deserializer deserializer = SerializeHelper.getInstance().createDeserializer(buff.toByteArray());
                boolean sync = deserializer.readObject(boolean.class);
                String method = deserializer.readObject(String.class);
                Object[] args = deserializer.readObject(Object[].class);
                long startTimeStamp = deserializer.readObject(long.class);
                deserializer.close();
                debugLog(TAG, "start to deserialize, sync=" + sync + ", method=" + method + ", size=" + buff.size() + ", timeCosts=" + (System.currentTimeMillis() - startTimeStamp));

                SandboxLogHelper.onChannelReceive(getQuickAppPkg(), method, buff.size(), startTimeStamp);

                Object response;
                if (SandboxIpcMethods.HEART_BEAT.equals(method)) {
                    response = true;
                } else {
                    response = onInvoke(method, args);
                }
                debugLog(TAG, "end to invoke, response=" + response);

                if (sync) {
                    SerializeHelper.Serializer serializer = SerializeHelper.getInstance().createSerializer();
                    serializer.writeObject(response == null);
                    if (response != null) {
                        serializer.writeObject(response);
                    }
                    byte[] bytes = serializer.closeAndGetBytes();
                    write(bytes);
                    debugLog(TAG, "end to write response");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getQuickAppPkg();
    protected abstract Object onInvoke(String method, Object[] args);
}
