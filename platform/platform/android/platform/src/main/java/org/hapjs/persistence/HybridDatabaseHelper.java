/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.Context;

public class HybridDatabaseHelper extends AbstractDatabase {
    private static final String DB_NAME = "hybrid.db";
    private static final int DB_VERSION = 7;

    public HybridDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        addTable(new LauncherTable(this));
        addTable(new PermissionTable(this));
        addTable(new InstalledSubpackageTable(this));
        addTable(new ShortcutParamsTable(this));
    }
}
