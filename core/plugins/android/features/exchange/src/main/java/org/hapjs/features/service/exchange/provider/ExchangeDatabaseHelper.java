/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import org.hapjs.common.AbsSQLiteOpenHelper;

public class ExchangeDatabaseHelper extends AbsSQLiteOpenHelper {

    private static final String DB_NAME = "system_exchange.db";
    private static final int VERSION = 2;
    private static final String SQL_CREATE_TABLE_APP =
            "CREATE TABLE "
                    + Table.APP
                    + "("
                    + AppColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + AppColumns.PKG
                    + " TEXT NOT NULL,"
                    + AppColumns.SIGN
                    + " TEXT NOT NULL,"
                    + "UNIQUE("
                    + AppColumns.PKG
                    + ","
                    + AppColumns.SIGN
                    + ")"
                    + ")";
    private static final String SQL_CREATE_TABLE_DATA =
            "CREATE TABLE "
                    + Table.DATA
                    + "("
                    + DataColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + DataColumns.APP_ID
                    + " INTEGER NOT NULL,"
                    + DataColumns.KEY
                    + " TEXT NOT NULL,"
                    + DataColumns.VALUE
                    + " TEXT,"
                    + "UNIQUE("
                    + DataColumns.APP_ID
                    + ","
                    + DataColumns.KEY
                    + "),"
                    + "FOREIGN KEY("
                    + DataColumns.APP_ID
                    + ") REFERENCES "
                    + Table.APP
                    + "("
                    + AppColumns._ID
                    + ")"
                    + " ON DELETE CASCADE ON UPDATE CASCADE"
                    + ")";
    private static final String SQL_CREATE_TABLE_PERMISSION =
            "CREATE TABLE "
                    + Table.PERMISSION
                    + "("
                    + PermissionColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + PermissionColumns.DATA_ID
                    + " INTEGER NOT NULL,"
                    + PermissionColumns.GRANT_APP_ID
                    + " INTEGER NOT NULL,"
                    + PermissionColumns.WRITABLE
                    + " INTEGER NOT NULL,"
                    + "UNIQUE("
                    + PermissionColumns.DATA_ID
                    + ","
                    + PermissionColumns.GRANT_APP_ID
                    + "),"
                    + "FOREIGN KEY("
                    + PermissionColumns.DATA_ID
                    + ") REFERENCES "
                    + Table.DATA
                    + "("
                    + DataColumns._ID
                    + ")"
                    + " ON DELETE CASCADE ON UPDATE CASCADE "
                    + "FOREIGN KEY("
                    + PermissionColumns.GRANT_APP_ID
                    + ") REFERENCES "
                    + Table.APP
                    + "("
                    + AppColumns._ID
                    + ")"
                    + " ON DELETE RESTRICT ON UPDATE CASCADE"
                    + ")";
    private static final String SQL_CREATE_CLEAR_APP_TRIGGER =
            "CREATE TRIGGER clear_app"
                    + " AFTER DELETE ON "
                    + Table.DATA
                    + " WHEN 0=(SELECT count(*) FROM "
                    + Table.PERMISSION
                    + " WHERE "
                    + PermissionColumns.GRANT_APP_ID
                    + "=old."
                    + DataColumns.APP_ID
                    + ") AND 0=(SELECT count(*) FROM "
                    + Table.DATA
                    + " WHERE "
                    + DataColumns.APP_ID
                    + "=old."
                    + DataColumns.APP_ID
                    + ")"
                    + " BEGIN "
                    + "  DELETE FROM "
                    + Table.APP
                    + "  WHERE "
                    + AppColumns._ID
                    + "=old."
                    + DataColumns.APP_ID
                    + ";"
                    + " END;";

    public ExchangeDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_APP);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_DATA);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_PERMISSION);
        sqLiteDatabase.execSQL(SQL_CREATE_CLEAR_APP_TRIGGER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            upgradeToV2(db);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void upgradeToV2(SQLiteDatabase db) {
        db.execSQL(
                "ALTER TABLE "
                        + Table.PERMISSION
                        + " ADD COLUMN "
                        + PermissionColumns.WRITABLE
                        + " INTEGER NOT NULL DEFAULT "
                        + 0);
    }

    interface Table {
        String APP = "app";
        String DATA = "data";
        String PERMISSION = "permission";
    }

    interface AppColumns extends BaseColumns {
        String PKG = "pkg";
        String SIGN = "sign";
    }

    interface DataColumns extends BaseColumns {
        String KEY = "key";
        String VALUE = "value";
        String APP_ID = "app_id";
    }

    interface PermissionColumns extends BaseColumns {
        String DATA_ID = "data_id";
        String GRANT_APP_ID = "grant_app_id";
        String WRITABLE = "writable";
    }
}
