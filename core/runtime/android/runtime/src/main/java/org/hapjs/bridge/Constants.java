/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public interface Constants {
    /**
     * Don't use this directly, please use {@link FeatureExtension#getRequestBaseCode()} instead
     */
    int FEATURE_REQUEST_CODE_BASE = 43210;

    /**
     * Don't use this, it's only for {@link org.hapjs.bridge.permission.SystemPermissionManager }
     * usage
     */
    int FEATURE_PERMISSION_CODE_BASE = 32100;

    /**
     * Don't use this directly, please use {@link
     * org.hapjs.runtime.RuntimeActivity#getRequestBaseCode()} instead
     */
    int ACTIVITY_REQUEST_CODE_BASE = 23210;
}
