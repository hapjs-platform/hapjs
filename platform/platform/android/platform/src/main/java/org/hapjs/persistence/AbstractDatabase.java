/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.AbsSQLiteOpenHelper;

public abstract class AbstractDatabase extends AbsSQLiteOpenHelper {

    private static final String TAG = "AbstractTable";
    private List<Table> mTables;

    public AbstractDatabase(
            Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mTables = new ArrayList<>();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        synchronized (this) {
            SQLiteDatabase sqLiteDatabase;
            try {
                sqLiteDatabase = super.getWritableDatabase();
                ensureTables(sqLiteDatabase);
            } catch (SQLException e) {
                Log.e(TAG, "getWritableDatabase fail! ", e);
                deleteDatabase();
                sqLiteDatabase = super.getWritableDatabase();
            }
            return sqLiteDatabase;
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        synchronized (this) {
            SQLiteDatabase sqLiteDatabase;
            try {
                sqLiteDatabase = super.getReadableDatabase();
                if (!getMissedTables(sqLiteDatabase).isEmpty()) {
                    sqLiteDatabase = getWritableDatabase();
                }
            } catch (SQLException e) {
                Log.e(TAG, "getReadableDatabase fail! ", e);
                deleteDatabase();
                sqLiteDatabase = super.getReadableDatabase();
            }
            return sqLiteDatabase;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Table table : mTables) {
            table.onCreate(db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (Table table : mTables) {
            table.onUpgrade(db, oldVersion, newVersion);
        }
    }

    public void addTable(Table table) {
        if (table != null) {
            mTables.add(table);
        }
    }

    public void addTables(List<Table> tables) {
        if (tables != null) {
            mTables.addAll(tables);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public List<Table> getTables() {
        return mTables;
    }

    private void ensureTables(SQLiteDatabase database) {
        List<String> missedTables = getMissedTables(database);
        for (Table table : mTables) {
            String name = table.getName();
            boolean missed = missedTables.contains(name);
            if (missed) {
                Log.d(TAG, "table: " + name + " is not exist, recreate table");
                table.onCreate(database);
            }
        }
    }

    private List<String> getMissedTables(SQLiteDatabase database) {
        List<String> missedTables = new ArrayList<>();
        List<String> existTables = getExistTables(database);
        for (Table table : mTables) {
            String name = table.getName();
            if (!existTables.contains(name)) {
                missedTables.add(name);
            }
        }
        return missedTables;
    }

    private List<String> getExistTables(SQLiteDatabase database) {
        List<String> existTables = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor =
                    database.rawQuery(
                            "select name from sqlite_master where type='table' order by name",
                            null);
            while (cursor != null && cursor.moveToNext()) {
                String name = cursor.getString(0);
                existTables.add(name);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return existTables;
    }
}
