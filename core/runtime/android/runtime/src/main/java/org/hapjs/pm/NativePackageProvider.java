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

    boolean inRouterForbiddenList(Context context, String rpkPkg, String appPkg);

    boolean inRouterDialogList(Context context, String rpkPkg, String nativePkg);

    boolean triggeredByGestureEvent(Context context, String pkg);

    void recordFireEvent(String eventType);

    boolean inAlipayForbiddenList(Context context, String url);
}
