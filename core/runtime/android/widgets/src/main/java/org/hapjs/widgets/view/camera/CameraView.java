/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.core.view.ViewCompat;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.component.Component;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.render.Page;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.widgets.view.camera.googlecameraview.AspectRatio;
import org.hapjs.widgets.view.camera.googlecameraview.Constants;
import org.hapjs.widgets.view.camera.googlecameraview.Size;
import org.hapjs.widgets.view.camera.googlecameraview.SizeMap;
import org.json.JSONException;

public class CameraView extends FrameLayout implements ComponentHost, ConfigurationManager.ConfigurationListener  {
    public static final int CAMERA_OK = 200;
    public static final int CAMERA_ERROR = 201;
    public static final int CAMERA_TAKEPHOTO_ERROR = 202;
    public static final int CAMERA_TIMEOUT = 203;
    public static final String CAMERA_MESSAGE = "message";
    public static final String CAMERA_OK_MESSAGE = "success";
    public static final String CAMERA_ERROR_MESSAGE = "failure";
    public static final String CAMERA_TIMEOUT_MESSAGE = "timeout";
    public static final String SCENE_MODE_ACTION = "action";
    public static final String SCENE_MODE_PORTRAIT = "portrait";
    public static final String SCENE_MODE_LANDSCAPE = "landscape";
    public static final String SCENE_MODE_NIGHT = "night";
    public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
    public static final String SCENE_MODE_THEATRE = "theatre";
    public static final String SCENE_MODE_BEACH = "beach";
    public static final String SCENE_MODE_SNOW = "snow";
    public static final String SCENE_MODE_SUNSET = "sunset";
    public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
    public static final String SCENE_MODE_FIREWORKS = "fireworks";
    public static final String SCENE_MODE_SPORTS = "sports";
    public static final String SCENE_MODE_PARTY = "party";
    public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
    public static final String SCENE_MODE_BARCODE = "barcode";
    public static final String FLASH_LIGHT_ON = "on";
    public static final String FLASH_LIGHT_OFF = "off";
    public static final String FLASH_LIGHT_AUTO = "auto";
    public static final String FLASH_LIGHT_TORCH = "torch";
    public static final String CAMERA_LENS_BACK = "back";
    public static final String CAMERA_LENS_FRONT = "front";
    public static final String CAMERA_PICTURESIZE_NORMAL = "normal";
    public static final String CAMERA_PICTURESIZE_HIGH = "high";
    public static final String CAMERA_PICTURESIZE_LOW = "low";
    private static final String CAMERA_SUPPORT_PREVIEW_PS_RANGE = "supportfpsrange";
    private static final String CAMERA_SUPPORT_MIN = "min";
    private static final String CAMERA_SUPPORT_MAX = "max";
    private static final HashMap<String, String> mSceneModes = new HashMap<>();
    private static final String SCENE_MODE_AUTO = "auto";
    private static final int MSG_FRAME_CALLBACK = 1;
    private static final int LOW_PREVIEW_SIZE_VALUE = 240;
    private static final int DEFAULT_PREVIEW_SIZE_VALUE = 480;
    private static final int HIGH_PREVIEW_SIZE_VALUE = 960;
    private static final int INVALID_CAMERA_ID = -1;
    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();
    private static String TAG = "CameraView";
    private static int currentOrientation = 0;
    // google init camera Params
    private static boolean mAutoFocus = true;

    static {
        mSceneModes.clear();
        mSceneModes.put(SCENE_MODE_AUTO, Camera.Parameters.SCENE_MODE_AUTO);
        mSceneModes.put(SCENE_MODE_ACTION, Camera.Parameters.SCENE_MODE_ACTION);
        mSceneModes.put(SCENE_MODE_PORTRAIT, Camera.Parameters.SCENE_MODE_PORTRAIT);
        mSceneModes.put(SCENE_MODE_LANDSCAPE, Camera.Parameters.SCENE_MODE_LANDSCAPE);
        mSceneModes.put(SCENE_MODE_NIGHT, Camera.Parameters.SCENE_MODE_NIGHT);
        mSceneModes.put(SCENE_MODE_NIGHT_PORTRAIT, Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT);
        mSceneModes.put(SCENE_MODE_THEATRE, Camera.Parameters.SCENE_MODE_THEATRE);
        mSceneModes.put(SCENE_MODE_BEACH, Camera.Parameters.SCENE_MODE_BEACH);
        mSceneModes.put(SCENE_MODE_SNOW, Camera.Parameters.SCENE_MODE_SNOW);
        mSceneModes.put(SCENE_MODE_SUNSET, Camera.Parameters.SCENE_MODE_SUNSET);
        mSceneModes.put(SCENE_MODE_STEADYPHOTO, Camera.Parameters.SCENE_MODE_STEADYPHOTO);
        mSceneModes.put(SCENE_MODE_FIREWORKS, Camera.Parameters.SCENE_MODE_FIREWORKS);
        mSceneModes.put(SCENE_MODE_SPORTS, Camera.Parameters.SCENE_MODE_SPORTS);
        mSceneModes.put(SCENE_MODE_PARTY, Camera.Parameters.SCENE_MODE_PARTY);
        mSceneModes.put(SCENE_MODE_CANDLELIGHT, Camera.Parameters.SCENE_MODE_CANDLELIGHT);
        mSceneModes.put(SCENE_MODE_BARCODE, Camera.Parameters.SCENE_MODE_BARCODE);
    }

    static {
        FLASH_MODES.put(Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);
    private final int mCurFrameOrientation = 0;
    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private final SizeMap mPreviewSizes = new SizeMap();
    private final SizeMap mPictureSizes = new SizeMap();
    public Camera mCamera;
    public boolean mIsHasPermission = false;
    public String mCameraMode = org.hapjs.widgets.Camera.CAMERA_VIDEORECORD_MODE;
    public CameraBaseMode mCameraBaseMode = null;
    OrientationEventListener mOrientationListener;
    AspectRatio mAspectRatio = null;
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private SurfaceView mSurfaceView;
    private boolean mShowingPreview;
    private Context mContext;
    private HybridManager mHybridManager = null;
    // camera cache file need delete
    private File mTmpCamerafile = null;
    private int curDisplayOri = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    private Display mDisplay;
    private OnPhotoTakeListener mOnPhotoTakeListener;
    private boolean mRequestLayoutOnOpen = false;
    //    private TakePictureSurfaceCallback mTakePictureSurfaceCallback;
    private OnCameraFrameListener mOnCameraFrameListener;
    private OnCameraInitDoneListener mOnCameraInitDoneListener;
    private NV21ToBitmap mNV21ToBitmap = null;
    private byte[] mPreviewBuffer;
    private Handler mProcessHandler = null;
    private int mStartCripX = 0;
    private int mStartCripY = 0;
    private boolean mIsInitFrameListener = false;
    private boolean mIsNeedResume = true;
    private OnCameraPermissionListener mOnCameraPermissionListener;
    private boolean mIsInit = false;
    private boolean mIsCameraDestroy;
    private int curTakePhotoOrientation = 0;
    public volatile boolean mIsCamervalid = true;
    /**
     * This is either Surface.Rotation_0, _90, _180, _270, or -1 (invalid).
     */
    private int mLastKnownRotation = -1;
    private int mDisplayOrientation;
    private Handler mBackgroundHandler;
    private int mWidth;
    private int mHeight;
    private Camera.Parameters mCameraParameters;
    private int mCameraId;
    private int mFacing;
    private boolean mIsNeedClose = false;
    private int mFlash;
    private String mPhotoQuality;
    private String mPreviewQuality;
    private boolean mAutoExposureLock = false;
    private boolean mAutoWhiteBalanceLock = false;
    private int mExposureValue = 0;
    private String mSceneMode = "";

    public CameraView(Context context) {
        super(context);
        mContext = context;
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            Log.e(TAG, "nv21ToBitmap error : " + e.getMessage());
        }
        return bitmap;
    }

    public void setOnCameraFrameListener(OnCameraFrameListener listener) {
        this.mOnCameraFrameListener = listener;
    }

    public void setOnCameraInitDoneListener(OnCameraInitDoneListener listener) {
        this.mOnCameraInitDoneListener = listener;
    }

    public void setOnCameraPermissionListener(OnCameraPermissionListener listener) {
        this.mOnCameraPermissionListener = listener;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            enableOrientationListener();
        } else {
            disableOrientationListener();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    public void enableOrientationListener() {
        if (null != mOrientationListener && mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }

    public void disableOrientationListener() {
        if (null != mOrientationListener && mOrientationListener.canDetectOrientation()) {
            mOrientationListener.disable();
        }
    }

    public void initCameraSetting() {
        mFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        mFlash = Constants.FLASH_AUTO;
        mPhotoQuality = CameraView.CAMERA_PICTURESIZE_NORMAL;
        mPreviewQuality = CameraView.CAMERA_PICTURESIZE_NORMAL;
        mCameraBaseMode = new VideoRecordMode(this, mComponent);

        mSurfaceView = mCameraBaseMode.getModeView(mContext, this);
        if (!mIsInit) {
            mCameraBaseMode.initCameraMode();
            initListener();
            mIsInit = true;
        }
        checkCameraPermission();
    }

    private void initListener() {
        initOrientation();
        ConfigurationManager.getInstance().addListener(this);
    }

    public void takePhoto(OnPhotoTakeListener onPhotoTakeListener) {
        this.mOnPhotoTakeListener = onPhotoTakeListener;
        if (mCamera != null) {
            curDisplayOri = ((Activity) mContext).getRequestedOrientation();
            curTakePhotoOrientation = currentOrientation;
            if (!isCameraOpened()) {
                if (null != mOnPhotoTakeListener) {
                    CameraData cameraData = new CameraData();
                    cameraData.setRetCode(CAMERA_TAKEPHOTO_ERROR);
                    cameraData.setMsg("Camera is not ready. Call start() before takePhoto().");
                    mOnPhotoTakeListener.onPhotoTakeCallback(cameraData);
                }
                return;
            }
            if (getAutoFocus()) {
                mCamera.cancelAutoFocus();
                mCamera.autoFocus(
                        new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                takePictureInternal();
                            }
                        });
            } else {
                takePictureInternal();
            }
        } else {
            if (null != mOnCameraPermissionListener) {
                mOnCameraPermissionListener.onCameraFailure("takePhoto error camera null.");
            }
        }
    }

    private boolean setVideoAutoFocus(boolean autoFocus) {
        if (isCameraOpened() && null != mCameraParameters) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(mCameraParameters);
            } else {
                Log.e(TAG, "setVideoAutoFocus no FOCUS_MODE_CONTINUOUS_VIDEO");
            }
            return true;
        } else {
            return false;
        }
    }

    public void startRecord(
            OnVideoRecordListener onVideoRecordListener, int maxDuration, boolean compressed) {
        setVideoAutoFocus(true);
        if (null != mCameraBaseMode
                && org.hapjs.widgets.Camera.CAMERA_VIDEORECORD_MODE.equals(mCameraMode)) {
            if (mCameraBaseMode instanceof VideoRecordMode) {
                ((VideoRecordMode) mCameraBaseMode)
                        .startRecording(onVideoRecordListener, maxDuration, compressed);
            }
        }
    }

    public void stopRecord(final OnVideoRecordListener onVideoRecordListener) {
        if (null != mCameraBaseMode
                && org.hapjs.widgets.Camera.CAMERA_VIDEORECORD_MODE.equals(mCameraMode)) {
            if (mCameraBaseMode instanceof VideoRecordMode) {
                ((VideoRecordMode) mCameraBaseMode).stopRecording(onVideoRecordListener);
            } else {
                Log.e(
                        TAG,
                        CameraBaseMode.VIDEO_RECORD_TAG
                                + "mCameraBaseMode is not instance VideoRecordMode.");
            }
        } else {
            Log.e(
                    TAG,
                    CameraBaseMode.VIDEO_RECORD_TAG
                            + " mCameraBaseMode is null or mCameraMode : "
                            + mCameraMode);
        }
    }

    public void setSceneMode(String sceneMode,
                             final OnCameraParamsListener onCameraParamsListener) {
        if (!TextUtils.isEmpty(sceneMode)) {
            String realSceneMode = mSceneModes.get(sceneMode);
            HashMap<String, Object> params = new HashMap<>();
            if (!TextUtils.isEmpty(realSceneMode) && isCameraOpened()) {
                Camera.Parameters parameters = mCamera.getParameters();
                if (null == parameters) {
                    params.put(CAMERA_MESSAGE, "setSceneMode Camera.Parameters is null");
                    onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                    return;
                }
                try {
                    parameters.setSceneMode(realSceneMode);
                    mCamera.setParameters(parameters);
                    mSceneMode = realSceneMode;
                    onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
                } catch (RuntimeException e) {
                    params.put(CAMERA_MESSAGE,
                            "Camera.Parameters is not support sceneMode : " + sceneMode);
                    onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                }
            } else {
                params.put(CAMERA_MESSAGE, "Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
            }
        }
    }

    public void setPreviewFpsRange(
            int min, int max, final OnCameraParamsListener onCameraParamsListener) {
        HashMap<String, Object> params = new HashMap<>();
        if (isCameraOpened()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters) {
                params.put(CAMERA_MESSAGE, "setPreviewFpsRange Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                return;
            }
            List<int[]> supportPreviewFps = parameters.getSupportedPreviewFpsRange();
            int realMin = -1;
            int realMax = -1;
            int[] supportSize = null;
            boolean isValid = false;
            for (int i = 0; i < supportPreviewFps.size(); i++) {
                supportSize = supportPreviewFps.get(i);
                if (null != supportSize && supportSize.length == 2) {
                    realMin = supportSize[0];
                    realMax = supportSize[1];
                }
                if (realMin != -1 && realMax != -1 && min >= realMin && max <= realMax
                        && min <= max) {
                    isValid = true;
                    break;
                }
            }
            if (isValid) {
                parameters.setPreviewFpsRange(min, max);
                mCamera.setParameters(parameters);
                onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
            } else {
                params.put(
                        CAMERA_MESSAGE,
                        "Camera.Parameters min : " + min + " max : " + max + " is not valid.");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
            }
        } else {
            params.put(CAMERA_MESSAGE, "Camera.Parameters is null");
            onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
        }
    }

    public void getPreviewFpsRange(final OnCameraParamsListener onCameraParamsListener) {
        HashMap<String, Object> params = new HashMap<>();
        if (isCameraOpened()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters) {
                params.put(CAMERA_MESSAGE, "getPreviewFpsRange Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                return;
            }
            int[] range = new int[2];
            parameters.getPreviewFpsRange(range);
            params.put(CAMERA_SUPPORT_MIN, range[0]);
            params.put(CAMERA_SUPPORT_MAX, range[1]);
            onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
        } else {
            params.put(CAMERA_MESSAGE, "Camera.Parameters is null");
            onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
        }
    }

    public void setExposureCompensation(
            int number, final OnCameraParamsListener onCameraParamsListener) {
        HashMap<String, Object> params = new HashMap<>();
        if (isCameraOpened()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters) {
                params.put(CAMERA_MESSAGE, "setExposureCompensation Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                return;
            }
            int min = parameters.getMinExposureCompensation();
            int max = parameters.getMaxExposureCompensation();
            if (min <= number && number <= max && min <= max) {
                mExposureValue = number;
                parameters.setExposureCompensation(number);
                mCamera.setParameters(parameters);
                onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
            } else {
                params.put(
                        CAMERA_MESSAGE,
                        "Camera.Parameters exposurecompensation : "
                                + number
                                + " is not in range of   "
                                + min
                                + " and  "
                                + max);
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
            }
        } else {
            params.put(CAMERA_MESSAGE, "Camera.Parameters is null");
            onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
        }
    }

    public void getExposureCompensation(final OnCameraParamsListener onCameraParamsListener) {
        HashMap<String, Object> params = new HashMap<>();
        if (isCameraOpened()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters) {
                params.put(CAMERA_MESSAGE, "getExposureCompensation Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                return;
            }
            params.put(
                    org.hapjs.widgets.Camera.CAMERA_EXPOSURE_COMPENSATION,
                    parameters.getExposureCompensation());
            onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
        } else {
            params.put(CAMERA_MESSAGE, "Camera.Parameters is null");
            onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
        }
    }

    public void getSupportedPreviewFpsRange(final OnCameraParamsListener onCameraParamsListener) {
        HashMap<String, Object> params = new HashMap<>();
        if (isCameraOpened()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters) {
                params.put(CAMERA_MESSAGE, "getSupportedPreviewFpsRange Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                return;
            }
            List<int[]> supportPreviewFps = parameters.getSupportedPreviewFpsRange();
            JSONObject jsonObject = null;
            JSONArray jsonArray = new JSONArray();
            int[] supportSize = null;
            for (int i = 0; i < supportPreviewFps.size(); i++) {
                supportSize = supportPreviewFps.get(i);
                if (null != supportSize && supportSize.length == 2) {
                    jsonObject = new JSONObject();
                    try {
                        jsonObject.put(CAMERA_SUPPORT_MIN, supportSize[0]);

                        jsonObject.put(CAMERA_SUPPORT_MAX, supportSize[1]);
                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        Log.e(TAG, "get supported preview fps range error", e);
                    }
                }
            }
            params.put(CAMERA_SUPPORT_PREVIEW_PS_RANGE, jsonArray);
            onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
        } else {
            params.put(CAMERA_MESSAGE, "getSupportedPreviewFpsRange Camera.Parameters is null");
            onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
        }
    }

    public void getExposureCompensationRange(final OnCameraParamsListener onCameraParamsListener) {
        HashMap<String, Object> params = new HashMap<>();
        if (isCameraOpened()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (null == parameters) {
                params.put(CAMERA_MESSAGE,
                        "getExposureCompensationRange Camera.Parameters is null");
                onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
                return;
            }
            int min = parameters.getMinExposureCompensation();
            int max = parameters.getMaxExposureCompensation();
            params.put(CAMERA_SUPPORT_MIN, min);
            params.put(CAMERA_SUPPORT_MAX, max);
            onCameraParamsListener.onCameraParamsCallback(CAMERA_OK, params);
        } else {
            params.put(CAMERA_MESSAGE, "getExposureCompensationRange Camera.Parameters is null");
            onCameraParamsListener.onCameraParamsCallback(CAMERA_ERROR, params);
        }
    }

    void takePictureInternal() {
        if (!isPictureCaptureInProgress.getAndSet(true)) {
            mCamera.takePicture(null, null, new TakePictureCallback());
        }
    }

    boolean getAutoFocus() {
        if (!isCameraOpened()) {
            return mAutoFocus;
        }
        String focusMode = mCameraParameters.getFocusMode();
        return focusMode != null && focusMode.contains("continuous");
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    public AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    // 处理图片，旋转、裁剪
    private void resolveBitmapData(final byte[] data) {
        if (null == data || data.length == 0) {
            return;
        }
        BufferedOutputStream bos = null;
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            if (mFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                matrix.postRotate(((curTakePhotoOrientation + 90) % 360)); // 后置
            } else {
                matrix.postScale(-1, 1); // 相当于加了逆向180
                matrix.postRotate(((curTakePhotoOrientation + 90) % 360)); // 前置
            }
            Log.d(
                    TAG,
                    "picture rotate degree : "
                            + (((curTakePhotoOrientation + 90) % 360))
                            + " curTakePhotoOrientation : "
                            + curTakePhotoOrientation);
            if (null != mComponent) {
                RenderEventCallback callback = mComponent.getCallback();
                if (null != callback) {
                    mTmpCamerafile = callback.createFileOnCache("camera", ".jpg");
                    bos = new BufferedOutputStream(new FileOutputStream(mTmpCamerafile));
                    Bitmap destBitMap = cropBitmap(bm, matrix);
                    if (null != destBitMap) {
                        destBitMap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    } else {
                        Log.e(TAG, "resolveBitmapData error destBitMap null.");
                    }
                }
            }
            CameraView.this.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (null != mOnPhotoTakeListener) {
                                CameraData cameraData = new CameraData();
                                if (null != mTmpCamerafile) {
                                    cameraData.setUrl(Uri.fromFile(mTmpCamerafile));
                                    cameraData.setRetCode(CAMERA_OK);
                                    cameraData.setMsg(CAMERA_OK_MESSAGE);
                                } else {
                                    cameraData.setRetCode(CAMERA_ERROR);
                                    cameraData.setMsg(CAMERA_ERROR_MESSAGE);
                                }
                                mOnPhotoTakeListener.onPhotoTakeCallback(cameraData);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "onPictureTaken Exception : " + e.getMessage());
            if (null != mOnPhotoTakeListener) {
                CameraData cameraData = new CameraData();
                cameraData.setRetCode(CAMERA_ERROR);
                cameraData.setMsg(CAMERA_ERROR_MESSAGE + " : " + e.getMessage());
                mOnPhotoTakeListener.onPhotoTakeCallback(cameraData);
            }
        } finally {
            try {
                if (bos != null) {
                    bos.flush(); // 输出
                    FileUtils.closeQuietly(bos); // 关闭
                }
                if (bm != null) {
                    bm.recycle(); // 回收bitmap空间
                }
            } catch (IOException e) {
                Log.e(TAG, "onPictureTaken bos IOException : " + e.getMessage());
            }
        }
    }

    private Bitmap cropBitmap(Bitmap bm, Matrix matrix) {
        // 宽高自定义根据控件宽高 1、width  height   2、比例按照控件 宽高    3、当前的picturesize width height，之后按照比例进行裁剪
        int parentWidth = getWidth();
        int parentHeight = getHeight();
        int bitmapWidth = bm.getWidth();
        int bitmapHeight = bm.getHeight();
        // parentHeight parentWidth  控件宽高
        // mWidth mHeight 预览成像宽高，大于控件宽高，有一个和控件相同
        // bitmapWidth bitmapHeight 形成的图片宽高,按照picture的设置，且是按照横屏情况 4:3类似的设置的,对于竖屏时候，要使用bitmap的宽作为高
        int realStartCripX = 0;
        int realStartCripY = 0;
        // mStartCripX是 width ，mStartCripY是height，对于横竖屏对应的是不一样的，而对于bm的宽 高，横竖屏都是一样的横屏的方向
        if (parentHeight != 0 && parentWidth != 0 && mWidth != 0 && mHeight != 0) {
            if (parentWidth == mWidth) {
                if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    // view宽相等
                    realStartCripX = 0;
                    realStartCripY = (int) (mStartCripY * (float) bm.getWidth() / mHeight);
                    bitmapWidth = (int) (parentHeight * (float) bm.getWidth() / mHeight);
                    bitmapHeight = bm.getHeight();
                } else {
                    // view宽相等
                    realStartCripX = 0;
                    realStartCripY = (int) (mStartCripY * (float) bm.getHeight() / mHeight);
                    bitmapHeight = (int) (parentHeight * (float) bm.getHeight() / mHeight);
                    bitmapWidth = bm.getWidth();
                }
            } else {
                // x / 总的  为  对应的  a小的实际组件 / b
                if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    // view宽不相等
                    realStartCripX = (int) (mStartCripX * (float) bm.getHeight() / mWidth);
                    realStartCripY = 0;
                    bitmapHeight = (int) (parentWidth * (float) bm.getHeight() / mWidth);
                    bitmapWidth = bm.getWidth();
                } else {
                    // view宽不相等
                    realStartCripX = (int) (mStartCripX * (float) bm.getWidth() / mWidth);
                    realStartCripY = 0;
                    bitmapWidth = (int) (parentWidth * (float) bm.getWidth() / mWidth);
                    bitmapHeight = bm.getHeight();
                }
            }
        }
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            if (bm.getWidth() == bitmapWidth) {
                int anotherPix = 0;
                int tmpValue = 0;
                if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    tmpValue = realStartCripX;
                } else {
                    tmpValue = realStartCripY;
                }
                if (bitmapHeight % 2 != 0) {
                    if (tmpValue + bitmapHeight + 1 <= bm.getHeight()) {
                        anotherPix = 1;
                    } else {
                        anotherPix = -1;
                    }
                }
                // 横屏
                if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    bm =
                            Bitmap.createBitmap(
                                    bm,
                                    realStartCripY,
                                    bm.getHeight() - bitmapHeight - realStartCripX - anotherPix,
                                    bitmapWidth,
                                    bitmapHeight + anotherPix,
                                    matrix,
                                    true); // 变换之后宽高方向发生变化
                } else {
                    bm =
                            Bitmap.createBitmap(
                                    bm,
                                    realStartCripX,
                                    bm.getHeight() - bitmapHeight - realStartCripY - anotherPix,
                                    bitmapWidth,
                                    bitmapHeight + anotherPix,
                                    matrix,
                                    true); // 变换之后宽高方向发生变化
                }
            } else {
                int anotherPix = 0;
                int tmpValue = 0;
                if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    tmpValue = realStartCripY;
                } else {
                    tmpValue = realStartCripX;
                }
                if (bitmapWidth % 2 != 0) {
                    if (tmpValue + bitmapWidth + 1 <= bm.getWidth()) {
                        anotherPix = 1;
                    } else {
                        anotherPix = -1;
                    }
                }
                // 竖屏
                if (mFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    // 正面拍照 0，0点 相反原因
                    if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        bm =
                                Bitmap.createBitmap(
                                        bm,
                                        bm.getWidth() - bitmapWidth - realStartCripY - anotherPix,
                                        realStartCripX,
                                        bitmapWidth + anotherPix,
                                        bitmapHeight,
                                        matrix,
                                        true);
                    } else {
                        bm =
                                Bitmap.createBitmap(
                                        bm,
                                        bm.getWidth() - bitmapWidth - realStartCripX - anotherPix,
                                        realStartCripY,
                                        bitmapWidth + anotherPix,
                                        bitmapHeight,
                                        matrix,
                                        true);
                    }
                } else {
                    if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        bm =
                                Bitmap.createBitmap(
                                        bm,
                                        realStartCripY - anotherPix,
                                        realStartCripX,
                                        bitmapWidth + anotherPix,
                                        bitmapHeight,
                                        matrix,
                                        true);
                    } else {
                        bm =
                                Bitmap.createBitmap(
                                        bm,
                                        realStartCripX - anotherPix,
                                        realStartCripY,
                                        bitmapWidth + anotherPix,
                                        bitmapHeight,
                                        matrix,
                                        true);
                    }
                }
            }
        } else {
            Log.e(TAG,
                    "cropBitmap bitmapWidth : " + bitmapWidth + " bitmapHeight : " + bitmapHeight);
        }
        return bm;
    }

    // 处理图片，旋转、裁剪
    private Bitmap resolveFrameData(final byte[] data, int width, int height) {
        if (null == data || data.length == 0) {
            return null;
        }
        Bitmap bm = null;
        Bitmap destBitmap = null;
        try {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (null == mNV21ToBitmap) {
                    mNV21ToBitmap = new NV21ToBitmap(getContext());
                }
                bm = mNV21ToBitmap.nv21ToBitmap(data, width, height);
            } else {
                bm = nv21ToBitmap(data, width, height);
            }
            Matrix matrix = new Matrix();
            if (mFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                matrix.postRotate(((mCurFrameOrientation + 90) % 360)); // 后置
            } else {
                matrix.postScale(-1, 1); // 相当于加了逆向180
                matrix.postRotate(((mCurFrameOrientation + 90) % 360)); // 前置
            }
            if (null != bm) {
                destBitmap = cropBitmap(bm, matrix);
            } else {
                Log.e(TAG, "resolveFrameData bm null.");
            }
        } catch (Exception e) {
            Log.e(TAG, "resolveFrameData Exception : " + e.getMessage());
        }
        return destBitmap;
    }

    private void initOrientation() {
        if (mOrientationListener != null) {
            return;
        }
        mOrientationListener =
                new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                            return;
                        }
                        // 可以根据不同角度检测处理，这里只检测四个角度的改变
                        if (orientation > 350 || orientation < 10) { // 0度
                            orientation = 0;
                        } else if (orientation > 80 && orientation < 100) { // 90度
                            orientation = 90;
                        } else if (orientation > 170 && orientation < 190) { // 180度
                            orientation = 180;
                        } else if (orientation > 260 && orientation < 280) { // 270度
                            orientation = 270;
                        } else {
                            return;
                        }
                        if (currentOrientation != orientation) {
                            currentOrientation = orientation;
                        }
                        if (null != mDisplay) {
                            final int rotation = mDisplay.getRotation();
                            if (mLastKnownRotation != rotation && isCameraOpened()) {
                                mLastKnownRotation = rotation;
                                if (mDisplayOrientation != getDisplayOrientation(false)) {
                                    refreshDisplayOrientation();
                                }
                            }
                        }
                    }
                };
    }

    // for dynamic set orientation
    void refreshDisplayOrientation() {
        if (isCameraOpened()) {
            final boolean needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
            if (needsToStopPreview) {
                stopPreview();
            }
            setCameraDisplayOrientation();
            if (needsToStopPreview) {
                startPreview();
            }
        }
    }

    private void startCameraPreview() {
        try {
            if (null != mCamera && mCameraId != INVALID_CAMERA_ID) {
                mCamera.startPreview();
            } else {
                Log.e(TAG, " mCamera : " + mCamera + " mCameraId : " + mCameraId);
            }
        } catch (RuntimeException e) {
            Log.e(
                    TAG,
                    "startCameraPreview  mCamera : "
                            + mCamera
                            + " mCameraId : "
                            + mCameraId
                            + " error : "
                            + e.getMessage());
            if (null != mOnCameraPermissionListener) {
                mOnCameraPermissionListener.onCameraFailure("error startCameraPreview.");
            }
        }
    }

    public void startCamera() {
        if (mIsHasPermission && null != mDisplay) {
            start();
        }
    }

    public void stopCamera() {
        stop();
    }

    public void onActivityPause() {
        mIsCamervalid = false;
        if (null != mCameraBaseMode && mIsHasPermission && !mIsNeedResume) {
            Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onActivityPause valid.");
            mIsNeedResume = true;
            mCameraBaseMode.onActivityPause();
        } else {
            if (mSurfaceView instanceof GLSurfaceView) {
                Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onActivityPause else valid.");
                ((GLSurfaceView) mSurfaceView).onPause();
            }
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG + "onActivityPause mCameraBaseMode is null or no permission or mIsNeedResume true"
                    + " mIsHasPermission : " + mIsHasPermission
                    + " mIsNeedResume : " + mIsNeedResume);
        }
    }

    public void onCameraDestroy() {
        if (null != mCameraBaseMode) {
            mCameraBaseMode.onCameraDestroy();
        }
        mIsCameraDestroy = true;
    }

    public void onActivityResume() {
        if (null != mCameraBaseMode && mIsHasPermission) {
            mIsNeedResume = false;
            mIsCamervalid = true;
            mCameraBaseMode.onActivityResume();
        }
    }

    private void checkCameraPermission() {
        HybridView hybridView = getComponent().getHybridView();
        if (hybridView == null) {
            Log.e(TAG, "error: hybrid view is null.");
            return;
        }
        mHybridManager = hybridView.getHybridManager();
        HapPermissionManager.getDefault()
                .requestPermissions(
                        mHybridManager,
                        new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                        new PermissionCallback() {
                            @Override
                            public void onPermissionAccept() {
                                CameraView.this.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                mIsHasPermission = true;
                                                start();
                                            }
                                        });
                            }

                            @Override
                            public void onPermissionReject(int reason) {
                                CameraView.this.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                mIsHasPermission = false;
                                                if (null != mOnCameraPermissionListener) {
                                                    mOnCameraPermissionListener.onCameraFailure(
                                                            "camera permission deny.");
                                                }
                                            }
                                        });
                            }
                        });
    }

    private void start() {
        chooseCamera();
        openCamera();
        if (isReady()) {
            if (null != mCameraBaseMode) {
                mCameraBaseMode.setUpPreview(mShowingPreview);
            }
        }
        mShowingPreview = true;
        if (null != mCameraBaseMode) {
            mCameraBaseMode.notifyShowingPreview(mShowingPreview);
        }
        if (isCameraOpened()) {
            startPreview();
        }
    }

    public void startPreview() {
        startCameraPreview();
    }

    private void initFrameListener(boolean isForceInit, boolean isNeedStopStart) {
        if (mCameraParameters == null || mCamera == null || null == mOnCameraFrameListener) {
            Log.e(
                    TAG,
                    "initFrameListener mCameraParameters : "
                            + mCameraParameters
                            + " mCamera : "
                            + mCamera
                            + " mOnCameraFrameListener : "
                            + mOnCameraFrameListener);
            return;
        }
        if (mIsInitFrameListener && !isForceInit) {
            return;
        }
        mIsInitFrameListener = true;
        if (mProcessHandler != null) {
            mProcessHandler.getLooper().quit();
            mProcessHandler = null;
        }
        if (null != mNV21ToBitmap) {
            mNV21ToBitmap = null;
        }
        HandlerThread processThread = new HandlerThread("preview-buffer-handlerthread");
        processThread.start();
        if (isNeedStopStart) {
            mCamera.stopPreview();
            Size previewSize = initPreviewSize();
            if (null != previewSize) {
                mCameraParameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            }
            mCamera.setParameters(mCameraParameters);
        }
        final int previewWidth = mCameraParameters.getPreviewSize().width;
        final int previewHeight = mCameraParameters.getPreviewSize().height;
        mProcessHandler =
                new Handler(processThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        switch (msg.what) {
                            case MSG_FRAME_CALLBACK: {
                                Object tmpObj = msg.obj;
                                long time = -1;
                                if (tmpObj instanceof Bundle) {
                                    Bundle bundle = (Bundle) tmpObj;
                                    if (bundle.containsKey("time")) {
                                        time = bundle.getLong("time");
                                    }
                                    if (time != -1) {
                                        preResolveFrame(time, previewWidth, previewHeight);
                                    }
                                }
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    }
                };
        mPreviewBuffer =
                new byte[previewWidth * previewHeight
                        * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
        mCamera.addCallbackBuffer(mPreviewBuffer);
        mCamera.setPreviewCallbackWithBuffer(
                new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if (null != mProcessHandler) {
                            long currentTime = System.currentTimeMillis();
                            Bundle mBundle = new Bundle();
                            mBundle.putLong("time", currentTime);
                            Message msg =
                                    mProcessHandler.obtainMessage(MSG_FRAME_CALLBACK, mBundle);
                            mProcessHandler.sendMessage(msg);
                        }
                    }
                });
        if (isNeedStopStart) {
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (null != mCamera) {
            mCamera.stopPreview();
        }
        if (mCameraParameters == null
                || null == mOnCameraFrameListener
                || null == mProcessHandler
                || null == mCamera) {
            Log.e(
                    TAG,
                    "stopPreview mCameraParameters : "
                            + mCameraParameters
                            + " mOnCameraFrameListener : "
                            + mOnCameraFrameListener
                            + " mProcessHandler : "
                            + mProcessHandler
                            + " mCamera : "
                            + mCamera);
            return;
        }
        final int previewWidth = mCameraParameters.getPreviewSize().width;
        final int previewHeight = mCameraParameters.getPreviewSize().height;
        mPreviewBuffer =
                new byte[previewWidth * previewHeight
                        * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
        mCamera.addCallbackBuffer(mPreviewBuffer);
        mCamera.setPreviewCallbackWithBuffer(
                new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if (null != mProcessHandler) {
                            long currentTime = System.currentTimeMillis();
                            Bundle mBundle = new Bundle();
                            mBundle.putLong("time", currentTime);
                            Message msg =
                                    mProcessHandler.obtainMessage(MSG_FRAME_CALLBACK, mBundle);
                            mProcessHandler.sendMessage(msg);
                        }
                    }
                });
    }

    private void preResolveFrame(long time, int previewWidth, int previewHeight) {
        if (mPreviewBuffer != null && mOnCameraFrameListener != null) {
            Bitmap bitmap = null;
            try {
                bitmap = resolveFrameData(mPreviewBuffer, previewWidth, previewHeight);
                byte[] byteArray = null;
                ByteBuffer buf = null;
                if (null != bitmap) {
                    int bytes = bitmap.getByteCount();
                    if (bytes > 0) {
                        buf = ByteBuffer.allocate(bytes);
                        bitmap.copyPixelsToBuffer(buf);
                        byteArray = buf.array();
                    }

                } else {
                    Log.e(TAG, " preResolveFrame bitmap null ");
                }
                if (null == byteArray) {
                    Log.e(TAG, " preResolveFrame byteArray null ");
                    return;
                }
                if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    mOnCameraFrameListener.onCameraFrameListener(
                            bitmap.getWidth(), bitmap.getHeight(), byteArray, time);
                } else {
                    mOnCameraFrameListener.onCameraFrameListener(
                            bitmap.getWidth(), bitmap.getHeight(), byteArray, time);
                }
            } catch (Exception e) {
                Log.e(TAG, " preResolveFrame exception  : " + e.getMessage());
            } finally {
                if (null != bitmap) {
                    bitmap.recycle();
                    bitmap = null;
                }
                if (mCamera != null) {
                    mCamera.addCallbackBuffer(mPreviewBuffer);
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (null != mComponent) {
            Page page = mComponent.getPage();
            if (null != page) {
                curDisplayOri = page.getOrientation();
            } else {
                Log.w(TAG, "onAttachedToWindow getOrientation page is null.");
            }
        } else {
            Log.w(TAG, "onAttachedToWindow getOrientation mComponent is null.");
        }
        mDisplay = ViewCompat.getDisplay(this);
        reConfirmListenerEnvent();
        enableOrientationListener();
        if (!mShowingPreview) {
            startCamera();
        }
    }

    private void reConfirmListenerEnvent() {
        if (mIsInit) {
            Log.w(TAG, "reConfirmListenerEnvent mIsInit : " + mIsInit);
            return;
        }
        if (null != mCameraBaseMode) {
            mCameraBaseMode.onBackAttachCameraMode();
        }
        initListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, CameraBaseMode.VIDEO_RECORD_TAG + " onDetachedFromWindow");
        onActivityPause();
        // stopRecord(null);
        disableOrientationListener();
        ConfigurationManager.getInstance().removeListener(this);
        mIsInit = false;
        mIsNeedClose = true;
        setFlashLightMode(mFlash);
        mDisplay = null;
        stopCamera();
        mPreviewBuffer = null;
        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
        if (null != mCameraBaseMode) {
            mCameraBaseMode.onViewDetachFromWindow();
        }
        if (mIsCameraDestroy) {
            mCameraBaseMode = null;
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }
        try {
            mCamera = Camera.open(mCameraId);
        } catch (RuntimeException e) {
            if (null != mOnCameraPermissionListener) {
                mOnCameraPermissionListener.onCameraFailure("open camera fail.");
            }
            return;
        }
        if (mCamera == null) {
            Log.e(TAG, "openCamera: mCamera is null");
            if (mOnCameraPermissionListener != null) {
                mOnCameraPermissionListener.onCameraFailure("camera is no available.");
            }
            return;
        }
        mCameraParameters = mCamera.getParameters();
        if (mCameraParameters == null) {
            if (mOnCameraPermissionListener != null) {
                mOnCameraPermissionListener.onCameraFailure("get camera parameters fail.");
            }
            return;
        }
        if (org.hapjs.widgets.Camera.CAMERA_VIDEORECORD_MODE.equals(mCameraMode)) {
            // Give the camera a hint that we're recording video.  This can have a big
            // impact on frame rate.
            mCameraParameters.setRecordingHint(true);
            ((VideoRecordMode) mCameraBaseMode).createDetachedSurfaceTexture();
        }
        // Supported preview sizes
        mPreviewSizes.clear();
        if (mCameraParameters != null) {
            List<Camera.Size> previewSizes = mCameraParameters.getSupportedPreviewSizes();
            if (previewSizes != null && previewSizes.size() > 0) {
                for (Camera.Size size : previewSizes) {
                    if (size == null) {
                        continue;
                    }
                    mPreviewSizes.add(new Size(size.width, size.height));
                }
            }
        }
        // Supported picture sizes;
        mPictureSizes.clear();
        if (mCameraParameters != null) {
            List<Camera.Size> pictureSizes = mCameraParameters.getSupportedPictureSizes();
            if (pictureSizes != null && pictureSizes.size() > 0) {
                for (Camera.Size size : pictureSizes) {
                    if (size == null) {
                        continue;
                    }
                    mPictureSizes.add(new Size(size.width, size.height));
                }
            }
        }

        mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
        initAspectRatio();
        adjustCameraParameters();
        setCameraDisplayOrientation();
        if (mRequestLayoutOnOpen) {
            mRequestLayoutOnOpen = false;
            requestLayout();
        }
        initFrameListener(false, true);
        if (mIsNeedResume) {
            onActivityResume();
        }
        if (null != mOnCameraInitDoneListener) {
            mOnCameraInitDoneListener.onCameraInitDone(null);
        }
    }

    private void initAspectRatio() {
        Set<AspectRatio> supportedRatios = getSupportedAspectRatios();
        AspectRatio tmpAspectRatio = null;
        AspectRatio chooseAspectRatio = null;
        boolean hasDefaultRatio = false;
        if (null != supportedRatios) {
            Iterator<AspectRatio> iterator = supportedRatios.iterator();
            while (iterator.hasNext()) {
                tmpAspectRatio = iterator.next();
                if (chooseAspectRatio == null) {
                    chooseAspectRatio = tmpAspectRatio;
                }
                if (tmpAspectRatio.getX() == mAspectRatio.getX()
                        && tmpAspectRatio.getY() == mAspectRatio.getY()) {
                    hasDefaultRatio = true;
                    break;
                }
            }
        }
        if (!hasDefaultRatio && chooseAspectRatio != null) {
            mAspectRatio = chooseAspectRatio;
        }
    }

    Set<AspectRatio> getSupportedAspectRatios() {
        SizeMap idealAspectRatios = mPreviewSizes;
        if (null != idealAspectRatios) {
            for (AspectRatio aspectRatio : idealAspectRatios.ratios()) {
                if (mPictureSizes.sizes(aspectRatio) == null) {
                    idealAspectRatios.remove(aspectRatio);
                }
            }
            return idealAspectRatios.ratios();
        } else {
            return null;
        }
    }

    public void setCameraDisplayOrientation() {
        int orientation = getDisplayOrientation(true);
        if (isCameraOpened() && orientation != -1) {
            mDisplayOrientation = orientation;
            mCamera.setDisplayOrientation(orientation);
        }
    }

    private int getDisplayOrientation(boolean needLog) {
        int result = -1;
        if (isCameraOpened() && mDisplay != null) {
            // info.orientation  横竖屏相关
            // 相机 横屏 0  竖屏 90  逆向横屏 180  逆向竖屏 270
            Camera.CameraInfo info = new Camera.CameraInfo();
            try {
                Camera.getCameraInfo(mCameraId, info);
            } catch (RuntimeException e) {
                Log.e(TAG, "getDisplayOrientation RuntimeException error : " + e.getMessage());
                return result;
            }
            // 竖屏 0  横屏 90  逆向竖屏 180  逆向横屏 270
            int rotation = mDisplay.getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
                default:
                    break;
            }

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360; // compensate the mirror
            } else { // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            if (needLog) {
                Log.d(
                        TAG,
                        "  rotation  : "
                                + rotation
                                +
                                "  portrait 0  landscape 1  reverse portrait 2  reverse landscape 3 \n"
                                + "  curDisplayOri : "
                                + curDisplayOri
                                + "  -1 unknown 0 landscape 1 portrait \n"
                                + " info.facing"
                                + info.facing
                                + " 0  back  1 front \n"
                                + "    result : "
                                + result);
            }
        }
        return result;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mIsInitFrameListener = false;
        }
    }

    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraId = i;
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
    }

    void adjustCameraParameters() {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
        if (sizes == null) { // Not supported
            mAspectRatio = chooseAspectRatio();
        }
        Size size = initPreviewSize();
        // Always re-apply camera parameters
        // Largest picture size in this ratio
        // final SortedSet<Size> sortedSet= mPictureSizes.sizes(mAspectRatio);
        Size pictureSize = initPictureSize();
        // final Size pictureSize = mPictureSizes.sizes(mAspectRatio).last();
        if (mShowingPreview && isCameraOpened()) {
            stopPreview();
        }
        if (null != size) {
            mCameraParameters.setPreviewSize(size.getWidth(), size.getHeight());
        }
        if (null != pictureSize) {
            mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }
        setAutoFocusInternal(mAutoFocus);
        setFlashInternal(mFlash);
        try {
            if (isCameraOpened()) {
                mCamera.setParameters(mCameraParameters);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "parameter is invalid or not supported message : " + e.getMessage());
        }
        if (mShowingPreview && isCameraOpened()) {
            startPreview();
        }
    }

    private Size initPictureSize() {
        SortedSet<Size> sizeSortedSet = mPictureSizes.sizes(mAspectRatio);
        int allSize = sizeSortedSet.size();
        Iterator sizeIterator = sizeSortedSet.iterator();
        // 根据接近比例取 低中高
        Size pictureSize = mPictureSizes.sizes(mAspectRatio).last();
        if (!TextUtils.isEmpty(mPhotoQuality)) {
            if (CameraView.CAMERA_PICTURESIZE_NORMAL.equals(mPhotoQuality)) {
                int i = 0;
                while (sizeIterator.hasNext()) {
                    i++;
                    pictureSize = (Size) sizeIterator.next();
                    if (i == (allSize % 2 == 0 ? (allSize / 2) : (allSize + 1) / 2)) {
                        break;
                    }
                }
            } else if (CameraView.CAMERA_PICTURESIZE_LOW.equals(mPhotoQuality)) {
                pictureSize = mPictureSizes.sizes(mAspectRatio).first();
            }
        }
        return pictureSize;
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashInternal(int flash) {
        if (isCameraOpened()) {
            List<String> modes = mCameraParameters.getSupportedFlashModes();
            String mode = FLASH_MODES.get(flash);
            if (modes != null && modes.contains(mode)) {
                if (mIsNeedClose) {
                    mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mIsNeedClose = false;
                    return true;
                }
                if (mode != null) {
                    mCameraParameters.setFlashMode(mode);
                }
                mFlash = flash;
                return true;
            }
            return false;
        } else {
            mFlash = flash;
            return false;
        }
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constants.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    private Size initPreviewSize() {
        SortedSet<Size> sizeSortedSet = mPreviewSizes.sizes(mAspectRatio);
        if (null == sizeSortedSet) {
            Log.e(TAG, "error initPreviewSize previewSizes null.");
            return null;
        }
        Iterator sizeIterator = sizeSortedSet.iterator();
        Size previewSize = sizeSortedSet.first();

        Size tmpPreviewSize = null;
        while (sizeIterator.hasNext()) {
            tmpPreviewSize = (Size) sizeIterator.next();
            if (Math.min(tmpPreviewSize.getWidth(), tmpPreviewSize.getHeight())
                    >= DEFAULT_PREVIEW_SIZE_VALUE) {
                break;
            }
        }
        if (null != tmpPreviewSize) {
            previewSize = tmpPreviewSize;
        }
        if (TextUtils.isEmpty(mPreviewQuality)
                || CameraView.CAMERA_PICTURESIZE_NORMAL.equals(mPreviewQuality)) {
            return previewSize;
        }
        if (CameraView.CAMERA_PICTURESIZE_HIGH.equals(mPreviewQuality)) {
            Iterator highSizeIterator = sizeSortedSet.iterator();
            while (highSizeIterator.hasNext()) {
                previewSize = (Size) highSizeIterator.next();
                if (Math.min(previewSize.getWidth(), previewSize.getHeight())
                        >= HIGH_PREVIEW_SIZE_VALUE) {
                    break;
                }
            }
        } else if (CameraView.CAMERA_PICTURESIZE_LOW.equals(mPreviewQuality)) {
            Iterator lowsizeIterator = sizeSortedSet.iterator();
            while (lowsizeIterator.hasNext()) {
                previewSize = (Size) lowsizeIterator.next();
                if (Math.min(previewSize.getWidth(), previewSize.getHeight())
                        >= LOW_PREVIEW_SIZE_VALUE) {
                    break;
                }
            }
        }
        return previewSize;
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    boolean isReady() {
        return mWidth != 0 && mHeight != 0;
    }

    boolean isCameraOpened() {
        return mCamera != null;
    }

    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
        if (isCameraOpened()) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        } else {
            return false;
        }
    }

    public void setLensMode(int lensMode) {
        mFacing = lensMode;
        if (mIsHasPermission) {
            stop();
            mIsNeedClose = true;
            start();
            setFlashLightMode(mFlash);
        }
    }

    private void stop() {
        if (mProcessHandler != null) {
            mProcessHandler.getLooper().quit();
            mProcessHandler = null;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mShowingPreview = false;
        if (null != mCameraBaseMode) {
            mCameraBaseMode.notifyShowingPreview(mShowingPreview);
        }
        releaseCamera();
    }

    public void setFlashLightMode(int flashLightMode) {
        mFlash = flashLightMode;
        boolean isStop = false;
        if (isCameraOpened() && mIsHasPermission) {
            if (mShowingPreview) {
                stopPreview();
                isStop = true;
            }
            setFlashInternal(mFlash);
            mCamera.setParameters(mCameraParameters);
            if (isStop) {
                startPreview();
            }
        }
    }

    public void setPhotoQuality(String photoQuality) {
        mPhotoQuality = photoQuality;
        if (mIsHasPermission && isCameraOpened()) {
            Size pictureSize = initPictureSize();
            if (null != pictureSize) {
                mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
            }
            mCamera.setParameters(mCameraParameters);
        }
    }

    public void setPreviewQuality(String previewQuality) {
        mPreviewQuality = previewQuality;
        boolean isStop = false;
        if (mIsHasPermission && isCameraOpened()) {
            if (mShowingPreview) {
                mCamera.stopPreview();
                isStop = true;
            }
            Size previewSize = initPreviewSize();
            if (null != previewSize) {
                mCameraParameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            }
            mCamera.setParameters(mCameraParameters);
            if (isStop) {
                initFrameListener(true, false);
                startPreview();
            }
        }
    }

    public void setAutoExposureLock(boolean autoExposureLock) {
        mAutoExposureLock = autoExposureLock;
        if (mIsHasPermission && isCameraOpened()) {
            if (mCameraParameters.isAutoExposureLockSupported()) {
                mCameraParameters.setAutoExposureLock(autoExposureLock);
                mCamera.setParameters(mCameraParameters);
            } else {
                if (null != mOnCameraPermissionListener) {
                    mOnCameraPermissionListener.onCameraFailure(
                            "error camera is not support autoExposureLock.");
                }
            }
        }
    }

    public void setAutoWhiteBalanceLock(boolean autoWhiteBalanceLock) {
        mAutoWhiteBalanceLock = autoWhiteBalanceLock;
        if (mIsHasPermission && isCameraOpened()) {
            if (mCameraParameters.isAutoWhiteBalanceLockSupported()) {
                mCameraParameters.setAutoWhiteBalanceLock(autoWhiteBalanceLock);
                mCamera.setParameters(mCameraParameters);
            } else {
                if (null != mOnCameraPermissionListener) {
                    mOnCameraPermissionListener
                            .onCameraFailure("error camera not support autoWhiteBalance.");
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!isCameraOpened()) {
            mRequestLayoutOnOpen = true;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        } else {
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY
                    && heightMode != MeasureSpec.EXACTLY
                    && (null != mComponent && mComponent.isWidthDefined())) {
                final AspectRatio ratio = getAspectRatio();
                if (null != ratio) {
                    int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                    if (heightMode == MeasureSpec.AT_MOST) {
                        height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                    }
                    super.onMeasure(
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                } else {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }

            } else if (widthMode != MeasureSpec.EXACTLY
                    && heightMode == MeasureSpec.EXACTLY
                    && (null != mComponent && mComponent.isHeightDefined())) {
                final AspectRatio ratio = getAspectRatio();
                if (null != ratio) {
                    int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                    if (widthMode == MeasureSpec.AT_MOST) {
                        width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                    }
                    super.onMeasure(
                            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            heightMeasureSpec);
                } else {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }

            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (null != ratio && ratio.getX() != 0 && ratio.getY() != 0) {
            if (curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    || curDisplayOri == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                ratio = ratio.inverse();
            }
            assert ratio != null;
            if (height < width * ratio.getY() / ratio.getX()) {
                // 对齐宽度情况
                mSurfaceView.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                                MeasureSpec.EXACTLY));
            } else {
                // 对齐高度情况
                mSurfaceView.measure(
                        MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                                MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = CameraView.this.getWidth();
        int height = CameraView.this.getHeight();
        boolean isWidthSame = (width == mSurfaceView.getMeasuredWidth());
        if (!isWidthSame) {
            int[] cameraviewLocation = new int[2];
            CameraView.this.getLocationInWindow(cameraviewLocation);
            int[] surfaceviewLocation = new int[2];
            mSurfaceView.getLocationInWindow(surfaceviewLocation);
            int leftMargin =
                    (cameraviewLocation[0] + width / 2)
                            - (surfaceviewLocation[0] + mSurfaceView.getMeasuredWidth() / 2);
            if (leftMargin != 0) {
                Log.e(
                        TAG,
                        "cameraview_onLayout_error leftMargin  : "
                                + leftMargin
                                + " cameraviewLocation[0]  : "
                                + cameraviewLocation[0]
                                + " cameraviewLocation[1]  : "
                                + cameraviewLocation[1]
                                + " surfaceviewLocation[0] : "
                                + surfaceviewLocation[0]
                                + " surfaceviewLocation[1] : "
                                + surfaceviewLocation[1]
                                + " width : "
                                + width
                                + " height : "
                                + height
                                + "  mSurfaceView.getMeasuredWidth() : "
                                + mSurfaceView.getMeasuredWidth()
                                + "  mSurfaceView.getMeasuredHeight() : "
                                + mSurfaceView.getMeasuredHeight());
            }
            mStartCripX = Math.abs(width / 2 - mSurfaceView.getMeasuredWidth() / 2);
            mStartCripY = 0;
        } else {
            int[] cameraviewLocation = new int[2];
            CameraView.this.getLocationInWindow(cameraviewLocation);
            int[] surfaceviewLocation = new int[2];
            mSurfaceView.getLocationInWindow(surfaceviewLocation);
            int topMargin =
                    (cameraviewLocation[1] + height / 2)
                            - (surfaceviewLocation[1] + mSurfaceView.getMeasuredHeight() / 2);
            if (topMargin != 0) {
                Log.e(
                        TAG,
                        "cameraview_onLayout_error topMargin  : "
                                + topMargin
                                + " cameraviewLocation[0]  : "
                                + cameraviewLocation[0]
                                + " cameraviewLocation[1]  : "
                                + cameraviewLocation[1]
                                + " surfaceviewLocation[0] : "
                                + surfaceviewLocation[0]
                                + " surfaceviewLocation[1] : "
                                + surfaceviewLocation[1]
                                + " width : "
                                + width
                                + " height : "
                                + height
                                + "  mSurfaceView.getMeasuredWidth() : "
                                + mSurfaceView.getMeasuredWidth()
                                + "  mSurfaceView.getMeasuredHeight() : "
                                + mSurfaceView.getMeasuredHeight());
            }
            mStartCripX = 0;
            mStartCripY = Math.abs(height / 2 - mSurfaceView.getMeasuredHeight() / 2);
        }
    }

    public interface OnPhotoTakeListener {
        /**
         * Called when pickPhoto click
         */
        void onPhotoTakeCallback(CameraData data);
    }

    public interface OnVideoRecordListener {
        /**
         * Called when video click
         */
        void onVideoRecordCallback(CameraData data);
    }

    public interface OnCameraPermissionListener {
        /**
         * Called when pickPhoto click
         */
        void onCameraFailure(String message);
    }

    public interface OnCameraParamsListener {
        /**
         * Called after set Camera Params
         */
        void onCameraParamsCallback(int retCode, HashMap<String, Object> params);
    }

    public interface OnCameraFrameListener {
        /**
         * Called when frame
         */
        void onCameraFrameListener(int width, int height, byte[] bytes, long time);
    }

    public interface OnCameraInitDoneListener {
        /**
         * Called when camera init done
         */
        void onCameraInitDone(HashMap<String, Object> datas);
    }

    @Override
    public void onConfigurationChanged(HapConfiguration newConfig) {
        if (mIsHasPermission && mDisplayOrientation != getDisplayOrientation(false)) {
            refreshDisplayOrientation();
        }
    }

    private final class TakePictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(final byte[] data, final Camera camera) {
            isPictureCaptureInProgress.set(false);
            getBackgroundHandler()
                    .post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    resolveBitmapData(data);
                                }
                            });
            if (isCameraOpened()) {
                mCamera.cancelAutoFocus();
                stop();
                mIsNeedClose = true;
                start();
                setFlashLightMode(mFlash);
                setAutoExposureLock(mAutoExposureLock);
                setAutoWhiteBalanceLock(mAutoWhiteBalanceLock);
                if (null != mCameraParameters) {
                    try {
                        mCameraParameters.setExposureCompensation(mExposureValue);
                    } catch (RuntimeException e) {
                        Log.e(TAG,
                                "onPictureTaken setExposureCompensation error : " + e.getMessage());
                    }
                }
                if (null != mCameraParameters && !TextUtils.isEmpty(mSceneMode)) {
                    try {
                        mCameraParameters.setSceneMode(mSceneMode);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "onPictureTaken setSceneMode error : " + e.getMessage());
                    }
                }
            }
        }
    }

    //    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public class NV21ToBitmap {
        private RenderScript rs;
        private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
        private Type.Builder yuvType;
        private Type.Builder rgbaType;
        private Allocation in;
        private Allocation out;

        public NV21ToBitmap(Context context) {
            rs = RenderScript.create(context);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        }

        public Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(nv21);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            out.copyTo(bmpout);
            return bmpout;
        }
    }
}
