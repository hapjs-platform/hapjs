/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

public interface PermissionCallback {
    void onPermissionAccept();

    void onPermissionReject(int reason);
}
