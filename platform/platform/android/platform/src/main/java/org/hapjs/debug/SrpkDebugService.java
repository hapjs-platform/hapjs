/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import android.content.Intent;
import android.net.Uri;

import org.hapjs.launch.LauncherManager;

public class SrpkDebugService extends org.hapjs.debug.DebugService {
    @Override
    protected int installPackage(String pkg, Uri uri) {
        return DebugSrpkInstaller.installPackage(this, pkg, uri);
    }

    @Override
    void launchToDebug(Intent intent) {
        LauncherManager.launch(this, intent);
    }
}
