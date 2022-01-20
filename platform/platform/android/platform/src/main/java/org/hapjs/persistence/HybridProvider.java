/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.AbstractContentProvider;
import org.hapjs.runtime.ResourceConfig;

public class HybridProvider extends AbstractContentProvider {

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final Map<String, Integer> sUriMatchMap = new HashMap<>();
    private static String AUTHORITY;
    private static int BASE_MATCH_CODE = 0;
    private static List<Table> sTables = new ArrayList<>();
    private static List<AbstractDatabase> sDatabases = new ArrayList<>();

    public static String getAuthority(Context context) {
        if (AUTHORITY == null) {
            if (ResourceConfig.getInstance().isLoadFromLocal()) {
                AUTHORITY = context.getPackageName();
            } else {
                AUTHORITY = ResourceConfig.getInstance().getPlatform();
            }
        }
        return AUTHORITY;
    }

    public static void addURIMatch(String path, int code) {
        sUriMatchMap.put(path, code);
    }

    public static int getBaseMatchCode() {
        BASE_MATCH_CODE += 100;
        return BASE_MATCH_CODE;
    }

    private static void addTable(Table table) {
        if (table != null) {
            sTables.add(table);
        }
    }

    private static void addTable(List<Table> tables) {
        if (tables != null) {
            sTables.addAll(tables);
        }
    }

    public static void addDatabase(AbstractDatabase database) {
        if (database != null) {
            sDatabases.add(database);
        }
    }

    public static void addDatabases(List<AbstractDatabase> databases) {
        if (databases != null) {
            sDatabases.addAll(databases);
        }
    }

    @Override
    public boolean onCreate() {
        String authority = getAuthority(getContext());
        for (Map.Entry<String, Integer> entry : sUriMatchMap.entrySet()) {
            MATCHER.addURI(authority, entry.getKey(), entry.getValue());
        }
        for (AbstractDatabase database : sDatabases) {
            addTable(database.getTables());
        }
        return true;
    }

    @Override
    public Cursor doQuery(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int matchCode = MATCHER.match(uri);
        for (Table table : sTables) {
            if (table.respond(matchCode)) {
                return table.query(matchCode, uri, projection, selection, selectionArgs, sortOrder);
            }
        }
        return null;
    }

    @Override
    public Uri doInsert(Uri uri, ContentValues values) {
        int matchCode = MATCHER.match(uri);
        for (Table table : sTables) {
            if (table.respond(matchCode)) {
                return table.insert(matchCode, uri, values);
            }
        }
        return null;
    }

    @Override
    public int doUpdate(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int matchCode = MATCHER.match(uri);
        for (Table table : sTables) {
            if (table.respond(matchCode)) {
                return table.update(matchCode, uri, values, selection, selectionArgs);
            }
        }
        return 0;
    }

    @Override
    public int doDelete(Uri uri, String selection, String[] selectionArgs) {
        int matchCode = MATCHER.match(uri);
        for (Table table : sTables) {
            if (table.respond(matchCode)) {
                return table.delete(matchCode, uri, selection, selectionArgs);
            }
        }
        return 0;
    }

    @Override
    public String doGetType(Uri uri) {
        int matchCode = MATCHER.match(uri);
        for (Table table : sTables) {
            if (table.respond(matchCode)) {
                return table.getType(matchCode, uri);
            }
        }
        return null;
    }
}
