/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Sensor.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(
                        name = Sensor.ACTION_SUBSCRIBE_ACCELEROMETER,
                        mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(
                        name = Sensor.ACTION_UNSUBSCRIBE_ACCELEROMETER,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = Sensor.ACTION_SUBSCRIBE_COMPASS,
                        mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(
                        name = Sensor.ACTION_UNSUBSCRIBE_COMPASS,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = Sensor.ACTION_SUBSCRIBE_PROXIMITY,
                        mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(
                        name = Sensor.ACTION_UNSUBSCRIBE_PROXIMITY,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = Sensor.ACTION_SUBSCRIBE_LIGHT,
                        mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(name = Sensor.ACTION_UNSUBSCRIBE_LIGHT, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = Sensor.ACTION_SUBSCRIBE_STEP_COUNTER,
                        mode = FeatureExtension.Mode.CALLBACK),
                @ActionAnnotation(
                        name = Sensor.ACTION_UNSUBSCRIBE_STEP_COUNTER,
                        mode = FeatureExtension.Mode.SYNC)
        })
public class Sensor extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "system.sensor";
    protected static final String ACTION_SUBSCRIBE_ACCELEROMETER = "subscribeAccelerometer";
    protected static final String ACTION_UNSUBSCRIBE_ACCELEROMETER = "unsubscribeAccelerometer";
    protected static final String ACTION_SUBSCRIBE_COMPASS = "subscribeCompass";
    protected static final String ACTION_UNSUBSCRIBE_COMPASS = "unsubscribeCompass";
    protected static final String ACTION_SUBSCRIBE_PROXIMITY = "subscribeProximity";
    protected static final String ACTION_UNSUBSCRIBE_PROXIMITY = "unsubscribeProximity";
    protected static final String ACTION_SUBSCRIBE_LIGHT = "subscribeLight";
    protected static final String ACTION_UNSUBSCRIBE_LIGHT = "unsubscribeLight";
    protected static final String ACTION_SUBSCRIBE_STEP_COUNTER = "subscribeStepCounter";
    protected static final String ACTION_UNSUBSCRIBE_STEP_COUNTER = "unsubscribeStepCounter";
    protected static final String PARAM_INTERVAL = "interval";
    protected static final String PARAM_INTERVAL_GAME = "game";
    protected static final String PARAM_INTERVAL_UI = "ui";
    protected static final String PARAM_INTERVAL_NORMAL = "normal";
    protected static final String PARAM_X = "x";
    protected static final String PARAM_Y = "y";
    protected static final String PARAM_Z = "z";
    protected static final String PARAM_DIRECTION = "direction";
    protected static final String PARAM_DISTANCE = "distance";
    protected static final String PARAM_INTENSITY = "intensity";
    protected static final String PARAM_STEPS = "steps";
    protected static final String PARAM_ACCURACY = "accuracy";
    private static final String TAG = "Sensor";
    private static final int MESSAGE_COMPASS_INTERVAL = 1;
    private static final int MESSAGE_ON_PAUSE = 2;
    private static final int MESSAGE_ON_RESUME = 3;
    private static final int SET_PAUSE_DELAY = 5000; // base on JsThread blocked delay time 5s
    private static final int COMPASS_EVENT_INTERVAL = 200;
    // microseconds
    private static final int EVENT_INTERVAL_GAME = 20 * 1000;
    private static final int EVENT_INTERVAL_UI = 60 * 1000;
    private static final int EVENT_INTERVAL_NORMAL = 200 * 1000;

    private static final Map<String, Integer> INTERVAL_MAP = new HashMap<>();

    static {
        INTERVAL_MAP.put(PARAM_INTERVAL_GAME, EVENT_INTERVAL_GAME);
        INTERVAL_MAP.put(PARAM_INTERVAL_UI, EVENT_INTERVAL_UI);
        INTERVAL_MAP.put(PARAM_INTERVAL_NORMAL, EVENT_INTERVAL_NORMAL);
    }

    private Handler mHandler;
    private NativeInterface mNativeInterface;
    private volatile boolean isPause = false;

    public Sensor() {
        mHandler =
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void dispatchMessage(Message msg) {
                        switch (msg.what) {
                            case MESSAGE_COMPASS_INTERVAL:
                                runCallbackContext(ACTION_SUBSCRIBE_COMPASS, 0, null);
                                break;
                            case MESSAGE_ON_PAUSE:
                                isPause = true;
                                break;
                            case MESSAGE_ON_RESUME:
                                isPause = false;
                                break;
                            default:
                                // do nothing
                        }
                    }
                };
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        addLifecycleListener(request);
        if (ACTION_SUBSCRIBE_ACCELEROMETER.equals(action)) {
            return subscribeAccelerometer(request);
        } else if (ACTION_UNSUBSCRIBE_ACCELEROMETER.equals(action)) {
            return unsubscribeAccelerometer(request);
        } else if (ACTION_SUBSCRIBE_COMPASS.equals(action)) {
            return subscribeCompass(request);
        } else if (ACTION_UNSUBSCRIBE_COMPASS.equals(action)) {
            return unsubscribeCompass(request);
        } else if (ACTION_SUBSCRIBE_PROXIMITY.equals(action)) {
            return subscribeProximity(request);
        } else if (ACTION_UNSUBSCRIBE_PROXIMITY.equals(action)) {
            return unsubscribeProximity(request);
        } else if (ACTION_SUBSCRIBE_LIGHT.equals(action)) {
            return subscribeLight(request);
        } else if (ACTION_UNSUBSCRIBE_LIGHT.equals(action)) {
            return unsubscribeLight(request);
        } else if (ACTION_SUBSCRIBE_STEP_COUNTER.equals(action)) {
            return subscribeStepCounter(request);
        } else if (ACTION_UNSUBSCRIBE_STEP_COUNTER.equals(action)) {
            return unsubscribeStepCounter(request);
        } else {
            Log.w(TAG, "undefined action:" + action);
            return Response.NO_ACTION;
        }
    }

    private void addLifecycleListener(Request request) {
        if (mNativeInterface == null) {
            mNativeInterface = request.getNativeInterface();
            mNativeInterface.addLifecycleListener(
                    new LifecycleListener() {
                        @Override
                        public void onResume() {
                            mHandler.removeMessages(MESSAGE_ON_PAUSE);
                            mHandler.sendEmptyMessage(MESSAGE_ON_RESUME);
                        }

                        @Override
                        public void onPause() {
                            mHandler.removeMessages(MESSAGE_ON_RESUME);
                            mHandler.removeMessages(MESSAGE_ON_PAUSE);
                            mHandler.sendEmptyMessageDelayed(MESSAGE_ON_PAUSE, SET_PAUSE_DELAY);
                        }

                        @Override
                        public void onDestroy() {
                            if (mNativeInterface != null) {
                                mNativeInterface.removeLifecycleListener(this);
                                mNativeInterface = null;
                            }
                        }
                    });
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Response subscribeAccelerometer(Request request) {
        AccelerometerCallbackContext accelerometerCallbackContext =
                new AccelerometerCallbackContext(request, isReserved(request));
        putCallbackContext(accelerometerCallbackContext);
        return Response.SUCCESS;
    }

    private Response unsubscribeAccelerometer(Request request) {
        removeCallbackContext(ACTION_SUBSCRIBE_ACCELEROMETER);
        return Response.SUCCESS;
    }

    private Response subscribeCompass(Request request) {
        CompassCallbackContext compassCallbackContext =
                new CompassCallbackContext(request, isReserved(request));
        putCallbackContext(compassCallbackContext);
        return Response.SUCCESS;
    }

    private Response unsubscribeCompass(Request request) {
        removeCallbackContext(ACTION_SUBSCRIBE_COMPASS);
        return Response.SUCCESS;
    }

    private Response subscribeProximity(Request request) {
        ProximityCallbackContext proximityCallbackContext =
                new ProximityCallbackContext(request, isReserved(request));
        putCallbackContext(proximityCallbackContext);
        return Response.SUCCESS;
    }

    private Response unsubscribeProximity(Request request) {
        removeCallbackContext(ACTION_SUBSCRIBE_PROXIMITY);
        return Response.SUCCESS;
    }

    private Response subscribeLight(Request request) {
        LightCallbackContext lightCallbackContext =
                new LightCallbackContext(request, isReserved(request));
        putCallbackContext(lightCallbackContext);
        return Response.SUCCESS;
    }

    private Response unsubscribeLight(Request request) {
        removeCallbackContext(ACTION_SUBSCRIBE_LIGHT);
        return Response.SUCCESS;
    }

    private Response subscribeStepCounter(Request request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            HapPermissionManager.getDefault()
                    .requestPermissions(
                            request.getView().getHybridManager(),
                            new String[] {Manifest.permission.ACTIVITY_RECOGNITION},
                            new PermissionCallback() {
                                @Override
                                public void onPermissionAccept() {
                                    StepCounterCallbackContext stepCounterCallbackContext =
                                            new StepCounterCallbackContext(request,
                                                    isReserved(request));
                                    putCallbackContext(stepCounterCallbackContext);
                                }

                                @Override
                                public void onPermissionReject(int reason) {
                                    request.getCallback().callback(Response.USER_DENIED);
                                }
                            });
        } else {
            StepCounterCallbackContext stepCounterCallbackContext =
                    new StepCounterCallbackContext(request, isReserved(request));
            putCallbackContext(stepCounterCallbackContext);
        }
        return Response.SUCCESS;
    }

    private Response unsubscribeStepCounter(Request request) {
        removeCallbackContext(ACTION_SUBSCRIBE_STEP_COUNTER);
        return Response.SUCCESS;
    }

    private int getMinAccuracy(int first, int sec) {
        boolean firstValid = checkValid(first);
        boolean secValid = checkValid(sec);
        // both valid, return smaller one
        if (firstValid && secValid) {
            if (first < sec) {
                return first;
            } else {
                return sec;
            }
        }

        // one valid, return unreliable
        return SensorManager.SENSOR_STATUS_UNRELIABLE;
    }

    private boolean checkValid(int accuracy) {
        return accuracy
                >=
                -1 /* not use SensorManager.SENSOR_STATUS_NO_CONTACT because it requires API 20 :( */
                && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
    }

    private class AccelerometerCallbackContext extends CallbackContext {
        SensorEventListener accelerometerListener;

        public AccelerometerCallbackContext(Request request, boolean reserved) {
            super(Sensor.this, ACTION_SUBSCRIBE_ACCELEROMETER, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            android.hardware.Sensor accelerometer =
                    sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
            accelerometerListener =
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            if (!isPause) {
                                callback(0, event);
                            }
                        }

                        @Override
                        public void onAccuracyChanged(android.hardware.Sensor sensor,
                                                      int accuracy) {
                        }
                    };
            Integer interval = EVENT_INTERVAL_NORMAL;
            try {
                JSONObject params = mRequest.getJSONParams();
                String intervalParam = params.optString(PARAM_INTERVAL);
                if (!TextUtils.isEmpty(intervalParam)) {
                    interval = INTERVAL_MAP.get(intervalParam);
                }
            } catch (JSONException e) {
                Log.e(TAG, "onCreate", e);
            }
            if (interval == null) {
                interval = EVENT_INTERVAL_NORMAL;
            }
            sm.registerListener(accelerometerListener, accelerometer, interval);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(accelerometerListener);
        }

        @Override
        public void callback(int what, Object obj) {
            SensorEvent event = (SensorEvent) obj;
            if (event != null) {
                try {
                    JSONObject result = new JSONObject();
                    result.put(PARAM_X, event.values[0]);
                    result.put(PARAM_Y, event.values[1]);
                    result.put(PARAM_Z, event.values[2]);
                    Response response = new Response(result);
                    mRequest.getCallback().callback(response);
                } catch (JSONException e) {
                    Log.e(TAG, "Fail to callback accelerometer event", e);
                }
            } else {
                Log.e(TAG, "Fail to callback accelerometer because event is null");
            }
        }
    }

    private class CompassCallbackContext extends CallbackContext {
        private SensorEventListener compassAccelerometerListener;
        private SensorEventListener compassGeomagneticListener;
        private float[] lastCompassGravity;
        private float[] lastCompassGeomagnetic;
        private long lastCompassChangedAt;
        private float[] compassR;
        private float[] orientation;
        private int lastAccelerometerAccuracy;
        private int lastGeomagneticAccuracy;

        public CompassCallbackContext(Request request, boolean reserved) {
            super(Sensor.this, ACTION_SUBSCRIBE_COMPASS, request, reserved);
            compassR = new float[9];
            orientation = new float[3];
        }

        @Override
        public void onCreate() {
            super.onCreate();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

            android.hardware.Sensor accelerometer =
                    sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
            compassAccelerometerListener =
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            if (!isPause) {
                                if (lastCompassGravity == null) {
                                    lastCompassGravity = new float[event.values.length];
                                }
                                System.arraycopy(event.values, 0, lastCompassGravity, 0,
                                        event.values.length);
                                callback(0, null);
                            }
                        }

                        @Override
                        public void onAccuracyChanged(android.hardware.Sensor sensor,
                                                      int accuracy) {
                            lastAccelerometerAccuracy = accuracy;
                        }
                    };
            sm.registerListener(compassAccelerometerListener, accelerometer,
                    EVENT_INTERVAL_NORMAL / 2);

            android.hardware.Sensor geomagnetic =
                    sm.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD);
            compassGeomagneticListener =
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            if (!isPause) {
                                if (lastCompassGeomagnetic == null) {
                                    lastCompassGeomagnetic = new float[event.values.length];
                                }
                                System.arraycopy(event.values, 0, lastCompassGeomagnetic, 0,
                                        event.values.length);
                                callback(0, null);
                            }
                        }

                        @Override
                        public void onAccuracyChanged(android.hardware.Sensor sensor,
                                                      int accuracy) {
                            lastGeomagneticAccuracy = accuracy;
                        }
                    };
            sm.registerListener(compassGeomagneticListener, geomagnetic, EVENT_INTERVAL_NORMAL / 2);
        }

        @Override
        public synchronized void onDestroy() {
            super.onDestroy();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            if (compassAccelerometerListener != null) {
                sm.unregisterListener(compassAccelerometerListener);
            }
            lastCompassGravity = null;
            if (compassGeomagneticListener != null) {
                sm.unregisterListener(compassGeomagneticListener);
                compassGeomagneticListener = null;
            }
            lastCompassGeomagnetic = null;
            mHandler.removeMessages(MESSAGE_COMPASS_INTERVAL);
        }

        @Override
        public synchronized void callback(int what, Object obj) {
            if (lastCompassGravity != null && lastCompassGeomagnetic != null) {
                long delay = lastCompassChangedAt + COMPASS_EVENT_INTERVAL
                        - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    SensorManager.getRotationMatrix(
                            compassR, null, lastCompassGravity, lastCompassGeomagnetic);
                    SensorManager.getOrientation(compassR, orientation);
                    try {
                        JSONObject result = new JSONObject();
                        result.put(PARAM_DIRECTION, orientation[0]);
                        result.put(
                                PARAM_ACCURACY,
                                getMinAccuracy(lastAccelerometerAccuracy, lastGeomagneticAccuracy));
                        Response response = new Response(result);
                        mRequest.getCallback().callback(response);
                        lastCompassChangedAt = SystemClock.elapsedRealtime();
                        mHandler.removeMessages(MESSAGE_COMPASS_INTERVAL);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    mHandler.sendEmptyMessageDelayed(MESSAGE_COMPASS_INTERVAL, delay);
                }
            }
        }
    }

    private class ProximityCallbackContext extends CallbackContext {
        SensorEventListener proximityListener;

        public ProximityCallbackContext(Request request, boolean reserved) {
            super(Sensor.this, ACTION_SUBSCRIBE_PROXIMITY, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            android.hardware.Sensor accelerometer = sm.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY);
            if (accelerometer != null) {
                proximityListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (!isPause) {
                            callback(0, event);
                        }
                    }

                    @Override
                    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
                    }
                };
                sm.registerListener(proximityListener, accelerometer, EVENT_INTERVAL_NORMAL);
            } else {
                Log.e(TAG, "subscribeProximity fail,device has no proximity sensor");
                mRequest.getCallback().callback(new Response(Response.CODE_SERVICE_UNAVAILABLE,
                        "devices has no proximity sensor"));
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(proximityListener);
        }

        @Override
        public void callback(int what, Object obj) {
            SensorEvent event = (SensorEvent) obj;
            try {
                JSONObject result = new JSONObject();
                result.put(PARAM_DISTANCE, event.values[0]);
                Response response = new Response(result);
                mRequest.getCallback().callback(response);
            } catch (JSONException e) {
                Log.e(TAG, "Fail to callback accelerometer event", e);
            }
        }
    }

    private class LightCallbackContext extends CallbackContext {
        SensorEventListener lightListener;

        public LightCallbackContext(Request request, boolean reserved) {
            super(Sensor.this, ACTION_SUBSCRIBE_LIGHT, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            android.hardware.Sensor accelerometer =
                    sm.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT);
            lightListener =
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            if (!isPause) {
                                callback(0, event);
                            }
                        }

                        @Override
                        public void onAccuracyChanged(android.hardware.Sensor sensor,
                                                      int accuracy) {
                        }
                    };
            sm.registerListener(lightListener, accelerometer, EVENT_INTERVAL_NORMAL);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(lightListener);
        }

        @Override
        public void callback(int what, Object obj) {
            SensorEvent event = (SensorEvent) obj;
            try {
                JSONObject result = new JSONObject();
                result.put(PARAM_INTENSITY, event.values[0]);
                Response response = new Response(result);
                mRequest.getCallback().callback(response);
            } catch (JSONException e) {
                Log.e(TAG, "Fail to callback accelerometer event", e);
            }
        }
    }

    private class StepCounterCallbackContext extends CallbackContext {

        SensorEventListener stepCounterListener;

        public StepCounterCallbackContext(Request request, boolean reserved) {
            super(Sensor.this, ACTION_SUBSCRIBE_STEP_COUNTER, request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            Activity activity = mRequest.getNativeInterface().getActivity();
            SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            android.hardware.Sensor stepCounter =
                    sm.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER);
            if (stepCounter != null) {
                stepCounterListener =
                        new SensorEventListener() {
                            @Override
                            public void onSensorChanged(SensorEvent event) {
                                if (!isPause) {
                                    callback(0, event);
                                }
                            }

                            @Override
                            public void onAccuracyChanged(android.hardware.Sensor sensor,
                                                          int accuracy) {
                            }
                        };
                sm.registerListener(stepCounterListener, stepCounter, EVENT_INTERVAL_NORMAL);
            } else {
                Log.e(TAG, "subscribeStepCounter fail,device has not step counter sensor");
                mRequest
                        .getCallback()
                        .callback(
                                new Response(Response.CODE_FEATURE_ERROR,
                                        "devices has not step counter sensor"));
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (stepCounterListener != null) {
                Activity activity = mRequest.getNativeInterface().getActivity();
                SensorManager sm =
                        (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
                sm.unregisterListener(stepCounterListener);
            }
        }

        @Override
        public void callback(int what, Object obj) {
            SensorEvent event = (SensorEvent) obj;
            try {
                JSONObject result = new JSONObject();
                result.put(PARAM_STEPS, event.values[0]);
                Response response = new Response(result);
                mRequest.getCallback().callback(response);
            } catch (JSONException e) {
                Log.e(TAG, "Fail to callback step count event", e);
            }
        }
    }
}
