/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public class MockCacheLimitStrategy implements CacheLimitStrategy {
    private long mMaxSize = 100;
    private CacheStorage mCacheStorage;

    public MockCacheLimitStrategy(CacheStorage cacheStorage, long maxSize) {
        mCacheStorage = cacheStorage;
        mMaxSize = maxSize;
    }

    @Override
    public long availableSize() {
        return Math.max(mMaxSize - mCacheStorage.size(), 0);
    }
}
