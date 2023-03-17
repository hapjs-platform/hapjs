/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.features.preview.PreviewImageDialog;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.ProviderManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Media.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = Media.ACTION_TAKE_PHOTO,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.CAMERA}),
                @ActionAnnotation(
                        name = Media.ACTION_TAKE_VIDEO,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.CAMERA}),
                @ActionAnnotation(
                        name = Media.ACTION_PICK_IMAGE,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_PICK_IMAGES,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_PICK_VIDEO,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_PICK_VIDEOS,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_PICK_FILE,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_PICK_FILES,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_SAVE_TO_ALBUM,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        residentType = ActionAnnotation.ResidentType.USEABLE),
                @ActionAnnotation(
                        name = Media.ACTION_GET_RINGTONE,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Media.ACTION_SET_RINGTONE,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE}),
                @ActionAnnotation(name = Media.ACTION_PREVIEW, mode = FeatureExtension.Mode.ASYNC)
        })
public class Media extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.media";
    protected static final String ACTION_TAKE_PHOTO = "takePhoto";
    protected static final String ACTION_TAKE_VIDEO = "takeVideo";
    protected static final String ACTION_PICK_IMAGE = "pickImage";
    protected static final String ACTION_PICK_IMAGES = "pickImages";
    protected static final String ACTION_PICK_VIDEO = "pickVideo";
    protected static final String ACTION_PICK_VIDEOS = "pickVideos";
    protected static final String ACTION_PICK_FILE = "pickFile";
    protected static final String ACTION_PICK_FILES = "pickFiles";
    protected static final String ACTION_SAVE_TO_ALBUM = "saveToPhotosAlbum";
    protected static final String ACTION_GET_RINGTONE = "getRingtone";
    protected static final String ACTION_SET_RINGTONE = "setRingtone";
    protected static final String ACTION_PREVIEW = "previewImage";
    protected static final String PARAMS_URI = "uri";
    protected static final String RESULT_URI = "uri";
    protected static final String RESULT_NAME = "name";
    protected static final String RESULT_SIZE = "size";
    protected static final String RESULT_URIS = "uris";
    protected static final String RESULT_FILES = "files";
    protected static final String PARAMS_TYPE = "type";
    protected static final String PARAMS_TITLE = "title";
    protected static final String PARAMS_MAX_DURATION = "maxDuration";
    protected static final String PARAMS_FOLDER_NAME = "folderName";
    protected static final int MAX_DURATION = 60;
    protected static final int REQUEST_CODE_BASE = getRequestBaseCode();
    protected static final int REQUEST_TAKE_PHOTO = REQUEST_CODE_BASE + 1;
    protected static final int REQUEST_TAKE_VIDEO = REQUEST_CODE_BASE + 2;
    protected static final int REQUEST_PICK_IMAGE = REQUEST_CODE_BASE + 3;
    protected static final int REQUEST_PICK_IMAGES = REQUEST_CODE_BASE + 4;
    protected static final int REQUEST_PICK_VIDEO = REQUEST_CODE_BASE + 5;
    protected static final int REQUEST_PICK_VIDEOS = REQUEST_CODE_BASE + 6;
    protected static final int REQUEST_PICK_FILE = REQUEST_CODE_BASE + 7;
    protected static final int REQUEST_PICK_FILES = REQUEST_CODE_BASE + 8;
    private static final String TAG = "Media";
    private static final String TYPE_RINGTONE = "ringtone";
    private static final String TYPE_ALARM = "alarm";
    private static final String TYPE_NOTIFICATION = "notification";
    private static final int CODE_FILE_NOT_EXIST_ERROR = Response.CODE_FEATURE_ERROR + 1;

    private static final String MIME_PREFFIX_IMAGE = "image";
    private static final String MIME_PREFFIX_VIDEO = "video";

    private static String[] illegalCharacters =
            new String[] {".", "\\", ":", "*", "?", "\"", "<", ">", "|"};

    private static String checkIllegalCharacter(String name) {
        for (String s : illegalCharacters) {
            if (name.contains(s)) {
                return "folderName " + name + " is contains illegal characters";
            }
        }
        return null;
    }

    private static String checkNumberOfFolder(String name) {
        if (name.contains("/")) {
            String[] folderArray = name.split("/");
            int count = 0;
            for (String folderName : folderArray) {
                if (!TextUtils.isEmpty(folderName)) {
                    count++;
                    if (count > 10) {
                        return "number of folder is too many";
                    }
                    if (folderName.length() > 50) {
                        return "folderName " + folderName + " is too long";
                    }
                }
            }
        } else {
            if (name.length() > 50) {
                return "folderName " + name + " is too long";
            }
        }
        return null;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_TAKE_PHOTO.equals(action)) {
            takePhoto(request);
        } else if (ACTION_TAKE_VIDEO.equals(action)) {
            takeVideo(request);
        } else if (ACTION_PICK_IMAGE.equals(action)) {
            pickImage(request);
        } else if (ACTION_PICK_IMAGES.equals(action)) {
            pickImages(request);
        } else if (ACTION_PICK_VIDEO.equals(action)) {
            pickVideo(request);
        } else if (ACTION_PICK_VIDEOS.equals(action)) {
            pickVideos(request);
        } else if (ACTION_PICK_FILE.equals(action)) {
            pickFile(request);
        } else if (ACTION_PICK_FILES.equals(action)) {
            pickFiles(request);
        } else if (ACTION_SAVE_TO_ALBUM.equals(action)) {
            saveToPhotoAlbum(request);
        } else if (ACTION_GET_RINGTONE.equals(action)) {
            getRingtone(request);
        } else if (ACTION_SET_RINGTONE.equals(action)) {
            setRingtone(request);
        } else if (ACTION_PREVIEW.equals(action)) {
            previewImage(request);
        }
        return null;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private void takePhoto(Request request) throws IOException {
        File scrapFile = getScrapFile(request, "photo", ".jpg");
        Uri scrapUri = getContentUri(request.getNativeInterface().getActivity(), scrapFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, scrapUri);
        Activity activity = request.getNativeInterface().getActivity();
        intent.setClipData(ClipData.newUri(activity.getContentResolver(), "takePhoto", scrapUri));
        //noinspection deprecation
        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        takeMedia(request, intent, scrapFile, REQUEST_TAKE_PHOTO);
    }

    private void takeVideo(Request request) throws IOException, SerializeException {
        SerializeObject params = request.getSerializeParams();
        File scrapFile = getScrapFile(request, "video", ".mp4");
        Uri scrapUri = getContentUri(request.getNativeInterface().getActivity(), scrapFile);
        int maxDuration = params.optInt(PARAMS_MAX_DURATION, MAX_DURATION);
        if (maxDuration <= 0) {
            request
                    .getCallback()
                    .callback(
                            new Response(Response.CODE_ILLEGAL_ARGUMENT,
                                    "invalid maxDuration:" + maxDuration));
            return;
        }
        maxDuration = maxDuration > MAX_DURATION ? MAX_DURATION : maxDuration;
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, scrapUri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, maxDuration);
        Activity activity = request.getNativeInterface().getActivity();
        intent.setClipData(ClipData.newUri(activity.getContentResolver(), "takeVideo", scrapUri));
        //noinspection deprecation
        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        takeMedia(request, intent, scrapFile, REQUEST_TAKE_VIDEO);
    }

    protected void pickImage(Request request) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        //noinspection deprecation
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("image/*");
        takeMedia(request, intent, null, REQUEST_PICK_IMAGE);
    }

    protected void pickImages(Request request) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //noinspection deprecation
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        takeMedia(request, intent, null, REQUEST_PICK_IMAGES, true);
    }

    protected void pickVideo(Request request) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        //noinspection deprecation
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("video/*");
        takeMedia(request, intent, null, REQUEST_PICK_VIDEO);
    }

    protected void pickVideos(Request request) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //noinspection deprecation
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        takeMedia(request, intent, null, REQUEST_PICK_VIDEOS, true);
    }

    protected void pickFile(Request request) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        //noinspection deprecation
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        takeMedia(request, intent, null, REQUEST_PICK_FILE);
    }

    protected void pickFiles(Request request) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //noinspection deprecation
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        takeMedia(request, intent, null, REQUEST_PICK_FILES, true);
    }

    protected void saveToPhotoAlbum(Request request) throws JSONException {
        JSONObject jsonParams = new JSONObject(request.getRawParams());
        String internalUri = jsonParams.optString(PARAMS_URI);
        String folderName = jsonParams.optString(PARAMS_FOLDER_NAME);
        if (TextUtils.isEmpty(internalUri)) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, PARAMS_URI + " not define");
            request.getCallback().callback(response);
            return;
        }

        if (internalUri.endsWith("/")) {
            Response response =
                    new Response(
                            Response.CODE_ILLEGAL_ARGUMENT,
                            "internalUri " + internalUri + " can not be a directory");
            request.getCallback().callback(response);
            return;
        }

        ApplicationContext applicationContext = request.getApplicationContext();
        Uri underlyingUri = applicationContext.getUnderlyingUri(internalUri);
        if (underlyingUri == null) {
            Response response =
                    new Response(
                            Response.CODE_ILLEGAL_ARGUMENT,
                            "can not resolve internalUri " + internalUri);
            request.getCallback().callback(response);
            return;
        }

        String mimeType = URLConnection.guessContentTypeFromName(internalUri);
        if (TextUtils.isEmpty(mimeType)
                || (!mimeType.startsWith(MIME_PREFFIX_IMAGE)
                && !mimeType.startsWith(MIME_PREFFIX_VIDEO))) {
            Response response =
                    new Response(
                            Response.CODE_ILLEGAL_ARGUMENT,
                            "internalUri " + internalUri + " is not a media file");
            request.getCallback().callback(response);
            return;
        }
        if (!TextUtils.isEmpty(folderName)) {
            String error = null;
            if ((error = checkIllegalCharacter(folderName)) != null
                    || (error = checkNumberOfFolder(folderName)) != null) {
                Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, error);
                request.getCallback().callback(response);
                return;
            }
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            File dir = getSaveDir(applicationContext.getPackage(), mimeType);
            if (TextUtils.isEmpty(folderName)) {
                dir = new File(dir, applicationContext.getName());
            }
            if (!dir.exists() && !FileUtils.mkdirs(dir)) {
                Response response = new Response(Response.CODE_IO_ERROR, "Fail to make dir " + dir);
                request.getCallback().callback(response);
                return;
            }

            InputStream input = null;
            Response response;
            try {
                File file = new File(dir, getFileName(internalUri));
                Activity activity = request.getNativeInterface().getActivity();
                input = activity.getContentResolver().openInputStream(underlyingUri);
                if (FileUtils.saveToFile(input, file)) {
                    notifyMediaScanner(activity, file);
                    response = Response.SUCCESS;
                } else {
                    response = new Response(Response.CODE_IO_ERROR, "Fail to save file.");
                }
            } catch (FileNotFoundException e) {
                response = getExceptionResponse(request, e);
                Log.e(TAG, "copy file failed!", e);
            } finally {
                FileUtils.closeQuietly(input);
            }
            request.getCallback().callback(response);
        } else {
            if (TextUtils.isEmpty(folderName)) {
                folderName = applicationContext.getName();
            }
            String pkgName = applicationContext.getPackage();
            String fileName = getFileName(internalUri);
            ContentResolver resolver =
                    request.getNativeInterface().getActivity().getContentResolver();
            String relativeFolderName =
                    TextUtils.isEmpty(folderName)
                            ? File.separator + pkgName
                            : File.separator + pkgName + File.separator + folderName;
            Uri saveContentUri =
                    getSaveContentUri(resolver, fileName, relativeFolderName, mimeType);
            if (saveContentUri == null) {
                Response response =
                        new Response(Response.CODE_IO_ERROR, "Fail to get saveContentUri ");
                request.getCallback().callback(response);
                return;
            }

            Response response;
            try (OutputStream output = resolver.openOutputStream(saveContentUri);
                    InputStream input = resolver.openInputStream(underlyingUri)) {
                if (input == null || output == null) {
                    response = new Response(Response.CODE_IO_ERROR, "Fail to save file.");
                    Log.e(
                            TAG,
                            "open stream get null,outputUri" + saveContentUri + ",inputUri "
                                    + underlyingUri);
                } else {
                    byte[] buffer = new byte[4 * 1024];
                    for (int length; (length = input.read(buffer)) != -1; ) {
                        output.write(buffer, 0, length);
                    }
                    response = Response.SUCCESS;
                }
            } catch (IOException e) {
                response = new Response(Response.CODE_IO_ERROR, "Fail to save file");
                Log.e(TAG, "copy file failed!", e);
            }
            request.getCallback().callback(response);
        }
    }

    private void getRingtone(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        Activity activity = request.getNativeInterface().getActivity();
        if (jsonParams == null) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid params"));
            return;
        }
        String type = jsonParams.optString(PARAMS_TYPE);
        if (TYPE_ALARM.equals(type) && !isAvailable(activity, request.getAction())) {
            Response response = new Response(
                    Response.CODE_SERVICE_UNAVAILABLE, "clock service not available");
            request.getCallback().callback(response);
            return;
        }
        if (TYPE_RINGTONE.equals(type) || TYPE_NOTIFICATION.equals(type)
                || TYPE_ALARM.equals(type)) {
            Context context = request.getNativeInterface().getActivity();
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringType = 0;
            int volume = 0;
            switch (type) {
                case TYPE_RINGTONE:
                    ringType = RingtoneManager.TYPE_RINGTONE;
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                    break;
                case TYPE_NOTIFICATION:
                    ringType = RingtoneManager.TYPE_NOTIFICATION;
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    break;
                case TYPE_ALARM:
                    ringType = RingtoneManager.TYPE_ALARM;
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    break;
                default:
                    break;
            }
            Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, ringType);
            JSONObject result = new JSONObject();
            if (FileHelper.isUriHasFile(context, ringtoneUri)) {
                String title = RingtoneManager.getRingtone(context, ringtoneUri).getTitle(context);
                result.put(PARAMS_TITLE, title);
            } else {
                Log.e(TAG, "getRingtone ringtoneUri:" + ringtoneUri + " file is not exist");
                result.put(PARAMS_TITLE, "");
            }
            request.getCallback().callback(new Response(result));
        } else {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid type:" + type));
        }
    }

    private void setRingtone(final Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        Activity activity = request.getNativeInterface().getActivity();
        if (jsonParams == null) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid params"));
            return;
        }
        String ringtoneUri = jsonParams.optString(PARAMS_URI);
        final String type = jsonParams.optString(PARAMS_TYPE);
        final String title = jsonParams.optString(PARAMS_TITLE);
        if (TextUtils.isEmpty(ringtoneUri)) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, PARAMS_URI + " not define");
            request.getCallback().callback(response);
            return;
        }

        if (TYPE_ALARM.equals(type) && !isAvailable(activity, request.getAction())) {
            Response response = new Response(
                    Response.CODE_SERVICE_UNAVAILABLE, "clock service not available");
            request.getCallback().callback(response);
            return;
        }

        if (ringtoneUri.endsWith("/")) {
            Response response =
                    new Response(
                            Response.CODE_ILLEGAL_ARGUMENT,
                            "uri " + ringtoneUri + " can not be a directory");
            request.getCallback().callback(response);
            return;
        }
        final Uri underlyingUri = request.getApplicationContext().getUnderlyingUri(ringtoneUri);
        if (underlyingUri == null || !FileHelper.isUriHasFile(activity, underlyingUri)) {
            Log.e(TAG, "setRingtone ringtoneUri:" + ringtoneUri + " file is not exist");
            Response response =
                    new Response(CODE_FILE_NOT_EXIST_ERROR, "can not resolve uri " + ringtoneUri);
            request.getCallback().callback(response);
            return;
        }
        if (TYPE_RINGTONE.equals(type) || TYPE_NOTIFICATION.equals(type)
                || TYPE_ALARM.equals(type)) {
            activity.runOnUiThread(
                    new Runnable() {

                        @Override
                        public void run() {
                            showPermissionDialog(request, underlyingUri, type, title);
                        }
                    });
        } else {
            Log.e(TAG, "setRingtone invalid type:" + type);
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid type:" + type));
        }
    }

    public boolean isAvailable(Context context, String action) {
        return true;
    }

    protected void setActualDefaultRingtone(Context context, int ringType, Uri ringtoneUri) {
        RingtoneManager.setActualDefaultRingtoneUri(context, ringType, ringtoneUri);
        if (RingtoneManager.TYPE_RINGTONE == ringType) {
            setActualDefaultRingtoneToSim2(context, ringtoneUri);
        }
    }

    protected void setActualDefaultRingtoneToSim2(Context context, Uri ringtoneUri) {
        // this method is to set phone ringtone(RingtoneManager.TYPE_RINGTONE) to SIM2
        // you should implement it base on your ROM
    }

    private void showPermissionDialog(
            final Request request, final Uri underUri, final String type, final String title) {
        final Activity activity = request.getNativeInterface().getActivity();
        if (activity.isFinishing()) {
            return;
        }
        String message =
                activity.getString(
                        R.string.features_media_ringtone_msg,
                        request.getApplicationContext().getName());
        RuntimePermissionProvider provider =
                ProviderManager.getDefault().getProvider(RuntimePermissionProvider.NAME);
        final Dialog dialog =
                provider.createPermissionDialog(
                        activity,
                        null,
                        request.getApplicationContext().getName(),
                        message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (!Settings.System.canWrite(activity)) {
                                            requestPermission(activity, request, underUri, type, title);
                                        } else {
                                            new SetRingtoneTask(request, underUri, type, title).execute();
                                        }
                                    } else {
                                        new SetRingtoneTask(request, underUri, type, title).execute();
                                    }
                                } else {
                                    request.getCallback().callback(Response.getUserDeniedResponse(false));
                                }
                            }
                        },
                        false);
        request
                .getNativeInterface()
                .addLifecycleListener(
                        new LifecycleListener() {
                            @Override
                            public void onDestroy() {
                                super.onDestroy();
                                dialog.dismiss();
                                request.getNativeInterface().removeLifecycleListener(this);
                            }
                        });
        dialog.show();
    }

    private void requestPermission(
            Activity activity, Request request, final Uri underUri, final String type, final String title) {
        final NativeInterface nativeInterface = request.getNativeInterface();
        nativeInterface.addLifecycleListener(new LifecycleListener() {
            @Override
            public void onResume() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(activity)) {
                    new SetRingtoneTask(request, underUri, type, title).execute();
                    request.getNativeInterface().removeLifecycleListener(this);
                }
            }

            @Override
            public void onDestroy() {
                request.getNativeInterface().removeLifecycleListener(this);
                super.onDestroy();
            }
        });
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private Uri getContentUri(Context context, File file) throws IOException {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".file", file);
    }

    private File getScrapFile(Request request, String prefix, String suffix) throws IOException {
        File cacheDir = request.getApplicationContext().getCacheDir();
        return File.createTempFile(prefix, suffix, cacheDir);
    }

    protected void takeMedia(final Request request, Intent intent, final File file,
                             final int code) {
        takeMedia(request, intent, file, code, false);
    }

    protected void takeMedia(
            final Request request,
            Intent intent,
            final File file,
            final int code,
            final boolean allowMultiple) {
        request
                .getNativeInterface()
                .addLifecycleListener(
                        new LifecycleListener() {
                            @Override
                            public void onDestroy() {
                                request.getNativeInterface().removeLifecycleListener(this);
                                super.onDestroy();
                            }

                            @Override
                            public void onPageChange() {
                                request.getNativeInterface().removeLifecycleListener(this);
                                super.onPageChange();
                            }

                            @Override
                            public void onActivityResult(int requestCode, int resultCode,
                                                         Intent data) {
                                if (requestCode != code) {
                                    super.onActivityResult(requestCode, resultCode, data);
                                    return;
                                }

                                request.getNativeInterface().removeLifecycleListener(this);

                                if (resultCode != Activity.RESULT_OK) {
                                    if (resultCode == Activity.RESULT_CANCELED) {
                                        request.getCallback().callback(Response.CANCEL);
                                    } else {
                                        request.getCallback().callback(Response.ERROR);
                                    }
                                    return;
                                }

                                Response response;
                                if (allowMultiple) {
                                    List<InternalFile> internalFileList =
                                            getInternalFiles(request, data);
                                    if (internalFileList != null) {
                                        response = new Response(makeResult(internalFileList));
                                    } else {
                                        response = Response.ERROR;
                                    }
                                } else {
                                    InternalFile resultFile =
                                            getInternalFile(request, file,
                                                    data == null ? null : data.getData());
                                    if (resultFile != null) {
                                        response = new Response(makeResult(resultFile));
                                    } else {
                                        response = Response.ERROR;
                                    }
                                }
                                request.getCallback().callback(response);
                            }
                        });
        Intent finalIntent = intent;
        // 各厂商定制化以后的intent可能指向平台私有的(exported = false)Activity
        // 而chooser dialog在android8.0上找不到这些私有的Activity
        // 因此针对有明确包名或者目标组件名的intent，不使用createChooser进行中间跳转
        if (TextUtils.isEmpty(intent.getPackage()) && intent.getComponent() == null) {
            finalIntent = Intent.createChooser(intent, null);
        }
        request.getNativeInterface().startActivityForResult(finalIntent, code);
    }

    @Nullable
    protected List<InternalFile> getInternalFiles(Request request, Intent data) {
        List<InternalFile> resultFileList = null;
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            resultFileList = new ArrayList<>();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                if (item == null) {
                    continue;
                }
                InternalFile file = getInternalFile(request, null, item.getUri());
                resultFileList.add(file);
            }
        } else if (data.getData() != null) {
            // 适用于响应的 Activity 忽略EXTRA_ALLOW_MULTIPLE的情况
            resultFileList = Arrays.asList(getInternalFile(request, null, data.getData()));
        }
        return resultFileList;
    }

    private InternalFile getInternalFile(Request request, File file, Uri underlyingUri) {
        String internalUri = null;
        String name;
        long size;
        if (file == null) {
            internalUri = request.getApplicationContext().getInternalUri(underlyingUri);
            size = getFileSize(request, underlyingUri);
            name = getFileName(internalUri);
        } else {
            internalUri = request.getApplicationContext().getInternalUri(file);
            size = file.length();
            name = file.getName();
        }
        if (internalUri != null) {
            return new InternalFile(internalUri, name, size);
        }
        return null;
    }

    private JSONObject makeResult(InternalFile file) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_URI, file.uri);
            result.put(RESULT_NAME, file.name);
            result.put(RESULT_SIZE, file.size);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private JSONObject makeResult(List<InternalFile> internalFiles) {
        JSONObject result = new JSONObject();
        try {
            JSONArray uriArray = new JSONArray();
            JSONArray fileArray = new JSONArray();
            for (InternalFile file : internalFiles) {
                uriArray.put(file.uri);
                JSONObject fileJSON = new JSONObject();
                fileJSON.put(RESULT_URI, file.uri);
                fileJSON.put(RESULT_NAME, file.name);
                fileJSON.put(RESULT_SIZE, file.size);
                fileArray.put(fileJSON);
            }
            result.put(RESULT_URIS, uriArray);
            result.put(RESULT_FILES, fileArray);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

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

    private long getFileSize(Request request, Uri underlyingUri) {
        long size = -1;
        if (underlyingUri == null) {
            return size;
        }
        try {
            ParcelFileDescriptor fd =
                    request
                            .getNativeInterface()
                            .getActivity()
                            .getContentResolver()
                            .openFileDescriptor(underlyingUri, "r");
            if (fd != null) {
                size = fd.getStatSize();
                fd.close();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + underlyingUri, e);
        } catch (IOException e) {
            Log.e(TAG, "io exception occurs: " + underlyingUri, e);
        }
        return size;
    }

    private File getSaveDir(String pkgName, String mimeType) {
        String dirType = Environment.DIRECTORY_DOCUMENTS;
        if (!TextUtils.isEmpty(mimeType)) {
            if (mimeType.startsWith(MIME_PREFFIX_IMAGE)) {
                dirType = Environment.DIRECTORY_PICTURES;
            } else if (mimeType.startsWith(MIME_PREFFIX_VIDEO)) {
                dirType = Environment.DIRECTORY_MOVIES;
            }
        }
        File parentDir = Environment.getExternalStoragePublicDirectory(dirType);
        return new File(parentDir, pkgName);
    }

    private void notifyMediaScanner(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        context.sendBroadcast(intent);
    }

    private void previewImage(final Request request) {
        request
                .getNativeInterface()
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject jsonParams = request.getJSONParams();
                                    PreviewImageDialog dialog =
                                            new PreviewImageDialog(
                                                    request.getNativeInterface().getActivity());
                                    JSONArray urisJson = jsonParams.optJSONArray("uris");
                                    String currentUri = jsonParams.optString("current");

                                    int current = -1;
                                    if (null != urisJson && urisJson.length() > 0) {
                                        if (null == currentUri) {
                                            current = 0;
                                        } else {
                                            try {
                                                current = Integer.parseInt(currentUri);
                                            } catch (NumberFormatException e) {
                                                for (int i = 0; i < urisJson.length(); i++) {
                                                    if (currentUri.equals(urisJson.getString(i))) {
                                                        current = i;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        request
                                                .getCallback()
                                                .callback(
                                                        new Response(
                                                                Response.CODE_ILLEGAL_ARGUMENT,
                                                                "Parameter of 'uris' error."));
                                        return;
                                    }

                                    if (current >= 0 && current < urisJson.length()) {
                                        List<String> uris = new ArrayList<>();
                                        for (int i = 0; i < urisJson.length(); i++) {
                                            uris.add(urisJson.getString(i));
                                        }
                                        dialog.setParams(request.getApplicationContext(), current,
                                                uris);
                                        dialog.setRequest(request);
                                        dialog.show();
                                        request.getCallback().callback(Response.SUCCESS);
                                    } else {
                                        request
                                                .getCallback()
                                                .callback(
                                                        new Response(
                                                                Response.CODE_ILLEGAL_ARGUMENT,
                                                                "Parameter of 'current' error."));
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "JSON params parse error.", e);
                                    request.getCallback().callback(Response.ERROR);
                                }
                            }
                        });
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getSaveContentUri(
            ContentResolver resolver, String fileName, String relativeFolderName, String mimeType) {
        ContentValues values = new ContentValues();
        Uri external;
        if (mimeType.startsWith(MIME_PREFFIX_IMAGE)) {
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + relativeFolderName);
            external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith(MIME_PREFFIX_VIDEO)) {
            values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
            values.put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + relativeFolderName);
            external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            values.put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + relativeFolderName);
            external = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        }

        if (external != null && values.size() != 0) {
            return resolver.insert(external, values);
        }
        return null;
    }

    private static class InternalFile {
        String uri;
        String name;
        long size;

        InternalFile(String uri, String name, long size) {
            this.uri = uri;
            this.name = name;
            this.size = size;
        }
    }

    private class SetRingtoneTask extends AsyncTask<Void, Void, Response> {

        private Request mRequest;
        private Uri mUnderUri;
        private Context mContext;
        private String mType;
        private String mTitle;
        private String mFileName;

        public SetRingtoneTask(Request request, Uri underUri, String type, String title) {
            this.mRequest = request;
            this.mContext = request.getNativeInterface().getActivity();
            this.mUnderUri = underUri;
            this.mType = type;
            this.mTitle = title;
        }

        private String copyFileToSdcard() {
            InputStream input = null;
            try {
                File destDir =
                        new File(
                                mRequest.getApplicationContext().getMassDir(),
                                Environment.DIRECTORY_RINGTONES);
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                input = mContext.getContentResolver().openInputStream(mUnderUri);
                String destFileName = FileHelper.getFileHashFromInputStream(input, "MD5");
                if (TextUtils.isEmpty(destFileName)) {
                    return null;
                }
                destFileName = destFileName.toUpperCase();
                File destFile = new File(destDir, destFileName);
                if (destFile.exists()) {
                    return destFile.getAbsolutePath();
                }
                input = mContext.getContentResolver().openInputStream(mUnderUri);
                if (FileUtils.saveToFile(input, destFile)) {
                    return destFile.getAbsolutePath();
                }
            } catch (Exception e) {
                Log.e(TAG, "setRingtone copy file fail", e);
            } finally {
                FileUtils.closeQuietly(input);
            }
            return null;
        }

        @Override
        protected Response doInBackground(Void... voids) {
            String filePath = FileHelper.getFileFromContentUri(mContext, mUnderUri);
            if (!TextUtils.isEmpty(filePath)) {
                mFileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            } else {
                Log.e(TAG, "underUri:" + mUnderUri + " can not get file path");
                return new Response(
                        Response.CODE_ILLEGAL_ARGUMENT,
                        "uri: " + mUnderUri + " can not get file path");
            }
            if (!FileUtils.isSupportedAudioType(mFileName)) {
                Log.e(TAG, "unSupport audio type:" + mFileName);
                return new Response(Response.CODE_ILLEGAL_ARGUMENT,
                        "unSupport audio type:" + mFileName);
            }
            if (filePath.startsWith("/data/")) {
                filePath = copyFileToSdcard();
                if (TextUtils.isEmpty(filePath)) {
                    Log.e(TAG, "underUri:" + mUnderUri + " copy file fail!!!");
                    return new Response(
                            Response.CODE_ILLEGAL_ARGUMENT,
                            "uri: " + mUnderUri + " can not copy file");
                }
            }
            int ringType = 0;
            ContentValues values = new ContentValues();
            switch (mType) {
                case TYPE_RINGTONE:
                    ringType = RingtoneManager.TYPE_RINGTONE;
                    values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, true);
                    break;
                case TYPE_NOTIFICATION:
                    ringType = RingtoneManager.TYPE_NOTIFICATION;
                    values.put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, true);
                    break;
                case TYPE_ALARM:
                    ringType = RingtoneManager.TYPE_ALARM;
                    values.put(MediaStore.Audio.AudioColumns.IS_ALARM, true);
                    break;
                default:
                    break;
            }
            if (!TextUtils.isEmpty(mTitle)) {
                values.put(MediaStore.MediaColumns.TITLE, mTitle);
            } else {
                values.put(MediaStore.MediaColumns.TITLE, mFileName);
            }
            Uri volumeUri = MediaStore.Audio.Media.getContentUriForPath(filePath);
            Cursor cursor = null;
            try {
                cursor =
                        mContext
                                .getContentResolver()
                                .query(
                                        volumeUri,
                                        new String[] {MediaStore.MediaColumns._ID},
                                        MediaStore.MediaColumns.DATA + "=?",
                                        new String[] {filePath},
                                        null);
                if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                    Uri fileUri = Uri
                            .withAppendedPath(
                                    volumeUri,
                                    cursor.getString(0)
                            );
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values2 = new ContentValues();
                        values2.put(MediaStore.Audio.Media.IS_PENDING, 1);
                        int result = mContext
                                .getContentResolver()
                                .update(
                                        fileUri,
                                        values2,
                                        null,
                                        null
                                );
                        if (result > 0) {
                            values.put(MediaStore.Audio.Media.IS_PENDING, 0);
                        } else {
                            return Response.ERROR;
                        }
                    }

                    int result = mContext
                            .getContentResolver()
                            .update(
                                    fileUri,
                                    values,
                                    null,
                                    null
                            );
                    if (result > 0) {
                        setActualDefaultRingtone(mContext, ringType, fileUri);
                        return Response.SUCCESS;
                    }
                } else {
                    values.put(MediaStore.MediaColumns.DATA, filePath);
                    values.put(
                            MediaStore.MediaColumns.MIME_TYPE,
                            URLConnection.guessContentTypeFromName(mFileName));
                    Uri fileUri = mContext
                            .getContentResolver()
                            .insert(
                                    volumeUri,
                                    values
                            );
                    setActualDefaultRingtone(mContext, ringType, fileUri);
                    return Response.SUCCESS;
                }
            } catch (Exception e) {
                Log.e(TAG, "underUri:" + mUnderUri + " set Ringstone fail", e);
            } finally {
                FileUtils.closeQuietly(cursor);
            }
            return Response.ERROR;
        }

        @Override
        protected void onPostExecute(Response result) {
            mRequest.getCallback().callback(result);
        }
    }
}
