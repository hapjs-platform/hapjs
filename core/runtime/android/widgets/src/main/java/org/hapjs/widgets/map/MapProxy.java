/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.hapjs.common.location.LocationClient;
import org.hapjs.common.location.LocationListener;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.component.bridge.ActivityStateListener;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.widgets.R;
import org.hapjs.widgets.map.model.BaseMapMarker;
import org.hapjs.widgets.map.model.HybridLatLng;
import org.hapjs.widgets.map.model.HybridLatLngBounds;
import org.hapjs.widgets.map.model.LocationInfo;
import org.hapjs.widgets.map.model.MapIndoorInfo;
import org.hapjs.widgets.map.model.MapMarker;
import org.hapjs.widgets.map.model.MapMyLocationStyle;

public abstract class MapProxy implements ActivityStateListener, SensorEventListener {
    public static final int MSG_REFRESH_LOCAL_MARKER_OVERLAY = 1;
    public static final int MSG_REFRESH_POLYLINE_OVERLAY = 2;
    public static final int MSG_REFRESH_CIRCLE_OVERLAY = 3;
    public static final int MSG_REFRESH_GOUND_OVERLAY = 4;
    public static final int MSG_REFRESH_CONTROL_VIEW = 5;
    public static final int MSG_REFRESH_POLYGON_OVERLAY = 6;

    /**
     * 室内图相关 MSG 及 CODE 与华为快应用同学商量决定，保持一致，不要随意改动
     */
    // 切换楼层, 室内ID信息错误
    protected static final int FLOOR_INFO_ERROR_CODE = 1;

    protected static final String FLOOR_INFO_ERROR_MSG = "floor info error";
    // 楼层溢出 即当前室内图不存在该楼层
    protected static final int FLOOR_OVERFLOW_CODE = 2;
    protected static final String FLOOR_OVERFLOW_MSG = "floor overflow";
    // 切换楼层室内ID与当前聚焦室内ID不匹配
    protected static final int FOCUSED_ID_ERROR_CODE = 3;
    protected static final String FOCUSED_ID_ERROR_MSG = "focused ID error";
    // 切换楼层失败
    protected static final int SWITCH_ERROR_CODE = 4;
    protected static final String SWITCH_ERROR_MSG = "switch error";

    protected Context mContext;
    protected HybridManager mHybridManager;

    protected String mMapType;

    protected boolean mIsShowMyLocation;
    protected boolean mNeedMoveToMyLocation = false;
    protected boolean mIsFirstLoc = true;
    protected HybridLatLng mCurrentLocation;
    protected float mCurrentAccuracy;
    protected int mCurrentDirection;
    protected LocationListener mLocationListener;
    protected MapMyLocationStyle mMyLocationStyle;
    private float mLastDirectionX;
    private SensorManager mSensorManager;
    private LocationClient mLocationClient;

    protected MapProxy(Activity context, HybridManager hybridManager, String mapType) {
        mContext = context;
        mHybridManager = hybridManager;
        if (mapType == null) {
            throw new IllegalArgumentException("mapType cant't be null.");
        }
        mMapType = mapType;
    }

    public final View getMapView() {
        MapProvider mapProvider = ProviderManager.getDefault().getProvider(MapProvider.NAME);
        if (mapProvider != null) {
            mapProvider.onMapCreate(mMapType);
        }

        return createMapView();
    }

    protected abstract View createMapView();

    @Override
    public void onActivityCreate() {
    }

    @Override
    public void onActivityStart() {
        if (mIsShowMyLocation && mLocationListener != null) {
            startLocationClient();
        }
    }

    @Override
    public void onActivityResume() {
        registerSensor();
    }

    @Override
    public void onActivityPause() {
    }

    @Override
    public void onActivityStop() {
        stopLocationClient();
        unregisterSensor();
    }

    @Override
    public void onActivityDestroy() {
        stopLocationClient();
        unregisterSensor();
    }

    public void onComponentDestroy() {
        stopLocationClient();
        unregisterSensor();
    }

    protected void registerSensor() {
        if (!mIsShowMyLocation) {
            return;
        }
        if (mSensorManager == null) {
            mSensorManager =
                    (SensorManager) mContext.getApplicationContext()
                            .getSystemService(Context.SENSOR_SERVICE);
        }
        unregisterSensor();
        mSensorManager.registerListener(
                this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    protected void unregisterSensor() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    protected void startLocationClient() {
        mIsFirstLoc = true;
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(mContext);
        }
        if (mLocationListener != null) {
            mLocationClient.registerLocationListener(mLocationListener);
        }
        if (mIsShowMyLocation) {
            mLocationClient.start();
        } else {
            mLocationClient.syncCurrentLocation();
        }
    }

    protected void stopLocationClient() {
        if (mLocationClient != null && mLocationListener != null) {
            mLocationClient.unRegisterLocationListener(mLocationListener);
            mLocationClient.stop();
            mLocationListener = null;
        }
    }

    public abstract void updateLatLng(double latitude, double longitude, String coordType);

    public abstract HybridLatLng getCenterLocation();

    public abstract Point getCenterPoint();

    public abstract float getScale();

    public abstract void setScale(float scale);

    public abstract void setIncludePoints(
            List<HybridLatLng> points, Rect padding, OnRetCallbackListener retCallbackListener);

    public abstract void setRotateAngle(float angle);

    public abstract void setShowMyLocation(boolean isShow, boolean needMoveToMyLocation);

    public abstract void setMyLocationStyle(MapMyLocationStyle myLocationStyle);

    public abstract void moveToMyLocation();

    public abstract String getCoordType();

    public abstract String getCoordType(double latitude, double longitude);

    public abstract String getCoordType(double latitude, double longitude, String originCoordType);

    public abstract HybridLatLng convertCoordType(
            String fromType, String toType, double latitude, double longitude);

    public abstract HybridLatLngBounds getRegion();

    public abstract List<String> getSupportedCoordTypes();

    public abstract void getMapViewSnapshot(OnMapViewSnapshotListener listener);

    public abstract void translateMarker(
            int markerId,
            HybridLatLng destination,
            boolean autoRotate,
            int rotate,
            int duration,
            OnAnimationEndListener listener,
            OnRetCallbackListener retCallbackListener);

    public abstract void setGeolocationMarkers(List<MapMarker> markers);

    public abstract void removeGeolocationMarkers();

    public abstract void setMarkers(String markerJSON);

    public abstract void setPolylines(String polylineJSON);

    public abstract void setPolygons(String polygonJSON);

    public abstract void setCircles(String circleJSON);

    public abstract void setGrounds(String groundJSON);

    public abstract void setHeatmapLayer(String heatMapLayerContent, RenderEventCallback callback);

    public abstract void setControls(String controlJSON);

    public abstract void addCustomMarkerView(View hostView, BaseMapMarker baseMapMarker);

    public abstract void removeCustomMarkerView(View markView);

    public abstract void setOnMapLoadedListener(OnMapLoadedListener listener);

    public abstract void setOnRegionChangeListener(OnRegionChangeListener listener);

    public abstract void setOnMapClickListener(OnMapTapListener listener);

    public abstract void setOnMapPoiClickListener(OnMapPoiTapListener onMapPoiTapListener);

    public abstract void setOnMarkerClickListener(OnMarkerTapListener listener);

    public abstract void setOnCalloutClickListener(OnCalloutTapListener listener);

    public abstract void setOnControlClickListener(OnControlTapListener listener);

    protected abstract void updateCurrentLocation();

    protected abstract void updateMapControls(String key, boolean show);

    protected abstract void updateMapGestureControler(String key, boolean enable);

    public abstract void setOnIndoorModeChangeListener(OnIndoorModeChangeListener listener);

    public abstract void setIndoorEnable(boolean enable);

    public abstract void switchIndoorFloor(
            String poiId, String toFloor, OnRetCallbackListener onRetCallbackListener);

    public abstract void setMapComponentPosition(String mapComponent, Point point);

    public abstract void setMaxAndMinScaleLevel(float maxLevel, float minLevel);

    public abstract void setOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener);

    public abstract void reverseGeoCodeResult(
            HybridLatLng latLng, OnLocGeoCoderResultListener listener);

    public abstract void poiSearchResult(
            String keyword, LocationInfo locationInfo, OnLocPoiSearchResultListener listener);

    public abstract void suggestionResult(
            String keyword, LocationInfo locationInfo, OnLocSuggestionResultListener listener);

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[SensorManager.DATA_X];
        if (Math.abs(x - mLastDirectionX) > 1.0) {
            mCurrentDirection = (int) x;
            updateCurrentLocation();
        }
        mLastDirectionX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void checkPermission(String[] permissions, final OnPermissionRequestListener listener) {
        final Activity activity = mHybridManager.getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        HapPermissionManager.getDefault()
                .requestPermissions(
                        mHybridManager,
                        permissions,
                        new PermissionCallback() {
                            @Override
                            public void onPermissionAccept() {
                                activity.runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (listener != null) {
                                                    listener.onPermissionResult(true);
                                                }
                                            }
                                        });
                            }

                            @Override
                            public void onPermissionReject(int reason, boolean dontDisturb) {
                                activity.runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (listener != null) {
                                                    listener.onPermissionResult(false);
                                                }
                                            }
                                        });
                            }
                        });
    }

    protected void requestLocationService() {
        Activity activity = mHybridManager.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity, ThemeUtils.getAlertDialogTheme());
        builder.setMessage(R.string.permission_request_location_service);
        builder.setPositiveButton(R.string.dlg_permission_ok, null);
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        final LifecycleListener lifecycleListener =
                new LifecycleListener() {
                    @Override
                    public void onDestroy() {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        mHybridManager.removeLifecycleListener(this);
                        super.onDestroy();
                    }
                };
        mHybridManager.addLifecycleListener(lifecycleListener);
        DarkThemeUtil.disableForceDark(dialog);
        dialog.show();
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }

    public interface OnCalloutTapListener {
        void onCalloutTap(int markerId);
    }

    public interface OnControlTapListener {
        void onControlTap(int controlId);
    }

    public interface OnMapLoadedListener {
        void onMapLoaded();
    }

    public interface OnMapTapListener {
        void onMapClick(HybridLatLng latLng);
    }

    public interface OnMapPoiTapListener {
        void onMapPoiClick(Map<String, Object> params);
    }

    public interface OnMarkerTapListener {
        void onMarkerTap(int markerId);
    }

    public interface OnRegionChangeListener {
        void onRegionChange(HybridLatLngBounds bounds);
    }

    public interface OnPermissionRequestListener {
        void onPermissionResult(boolean granted);
    }

    public interface OnRetCallbackListener {
        void onSuccess();

        void onFail(String reason);

        void onFail(Object... params);

        void onComplete();
    }

    public interface OnIndoorModeChangeListener {
        void onIndoorModeChange(MapIndoorInfo mapIndoorInfo);
    }

    public interface OnMapViewSnapshotListener {
        void onSnapshotReady(Bitmap bitmap);
    }

    public interface OnStatusChangeListener {
        void onStatusChangeStart();

        void onStatusChange();

        void onStatusChangeFinish(HybridLatLng latLngCur);
    }

    public interface OnLocGeoCoderResultListener {
        void onGeoCoderResultListener(List<LocationInfo> datas);
    }

    public interface OnLocPoiSearchResultListener {
        void onPoiSearchResultListener(List<LocationInfo> datas);
    }

    public interface OnLocSuggestionResultListener {
        void onSuggestionResultListener(List<LocationInfo> datas);
    }
}
