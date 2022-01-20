/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.internal;

public interface InternalInstallListener {
    void onInstallResult(String pkg, int resultCode, int errorCode);
}
