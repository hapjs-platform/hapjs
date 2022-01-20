/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Calendar.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = Calendar.ACTION_INSERT,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.WRITE_CALENDAR})
        })
public class Calendar extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.calendar";
    protected static final String ACTION_SELECT = "select";
    protected static final String ACTION_INSERT = "insert";
    protected static final String ACTION_UPDATE = "update";
    protected static final String ACTION_DELETE = "delete";
    protected static final String PARAM_UPDATE_VALUES = "values";
    protected static final String PARAM_ID = "id";
    protected static final String PARAM_TITLE = "title";
    protected static final String PARAM_DESCRIPTION = "description";
    protected static final String PARAM_START_DATE = "startDate";
    protected static final String PARAM_END_DATE = "endDate";
    protected static final String PARAM_TIMEZONE = "timezone";
    protected static final String PARAM_ALL_DAY = "allDay";
    protected static final String PARAM_RRULE = "rrule";
    protected static final String PARAM_ORGANIZER = "organizer";
    protected static final String PARAM_REMIND_TIME = "remindMinutes";
    private static final String TAG = "Calendar";
    private static final String RESULT_ID = PARAM_ID;
    private static final String RESULT_TITLE = PARAM_TITLE;
    private static final String RESULT_DESCRIPTION = PARAM_DESCRIPTION;
    private static final String RESULT_START_DATE = PARAM_START_DATE;
    private static final String RESULT_END_DATE = PARAM_END_DATE;
    private static final String RESULT_TIMEZONE = PARAM_TIMEZONE;
    private static final String RESULT_ALL_DAY = PARAM_ALL_DAY;
    private static final String RESULT_RRULE = PARAM_RRULE;
    private static final String RESULT_ORGANIZER = PARAM_ORGANIZER;

    private static final String[] QUERY_PROJECTION =
            new String[] {
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DESCRIPTION,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_TIMEZONE,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Events.RRULE,
                    CalendarContract.Events.ORGANIZER,
            };

    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_DESCRIPTION = 2;
    private static final int COLUMN_INDEX_DTSTART = 3;
    private static final int COLUMN_INDEX_DTEND = 4;
    private static final int COLUMN_INDEX_EVENT_TIMEZONE = 5;
    private static final int COLUMN_INDEX_ALL_DAY = 6;
    private static final int COLUMN_INDEX_RRULE = 7;
    private static final int COLUMN_INDEX_ORGANIZER = 8;

    private static final Param2Selection[] PARAM_SELECTTION_ARRAY =
            new Param2Selection[] {
                    new Param2Selection(
                            PARAM_TITLE, CalendarContract.Events.TITLE,
                            Param2Selection.TYPE_STRING),
                    new Param2Selection(
                            PARAM_DESCRIPTION, CalendarContract.Events.DESCRIPTION,
                            Param2Selection.TYPE_STRING),
                    new Param2Selection(
                            PARAM_START_DATE, CalendarContract.Events.DTSTART,
                            Param2Selection.TYPE_LONG),
                    new Param2Selection(
                            PARAM_END_DATE, CalendarContract.Events.DTEND,
                            Param2Selection.TYPE_LONG),
                    new Param2Selection(
                            PARAM_TIMEZONE, CalendarContract.Events.EVENT_TIMEZONE,
                            Param2Selection.TYPE_STRING),
                    new Param2Selection(
                            PARAM_ALL_DAY, CalendarContract.Events.ALL_DAY,
                            Param2Selection.TYPE_BOOL),
                    new Param2Selection(
                            PARAM_RRULE, CalendarContract.Events.RRULE,
                            Param2Selection.TYPE_STRING),
                    new Param2Selection(
                            PARAM_ORGANIZER, CalendarContract.Events.ORGANIZER,
                            Param2Selection.TYPE_STRING)
            };

    private String mPackageName;
    private static final int DEFAULT_CALENDAR_ID = 1;
    private int mCalendarId = -1;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        mPackageName = request.getApplicationContext().getPackage();
        if (ACTION_SELECT.equals(action)) {
            select(request);
        } else if (ACTION_INSERT.equals(action)) {
            insert(request);
        } else if (ACTION_UPDATE.equals(action)) {
            update(request);
        } else if (ACTION_DELETE.equals(action)) {
            delete(request);
        }
        return null;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @SuppressWarnings("MissingPermission")
    private void select(Request request) throws JSONException {
        if (!verifyParamsNotNull(request)) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        StringBuilder selectionBuilder = new StringBuilder();
        List<String> selectionArgList = new ArrayList<>();
        JSONObject params = new JSONObject(request.getRawParams());

        Uri uri = CalendarContract.Events.CONTENT_URI;
        if (params.has(PARAM_ID)) {
            long id = params.getLong(PARAM_ID);
            uri = ContentUris.withAppendedId(uri, id);
        } else {
            long startTime = params.optLong(PARAM_START_DATE, -1);
            long endTime = params.optLong(PARAM_END_DATE, -1);
            if (startTime < 0 || endTime < startTime) {
                request
                        .getCallback()
                        .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid date"));
                return;
            }
            selectionBuilder
                    .append(CalendarContract.Events.DTSTART)
                    .append(">=? AND ")
                    .append(CalendarContract.Events.DTEND)
                    .append("<=?");
            selectionArgList.add(String.valueOf(startTime));
            selectionArgList.add(String.valueOf(endTime));
            selectionBuilder.append(" AND ");
        }
        selectionBuilder.append(CalendarContract.Events.CUSTOM_APP_PACKAGE).append("=?");
        selectionArgList.add(mPackageName);

        ContentResolver contentResolver =
                request.getNativeInterface().getActivity().getContentResolver();
        Cursor cursor =
                contentResolver.query(
                        uri,
                        QUERY_PROJECTION,
                        selectionBuilder.toString(),
                        selectionArgList.toArray(new String[selectionArgList.size()]),
                        null);
        JSONArray result = new JSONArray();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    JSONObject event = new JSONObject();
                    event.put(RESULT_ID, cursor.getString(COLUMN_INDEX_ID));
                    event.put(RESULT_TITLE, cursor.getString(COLUMN_INDEX_TITLE));
                    event.put(RESULT_DESCRIPTION, cursor.getString(COLUMN_INDEX_DESCRIPTION));
                    event.put(RESULT_START_DATE, cursor.getString(COLUMN_INDEX_DTSTART));
                    event.put(RESULT_END_DATE, cursor.getString(COLUMN_INDEX_DTEND));
                    event.put(RESULT_TIMEZONE, cursor.getString(COLUMN_INDEX_EVENT_TIMEZONE));
                    event.put(RESULT_ALL_DAY, cursor.getString(COLUMN_INDEX_ALL_DAY));
                    event.put(RESULT_RRULE, cursor.getString(COLUMN_INDEX_RRULE));
                    event.put(RESULT_ORGANIZER, cursor.getString(COLUMN_INDEX_ORGANIZER));
                    result.put(event);
                }
                request.getCallback().callback(new Response(result));
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        request.getCallback().callback(Response.ERROR);
    }

    @SuppressWarnings("MissingPermission")
    private void insert(Request request) throws JSONException {
        if (!verifyParamsNotNull(request)) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        JSONObject params = new JSONObject(request.getRawParams());
        long startTime = params.optLong(PARAM_START_DATE, -1);
        long endTime = params.optLong(PARAM_END_DATE, -1);
        if (startTime < 0 || endTime < startTime) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid date"));
            return;
        }
        Uri uri = CalendarContract.Events.CONTENT_URI;
        if (params.has(PARAM_ID)) {
            long id = params.getLong(PARAM_ID);
            uri = ContentUris.withAppendedId(uri, id);
        }
        ContentResolver contentResolver =
                request.getNativeInterface().getActivity().getContentResolver();
        ContentValues values = new ContentValues();
        dealInsertArg2Values(contentResolver, values, params);
        if (values.size() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        JSONArray reminderTimeArray = params.optJSONArray(PARAM_REMIND_TIME);
        int[] remindTimes = null;
        if (reminderTimeArray != null && reminderTimeArray.length() > 0) {
            remindTimes = new int[reminderTimeArray.length()];
            for (int i = 0; i < reminderTimeArray.length(); i++) {
                remindTimes[i] = reminderTimeArray.optInt(i);
            }
        }
        values.put(
                CalendarContract.Events.HAS_ALARM,
                remindTimes != null && remindTimes.length > 0 ? "1" : "0");

        Uri insertUri = contentResolver.insert(uri, values);
        if (insertUri != null) {
            long insertId = ContentUris.parseId(insertUri);
            if (remindTimes != null && remindTimes.length >= 0) {
                values.clear();
                values.put(CalendarContract.Reminders.METHOD,
                        CalendarContract.Reminders.METHOD_ALERT);
                values.put(CalendarContract.Reminders.EVENT_ID, insertId);
                for (int remindTime : remindTimes) {
                    values.put(CalendarContract.Reminders.MINUTES, remindTime);
                    contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values);
                }
            }
            request.getCallback().callback(new Response(insertId));
            return;
        }
        request.getCallback().callback(Response.ERROR);
    }

    @SuppressWarnings("MissingPermission")
    private void update(Request request) throws JSONException {
        JSONObject params = new JSONObject(request.getRawParams());
        if (!params.has(PARAM_ID) || !params.has(PARAM_UPDATE_VALUES)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no id or values"));
            return;
        }
        long id = params.getLong(PARAM_ID);
        ContentResolver contentResolver =
                request.getNativeInterface().getActivity().getContentResolver();
        ContentValues values = new ContentValues();
        params = params.getJSONObject(PARAM_UPDATE_VALUES);
        dealInsertArg2Values(contentResolver, values, params);
        if (values.size() == 0) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no values"));
            return;
        }
        JSONArray reminderTimeArray = params.optJSONArray(PARAM_REMIND_TIME);
        int[] remindTimes = null;
        if (reminderTimeArray != null && reminderTimeArray.length() > 0) {
            remindTimes = new int[reminderTimeArray.length()];
            for (int i = 0; i < reminderTimeArray.length(); i++) {
                remindTimes[i] = reminderTimeArray.optInt(i);
            }
        }
        values.put(
                CalendarContract.Events.HAS_ALARM,
                remindTimes != null && remindTimes.length > 0 ? "1" : "0");
        String where =
                new StringBuilder()
                        .append(CalendarContract.Events._ID)
                        .append("=? AND ")
                        .append(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                        .append("=?")
                        .toString();
        String[] selectionArgs = new String[] {String.valueOf(id), mPackageName};
        int count =
                contentResolver
                        .update(CalendarContract.Events.CONTENT_URI, values, where, selectionArgs);
        if (remindTimes != null && remindTimes.length >= 0) {
            contentResolver.delete(
                    CalendarContract.Reminders.CONTENT_URI,
                    CalendarContract.Reminders.EVENT_ID + "=?",
                    new String[] {String.valueOf(id)});
            values.clear();
            values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            values.put(CalendarContract.Reminders.EVENT_ID, id);
            for (int remindTime : remindTimes) {
                values.put(CalendarContract.Reminders.MINUTES, remindTime);
                contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values);
            }
        }
        request.getCallback().callback(new Response(count));
    }

    @SuppressWarnings("MissingPermission")
    private void delete(Request request) throws JSONException {
        JSONObject params = new JSONObject(request.getRawParams());
        if (!params.has(PARAM_ID)) {
            request.getCallback().callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no id"));
            return;
        }

        long id = params.getLong(PARAM_ID);
        String where =
                new StringBuilder()
                        .append(CalendarContract.Events._ID)
                        .append("=? AND ")
                        .append(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                        .append("=?")
                        .toString();
        String[] selectionArgs = new String[] {String.valueOf(id), mPackageName};
        ContentResolver contentResolver =
                request.getNativeInterface().getActivity().getContentResolver();
        int count =
                contentResolver.delete(CalendarContract.Events.CONTENT_URI, where, selectionArgs);
        request.getCallback().callback(new Response(count));
    }

    private boolean verifyParamsNotNull(Request request) {
        try {
            JSONObject params = new JSONObject(request.getRawParams());
            if (params != null && params.length() != 0) {
                return true;
            }
        } catch (JSONException e) {
            Log.e(TAG, "verify params error", e);
        }
        return false;
    }

    private void dealInsertArg2Values(
            ContentResolver contentResolver, ContentValues values, JSONObject params)
            throws JSONException {
        for (Param2Selection param2Selection : PARAM_SELECTTION_ARRAY) {
            if (params.has(param2Selection.paramKey)) {
                switch (param2Selection.type) {
                    case Param2Selection.TYPE_STRING:
                        values.put(param2Selection.selectionKey,
                                params.getString(param2Selection.paramKey));
                        break;
                    case Param2Selection.TYPE_LONG:
                        values.put(param2Selection.selectionKey,
                                params.getLong(param2Selection.paramKey));
                        break;
                    case Param2Selection.TYPE_BOOL:
                        values.put(
                                param2Selection.selectionKey,
                                params.getBoolean(param2Selection.paramKey) ? "1" : "0");
                        break;
                    default:
                        break;
                }
            }
        }
        if (!values.containsKey(CalendarContract.Events.CALENDAR_ID)) {
            values.put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId(contentResolver));
        }
        if (!values.containsKey(CalendarContract.Events.CALENDAR_TIME_ZONE)) {
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        }
        if (!values.containsKey(CalendarContract.Events.CUSTOM_APP_PACKAGE)) {
            values.put(CalendarContract.Events.CUSTOM_APP_PACKAGE, mPackageName);
        }
    }

    @SuppressWarnings("MissingPermission")
    private int getDefaultCalendarId(ContentResolver contentResolver) {
        if (mCalendarId > 0) {
            return mCalendarId;
        }
        Cursor calendarsCursorWithPrimary = null;
        Cursor calendarsCursorWithoutPrimary = null;
        try {
            calendarsCursorWithPrimary =
                    contentResolver.query(
                            CalendarContract.Calendars.CONTENT_URI,
                            null,
                            CalendarContract.Calendars.VISIBLE
                                    + " = 1 AND "
                                    + CalendarContract.Calendars.IS_PRIMARY
                                    + "=1",
                            null,
                            CalendarContract.Calendars._ID + " ASC");
            if (calendarsCursorWithPrimary == null) {
                return DEFAULT_CALENDAR_ID;
            }
            if (calendarsCursorWithPrimary.getCount() <= 0) {
                calendarsCursorWithoutPrimary =
                        contentResolver.query(
                                CalendarContract.Calendars.CONTENT_URI,
                                null,
                                CalendarContract.Calendars.VISIBLE + " = 1",
                                null,
                                CalendarContract.Calendars._ID + " ASC");
                if (calendarsCursorWithoutPrimary == null) {
                    return DEFAULT_CALENDAR_ID;
                }
                if (calendarsCursorWithoutPrimary.getCount() > 0) {
                    calendarsCursorWithoutPrimary.moveToNext();
                    mCalendarId =
                            calendarsCursorWithoutPrimary.getInt(
                                    calendarsCursorWithoutPrimary
                                            .getColumnIndex(CalendarContract.Calendars._ID));
                    return mCalendarId;
                }
            } else {
                calendarsCursorWithPrimary.moveToNext();
                mCalendarId =
                        calendarsCursorWithPrimary.getInt(
                                calendarsCursorWithPrimary
                                        .getColumnIndex(CalendarContract.Calendars._ID));
                return mCalendarId;
            }
        } catch (Exception e) {
            Log.e(TAG, "getDefaultCalendarId: ", e);
        } finally {
            FileUtils.closeQuietly(calendarsCursorWithPrimary);
            FileUtils.closeQuietly(calendarsCursorWithoutPrimary);
        }
        return DEFAULT_CALENDAR_ID;
    }

    private static class Param2Selection {

        static final int TYPE_STRING = 0;
        static final int TYPE_LONG = 1;
        static final int TYPE_BOOL = 2;
        String paramKey;
        String selectionKey;
        int type;

        public Param2Selection(String paramKey, String selectionKey, int type) {
            this.paramKey = paramKey;
            this.selectionKey = selectionKey;
            this.type = type;
        }
    }
}
