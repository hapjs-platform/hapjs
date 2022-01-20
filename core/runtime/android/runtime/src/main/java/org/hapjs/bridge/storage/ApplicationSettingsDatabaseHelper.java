/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.common.AbsSQLiteOpenHelper;

public class ApplicationSettingsDatabaseHelper extends AbsSQLiteOpenHelper {
    public static final String TABLE_SETTINGS = "settings";
    private static final String DB_NAME = "settings.db";
    private static final int DB_VERSION = 1;
    private static final String CREATE_TABLE_SETTINGS =
            "CREATE TABLE "
                    + TABLE_SETTINGS
                    + "("
                    + SettingsColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SettingsColumns.NAME
                    + " TEXT UNIQUE ON CONFLICT REPLACE,"
                    + SettingsColumns.VALUE
                    + " TEXT"
                    + ")";

    public ApplicationSettingsDatabaseHelper(ApplicationContext context) {
        super(context.getContext(), context.getDatabasePath(DB_NAME).getPath(), null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SETTINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public interface SettingsColumns extends BaseColumns {
        String NAME = "name";
        String VALUE = "value";
    }
}
