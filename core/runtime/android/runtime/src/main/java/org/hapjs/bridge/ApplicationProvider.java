/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import java.io.File;

public interface ApplicationProvider {
    String NAME = "ApplicationProvider";

    File getCacheDir(Context context, String pkg);

    File getFilesDir(Context context, String pkg);

    File getMassDir(Context context, String pkg);

    File getDatabaseDir(Context context, String pkg);

    File getSharedPrefDir(Context context, String pkg);

    long getDiskUsage(Context context, String pkg);

    void clearData(Context context, String pkg);
}
