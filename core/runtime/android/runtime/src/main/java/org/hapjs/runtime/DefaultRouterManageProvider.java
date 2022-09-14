/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.net.Uri;

public class DefaultRouterManageProvider implements RouterManageProvider {

    @Override
    public boolean canGoBackToSourcePkg() {
        return false;
    }

    @Override
    public boolean inRouterRpkForbiddenList(Context context, String currentPackage,String targetPkg) {
        return false;
    }

    @Override
    public boolean inRouterRpkDialogList(Context context, String currentPkg, String targetPkg) {
        return false;
    }

    @Override
    public boolean inRouterForbiddenList(Context context, String rpkPkg, String appPkg) {
        return false;
    }

    @Override
    public boolean inRouterDialogList(Context context, String rpkPkg, String nativePkg) {
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
    public boolean inAlipayForbiddenList(Context context, String url) {
        return false;
    }
}
