/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record;

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;

import org.hapjs.common.executors.Executors;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.record.common.VideoHandlerThread;
import org.hapjs.widgets.view.camera.record.gles.EglCore;
import org.hapjs.widgets.view.camera.record.gles.FullFrameRect;
import org.hapjs.widgets.view.camera.record.gles.Texture2dProgram;
import org.hapjs.widgets.view.camera.record.gles.WindowSurface;

public class TextureMovieEncoder {
    private static final String TAG = "TextureMovieEncoder";
    private static final boolean VERBOSE = false;
    private static final int BASE_WIDHT = 720;
    private static final int BASE_HEIGHT = 1280;
    private static final float PX_240_WIDTH = 240.0f;
    private static final float PX_360_WIDTH = 360.0f;
    private static final float PX_480_WIDTH = 480.0f;
    private static final float PX_720_WIDTH = 720.0f;
    private static final int BITRATE_240_WIDTH = 576000;
    private static final int BITRATE_360_WIDTH = 704000;
    private static final int BITRATE_480_WIDTH = 896000;
    private static final int BITRATE_720_WIDTH = 1856000;
    private static volatile long mStartTime = -1;
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private FullFrameRect mFullRecordScreen;
    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;
    private volatile Handler mHandler;
    private Object mReadyFence = new Object();
    private volatile boolean mReady;
    private volatile boolean mIsStarted;
    private volatile boolean mRunning;
    private EncoderConfig mConfig = null;
    private float mCalWidth = 0.0f;
    private float mCalHeight = 0.0f;
    private int mBitRate = -1;
    public volatile boolean mIsEGLvalid = true;
    private volatile boolean mIsprepare = false;

    /**
     * Adds a bit of extra stuff to the display just to give it flavor.
     */
    private static void drawExtra(int frameNum, int width, int height) {
        // We "draw" with the scissor rect and clear calls.  Note this uses window coordinates.
        int val = frameNum % 3;
        switch (val) {
            case 0:
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
                break;
            case 1:
                GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                break;
            case 2:
                GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
                break;
            default:
                break;
        }

        int xpos = (int) (width * ((frameNum % 100) / 100.0f));
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, width / 32, height / 32);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public boolean isContextSurfaceAttached() {
        if (null == mInputWindowSurface) {
            return !mIsprepare;
        }
        return mIsprepare;
    }

    public void prepareRecording(EncoderConfig config, FullFrameRect fullFrameRect) {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "Encoder thread already running");
                return;
            }
            mRunning = true;
            if (null != config
                    && null != config.mOutputFile
                    && !TextUtils.isEmpty(config.mOutputFile.toString())) {
                MediaMuxerController mMuxer =
                        MediaMuxerController.getInstance(config.mOutputFile.toString());
                if (null != mMuxer) {
                    mMuxer.addVideoEncoder(this);
                } else {
                    Log.e(TAG,
                            CameraBaseMode.VIDEO_RECORD_TAG + "prepareRecording mMuxer is null.");
                }
            }
            if (mHandler == null) {
                mHandler = VideoHandlerThread.getInstance().getHandler(this);
                while (!mReady) {
                    try {
                        mReadyFence.wait();
                    } catch (InterruptedException ie) {
                        Log.w(
                                TAG,
                                CameraBaseMode.VIDEO_RECORD_TAG
                                        + "prepareRecording InterruptedException : "
                                        + ie.getMessage());
                    }
                }
            } else {
                synchronized (mReadyFence) {
                    mReady = true;
                }
            }
        }
        mFullScreen = fullFrameRect;
        VideoHandlerThread.getInstance().startHandlerMessage();
        getHandler()
                .sendMessage(getHandler()
                        .obtainMessage(VideoHandlerThread.MSG_PREPARE_RECORDING, config));
    }

    public void startVideoRecording() {
        mIsStarted = true;
    }

    public void resetStatus() {
        synchronized (mReadyFence) {
            mReady = mRunning = false;
        }
        mIsStarted = false;
        mStartTime = -1;
        mIsEGLvalid = true;
        mIsprepare = false;
    }

    public void stopRecording(boolean isDetachStop) {
        mIsStarted = false;
        synchronized (mReadyFence) {
            mReady = mRunning = false;
        }
        if (null != mHandler) {
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mHandler is not null.");
            mHandler.sendMessage(mHandler.obtainMessage(VideoHandlerThread.MSG_STOP_RECORDING));
            mHandler.sendMessage(mHandler.obtainMessage(VideoHandlerThread.MSG_QUIT));
        } else {
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording mHandler is null.");
            Executors.io().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        handleStopRecording();
                    } catch (Exception e) {
                        Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "stopRecording handleStopRecording error : " + e.getMessage());
                    }
                    handleVideoStop();
                    if (null != mHandler) {
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }
            });
        }
        mStartTime = -1;
    }

    public Handler getHandler() {
        if (null == mHandler) {
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " getHandler mHandler is null.");
            mHandler = VideoHandlerThread.getInstance().getHandler(this);
        }
        return mHandler;
    }

    /**
     * 正在录制返回true
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface. (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        getHandler()
                .sendMessage(
                        getHandler()
                                .obtainMessage(VideoHandlerThread.MSG_UPDATE_SHARED_CONTEXT,
                                        sharedContext));
    }

    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady || !mIsStarted) {
                return;
            }
        }
        // video queue start
        if (mStartTime == -1) {
            mStartTime = System.currentTimeMillis();
        }
        // video queue end
        float[] transform = new float[16];
        st.getTransformMatrix(transform);
        // video queue start
        long timestamp = (System.currentTimeMillis() - mStartTime);
        if (MediaMuxerController.DEBUG) {
            Log.d(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP
                            + " start frameAvailable video_timestamp : "
                            + timestamp);
        }
        // video queue end
        if (timestamp == 0) {
            Log.w(TAG, "frameAvailable got SurfaceTexture with timestamp of zero");
            return;
        }

        if (null != mHandler) {
            mHandler.sendMessage(mHandler.obtainMessage(VideoHandlerThread.MSG_FRAME_AVAILABLE,
                    (int) (timestamp >> 32), (int) timestamp, transform));
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP + "frameAvailable MSG_FRAME_AVAILABLE mHandler null.");
        }
    }

    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (null != mHandler) {
            mHandler.sendMessage(mHandler.obtainMessage(VideoHandlerThread.MSG_SET_TEXTURE_ID, id, 0, null));
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP + "setTextureId MSG_SET_TEXTURE_ID mHandler null.");
        }
    }

    public void handleLooperReady() {
        synchronized (mReadyFence) {
            mReady = true;
            mReadyFence.notifyAll();
        }
    }

    public void handleVideoStop() {
        synchronized (mReadyFence) {
            mReady = mRunning = false;
        }
    }

    /**
     * Starts recording.
     */
    public void handlePrepareRecording(EncoderConfig config) {
        mConfig = config;
        mFrameNum = 0;
        prepareEncoder(
                config.mEglContext,
                config.mWidth,
                config.mHeight,
                config.mBitRate,
                config.mIsCompress,
                config.mOutputFile);
    }

    /**
     * 接收有效帧数据
     *
     * @param transform      The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    public void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE) {
            Log.d(TAG, "handleFrameAvailable tr=" + transform);
        }
        // video queue start
        mVideoEncoder.drainEncoder(false);
        // video queue end
        if (null != mFullRecordScreen) {
            mFullRecordScreen.drawFrame(mTextureId, transform);
        }
        // video queue start
        if (MediaMuxerController.DEBUG) {
            Log.d(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG_TIMESTAMP
                            + " after start handleFrameAvailable video_timestamp : "
                            + timestampNanos);
        }
        mInputWindowSurface.setPresentationTime(timestampNanos * 1000000);
        // video queue end
        mInputWindowSurface.swapBuffers();
    }

    /**
     * Handles a request to stop encoding.
     */
    public void handleStopRecording() {
        if (null != mVideoEncoder) {
            mVideoEncoder.drainEncoder(true);
            releaseEncoder();
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "handleStopRecording mVideoEncoder is null.");
        }
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    public void handleSetTexture(int id) {
        mTextureId = id;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input surface,
     * and replaces it with a new one that shares with the new context.
     *
     * <p>This is useful if the old context we were sharing with went away (maybe a GLSurfaceView that
     * got torn down) and we need to hook up with the new one.
     */
    public void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);
        mIsEGLvalid = false;
        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mFullScreen.release(false);
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        mFullScreen =
                new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mIsEGLvalid = true;
    }

    private void prepareEncoder(
            EGLContext sharedContext,
            int width,
            int height,
            int bitRate,
            boolean isCompress,
            File outputFile) {
        try {
            float realHeight = height;
            float realwidth = width;
            if (isCompress) {
                calculateWidthHeight(width, height);
                realHeight = mCalHeight;
                realwidth = mCalWidth;
                if (width < height) {
                    if (realwidth > realHeight) {
                        realwidth = mCalHeight;
                        realHeight = mCalWidth;
                    }
                } else {
                    if (realwidth < realHeight) {
                        realwidth = mCalHeight;
                        realHeight = mCalWidth;
                    }
                }
            }
            int formatWidth = (int) realwidth;
            int formatHeight = (int) realHeight;
            if ((formatWidth & 1) == 1) {
                formatWidth--;
            }
            if ((formatHeight & 1) == 1) {
                formatHeight--;
            }
            if (formatWidth <= 0 || formatHeight <= 0) {
                Log.e(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "prepareEncoder formatWidth : "
                                + formatWidth
                                + " formatHeight : "
                                + formatHeight);
                return;
            }
            mVideoEncoder =
                    new VideoEncoderCore(formatWidth, formatHeight, isCompress, outputFile,
                            mBitRate);
        } catch (IOException ioe) {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG + "prepareEncoder IOException : "
                            + ioe.getMessage());
        }
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();
        if (null == mFullRecordScreen) {
            mFullRecordScreen = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "mFullRecordScreen is null.");
        }

        mIsprepare = true;
    }

    /**
     * baseValue/ heightV = tmpWidth / tmpHeight
     *
     * @param viewWidth
     * @param viewHeight
     */
    private void calculateWidthHeight(int viewWidth, int viewHeight) {
        int tmpWidth = viewWidth;
        int tmpHeight = viewHeight;
        if (viewWidth > viewHeight) {
            tmpWidth = viewHeight;
            tmpHeight = viewWidth;
        }
        mCalWidth = tmpWidth;
        mCalHeight = tmpHeight;
        if (tmpWidth > PX_720_WIDTH) {
            mCalWidth = PX_720_WIDTH;
            mCalHeight = PX_720_WIDTH * tmpHeight / tmpWidth;
            mBitRate = BITRATE_720_WIDTH;
        } else if (tmpWidth > PX_480_WIDTH) {
            mCalWidth = PX_480_WIDTH;
            mCalHeight = PX_480_WIDTH * tmpHeight / tmpWidth;
            mBitRate = BITRATE_480_WIDTH;
        } else if (tmpWidth > PX_360_WIDTH) {
            mCalWidth = PX_360_WIDTH;
            mCalHeight = PX_360_WIDTH * tmpHeight / tmpWidth;
            mBitRate = BITRATE_360_WIDTH;
        } else if (tmpWidth > PX_240_WIDTH) {
            mCalWidth = PX_240_WIDTH;
            mCalHeight = PX_240_WIDTH * tmpHeight / tmpWidth;
            mBitRate = BITRATE_240_WIDTH;
        }
    }

    private void releaseEncoder() {
        mIsprepare = false;
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullRecordScreen != null) {
            mFullRecordScreen.release(false);
            mFullRecordScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    /**
     * Draws a box, with position offset.
     */
    private void drawBox(int posn) {
        //        final int width = mInputWindowSurface.getWidth();
        //        int xpos = (posn * 4) % (width - 50);
        //        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        //        GLES20.glScissor(xpos, 0, 100, 100);
        //        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        //        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;
        boolean mIsCompress = false;

        public EncoderConfig(
                File outputFile,
                int width,
                int height,
                int bitRate,
                boolean isCompress,
                EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
            mIsCompress = isCompress;
        }

        @Override
        public String toString() {
            return "EncoderConfig: "
                    + mWidth
                    + "x"
                    + mHeight
                    + " @"
                    + mBitRate
                    + " to '"
                    + mOutputFile.toString()
                    + "' ctxt="
                    + mEglContext;
        }
    }
}
