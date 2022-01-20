/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;

public interface PermissionCheckProvider {

    String NAME = "permission_check";

    /**
     * check whether a pkg has permission to access
     */
    boolean verify(Context context, String callingPkg);
}
