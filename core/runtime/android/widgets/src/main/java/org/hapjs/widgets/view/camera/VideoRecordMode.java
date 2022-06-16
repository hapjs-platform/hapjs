/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera;

import static org.hapjs.widgets.view.camera.CameraView.CAMERA_ERROR;
import static org.hapjs.widgets.view.camera.CameraView.CAMERA_ERROR_MESSAGE;
import static org.hapjs.widgets.view.camera.CameraView.CAMERA_OK;
import static org.hapjs.widgets.view.camera.CameraView.CAMERA_OK_MESSAGE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.widgets.R;
import org.hapjs.widgets.view.camera.record.CameraSurfaceRender;
import org.hapjs.widgets.view.camera.record.MediaMuxerController;
import org.hapjs.widgets.view.camera.record.TextureMovieEncoder;

public class VideoRecordMode extends CameraBaseMode<GLSurfaceView>
        implements SurfaceTexture.OnFrameAvailableListener {

    public static final int VIDEO_MAX_DURATION = 60 * 10;
    private static final String TAG = "VideoRecordMode";
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    private CameraSurfaceRender mRenderer;
    private CameraHandler mCameraHandler;
    private File mTmpFile = null;
    private boolean mRecordingEnabled;
    private boolean mCompressed;
    private Handler mMainHandler = null;
    private Runnable mStopVideoRunnable = null;
    private CameraView.OnVideoRecordListener mOnVideoStopListener = null;
    private SurfaceTexture mSurfaceTexture = null;
    private int mMaxVideoDuration = VIDEO_MAX_DURATION;
    private boolean mIsRecordTimeout;
    private Runnable mStopDelayVideoRunnable = null;
    private int DELAY_STOP_TIME = 2;
    private boolean mCurrentStarted = false;
    private boolean mInitFrameAvailable = false;
    private boolean mIsPaused = false;
    private boolean mIsDelayStop;

    public VideoRecordMode(CameraView cameraView, Component component) {
        super(cameraView, component);
    }

    public CameraView getCameraView() {
        return mCameraView;
    }

    public SurfaceView getModeView(Context context, ViewGroup parentView) {
        final View view = View.inflate(context, R.layout.glsurface_view, parentView);
        mSurfaceView = view.findViewById(R.id.glsurface_view);
        return mSurfaceView;
    }

    @Override
    public void initCameraMode() {
        super.initCameraMode();
        initListener();
        prepareVideoRecord();
    }

    private void initListener() {
        if (null != mSurfaceView) {
            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.setKeepScreenOn(true);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    public void onBackAttachCameraMode() {
        super.onBackAttachCameraMode();
        initListener();
        if (null != sVideoEncoder) {
            sVideoEncoder.resetStatus();
        }
        if (null != mCameraHandler) {
            mCameraHandler.resetWeakRefVideoRecord(this);
        } else {
            Log.w(
                    TAG, CameraBaseMode.VIDEO_RECORD_TAG
                            + "onBackAttachCameraMode mCameraHandler is null.");
        }
    }

    private void prepareVideoRecord() {
        mCameraHandler = new CameraHandler(this);
        if (null != sVideoEncoder) {
            sVideoEncoder.resetStatus();
        }
        mRecordingEnabled = sVideoEncoder.isRecording();
        mMainHandler = new Handler(Looper.getMainLooper());
        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        if (null != mSurfaceView) {
            mSurfaceView.setEGLContextClientVersion(2); // select GLES 2.0
            mRenderer = new CameraSurfaceRender(mCameraHandler, sVideoEncoder);
            mSurfaceView.setRenderer(mRenderer);
            mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    public void createDetachedSurfaceTexture() {
        if (null != mRenderer) {
            mRenderer.createDetachedSurfaceTexture();
        } else {
            Log.w(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "createDetachedSurfaceTexture  mRenderer is null.");
        }
    }

    private void handleSurfaceChanged(int width, int height, SurfaceTexture surfaceTexture) {
        if (null != mCameraView && width > 0 && height > 0) {
            mCameraView.setSize(width, height);
            if (mCameraView.mCamera != null && mCameraView.mIsHasPermission) {
                mSurfaceTexture = surfaceTexture;
                setUpPreview(mShowingPreview);
                mCameraView.adjustCameraParameters();
            }
        }
    }

    @Override
    public void setUpPreview(boolean isShowingPreview) {
        super.setUpPreview(isShowingPreview);
        if (null == mSurfaceView || null == mSurfaceTexture || null == mCameraView) {
            mInitFrameAvailable = false;
            if (null != mRenderer) {
                mRenderer.refreshSurfaceTextureStatus(false);
            }
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "setUpPreview mSurfaceView null or mCameraView null ");
            return;
        }
        final boolean needsToStopPreview = isShowingPreview && Build.VERSION.SDK_INT < 14;
        if (needsToStopPreview) {
            mCameraView.stopPreview();
        }
        if (needsToStopPreview) {
            handleSetSurfaceTexture(mSurfaceTexture, true);
        }
    }

    private void handleSetSurfaceTexture(SurfaceTexture st, boolean isNeedStartPreview) {
        if (null != st) {
            st.setOnFrameAvailableListener(this);
        }
        try {
            if (null != mCameraView) {
                final Camera camera = mCameraView.mCamera;
                if (null != camera) {
                    camera.setPreviewTexture(st);
                }
                if (isNeedStartPreview) {
                    mCameraView.startPreview();
                }
            }
        } catch (IOException ioe) {
            if (null != mComponent) {
                RenderEventCallback callback = mComponent.getCallback();
                if (null != callback) {
                    callback.onJsException(new RuntimeException(ioe));
                }
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (null != mSurfaceView) {
            mSurfaceView.requestRender();
            if (!mInitFrameAvailable && !mIsPaused) {
                mInitFrameAvailable = true;
                mRenderer.refreshSurfaceTextureStatus(true);
            }
        }
    }

    public void startRecording(
            CameraView.OnVideoRecordListener onVideoRecordListener, int maxDuration,
            boolean compressed) {
        if (mRecordingEnabled) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "startRecording mRecordingEnabled : "
                            + mRecordingEnabled);
            if (null != onVideoRecordListener) {
                CameraData cameraData = new CameraData();
                cameraData.setRetCode(CAMERA_ERROR);
                cameraData.setMsg(CAMERA_ERROR_MESSAGE + " video is recording.");
                onVideoRecordListener.onVideoRecordCallback(cameraData);
            }
            return;
        }
        Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording .");
        mIsRecordTimeout = false;
        mMaxVideoDuration = maxDuration;
        mRecordingEnabled = true;
        mCompressed = compressed;
        try {
            if (null != mComponent) {
                RenderEventCallback callback = mComponent.getCallback();
                if (null != callback) {
                    mTmpFile = callback.createFileOnCache("videorecord", ".mp4");
                    Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording mTmpFile : "
                            + mTmpFile);
                } else {
                    Log.e(TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG + "startRecording callback is null.");
                }
            } else {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording mComponent is null.");
            }
        } catch (IOException e) {
            if (null != onVideoRecordListener) {
                CameraData cameraData = new CameraData();
                cameraData.setRetCode(CAMERA_ERROR);
                cameraData.setMsg(CAMERA_ERROR_MESSAGE + " video create file exception.");
                onVideoRecordListener.onVideoRecordCallback(cameraData);
            }
            Log.e(
                    TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording ioexception : "
                            + e.getMessage());
            return;
        }
        if (null == mTmpFile) {
            if (null != onVideoRecordListener) {
                CameraData cameraData = new CameraData();
                cameraData.setRetCode(CAMERA_ERROR);
                cameraData.setMsg(CAMERA_ERROR_MESSAGE + " video create file fail.");
                onVideoRecordListener.onVideoRecordCallback(cameraData);
            }
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording mTmpFile is null.");
        }
        if (null != mRenderer) {
            mRenderer.setOnVideoStartedListener(
                    mTmpFile,
                    new CameraSurfaceRender.OnVideoStatusListener() {
                        @Override
                        public void onVideoStarted() {
                            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording onVideoStarted.");
                            if (mMaxVideoDuration > VIDEO_MAX_DURATION || mMaxVideoDuration < 0) {
                                mMaxVideoDuration = VIDEO_MAX_DURATION;
                            }
                            if (null != mMainHandler && null != mStopVideoRunnable) {
                                mMainHandler
                                        .postDelayed(mStopVideoRunnable, mMaxVideoDuration * 1000);
                            } else {
                                Log.w(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG
                                                +
                                                "onVideoStarted mMainHandler  or mStopVideoRunnable  is null.");
                            }
                            if (null != onVideoRecordListener) {
                                CameraData cameraData = new CameraData();
                                cameraData.setRetCode(CAMERA_OK);
                                cameraData.setMsg(CAMERA_OK_MESSAGE);
                                onVideoRecordListener.onVideoRecordCallback(cameraData);
                            } else {
                                Log.w(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG
                                                + "onVideoStarted onVideoRecordListener  is null.");
                            }
                            mCurrentStarted = true;
                        }

                        @Override
                        public void onVideoStoped() {
                            if (null != mMainHandler && null != mStopVideoRunnable) {
                                mMainHandler.removeCallbacks(mStopVideoRunnable);
                                mStopVideoRunnable = null;
                            } else {
                                Log.w(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG
                                                +
                                                "onVideoStoped mMainHandler or  mStopVideoRunnable is null.");
                            }
                            if (null != mOnVideoStopListener) {
                                CameraData cameraData = new CameraData();
                                if (null != mTmpFile) {
                                    cameraData.setUrl(Uri.fromFile(mTmpFile));
                                    cameraData.setRetCode(CAMERA_OK);
                                    cameraData.setMsg(CAMERA_OK_MESSAGE);
                                    getVideoThumbnailUrl(null, cameraData, mTmpFile);
                                } else {
                                    cameraData.setRetCode(CAMERA_ERROR);
                                    cameraData.setMsg(CAMERA_ERROR_MESSAGE);
                                    mOnVideoStopListener.onVideoRecordCallback(cameraData);
                                }
                            } else {
                                Log.w(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG
                                                + "onVideoStoped mOnVideoStopListener is null.");
                            }
                            if (mIsRecordTimeout) {
                                if (null != onVideoRecordListener) {
                                    CameraData cameraData = new CameraData();
                                    if (null != mTmpFile) {
                                        cameraData.setUrl(Uri.fromFile(mTmpFile));
                                        cameraData.setRetCode(CameraView.CAMERA_TIMEOUT);
                                        cameraData.setMsg(CameraView.CAMERA_TIMEOUT_MESSAGE);
                                        getVideoThumbnailUrl(onVideoRecordListener, cameraData,
                                                mTmpFile);
                                    } else {
                                        cameraData.setRetCode(CAMERA_ERROR);
                                        cameraData.setMsg(CAMERA_ERROR_MESSAGE);
                                        onVideoRecordListener.onVideoRecordCallback(cameraData);
                                    }
                                } else {
                                    Log.w(
                                            TAG,
                                            CameraBaseMode.VIDEO_RECORD_TAG
                                                    +
                                                    "onVideoStoped mIsRecordTimeout onVideoRecordListener is null.");
                                }
                                mIsRecordTimeout = false;
                            }
                            if (null == mOnVideoStopListener) {
                                cleanVideoMode();
                            }
                            mCurrentStarted = false;
                        }
                    });
        } else {
            Log.w(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "startRecording setOnVideoStartedListener  mRenderer is null.");
        }
        if (null != mSurfaceView) {
            mSurfaceView.queueEvent(
                    new Runnable() {
                        @Override
                        public void run() {
                            // notify the renderer that we want to change the encoder's state
                            if (null != mStopVideoRunnable && null != mMainHandler) {
                                mMainHandler.removeCallbacks(mStopVideoRunnable);
                            } else {
                                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                        + "startRecording mSurfaceView.queueEvent mStopVideoRunnable"
                                        + " or mMainHandler is null.");
                            }
                            mStopVideoRunnable =
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!mRecordingEnabled) {
                                                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                                        + "startRecording queueEvent stopRecording"
                                                        + " mStopVideoRunnable mRecordingEnabled : "
                                                        + mRecordingEnabled);
                                                return;
                                            } else {
                                                if (null != mRenderer) {
                                                    mIsRecordTimeout = true;
                                                    mOnVideoStopListener = null;
                                                    Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startRecording queueEvent stopRecording mRecordingEnabled true.");
                                                    stopRecording(null);
                                                } else {
                                                    Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                                            + "startRecording queueEvent stopRecording  mRenderer is null.");
                                                }
                                            }
                                        }
                                    };
                            if (null != mRenderer && mRecordingEnabled) {
                                mRenderer.startRecording(mRecordingEnabled, mTmpFile, compressed);
                            } else {
                                Log.w(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG
                                                +
                                                "startRecording queueEvent startRecording  mRenderer is null, mRecordingEnabled  : " + mRecordingEnabled);
                            }
                        }
                    });
        }
    }

    private void getVideoThumbnailUrl(
            CameraView.OnVideoRecordListener onVideoRecordListener, CameraData cameraData,
            File file) {
        CameraView.OnVideoRecordListener tmpOnVideoRecordListener = mOnVideoStopListener;
        if (mIsRecordTimeout) {
            tmpOnVideoRecordListener = onVideoRecordListener;
            Log.w(TAG, "getVideoThumbnailUrl mIsRecordTimeout is true.");
        }
        final CameraView.OnVideoRecordListener realOnVideoRecordListener = tmpOnVideoRecordListener;
        if (null == file || !file.exists()) {
            Log.e(TAG, "getVideoThumbnailUrl file null or not exists.");
            if (null != realOnVideoRecordListener) {
                realOnVideoRecordListener.onVideoRecordCallback(cameraData);
            } else {
                Log.w(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "getVideoThumbnailUrl realOnVideoRecordListener is null.");
            }
            return;
        }
        String filePath = file.getAbsolutePath();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(filePath);
        } catch (Exception e) {
            Log.w(TAG, "getVideoThumbnailUrl setDataSource error : " + e.getMessage());
        }
        final Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime();
        if (null != bitmap) {
            Executors.io()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    Bitmap thumbBitmap = bitmap;
                                    if (null != mCameraView) {
                                        int width = mCameraView.getWidth();
                                        int height = mCameraView.getHeight();
                                        // 尺寸，对于压缩情况则是video的宽高，对于非压缩情况则是video的宽高的一半(注意取2)
                                        if (width > 0 && height > 0) {
                                            if (mCompressed) {
                                                thumbBitmap =
                                                        ThumbnailUtils.extractThumbnail(
                                                                bitmap,
                                                                width,
                                                                height,
                                                                ThumbnailUtils.OPTIONS_RECYCLE_INPUT); // 指定视频缩略图的大小
                                            } else {
                                                thumbBitmap =
                                                        ThumbnailUtils.extractThumbnail(
                                                                bitmap,
                                                                width / 2,
                                                                height / 2,
                                                                ThumbnailUtils.OPTIONS_RECYCLE_INPUT); // 指定视频缩略图的大小
                                            }
                                        } else {
                                            Log.e(TAG,
                                                    "getVideoThumbnailUrl width or height is 0.");
                                        }
                                    } else {
                                        Log.e(TAG, "getVideoThumbnailUrl mCameraView null.");
                                    }
                                    File posterFile = saveBitmapToFile(thumbBitmap);
                                    Uri posterFileUri = null;
                                    if (null != posterFile && posterFile.exists()) {
                                        posterFileUri = Uri.fromFile(posterFile);
                                        if (null != cameraData) {
                                            cameraData.setThumbnail(posterFileUri);
                                        }
                                    }
                                    ThreadUtils.runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (null != realOnVideoRecordListener) {
                                                    realOnVideoRecordListener
                                                            .onVideoRecordCallback(cameraData);
                                                } else {
                                                    Log.w(
                                                            TAG,
                                                            CameraBaseMode.VIDEO_RECORD_TAG
                                                            + "getVideoThumbnailUrl ThreadUtils.runOnUiThread "
                                                            + " realOnVideoRecordListener is null.");
                                                }
                                            }
                                        });
                                    if (null != bitmap) {
                                        bitmap.recycle();
                                    }
                                }
                            });
        } else {
            Log.e(TAG, "getVideoPosterUrl bitmap null.");
            if (null != realOnVideoRecordListener) {
                realOnVideoRecordListener.onVideoRecordCallback(cameraData);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                        + "getVideoThumbnailUrl bitmap null, realOnVideoRecordListener is null.");
            }
        }
        mediaMetadataRetriever.release();
    }

    private File saveBitmapToFile(Bitmap bm) {
        BufferedOutputStream bos = null;
        File mTmpCamerafile = null;
        try {
            if (null != mComponent) {
                RenderEventCallback callback = mComponent.getCallback();
                if (null != callback) {
                    mTmpCamerafile = callback.createFileOnCache("video_poster", ".jpg");
                    bos = new BufferedOutputStream(new FileOutputStream(mTmpCamerafile));
                    if (null != bm) {
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    } else {
                        Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                + "saveBitmapToFile error destBitMap null.");
                    }
                } else {
                    Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "saveBitmapToFile callback null.");
                }
            } else {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "saveBitmapToFile mComponent null.");
            }
        } catch (Exception e) {
            Log.e(
                    TAG, CameraBaseMode.VIDEO_RECORD_TAG + " saveBitmapToFile exception : "
                            + e.getMessage());
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    FileUtils.closeQuietly(bos);
                }
                if (bm != null) {
                    bm.recycle();
                }
            } catch (IOException e) {
                Log.e(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "saveBitmapToFile bos IOException : "
                                + e.getMessage());
            }
        }
        return mTmpCamerafile;
    }

    public void stopRecording(CameraView.OnVideoRecordListener onVideoRecordListener) {
        if (!mRecordingEnabled) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "stopRecording mRecordingEnabled : "
                            + mRecordingEnabled);
            if (null == onVideoRecordListener) {
                if (mIsDestroy && null != mSurfaceView) {
                    mSurfaceView.onPause();
                }
                cleanVideoMode();
            }
            return;
        }
        if (!mCurrentStarted) {
            if (null != mStopDelayVideoRunnable && mIsDelayStop) {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mIsDelayStop true mStopDelayVideoRunnable not null.");
                return;
            }
            if (null != mStopDelayVideoRunnable && null != mMainHandler) {
                mMainHandler.removeCallbacks(mStopDelayVideoRunnable);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mStopDelayVideoRunnable or mMainHandler is null.");
            }
            mStopDelayVideoRunnable = new Runnable() {
                @Override
                public void run() {
                    mIsDelayStop = false;
                    if (null != mRenderer) {
                        mRecordingEnabled = false;
                        mCurrentStarted = false;
                        mOnVideoStopListener = null;
                        Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mStopDelayVideoRunnable run.");
                        mRenderer.stopRecording(false, false);
                    } else {
                        Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mStopDelayVideoRunnable run, mRenderer is null.");
                    }
                }
            };
            if (null != mMainHandler && null != mStopDelayVideoRunnable) {
                mIsDelayStop = true;
                mMainHandler.postDelayed(mStopDelayVideoRunnable, DELAY_STOP_TIME * 1000);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording postDelayed mMainHandler  or mStopDelayVideoRunnable  is null.");
            }
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mCurrentStarted false.");
            if (null != onVideoRecordListener) {
                CameraData cameraData = new CameraData();
                cameraData.setRetCode(CAMERA_ERROR);
                cameraData.setMsg(CAMERA_ERROR_MESSAGE + " video startRecording is not ready,stop error.");
                onVideoRecordListener.onVideoRecordCallback(cameraData);
            }
            return;
        }
        if (null != mStopDelayVideoRunnable && null != mMainHandler) {
            mIsDelayStop = false;
            mMainHandler.removeCallbacks(mStopDelayVideoRunnable);
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording removeCallbacks mStopDelayVideoRunnable or mMainHandler is null.");
        }
        mRecordingEnabled = false;
        mCurrentStarted = false;
        mOnVideoStopListener = onVideoRecordListener;
        if (null != mRenderer) {
            mRenderer
                    .stopRecording(mRecordingEnabled, null == onVideoRecordListener ? true : false);
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording error mRenderer is null.");
        }
    }

    @Override
    public void onViewDetachFromWindow() {
        super.onViewDetachFromWindow();
        onRecordDestroy();
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (null != mCameraView) {
            Camera camera = mCameraView.mCamera;
            Camera.Parameters parameters = null;
            Camera.Size size = null;
            if (null != camera) {
                parameters = camera.getParameters();
            }
            if (null != parameters) {
                size = parameters.getPreviewSize();
            }
            if (null != size) {
                onRecordResume(size.width, size.height);
            }
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        mIsPaused = true;
        mInitFrameAvailable = false;
        if (null != mRenderer) {
            mRenderer.refreshSurfaceTextureStatus(false);
        }
        if (null != sVideoEncoder) {
            sVideoEncoder.resetStatus();
        }
        onRecordPause();
    }

    protected void onRecordResume(int cameraPreviewWidth, int cameraPreviewHeight) {
        mIsPaused = false;
        if (null == mCameraView || (null != mCameraView && !mCameraView.mIsHasPermission)) {
            Log.w(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "onRecordResume -- acquiring camera  isHasPermission : "
                            + (null != mCameraView ? mCameraView.mIsHasPermission :
                            " null mCameraView"));
            return;
        }
        if (null != mSurfaceView) {
            mSurfaceView.onResume();
            if (null != mRenderer) {
                mRenderer.setCameraPreviewSize(cameraPreviewWidth, cameraPreviewHeight);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onRecordResume mRenderer is null.");
            }
        }
    }

    protected void onRecordPause() {
        if (null != mStopVideoRunnable && null != mMainHandler) {
            mMainHandler.removeCallbacks(mStopVideoRunnable);
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onRecordPause mStopVideoRunnable or mMainHandler is null.");
        }
        if (null != mSurfaceView) {
            mSurfaceView.onPause();
            if (null != mRenderer) {
                mRenderer.notifyPausing();
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onRecordPause mRenderer is null.");
            }
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " onRecordPause stopRecord .");
            stopRecording(null);
            MediaMuxerController mMuxer = MediaMuxerController.getInstance("");
            if (null != mMuxer) {
                mMuxer.stopMuxer();
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onRecordPause mMuxer null or mCurrentStarted false.");
            }
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onRecordPause mSurfaceView is null.");
        }
        if (null != mCameraView) {
            mCameraView.releaseCamera();
        }
    }

    protected void onRecordDestroy() {
        if (null != mCameraHandler) {
            mCameraHandler.invalidateHandler();
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onRecordDestroy mCameraHandler is null.");
        }
    }

    public void onCameraDestroy() {
        mIsDestroy = true;
        if (mRecordingEnabled) {
            mRecordingEnabled = false;
            mCurrentStarted = false;
            mOnVideoStopListener = null;
            if (null != mRenderer) {
                mRenderer.stopRecording(mRecordingEnabled, true);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onCameraDestroy error mRenderer is null.");
            }
        }
    }

    private void cleanVideoMode() {
        if (mIsDestroy) {
            mCameraView = null;
            mSurfaceView = null;
            if (null != mRenderer) {
                mRenderer.onCameraDestroy();
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "cleanVideoMode mRenderer is null.");
            }
            mRenderer = null;
            if (null != mCameraHandler) {
                mCameraHandler.removeCallbacksAndMessages(null);
            } else {
                Log.w(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                +
                                "cleanVideoMode removeCallbacksAndMessages mCameraHandler is null.");
            }
            if (null != mCameraHandler) {
                mCameraHandler.invalidateHandler();
            } else {
                Log.w(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "cleanVideoMode invalidateHandler mCameraHandler is null.");
            }
            mCameraHandler = null;
            mMainHandler = null;
            mOnVideoStopListener = null;
            if (null != mSurfaceTexture) {
                mSurfaceTexture.setOnFrameAvailableListener(null);
            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                        + "cleanVideoMode mSurfaceTexture is null.");
            }
            mSurfaceTexture = null;
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "cleanVideoMode mIsDestroy false.");
        }
    }

    public static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;
        public static final int MSG_SURFACE_CREATED = 1;
        public static final int MSG_SURFACE_CHANGED = 2;
        public static final String KEY_SURFACE_WIDTH = "width";
        public static final String KEY_SURFACE_HEIGHT = "height";
        public static final String KEY_IS_NEED_STARTPREVIEW = "isNeedStartPreview";
        private WeakReference<VideoRecordMode> mWeakVideoRecordMode;

        public CameraHandler(VideoRecordMode videoRecordMode) {
            mWeakVideoRecordMode = new WeakReference<VideoRecordMode>(videoRecordMode);
        }

        public WeakReference<VideoRecordMode> getWeakVideoRecordMode() {
            return mWeakVideoRecordMode;
        }

        public void resetWeakRefVideoRecord(VideoRecordMode videoRecordMode) {
            mWeakVideoRecordMode = new WeakReference<VideoRecordMode>(videoRecordMode);
        }

        public void invalidateHandler() {
            if (null != mWeakVideoRecordMode) {
                mWeakVideoRecordMode.clear();
            }
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            VideoRecordMode videoRecordMode = mWeakVideoRecordMode.get();
            if (videoRecordMode == null) {
                Log.w(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "CameraHandler.handleMessage: videoRecordMode is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    if (inputMessage.obj instanceof SurfaceTexture) {
                        Bundle bundle = inputMessage.getData();
                        boolean isNeedStartPreview = true;
                        if (null != bundle) {
                            isNeedStartPreview = bundle.getBoolean(KEY_IS_NEED_STARTPREVIEW, true);
                        }
                        videoRecordMode.handleSetSurfaceTexture(
                                (SurfaceTexture) inputMessage.obj, isNeedStartPreview);
                    }

                    break;
                case MSG_SURFACE_CREATED:
                    break;
                case MSG_SURFACE_CHANGED:
                    Bundle bundle = inputMessage.getData();
                    if (null != bundle) {
                        int width = bundle.getInt(KEY_SURFACE_WIDTH);
                        int height = bundle.getInt(KEY_SURFACE_HEIGHT);
                        if (inputMessage.obj instanceof SurfaceTexture) {
                            videoRecordMode.handleSurfaceChanged(
                                    width, height, (SurfaceTexture) inputMessage.obj);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
