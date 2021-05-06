/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

public interface RouterManageProvider {
    String NAME = "routerManageProvider";

    boolean canGoBackToSourcePkg();

    boolean inRouterRpkForbiddenList(Context context, String currentPackage, String targetPkg);

    boolean inRouterRpkDialogList(Context context, String currentPkg, String targetPkg);

    boolean inRouterForbiddenList(Context context, String rpkPkg, String appPkg, ResolveInfo info);

    boolean inRouterDialogList(Context context, String rpkPkg, String nativePkg, ResolveInfo info);

    boolean triggeredByGestureEvent(Context context, String pkg);

    void recordFireEvent(String eventType);

    boolean inWebpayForbiddenList(Context context, String url);

    boolean startActivityIfNeeded(Activity activity, Intent intent, String rpkPkg);
}
