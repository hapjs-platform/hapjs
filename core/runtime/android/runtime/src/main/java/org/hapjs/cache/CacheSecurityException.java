/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public class CacheSecurityException extends Exception {
    public CacheSecurityException() {
    }

    public CacheSecurityException(String message) {
        super(message);
    }

    public CacheSecurityException(Throwable cause) {
        super(cause);
    }

    public CacheSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
