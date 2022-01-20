/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import org.hapjs.bridge.HybridManager;

public interface PermissionCallbackAdapter {
    void onPermissionAccept(
            HybridManager hybridManager, String[] grantedPermissions, boolean userGranted);

    void onPermissionReject(HybridManager hybridManager, String[] grantedPermissions);
}
