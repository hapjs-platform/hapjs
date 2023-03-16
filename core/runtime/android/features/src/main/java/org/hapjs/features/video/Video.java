/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.video;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Video.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Video.ACTION_INIT, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = Video.ACTION_COMPRESS_VIDEO,
                        instanceMethod = true,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Video.ACTION_GET_VIDEO_INFO,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Video.ACTION_GET_VIDEO_THUMBNAIL,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}),
                @ActionAnnotation(
                        name = Video.ACTION_ABORT,
                        instanceMethod = true,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Video.ACTION_ON_PROGRESS_UPDATE,
                        instanceMethod = true,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Video.ACTION_ON_PROGRESS_UPDATE_ALIAS)
        })
public class Video extends CallbackHybridFeature {
    public static final String EXTRA_RESULT = "curCompressTaskResult";
    protected static final String FEATURE_NAME = "hap.io.Video";
    protected static final String ACTION_INIT = FeatureExtension.ACTION_INIT;
    protected static final String ACTION_COMPRESS_VIDEO = "compressVideo";
    protected static final String ACTION_GET_VIDEO_INFO = "getVideoInfo";
    protected static final String ACTION_GET_VIDEO_THUMBNAIL = "getVideoThumbnail";
    protected static final String ACTION_ABORT = "abort";
    protected static final String ACTION_ON_PROGRESS_UPDATE = "__onprogressupdate";
    protected static final String ACTION_ON_PROGRESS_UPDATE_ALIAS = "onprogressupdate";
    protected static final int VIDEO_CODE_INVALID_VIDEO = Response.CODE_FEATURE_ERROR + 1;
    protected static final int PARALLEL_COMPRESS_TASK_SIZE = 100;
    protected static final int MSG_START_OR_ENQUEUE = 1;
    protected static final int MSG_COMPLETE = 2;
    protected static final int MSG_FINISH_ALL = 3;
    protected static final int MSG_ABORT = 4;
    protected static final ArrayBlockingQueue<VideoCompressTask> COMPRESS_TASKS_QUEUE =
            new ArrayBlockingQueue<VideoCompressTask>(PARALLEL_COMPRESS_TASK_SIZE);
    private static final String TAG = "Video";
    private static final String PARAMS_URI = "uri";
    private static final String RESULT_URI = "uri";
    private static final String RESULT_NAME = "name";
    private static final String RESULT_WIDTH = "width";
    private static final String RESULT_HEIGHT = "height";
    private static final String RESULT_SIZE = "size";
    private static final String PARAMS_BITRATE = "bitrate";
    private static final String PARAMS_FRAMERATE = "framerate";
    private static final String PARAMS_DURATION = "duration";
    private static final String PARAMS_WIDTH = "width";
    private static final String PARAMS_HEIGHT = "height";
    private static final String IMAGE_FORMAT_JPEG = ".jpeg";
    private static final String VIDEO_PREFIX = "video/";
    private static final String TASK_QUEUE_THREAD_NAME = "task-queue-handlerthread";
    private static final String COMPRESS_FILE_PREFIX = "videoCompress";
    private static final String COMPRESS_FILE_SUFFIX = ".mp4";
    private static final String VIDEO_THUMBNAIL_PREFIX = "videoThumbnail";
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final int IMAGE_QUALITY = 100;
    protected HandlerThread mTaskQueueProcessThread;
    protected Handler mTaskQueueHandler = null;
    // 使用标志位把多个压缩任务限制成单路串行
    protected volatile boolean isCompressStarted = false;
    protected VideoCompressCallback mVideoCompressCallback;
    VideoConverter mVideoConverter;

    private LifecycleListener mLifecycleListener;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_ABORT.equals(action)) {
            abortProgress(request);
        } else if (ACTION_ON_PROGRESS_UPDATE.equals(action)) {
            onProgressUpdate(request);
        } else if (ACTION_COMPRESS_VIDEO.equals(action)) {
            compressVideo(request);
        } else if (ACTION_INIT.equals(action)) {
            return createCompressTask(request);
        } else if (ACTION_GET_VIDEO_INFO.equals(action)) {
            getVideoInfo(request);
        } else if (ACTION_GET_VIDEO_THUMBNAIL.equals(action)) {
            getVideoThumbnail(request);
        } else {
            Log.e(TAG, "unsupport action");
            return Response.NO_ACTION;
        }
        return Response.SUCCESS;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Response createCompressTask(Request request) throws SerializeException {
        final VideoCompressTask task = new VideoCompressTask(request.getSerializeParams());
        HybridManager hybridManager = request.getView().getHybridManager();
        JavaSerializeObject instance =
                InstanceManager.getInstance().createInstance(hybridManager, task);
        return new Response(instance);
    }

    protected synchronized void compressVideo(Request request) {
        InstanceManager instanceManager = InstanceManager.getInstance();
        VideoCompressTask videoCompressTask =
                (VideoCompressTask) instanceManager.getInstance(request.getInstanceId());
        if (videoCompressTask == null) {
            Response response = new Response(Response.CODE_SERVICE_UNAVAILABLE, "no such instance");
            request.getCallback().callback(response);
            return;
        }
        if (videoCompressTask.isAbort()) {
            request
                    .getCallback()
                    .callback(
                            new Response(Response.CODE_GENERIC_ERROR,
                                    "the video compression has been aborted"));
            RuntimeLogManager.getDefault()
                    .logVideoFeature(request, Integer.toString(Response.CODE_TOO_MANY_REQUEST), "");
            return;
        }
        if (!isSupportConvert(videoCompressTask)) {
            request.getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "no supported"));
            return;
        }
        // 若当前任务已经被创建过则返回无效
        if (videoCompressTask.getInited().get()) {
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    Response.CODE_TOO_MANY_REQUEST,
                                    "the video has been scheduled for compression"));
            RuntimeLogManager.getDefault()
                    .logVideoFeature(request, Integer.toString(Response.CODE_TOO_MANY_REQUEST), "");
            return;
        }
        SerializeObject params = videoCompressTask.getJsonParams();
        Uri underlyingUri = getUnderlyingUri(params, request);
        if (underlyingUri == null) {
            return;
        }
        int paramsBitRate = params.optInt(PARAMS_BITRATE, 0);
        int paramsFrameRate = params.optInt(PARAMS_FRAMERATE, 0);
        int paramsHeight = params.optInt(PARAMS_HEIGHT, 0);
        int paramsWidth = params.optInt(PARAMS_WIDTH, 0);
        if (paramsHeight % 2 != 0 || paramsWidth % 2 != 0) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "height or width cannot be odd ");
            request.getCallback().callback(response);
            RuntimeLogManager.getDefault()
                    .logVideoFeature(
                            request, Integer.toString(Response.CODE_ILLEGAL_ARGUMENT),
                            "height or width ");
            return;
        }
        // 开始解析原视频的参数
        Context context = request.getNativeInterface().getActivity();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String width;
        String height;
        String bps;
        String duration;
        String rotation;
        try {
            retriever.setDataSource(context, underlyingUri);
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            bps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            rotation =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        } catch (Exception e) {
            Response response = new Response(VIDEO_CODE_INVALID_VIDEO, "video file invalid");
            request.getCallback().callback(response);
            RuntimeLogManager.getDefault()
                    .logVideoFeature(
                            request, Integer.toString(VIDEO_CODE_INVALID_VIDEO),
                            "retriever.setDataSource");
            return;
        } finally {
            retriever.release();
        }
        if (TextUtils.isEmpty(width)
                || TextUtils.isEmpty(height)
                || TextUtils.isEmpty(bps)
                || TextUtils.isEmpty(duration)) {
            Response response = new Response(VIDEO_CODE_INVALID_VIDEO, "can't get the video info");
            request.getCallback().callback(response);
            RuntimeLogManager.getDefault()
                    .logVideoFeature(request, Integer.toString(VIDEO_CODE_INVALID_VIDEO),
                            "video info null");
            return;
        }

        int targetFps = paramsFrameRate;
        int originFps = 0;
        if (targetFps <= 0) {
            MediaExtractor videoExtractor = new MediaExtractor();
            MediaFormat videoInputFormat = null;
            try {
                videoExtractor.setDataSource(context, underlyingUri, null);
                int numTracks = videoExtractor.getTrackCount();
                for (int i = 0; i < numTracks; i++) {
                    MediaFormat format = videoExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith(VIDEO_PREFIX)) {
                        videoInputFormat = format;
                    }
                }
                if (videoInputFormat != null) {
                    originFps = videoInputFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                }
            } catch (Exception e) {
                Log.e(TAG, "extractor get fps fail");
            } finally {
                videoExtractor.release();
            }
            if (originFps <= 0) {
                targetFps = DEFAULT_FRAME_RATE;
            } else {
                targetFps = originFps;
            }
        }
        int originBitRate = Integer.parseInt(bps);
        int originWidth = Integer.parseInt(width);
        int originHeight = Integer.parseInt(height);
        int targetHeight = paramsHeight > 0 ? paramsHeight : originHeight;
        int targetWidth = paramsWidth > 0 ? paramsWidth : originWidth;
        int targetBitRate = paramsBitRate > 0 ? paramsBitRate : originBitRate / 2;
        // 所设码率高于原码率，则原视频无需被压缩的更新
        if (originBitRate <= targetBitRate) {
            Response response =
                    new Response(
                            Response.CODE_ILLEGAL_ARGUMENT,
                            "origin bitRate is less than params bitRate,it doesn't have to be compressed less");
            request.getCallback().callback(response);
            RuntimeLogManager.getDefault()
                    .logVideoFeature(
                            request, Integer.toString(Response.CODE_ILLEGAL_ARGUMENT),
                            "origin bps too less");
            return;
        }
        // 视频时长，单位ms换成s
        int exportDuration = (int) (Long.parseLong(duration) / 1000);
        // 设置压缩后文件路径且文件被创建
        File scrapFile = null;
        try {
            scrapFile = getScrapFile(request, COMPRESS_FILE_PREFIX, COMPRESS_FILE_SUFFIX);
        } catch (IOException e) {
            Response response = new Response(Response.CODE_IO_ERROR, "create output file fail");
            request.getCallback().callback(response);
            RuntimeLogManager.getDefault()
                    .logVideoFeature(
                            videoCompressTask.getCompressRequest(),
                            Integer.toString(Response.CODE_IO_ERROR),
                            "create output file fail");
            return;
        }
        String targetPath = scrapFile.getAbsolutePath();
        videoCompressTask.getInited().set(true);
        videoCompressTask.updateParams(
                request,
                targetBitRate,
                targetFps,
                targetHeight,
                targetWidth,
                underlyingUri,
                exportDuration,
                targetPath,
                Integer.parseInt(rotation));
        initTaskProcessQueue(request);
        sendStartOrEnqueue(videoCompressTask);
    }

    private void sendStartOrEnqueue(VideoCompressTask videoCompressTask) {
        Message msg = Message.obtain();
        msg.what = MSG_START_OR_ENQUEUE;
        msg.obj = videoCompressTask;
        mTaskQueueHandler.sendMessage(msg);
    }

    // 如果当前没有压缩任务正在执行且前面无等待任务则可以直接开始本任务，否则加入等待队列
    protected void onStartOrEnqueue(VideoCompressTask videoCompressTask) {
        if (COMPRESS_TASKS_QUEUE.size() != 0 || isCompressStarted) {
            Log.i(TAG, "offer queue:" + videoCompressTask.getSourceUrl());
            if (!(COMPRESS_TASKS_QUEUE.offer(videoCompressTask))) {
                Request request = videoCompressTask.getCompressRequest();
                request
                        .getCallback()
                        .callback(
                                new Response(Response.CODE_TOO_MANY_REQUEST,
                                        "the compress task queue is full"));
                RuntimeLogManager.getDefault()
                        .logVideoFeature(
                                request, Integer.toString(Response.CODE_TOO_MANY_REQUEST),
                                "queue full");
            }
        } else {
            isCompressStarted = true;
            convertVideoFromTask(videoCompressTask);
        }
    }

    protected void initTaskProcessQueue(Request request) {
        if (mLifecycleListener == null) {
            mLifecycleListener =
                    new LifecycleListener() {
                        @Override
                        public void onDestroy() {
                            mTaskQueueHandler.removeCallbacksAndMessages(null);
                            mTaskQueueHandler.sendEmptyMessage(MSG_FINISH_ALL);
                            request.getNativeInterface().removeLifecycleListener(this);
                        }
                    };
            request.getNativeInterface().addLifecycleListener(mLifecycleListener);
        }
        // 处理等待task的线程开启
        if (mTaskQueueProcessThread == null) {
            mTaskQueueProcessThread = new HandlerThread(TASK_QUEUE_THREAD_NAME);
            mTaskQueueProcessThread.start();
            mTaskQueueHandler =
                    new Handler(mTaskQueueProcessThread.getLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            Log.w(TAG, "handleMessage " + msg.what);
                            switch (msg.what) {
                                case MSG_START_OR_ENQUEUE:
                                    onStartOrEnqueue((VideoCompressTask) msg.obj);
                                    break;
                                case MSG_COMPLETE:
                                    Bundle bundle = msg.getData();
                                    if (bundle == null) {
                                        return;
                                    }
                                    onCompletetTask((VideoCompressTask) msg.obj,
                                            bundle.getBoolean(EXTRA_RESULT));
                                    isCompressStarted = false;
                                    onHandleNext();
                                    break;
                                case MSG_FINISH_ALL:
                                    onFinishAll();
                                    break;
                                case MSG_ABORT:
                                    onAbortTask((VideoCompressTask) msg.obj);
                                    break;
                                default:
                                    Log.d(TAG, "missing key =" + msg.what);
                                    break;
                            }
                        }
                    };
        }
        if (mVideoCompressCallback == null) {
            mVideoCompressCallback =
                    new VideoCompressCallback() {
                        @Override
                        public void notifyComplete(VideoCompressTask curVideoCompressTask,
                                                   boolean isSuccess) {
                            sendComplete(curVideoCompressTask, isSuccess);
                        }

                        @Override
                        public void notifyAbort(VideoCompressTask curVideoCompressTask) {
                            sendAbortTask(curVideoCompressTask);
                        }
                    };
        }
    }

    private void onFinishAll() {
        stopConvert();
        // 无需执行剩余任务
        COMPRESS_TASKS_QUEUE.clear();
        if (mTaskQueueProcessThread != null) {
            mTaskQueueProcessThread.quit();
        }
        if (mTaskQueueHandler != null) {
            mTaskQueueHandler.removeCallbacksAndMessages(null);
        }
        mVideoCompressCallback = null;
        mVideoConverter = null;
    }

    void sendComplete(VideoCompressTask curVideoCompressTask, boolean isSuccess) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_RESULT, isSuccess);
        Message msg = Message.obtain();
        msg.what = MSG_COMPLETE;
        msg.obj = curVideoCompressTask;
        msg.setData(bundle);
        mTaskQueueHandler.sendMessage(msg);
    }

    private void onCompletetTask(VideoCompressTask curVideoCompressTask, boolean isSuccess) {
        if (curVideoCompressTask != null) {
            if (isSuccess) {
                curVideoCompressTask.setSuccess(true);
                curVideoCompressTask.notifyTaskSuccess();
            } else {
                curVideoCompressTask.clearTargetFile();
                curVideoCompressTask.setAbort(true);
            }
            curVideoCompressTask.setCompressing(false);
        }
    }

    private void onHandleNext() {
        if (COMPRESS_TASKS_QUEUE.size() == 0) {
            return;
        }
        VideoCompressTask nextVideoCompressTask = COMPRESS_TASKS_QUEUE.poll();
        if (nextVideoCompressTask != null && !nextVideoCompressTask.isAbort()) {
            isCompressStarted = true;
            Log.i(TAG, "poll queue:" + nextVideoCompressTask.getSourceUrl());
            convertVideoFromTask(nextVideoCompressTask);
        } else {
            onHandleNext();
        }
    }

    protected void convertVideoFromTask(VideoCompressTask videoCompressTask) {
        if (videoCompressTask.isAbort()) {
            sendComplete(videoCompressTask, false);
            return;
        }
        videoCompressTask.setCompressing(true);
        try {
            startConvert(videoCompressTask, mVideoCompressCallback);
        } catch (Exception e) {
            Request request = videoCompressTask.getCompressRequest();
            Log.e(TAG, "startConvertTask exception:" + e.getMessage());
            Response response =
                    new Response(Response.CODE_GENERIC_ERROR, "fail to compress the video");
            RuntimeLogManager.getDefault()
                    .logVideoFeature(
                            request, Integer.toString(Response.CODE_GENERIC_ERROR),
                            "startConvertTask");
            request.getCallback().callback(response);
            stopConvert();
        }
    }

    protected void startConvert(VideoCompressTask videoCompressTask,
                                VideoCompressCallback callback) {
        mVideoConverter = new VideoConverter(videoCompressTask, callback);
        mVideoConverter.startConvertTask();
    }

    protected void stopConvert() {
        if (mVideoConverter != null) {
            mVideoConverter.stopConvertTask();
        }
    }

    protected boolean isSupportConvert(VideoCompressTask videoCompressTask) {
        return true;
    }

    private void abortProgress(Request request) {
        InstanceManager instanceManager = InstanceManager.getInstance();
        VideoCompressTask videoCompressTask =
                (VideoCompressTask) instanceManager.getInstance(request.getInstanceId());
        if (videoCompressTask == null) {
            Response response = new Response(Response.CODE_SERVICE_UNAVAILABLE, "no such instance");
            request.getCallback().callback(response);
            return;
        }
        if (videoCompressTask.isSuccess() || videoCompressTask.isAbort()) {
            Response response =
                    new Response(Response.CODE_GENERIC_ERROR,
                            "compression of the video has been completed");
            request.getCallback().callback(response);
            return;
        }
        if (mTaskQueueHandler == null) {
            videoCompressTask.setAbort(true);
        } else {
            sendAbortTask(videoCompressTask);
        }
        request.getCallback().callback(Response.SUCCESS);
    }

    private void sendAbortTask(VideoCompressTask videoCompressTask) {
        Message msg = Message.obtain();
        msg.what = MSG_ABORT;
        msg.obj = videoCompressTask;
        mTaskQueueHandler.sendMessage(msg);
    }

    private void onAbortTask(VideoCompressTask videoCompressTask) {
        if (videoCompressTask.isAbort()) {
            return;
        }
        videoCompressTask.setAbort(true);
        if (videoCompressTask.isCompressing()) {
            stopConvert();
        }
    }

    private void onProgressUpdate(Request request) {
        InstanceManager instanceManager = InstanceManager.getInstance();
        VideoCompressTask videoCompressTask =
                (VideoCompressTask) instanceManager.getInstance(request.getInstanceId());
        if (videoCompressTask == null) {
            Response response = new Response(Response.CODE_SERVICE_UNAVAILABLE, "no such instance");
            request.getCallback().callback(response);
            return;
        }
        videoCompressTask.registerProgressListener(request);
    }

    private void getVideoInfo(Request request) throws SerializeException {
        Uri underlyingUri = getUnderlyingUri(request.getSerializeParams(), request);
        if (underlyingUri == null) {
            return;
        }
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        String fps = "";
        String time;
        String bps;
        String width;
        String height;
        try {
            metadataRetriever
                    .setDataSource(request.getNativeInterface().getActivity(), underlyingUri);
            time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            bps = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fps =
                        metadataRetriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            }
            width = metadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            height = metadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        } catch (Exception e) {
            Log.e(TAG, "setDataSource error", e);
            request.getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "video uri error"));
            return;
        } finally {
            metadataRetriever.release();
        }
        if (TextUtils.isEmpty(time)) {
            time = "0";
        }
        long timeInMillisec = 0;
        try {
            timeInMillisec = Long.parseLong(time);
        } catch (Exception e) {
            Log.d(TAG, " get timeInMillisec fail");
        }
        int duration = (int) (timeInMillisec / 1000);
        if (!isValidFPS(fps)) {
            Context context = request.getNativeInterface().getActivity();
            MediaExtractor videoExtractor = new MediaExtractor();
            MediaFormat videoInputFormat = null;
            try {
                videoExtractor.setDataSource(context, underlyingUri, null);
                int numTracks = videoExtractor.getTrackCount();
                for (int i = 0; i < numTracks; i++) {
                    MediaFormat format = videoExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith(VIDEO_PREFIX)) {
                        videoInputFormat = format;
                    }
                }
                if (videoInputFormat != null) {
                    fps = Integer.toString(videoInputFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
                }
            } catch (Exception e) {
                Log.e(TAG, "extractor get fps fail");
            } finally {
                videoExtractor.release();
            }
        }
        long size = getFileSize(request, underlyingUri);
        String internalUri = request.getApplicationContext().getInternalUri(underlyingUri);
        String name = getFileName(internalUri);
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_NAME, name);
            result.put(RESULT_URI, internalUri);
            result.put(PARAMS_FRAMERATE, fps == null ? "" : fps);
            result.put(PARAMS_BITRATE, bps == null ? "" : bps);
            result.put(PARAMS_DURATION, duration);
            result.put(RESULT_WIDTH, width == null ? "" : width);
            result.put(RESULT_HEIGHT, height == null ? "" : height);
            result.put(RESULT_SIZE, size);
            Response response = new Response(result);
            request.getCallback().callback(response);
        } catch (JSONException e) {
            Log.e(TAG, "Fail to put result to json.", e);
            request.getCallback().callback(getExceptionResponse(request, e));
        }
    }

    private boolean isValidFPS(String fps) {
        if (TextUtils.isEmpty(fps)) {
            return false;
        }
        try {
            float fpsFloat = Float.parseFloat(fps);
            if (fpsFloat == 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "FPS format error: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void getVideoThumbnail(Request request) throws SerializeException {
        Uri underlyingUri = getUnderlyingUri(request.getSerializeParams(), request);
        if (underlyingUri == null) {
            return;
        }
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Bitmap bitmap;
        try {
            metadataRetriever
                    .setDataSource(request.getNativeInterface().getActivity(), underlyingUri);
            bitmap = metadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            Log.e(TAG, "setDataSource error", e);
            request.getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR, "video uri error"));
            return;
        } finally {
            metadataRetriever.release();
        }
        if (bitmap == null) {
            Log.e(TAG, "Fail to get a thumbnail image");
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_GENERIC_ERROR,
                            "Fail to get a thumbnail image"));
            return;
        }
        OutputStream out = null;
        File tmpFile;
        try {
            tmpFile =
                    request.getApplicationContext()
                            .createTempFile(VIDEO_THUMBNAIL_PREFIX, IMAGE_FORMAT_JPEG);
            out = new FileOutputStream(tmpFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out);
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
            return;
        } finally {
            FileUtils.closeQuietly(out);
            bitmap.recycle();
        }
        String resultUri = request.getApplicationContext().getInternalUri(tmpFile);
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_URI, resultUri);
            request.getCallback().callback(new Response(result));
        } catch (JSONException e) {
            Log.e(TAG, "Fail to put result to json.", e);
            request.getCallback().callback(getExceptionResponse(request, e));
        }
    }

    private Uri getUnderlyingUri(SerializeObject jsonParams, Request request) {
        if (jsonParams == null) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "params is null");
            request.getCallback().callback(response);
            return null;
        }
        String uri = jsonParams.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri) || !InternalUriUtils.isValidUri(uri)) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid uri: " + uri);
            request.getCallback().callback(response);
            return null;
        }
        Uri underlyingUri = null;
        try {
            underlyingUri = request.getApplicationContext().getUnderlyingUri(uri);
        } catch (IllegalArgumentException e) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid uri: " + uri);
            request.getCallback().callback(response);
            return null;
        }
        if (underlyingUri == null) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid uri: " + uri);
            request.getCallback().callback(response);
            return null;
        }
        return underlyingUri;
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

    private String getFileName(String uri) {
        int index = 0;
        if (uri != null) {
            index = uri.lastIndexOf('/');
        }
        if (index > 0 && index < uri.length() - 1) {
            return uri.substring(index + 1);
        } else {
            return uri;
        }
    }

    private File getScrapFile(Request request, String prefix, String suffix) throws IOException {
        File cacheDir = request.getApplicationContext().getCacheDir();
        return File.createTempFile(prefix, suffix, cacheDir);
    }
}
