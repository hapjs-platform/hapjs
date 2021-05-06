/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

public class DefaultRouterManageProvider implements RouterManageProvider {

    @Override
    public boolean canGoBackToSourcePkg() {
        return false;
    }

    @Override
    public boolean inRouterRpkForbiddenList(Context context, String currentPackage, String targetPkg) {
        return false;
    }

    @Override
    public boolean inRouterRpkDialogList(Context context, String currentPkg, String targetPkg) {
        return false;
    }

    @Override
    public boolean inRouterForbiddenList(Context context, String rpkPkg, String appPkg, ResolveInfo info) {
        return false;
    }

    @Override
    public boolean inRouterDialogList(Context context, String rpkPkg, String nativePkg, ResolveInfo info) {
        return false;
    }


    @Override
    public boolean triggeredByGestureEvent(Context context, String pkg) {
        return true;
    }

    @Override
    public void recordFireEvent(String eventType) {
    }

    @Override
    public boolean inWebpayForbiddenList(Context context, String url) {
        return false;
    }

    @Override
    public boolean startActivityIfNeeded(Activity activity, Intent intent, String rpkPkg) {
        activity.startActivity(intent);
        return true;
    }
}
