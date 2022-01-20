/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.collection.LruCache;
import java.util.concurrent.atomic.AtomicInteger;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.provider.AbstractSettings;
import org.hapjs.common.utils.FileUtils;

public class ApplicationSettings extends AbstractSettings implements AutoCloseable {
    private static final String TAG = "ApplicationSettings";
    private static LruCache<ApplicationContext, ApplicationSettings> sCache =
            new LruCache<ApplicationContext, ApplicationSettings>(1) {
                @Override
                protected void entryRemoved(
                        boolean evicted,
                        ApplicationContext context,
                        ApplicationSettings oldValue,
                        ApplicationSettings newValue) {
                    super.entryRemoved(evicted, context, oldValue, newValue);
                    if (oldValue != null) {
                        oldValue.close();
                    }
                }

                @Override
                protected ApplicationSettings create(ApplicationContext context) {
                    return new ApplicationSettings(context);
                }
            };
    private ApplicationSettingsDatabaseHelper mDbHelper;
    private AtomicInteger mReferenceCounter;

    private ApplicationSettings(ApplicationContext context) {
        mDbHelper = new ApplicationSettingsDatabaseHelper(context);
        mReferenceCounter = new AtomicInteger(1);
    }

    public static ApplicationSettings getInstance(ApplicationContext context) {
        ApplicationSettings applicationSettings = sCache.get(context);
        if (applicationSettings != null) {
            return applicationSettings.incrementReference();
        } else {
            Log.e(TAG, "getInstance: applicationSettings is null");
        }
        return null;
    }

    public Cursor query(String[] projection, String selection, String[] selectionArgs) {
        return mDbHelper
                .getReadableDatabase()
                .query(
                        ApplicationSettingsDatabaseHelper.TABLE_SETTINGS,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null);
    }

    @Override
    protected String getValue(String name) {
        String[] projections =
                new String[] {ApplicationSettingsDatabaseHelper.SettingsColumns.VALUE};
        String selection = ApplicationSettingsDatabaseHelper.SettingsColumns.NAME + "=?";
        String[] selectionArgs = new String[] {name};
        Cursor cursor =
                mDbHelper
                        .getReadableDatabase()
                        .query(
                                ApplicationSettingsDatabaseHelper.TABLE_SETTINGS,
                                projections,
                                selection,
                                selectionArgs,
                                null,
                                null,
                                null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return cursor.getString(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "getValue: ", e);
            } finally {
                FileUtils.closeQuietly(cursor);
            }
        }
        return null;
    }

    @Override
    protected boolean putValue(String name, String value) {
        ContentValues values = new ContentValues();
        values.put(ApplicationSettingsDatabaseHelper.SettingsColumns.NAME, name);
        values.put(ApplicationSettingsDatabaseHelper.SettingsColumns.VALUE, value);
        long id =
                mDbHelper
                        .getWritableDatabase()
                        .insertWithOnConflict(
                                ApplicationSettingsDatabaseHelper.TABLE_SETTINGS,
                                null,
                                values,
                                SQLiteDatabase.CONFLICT_REPLACE);
        return id >= 0;
    }

    @Override
    public void close() {
        int count = mReferenceCounter.decrementAndGet();
        if (count == 0) {
            mDbHelper.close();
        }
    }

    private ApplicationSettings incrementReference() {
        mReferenceCounter.incrementAndGet();
        return this;
    }
}
