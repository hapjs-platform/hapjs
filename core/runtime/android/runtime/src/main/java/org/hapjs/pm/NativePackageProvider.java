/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.pm;

import android.content.Context;

public interface NativePackageProvider {
    String NAME = "nativePackageProvider";

    boolean hasPackageInstalled(Context context, String pkg);

    boolean installPackage(Context context, String pkg, String callingPkg);
}
