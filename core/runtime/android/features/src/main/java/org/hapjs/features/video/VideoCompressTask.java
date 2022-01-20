/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.video;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoCompressTask implements InstanceManager.IInstance {
    public static final String TAG = "VideoCompressTask";
    protected static final int SUCCESS_PERCENT = 100;
    protected static final int ABORT_PERCENT = 0;
    protected static final String RESULT_KEY_PROGRESS = "progress";
    protected static final String RESULT_URI = "uri";
    protected static final String RESULT_NAME = "name";
    protected static final String RESULT_SIZE = "size";
    protected volatile boolean isAbort = false;
    protected volatile boolean isCompressing = false;
    protected volatile boolean isSuccess = false;
    protected AtomicBoolean isInited = new AtomicBoolean(false);
    protected int mBps;
    protected int mFps;
    protected int mHeight;
    protected int mWidth;
    protected int mRotation;
    protected Uri mSourceUrl;
    protected String mTargetPath;
    protected int mExportDuration;
    protected SerializeObject mJsonParams;
    protected Request mCompressRequest;
    protected Request mProgressRequest;
    protected OnExportPercentListener mProgressListener;

    public VideoCompressTask(SerializeObject jsonParams) {
        this.mJsonParams = jsonParams;
    }

    public void updateParams(
            Request request,
            int bps,
            int fps,
            int height,
            int width,
            Uri sourceUrl,
            int exportDuration,
            String targetPath,
            int rotation) {
        this.mCompressRequest = request;
        this.mBps = bps;
        this.mFps = fps;
        this.mHeight = height;
        this.mWidth = width;
        this.mSourceUrl = sourceUrl;
        this.mExportDuration = exportDuration;
        this.mTargetPath = targetPath;
        this.mRotation = rotation;
        isAbort = false;
        isCompressing = false;
        isSuccess = false;
    }

    public SerializeObject getJsonParams() {
        return mJsonParams;
    }

    public AtomicBoolean getInited() {
        return isInited;
    }

    public int getExportDuration() {
        return mExportDuration;
    }

    public Request getCompressRequest() {
        return mCompressRequest;
    }

    public String getTargetPath() {
        return mTargetPath;
    }

    public void setTargetPath(String targetPath) {
        this.mTargetPath = targetPath;
    }

    public int getBps() {
        return mBps;
    }

    public int getFps() {
        return mFps;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public int getRotation() {
        return mRotation;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public Uri getSourceUrl() {
        return mSourceUrl;
    }

    public boolean isAbort() {
        return isAbort;
    }

    public void setAbort(boolean abort) {
        isAbort = abort;
    }

    public boolean isCompressing() {
        return isCompressing;
    }

    public void setCompressing(boolean compressing) {
        isCompressing = compressing;
    }

    public void registerProgressListener(Request request) {
        mProgressRequest = request;
        mProgressListener =
                new OnExportPercentListener() {
                    @Override
                    public void onPercentChanged(int percent) {
                        try {
                            JSONObject result = new JSONObject();
                            result.put(RESULT_KEY_PROGRESS, percent);
                            Response response = new Response(result);
                            mProgressRequest.getCallback().callback(response);
                            Log.d(TAG, "percent" + percent);
                        } catch (JSONException e) {
                            Log.e(TAG, "Fail to callback onPercentChanged", e);
                            request
                                    .getCallback()
                                    .callback(new Response(Response.CODE_GENERIC_ERROR,
                                            "params error"));
                            RuntimeLogManager.getDefault()
                                    .logVideoFeature(
                                            mProgressRequest,
                                            Integer.toString(Response.CODE_GENERIC_ERROR),
                                            "JSONException onPercentChanged");
                        }
                    }
                };
    }

    public void notifyTaskProgress(int percent) {
        if (mProgressListener != null) {
            mProgressListener.onPercentChanged(percent);
            if (isSuccess) {
                mProgressListener.onPercentChanged(SUCCESS_PERCENT);
            }
        }
    }

    public void clearTargetFile() {
        if (mTargetPath == null) {
            return;
        }
        File file = new File(mTargetPath);
        if (!file.delete()) {
            Log.i(TAG, "delete file failed ");
        }
    }

    public void notifyTaskSuccess() {
        if (mCompressRequest == null) {
            return;
        }
        if (mTargetPath != null) {
            File file = new File(mTargetPath);
            Uri targetFileUri = null;
            try {
                targetFileUri =
                        getContentUri(mCompressRequest.getNativeInterface().getActivity(), file);
            } catch (IOException e) {
                Response response = new Response(Response.CODE_IO_ERROR, "create output uri fail");
                mCompressRequest.getCallback().callback(response);
                RuntimeLogManager.getDefault()
                        .logVideoFeature(
                                mCompressRequest,
                                Integer.toString(Response.CODE_IO_ERROR),
                                "create output file fail");
                return;
            }
            String resultUri =
                    mCompressRequest.getApplicationContext().getInternalUri(targetFileUri);
            JSONObject result = new JSONObject();
            try {
                result.put(RESULT_URI, resultUri);
                result.put(RESULT_NAME, file.getName());
                result.put(RESULT_SIZE, file.length());
                mCompressRequest.getCallback().callback(new Response(result));
                RuntimeLogManager.getDefault()
                        .logVideoFeature(mCompressRequest, Integer.toString(Response.CODE_SUCCESS),
                                "");
            } catch (JSONException e) {
                Log.e(TAG, "Parse result failed, ", e);
                mCompressRequest.getCallback().callback(Response.ERROR);
            }
        }
    }

    @Override
    public void release() {
    }

    @Override
    public String getFeatureName() {
        return Video.FEATURE_NAME;
    }

    protected Uri getContentUri(Context context, File file) throws IOException {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".file", file);
    }

    protected interface OnExportPercentListener {
        void onPercentChanged(int percent);
    }
}
