/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import okhttp3.internal.http.HttpMethod;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackContextHolder;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executor;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.common.net.UserAgentHelper;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Request.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.RESIDENT_NORMAL,
        actions = {
                @ActionAnnotation(name = Request.ACTION_UPLOAD, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Request.ACTION_DOWNLOAD, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Request.ACTION_ON_DOWNLOAD_COMPLETE,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class Request extends AbstractRequest {
    protected static final String FEATURE_NAME = "system.request";
    protected static final String ACTION_UPLOAD = "upload";
    protected static final String ACTION_DOWNLOAD = "download";
    protected static final String ACTION_ON_DOWNLOAD_COMPLETE = "onDownloadComplete";
    protected static final String PARAMS_KEY_DOWNLOAD_URL = "url";
    protected static final String PARAMS_KEY_DOWNLOAD_HEADERS = "header";
    protected static final String PARAMS_KEY_DOWNLOAD_HEADERS_OLD = "headers";
    protected static final String PARAMS_KEY_DOWNLOAD_TOKEN = "token";
    protected static final String PARAMS_KEY_DOWNLOAD_DESCRIPTION = "description";
    protected static final String PARAMS_KEY_DOWNLOAD_FILENAME = "filename";
    protected static final String RESULT_KEY_DOWNLOAD_TOKEN = "token";
    protected static final String RESULT_KEY_DOWNLOAD_URI = "uri";
    protected static final String DOWNLOAD_DIR = "download";
    protected static final String USER_AGENT = "User-Agent";
    protected static final int ERROR_CODE_BASE = Response.CODE_FEATURE_ERROR;
    protected static final int ERROR_CODE_UNKNOWN = ERROR_CODE_BASE;
    protected static final int ERROR_TASK_NOT_FOUND = ERROR_CODE_BASE + 1;
    private static final String TAG = "Request";
    protected HapCompleteCallback mHapCompleteCallback;
    protected ResidentCompleteCallback mResidentCompleteCallback;

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(org.hapjs.bridge.Request request)
            throws JSONException, UnsupportedEncodingException, SerializeException {
        if (ACTION_UPLOAD.equals(request.getAction())) {

            String rawParams = request.getRawParams();
            JSONObject jsonParams = new JSONObject(rawParams);

            String method = jsonParams.optString(PARAMS_KEY_METHOD).toUpperCase();
            if (TextUtils.isEmpty(method)) {
                jsonParams.put(PARAMS_KEY_METHOD, "POST");
                request.setRawParams(jsonParams.toString());
            } else if (!HttpMethod.requiresRequestBody(method)) {
                request
                        .getCallback()
                        .callback(
                                new Response(
                                        Response.CODE_ILLEGAL_ARGUMENT,
                                        "unsupported method: " + PARAMS_KEY_METHOD));
                return null;
            }

            if (!jsonParams.has(PARAMS_KEY_FILES)) {
                request
                        .getCallback()
                        .callback(
                                new Response(Response.CODE_ILLEGAL_ARGUMENT,
                                        "no param: " + PARAMS_KEY_FILES));
                return null;
            } else {
                request.setAction(ACTION_FETCH);
                return super.invokeInner(request);
            }
        } else if (ACTION_DOWNLOAD.equals(request.getAction())) {
            download(request);
        } else if (ACTION_ON_DOWNLOAD_COMPLETE.equals(request.getAction())) {
            onDownloadComplete(request);
        }
        return null;
    }

    protected void download(org.hapjs.bridge.Request request) throws JSONException {
        String rawParams = request.getRawParams();
        JSONObject jsonParams = new JSONObject(rawParams);
        String url = jsonParams.getString(PARAMS_KEY_DOWNLOAD_URL);
        JSONObject jsonHeader = jsonParams.optJSONObject(PARAMS_KEY_DOWNLOAD_HEADERS);
        // for old version headers
        if (jsonHeader == null) {
            jsonHeader = jsonParams.optJSONObject(PARAMS_KEY_DOWNLOAD_HEADERS_OLD);
        }
        String description = jsonParams.optString(PARAMS_KEY_DOWNLOAD_DESCRIPTION, null);
        String fileName = jsonParams.optString(PARAMS_KEY_DOWNLOAD_FILENAME);

        File massDir = request.getApplicationContext().getMassDir();
        File downloadDir = new File(massDir, DOWNLOAD_DIR);
        if (massDir == null || !FileUtils.mkdirs(downloadDir)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_IO_ERROR,
                            "can't create download directory"));
            return;
        }

        Uri destinationUri = null;
        if (!TextUtils.isEmpty(fileName)) {
            destinationUri = Uri.fromFile(new File(downloadDir, fileName));
            if (!FileHelper.isValidUri(destinationUri.toString())) {
                request
                        .getCallback()
                        .callback(
                                new Response(Response.CODE_ILLEGAL_ARGUMENT,
                                        "Illegal filename: " + fileName));
                return;
            }
            description = TextUtils.isEmpty(description) ? fileName : description;
        }

        NetworkReportManager.getInstance().reportNetwork(getName(), url);
        DownloadManager.Request downloadRequest =
                buildDownloadRequest(
                        request.getApplicationContext(),
                        Uri.parse(url),
                        destinationUri,
                        jsonHeader,
                        description);

        Activity activity = request.getNativeInterface().getActivity();
        if (activity.isFinishing() || activity.isDestroyed()) {
            request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "app has exited."));
            return;
        }

        long downloadId;
        synchronized (mCallbackLock) {
            request.getNativeInterface().getResidentManager().postRegisterFeature(this);

            if (mResidentCompleteCallback == null) {
                mResidentCompleteCallback = new ResidentCompleteCallback(this, request);
                putCallbackContext(mResidentCompleteCallback);
            }

            DownloadManager downloadManager =
                    (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadId = downloadManager.enqueue(downloadRequest);

            // Listening for loading tasks.
            mResidentCompleteCallback.register(downloadId, request.getCallback());
        }

        clearDownloadInfo(request.getApplicationContext(), downloadId);
        request
                .getCallback()
                .callback(
                        new Response(
                                new JSONObject().put(RESULT_KEY_DOWNLOAD_TOKEN,
                                        String.valueOf(downloadId))));
    }

    protected DownloadManager.Request buildDownloadRequest(
            ApplicationContext applicationContext,
            Uri uri,
            Uri destinationUri,
            JSONObject jsonHeader,
            String description)
            throws JSONException {
        DownloadManager.Request downloadRequest = new DownloadManager.Request(uri);
        downloadRequest.setTitle(description);
        downloadRequest.setDestinationUri(destinationUri);
        downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        if (jsonHeader != null) {
            Iterator<String> keys = jsonHeader.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonHeader.get(key);
                if (value instanceof Object[]) {
                    Object[] values = (Object[]) value;
                    for (Object obj : values) {
                        downloadRequest.addRequestHeader(key, obj.toString());
                    }
                } else {
                    downloadRequest.addRequestHeader(key, value.toString());
                }
            }
        }
        if (jsonHeader == null || TextUtils.isEmpty(jsonHeader.optString(USER_AGENT))) {
            String pkgName = applicationContext.getPackage();
            downloadRequest
                    .addRequestHeader(USER_AGENT, UserAgentHelper.getFullWebkitUserAgent(pkgName));
        }
        return downloadRequest;
    }

    protected void onDownloadComplete(org.hapjs.bridge.Request request) throws JSONException {
        String rawParams = request.getRawParams();
        JSONObject jsonParams = new JSONObject(rawParams);
        String token = jsonParams.optString(PARAMS_KEY_DOWNLOAD_TOKEN);

        if (TextUtils.isEmpty(token)) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "token is null"));
            return;
        }

        synchronized (mCallbackLock) {
            if (mHapCompleteCallback == null) {
                mHapCompleteCallback = new HapCompleteCallback(this, request);
                putCallbackContext(mHapCompleteCallback);
            }
        }

        long downloadId = Long.parseLong(token);
        Context context = request.getNativeInterface().getActivity();
        DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Object[] statusData =
                queryStatusData(request.getApplicationContext(), downloadManager, downloadId);

        int status = (int) statusData[0];
        if (status == DownloadManager.STATUS_SUCCESSFUL
                || status == DownloadManager.STATUS_FAILED) {
            request.getCallback().callback((Response) statusData[1]);
        } else {
            synchronized (mCallbackLock) {
                if (null != mHapCompleteCallback) {
                    mHapCompleteCallback.register(downloadId, request.getCallback());
                }
            }
        }
    }

    private Object[] queryStatusData(
            ApplicationContext applicationContext, DownloadManager downloadManager, long downloadId)
            throws JSONException {
        String internalUri = getDownloadInfo(applicationContext, downloadId);
        if (!TextUtils.isEmpty(internalUri)) {
            JSONObject content = new JSONObject().put(RESULT_KEY_DOWNLOAD_URI, internalUri);
            return new Object[] {DownloadManager.STATUS_SUCCESSFUL, new Response(content)};
        }

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Object[] result;
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                String localUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                long errorCode = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                String url = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Uri uri =
                            getDownloadFileLocalUri(
                                    applicationContext, url, localUri, downloadManager, downloadId);
                    internalUri = applicationContext.getInternalUri(uri);
                    saveDownloadInfo(applicationContext, downloadId, internalUri);
                    JSONObject content = new JSONObject().put(RESULT_KEY_DOWNLOAD_URI, internalUri);
                    result = new Object[] {status, new Response(content)};
                } else {
                    result =
                            new Object[] {
                                    status, new Response(ERROR_CODE_UNKNOWN,
                                    getErrorMessage((int) errorCode))
                            };
                }
            } else {
                result =
                        new Object[] {
                                DownloadManager.STATUS_FAILED,
                                new Response(ERROR_TASK_NOT_FOUND, "task not exists")
                        };
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to queryStatusData", e);
            result =
                    new Object[] {
                            DownloadManager.STATUS_FAILED,
                            new Response(ERROR_CODE_UNKNOWN, e.getMessage())
                    };

        } finally {
            FileUtils.closeQuietly(c);
        }
        return result;
    }

    private Uri getDownloadFileLocalUri(
            ApplicationContext applicationContext,
            String url,
            String localUri,
            DownloadManager downloadManager,
            long downloadId) {
        Uri uri = Uri.parse(localUri);

        File massDir = applicationContext.getMassDir();
        if (massDir == null) {
            return uri;
        }
        File downloadDir = new File(massDir, DOWNLOAD_DIR);
        String filePath = "file".equals(uri.getScheme()) ? uri.getPath() : null;
        if (!TextUtils.isEmpty(filePath)
                && uri.getPath() != null
                && uri.getPath().startsWith(massDir.getAbsolutePath())) {
            return uri;
        }

        if (TextUtils.isEmpty(filePath)) {
            filePath = FileHelper.getFileFromContentUri(applicationContext.getContext(), uri);
        }

        if (TextUtils.isEmpty(filePath)) {
            filePath = Uri.parse(url).getLastPathSegment();
            if (filePath != null && filePath.length() > 100) {
                filePath = filePath.substring(0, 100);
            }
        }

        if (TextUtils.isEmpty(filePath)) {
            filePath = "download";
        }

        String fileName = new File(filePath).getName();
        InputStream in = null;
        try {
            in = applicationContext.getContext().getContentResolver().openInputStream(uri);
            File dest = FileHelper.generateAvailableFile(fileName, downloadDir);
            boolean result = FileUtils.saveToFile(in, dest);
            if (result) {

                Executors.io().execute(() -> downloadManager.remove(downloadId));
                return Uri.fromFile(dest);
            }
        } catch (IOException e) {
            // ignore
            Log.e(TAG, "get download file error", e);
        } finally {
            FileUtils.closeQuietly(in);
        }
        return uri;
    }

    private void saveDownloadInfo(ApplicationContext context, long id, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreference();
        sharedPreferences.edit().putString(getDownloadKey(id), value).apply();
    }

    private String getDownloadInfo(ApplicationContext context, long id) {
        SharedPreferences sharedPreferences = context.getSharedPreference();
        return sharedPreferences.getString(getDownloadKey(id), "");
    }

    private void clearDownloadInfo(ApplicationContext context, long id) {
        SharedPreferences sharedPreferences = context.getSharedPreference();
        sharedPreferences.edit().remove(getDownloadKey(id)).apply();
    }

    private String getDownloadKey(long id) {
        return "download_" + id;
    }

    private String getErrorMessage(int code) {
        switch (code) {
            case DownloadManager.ERROR_FILE_ERROR:
                return "file error";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "unhandled http code";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "http data error";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "too many redirects";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "insufficient storage space";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "no external storage device was found";
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "can't resume the download";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "download file already exists";
            default:
                return "unknown error: " + code;
        }
    }

    @Override
    public void dispose(boolean force) {
        synchronized (mCallbackLock) {
            super.dispose(force);
            if (force) {
                mHapCompleteCallback = null;
                mResidentCompleteCallback = null;
            }
        }
    }

    @Override
    protected boolean isResident() {
        return true;
    }

    @Override
    public Executor getExecutor(org.hapjs.bridge.Request request) {
        return ExecutorHolder.INSTANCE;
    }

    private static class ExecutorHolder {
        private static final Executor INSTANCE = Executors.backgroundExecutor();
    }

    private abstract class CompleteCallbackBase extends CallbackContext {
        protected final DownloadManager mDownloadManager;
        protected BroadcastReceiver mBroadcastReceiver;
        protected Map<Long, Callback> mRequests = new HashMap<>();

        public CompleteCallbackBase(
                CallbackContextHolder holder, final org.hapjs.bridge.Request request) {
            super(holder, request.getAction(), request, true);
            mBroadcastReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE
                                    .equals(intent.getAction())) {
                                Executors.io()
                                        .execute(
                                                () -> {
                                                    long completeDownloadId =
                                                            intent.getLongExtra(
                                                                    DownloadManager.EXTRA_DOWNLOAD_ID,
                                                                    -1);
                                                    synchronized (mCallbackLock) {
                                                        onReceiveDownloadComplete(
                                                                completeDownloadId);
                                                    }
                                                });
                            }
                        }
                    };

            Context context = request.getNativeInterface().getActivity();
            mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        protected abstract void onReceiveDownloadComplete(long completeDownloadId);

        @Override
        public void onCreate() {
            super.onCreate();
            getRequest()
                    .getNativeInterface()
                    .getActivity()
                    .getApplicationContext()
                    .registerReceiver(
                            mBroadcastReceiver,
                            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        void register(Long id, Callback callback) {
            synchronized (mCallbackLock) {
                mRequests.put(id, callback);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            synchronized (mCallbackLock) {
                if (mBroadcastReceiver != null) {
                    try {
                        getRequest()
                                .getNativeInterface()
                                .getActivity()
                                .getApplicationContext()
                                .unregisterReceiver(mBroadcastReceiver);
                    } catch (Exception e) {
                        Log.e(TAG, "complete callback base error", e);
                    }
                    mBroadcastReceiver = null;
                }
            }
        }
    }

    private class HapCompleteCallback extends CompleteCallbackBase {

        public HapCompleteCallback(CallbackContextHolder holder, org.hapjs.bridge.Request request) {
            super(holder, request);
        }

        @Override
        protected void onReceiveDownloadComplete(long completeDownloadId) {
            if (mRequests.containsKey(completeDownloadId)) {
                runCallbackContext(getAction(), 0, completeDownloadId);
            }
        }

        @Override
        public void callback(int what, Object obj) {
            long completeDownloadId = (long) obj;
            Callback callback = mRequests.remove(completeDownloadId);
            if (callback == null) {
                return;
            }
            try {
                Object[] statusData =
                        queryStatusData(
                                getRequest().getApplicationContext(), mDownloadManager,
                                completeDownloadId);
                callback.callback((Response) statusData[1]);
            } catch (JSONException e) {
                callback.callback(new Response(ERROR_CODE_UNKNOWN, e.getMessage()));
            }
        }
    }

    private class ResidentCompleteCallback extends CompleteCallbackBase {

        public ResidentCompleteCallback(
                CallbackContextHolder holder, org.hapjs.bridge.Request request) {
            super(holder, request);
        }

        @Override
        protected void onReceiveDownloadComplete(long completeDownloadId) {
            if (mRequests.containsKey(completeDownloadId)) {
                mRequests.remove(completeDownloadId);
                if (mRequests.isEmpty()) {
                    getRequest()
                            .getNativeInterface()
                            .getResidentManager()
                            .postUnregisterFeature(Request.this);
                }
            }
        }

        @Override
        public void callback(int what, Object obj) {
        }
    }
}
