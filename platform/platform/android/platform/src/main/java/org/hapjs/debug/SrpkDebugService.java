/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import android.net.Uri;

public class SrpkDebugService extends org.hapjs.debug.DebugService {
    @Override
    protected int installPackage(String pkg, Uri uri) {
        return DebugSrpkInstaller.installPackage(this, pkg, uri);
    }
}
