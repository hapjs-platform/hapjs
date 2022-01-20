/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data.internal;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.features.storage.data.Columns;
import org.hapjs.features.storage.data.LocalStorageDatabase;
import org.hapjs.runtime.HapEngine;

public class SQLiteStorage implements IStorage {

    public static final int KEY_COLUMNS_INDEX = 0;
    public static final int VALUE_COLUMNS_INDEX = 1;
    private static final String[] VALUE_PROJECTION = new String[] {Columns.VALUE};
    private static final String[] KEY_PROJECTION = new String[] {Columns.KEY};
    private static final String[] COUNT_PROJECTION = new String[] {"COUNT(" + Columns._ID + ")"};
    private static final String[] PROJECTIONS = new String[] {Columns.KEY, Columns.VALUE};
    private static final String SELECTION = Columns.KEY + "=?";
    private static final ConcurrentHashMap<String, LocalStorageDatabase> sDatabaseMap =
            new ConcurrentHashMap<>();
    private ApplicationContext mApplicationContext;

    public SQLiteStorage(ApplicationContext context) {
        mApplicationContext = context;
    }

    private SQLiteDatabase getDatabase() {
        return getDatabase(mApplicationContext.getPackage()).getWritableDatabase();
    }

    private static LocalStorageDatabase getDatabase(String packageName) {
        LocalStorageDatabase database = sDatabaseMap.get(packageName);
        if (database == null) {
            ApplicationContext applicationContext =
                    HapEngine.getInstance(packageName).getApplicationContext();
            database = new LocalStorageDatabase(applicationContext);
            LocalStorageDatabase oldDatabase = sDatabaseMap.putIfAbsent(packageName, database);
            // Fail to put database, use the old value
            if (oldDatabase != null) {
                database = oldDatabase;
            }
        }
        return database;
    }

    // todo delete it
    public static void reset() {
        Iterator<String> it = sDatabaseMap.keySet().iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            sDatabaseMap.get(pkg).close();
            it.remove();
        }
    }

    public static void reset(String packageName) {
        LocalStorageDatabase database = sDatabaseMap.remove(packageName);
        if (database != null) {
            database.close();
        }
    }

    static void closeAndDelete(ApplicationContext context) {
        getDatabase(context.getPackage()).close();
        SQLiteDatabase.deleteDatabase(context.getDatabasePath(LocalStorageDatabase.DB_NAME));
    }

    protected static String getPackageName(@NonNull Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        return pathSegments.get(0);
    }

    @Override
    public String get(String key) {
        Cursor cursor =
                getDatabase()
                        .query(
                                Columns.TABLE_NAME,
                                VALUE_PROJECTION,
                                SELECTION,
                                new String[] {key},
                                null,
                                null,
                                null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    public boolean set(String key, String value) {
        // when switch to mmkv, new values are not allowed to store in SQLite db.
        return false;
    }

    @Override
    public Map<String, String> entries() {
        Map<String, String> entries = new LinkedHashMap<>();
        Cursor cursor =
                getDatabase().query(Columns.TABLE_NAME, PROJECTIONS, null, null, null, null,
                        Columns._ID);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        entries.put(cursor.getString(KEY_COLUMNS_INDEX),
                                cursor.getString(VALUE_COLUMNS_INDEX));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return entries;
    }

    public Cursor entriesCursor(String limit) {
        return getDatabase()
                .query(Columns.TABLE_NAME, PROJECTIONS, null, null, null, null, Columns._ID, limit);
    }

    @Override
    public String key(int index) {
        String key = null;
        Cursor cursor =
                getDatabase()
                        .query(Columns.TABLE_NAME, KEY_PROJECTION, null, null, null, null,
                                Columns._ID);
        if (cursor != null) {
            try {
                if (cursor.moveToPosition(index)) {
                    key = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return key;
    }

    @Override
    public int length() {
        int length = -1;
        Cursor cursor =
                getDatabase()
                        .query(Columns.TABLE_NAME, COUNT_PROJECTION, null, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    length = cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return length;
    }

    @Override
    public boolean delete(String key) {
        int deletedRowsSize =
                getDatabase().delete(Columns.TABLE_NAME, SELECTION, new String[] {key});
        return deletedRowsSize > 0;
    }

    @Override
    public boolean clear() {
        int deletedRowsSize = getDatabase().delete(Columns.TABLE_NAME, null, null);
        return deletedRowsSize > 0;
    }
}
