/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.launch.LauncherManager;
import org.hapjs.runtime.Runtime;

public class LauncherTable extends AbstractTable {
    public static final String NAME = "launcher";
    private static final String TAG = "LauncherTable";
    private static final String URI_PATH_BASE = "launcher";
    private static final String URI_PATH_SELECT = "select";
    private static final String URI_PATH_ACTIVE = "active";
    private static final String URI_PATH_INACTIVE = "inactive";
    private static final String URI_PATH_QUERY = "query";
    private static final String URI_PATH_RESIDENT = "resident";

    private static final int INVALID_PID = 0;
    private static final int INVALID_ACTIVE_AT = 0;
    private static final int INVALID_BORN_AT = 0;
    private static final int INVALID_RESIDENT_TYPE = 0;
    private static final int MATCH_SELECT = 0;
    private static final int MATCH_ACTIVE = 1;
    private static final int MATCH_INACTIVE = 2;
    private static final int MATCH_QUERY = 3;
    private static final int MATCH_RESIDENT = 4;
    private static final int MATCH_SIZE = 5;
    private static final int BASE_MATCH_CODE = HybridProvider.getBaseMatchCode();
    private static final String CREATE_TABLE_LAUNCHER =
            "CREATE TABLE "
                    + NAME
                    + "("
                    + Columns._ID
                    + " INTEGER PRIMARY KEY,"
                    + Columns.APP_ID
                    + " TEXT NOT NULL,"
                    + Columns.ACTIVE_AT
                    + " INTEGER,"
                    + Columns.PID
                    + " INTEGER NOT NULL DEFAULT "
                    + INVALID_PID
                    + ","
                    + Columns.BORN_AT
                    + " INTEGER NOT NULL DEFAULT "
                    + INVALID_BORN_AT
                    + ","
                    + Columns.RESIDENT_TYPE
                    + " INTEGER NOT NULL DEFAULT "
                    + INVALID_RESIDENT_TYPE
                    + ")";
    private static final String[] COLUMNS =
            new String[] {
                    Columns._ID,
                    Columns.APP_ID,
                    Columns.ACTIVE_AT,
                    Columns.PID,
                    Columns.BORN_AT,
                    Columns.RESIDENT_TYPE,
            };
    private static final int COLUMN_ID_INDEX = 0;
    private static final int COLUMN_APP_ID_INDEX = 1;
    private static final int COLUMN_ACTIVE_AT_INDEX = 2;
    private static final int COLUMN_PID_INDEX = 3;
    private static final int COLUMN_BORN_AT_INDEX = 4;
    private static final int COLUMN_RESIDENT_INDEX = 5;
    private static final int LAUNCHER_SIZE = 5;
    private static Map<String, Uri> sActionUriMap = new HashMap<>();

    static {
        HybridProvider.addURIMatch(
                URI_PATH_BASE + "/" + URI_PATH_SELECT + "/*", BASE_MATCH_CODE + MATCH_SELECT);
        HybridProvider.addURIMatch(
                URI_PATH_BASE + "/" + URI_PATH_ACTIVE, BASE_MATCH_CODE + MATCH_ACTIVE);
        HybridProvider.addURIMatch(
                URI_PATH_BASE + "/" + URI_PATH_INACTIVE, BASE_MATCH_CODE + MATCH_INACTIVE);
        HybridProvider
                .addURIMatch(URI_PATH_BASE + "/" + URI_PATH_QUERY, BASE_MATCH_CODE + MATCH_QUERY);
        HybridProvider.addURIMatch(
                URI_PATH_BASE + "/" + URI_PATH_RESIDENT, BASE_MATCH_CODE + MATCH_RESIDENT);
    }

    private HybridDatabaseHelper mDbHelper;
    private Map<String, LauncherInfo> mActives;
    private boolean mInitialized;

    public LauncherTable(HybridDatabaseHelper dbHelper) {
        mDbHelper = dbHelper;
        mActives = new LinkedHashMap<>(LAUNCHER_SIZE, 0.75f, true);
    }

    public static Uri getSelectUri(Context context) {
        return getActionUri(context, URI_PATH_SELECT);
    }

    public static Uri getActiveUri(Context context) {
        return getActionUri(context, URI_PATH_ACTIVE);
    }

    public static Uri getInactiveUri(Context context) {
        return getActionUri(context, URI_PATH_INACTIVE);
    }

    public static Uri getQueryUri(Context context) {
        return getActionUri(context, URI_PATH_QUERY);
    }

    private static Uri getActionUri(Context context, String action) {
        Uri uri = sActionUriMap.get(action);
        if (uri == null) {
            uri =
                    Uri.parse(
                            "content://"
                                    + HybridProvider.getAuthority(context)
                                    + "/"
                                    + URI_PATH_BASE
                                    + "/"
                                    + action);
            sActionUriMap.put(action, uri);
        }
        return uri;
    }

    public static Uri getResidentUri(Context context) {
        return getActionUri(context, URI_PATH_RESIDENT);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_LAUNCHER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            upgradeToV2(db);
        }
        if (oldVersion < 4) {
            upgradeToV4(db);
        }
        if (oldVersion < 7) {
            upgradeToV7(db);
        }
    }

    private synchronized Cursor query(int id) {
        Log.d(TAG, "query: id=" + id);

        refresh();

        for (LauncherInfo info : mActives.values()) {
            if (info.id == id) {
                Log.d(TAG, "query: appId=" + info.appId);
                return buildCursor(info);
            }
        }
        return null;
    }

    private synchronized Cursor query(String appId) {
        Log.d(TAG, "query: appId=" + appId);

        refresh();

        LauncherInfo info = mActives.get(appId);
        return info == null ? null : buildCursor(info);
    }

    @Override
    public Cursor query(
            int matchCode,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_SELECT:
                String appId = uri.getLastPathSegment();
                return select(appId);
            case MATCH_QUERY:
                Set<String> params = uri.getQueryParameterNames();
                if (params.contains(Columns.APP_ID)) {
                    appId = uri.getQueryParameter(Columns.APP_ID);
                    return query(appId);
                } else if (params.contains(Columns._ID)) {
                    String launcherId = uri.getQueryParameter(Columns._ID);
                    return query(Integer.parseInt(launcherId));
                }
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    public int update(
            int matchCode, Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        Integer id = values.getAsInteger(Columns._ID);
        if (null == id) {
            return 0;
        }
        switch (matchCode - BASE_MATCH_CODE) {
            case MATCH_ACTIVE: {
                String appId = values.getAsString(Columns.APP_ID);
                return active(id, appId);
            }
            case MATCH_INACTIVE: {
                String appId = values.getAsString(Columns.APP_ID);
                return inactive(id, appId);
            }
            case MATCH_RESIDENT: {
                String appId = values.getAsString(Columns.APP_ID);
                Integer residentType = values.getAsInteger(Columns.RESIDENT_TYPE);
                if (null == residentType) {
                    // param residentType in LauncherInfo`default is 0
                    residentType = 0;
                }
                return updateResident(id, appId, residentType);
            }
            default:
                break;
        }
        return 0;
    }

    @Override
    public boolean respond(int matchCode) {
        return matchCode >= BASE_MATCH_CODE && matchCode < BASE_MATCH_CODE + MATCH_SIZE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    private synchronized Cursor select(String appId) {
        Log.d(TAG, "select: appId=" + appId);

        refresh();

        LauncherInfo info = mActives.get(appId);
        if (info == null) {
            if (mActives.size() < LAUNCHER_SIZE) {
                info = new LauncherInfo(getAvailableId(), appId);
            } else {
                for (LauncherInfo li : mActives.values()) {
                    if (info == null || info.compareTo(li) > 0) {
                        info = li;
                    }
                }
                if (info == null) {
                    Log.d(TAG, "select: LauncherInfo is null, appId= " + appId);
                    return null;
                }
                mActives.remove(info.appId);
                // Kill launcher process
                if (info.pid > 0) {
                    Process.killProcess(info.pid);
                    info.reset();
                }
                info.appId = appId;
                info.bornAt = System.currentTimeMillis();
            }

            mActives.put(appId, info);
            insertOrUpdate(info);
        }

        return buildCursor(info);
    }

    private synchronized int active(int id, String appId) {
        if (TextUtils.isEmpty(appId)) {
            Log.e(TAG, "appId is empty");
            return 0;
        }
        Log.d(TAG, "active: id=" + id + ", appId=" + appId);

        ensureInitialized();

        for (LauncherInfo info : mActives.values()) {
            if (info.id == id) {
                if (!appId.equals(info.appId)) {
                    Log.e(
                            TAG,
                            "Conflict appId for launcher "
                                    + id
                                    + ", newAppId: "
                                    + appId
                                    + ", oldAppId: "
                                    + info.appId);
                    return 0;
                }
                info.bornAt = INVALID_BORN_AT;
                info.activeAt = System.currentTimeMillis();
                if (info.pid == INVALID_PID) {
                    info.pid = Binder.getCallingPid();
                }
                insertOrUpdate(info);
                // Adjust the order of specified elements to the end
                mActives.get(info.appId);
                return 1;
            }
        }

        Log.e(TAG, "Fail to active with unknown id " + id + " for app " + appId);
        return 0;
    }

    private synchronized int inactive(int id, String appId) {
        if (TextUtils.isEmpty(appId)) {
            Log.e(TAG, "appId is empty");
            return 0;
        }
        Log.d(TAG, "inactive: id=" + id + ", appId=" + appId);

        ensureInitialized();

        for (LauncherInfo info : mActives.values()) {
            if (info.id == id) {
                if (!appId.equals(info.appId)) {
                    Log.e(
                            TAG,
                            "Conflict appId for launcher "
                                    + id
                                    + ", newAppId: "
                                    + appId
                                    + ", oldAppId: "
                                    + info.appId);
                    return 0;
                }
                info.bornAt = INVALID_BORN_AT;
                info.activeAt = INVALID_ACTIVE_AT;
                insertOrUpdate(info);
                return 1;
            }
        }

        return 0;
    }

    private synchronized int updateResident(int id, String appId, int residentType) {
        if (TextUtils.isEmpty(appId)) {
            Log.e(TAG, "appId is empty");
            return 0;
        }
        Log.d(TAG, "updateResident: id=" + id + ", appId=" + appId);
        ensureInitialized();

        for (LauncherInfo info : mActives.values()) {
            if (info.id == id) {
                if (!appId.equals(info.appId)) {
                    Log.e(
                            TAG,
                            "Conflict appId for launcher "
                                    + id
                                    + ", newAppId: "
                                    + appId
                                    + ", "
                                    + "oldAppId: "
                                    + info.appId);
                    return 0;
                }
                info.residentType = residentType;
                insertOrUpdate(info);
                return 1;
            }
        }
        Log.e(TAG, "Fail to update resident info id " + id + " for app " + appId);
        return 0;
    }

    private void ensureInitialized() {
        if (mInitialized) {
            return;
        }

        Cursor cursor =
                mDbHelper.getReadableDatabase().query(NAME, COLUMNS, null, null, null, null, null);
        if (cursor != null) {
            try {
                long currentTime = System.currentTimeMillis();
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(COLUMN_ID_INDEX);
                    String appId = cursor.getString(COLUMN_APP_ID_INDEX);
                    long bornAt = cursor.getLong(COLUMN_BORN_AT_INDEX);
                    long activeAt = cursor.getLong(COLUMN_ACTIVE_AT_INDEX);
                    int residentType = cursor.getInt(COLUMN_RESIDENT_INDEX);

                    if (activeAt < currentTime && id >= 0 && id < LAUNCHER_SIZE) {
                        LauncherInfo info =
                                new LauncherInfo(id, appId, bornAt, activeAt, residentType);
                        mActives.put(appId, info);
                    } else {
                        Log.e(
                                TAG,
                                "Discard invalid launcher info: "
                                        + "id="
                                        + id
                                        + ", appId="
                                        + appId
                                        + ", activeAt="
                                        + activeAt);
                    }
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.e(TAG, "Fail to initialize: cursor is null");
        }
        mInitialized = true;
    }

    private void refresh() {
        ensureInitialized();
        updateProcess();
    }

    private void updateProcess() {
        Context context = Runtime.getInstance().getContext();
        Map<String, Integer> aliveProcesses = ProcessUtils.getAppProcesses(context);
        if (aliveProcesses == null || aliveProcesses.isEmpty()) {
            for (LauncherInfo info : mActives.values()) {
                info.reset();
            }
            return;
        }
        for (LauncherInfo info : mActives.values()) {
            String processName = LauncherManager.getLauncherProcessName(context, info.id);
            Integer pid = aliveProcesses.get(processName);
            if (pid != null) {
                // 某些厂商启动快应用时会select两次, 第二次select时pid已设置上的话, 上层判断info.isAlive=true
                // 而出错. 这里增加bornAt判断, 在启动完毕才将pid设置上以避免select判断错误.
                if (info.bornAt == INVALID_BORN_AT) {
                    info.pid = pid;
                }
            } else {
                info.reset();
            }
        }
    }

    private int getAvailableId() {
        int ids = 0;
        for (LauncherInfo info : mActives.values()) {
            ids += 1 << info.id;
        }
        for (int i = 0; i < LAUNCHER_SIZE; ++i) {
            if ((ids & (1 << i)) == 0) {
                return i;
            }
        }
        throw new IllegalStateException("No available id");
    }

    private void insertOrUpdate(LauncherInfo info) {
        ContentValues values = new ContentValues();
        values.put(Columns._ID, info.id);
        values.put(Columns.APP_ID, info.appId);
        values.put(Columns.ACTIVE_AT, info.activeAt);
        values.put(Columns.RESIDENT_TYPE, info.residentType);
        mDbHelper
                .getWritableDatabase()
                .insertWithOnConflict(NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private Cursor buildCursor(LauncherInfo info) {
        MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {Columns._ID, Columns.APP_ID, Columns.IS_ALIVE,
                                Columns.ACTIVE_AT}, 1);
        cursor.addRow(new Object[] {info.id, info.appId, info.isAlive() ? 1 : 0, info.activeAt});
        return cursor;
    }

    private void upgradeToV2(SQLiteDatabase db) {
        // 升级操作不可使用全局建表语句（CREATE_TABLE_LAUNCHER会被更新）
        db.execSQL(
                "CREATE TABLE "
                        + NAME
                        + "("
                        + Columns._ID
                        + " INTEGER PRIMARY KEY,"
                        + Columns.APP_ID
                        + " TEXT NOT NULL,"
                        + Columns.ACTIVE_AT
                        + " INTEGER"
                        + ")");
    }

    private void upgradeToV4(SQLiteDatabase db) {
        db.execSQL(
                "ALTER TABLE "
                        + NAME
                        + " ADD COLUMN "
                        + Columns.PID
                        + " INTEGER NOT NULL DEFAULT "
                        + INVALID_PID);
        db.execSQL(
                "ALTER TABLE "
                        + NAME
                        + " ADD COLUMN "
                        + Columns.BORN_AT
                        + " INTEGER NOT NULL DEFAULT "
                        + INVALID_BORN_AT);
    }

    private void upgradeToV7(SQLiteDatabase db) {
        db.execSQL(
                "ALTER TABLE "
                        + NAME
                        + " ADD COLUMN "
                        + Columns.RESIDENT_TYPE
                        + " INTEGER NOT NULL DEFAULT "
                        + INVALID_RESIDENT_TYPE);
    }

    public interface Columns extends BaseColumns {
        String APP_ID = "appId";
        String ACTIVE_AT = "activeAt";
        @Deprecated
        String PID = "pid";
        String BORN_AT = "bornAt";
        String IS_ALIVE = "isAlive";
        String RESIDENT_TYPE = "residentType";
    }

    private static class LauncherInfo {
        int id;
        String appId;
        long bornAt;
        long activeAt;
        int pid;
        // 0: none, 1:normal, 2:special
        int residentType;

        public LauncherInfo(int id, String appId) {
            this(id, appId, System.currentTimeMillis(), INVALID_ACTIVE_AT, 0);
        }

        public LauncherInfo(int id, String appId, long bornAt, long activeAt, int residentType) {
            this.id = id;
            this.appId = appId;
            this.bornAt = bornAt;
            this.activeAt = activeAt;
            this.pid = INVALID_PID;
            this.residentType = residentType;
        }

        private void reset() {
            this.bornAt = INVALID_BORN_AT;
            this.activeAt = INVALID_ACTIVE_AT;
            this.pid = INVALID_PID;
            this.residentType = INVALID_RESIDENT_TYPE;
        }

        private boolean isAlive() {
            return pid > INVALID_PID;
        }

        public int compareTo(LauncherInfo o) {
            if (bornAt != o.bornAt) {
                return Long.compare(bornAt, o.bornAt);
            }

            if (residentType > o.residentType) {
                return 1;
            }

            boolean isAlive = isAlive();

            if (isAlive != o.isAlive()) {
                return isAlive ? 1 : -1;
            }

            return Long.compare(activeAt, o.activeAt);
        }
    }
}
