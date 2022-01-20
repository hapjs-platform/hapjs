/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SafeContentResolver {

    private static final String TAG = "SafeContentResolver";
    private ContentResolver mResolver;

    private SafeContentResolver(ContentResolver resolver) {
        mResolver = resolver;
    }

    public static SafeContentResolver get(ContentResolver resolver) {
        return new SafeContentResolver(resolver);
    }

    /**
     * @see ContentResolver#insert(Uri, ContentValues)
     */
    public final Uri insert(Uri url, ContentValues values) {
        try {
            return mResolver.insert(url, values);
        } catch (Exception e) {
            Log.e(TAG, "insert: ", e);
        }
        return null;
    }

    /**
     * @see ContentResolver#bulkInsert(Uri, ContentValues[])
     */
    public final int bulkInsert(Uri url, ContentValues[] values) {
        try {
            return mResolver.bulkInsert(url, values);
        } catch (Exception e) {
            Log.e(TAG, "bulkInsert: ", e);
        }
        return 0;
    }

    /**
     * @see ContentResolver#update(Uri, ContentValues, String, String[])
     */
    public final int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
        try {
            return mResolver.update(uri, values, where, selectionArgs);
        } catch (Exception e) {
            Log.e(TAG, "update: ", e);
        }
        return -1;
    }

    /**
     * @see ContentResolver#delete(Uri, String, String[])
     */
    public final int delete(Uri url, String where, String[] selectionArgs) {
        try {
            return mResolver.delete(url, where, selectionArgs);
        } catch (Exception e) {
            Log.e(TAG, "delete: ", e);
        }
        return -1;
    }

    /**
     * @see ContentResolver#query(Uri, String[], String, String[], String)
     */
    public final Cursor query(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        try {
            return mResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (Exception e) {
            Log.e(TAG, "query: ", e);
        }
        return null;
    }
}
