/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map;

import static org.hapjs.widgets.map.model.MapMyLocationStyle.DEFAULT_CIRCLE_FILL_COLOR;
import static org.hapjs.widgets.map.model.MapMyLocationStyle.DEFAULT_CIRCLE_STROKE_COLOR;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.SnapshotUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.OnDomTreeChangeListener;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.widgets.map.model.HybridLatLng;
import org.hapjs.widgets.map.model.HybridLatLngBounds;
import org.hapjs.widgets.map.model.MapFrameLayout;
import org.hapjs.widgets.map.model.MapIndoorInfo;
import org.hapjs.widgets.map.model.MapMyLocationStyle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@WidgetAnnotation(
        name = Map.WIDGET_NAME,
        methods = {
                Map.METHOD_GET_CENTER_LOCATION,
                Map.METHOD_TRANSLATE_MARKER,
                Map.METHOD_MOVE_TO_MY_LOCATION,
                Map.METHOD_INCLUDE_POINTS,
                Map.METHOD_GET_COORDTYPE,
                Map.METHOD_CONVERT_COORD,
                Map.METHOD_GET_REGION,
                Map.METHOD_GET_SCALE,
                Map.METHOD_GET_SUPPORTED_COORDTYPES,
                Map.METHOD_SET_INDOOR_ENABLE,
                Map.METHOD_SWITCH_INDOOR_FLOOR,
                Map.METHOD_SET_COMPASS_POSITION,
                Map.METHOD_SET_SCALE_POSITION,
                Map.METHOD_SET_ZOOM_POSITION,
                Map.METHOD_SET_MAX_AND_MIN_SCALE_LEVEL,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Map extends Container<MapFrameLayout> {

    public static final double DEFAULT_LONGITUDE = 116.39739;
    public static final double DEFAULT_LATITUDE = 39.90886;
    public static final int DEFAULT_SCALE = 11;
    // attribute
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String SCALE = "scale";
    public static final String ROTATE_ANGLE = "rotate";
    public static final String MARKERS = "markers";
    public static final String IS_SHOW_MY_LOCATION = "showmylocation";
    public static final String POLYLINES = "polylines";
    public static final String POLYGONS = "polygons";
    public static final String CIRCLES = "circles";
    public static final String CONTROLS = "controls";
    public static final String INCLUDE_POINTS = "includepoints";
    public static final String GROUNDOVERLAYS = "groundoverlays";
    public static final String HEATMAPLAYERS = "heatmaplayer";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String ICON_PATH = "iconPath";
    public static final String OPACITY = "opacity";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String Z_INDEX = "zIndex";
    public static final String ANCHOR = "anchor";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String CALLOUT = "callout";
    public static final String LABEL = "label";
    public static final String COORDTYPE_CAMEL = "coordType";
    public static final String OFFSET_X = "offsetX";
    public static final String OFFSET_Y = "offsetY";
    public static final String WEIGHTED_POINTS = "weightedPoints";
    public static final String INTENSITY = "intensity";
    public static final String GRADIENT = "gradient";
    public static final String COLORS = "colors";
    public static final String START_POINTS = "startPoints";
    public static final String IS_LATLNG_IN_CHINA = "isLatLngInChina";
    public static final String POINTS = "points";
    public static final String COLOR = "color";
    public static final String DOTTED = "dotted";
    public static final String RADIUS = "radius";
    public static final String FILL_COLOR = "fillColor";
    public static final String BORDER_WIDTH = "borderWidth";
    public static final String BORDER_COLOR = "borderColor";
    public static final String STROKE_WIDTH = "strokeWidth";
    public static final String STROKE_COLOR = "strokeColor";
    public static final String ARROW_LINE = "arrowLine";
    public static final String ARROW_ICON_PATH = "arrowIconPath";
    public static final String CLICKABLE = "clickable";
    public static final String POSITION = "position";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";
    public static final String CONTENT = "content";
    public static final String FONT_SIZE = "fontSize";
    public static final String BORDER_RADIUS = "borderRadius";
    public static final String PADDING = "padding";
    public static final String BACKGROUND_COLOR = "backgroundColor";
    public static final String DISPLAY = "display";
    public static final String TEXT_ALIGN = "textAlign";
    public static final String OFFSET = "offset";
    public static final String IS_SHOW_CALLOUT = "isShowCallout";
    public static final String SHOW_COMPASS = "showcompass";
    public static final String SHOW_SCALE = "showscale";
    public static final String SHOW_ZOOM = "showzoom";
    public static final String ENABLE_OVERLOOKING = "enableoverlooking";
    public static final String ENABLE_ZOOM = "enablezoom";
    public static final String ENABLE_SCROLL = "enablescroll";
    public static final String ENABLE_ROTATE = "enablerotate";
    public static final String IS_CONVERT_HTML = "convertHtml";
    public static final String ANCHOR_X = "anchorX";
    public static final String ANCHOR_Y = "anchorY";
    public static final String NORTH_EAST = "northEast";
    public static final String SOUTH_WEST = "southWest";
    public static final String VISIBLE = "visible";
    // map component
    public static final String MAP_COMPONENT_COMPASS = "compass";
    public static final String MAP_COMPONENT_SCALE_CONTROL = "scaleControl";
    public static final String MAP_COMPONENT_ZOOM_CONTROLS = "zoomControls";
    public static final String CALLBACK_KEY_MARKER_ID = "markerId";
    protected static final String WIDGET_NAME = "map";
    // method
    protected static final String METHOD_GET_CENTER_LOCATION = "getCenterLocation";
    protected static final String METHOD_TRANSLATE_MARKER = "translateMarker";
    protected static final String METHOD_MOVE_TO_MY_LOCATION = "moveToMyLocation";
    protected static final String METHOD_INCLUDE_POINTS = "includePoints";
    protected static final String METHOD_GET_REGION = "getRegion";
    protected static final String METHOD_GET_SCALE = "getScale";
    protected static final String METHOD_GET_COORDTYPE = "getCoordType";
    protected static final String METHOD_CONVERT_COORD = "convertCoord";
    protected static final String METHOD_GET_SUPPORTED_COORDTYPES = "getSupportedCoordTypes";
    protected static final String METHOD_SET_INDOOR_ENABLE = "setIndoorEnable";
    protected static final String METHOD_SWITCH_INDOOR_FLOOR = "switchIndoorFloor";
    protected static final String METHOD_SET_COMPASS_POSITION = "setCompassPosition";
    protected static final String METHOD_SET_SCALE_POSITION = "setScalePosition";
    protected static final String METHOD_SET_ZOOM_POSITION = "setZoomPosition";
    protected static final String METHOD_SET_MAX_AND_MIN_SCALE_LEVEL = "setMaxAndMinScaleLevel";
    private static final String TAG = "Map";
    private static final String COORDTYPE = "coordtype";
    private static final String ENABLE = "enable";
    private static final String POI_ID = "poiId";
    private static final String TO_FLOOR = "toFloor";
    private static final String MAX_LEVEL = "maxLevel";
    private static final String MIN_LEVEL = "minLevel";
    // style
    private static final String MY_LOCATION_FILL_COLOR = "mylocationFillColor";
    private static final String MY_LOCATION_STROKE_COLOR = "mylocationStrokeColor";
    private static final String MY_LOCATION_ICON_PATH = "mylocationIconPath";
    // event
    private static final String EVENT_LOADED = "loaded";
    private static final String EVENT_REGION_CHANGE = "regionchange";
    private static final String EVENT_TAP = "tap";
    private static final String EVENT_MARKER_TAP = "markertap";
    private static final String EVENT_CALLOUT_TAP = "callouttap";
    private static final String EVENT_CONTROL_TAP = "controltap";
    private static final String EVENT_INDOOR_MODE_CHANGE = "indoormodechange";
    private static final String EVENT_POI_TAP = "poitap";
    // callback_key
    private static final String CALLBACK_KEY_POINTS = "points";
    private static final String CALLBACK_KEY_CONTROL_ID = "controlId";
    private static final String CALLBACK_KEY_PADDING = "padding";

    private static final String CALLBACK_KEY_SOUTHWEST = "southwest";
    private static final String CALLBACK_KEY_NORTHEAST = "northeast";

    private static final String CALLBACK_KEY_DESTINATION = "destination";
    private static final String CALLBACK_KEY_AUTO_ROTATE = "autoRotate";
    private static final String CALLBACK_KEY_ROTATE = "rotate";
    private static final String CALLBACK_KEY_DURATION = "duration";
    private static final String CALLBACK_KEY_ANIMATION_END = "animationEnd";

    private static final String CALLBACK_KEY_SUPPORTED_COORDTYPES = "coordTypes";

    private static final String CALLBACK_KEY_SUCCESS = "success";
    private static final String CALLBACK_KEY_FAIL = "fail";
    private static final String CALLBACK_KEY_COMPLETE = "complete";

    private double mLongitude = Double.MAX_VALUE;
    private double mLatitude = Double.MAX_VALUE;
    private String mCenterLocCoordType;
    private Boolean mIsShowMyLocation = null;
    private boolean mIsApplyAttributeByHostViewAttached = false;

    private MapMyLocationStyle mMyLocationStyle;
    private MapProxy mMapProxy;
    private MapProvider mMapProvider;

    public Map(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            java.util.Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        callback.addActivityStateListener(this);
        initMapProxy();
    }

    public static Rect parsePadding(HapEngine hapEngine, String paddingStr) {
        Rect padding = new Rect(0, 0, 0, 0);
        if (paddingStr == null) {
            return padding;
        }
        String[] paddingArray = paddingStr.trim().split(" +");

        switch (paddingArray.length) {
            case 1:
                int all = (int) Attributes.getFloat(hapEngine, paddingArray[0], 0);
                padding.set(all, all, all, all);
                return padding;
            case 2:
                int y = (int) Attributes.getFloat(hapEngine, paddingArray[0], 0);
                int x = (int) Attributes.getFloat(hapEngine, paddingArray[1], 0);
                padding.set(x, y, x, y);
                return padding;
            case 3:
                int top1 = (int) Attributes.getFloat(hapEngine, paddingArray[0], 0);
                int x1 = (int) Attributes.getFloat(hapEngine, paddingArray[1], 0);
                int bottom1 = (int) Attributes.getFloat(hapEngine, paddingArray[2], 0);
                padding.set(x1, top1, x1, bottom1);
                return padding;
            case 4:
                int top2 = (int) Attributes.getFloat(hapEngine, paddingArray[0], 0);
                int right2 = (int) Attributes.getFloat(hapEngine, paddingArray[1], 0);
                int bottom2 = (int) Attributes.getFloat(hapEngine, paddingArray[2], 0);
                int left2 = (int) Attributes.getFloat(hapEngine, paddingArray[3], 0);
                padding.set(left2, top2, right2, bottom2);
                return padding;
            default:
                return padding;
        }
    }

    private void initMapProxy() {
        mMapProvider = ProviderManager.getDefault().getProvider(MapProvider.NAME);
        if (mMapProvider == null) {
            Log.e(TAG, "Map: mMapProvider == null, set mMapProxy = null.");
            return;
        }
        HybridView hybridView = getHybridView();
        if (hybridView == null) {
            Log.e(TAG, "Map: hybridView == null, set mMapProxy = null.");
            return;
        }
        HybridManager manager = hybridView.getHybridManager();
        mMapProxy = mMapProvider.createMapProxy(manager, mCallback);
    }

    @Override
    protected MapFrameLayout createViewImpl() {
        if (mMapProxy == null) {
            return MapFrameLayout.getNoMapView(this, mContext);
        }
        MapFrameLayout layout = (MapFrameLayout) mMapProxy.getMapView();
        layout.setComponent(this);
        if (GrayModeManager.getInstance().shouldApplyGrayMode()) {
            DocComponent docComponent = getRootComponent();
            DecorLayout decorLayout = docComponent.getDecorLayout();
            decorLayout.applyGrayMode(false);
        }
        return layout;
    }

    @Override
    public void onHostViewAttached(ViewGroup parent) {
        mIsApplyAttributeByHostViewAttached = true;
        super.onHostViewAttached(parent);
        mIsApplyAttributeByHostViewAttached = false;
    }

    @Override
    public void addChild(Component child, int index) {
        if (mMapProxy == null) {
            return;
        }
        if (!(child instanceof CustomMarker)) {
            mCallback.onJsException(
                    new IllegalArgumentException("Map child component must be CustomMarker"));
        }
        final int childrenCount = getChildCount();
        if (index < 0 || index > childrenCount) {
            index = childrenCount;
        }
        mChildren.add(index, child);
        for (OnDomTreeChangeListener listener : mDomTreeChangeListeners) {
            listener.onDomTreeChange(child, true);
        }
        mMapProxy.addCustomMarkerView(
                child.getHostView(), ((CustomMarker) child).getCustomMarkerAttr());
    }

    @Override
    public void removeChild(Component child) {
        if (mMapProxy == null) {
            return;
        }
        final int index = mChildren.indexOf(child);
        if (index < 0) {
            return;
        }
        mChildren.remove(child);
        mMapProxy.removeCustomMarkerView(child.getHostView());
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        if (mIsApplyAttributeByHostViewAttached) {
            return super.setAttribute(key, attribute);
        }
        if (mMapProxy == null) {
            return true;
        }
        switch (key) {
            case LONGITUDE:
                mLongitude = Attributes.getDouble(attribute, DEFAULT_LONGITUDE);
                updateCenterLocation();
                return true;
            case LATITUDE:
                mLatitude = Attributes.getDouble(attribute, DEFAULT_LATITUDE);
                updateCenterLocation();
                return true;
            case COORDTYPE:
                updateCenterCoordType(Attributes.getString(attribute));
                return true;
            case SCALE:
                float scale = Attributes.getFloat(mHapEngine, attribute, DEFAULT_SCALE);
                mMapProxy.setScale(scale);
                return true;
            case ROTATE_ANGLE:
                float angle = Attributes.getFloat(mHapEngine, attribute, 0);
                mMapProxy.setRotateAngle(angle);
                return true;
            case IS_SHOW_MY_LOCATION:
                boolean isShow = Attributes.getBoolean(attribute, false);

                // 防止快应用在第一次使用地图组件的时候，弹出两次定位权限申请框
                if (mIsShowMyLocation != null && isShow == mIsShowMyLocation) {
                    return true;
                }

                mIsShowMyLocation = isShow;
                // 增加needMoveToMyLocation参数来表示是否在显示当前位置时，将视图中心点移动到当前位置
                // 因遗留原因该值需要为true，以兼容老版本的表现
                mMapProxy.setShowMyLocation(isShow, true);
                return true;
            case MARKERS:
                String markers = Attributes.getString(attribute);
                setMarkers(markers);
                return true;
            case POLYLINES:
                String polylines = Attributes.getString(attribute);
                setPolylines(polylines);
                return true;
            case POLYGONS:
                String polygons = Attributes.getString(attribute);
                setPolygons(polygons);
                return true;
            case CIRCLES:
                String circles = Attributes.getString(attribute);
                setCircles(circles);
                return true;
            case CONTROLS:
                final String controls = Attributes.getString(attribute);
                if (mHost.getWidth() == 0 || mHost.getHeight() == 0) {
                    mHost
                            .getViewTreeObserver()
                            .addOnPreDrawListener(
                                    new ViewTreeObserver.OnPreDrawListener() {
                                        @Override
                                        public boolean onPreDraw() {
                                            if (mHost != null) {
                                                mHost.getViewTreeObserver()
                                                        .removeOnPreDrawListener(this);
                                            }
                                            setControls(controls);
                                            return true;
                                        }
                                    });
                } else {
                    setControls(controls);
                }
                return true;
            case INCLUDE_POINTS:
                String points = Attributes.getString(attribute);
                setIncludePoints(points);
                return true;
            case GROUNDOVERLAYS:
                String groundoverlays = Attributes.getString(attribute);
                setGroundoverlay(groundoverlays);
                return true;
            case HEATMAPLAYERS:
                String heatMapLayerContent = Attributes.getString(attribute);
                setHeatmapLayer(heatMapLayerContent);
                return true;
            case MY_LOCATION_FILL_COLOR:
            case MY_LOCATION_STROKE_COLOR:
            case MY_LOCATION_ICON_PATH:
                String myLocationStyleStr = Attributes.getString(attribute);
                setMyLocationStyle(key, myLocationStyleStr);
                return true;
            case ENABLE_OVERLOOKING:
                updateMapGestureControler(key, Attributes.getBoolean(attribute, false));
                return true;
            case SHOW_ZOOM:
            case SHOW_SCALE:
                boolean show = Attributes.getBoolean(attribute, true);
                updateMapControls(key, show);
                return true;
            case SHOW_COMPASS:
            case ENABLE_ZOOM:
            case ENABLE_ROTATE:
            case ENABLE_SCROLL:
                boolean enable = Attributes.getBoolean(attribute, true);
                updateMapGestureControler(key, enable);
                return true;
            default:
                return super.setAttribute(key, attribute);
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null || mMapProxy == null) {
            return true;
        }

        switch (event) {
            case EVENT_LOADED:
                mMapProxy.setOnMapLoadedListener(
                        new MapProxy.OnMapLoadedListener() {
                            @Override
                            public void onMapLoaded() {
                                mCallback.onJsEventCallback(getPageId(), mRef, EVENT_LOADED,
                                        Map.this, null, null);
                            }
                        });
                return true;
            case EVENT_REGION_CHANGE:
                mMapProxy.setOnRegionChangeListener(
                        new MapProxy.OnRegionChangeListener() {
                            @Override
                            public void onRegionChange(HybridLatLngBounds bounds) {

                                mCallback.onJsEventCallback(
                                        getPageId(),
                                        mRef,
                                        EVENT_REGION_CHANGE,
                                        Map.this,
                                        convertBoundsToMap(bounds),
                                        null);
                            }
                        });
                return true;
            case EVENT_TAP:
                mMapProxy.setOnMapClickListener(
                        new MapProxy.OnMapTapListener() {
                            @Override
                            public void onMapClick(HybridLatLng latLng) {
                                java.util.Map<String, Object> params = new HashMap<>(2);
                                params.put(LATITUDE, latLng.latitude);
                                params.put(LONGITUDE, latLng.longitude);
                                mCallback.onJsEventCallback(getPageId(), mRef, EVENT_TAP, Map.this,
                                        params, null);
                            }
                        });
                return true;
            case EVENT_MARKER_TAP:
                mMapProxy.setOnMarkerClickListener(
                        new MapProxy.OnMarkerTapListener() {
                            @Override
                            public void onMarkerTap(int markerId) {
                                java.util.Map<String, Object> params = new HashMap<>(1);
                                params.put(CALLBACK_KEY_MARKER_ID, markerId);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_MARKER_TAP, Map.this, params,
                                        null);
                            }
                        });
                return true;
            case EVENT_CALLOUT_TAP:
                mMapProxy.setOnCalloutClickListener(
                        new MapProxy.OnCalloutTapListener() {
                            @Override
                            public void onCalloutTap(int markerId) {
                                java.util.Map<String, Object> params = new HashMap<>(1);
                                params.put(CALLBACK_KEY_MARKER_ID, markerId);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_CALLOUT_TAP, Map.this, params,
                                        null);
                            }
                        });
                return true;
            case EVENT_CONTROL_TAP:
                mMapProxy.setOnControlClickListener(
                        new MapProxy.OnControlTapListener() {
                            @Override
                            public void onControlTap(int controlId) {
                                java.util.Map<String, Object> params = new HashMap<>(1);
                                params.put(CALLBACK_KEY_CONTROL_ID, controlId);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_CONTROL_TAP, Map.this, params,
                                        null);
                            }
                        });
                return true;
            case EVENT_INDOOR_MODE_CHANGE:
                mMapProxy.setOnIndoorModeChangeListener(
                        new MapProxy.OnIndoorModeChangeListener() {
                            @Override
                            public void onIndoorModeChange(MapIndoorInfo mapIndoorInfo) {
                                java.util.Map<String, Object> params = mapIndoorInfo.converToMap();
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_INDOOR_MODE_CHANGE, Map.this,
                                        params, null);
                            }
                        });
                return true;
            case EVENT_POI_TAP:
                mMapProxy.setOnMapPoiClickListener(
                        new MapProxy.OnMapPoiTapListener() {

                            @Override
                            public void onMapPoiClick(java.util.Map<String, Object> params) {
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_POI_TAP, Map.this, params, null);
                            }
                        });
                return true;
            default:
                return super.addEvent(event);
        }
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null || mMapProxy == null) {
            return true;
        }

        switch (event) {
            case EVENT_LOADED:
                mMapProxy.setOnMapLoadedListener(null);
                return true;
            case EVENT_REGION_CHANGE:
                mMapProxy.setOnRegionChangeListener(null);
                return true;
            case EVENT_TAP:
                mMapProxy.setOnMapClickListener(null);
                return true;
            case EVENT_MARKER_TAP:
                mMapProxy.setOnMarkerClickListener(null);
                return true;
            case EVENT_CALLOUT_TAP:
                mMapProxy.setOnCalloutClickListener(null);
                return true;
            case EVENT_CONTROL_TAP:
                mMapProxy.setOnControlClickListener(null);
                return true;
            case EVENT_INDOOR_MODE_CHANGE:
                mMapProxy.setOnIndoorModeChangeListener(null);
                return true;
            default:
                return super.removeEvent(event);
        }
    }

    @Override
    public void invokeMethod(String methodName, java.util.Map<String, Object> args) {
        super.invokeMethod(methodName, args);

        if (mMapProxy == null) {
            return;
        }

        switch (methodName) {
            case METHOD_GET_CENTER_LOCATION:
                getCenterLocation(args);
                break;
            case METHOD_TRANSLATE_MARKER:
                translateMarker(args);
                break;
            case METHOD_MOVE_TO_MY_LOCATION:
                mMapProxy.moveToMyLocation();
                break;
            case METHOD_INCLUDE_POINTS:
                includePoints(args);
                break;
            case METHOD_GET_COORDTYPE:
                getCoordType(args);
                break;
            case METHOD_CONVERT_COORD:
                convertCoord(args);
                break;
            case METHOD_GET_REGION:
                getRegion(args);
                break;
            case METHOD_GET_SCALE:
                getScale(args);
                break;
            case METHOD_GET_SUPPORTED_COORDTYPES:
                getSupportedCoordTypes(args);
                break;
            case METHOD_SET_INDOOR_ENABLE:
                setIndoorEnable(args);
                break;
            case METHOD_SWITCH_INDOOR_FLOOR:
                switchIndoorFloor(args);
                break;
            case METHOD_SET_COMPASS_POSITION:
                setMapComponentPosition(MAP_COMPONENT_COMPASS, args);
                break;
            case METHOD_SET_SCALE_POSITION:
                setMapComponentPosition(MAP_COMPONENT_SCALE_CONTROL, args);
                break;
            case METHOD_SET_ZOOM_POSITION:
                setMapComponentPosition(MAP_COMPONENT_ZOOM_CONTROLS, args);
                break;
            case METHOD_SET_MAX_AND_MIN_SCALE_LEVEL:
                setMaxAndMinScaleLevel(args);
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityCreate() {
        super.onActivityCreate();
        if (mMapProxy != null) {
            mMapProxy.onActivityCreate();
        }
    }

    @Override
    public void onActivityStart() {
        super.onActivityStart();
        if (mMapProxy != null) {
            mMapProxy.onActivityStart();
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mMapProxy != null) {
            mMapProxy.onActivityResume();
        }
    }

    @Override
    public void onActivityPause() {
        if (mMapProxy != null) {
            mMapProxy.onActivityPause();
        }
        super.onActivityPause();
    }

    @Override
    public void onActivityStop() {
        if (mMapProxy != null) {
            mMapProxy.onActivityStop();
        }
        super.onActivityStop();
    }

    @Override
    public void onActivityDestroy() {
        if (mMapProxy != null) {
            mMapProxy.onActivityDestroy();
        }
        super.onActivityDestroy();
    }

    @Override
    public void destroy() {
        if (mMapProvider != null && mMapProvider.isCompatibleWithVersionKitkat()) {
            Log.i(TAG, "use Compatible strategy");
        } else {
            mCallback.removeActivityStateListener(this);
        }
        if (mMapProxy != null) {
            mMapProxy.onComponentDestroy();
        } else {
            Log.w(TAG, "destroy(), mMapProxy is null.");
        }
        super.destroy();
    }

    @Override
    protected void hostViewToTempFilePath(java.util.Map<String, Object> args) {
        if (args == null) {
            Log.e(TAG, "hostViewToTempFilePath failed: args is null");
            return;
        }
        if (mMapProxy != null) {
            mMapProxy.getMapViewSnapshot(
                    new MapProxy.OnMapViewSnapshotListener() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {
                            Executors.io()
                                    .execute(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    String fileType = Attributes
                                                            .getString(args.get(KEY_FILE_TYPE),
                                                                    "png");
                                                    double quality = Attributes
                                                            .getDouble(args.get(KEY_QUALITY), 1.0);
                                                    String successCallbackId = Attributes
                                                            .getString(args.get(KEY_SUCCESS));
                                                    String failCallbackId = Attributes
                                                            .getString(args.get(KEY_FAIL));
                                                    String completeCallbackId = Attributes
                                                            .getString(args.get(KEY_COMPLETE));

                                                    Uri snapshotUri =
                                                            SnapshotUtils.saveSnapshot(
                                                                    getHybridView(), bitmap,
                                                                    getRef(), fileType, quality);
                                                    if (snapshotUri != null
                                                            && !TextUtils.isEmpty(successCallbackId)) {
                                                        String internalUri =
                                                                getHybridView()
                                                                        .getHybridManager()
                                                                        .getApplicationContext()
                                                                        .getInternalUri(
                                                                                snapshotUri);
                                                        java.util.Map<String, Object> params =
                                                                new HashMap<>();
                                                        params.put(TEMP_FILE_PATH, internalUri);
                                                        mCallback.onJsMethodCallback(getPageId(),
                                                                successCallbackId, params);
                                                    } else if (!TextUtils.isEmpty(failCallbackId)) {
                                                        mCallback.onJsMethodCallback(getPageId(),
                                                                failCallbackId);
                                                    }
                                                    if (!TextUtils.isEmpty(completeCallbackId)) {
                                                        mCallback.onJsMethodCallback(getPageId(),
                                                                completeCallbackId);
                                                    }
                                                }
                                            });
                        }
                    });
        }
    }

    private void updateCenterLocation() {
        if (mMapProxy == null) {
            return;
        }
        if (mLongitude == Double.MAX_VALUE || mLatitude == Double.MAX_VALUE) {
            return;
        }
        mMapProxy.updateLatLng(mLatitude, mLongitude, mCenterLocCoordType);
    }

    private void updateCenterCoordType(String coordType) {
        if (mMapProxy == null) {
            return;
        }
        if (!TextUtils.isEmpty(coordType) && !CoordType.isLegal(coordType)) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "coordtype attr only supports [wgs84, gcj02]. current is "
                                    + coordType));
        } else {
            mCenterLocCoordType = coordType;
            updateCenterLocation();
        }
    }

    private void updateMapControls(String key, boolean show) {
        if (mMapProxy != null) {
            mMapProxy.updateMapControls(key, show);
        }
    }

    private void updateMapGestureControler(String key, boolean enable) {
        if (mMapProxy != null) {
            mMapProxy.updateMapGestureControler(key, enable);
        }
    }

    private void getScale(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null || !args.containsKey(CALLBACK_KEY_SUCCESS)) {
            callbackComplete(args);
            return;
        }
        float scale = mMapProxy.getScale();
        BigDecimal scaleBig = new BigDecimal(String.valueOf(scale));
        double scaleFix = scaleBig.doubleValue();
        java.util.Map<String, Object> map = new HashMap<>(1);
        map.put(SCALE, scaleFix);
        String callbackId = (String) args.get(CALLBACK_KEY_SUCCESS);
        mCallback.onJsMethodCallback(getPageId(), callbackId, map);
        callbackComplete(args);
    }

    private void getRegion(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null || !args.containsKey(CALLBACK_KEY_SUCCESS)) {
            callbackComplete(args);
            return;
        }
        HybridLatLngBounds bounds = mMapProxy.getRegion();
        if (bounds == null) {
            callbackFailed(args, "bounds is null");
            callbackComplete(args);
            return;
        }

        String callbackId = (String) args.get(CALLBACK_KEY_SUCCESS);
        mCallback.onJsMethodCallback(getPageId(), callbackId, convertBoundsToMap(bounds));
        callbackComplete(args);
    }

    private void includePoints(final java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null) {
            callbackFailed(args, "args is null");
            callbackComplete(args);
            return;
        }
        JSONArray jsonArray = null;
        try {
            jsonArray = ((JSONArray) args.get(CALLBACK_KEY_POINTS));
        } catch (ClassCastException e) {
            mCallback.onJsException(e);
            return;
        }
        if (jsonArray == null || jsonArray.length() == 0) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "points must be non-empty array when calling includePoints method."));
            return;
        }
        List<HybridLatLng> points = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = (JSONObject) jsonArray.get(i);
                double latitude = object.getDouble(LATITUDE);
                double longitude = object.getDouble(LONGITUDE);
                String coordType = object.optString(COORDTYPE_CAMEL);
                points.add(new HybridLatLng(latitude, longitude, coordType));
            }
        } catch (JSONException e) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "parsing points error, value: " + jsonArray.toString()));
            return;
        }
        Object paddingJSON = null;
        try {
            paddingJSON = args.get(CALLBACK_KEY_PADDING);
        } catch (ClassCastException e) {
            mCallback.onJsException(e);
        }
        Rect padding = parsePadding(mHapEngine, (String) paddingJSON);

        mMapProxy.setIncludePoints(
                points,
                padding,
                new MapProxy.OnRetCallbackListener() {
                    @Override
                    public void onSuccess() {
                        callbackSuccess(args);
                    }

                    @Override
                    public void onFail(String reason) {
                        callbackFailed(args, reason);
                    }

                    @Override
                    public void onFail(Object... params) {
                    }

                    @Override
                    public void onComplete() {
                        callbackComplete(args);
                    }
                });
    }

    private void getCenterLocation(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null || !args.containsKey(CALLBACK_KEY_SUCCESS)) {
            callbackComplete(args);
            return;
        }
        HybridLatLng latLng = mMapProxy.getCenterLocation();
        if (latLng == null) {
            callbackFailed(args, "latLng is null.");
            callbackComplete(args);
            return;
        }
        java.util.Map<String, Object> map = new HashMap<>(2);
        map.put(LATITUDE, latLng.latitude);
        map.put(LONGITUDE, latLng.longitude);
        String callbackId = (String) args.get(CALLBACK_KEY_SUCCESS);
        mCallback.onJsMethodCallback(getPageId(), callbackId, map);
        callbackComplete(args);
    }

    private void translateMarker(final java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null) {
            callbackFailed(args, "args is null");
            callbackComplete(args);
            return;
        }
        Object markerIdObj = args.get(CALLBACK_KEY_MARKER_ID);
        Object destinationObj = args.get(CALLBACK_KEY_DESTINATION);
        Object autoRotateObj = args.get(CALLBACK_KEY_AUTO_ROTATE);
        Object rotateObj = args.get(CALLBACK_KEY_ROTATE);
        Object durationObj = args.get(CALLBACK_KEY_DURATION);
        Object animationEndObj = args.get(CALLBACK_KEY_ANIMATION_END);

        if (markerIdObj == null || destinationObj == null) {
            mCallback.onJsException(
                    new IllegalArgumentException("markerId and destination must not be empty."));
            return;
        }

        int markerId = (int) markerIdObj;
        HybridLatLng destination;
        try {
            JSONObject destinationJSON = (JSONObject) destinationObj;
            destination =
                    new HybridLatLng(
                            destinationJSON.getDouble(LATITUDE),
                            destinationJSON.getDouble(LONGITUDE));
        } catch (JSONException e) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "destination must have both of latitude and longitude."));
            e.printStackTrace();
            return;
        }

        Boolean autoRotate = autoRotateObj == null ? true : (Boolean) autoRotateObj;
        int rotate = rotateObj == null ? 0 : (int) rotateObj;
        int duration = durationObj == null ? 1000 : (int) durationObj;
        final String animationEndId = (String) animationEndObj;

        mMapProxy.translateMarker(
                markerId,
                destination,
                autoRotate,
                rotate,
                duration,
                new MapProxy.OnAnimationEndListener() {
                    @Override
                    public void onAnimationEnd() {
                        if (!TextUtils.isEmpty(animationEndId)) {
                            mCallback.onJsMethodCallback(getPageId(), animationEndId);
                        }
                    }
                },
                new MapProxy.OnRetCallbackListener() {

                    @Override
                    public void onSuccess() {
                        callbackSuccess(args);
                    }

                    @Override
                    public void onFail(String reason) {
                        callbackFailed(args, reason);
                    }

                    @Override
                    public void onFail(Object... params) {
                    }

                    @Override
                    public void onComplete() {
                        callbackComplete(args);
                    }
                });
    }

    private void getCoordType(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null || !args.containsKey(CALLBACK_KEY_SUCCESS)) {
            callbackComplete(args);
            return;
        }
        String coordType = (String) args.get(COORDTYPE_CAMEL);
        if (!args.containsKey(LONGITUDE) && !args.containsKey(LATITUDE)) {
            // get current center point's coordType.
            callbackCoordType(args, mMapProxy.getCoordType());
        } else {
            try {
                double lng = (double) args.get(LONGITUDE);
                double lat = (double) args.get(LATITUDE);
                String destCoordType;
                // get coordType from the point of map.
                if (TextUtils.isEmpty(coordType)) {
                    destCoordType = mMapProxy.getCoordType(lat, lng);
                } else {
                    // get coordType of designated point.
                    if (!CoordType.isLegal(coordType)) {
                        callbackFailed(args, "coordType illegal.");
                        callbackComplete(args);
                        return;
                    }
                    destCoordType = mMapProxy.getCoordType(lat, lng, coordType);
                }
                callbackCoordType(args, destCoordType);
            } catch (NumberFormatException e) {
                callbackFailed(args, "param illegal.");
                callbackComplete(args);
            }
        }
    }

    private void getSupportedCoordTypes(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null || !args.containsKey(CALLBACK_KEY_SUCCESS)) {
            callbackComplete(args);
            return;
        }
        java.util.Map<String, List<String>> map = new HashMap<>(1);
        List<String> coordTypes = mMapProxy.getSupportedCoordTypes();
        if (coordTypes == null || coordTypes.size() == 0) {
            callbackFailed(args, "coordTypes is empty");
            callbackComplete(args);
            return;
        }
        map.put(CALLBACK_KEY_SUPPORTED_COORDTYPES, coordTypes);
        String callbackId = (String) args.get(CALLBACK_KEY_SUCCESS);
        mCallback.onJsMethodCallback(getPageId(), callbackId, map);
        callbackComplete(args);
    }

    private void callbackCoordType(java.util.Map<String, Object> args, String coordType) {
        if (args.containsKey(CALLBACK_KEY_SUCCESS)) {
            java.util.Map<String, Object> map = new HashMap<>(1);
            map.put(COORDTYPE_CAMEL, coordType);
            String callbackId = (String) args.get(CALLBACK_KEY_SUCCESS);
            mCallback.onJsMethodCallback(getPageId(), callbackId, map);
            callbackComplete(args);
        }
    }

    private void callbackSuccess(java.util.Map<String, Object> args) {
        if (args != null && args.containsKey(CALLBACK_KEY_SUCCESS)) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_SUCCESS));
        }
    }

    private void callbackFailed(java.util.Map<String, Object> args, String reason) {
        if (args != null && args.containsKey(CALLBACK_KEY_FAIL)) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_FAIL), reason);
        }
    }

    private void callbackFailedWithParams(java.util.Map<String, Object> args, Object... params) {
        if (args != null && args.containsKey(CALLBACK_KEY_FAIL)) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_FAIL), params);
        }
    }

    private void callbackComplete(java.util.Map<String, Object> args) {
        if (args != null && args.containsKey(CALLBACK_KEY_COMPLETE)) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_COMPLETE));
        }
    }

    private void setMyLocationStyle(String key, String myLocationStyle) {
        if (mMapProxy == null) {
            return;
        }
        if (null == mMyLocationStyle) {
            mMyLocationStyle = new MapMyLocationStyle();
        }

        try {
            switch (key) {
                case MY_LOCATION_FILL_COLOR:
                    mMyLocationStyle.accuracyCircleFillColor =
                            ColorUtil.getColor(myLocationStyle, DEFAULT_CIRCLE_FILL_COLOR);
                    break;
                case MY_LOCATION_STROKE_COLOR:
                    mMyLocationStyle.accuracyCircleStrokeColor =
                            ColorUtil.getColor(myLocationStyle, DEFAULT_CIRCLE_STROKE_COLOR);
                    break;
                case MY_LOCATION_ICON_PATH:
                    if (!TextUtils.isEmpty(myLocationStyle)) {
                        mMyLocationStyle.iconPath = tryParseUri(myLocationStyle).getPath();
                    }
                    break;
                default:
                    break;
            }
            mMapProxy.setMyLocationStyle(mMyLocationStyle);
        } catch (Exception e) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "parsing mylocation style " + key + " error: " + myLocationStyle));
        }
    }

    private void convertCoord(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null || !args.containsKey(CALLBACK_KEY_SUCCESS)) {
            callbackComplete(args);
            return;
        }
        String from = (String) args.get("from");
        String to = (String) args.get("to");
        if (TextUtils.isEmpty(from)) {
            from = CoordType.GPS;
        }
        if (TextUtils.isEmpty(to)) {
            to = mMapProxy.getCoordType();
        }
        if (!CoordType.isLegalCovertFrom(from) || !CoordType.isLegalCovertTo(to)) {
            callbackFailed(args, "param illegal.");
            callbackComplete(args);
            return;
        }

        try {
            if (args.get(LONGITUDE) == null || args.get(LATITUDE) == null) {
                callbackFailed(args, "parameter is missing.");
                callbackComplete(args);
                return;
            }
            Object longitudeObj = args.get(LONGITUDE);
            Object latitudeObj = args.get(LATITUDE);
            if (longitudeObj == null || latitudeObj == null) {
                callbackFailed(args, "longitude and latitude must be defined.");
                callbackComplete(args);
                return;
            }
            double longitude = Double.parseDouble(longitudeObj.toString());
            double latitude = Double.parseDouble(latitudeObj.toString());
            HybridLatLng latLng = mMapProxy.convertCoordType(from, to, latitude, longitude);
            if (latLng == null) {
                callbackFailed(args, "convert failed.");
            } else {
                java.util.Map<String, Object> map = new HashMap<>(2);
                map.put(LONGITUDE, latLng.longitude);
                map.put(LATITUDE, latLng.latitude);
                String callbackId = (String) args.get(CALLBACK_KEY_SUCCESS);
                mCallback.onJsMethodCallback(getPageId(), callbackId, map);
            }
            callbackComplete(args);
        } catch (NumberFormatException e) {
            callbackFailed(args, "param illegal.");
            callbackComplete(args);
        }
    }

    private void setMarkers(String markers) {
        if (mMapProxy != null) {
            Log.d(TAG, "setMarkers");
            mMapProxy.setMarkers(markers);
        }
    }

    private void setPolylines(String polylines) {
        if (mMapProxy != null) {
            Log.d(TAG, "setPolylines");
            mMapProxy.setPolylines(polylines);
        }
    }

    private void setPolygons(String polygons) {
        if (mMapProxy != null) {
            Log.d(TAG, "setPolygons");
            mMapProxy.setPolygons(polygons);
        }
    }

    private void setCircles(String circles) {
        if (mMapProxy != null) {
            Log.d(TAG, "setCircles");
            mMapProxy.setCircles(circles);
        }
    }

    private void setGroundoverlay(String groundoverlays) {
        if (mMapProxy != null) {
            Log.d(TAG, "setGroundoverlay");
            mMapProxy.setGrounds(groundoverlays);
        }
    }

    private void setHeatmapLayer(String heatMapLayerContent) {
        if (mMapProxy != null) {
            mMapProxy.setHeatmapLayer(heatMapLayerContent, mCallback);
        }
    }

    private void setControls(String controls) {
        if (mMapProxy != null) {
            mMapProxy.setControls(controls);
        }
    }

    private void setIncludePoints(String points) {
        if (TextUtils.isEmpty(points) || mMapProxy == null) {
            return;
        }

        try {
            final List<HybridLatLng> pointsList = new ArrayList<>();
            JSONArray array = new JSONArray(points);
            for (int i = 0; i < array.length(); i++) {
                JSONObject pointJSON = array.getJSONObject(i);
                HybridLatLng point =
                        new HybridLatLng(
                                pointJSON.getDouble(LATITUDE),
                                pointJSON.getDouble(LONGITUDE),
                                pointJSON.optString(COORDTYPE_CAMEL));
                pointsList.add(point);
            }
            if (mHost.getWidth() == 0 || mHost.getHeight() == 0) {
                mHost
                        .getViewTreeObserver()
                        .addOnPreDrawListener(
                                new ViewTreeObserver.OnPreDrawListener() {
                                    @Override
                                    public boolean onPreDraw() {
                                        if (mHost != null) {
                                            mHost.getViewTreeObserver()
                                                    .removeOnPreDrawListener(this);
                                        }
                                        mMapProxy.setIncludePoints(pointsList, null, null);
                                        return true;
                                    }
                                });
            } else {
                mMapProxy.setIncludePoints(pointsList, null, null);
            }
        } catch (JSONException e) {
            mCallback.onJsException(
                    new IllegalArgumentException("parsing points error. points: " + points));
        }
    }

    private void setIndoorEnable(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null) {
            callbackFailed(args, "args is null");
            callbackComplete(args);
            return;
        }
        Object indoorModeObj = args.get(ENABLE);
        Boolean enableIndoorMode = indoorModeObj == null ? false : (Boolean) indoorModeObj;
        mMapProxy.setIndoorEnable(enableIndoorMode);
    }

    private void switchIndoorFloor(final java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null) {
            mCallback.onJsException(new IllegalArgumentException("switchIndoorFloor args is null"));
            return;
        }
        String poiId = (String) args.get(POI_ID);
        String toFloor = (String) args.get(TO_FLOOR);
        mMapProxy.switchIndoorFloor(
                poiId,
                toFloor,
                new MapProxy.OnRetCallbackListener() {
                    @Override
                    public void onSuccess() {
                        callbackSuccess(args);
                    }

                    @Override
                    public void onFail(String reason) {
                    }

                    @Override
                    public void onFail(Object... params) {
                        callbackFailedWithParams(args, params);
                    }

                    @Override
                    public void onComplete() {
                        callbackComplete(args);
                    }
                });
    }

    private void setMapComponentPosition(
            final String mapComponent, final java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null) {
            mCallback.onJsException(new IllegalArgumentException("args is null."));
            return;
        }
        if (!args.containsKey(X) || !args.containsKey(Y)) {
            mCallback.onJsException(
                    new IllegalArgumentException("args must contain x and y. args: " + args));
            return;
        }
        Object objX = args.get(X);
        Object objY = args.get(Y);
        if (objX == null || objY == null) {
            mCallback.onJsException(
                    new IllegalArgumentException("x and y must be defined. args: " + args));
            return;
        }
        try {
            int x = Attributes.getInt(mHapEngine, objX.toString(), -1);
            int y = Attributes.getInt(mHapEngine, objY.toString(), -1);
            if (x < 0 || y < 0) {
                mCallback.onJsException(
                        new IllegalArgumentException("x and y must be larger than 0"));
                return;
            }
            mMapProxy.setMapComponentPosition(mapComponent, new Point(x, y));
        } catch (NumberFormatException e) {
            Log.e(TAG, "setMapComponentPosition: ", e);
            mCallback.onJsException(e);
        }
    }

    private void setMaxAndMinScaleLevel(java.util.Map<String, Object> args) {
        if (mMapProxy == null) {
            return;
        }
        if (args == null) {
            mCallback.onJsException(new IllegalArgumentException("args is null."));
            return;
        }
        if (!args.containsKey(MAX_LEVEL) || !args.containsKey(MIN_LEVEL)) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "args must contain maxLevel and minLevel. args: " + args));
            return;
        }
        Object maxObj = args.get(MAX_LEVEL);
        Object minObj = args.get(MIN_LEVEL);
        if (maxObj == null || minObj == null) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "maxLevel and minLevel must be defined. args: " + args));
            return;
        }
        try {
            float maxLevel = Float.parseFloat(maxObj.toString());
            float minLevel = Float.parseFloat(minObj.toString());
            mMapProxy.setMaxAndMinScaleLevel(maxLevel, minLevel);
        } catch (NumberFormatException e) {
            Log.e(TAG, "setMaxAndMinScaleLevel: ", e);
            mCallback.onJsException(e);
        }
    }

    private java.util.Map<String, Object> convertBoundsToMap(HybridLatLngBounds bounds) {
        java.util.Map<String, Object> southwest = new HashMap<>(3);
        southwest.put(LATITUDE, bounds.southwest.latitude);
        southwest.put(LONGITUDE, bounds.southwest.longitude);
        southwest.put(
                COORDTYPE_CAMEL,
                mMapProxy.getCoordType(bounds.southwest.latitude, bounds.southwest.longitude));

        java.util.Map<String, Object> northeast = new HashMap<>(3);
        northeast.put(LATITUDE, bounds.northeast.latitude);
        northeast.put(LONGITUDE, bounds.northeast.longitude);
        northeast.put(
                COORDTYPE_CAMEL,
                mMapProxy.getCoordType(bounds.northeast.latitude, bounds.northeast.longitude));

        java.util.Map<String, Object> map = new HashMap<>(2);
        map.put(CALLBACK_KEY_SOUTHWEST, southwest);
        map.put(CALLBACK_KEY_NORTHEAST, northeast);

        return map;
    }
}
