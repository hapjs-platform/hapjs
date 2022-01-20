/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public interface CallbackContextHolder {
    void putCallbackContext(CallbackContext callbackContext);

    void removeCallbackContext(String action);

    void runCallbackContext(String action, int what, Object obj);
}
