/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import org.hapjs.common.AbsSQLiteOpenHelper;

public class SettingsDatabaseHelper extends AbsSQLiteOpenHelper {
    public static final String TABLE_SYSTEM = "system";
    private static final String DB_NAME = "settings.db";
    private static final int DB_VERSION = 1;
    private static final String CREATE_TABLE_SYSTEM =
            "CREATE TABLE "
                    + TABLE_SYSTEM
                    + "("
                    + SettingsColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SettingsColumns.NAME
                    + " TEXT UNIQUE ON CONFLICT REPLACE,"
                    + SettingsColumns.VALUE
                    + " TEXT"
                    + ")";

    public SettingsDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SYSTEM);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public interface SettingsColumns extends BaseColumns {
        String NAME = "name";
        String VALUE = "value";
    }
}
