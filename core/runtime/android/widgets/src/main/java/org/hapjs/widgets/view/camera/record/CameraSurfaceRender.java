/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record;

import static android.opengl.Matrix.setIdentityM;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import androidx.annotation.RequiresApi;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.CameraView;
import org.hapjs.widgets.view.camera.VideoRecordMode;
import org.hapjs.widgets.view.camera.record.gles.Drawable2d;
import org.hapjs.widgets.view.camera.record.gles.FullFrameRect;
import org.hapjs.widgets.view.camera.record.gles.GlUtil;
import org.hapjs.widgets.view.camera.record.gles.Texture2dProgram;

public class CameraSurfaceRender implements GLSurfaceView.Renderer {
    // Camera filters; must match up with cameraFilterNames in strings.xml
    static final int FILTER_NONE = 0;
    static final int FILTER_BLACK_WHITE = 1;
    static final int FILTER_BLUR = 2;
    static final int FILTER_SHARPEN = 3;
    static final int FILTER_EDGE_DETECT = 4;
    static final int FILTER_EMBOSS = 5;
    private static final String TAG = "CameraSurfaceRender";
    private static final boolean VERBOSE = false;
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private final float[] mSTMatrix = new float[16];
    private final float[] mGlPosMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    public float mlipEndY = 1.0f;
    public float mclipEndX = 1.0f;
    private VideoRecordMode.CameraHandler mCameraHandler;
    private TextureMovieEncoder mVideoEncoder;
    private File mOutputFile;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private volatile SurfaceTexture mSurfaceTexture;
    private boolean mRecordingEnabled;
    private boolean mCompressed;
    private int mRecordingStatus;
    private int mFrameCount;
    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;
    private int mCurrentFilter;
    private int mNewFilter;
    private CameraView mCameraView = null;
    private int mCurrentSurfaceWidth = 0;
    private int mCurrentSurfaceHeight = 0;
    private float[] mFullRectangleTexCoords = null;
    private boolean mIsFrameAvailable = false;
    private volatile boolean mIsBindCamera = false;
    private boolean mIsLastError = false;
    private boolean mIsGlError = false;

    public CameraSurfaceRender(
            VideoRecordMode.CameraHandler cameraHandler, TextureMovieEncoder movieEncoder) {
        mCameraHandler = cameraHandler;
        mVideoEncoder = movieEncoder;
        mTextureId = -1;
        mRecordingStatus = -1;
        mRecordingEnabled = false;
        mFrameCount = -1;
        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;
        // We could preserve the old filter mode, but currently not bothering.
        mCurrentFilter = -1;
        mNewFilter = FILTER_NONE;
        WeakReference<VideoRecordMode> weakVideoRecordMode =
                mCameraHandler.getWeakVideoRecordMode();
        if (null != weakVideoRecordMode) {
            VideoRecordMode videoRecordMode = weakVideoRecordMode.get();
            if (null != videoRecordMode && null != videoRecordMode.mCameraView) {
                mCameraView = videoRecordMode.mCameraView;
            }
        }
    }

    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                    + "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false); // assume the GLSurfaceView EGL context is about
            mFullScreen = null; //  to be destroyed
        }
        mIncomingWidth = mIncomingHeight = -1;
    }

    public void startRecording(boolean isRecording, File recordFile, boolean compressed) {

        Log.d(
                TAG,
                CameraBaseMode.VIDEO_RECORD_TAG
                        + "startRecording: was "
                        + mRecordingEnabled
                        + " now "
                        + isRecording);
        mOutputFile = recordFile;
        mCompressed = compressed;
        mRecordingEnabled = isRecording;
    }

    public void stopRecording(boolean isRecording, boolean isDetachStop) {
        Log.d(
                TAG,
                CameraBaseMode.VIDEO_RECORD_TAG
                        + "stopRecording: was "
                        + mRecordingEnabled
                        + " now "
                        + isRecording);
        mRecordingEnabled = isRecording;
        stopVideoRecord(isDetachStop);
    }

    public void changeFilterMode(int filter) {
        mNewFilter = filter;
    }

    public void updateFilter() {
        Texture2dProgram.ProgramType programType = Texture2dProgram.ProgramType.TEXTURE_EXT;
        float[] kernel = null;
        float colorAdj = 0.0f;

        Log.d(TAG, "Updating filter to " + mNewFilter);
        switch (mNewFilter) {
            case FILTER_NONE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT;
                break;
            case FILTER_BLACK_WHITE:
                // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
                // ROSE_COLORED_GLASSES, because the shader set the red channel to the B&W color
                // and green/blue to zero.)
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW;
                break;
            case FILTER_BLUR:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel =
                        new float[] {
                                1f / 16f, 2f / 16f, 1f / 16f,
                                2f / 16f, 4f / 16f, 2f / 16f,
                                1f / 16f, 2f / 16f, 1f / 16f
                        };
                break;
            case FILTER_SHARPEN:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f};
                break;
            case FILTER_EDGE_DETECT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel =
                        new float[] {
                                -1f, -1f, -1f,
                                -1f, 8f, -1f,
                                -1f, -1f, -1f
                        };
                break;
            case FILTER_EMBOSS:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel =
                        new float[] {
                                2f, 0f, 0f,
                                0f, -1f, 0f,
                                0f, 0f, -1f
                        };
                colorAdj = 0.5f;
                break;
            default:
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "Unknown filter mode " + mNewFilter);
                break;
        }

        // Do we need a whole new program?  (We want to avoid doing this if we don't have
        // too -- compiling a program could be expensive.)
        if (programType != mFullScreen.getProgram().getProgramType()) {
            mFullScreen.changeProgram(new Texture2dProgram(programType));
            // If we created a new program, we need to initialize the texture width/height.
            mIncomingSizeUpdated = true;
        }

        // Update the filter kernel (if any).
        if (kernel != null) {
            mFullScreen.getProgram().setKernel(kernel, colorAdj);
        }

        mCurrentFilter = mNewFilter;
    }

    /**
     * Records the size of the incoming camera preview frames.
     */
    public void setCameraPreviewSize(int width, int height) {
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }
    }

    public void refreshSurfaceTextureStatus(boolean isBindcamera) {
        mIsBindCamera = isBindcamera;
    }

    private void initFrameTexture() {
        if (mSurfaceTexture == null && null != mCameraHandler) {
            mFullScreen =
                    new FullFrameRect(
                            new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mTextureId = mFullScreen.createTextureObject();
            // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
            // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
            // available messages will arrive on the main thread.
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            // Tell the UI thread to enable the camera preview.
            mCameraHandler.sendMessage(
                    mCameraHandler.obtainMessage(
                            VideoRecordMode.CameraHandler.MSG_SET_SURFACE_TEXTURE,
                            mSurfaceTexture));
        }
    }

    public void createDetachedSurfaceTexture() {
        if (null != mVideoEncoder && mIsFrameAvailable && null != mCameraHandler) {
            if (null != mFullScreen) {
                Message message = new Message();
                message.what = VideoRecordMode.CameraHandler.MSG_SET_SURFACE_TEXTURE;
                Bundle bundle = new Bundle();
                bundle.putBoolean(VideoRecordMode.CameraHandler.KEY_IS_NEED_STARTPREVIEW, false);
                message.obj = mSurfaceTexture;
                message.setData(bundle);
                mCameraHandler.sendMessage(message);
            }
        }
    }

    private void notifySurfaceChange() {
        if (null != mCameraHandler && null != mSurfaceTexture) {
            Message message = new Message();
            message.what = VideoRecordMode.CameraHandler.MSG_SURFACE_CHANGED;
            Bundle bundle = new Bundle();
            message.obj = mSurfaceTexture;
            bundle.putInt(VideoRecordMode.CameraHandler.KEY_SURFACE_WIDTH, mCurrentSurfaceWidth);
            bundle.putInt(VideoRecordMode.CameraHandler.KEY_SURFACE_HEIGHT, mCurrentSurfaceHeight);
            message.setData(bundle);
            mCameraHandler.sendMessage(message);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        initMatrix(width, height);
        mCurrentSurfaceWidth = width;
        mCurrentSurfaceHeight = height;
        notifySurfaceChange();
    }

    private void initMatrix(int width, int height) {
        View layout = mCameraView;
        if (null == layout) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "initMatrix layout is null.");
            return;
        }
        setIdentityM(mGlPosMatrix, 0);
        if (width == layout.getMeasuredWidth()) {
            if ((float) height / (float) layout.getMeasuredHeight() >= 1.0f) {
                GLES20.glViewport(
                        0,
                        (height - layout.getMeasuredHeight()) / 2,
                        layout.getMeasuredWidth(),
                        layout.getMeasuredHeight());
                mlipEndY = ((float) layout.getMeasuredHeight() / (float) height);
                mFullRectangleTexCoords = new float[16];
                mFullRectangleTexCoords[0] = (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[1] = (1 - mlipEndY) / 2.0f;
                mFullRectangleTexCoords[2] = mclipEndX + (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[3] = (1.0f - mlipEndY) / 2.0f;
                mFullRectangleTexCoords[4] = (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[5] = mlipEndY + (1.0f - mlipEndY) / 2.0f;
                mFullRectangleTexCoords[6] = mclipEndX + (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[7] = mlipEndY + (1.0f - mlipEndY) / 2.0f;
                Drawable2d.setTexCoordArray(GlUtil.createFloatBuffer(mFullRectangleTexCoords));
                initFrameTexture();
            }

        } else if (height == layout.getMeasuredHeight()) {
            if ((float) width / (float) layout.getMeasuredWidth() >= 1.0f) {
                GLES20.glViewport(
                        (width - layout.getMeasuredWidth()) / 2,
                        0,
                        layout.getMeasuredWidth(),
                        layout.getMeasuredHeight());
                mclipEndX = ((float) layout.getMeasuredWidth() / (float) width);
                mFullRectangleTexCoords = new float[16];
                mFullRectangleTexCoords[0] = (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[1] = (1 - mlipEndY) / 2.0f;
                mFullRectangleTexCoords[2] = mclipEndX + (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[3] = (1.0f - mlipEndY) / 2.0f;
                mFullRectangleTexCoords[4] = (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[5] = mlipEndY + (1.0f - mlipEndY) / 2.0f;
                mFullRectangleTexCoords[6] = mclipEndX + (1.0f - mclipEndX) / 2.0f;
                mFullRectangleTexCoords[7] = mlipEndY + (1.0f - mlipEndY) / 2.0f;
                Drawable2d.setTexCoordArray(GlUtil.createFloatBuffer(mFullRectangleTexCoords));
                initFrameTexture();
            }
        }
        // orthoM();
        // setIdentityM(modelMatrix, 0);
        // rotateM(mSTMatrix,0,270f,1f,0f,0f);
        // Matrix.rotateM(mSTMatrix, 0, 90, 0, 0, 1);
        // Matrix.translateM(mSTMatrix, 0, 0, -1, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDrawFrame(GL10 unused) {
        if (null == mSurfaceTexture) {
            return;
        }
        if (VERBOSE) {
            Log.d(TAG, "onDrawFrame tex=" + mTextureId);
        }
        boolean showBox = false;

        // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
        // was there before.
        if (null != mVideoEncoder) {
            if (mVideoEncoder.mIsEGLvalid) {
                boolean isCameraValid = (null != mCameraView && mCameraView.mIsCamervalid);
                if (!isCameraValid) {
                    Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame isCameraValid false.");
                    return;
                }
                if (!mIsBindCamera || mIsLastError || mIsGlError) {
                    boolean isCrash = false;
                    try {
                        Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame updateTexImage mIsBindCamera false.");
                        mSurfaceTexture.updateTexImage();
                    } catch (Exception e) {
                        isCrash = true;
                        mIsLastError = true;
                        Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame mIsBindCamera false error : " + e.getMessage());
                    }
                    if (!isCrash) {
                        mIsLastError = false;
                    }
                } else {
                    if (mRecordingEnabled) {
                        boolean isContextSurfaceAttached = mVideoEncoder.isContextSurfaceAttached();
                        if (isContextSurfaceAttached) {
                            mSurfaceTexture.updateTexImage();
                        } else {
                            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame isContextSurfaceAttached false.");
                            try {
                                mSurfaceTexture.updateTexImage();
                            } catch (Exception e) {
                                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame isContextSurfaceAttached false error : " + e.getMessage());
                            }
                        }
                    } else {
                        mSurfaceTexture.updateTexImage();
                    }
                }

            } else {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame mIsEGLvalid  is false.");
                return;
            }
        }
        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    startVideoRecord();
                    break;
                case RECORDING_RESUMED:
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    break;
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    // stopVideoRecord();
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    break;
            }
        }

        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        //
        if (mIsBindCamera) {
            mVideoEncoder.setTextureId(mTextureId);
            // Tell the video encoder thread that a new frame is available.
            // This will be ignored if we're not actually recording.
            mVideoEncoder.frameAvailable(mSurfaceTexture);
        }

        if (!mIsFrameAvailable) {
            mIsFrameAvailable = true;
        }
        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }
        // Update the filter, if necessary.
        if (mCurrentFilter != mNewFilter) {
            updateFilter();
        }
        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }
        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawCameraFrame(mGlPosMatrix, mTextureId, mSTMatrix);
        mIsGlError = mFullScreen.mIsCurrentError;
        if (mIsGlError) {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onDrawFrame mIsGlError true.");
        }
        // Draw a flashing box if we're recording.  This only appears on screen.
        showBox = (mRecordingStatus == RECORDING_ON);
        if (showBox && (++mFrameCount & 0x04) == 0) {
            drawBox();
        }
    }

    public void prepareVideoRecord(boolean compressed) {
        View layout = mCameraView;
        if (null == layout) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "prepareVideoRecord mCameraView is null.");
            return;
        }
        // origin 1000000
        mVideoEncoder.prepareRecording(
                new TextureMovieEncoder.EncoderConfig(
                        mOutputFile,
                        layout.getMeasuredWidth(),
                        layout.getMeasuredHeight(),
                        0,
                        compressed,
                        EGL14.eglGetCurrentContext()),
                mFullScreen);
        try {
            final MediaMuxerController mMuxer =
                    MediaMuxerController.getInstance(mOutputFile.toString());
            if (null != mMuxer) {
                new MediaCodecAudioEncoder(
                        mMuxer,
                        new MediaEncoder.MediaEncoderListener() {
                            @Override
                            public void onPrepared(MediaEncoder encoder) {
                            }

                            @Override
                            public void onStopped(MediaEncoder encoder) {
                            }
                        });
                mMuxer.prepare();
            } else {
                Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "prepareVideoRecord mMuxer is null.");
            }
        } catch (IOException e) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG + "prepareVideoRecord IOException : "
                            + e.getMessage());
        }
    }

    private void startVideoRecord() {
        if (null == mCameraView || null == mOutputFile) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "startVideoRecord mCameraView or outPutFile is null.");
            return;
        }
        mRecordingStatus = RECORDING_ON;
        final MediaMuxerController mMuxer =
                MediaMuxerController.getInstance(mOutputFile.toString());
        if (null != mMuxer) {
            mMuxer.initStartStatus();
        }
        prepareVideoRecord(mCompressed);
        if (null != mMuxer) {
            mMuxer.startRecording();
        } else {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "startVideoRecord mMuxer is null.");
        }
    }

    private void stopVideoRecord(boolean isDetachStop) {
        if (null == mVideoEncoder || null == mOutputFile) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + "stopVideoRecord mVideoEncoder or mOutputFile null.");
            return;
        }
        mVideoEncoder.stopRecording(isDetachStop);
        mRecordingStatus = RECORDING_OFF;
        MediaMuxerController mMuxer = MediaMuxerController.getInstance(mOutputFile.toString());
        if (mMuxer != null) {
            Log.d(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + " stopVideoRecord success , isDetachStop  : "
                            + isDetachStop);
            mMuxer.stopRecording();
        } else {
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " stopVideoRecord error mMuxer is null.");
        }
    }

    /**
     * Draws a red box in the corner.
     */
    private void drawBox() {
        //        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        //        GLES20.glScissor(0, 0, 100, 100);
        //        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        //        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public void setOnVideoStartedListener(File file, OnVideoStatusListener onVideoStatusListener) {
        if (null != file) {
            MediaMuxerController mMuxer = MediaMuxerController.getInstance(file.toString());
            if (null != mMuxer) {
                mMuxer.setOnVideoStartedListener(onVideoStatusListener);
            }
        }
    }

    public void onCameraDestroy() {
        mCameraView = null;
        if (null != mCameraHandler) {
            mCameraHandler.removeCallbacksAndMessages(null);
            mCameraHandler = null;
        }
    }

    public interface OnVideoStatusListener {
        void onVideoStarted();

        void onVideoStoped();
    }
}
