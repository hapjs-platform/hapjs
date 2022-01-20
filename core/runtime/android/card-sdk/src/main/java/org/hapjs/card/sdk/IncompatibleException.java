/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk;

public class IncompatibleException extends Exception {

    public IncompatibleException(String message) {
        super(message);
    }

    public IncompatibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompatibleException(Throwable cause) {
        super(cause);
    }
}
