/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data.internal;

import android.database.Cursor;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Locale;
import java.util.Map;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.storage.data.LocalStorageDatabase;

public class LocalStorage implements IStorage {

    private static final String TAG = "LocalStorage";
    private static final String STORAGE_FILE_LOCK = "storage_file_lock.lock";

    private static final int LIMIT_STEP = 20;
    private static final Object LOCK = new Object();
    private IStorage mMMKVStorage;

    public LocalStorage(ApplicationContext context) {
        mMMKVStorage = new MMKVStorage(context);
        File appDatabaseFile = context.getDatabasePath(LocalStorageDatabase.DB_NAME);
        if (appDatabaseFile.exists()) {
            synchronized (LOCK) {
                File file = new File(context.getFilesDir(), STORAGE_FILE_LOCK);
                RandomAccessFile lockFile = null;
                FileLock fileLock = null;
                try {
                    lockFile = new RandomAccessFile(file.getPath(), "rw");
                    fileLock = lockFile.getChannel().lock();
                    if (appDatabaseFile.exists()) {
                        if (mMMKVStorage.length() == 0) {
                            transferData(context);
                            Log.i(TAG, "data transfer complete, delete db file!");
                        }
                        SQLiteStorage.closeAndDelete(context);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "create local storage fail! ", e);
                } finally {
                    if (fileLock != null) {
                        try {
                            fileLock.release();
                        } catch (IOException e) {
                            Log.e(TAG, "Fail to release lock", e);
                        }
                    }
                    FileUtils.closeQuietly(lockFile);
                }
            }
        }
    }

    private void transferData(ApplicationContext context) {
        SQLiteStorage sqLiteStorage = new SQLiteStorage(context);
        int length = sqLiteStorage.length();
        for (int index = 0; index < length; index += LIMIT_STEP) {
            Cursor cursor = null;
            try {
                String limit = String.format(Locale.ROOT, "%d, %d", index, LIMIT_STEP);
                cursor = sqLiteStorage.entriesCursor(limit);
                if (null != cursor && cursor.moveToFirst()) {
                    do {
                        try {
                            mMMKVStorage.set(
                                    cursor.getString(SQLiteStorage.KEY_COLUMNS_INDEX),
                                    cursor.getString(SQLiteStorage.VALUE_COLUMNS_INDEX));
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to tranfer data.", e);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                if (null != cursor) {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public String get(String key) {
        return mMMKVStorage.get(key);
    }

    @Override
    public boolean set(String key, String value) {
        return mMMKVStorage.set(key, value);
    }

    @Override
    public Map<String, String> entries() {
        return mMMKVStorage.entries();
    }

    @Override
    public String key(int index) {
        return mMMKVStorage.key(index);
    }

    @Override
    public int length() {
        return mMMKVStorage.length();
    }

    @Override
    public boolean delete(String key) {
        return mMMKVStorage.delete(key);
    }

    @Override
    public boolean clear() {
        return mMMKVStorage.clear();
    }
}
