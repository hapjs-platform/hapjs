/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;

public abstract class AbsSQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String TAG = "AbsSQLiteOpenHelper";
    protected Context mContext;
    protected String mName;

    public AbsSQLiteOpenHelper(
            @Nullable Context context,
            @Nullable String name,
            @Nullable SQLiteDatabase.CursorFactory factory,
            int version) {
        super(context, name, factory, version);
        this.mContext = context;
        this.mName = name;
    }

    public AbsSQLiteOpenHelper(
            @Nullable Context context,
            @Nullable String name,
            @Nullable SQLiteDatabase.CursorFactory factory,
            int version,
            @Nullable DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
        this.mContext = context;
        this.mName = name;
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.close();
        deleteDatabase();
        db = getWritableDatabase();
        onCreate(db);
    }

    public void deleteDatabase() {
        File databaseFile = mContext.getDatabasePath(mName);
        if (databaseFile.exists()) {
            boolean deleteResult = SQLiteDatabase.deleteDatabase(databaseFile);
            Log.i(TAG, "deleteDatabase: " + mName + ", deleteResult: " + deleteResult);
        }
    }
}
