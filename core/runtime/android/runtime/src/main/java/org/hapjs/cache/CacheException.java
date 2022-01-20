/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public class CacheException extends Exception {

    private int mCacheErrorCode;

    public CacheException(int cacheErrorCode, String msg) {
        super(msg);
        mCacheErrorCode = cacheErrorCode;
    }

    public CacheException(int errorCode, String msg, Throwable cause) {
        super(msg, cause);
        mCacheErrorCode = errorCode;
    }

    public int getErrorCode() {
        return mCacheErrorCode;
    }
}
