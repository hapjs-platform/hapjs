/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.HybridManager;

/* package */ interface PermissionManager {
    void requestPermissions(
            HybridManager hybridManager,
            String[] permissions,
            PermissionCallbackAdapter callback,
            AbstractExtension.PermissionPromptStrategy strategy);
}
