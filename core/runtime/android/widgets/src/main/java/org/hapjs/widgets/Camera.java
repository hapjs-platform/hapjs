/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.camera.CameraBaseMode;
import org.hapjs.widgets.view.camera.CameraData;
import org.hapjs.widgets.view.camera.CameraView;
import org.hapjs.widgets.view.camera.VideoRecordMode;
import org.hapjs.widgets.view.camera.googlecameraview.Constants;
import org.json.JSONException;
import org.json.JSONObject;

@WidgetAnnotation(
        name = Camera.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS,
                Camera.METHOD_TAKE_PHOTO,
                Camera.METHOD_SET_SCENE_MODE,
                Camera.METHOD_SET_FPS_RANGE,
                Camera.METHOD_SET_EXPOSURE_VALUE,
                Camera.METHOD_GET_SUPPORT_FPS_RANGE,
                Camera.METHOD_GET_EXPOSURE_RANGE,
                Camera.METHOD_GET_EXPOSURE_VALUE,
                Camera.METHOD_GET_FPS_RANGE,
                Camera.METHOD_START_RECORD,
                Camera.METHOD_STOP_RECORD
        })
public class Camera extends Component<CameraView> {

    public static final String CAMERA_VIDEORECORD_MODE = "record";
    public static final String CAMERA_PREVIEW_FPS_RANGE = "previewfpsrange";
    public static final String CAMERA_EXPOSURE_COMPENSATION = "exposurecompensation";
    protected static final String WIDGET_NAME = "camera";
    protected static final String CAMERA_AUTO_EXPOSURE = "autoexposurelock";
    protected static final String CAMERA_AUTO_WHITEBALANCE = "autowhitebalancelock";
    // method
    protected static final String METHOD_TAKE_PHOTO = "takePhoto";
    protected static final String METHOD_START_RECORD = "startRecord";
    protected static final String METHOD_STOP_RECORD = "stopRecord";
    /**
     * 'SCENE_MODE_AUTO' auto ,//自动模式 'SCENE_MODE_ACTION' action ,//动作模式 'SCENE_MODE_PORTRAIT'
     * portrait ,//竖屏模式 'SCENE_MODE_LANDSCAPE’landscape ,//横屏模式 'SCENE_MODE_NIGHT' night ,//夜晚模式
     * 'SCENE_MODE_NIGHT_PORTRAIT' night-portrait,//晚间运动模式 'SCENE_MODE_THEATRE' theatre ,//剧院模式
     * 'SCENE_MODE_BEACH' beach,//海滩模式 'SCENE_MODE_SNOW' snow,//雪景模式 'SCENE_MODE_SUNSET' sunset,//日落模式
     * 'SCENE_MODE_STEADYPHOTO' steadyphoto,//稳定模式 'SCENE_MODE_FIREWORKS' fireworks,//烟花模式
     * 'SCENE_MODE_SPORTS' sports,//运动模式 'SCENE_MODE_PARTY' party,//派对模式 'SCENE_MODE_CANDLELIGHT'
     * candlelight,//烛光模式 'SCENE_MODE_BARCODE' barcode//条码模式
     */
    protected static final String METHOD_SET_SCENE_MODE = "setSceneMode";
    protected static final String METHOD_SET_FPS_RANGE = "setPreviewFpsRange";
    protected static final String METHOD_SET_EXPOSURE_VALUE = "setExposureCompensation";
    protected static final String METHOD_GET_SUPPORT_FPS_RANGE = "getSupportedPreviewFpsRange";
    protected static final String METHOD_GET_EXPOSURE_RANGE = "getExposureCompensationRange";
    protected static final String METHOD_GET_EXPOSURE_VALUE = "getExposureCompensation";
    protected static final String METHOD_GET_FPS_RANGE = "getPreviewFpsRange";
    // camera
    private static final String CAMERA_LENS_MODE = "deviceposition";
    private static final String CAMERA_FLASHLIGHT_MODE = "flash";
    private static final String CAMERA_FRAMESIZE_MODE = "framesize";
    private static final String CAMERA_DATA_CODE = "code";
    private static final String CAMERA_DATA_URL = "uri";
    private static final String CAMERA_DATA_THUMB_PATH = "thumbPath";
    private static final String CAMERA_DATA_MSG = "message";
    // camera event
    private static final String CAMERA_ERROR = "error";
    private static final String CAMERA_FRAME = "cameraframe";
    private static final String CAMERA_INIT_DONE = "camerainitdone";
    private static final String CAMERA_PICTURE_QUALITY = "quality";
    private static final String CAMERA_VIDEO_MAXDURATION = "maxduration";
    private static final String CAMERA_VIDEO_COMPRESSED = "compressed";
    private static final String CAMERA_SCENE_MODE = "scenemode";
    private static final String CAMERA_PREVIEW_FPS_MIN = "min";
    private static final String CAMERA_PREVIEW_FPS_MAX = "max";
    private static String TAG = "camera";
    private int mExposureCompensation = 0;
    private String mScenemode = "";
    private int mPreviewFpsMin = -1;
    private int mPreviewFpsMax = -1;
    private String mLensMode = "";
    private String mFlashMode = "";
    private String mPhotoQuality = "";
    private String mPreviewQuality = "";
    private boolean mAutoWhiteBalanceLock = false;
    private boolean mAutoExposureLock = false;

    public Camera(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        callback.addActivityStateListener(this);
    }

    @Override
    protected CameraView createViewImpl() {
        CameraView cameraView = new CameraView(mContext);
        cameraView.setComponent(this);
        cameraView.initCameraSetting();
        if (GrayModeManager.getInstance().shouldApplyGrayMode()) {
            DocComponent docComponent = getRootComponent();
            DecorLayout decorLayout = docComponent.getDecorLayout();
            decorLayout.applyGrayMode(false);
        }
        return cameraView;
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (CAMERA_ERROR.equals(event)) {
            mHost.setOnCameraPermissionListener(
                    new CameraView.OnCameraPermissionListener() {
                        @Override
                        public void onCameraFailure(String message) {
                            Log.e(TAG, "onCameraFailure  message: " + message);
                            mCallback
                                    .onJsEventCallback(getPageId(), mRef, CAMERA_ERROR, Camera.this,
                                            null, null);
                        }
                    });
            return true;
        }
        if (CAMERA_FRAME.equals(event)) {
            mHost.setOnCameraFrameListener(
                    new CameraView.OnCameraFrameListener() {
                        @Override
                        public void onCameraFrameListener(int width, int height, byte[] bytes,
                                                          long time) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("width", width);
                            params.put("height", height);
                            params.put("time", time);
                            ArrayBuffer frameBuffer = new ArrayBuffer(bytes);
                            params.put("frame", frameBuffer);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, CAMERA_FRAME, Camera.this, params, null);
                        }
                    });
            return true;
        }
        if (CAMERA_INIT_DONE.equals(event)) {
            mHost.setOnCameraInitDoneListener(
                    new CameraView.OnCameraInitDoneListener() {
                        @Override
                        public void onCameraInitDone(HashMap<String, Object> datas) {
                            Map<String, Object> params = new HashMap<>();
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, CAMERA_INIT_DONE, Camera.this, params, null);
                        }
                    });
            return true;
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (CAMERA_ERROR.equals(event)) {
            mHost.setOnCameraPermissionListener(null);
            return true;
        }
        if (CAMERA_FRAME.equals(event)) {
            mHost.setOnCameraFrameListener(null);
            return true;
        }
        if (CAMERA_INIT_DONE.equals(event)) {
            mHost.setOnCameraInitDoneListener(null);
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (METHOD_TAKE_PHOTO.equals(methodName)) {
            takePhoto(args);
        } else if (METHOD_SET_SCENE_MODE.equals(methodName)) {
            setSceneMode(args);
        } else if (METHOD_SET_FPS_RANGE.equals(methodName)) {
            setPreviewFpsRange(args);
        } else if (METHOD_SET_EXPOSURE_VALUE.equals(methodName)) {
            setExposureCompensation(args);
        } else if (METHOD_GET_SUPPORT_FPS_RANGE.equals(methodName)) {
            getSupportedPreviewFpsRange(args);
        } else if (METHOD_GET_EXPOSURE_RANGE.equals(methodName)) {
            getExposureCompensationRange(args);
        } else if (METHOD_GET_EXPOSURE_VALUE.equals(methodName)) {
            getExposureCompensation(args);
        } else if (METHOD_GET_FPS_RANGE.equals(methodName)) {
            getPreviewFpsRange(args);
        } else if (METHOD_START_RECORD.equals(methodName)) {
            startRecord(args);
        } else if (METHOD_STOP_RECORD.equals(methodName)) {
            stopRecord(args);
        } else if (METHOD_GET_BOUNDING_CLIENT_RECT.equals(methodName)) {
            super.invokeMethod(methodName, args);
        }
    }

    public void startRecord(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }

        if (args.get(CAMERA_PICTURE_QUALITY) != null) {
            String quality = (String) args.get(CAMERA_PICTURE_QUALITY);
            if (CameraView.CAMERA_PICTURESIZE_HIGH.equals(quality)
                    || CameraView.CAMERA_PICTURESIZE_NORMAL.equals(quality)
                    || CameraView.CAMERA_PICTURESIZE_LOW.equals(quality)) {
                mPreviewQuality = quality;
                mHost.setPreviewQuality(quality);
            }
        }
        int maxDuration = VideoRecordMode.VIDEO_MAX_DURATION;
        if (args.get(CAMERA_VIDEO_MAXDURATION) != null) {
            maxDuration = (int) args.get(CAMERA_VIDEO_MAXDURATION);
        }
        boolean compressed = false;
        if (args.get(CAMERA_VIDEO_COMPRESSED) != null) {
            compressed = (boolean) args.get(CAMERA_VIDEO_COMPRESSED);
        }
        mHost.startRecord(
                new CameraView.OnVideoRecordListener() {
                    @Override
                    public void onVideoRecordCallback(CameraData data) {
                        if (null != data) {
                            String callbackId = null;
                            boolean isTimeCallback = false;
                            if (data.getRetCode() == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (data.getRetCode() == CameraView.CAMERA_ERROR
                                    || data.getRetCode() == CameraView.CAMERA_TAKEPHOTO_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else if (data.getRetCode() == CameraView.CAMERA_TIMEOUT) {
                                callbackId = (String) args.get("timeoutCallback");
                                isTimeCallback = true;
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (!isTimeCallback && null != callbackId) {
                                Map<String, Object> params = new HashMap<>();
                                params.put(CAMERA_DATA_CODE, data.getRetCode());
                                params.put(CAMERA_DATA_MSG, data.getMsg());
                                Map<String, Object> paramsCode = new HashMap<>();
                                paramsCode.put(CAMERA_DATA_CODE, data.getRetCode());

                                mCallback.onJsMethodCallback(getPageId(), callbackId, params,
                                        paramsCode);
                            }
                            if (isTimeCallback && null != callbackId) {
                                Map<String, Object> params = new HashMap<>();
                                params.put(CAMERA_DATA_CODE, data.getRetCode());
                                DocComponent rootComponent = getRootComponent();
                                ApplicationContext applicationContext = null;
                                String internalUriStr = "";
                                String thumbnailUriStr = "";
                                if (rootComponent != null) {
                                    View topView = rootComponent.getHostView();
                                    if (topView instanceof RootView) {
                                        applicationContext = ((RootView) topView).getAppContext();
                                    }
                                }
                                if (null != applicationContext) {
                                    internalUriStr =
                                            applicationContext.getInternalUri(data.getUrl());
                                    if (null != data.getThumbnail()) {
                                        thumbnailUriStr = applicationContext
                                                .getInternalUri(data.getThumbnail());
                                    }
                                }
                                params.put(CAMERA_DATA_URL, internalUriStr);
                                params.put(CAMERA_DATA_THUMB_PATH, thumbnailUriStr);
                                params.put(CAMERA_DATA_MSG, data.getMsg());
                                Map<String, Object> paramsCode = new HashMap<>();
                                paramsCode.put(CAMERA_DATA_CODE, data.getRetCode());
                                mCallback.onJsMethodCallback(getPageId(), callbackId, params,
                                        paramsCode);
                            }
                            if (null == callbackId) {
                                Log.w(
                                        TAG,
                                        CameraBaseMode.VIDEO_RECORD_TAG
                                                + "  startRecord error isTimeCallback : "
                                                + isTimeCallback
                                                + " callbackId is null.");
                            }
                        }
                        callbackComplete(args);
                    }
                },
                maxDuration,
                compressed);
    }

    public void stopRecord(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.e(TAG, CameraBaseMode.VIDEO_RECORD_TAG
                    + "  stopRecord error mHost null or args null.");
            return;
        }
        mHost.stopRecord(
                new CameraView.OnVideoRecordListener() {
                    @Override
                    public void onVideoRecordCallback(CameraData data) {
                        if (null != data) {
                            String callbackId = null;
                            if (data.getRetCode() == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (data.getRetCode() == CameraView.CAMERA_ERROR
                                    || data.getRetCode() == CameraView.CAMERA_TAKEPHOTO_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                Map<String, Object> params = new HashMap<>();
                                params.put(CAMERA_DATA_CODE, data.getRetCode());
                                DocComponent rootComponent = getRootComponent();
                                ApplicationContext applicationContext = null;
                                String internalUriStr = "";
                                String thumbnailUriStr = "";
                                if (rootComponent != null) {
                                    View topView = rootComponent.getHostView();
                                    if (topView instanceof RootView) {
                                        applicationContext = ((RootView) topView).getAppContext();
                                    }
                                }
                                if (null != applicationContext) {
                                    internalUriStr =
                                            applicationContext.getInternalUri(data.getUrl());
                                    if (null != data.getThumbnail()) {
                                        thumbnailUriStr = applicationContext
                                                .getInternalUri(data.getThumbnail());
                                    }
                                }
                                params.put(CAMERA_DATA_URL, internalUriStr);
                                params.put(CAMERA_DATA_THUMB_PATH, thumbnailUriStr);
                                params.put(CAMERA_DATA_MSG, data.getMsg());
                                Map<String, Object> paramsCode = new HashMap<>();
                                paramsCode.put(CAMERA_DATA_CODE, data.getRetCode());
                                mCallback.onJsMethodCallback(getPageId(), callbackId, params,
                                        paramsCode);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void setSceneMode(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        String sceneMode = "";
        if (args.get(CAMERA_SCENE_MODE) != null) {
            sceneMode = (String) args.get(CAMERA_SCENE_MODE);
        }
        if (TextUtils.isEmpty(sceneMode)) {
            return;
        }
        final String realSceneMode = sceneMode;
        mHost.setSceneMode(
                realSceneMode,
                new CameraView.OnCameraParamsListener() {

                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                mScenemode = realSceneMode;
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void setPreviewFpsRange(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        int min = -1;
        int max = -1;
        if (args.get(CAMERA_PREVIEW_FPS_RANGE) != null) {
            Object obj = args.get(CAMERA_PREVIEW_FPS_RANGE);
            if (obj instanceof JSONObject) {
                try {
                    min = ((JSONObject) obj).getInt(CAMERA_PREVIEW_FPS_MIN);
                    max = ((JSONObject) obj).getInt(CAMERA_PREVIEW_FPS_MAX);
                } catch (JSONException e) {
                    Log.e(TAG, "setPreviewFpsRange error : " + e.getMessage());
                }
            }
        }
        if (min == -1 || max == -1) {
            return;
        }
        final int realMin = min;
        final int realMax = max;
        mHost.setPreviewFpsRange(
                realMin,
                realMax,
                new CameraView.OnCameraParamsListener() {

                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                mPreviewFpsMin = realMin;
                                mPreviewFpsMax = realMax;
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void setExposureCompensation(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        int exposureCompensation = 0;
        if (args.get(CAMERA_EXPOSURE_COMPENSATION) != null) {
            exposureCompensation = (int) args.get(CAMERA_EXPOSURE_COMPENSATION);
        }
        final int realExposureCompensation = exposureCompensation;
        mHost.setExposureCompensation(
                realExposureCompensation,
                new CameraView.OnCameraParamsListener() {
                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                mExposureCompensation = realExposureCompensation;
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void getSupportedPreviewFpsRange(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        mHost.getSupportedPreviewFpsRange(
                new CameraView.OnCameraParamsListener() {

                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void getExposureCompensationRange(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        mHost.getExposureCompensationRange(
                new CameraView.OnCameraParamsListener() {

                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void getExposureCompensation(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        mHost.getExposureCompensation(
                new CameraView.OnCameraParamsListener() {

                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void getPreviewFpsRange(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        mHost.getPreviewFpsRange(
                new CameraView.OnCameraParamsListener() {

                    @Override
                    public void onCameraParamsCallback(int retCode,
                                                       HashMap<String, Object> retParams) {
                        if (null != retParams) {
                            String callbackId = null;
                            if (retCode == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (retCode == CameraView.CAMERA_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                mCallback.onJsMethodCallback(getPageId(), callbackId, retParams);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    public void takePhoto(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }
        if (args.get(CAMERA_PICTURE_QUALITY) != null) {
            String quality = (String) args.get(CAMERA_PICTURE_QUALITY);
            setPhotoQuality(quality);
        }
        mHost.takePhoto(
                new CameraView.OnPhotoTakeListener() {
                    @Override
                    public void onPhotoTakeCallback(CameraData data) {
                        if (null != data) {
                            String callbackId = null;
                            if (data.getRetCode() == CameraView.CAMERA_OK) {
                                callbackId = (String) args.get("success");
                            } else if (data.getRetCode() == CameraView.CAMERA_ERROR
                                    || data.getRetCode() == CameraView.CAMERA_TAKEPHOTO_ERROR) {
                                callbackId = (String) args.get("fail");
                            } else {
                                callbackId = (String) args.get("fail");
                            }
                            if (null != callbackId) {
                                Map<String, Object> params = new HashMap<>();
                                params.put(CAMERA_DATA_CODE, data.getRetCode());
                                DocComponent rootComponent = getRootComponent();
                                ApplicationContext applicationContext = null;
                                String internalUriStr = "";
                                if (rootComponent != null) {
                                    View topView = rootComponent.getHostView();
                                    if (topView instanceof RootView) {
                                        applicationContext = ((RootView) topView).getAppContext();
                                    }
                                }
                                if (null != applicationContext) {
                                    internalUriStr =
                                            applicationContext.getInternalUri(data.getUrl());
                                }
                                params.put(CAMERA_DATA_URL, internalUriStr);
                                params.put(CAMERA_DATA_MSG, data.getMsg());
                                Map<String, Object> paramsCode = new HashMap<>();
                                paramsCode.put(CAMERA_DATA_CODE, data.getRetCode());
                                mCallback.onJsMethodCallback(getPageId(), callbackId, params,
                                        paramsCode);
                            }
                        }
                        callbackComplete(args);
                    }
                });
    }

    private void callbackComplete(Map<String, Object> args) {
        if (args != null && args.containsKey("complete")) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get("complete"));
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case CAMERA_LENS_MODE:
                String lensMode = Attributes.getString(attribute, CameraView.CAMERA_LENS_BACK);
                setLensMode(lensMode);
                return true;
            case CAMERA_FLASHLIGHT_MODE:
                String flashLightMode =
                        Attributes.getString(attribute, CameraView.FLASH_LIGHT_AUTO);
                setFlashLightMode(flashLightMode);
                return true;
            case Attributes.Style.SHOW:
                mShow = parseShowAttribute(attribute);
                mShowAttrInitialized = true;
                show(mShow);
                if (null != mHost) {
                    if (mShow) {
                        mHost.startCamera();
                    } else {
                        mHost.stopCamera();
                    }
                }
                return true;
            case CAMERA_FRAMESIZE_MODE:
                String frameSize =
                        Attributes.getString(attribute, CameraView.CAMERA_PICTURESIZE_NORMAL);
                setPreviewQuality(frameSize);
                return true;
            case CAMERA_AUTO_EXPOSURE:
                boolean autoExposure = Attributes.getBoolean(attribute, false);
                setAutoExposureLock(autoExposure);
                return true;
            case CAMERA_AUTO_WHITEBALANCE:
                boolean whiteBalance = Attributes.getBoolean(attribute, false);
                setAutoWhiteBalanceLock(whiteBalance);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public void setLensMode(String lensMode) {
        if (mHost == null) {
            return;
        }
        mLensMode = lensMode;
        mHost.setLensMode(getLensMode(mLensMode));
    }

    public void setFlashLightMode(String flashLightMode) {
        if (mHost == null) {
            return;
        }
        mFlashMode = flashLightMode;
        mHost.setFlashLightMode(getFlashLightMode(flashLightMode));
    }

    public void setPreviewQuality(String previewQuality) {
        if (mHost == null || TextUtils.isEmpty(previewQuality)) {
            return;
        }
        if (CameraView.CAMERA_PICTURESIZE_HIGH.equals(previewQuality)
                || CameraView.CAMERA_PICTURESIZE_NORMAL.equals(previewQuality)
                || CameraView.CAMERA_PICTURESIZE_LOW.equals(previewQuality)) {
            mPreviewQuality = previewQuality;
            mHost.setPreviewQuality(previewQuality);
        }
    }

    public void setAutoExposureLock(boolean autoExposureLock) {
        if (mHost == null) {
            return;
        }
        mAutoExposureLock = autoExposureLock;
        mHost.setAutoExposureLock(mAutoExposureLock);
    }

    public void setAutoWhiteBalanceLock(boolean autoWhiteBalanceLock) {
        if (mHost == null) {
            return;
        }
        mAutoWhiteBalanceLock = autoWhiteBalanceLock;
        mHost.setAutoWhiteBalanceLock(mAutoWhiteBalanceLock);
    }

    public void setPhotoQuality(String photoQuality) {
        if (mHost == null || TextUtils.isEmpty(photoQuality)) {
            return;
        }
        if (CameraView.CAMERA_PICTURESIZE_HIGH.equals(photoQuality)
                || CameraView.CAMERA_PICTURESIZE_NORMAL.equals(photoQuality)
                || CameraView.CAMERA_PICTURESIZE_LOW.equals(photoQuality)) {
            mPhotoQuality = photoQuality;
            mHost.setPhotoQuality(photoQuality);
        }
    }

    private int getLensMode(String lensMode) {
        int mode = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
        if (TextUtils.isEmpty(lensMode)) {
            return mode;
        }
        if (CameraView.CAMERA_LENS_BACK.equals(lensMode)) {
            mode = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
        } else if (CameraView.CAMERA_LENS_FRONT.equals(lensMode)) {
            mode = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        return mode;
    }

    private int getFlashLightMode(String flashLightMode) {
        int mode = Constants.FLASH_AUTO;
        if (TextUtils.isEmpty(flashLightMode)) {
            return mode;
        }
        if (CameraView.FLASH_LIGHT_ON.equals(flashLightMode)) {
            mode = Constants.FLASH_ON;
        } else if (CameraView.FLASH_LIGHT_OFF.equals(flashLightMode)) {
            mode = Constants.FLASH_OFF;
        } else if (CameraView.FLASH_LIGHT_AUTO.equals(flashLightMode)) {
            mode = Constants.FLASH_AUTO;
        } else if (CameraView.FLASH_LIGHT_TORCH.equals(flashLightMode)) {
            mode = Constants.FLASH_TORCH;
        }
        return mode;
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mHost != null && mHost.isAttachedToWindow() && mHost.mIsHasPermission) {
            mHost.enableOrientationListener();
            mHost.startCamera();
            mHost.onActivityResume();
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        if (mHost != null && mHost.isAttachedToWindow() && mHost.mIsHasPermission) {
            mHost.stopCamera();
            mHost.disableOrientationListener();
            mHost.onActivityPause();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mCallback.removeActivityStateListener(this);
        if (mHost != null) {
            mHost.stopCamera();
            mHost.onCameraDestroy();
        }
    }
}
