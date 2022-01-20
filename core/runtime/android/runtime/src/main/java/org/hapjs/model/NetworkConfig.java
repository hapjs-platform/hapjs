/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import org.json.JSONObject;

public class NetworkConfig {

    public static final long CONNECT_TIMEOUT = 30_000;
    public static final long READ_TIMEOUT = 30_000;
    public static final long WRITE_TIMEOUT = 30_000;
    private static final String KEY_CONNECT_TIMEOUT = "connectTimeout";
    private static final String KEY_READ_TIMEOUT = "readTimeout";
    private static final String KEY_WRITE_TIMEOUT = "writeTimeout";
    private long mConnectTimeout;
    private long mReadTimeout;
    private long mWriteTimeout;

    public NetworkConfig() {
    }

    public static NetworkConfig parse(JSONObject networkObject) {
        NetworkConfig networkConfig = new NetworkConfig();

        long connectTimeout = CONNECT_TIMEOUT;
        long readTimeout = READ_TIMEOUT;
        long writeTimeout = WRITE_TIMEOUT;

        if (networkObject != null) {
            connectTimeout = networkObject.optInt(KEY_CONNECT_TIMEOUT);
            readTimeout = networkObject.optInt(KEY_READ_TIMEOUT);
            writeTimeout = networkObject.optInt(KEY_WRITE_TIMEOUT);
        }

        networkConfig.mConnectTimeout = connectTimeout > 0 ? connectTimeout : CONNECT_TIMEOUT;
        networkConfig.mReadTimeout = readTimeout > 0 ? readTimeout : READ_TIMEOUT;
        networkConfig.mWriteTimeout = writeTimeout > 0 ? writeTimeout : WRITE_TIMEOUT;
        return networkConfig;
    }

    public long getConnectTimeout() {
        return mConnectTimeout;
    }

    public long getReadTimeout() {
        return mReadTimeout;
    }

    public long getWriteTimeout() {
        return mWriteTimeout;
    }
}
