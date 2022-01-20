/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.AbstractContentProvider;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.service.exchange.common.Constant;
import org.hapjs.features.service.exchange.common.PackageUtil;

public class ExchangeProvider extends AbstractContentProvider {
    private static final String TAG = "ExchangeProvider";

    private static final int MATCH_DATA = 1;
    private static final int MATCH_PERMISSION = 2;
    private static final int MATCH_CLEAR = 3;

    private static final String SELECTION_APP_PKG = ExchangeDatabaseHelper.AppColumns.PKG + "=?";
    private static final String SELECTION_APP_SIGN = ExchangeDatabaseHelper.AppColumns.SIGN + "=?";
    private static final String SELECTION_DATA_APP_ID =
            ExchangeDatabaseHelper.DataColumns.APP_ID + "=?";
    private static final String SELECTION_DATA_KEY = ExchangeDatabaseHelper.DataColumns.KEY + "=?";
    private static final String SELECTION_PERMISSION_DATA_ID =
            ExchangeDatabaseHelper.PermissionColumns.DATA_ID + "=?";
    private static final String SELECTION_PERMISSION_GRANT_APP_ID =
            ExchangeDatabaseHelper.PermissionColumns.GRANT_APP_ID + "=?";
    private static final String SELECTION_PERMISSION_WRITABLE =
            ExchangeDatabaseHelper.PermissionColumns.WRITABLE + "=?";

    private static final String PKG_GLOBAL = "global.package";
    private static final String PKG_VENDOR = "vendor.package";
    private static final String SIGN_GLOBAL = "global.sign";

    private ExchangeDatabaseHelper mDBHelper;
    private UriMatcher mMatcher;
    private String mAuthority;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mAuthority = getAuthority(context);
        mDBHelper = new ExchangeDatabaseHelper(context);
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(mAuthority, Constant.URI_PATH_DATA, MATCH_DATA);
        mMatcher.addURI(mAuthority, Constant.URI_PATH_PERMISSION, MATCH_PERMISSION);
        mMatcher.addURI(mAuthority, Constant.URI_PATH_CLEAR, MATCH_CLEAR);
        return true;
    }

    protected String getAuthority(Context context) {
        return context.getPackageName() + ".exchange";
    }

    @Override
    public synchronized Cursor doQuery(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int code = mMatcher.match(uri);
        String[] callingPkgAndSign = getCallingPkgAndSign(uri);
        if (code == MATCH_DATA) {
            String scope = uri.getQueryParameter(Constant.PARAM_SCOPE);
            String key = uri.getQueryParameter(Constant.PARAM_KEY);
            if (TextUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Illegal Params Key");
            }
            if (Constant.SCOPE_GLOBAL.equals(scope)) {
                return queryData(PKG_GLOBAL, SIGN_GLOBAL, key, callingPkgAndSign[0],
                        callingPkgAndSign[1]);
            } else if (Constant.SCOPE_VENDOR.equals(scope)) {
                return queryData(
                        PKG_VENDOR, callingPkgAndSign[1], key, callingPkgAndSign[0],
                        callingPkgAndSign[1]);
            } else {
                String targetPkg = uri.getQueryParameter(Constant.PARAM_PKG);
                String targetSign = uri.getQueryParameter(Constant.PARAM_SIGN);
                if (TextUtils.isEmpty(targetPkg) || TextUtils.isEmpty(targetSign)) {
                    throw new IllegalArgumentException("Illegal Params");
                }
                return queryData(
                        targetPkg, targetSign.toLowerCase(), key, callingPkgAndSign[0],
                        callingPkgAndSign[1]);
            }
        }
        throw new IllegalArgumentException("Illegal Uri");
    }

    @Override
    public synchronized Uri doInsert(Uri uri, ContentValues values) {
        int code = mMatcher.match(uri);
        String[] callingPkgAndSign = getCallingPkgAndSign(uri);
        switch (code) {
            case MATCH_DATA: {
                String key = uri.getQueryParameter(Constant.PARAM_KEY);
                String value = uri.getQueryParameter(Constant.PARAM_VALUE);
                if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                    throw new IllegalArgumentException("Illegal Param");
                }
                String scope = uri.getQueryParameter(Constant.PARAM_SCOPE);
                if (Constant.SCOPE_GLOBAL.equals(scope)) {
                    return insertData(uri, PKG_GLOBAL, SIGN_GLOBAL, key, value);
                } else if (Constant.SCOPE_VENDOR.equals(scope)) {
                    return insertData(uri, PKG_VENDOR, callingPkgAndSign[1], key, value);
                } else {
                    String targetPkg = uri.getQueryParameter(Constant.PARAM_PKG);
                    String targetSign = uri.getQueryParameter(Constant.PARAM_SIGN);
                    if (TextUtils.isEmpty(targetPkg) || TextUtils.isEmpty(targetSign)) {
                        targetPkg = callingPkgAndSign[0];
                        targetSign = callingPkgAndSign[1];
                    }
                    Boolean writable =
                            checkWritaData(
                                    targetPkg,
                                    targetSign.toLowerCase(),
                                    key,
                                    callingPkgAndSign[0],
                                    callingPkgAndSign[1]);
                    if (writable) {
                        return insertData(uri, targetPkg, targetSign, key, value);
                    } else {
                        throw new SecurityException("no permission to set other application data!");
                    }
                }
            }
            case MATCH_PERMISSION: {
                String targetPkg = uri.getQueryParameter(Constant.PARAM_PKG);
                String targetSign = uri.getQueryParameter(Constant.PARAM_SIGN);
                String targetKey = uri.getQueryParameter(Constant.PARAM_KEY);
                String targetWritable = uri.getQueryParameter(Constant.PARAM_WRITABLE);
                if (TextUtils.isEmpty(targetPkg) || TextUtils.isEmpty(targetSign)) {
                    throw new IllegalArgumentException("Illegal Params");
                }
                int writable = TextUtils.equals("true", targetWritable) ? 1 : 0;
                return insertPermission(
                        uri,
                        callingPkgAndSign[0],
                        callingPkgAndSign[1],
                        targetPkg,
                        targetSign.toLowerCase(),
                        targetKey,
                        writable);
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public synchronized int doUpdate(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return -1;
    }

    @Override
    public synchronized int doDelete(Uri uri, String selection, String[] selectionArgs) {
        int code = mMatcher.match(uri);
        String[] callingPkgAndSign = getCallingPkgAndSign(uri);
        switch (code) {
            case MATCH_DATA: {
                String targetPkg = uri.getQueryParameter(Constant.PARAM_PKG);
                String targetSign = uri.getQueryParameter(Constant.PARAM_SIGN);
                String key = uri.getQueryParameter(Constant.PARAM_KEY);
                if (TextUtils.isEmpty(targetPkg) || TextUtils.isEmpty(targetSign)) {
                    targetPkg = callingPkgAndSign[0];
                    targetSign = callingPkgAndSign[1];
                }
                Boolean writable =
                        checkWritaData(
                                targetPkg,
                                targetSign.toLowerCase(),
                                key,
                                callingPkgAndSign[0],
                                callingPkgAndSign[1]);
                if (writable) {
                    if (TextUtils.isEmpty(key)) {
                        return clear(targetPkg, targetSign);
                    }
                    return removeData(targetPkg, targetSign, key);
                } else {
                    throw new SecurityException("no permission to remove other application data!");
                }
            }
            case MATCH_PERMISSION: {
                String targetPkg = uri.getQueryParameter(Constant.PARAM_PKG);
                if (TextUtils.isEmpty(targetPkg)) {
                    throw new IllegalArgumentException("Illegal Params");
                }
                String targetKey = uri.getQueryParameter(Constant.PARAM_KEY);
                return deletePermission(callingPkgAndSign[0], callingPkgAndSign[1], targetPkg,
                        targetKey);
            }
            case MATCH_CLEAR: {
                String clearPkg = uri.getQueryParameter(Constant.PARAM_PKG);
                if (TextUtils.isEmpty(clearPkg)) {
                    throw new IllegalArgumentException("Illegal Params");
                }
                if (!TextUtils.equals(getContext().getPackageName(), callingPkgAndSign[0])) {
                    throw new SecurityException("no permission");
                }
                return clear(clearPkg);
            }
            default:
                break;
        }
        return -1;
    }

    private String[] getCallingPkgAndSign(Uri uri) {
        String callingPkg = getCallingPackage();
        if (TextUtils.isEmpty(callingPkg)) {
            throw new IllegalArgumentException("Unknown source");
        }
        String callingSign;
        String queryPkg = uri.getQueryParameter(Constant.PARAM_CALLING_PKG);
        if (checkPermissionWithCallingPkg(callingPkg) && !TextUtils.isEmpty(queryPkg)) {
            callingPkg = queryPkg;
            callingSign = uri.getQueryParameter(Constant.PARAM_CALLING_SIGN);
        } else {
            callingSign = PackageUtil.getNativeAppSignDigest(getContext(), callingPkg);
        }
        if (TextUtils.isEmpty(callingPkg) || TextUtils.isEmpty(callingSign)) {
            throw new IllegalArgumentException("Unknown source");
        }
        return new String[] {callingPkg, callingSign.toLowerCase()};
    }

    protected boolean checkPermissionWithCallingPkg(String callingPkg) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "checkPermissionWithCallingPkg: context is null");
            return false;
        }
        return TextUtils.equals(callingPkg, context.getPackageName())
                || context.getPackageManager().checkSignatures(callingPkg, context.getPackageName())
                == PackageManager.SIGNATURE_MATCH;
    }

    private Cursor queryData(int appId, String key) {
        String select = SELECTION_DATA_APP_ID + " AND " + SELECTION_DATA_KEY;
        String id = String.valueOf(appId);
        String[] args = new String[] {id, key};
        return mDBHelper
                .getWritableDatabase()
                .query(
                        ExchangeDatabaseHelper.Table.DATA,
                        new String[] {
                                ExchangeDatabaseHelper.DataColumns._ID,
                                ExchangeDatabaseHelper.DataColumns.KEY,
                                ExchangeDatabaseHelper.DataColumns.VALUE
                        },
                        select,
                        args,
                        null,
                        null,
                        null);
    }

    private Cursor queryData(
            String targetPkg, String targetSign, String key, String callingPkg,
            String callingSign) {
        int targetAppId = queryAppId(targetPkg, targetSign);
        if (targetAppId < 0) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = queryData(targetAppId, key);
            if (cursor == null) {
                return null;
            }
            if (!cursor.moveToNext()) {
                return null;
            }
            String value =
                    cursor.getString(
                            cursor.getColumnIndex(ExchangeDatabaseHelper.DataColumns.VALUE));
            if (TextUtils.equals(PKG_GLOBAL, targetPkg)
                    || TextUtils.equals(targetSign, callingSign)) {
                return makeResult(key, value);
            }
            int grantAppId = queryAppId(callingPkg, callingSign);
            if (grantAppId >= 0) {
                int dataId = cursor.getInt(
                        cursor.getColumnIndex(ExchangeDatabaseHelper.DataColumns._ID));
                if (queryPermission(dataId, grantAppId)) {
                    return makeResult(key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "queryData: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        throw new SecurityException("no permission to get other application data!");
    }

    private Boolean checkWritaData(
            String targetPkg, String targetSign, String key, String callingPkg,
            String callingSign) {
        if (TextUtils.equals(targetPkg, callingPkg) && TextUtils.equals(targetSign, callingSign)) {
            return true;
        }
        int targetAppId = queryAppId(targetPkg, targetSign);
        if (targetAppId < 0) {
            return false;
        }
        int grantAppId = queryAppId(callingPkg, callingSign);
        if (grantAppId < 0) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = queryData(targetAppId, key);
            if (cursor == null || !cursor.moveToNext()) {
                return false;
            }
            int dataId =
                    cursor.getInt(cursor.getColumnIndex(ExchangeDatabaseHelper.DataColumns._ID));
            if (checkWritePermission(dataId, grantAppId)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "checkWritaData: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return false;
    }

    private Cursor makeResult(String key, String value) {
        MatrixCursor cursor =
                new MatrixCursor(new String[] {Constant.PARAM_KEY, Constant.PARAM_VALUE});
        cursor.addRow(new Object[] {key, value});
        return cursor;
    }

    private int queryAppId(String pkg, String sign) {
        Cursor cursor = null;
        try {
            cursor =
                    mDBHelper
                            .getWritableDatabase()
                            .query(
                                    ExchangeDatabaseHelper.Table.APP,
                                    new String[] {ExchangeDatabaseHelper.AppColumns._ID},
                                    SELECTION_APP_PKG + " AND " + SELECTION_APP_SIGN,
                                    new String[] {pkg, sign},
                                    null,
                                    null,
                                    null);
            if (cursor == null) {
                return -1;
            }
            if (cursor.getCount() == 0) {
                return -1;
            }
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(0);
                return id;
            }
        } catch (Exception e) {
            Log.e(TAG, "queryAppId: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return -1;
    }

    private int[] queryAppIds(String pkg) {
        Cursor cursor = null;
        try {
            cursor =
                    mDBHelper
                            .getWritableDatabase()
                            .query(
                                    ExchangeDatabaseHelper.Table.APP,
                                    new String[] {ExchangeDatabaseHelper.AppColumns._ID},
                                    SELECTION_APP_PKG,
                                    new String[] {pkg},
                                    null,
                                    null,
                                    null);
            if (cursor == null) {
                return null;
            }
            if (cursor.getCount() == 0) {
                return null;
            }
            int[] result = new int[cursor.getCount()];
            int index = 0;
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(0);
                result[index++] = id;
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "queryAppIds: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return null;
    }

    private boolean queryPermission(int dataId, int grantAppId) {
        Cursor cursor = null;
        try {
            cursor =
                    mDBHelper
                            .getWritableDatabase()
                            .query(
                                    ExchangeDatabaseHelper.Table.PERMISSION,
                                    new String[] {ExchangeDatabaseHelper.PermissionColumns._ID},
                                    SELECTION_PERMISSION_DATA_ID + " AND "
                                            + SELECTION_PERMISSION_GRANT_APP_ID,
                                    new String[] {String.valueOf(dataId),
                                            String.valueOf(grantAppId)},
                                    null,
                                    null,
                                    null);
            if (cursor != null && cursor.getCount() != 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "queryPermission: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return false;
    }

    private boolean checkWritePermission(int dataId, int grantAppId) {
        Cursor cursor = null;
        try {
            cursor =
                    mDBHelper
                            .getWritableDatabase()
                            .query(
                                    ExchangeDatabaseHelper.Table.PERMISSION,
                                    new String[] {ExchangeDatabaseHelper.PermissionColumns._ID},
                                    SELECTION_PERMISSION_DATA_ID
                                            + " AND "
                                            + SELECTION_PERMISSION_GRANT_APP_ID
                                            + " AND "
                                            + SELECTION_PERMISSION_WRITABLE,
                                    new String[] {
                                            String.valueOf(dataId), String.valueOf(grantAppId),
                                            String.valueOf(1)
                                    },
                                    null,
                                    null,
                                    null);
            if (cursor != null && cursor.getCount() != 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "checkWritePermission: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return false;
    }

    private long insertData(long appId, String key, String value) {
        ContentValues values = new ContentValues();
        values.put(ExchangeDatabaseHelper.DataColumns.APP_ID, appId);
        values.put(ExchangeDatabaseHelper.DataColumns.KEY, key);
        values.put(ExchangeDatabaseHelper.DataColumns.VALUE, value);
        long id =
                mDBHelper
                        .getWritableDatabase()
                        .insertWithOnConflict(
                                ExchangeDatabaseHelper.Table.DATA, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE);
        return id;
    }

    private Uri insertData(Uri uri, String ownerPkg, String ownerSign, String key, String value) {
        long appId = getOrInsertApp(ownerPkg, ownerSign);
        if (appId < 0) {
            return null;
        }
        long dataId = insertData(appId, key, value);
        if (dataId >= 0) {
            return uri;
        }
        long count = updateData(appId, key, value);
        if (count > 0) {
            return uri;
        }
        return null;
    }

    private long getOrInsertApp(String pkg, String sign) {
        long id = queryAppId(pkg, sign);
        if (id > 0) {
            return id;
        }

        ContentValues values = new ContentValues();
        values.put(ExchangeDatabaseHelper.AppColumns.PKG, pkg);
        values.put(ExchangeDatabaseHelper.AppColumns.SIGN, sign);
        id =
                mDBHelper
                        .getWritableDatabase()
                        .insertWithOnConflict(
                                ExchangeDatabaseHelper.Table.APP, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE);
        return id;
    }

    private long updateData(long appId, String key, String value) {
        ContentValues values = new ContentValues();
        values.put(ExchangeDatabaseHelper.DataColumns.VALUE, value);
        long id =
                mDBHelper
                        .getWritableDatabase()
                        .updateWithOnConflict(
                                ExchangeDatabaseHelper.Table.DATA,
                                values,
                                SELECTION_DATA_APP_ID + " AND " + SELECTION_DATA_KEY,
                                new String[] {String.valueOf(appId), key},
                                SQLiteDatabase.CONFLICT_IGNORE);
        return id;
    }

    private Uri insertPermission(
            Uri uri,
            String ownerPkg,
            String ownerSign,
            String grantPkg,
            String grantSign,
            String grantKey,
            int writable) {
        if (TextUtils.equals(ownerSign, grantSign)) {
            return uri;
        }
        long ownerAppId = queryAppId(ownerPkg, ownerSign);
        if (ownerAppId < 0) {
            return null;
        }
        long grantAppId = getOrInsertApp(grantPkg, grantSign);
        if (grantAppId < 0) {
            return null;
        }
        long id = insertPermission(ownerAppId, grantAppId, grantKey, writable);
        if (id < 0) {
            return null;
        }
        return Uri.withAppendedPath(uri, String.valueOf(id));
    }

    private long insertPermission(long ownerAppId, long grantAppId, String grantKey, int writable) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT");
        sql.append(" OR REPLACE");
        sql.append(" INTO ");
        sql.append(ExchangeDatabaseHelper.Table.PERMISSION);
        sql.append("(");
        sql.append(ExchangeDatabaseHelper.PermissionColumns.DATA_ID);
        sql.append(",");
        sql.append(ExchangeDatabaseHelper.PermissionColumns.GRANT_APP_ID);
        sql.append(",");
        sql.append(ExchangeDatabaseHelper.PermissionColumns.WRITABLE);
        sql.append(")");
        sql.append(" SELECT ");
        sql.append(ExchangeDatabaseHelper.DataColumns._ID);
        sql.append(",");
        sql.append(grantAppId);
        sql.append(",");
        sql.append(writable);
        sql.append(" FROM ");
        sql.append(ExchangeDatabaseHelper.Table.DATA);
        sql.append(" WHERE ");
        sql.append(SELECTION_DATA_APP_ID);
        boolean isAll = TextUtils.isEmpty(grantKey);
        if (!isAll) {
            sql.append(" AND ");
            sql.append(SELECTION_DATA_KEY);
        }
        String[] args =
                isAll
                        ? new String[] {String.valueOf(ownerAppId)}
                        : new String[] {String.valueOf(ownerAppId), grantKey};
        SQLiteStatement statement =
                mDBHelper.getWritableDatabase().compileStatement(sql.toString());
        statement.bindAllArgsAsStrings(args);
        try {
            return statement.executeInsert();
        } finally {
            statement.close();
        }
    }

    private int removeData(String ownerPkg, String ownerSign, String key) {
        int ownerAppId = queryAppId(ownerPkg, ownerSign);
        if (ownerAppId < 0) {
            return 0;
        }
        String selection = SELECTION_DATA_APP_ID + " AND " + SELECTION_DATA_KEY;
        String appId = String.valueOf(ownerAppId);
        String[] selectionArgs = new String[] {appId, key};
        return mDBHelper
                .getWritableDatabase()
                .delete(ExchangeDatabaseHelper.Table.DATA, selection, selectionArgs);
    }

    private int clear(String ownerPkg, String ownerSign) {
        int appId = queryAppId(ownerPkg, ownerSign);
        if (appId < 0) {
            return 0;
        }
        return clear(appId);
    }

    private int clear(String ownerPkg) {
        int[] appIds = queryAppIds(ownerPkg);
        if (appIds == null || appIds.length == 0) {
            return 0;
        }
        int result = 0;
        for (int appId : appIds) {
            result += clear(appId);
        }
        return result;
    }

    private int clear(int appId) {
        int count =
                mDBHelper
                        .getWritableDatabase()
                        .delete(
                                ExchangeDatabaseHelper.Table.DATA,
                                SELECTION_DATA_APP_ID,
                                new String[] {String.valueOf(appId)});
        return count;
    }

    private int deletePermission(
            String ownerPkg, String ownerSign, String revokePkg, String revokeKey) {
        int ownerAppId = queryAppId(ownerPkg, ownerSign);
        if (ownerAppId < 0) {
            throw new IllegalArgumentException("No permission data");
        }
        int[] revokePkgIds = queryAppIds(revokePkg);
        if (revokePkgIds == null || revokePkgIds.length == 0) {
            return 0;
        }

        boolean isAll = TextUtils.isEmpty(revokeKey);
        StringBuilder selections = new StringBuilder();
        int l = revokePkgIds.length + 1 + (isAll ? 0 : 1);
        String[] selectionArgs = new String[l];
        selections.append("(");
        for (int i = 0; i < revokePkgIds.length; i++) {
            if (i != 0) {
                selections.append(" OR ");
            }
            selections.append(SELECTION_PERMISSION_GRANT_APP_ID);
            selectionArgs[i] = String.valueOf(revokePkgIds[i]);
        }
        selections.append(") AND ");
        selections.append(ExchangeDatabaseHelper.PermissionColumns.DATA_ID);
        selections.append(" IN (SELECT ");
        selections.append(ExchangeDatabaseHelper.DataColumns._ID);
        selections.append(" FROM ");
        selections.append(ExchangeDatabaseHelper.Table.DATA);
        selections.append(" WHERE ");
        selections.append(SELECTION_DATA_APP_ID);
        selectionArgs[revokePkgIds.length] = String.valueOf(ownerAppId);
        if (!isAll) {
            selections.append(" AND ");
            selections.append(SELECTION_DATA_KEY);
            selectionArgs[revokePkgIds.length + 1] = revokeKey;
        }
        selections.append(")");

        int result =
                mDBHelper
                        .getWritableDatabase()
                        .delete(ExchangeDatabaseHelper.Table.PERMISSION, selections.toString(),
                                selectionArgs);
        return result;
    }
}
