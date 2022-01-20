/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public interface Extension {
    String ACTION_INIT = "__init__";

    /**
     * Invocation mode.
     */
    enum Mode {
        /**
         * Synchronous invocation. When calling actions in such mode, caller
         * will get response until invocation finished.
         */
        SYNC,
        /**
         * Asynchronous invocation. When calling actions in such mode, caller
         * will get an empty response immediately, but wait in a different
         * thread to get response until invocation finished.
         */
        ASYNC,
        /**
         * Callback invocation. When calling actions in such mode, caller will
         * get an empty response immediately, but receive response through
         * callback when invocation finished.
         */
        CALLBACK
    }

    enum Type {
        FUNCTION,
        ATTRIBUTE,
        EVENT
    }

    enum Access {
        NONE,
        READ,
        WRITE
    }

    enum Normalize {
        RAW,
        JSON
    }

    enum NativeType {
        INSTANCE
    }

    enum Multiple {
        SINGLE,
        MULTI
    }
}
