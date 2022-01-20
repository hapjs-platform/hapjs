/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import org.hapjs.model.AppInfo;
import org.hapjs.model.SubpackageInfo;

public interface PackageListener {
    void onPackageInstalled(String pkg, AppInfo appInfo);

    void onPackageUpdated(String pkg, AppInfo appInfo);

    void onPackageRemoved(String pkg);

    void onSubpackageInstalled(String pkg, SubpackageInfo subpackageInfo, int versionCode);
}
