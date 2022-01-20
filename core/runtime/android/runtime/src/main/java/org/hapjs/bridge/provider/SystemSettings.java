/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.hapjs.runtime.Runtime;

public class SystemSettings extends AbstractSettings {
    private static final String TAG = "SystemSettings";
    private Context mContext;
    private Uri mUri;

    private SystemSettings(Context context) {
        mContext = context;
        mUri = SettingsProvider.getSystemUri(context);
    }

    public static SystemSettings getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    protected String getValue(String name) {
        String[] projections = new String[] {SettingsDatabaseHelper.SettingsColumns.VALUE};
        String selection = SettingsDatabaseHelper.SettingsColumns.NAME + "=?";
        String[] selectionArgs = new String[] {name};
        Cursor cursor =
                mContext.getContentResolver()
                        .query(mUri, projections, selection, selectionArgs, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    protected boolean putValue(String name, String value) {
        ContentValues values = new ContentValues();
        values.put(SettingsDatabaseHelper.SettingsColumns.NAME, name);
        values.put(SettingsDatabaseHelper.SettingsColumns.VALUE, value);
        Uri uri = null;
        try {
            uri = mContext.getContentResolver().insert(mUri, values);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown Uri " + mUri, e);
        }
        return uri != null;
    }

    private static class Holder {
        static SystemSettings INSTANCE = new SystemSettings(Runtime.getInstance().getContext());
    }
}
