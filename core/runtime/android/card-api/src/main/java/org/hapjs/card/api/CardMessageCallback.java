/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public interface CardMessageCallback {
    void onMessage(int code, String data);
}
