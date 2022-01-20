/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.adapter;

import android.content.Context;
import android.os.Binder;
import org.hapjs.runtime.PermissionChecker;

public class StorageProvider extends org.hapjs.features.storage.data.StorageProvider {
    @Override
    protected boolean checkPermission(Context context) {
        return PermissionChecker.verify(context, Binder.getCallingUid());
    }
}
