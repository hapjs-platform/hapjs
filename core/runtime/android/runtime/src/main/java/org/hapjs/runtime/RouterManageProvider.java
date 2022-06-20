/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;

public interface RouterManageProvider {
    String NAME = "routerManageProvider";

    boolean canGoBackToSourcePkg();

    boolean inRouterRpkForbiddenList(Context context, String currentPackage, String targetPkg);

    boolean inRouterRpkDialogList(Context context, String currentPkg, String targetPkg);

    boolean inRouterForbiddenList(Context context, String rpkPkg, String appPkg);

    boolean inRouterDialogList(Context context, String rpkPkg, String nativePkg);

    boolean triggeredByGestureEvent(Context context, String pkg);

    void recordFireEvent(String eventType);

    boolean inAlipayForbiddenList(Context context, String url);
}
