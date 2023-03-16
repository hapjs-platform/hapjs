/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.permission.RuntimePermissionProvider;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@FeatureExtensionAnnotation(
        name = Alarm.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Alarm.ACTION_SET_ALARM, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Alarm.ACTION_GET_PROVIDER, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Alarm.ACTION_IS_AVAILABLE, mode = FeatureExtension.Mode.ASYNC)
        })
public class Alarm extends FeatureExtension {
    public static final String PARAM_HOUR = "hour";
    public static final String PARAM_MINUTE = "minute";
    public static final String PARAM_MESSAGE = "message";
    public static final String PARAM_DAYS = "days";
    public static final String PARAM_VIBRATE = "vibrate";
    public static final String PARAM_RINGTONE = "ringtone";
    public static final String PARAM_IS_AVAILABLE = "isAvailable";
    protected static final String FEATURE_NAME = "system.alarm";
    protected static final String ACTION_SET_ALARM = "setAlarm";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    protected static final String ACTION_IS_AVAILABLE = "isAvailable";
    private static final String TAG = "AlarmFeature";
    private static final int PARAM_DEFAULT = -1;
    private List<Dialog> mDialogs;
    private LifecycleListener mLifecycleListener;
    private RuntimePermissionProvider mProvider;

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        switch (action) {
            case ACTION_SET_ALARM:
                setAlarm(request);
                break;

            case ACTION_GET_PROVIDER:
                return new Response(getProvider());
            case ACTION_IS_AVAILABLE:
                invokeIsAvailable(request);
                break;
            default:
                break;
        }
        return null;
    }

    private void invokeIsAvailable(Request request) {
        Activity activity = request.getNativeInterface().getActivity();
        boolean isInstalled = isAvailable(activity);
        JSONObject result = new JSONObject();
        try {
            result.put(PARAM_IS_AVAILABLE, isInstalled);
        } catch (JSONException e) {
            Log.e(TAG, "invokeIsAvailable put result error!");
        }
        request.getCallback().callback(new Response(result));
    }

    public boolean isAvailable(Context context) {
        return true;
    }

    private void setAlarm(final Request request) throws JSONException {
        final Activity activity = request.getNativeInterface().getActivity();
        final JSONObject jsonParams = request.getJSONParams(); // throw JSONException here.
        if (!isAvailable(activity)) {
            Response response = new Response(Response.CODE_SERVICE_UNAVAILABLE, "clock service not available");
            request.getCallback().callback(response);
            return;
        }
        final ParamHolder paramHolder = new ParamHolder();
        Response response = checkAndHoldParams(request, jsonParams, paramHolder);
        if (response != null) { // null means no error response return,check passed.
            request.getCallback().callback(response);
            return;
        }
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showPermissionPrompt(request, paramHolder);
                    }
                });
    }

    private void showPermissionPrompt(final Request request, final ParamHolder paramHolder) {
        final NativeInterface nativeInterface = request.getNativeInterface();
        final Activity activity = nativeInterface.getActivity();
        if (activity.isFinishing()) {
            request.getCallback().callback(Response.ERROR);
            return;
        }
        String name = request.getApplicationContext().getName();
        String message = activity.getString(R.string.set_alarm_tip, name);

        if (mDialogs == null) {
            mDialogs = new ArrayList<>();
        }
        if (mProvider == null) {
            mProvider = ProviderManager.getDefault().getProvider(RuntimePermissionProvider.NAME);
        }
        final Dialog dialog =
                mProvider.createPermissionDialog(
                        activity,
                        null,
                        name,
                        message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    Executors.io()
                                            .execute(
                                                    () -> {
                                                        Response response =
                                                                doSetAlarm(request, paramHolder);
                                                        request.getCallback().callback(response);
                                                    });
                                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    request.getCallback().callback(Response.getUserDeniedResponse(false));
                                }
                            }
                        },
                        false);
        mDialogs.add(dialog);
        if (mLifecycleListener == null) {
            mLifecycleListener =
                    new LifecycleListener() {
                        @Override
                        public void onDestroy() {
                            for (Dialog dialog : mDialogs) {
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            }
                            nativeInterface.removeLifecycleListener(mLifecycleListener);
                        }
                    };
            nativeInterface.addLifecycleListener(mLifecycleListener);
        }
        dialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mDialogs.remove(dialog);
                    }
                });
        dialog.show();
    }

    /**
     * do set alarm when user authorized.
     */
    private Response doSetAlarm(final Request request, final ParamHolder paramHolder) {
        int result = insertAlarm(request, paramHolder);
        if (result == -1) {
            return new Response(Response.CODE_GENERIC_ERROR, "Fail to set ringtone.");
        }
        return Response.SUCCESS;
    }

    private Response checkAndHoldParams(Request request, JSONObject jsonParams,
                                        ParamHolder holder) {
        if (jsonParams == null) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid params!");
        }
        holder.hour = jsonParams.optInt(PARAM_HOUR, PARAM_DEFAULT);
        if (holder.hour < 0 || holder.hour > 23) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid param hour!");
        }
        holder.minute = jsonParams.optInt(PARAM_MINUTE, PARAM_DEFAULT);
        if (holder.minute < 0 || holder.minute > 59) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid param minute!");
        }
        holder.message = jsonParams.optString(PARAM_MESSAGE);
        if (!TextUtils.isEmpty(holder.message) && holder.message.length() > 200) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid param message!");
        }
        holder.days = jsonParams.optJSONArray(PARAM_DAYS);
        if (holder.days != null) {
            boolean validDay = true;
            if (holder.days.length() > 7) {
                validDay = false;
            } else {
                for (int i = 0; i < holder.days.length(); i++) {
                    int day = holder.days.optInt(i, PARAM_DEFAULT);
                    if (day < 0 || day > 6) {
                        validDay = false;
                        break;
                    }
                }
            }
            if (!validDay) {
                return new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid param days!");
            }
        }
        holder.vibrate = jsonParams.optBoolean(PARAM_VIBRATE);
        String ringtone = jsonParams.optString(PARAM_RINGTONE);
        return queryMediaStoreUri(request, ringtone, holder);
    }

    private Response queryMediaStoreUri(Request request, String internalUri, ParamHolder holder) {
        Activity activity = request.getNativeInterface().getActivity();
        if (TextUtils.isEmpty(internalUri)) { // if internalUri="" ,use default ringtone uri
            Uri defaultRingtoneUri =
                    RingtoneManager
                            .getActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_ALARM);
            if (defaultRingtoneUri != null) {
                holder.ringtone = defaultRingtoneUri.toString();
                return null;
            }
            return new Response(Response.CODE_GENERIC_ERROR,
                    "Failed to get system default ringtone.");
        }
        ApplicationContext appContext = request.getApplicationContext();
        Uri underlyingUri = null;
        try {
            underlyingUri = appContext.getUnderlyingUri(internalUri);
        } catch (IllegalArgumentException e) { // check file uri failed.
            Log.e(TAG, "ringtone file uri is illegal. uri is " + internalUri, e);
            return new Response(
                    Response.CODE_ILLEGAL_ARGUMENT, "invalid param ringtone.uri is " + internalUri);
        }
        if (underlyingUri != null) {
            String underlying = underlyingUri.toString();
            String externalAudioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
            String internalAudioUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI.toString();
            if (underlying.startsWith(externalAudioUri)
                    || underlying
                    .startsWith(internalAudioUri)) { // media.pickFile() --> internal://temp
                holder.ringtone = underlying;
                return null;
            }
            String fileName = getFileName(internalUri);
            if (!FileUtils.isSupportedAudioType(fileName)) {
                return new Response(Response.CODE_ILLEGAL_ARGUMENT,
                        "unSupport audio type:" + fileName);
            }
            String fileMD5 = getFileMD5(activity, underlyingUri);
            if (TextUtils.isEmpty(fileMD5)) {
                return new Response(Response.CODE_IO_ERROR, "get file md5 failed.");
            }
            File destDir = createAudioDestDir(appContext);
            if (destDir == null) {
                return new Response(Response.CODE_IO_ERROR, "create audio dir failed.");
            }

            File destFile = new File(destDir, fileMD5);
            if (!destFile.exists()) {
                if (!saveRingtoneFile(activity, destFile, underlyingUri)) {
                    return new Response(Response.CODE_GENERIC_ERROR, "Fail to set ringtone.");
                }
            }

            String relativeFolderName = File.separator +activity.getApplication().getPackageName() + File.separator;
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            String fileMD5Name = fileMD5 + "." + extension;
            Cursor cursor = null;
            try {
                String selection;
                String[] selectionArgs;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    selection = MediaStore.Audio.Media.RELATIVE_PATH + "=? AND "
                            + MediaStore.Audio.Media.DISPLAY_NAME + "=?";
                    selectionArgs = new String[]{Environment.DIRECTORY_RINGTONES + relativeFolderName, fileMD5Name};
                } else {
                    selection = MediaStore.Audio.Media.DATA + "=?";
                    selectionArgs = new String[]{destFile.getAbsolutePath()};
                }

                cursor = activity.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.MediaColumns._ID},//care only _id .
                        selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {//return file uri if audio file exists.
                    int index = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                    if (index > -1) {
                        int ringtoneID = cursor.getInt(index);
                        holder.ringtone = externalAudioUri + File.separator + ringtoneID;
                        return null;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "query media store failed.", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            //run here means never inserted to media store before.
            Uri ringtoneUri = insertMediaStore(activity, fileName, relativeFolderName, fileMD5Name, destFile);
            if (ringtoneUri != null) {
                holder.ringtone = ringtoneUri.toString();
                return null;
            }
        }
        return new Response(Response.CODE_GENERIC_ERROR, "Fail to set ringtone.");
    }

    private File createAudioDestDir(ApplicationContext appContext) {
        File destDir = new File(appContext.getMassDir(), Environment.DIRECTORY_RINGTONES);
        if (destDir.exists() || FileUtils.mkdirs(destDir)) {
            return destDir;
        }
        return null;
    }

    private boolean saveRingtoneFile(Activity activity, File destFile, Uri underlyingUri) {
        if (destFile.exists()) {
            return true;
        }
        InputStream input = null;
        try {
            input = activity.getContentResolver().openInputStream(underlyingUri);
            if (FileUtils.saveToFile(input, destFile)) {
                return true;
            } else {
                return false;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "copy file failed!", e);
            return false;
        } finally {
            FileUtils.closeQuietly(input);
        }
    }

    private String getFileMD5(Activity activity, Uri underlyingUri) {
        InputStream input = null;
        try {
            input = activity.getContentResolver().openInputStream(underlyingUri);
            if (input == null) {
                return null;
            }
            String md5 = FileHelper.getFileHashFromInputStream(input, "MD5");
            if (TextUtils.isEmpty(md5)) {
                return null;
            }
            return md5.toUpperCase();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "get file md5 failed.", e);
            return null;
        } finally {
            FileUtils.closeQuietly(input);
        }
    }

    //insert into MediaStore.
    private Uri insertMediaStore(Activity activity, String displayName, String relativeFolderName,
                                 String fileMD5Name, File destFile) {
        ContentValues values = new ContentValues();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES + relativeFolderName);
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, fileMD5Name);
        } else {
            values.put(MediaStore.Audio.Media.DATA, destFile.getAbsolutePath());
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, displayName);
        }
        values.put(MediaStore.Audio.Media.IS_ALARM, true);
        values.put(MediaStore.Audio.Media.MIME_TYPE, URLConnection.guessContentTypeFromName(displayName));
        values.put(MediaStore.Audio.Media.TITLE, FileUtils.getFileNameWithoutExtension(displayName));
        Uri mediaUri = activity.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            OutputStream output = null;
            InputStream input = null;
            try {
                output = activity.getApplication().getContentResolver().openOutputStream(mediaUri);
                input = new FileInputStream(destFile);
                if (output == null) {
                    return null;
                }
                byte[] buffer = new byte[4 * 1024];
                for (int length; (length = input.read(buffer)) != -1; ) {
                    output.write(buffer, 0, length);
                }
            } catch (Exception e) {
                Log.e(TAG, "failed to save ringtone file");
                return null;
            } finally {
                FileUtils.closeQuietly(output, input);
            }
        }
        return mediaUri;
    }

    // file name with suffix.
    private String getFileName(String uri) {
        int index = 0;
        if (uri != null) {
            index = uri.lastIndexOf("/");
        }
        if (index > 0) {
            return uri.substring(index + 1);
        } else {
            return uri;
        }
    }

    private boolean matchRegex(String src, String reg) {
        if (TextUtils.isEmpty(src)) {
            return false;
        }
        return src.matches(reg);
    }

    protected int insertAlarm(Request request, ParamHolder holder) {
        Activity activity = request.getNativeInterface().getActivity();
        ContentValues values = getContentValue(holder);
        Uri uri = getContentUri();
        if (uri == null || values == null) {
            return -1;
        }
        try {
            Uri resultUri =
                    activity.getApplicationContext().getContentResolver().insert(uri, values);
            if (resultUri != null) {
                return (int) ContentUris.parseId(resultUri);
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "insert alarm failed", e);
            return -1;
        }
    }

    protected Uri getContentUri() {
        return null;
    }

    protected ContentValues getContentValue(ParamHolder holder) {
        return null;
    }

    protected String getProvider() {
        return "";
    }

    protected class ParamHolder {
        public int hour;
        public int minute;
        public String message;
        public JSONArray days;
        public boolean vibrate = true;
        public String ringtone;
    }
}
