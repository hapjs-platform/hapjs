/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.common;

import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.lang.ref.WeakReference;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.record.TextureMovieEncoder;

public class VideoHandlerThread extends HandlerThread {
    public static final int MSG_STOP_RECORDING = 1;
    public static final int MSG_FRAME_AVAILABLE = 2;
    public static final int MSG_SET_TEXTURE_ID = 3;
    public static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    public static final int MSG_QUIT = 5;
    public static final int MSG_PREPARE_RECORDING = 6;
    private static final String TAG = "VideoHandlerThread";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private WeakReference<TextureMovieEncoder> mWeakEncoder;
    private volatile boolean mIsHandleMessage = true;

    private VideoHandlerThread(String name) {
        super(name);
    }

    public static VideoHandlerThread getInstance() {
        return HandlerThreadHolder.sVideoHandlerThread;
    }

    public Handler getHandler(TextureMovieEncoder encoder) {
        mHandlerThread = HandlerThreadHolder.sVideoHandlerThread;
        mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        if (null == mHandler) {
            mHandler =
                    new Handler(mHandlerThread.getLooper()) {
                        @Override
                        public void handleMessage(Message inputMessage) {
                            if (!mIsHandleMessage) {
                                Log.w(TAG, "handleMessage mIsHandleMessage is false.");
                                return;
                            }
                            int what = inputMessage.what;
                            Object obj = inputMessage.obj;

                            TextureMovieEncoder encoder = mWeakEncoder.get();
                            if (encoder == null) {
                                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                        + "handleMessage: encoder is null");
                                return;
                            }

                            switch (what) {
                                case MSG_PREPARE_RECORDING:
                                    Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "handleMessage handlePrepareRecording .");
                                    mIsHandleMessage = true;
                                    encoder.handlePrepareRecording(
                                            (TextureMovieEncoder.EncoderConfig) obj);
                                    break;
                                case MSG_STOP_RECORDING:
                                    Log.d(
                                            TAG, CameraBaseMode.VIDEO_RECORD_TAG
                                                    + "handleMessage handleStopRecording .");
                                    try {
                                        encoder.handleStopRecording();
                                    } catch (Exception e) {
                                        Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "handleMessage handleStopRecording error : " + e.getMessage());
                                    }
                                    break;
                                case MSG_FRAME_AVAILABLE:
                                    long timestamp =
                                            (((long) inputMessage.arg1) << 32)
                                                    | (((long) inputMessage.arg2) & 0xffffffffL);
                                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                                    break;
                                case MSG_SET_TEXTURE_ID:
                                    encoder.handleSetTexture(inputMessage.arg1);
                                    break;
                                case MSG_UPDATE_SHARED_CONTEXT:
                                    Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "handleMessage handleUpdateSharedContext .");
                                    encoder.handleUpdateSharedContext(
                                            (EGLContext) inputMessage.obj);
                                    break;
                                case MSG_QUIT:
                                    Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "handleMessage handleVideoStop .");
                                    mIsHandleMessage = false;
                                    encoder.handleVideoStop();
                                    if (null != mHandler) {
                                        mHandler.removeCallbacksAndMessages(null);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    };
        }
        return mHandler;
    }

    @Override
    protected void onLooperPrepared() {
        if (null != mWeakEncoder) {
            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onLooperPrepared encoder is null");
                return;
            } else {
                encoder.handleLooperReady();
            }
        } else {
            Log.w(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onLooperPrepared mWeakEncoder is null");
        }
    }

    public void startHandlerMessage() {
        mIsHandleMessage = true;
    }

    public static class HandlerThreadHolder {
        static VideoHandlerThread sVideoHandlerThread;

        static {
            sVideoHandlerThread = new VideoHandlerThread(TAG);
            sVideoHandlerThread.start();
        }
    }
}
