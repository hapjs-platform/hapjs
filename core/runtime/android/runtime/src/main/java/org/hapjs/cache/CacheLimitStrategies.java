/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import org.hapjs.cache.utils.StorageUtil;

public class CacheLimitStrategies {
    public static CacheLimitStrategy simpleCacheLimitStrategy() {
        return new SimpleCacheLimitStrategies();
    }

    public static class SimpleCacheLimitStrategies implements CacheLimitStrategy {
        private static final int TEN_M = 10 * 1024 * 1024;

        @Override
        public long availableSize() {
            return StorageUtil.getAvailableInternalMemorySize() - TEN_M;
        }
    }
}
