/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import static org.hapjs.bridge.Response.CODE_GENERIC_ERROR;
import static org.hapjs.common.utils.DisplayUtil.getStatusBarHeight;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hapjs.LauncherActivity;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.location.DefaultLocationClient;
import org.hapjs.common.location.HapLocation;
import org.hapjs.common.location.ILocationClient;
import org.hapjs.common.location.LocationChangedListener;
import org.hapjs.common.location.LocationProvider;
import org.hapjs.common.location.LocationUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.features.geolocation.GeolocationProvider;
import org.hapjs.features.geolocation.GeolocationProviderImpl;
import org.hapjs.features.geolocation.NavigationInfo;
import org.hapjs.features.geolocation.adapter.LocationAdapter;
import org.hapjs.features.geolocation.adapter.LocationSearchAdapter;
import org.hapjs.features.geolocation.rgc.GeocodeProvider;
import org.hapjs.render.Display;
import org.hapjs.render.RootView;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.widgets.map.CoordType;
import org.hapjs.widgets.map.MapProvider;
import org.hapjs.widgets.map.MapProxy;
import org.hapjs.widgets.map.model.HybridLatLng;
import org.hapjs.widgets.map.model.LocationInfo;
import org.hapjs.widgets.map.model.MapFrameLayout;
import org.hapjs.widgets.map.model.MapMarker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A hybrid feature to get location information
 *
 * <table>
 * <tr>
 * <th>Action name</th> <th>Action description</th> <th>Invocation mode</th>
 * <th>Request parameter name</th> <th>Request parameter description</th>
 * <th>Response parameter name</th> <th>Response parameter description</th>
 * </tr>
 *
 * <tr>
 * <td>enableListener</td> <td>get current location with following update for location change</td>
 * <td>CALLBACK</td> <td></td> <td></td>
 * <td>code</td> <td>error code if failed</td>
 * </tr>
 *
 * <tr>
 * <td>get</td> <td>get current location without following update</td>
 * <td>SYNC</td> <td></td> <td></td>
 * <td>message</td> <td>error message if failed</td>
 * </tr>
 *
 * <tr>
 * <td>disableListener</td> <td>remove any further update for location change</td>
 * <td>CALLBACK</td> <td></td> <td></td> <td></td> <td></td>
 * </tr>
 *
 * </table>
 *
 * @hide
 */
@FeatureExtensionAnnotation(
        name = Geolocation.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.RESIDENT_NORMAL,
        actions = {
                @ActionAnnotation(
                        name = Geolocation.ACTION_GET_LOCATION,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.ACCESS_FINE_LOCATION}),
                @ActionAnnotation(
                        name = Geolocation.ACTION_OPEN_LOCATION,
                        mode = FeatureExtension.Mode.CALLBACK,
                        permissions = {Manifest.permission.ACCESS_FINE_LOCATION}),
                @ActionAnnotation(
                        name = Geolocation.ACTION_CHOOSE_LOCATION,
                        mode = FeatureExtension.Mode.CALLBACK,
                        permissions = {Manifest.permission.ACCESS_FINE_LOCATION}),
                @ActionAnnotation(
                        name = Geolocation.ACTION_GET_LOCATION_TYPE,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.ACCESS_FINE_LOCATION}),
                @ActionAnnotation(
                        name = Geolocation.ACTION_SUBSCRIBE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        permissions = {Manifest.permission.ACCESS_FINE_LOCATION}),
                @ActionAnnotation(name = Geolocation.ACTION_UNSUBSCRIBE, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = Geolocation.ACTION_GET_SUPPORTED_COORD_TYPES,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = Geolocation.ACTION_GEOCODE_QUERY,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Geolocation.ACTION_REVERSE_GEOCODE_QUERY,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class Geolocation extends CallbackHybridFeature {
    public static final String COORD_TYPE = "coordType";
    public static final String TYPE_WGS84 = CoordType.WGS84;
    public static final String TYPE_GCJ02 = CoordType.GCJ02;
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String PACKAGE_NAME_BAIDUMAP = "com.baidu.BaiduMap";
    public static final String PACKAGE_NAME_AMAP = "com.autonavi.minimap";
    public static final String PACKAGE_NAME_QQMAP = "com.tencent.map";
    public static final int ERROR_OPEN_MAP_APP_FAIL = Response.CODE_FEATURE_ERROR;
    public static final int ERROR_MAP_APP_NOT_INSTALL = Response.CODE_FEATURE_ERROR + 1;
    protected static final String FEATURE_NAME = "system.geolocation";
    protected static final String ACTION_GET_LOCATION = "getLocation";
    protected static final String ACTION_OPEN_LOCATION = "openLocation";
    protected static final String ACTION_CHOOSE_LOCATION = "chooseLocation";
    protected static final String ACTION_GET_LOCATION_TYPE = "getLocationType";
    protected static final String ACTION_SUBSCRIBE = "subscribe";
    protected static final String ACTION_UNSUBSCRIBE = "unsubscribe";
    protected static final String ACTION_GET_SUPPORTED_COORD_TYPES = "getSupportedCoordTypes";
    protected static final String ACTION_GEOCODE_QUERY = "geocodeQuery";
    protected static final String ACTION_REVERSE_GEOCODE_QUERY = "reverseGeocodeQuery";
    protected static final String PARAM_TIMEOUT = "timeout";
    protected static final String PARAM_COORTYPE = "coorType";
    protected static final String PARAM_COORDTYPE = "coordType";
    protected static final long PARAM_TIMEOUT_DEFAULT = 30000; // 30000ms
    protected static final String RESULT_LATITUDE = "latitude";
    protected static final String RESULT_LONGITUDE = "longitude";
    protected static final String RESULT_ACCURACY = "accuracy";
    protected static final String RESULT_TIME = "time";
    protected static final String RESULT_TYPES = "types";
    protected static final String ALTITUDE = "altitude";
    protected static final String SPEED = "speed";
    protected static final String ACCURACY = "accuracy";
    protected static final String VERTICAL_ACCURACY = "verticalAccuracy";
    protected static final String HORIZONTAL_ACCURACY = "horizontalAccuracy";
    protected static final String SCALE = "scale";
    protected static final String NAME = "name";
    protected static final String ADDRESS = "address";
    private static final String TAG = "Geolocation";
    private static final int CODE_RESULT_RECEIVED = 1;
    private static final int CODE_TIMEOUT = 2;
    private static final int CODE_UNAVAILABLE = 3;
    private static final int CODE_CLOSE = 4;
    private static final int ERROR_SERVICE_CLOSE = Response.CODE_FEATURE_ERROR;
    private static final int ERROR_ILLEGAL_ARGUMENT = Response.CODE_ILLEGAL_ARGUMENT;
    private static final int ERROR_CHOOSE_LOCATION_INVALID = Response.CODE_FEATURE_ERROR;
    private final Object mGeocodeProviderLock = new Object();
    private Handler handler = new Handler(Looper.getMainLooper());
    private LocationProvider mProvider;
    private Request mChooseRequest;
    private Request mOpenRequest;
    private RootView.OnBackPressedFeatureListener onOpenLocationBackPressedFeatureListener;
    private RootView.OnBackPressedFeatureListener onChooseLocationBackPressedFeatureListener;
    private MapProxy mMapProxy;
    private RootView openLocationRootView;
    private FrameLayout openLocationMapLayout;
    private View openLocationView;
    private RootView chooseLocationRootView;
    private View chooseLocationView;
    private LocationInfo curLocInfo;
    private RecyclerView recyclerviewNearBy;
    private List<LocationInfo> datasNearBy;
    private LocationAdapter locationAdapterNearBy;
    private RecyclerView recyclerviewSearch;
    private List<LocationInfo> datasSearch;
    private LocationSearchAdapter locationAdapterSearch;
    private Runnable mTimeoutRunnable;
    private volatile boolean mIsCallbackRet = false;
    // RGC 相关功能Provider
    private GeocodeProvider mGeocodeProvider;

    public static MapFrameLayout getNoMapView(Context context) {
        MapFrameLayout layout = new MapFrameLayout(context);
        layout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView view = new TextView(context);
        view.setText(R.string.no_map);
        view.setGravity(Gravity.CENTER);
        layout.addView(view);

        return layout;
    }

    /**
     * Invoke the action.
     *
     * @param request invocation request with action of update, get or remove.
     * @return invocation response.
     */
    @Override
    public Response invokeInner(Request request) throws JSONException {
        if (mProvider == null) {
            mProvider = ProviderManager.getDefault().getProvider(LocationProvider.NAME);
        }

        String action = request.getAction();
        switch (action) {
            case ACTION_GET_LOCATION:
                return getLocation(request);
            case ACTION_OPEN_LOCATION:
                openLocation(request);
                return Response.SUCCESS;
            case ACTION_CHOOSE_LOCATION:
                chooseLocation(request);
                return Response.SUCCESS;
            case ACTION_GET_LOCATION_TYPE:
                return getLocationType(request);
            case ACTION_UNSUBSCRIBE:
                return unsubscribe(request);
            case ACTION_GET_SUPPORTED_COORD_TYPES:
                return getSupportedCoordTypes(request);
            case ACTION_GEOCODE_QUERY:
                geocodeQuery(request);
                return null;
            case ACTION_REVERSE_GEOCODE_QUERY:
                reverseGeocodeQuery(request);
                return null;
            default:
                return subscribe(request);
        }
    }

    private void geocodeQuery(Request request) {
        synchronized (mGeocodeProviderLock) {
            if (mGeocodeProvider != null) {
                mGeocodeProvider.geocodeQuery(request);
            } else {
                mGeocodeProvider = ProviderManager.getDefault().getProvider(GeocodeProvider.NAME);
                if (mGeocodeProvider != null) {
                    mGeocodeProvider.geocodeQuery(request);
                } else {
                    request.getCallback()
                            .callback(new Response(CODE_GENERIC_ERROR, "Not supported"));
                }
            }
        }
    }

    private void reverseGeocodeQuery(Request request) {
        synchronized (mGeocodeProviderLock) {
            if (mGeocodeProvider != null) {
                mGeocodeProvider.reverseGeocodeQuery(request);
            } else {
                mGeocodeProvider = ProviderManager.getDefault().getProvider(GeocodeProvider.NAME);
                if (mGeocodeProvider != null) {
                    mGeocodeProvider.reverseGeocodeQuery(request);
                } else {
                    request.getCallback()
                            .callback(new Response(CODE_GENERIC_ERROR, "Not supported"));
                }
            }
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (mTimeoutRunnable != null) {
            handler.removeCallbacks(mTimeoutRunnable);
            mTimeoutRunnable = null;
        }
        if (mGeocodeProvider != null) {
            mGeocodeProvider.releaseGeocode();
        }
    }

    private Response getLocation(Request request) throws JSONException {
        final Handler handler = new Handler(Looper.getMainLooper());
        notifyFeatureStatus(handler, request, Display.DISPLAY_LOCATION_START);
        long timeout = PARAM_TIMEOUT_DEFAULT;
        String rawParams = request.getRawParams();
        if (rawParams != null && !rawParams.isEmpty()) {
            JSONObject params = new JSONObject(rawParams);
            timeout = params.optLong(PARAM_TIMEOUT, PARAM_TIMEOUT_DEFAULT);
        }

        ILocationClient client = chooseClient(request);

        if (client == null) {
            notifyFeatureStatus(handler, request, Display.DISPLAY_STATUS_FINISH);
            request.getCallback().callback(responseLocation(CODE_UNAVAILABLE, null));
            return Response.SUCCESS;
        }

        mIsCallbackRet = false;
        final SubscribeCallbackContext subscribeCallbackContext =
                new SubscribeCallbackContext(request, ACTION_GET_LOCATION, client, true, true) {
                    @Override
                    public void callback(int what, Object obj) {
                        super.callback(what, obj);
                        mIsCallbackRet = true;
                        // Bellow will run on UI thread
                        notifyFeatureStatus(handler, request, Display.DISPLAY_STATUS_FINISH);
                        removeCallbackContext(ACTION_GET_LOCATION);
                        handler.removeCallbacks(mTimeoutRunnable);
                        mTimeoutRunnable = null;
                    }

                    @Override
                    public void onDestroy() {
                        super.onDestroy();
                        if (!mIsCallbackRet) {
                            notifyFeatureStatus(handler, request, Display.DISPLAY_STATUS_FINISH);
                        }
                        handler.removeCallbacks(mTimeoutRunnable);
                        mTimeoutRunnable = null;
                    }
                };

        if (mTimeoutRunnable != null) {
            handler.removeCallbacks(mTimeoutRunnable);
        }
        mTimeoutRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        // Bellow will run on UI thread
                        if (!mIsCallbackRet) {
                            notifyFeatureStatus(handler, request, Display.DISPLAY_STATUS_FINISH);
                        }
                        runCallbackContext(ACTION_GET_LOCATION, CODE_TIMEOUT, null);
                        mTimeoutRunnable = null;
                    }
                };
        putCallbackContext(subscribeCallbackContext);
        // Wait result as long as timeout
        handler.postDelayed(mTimeoutRunnable, timeout);
        return Response.SUCCESS;
    }

    private ILocationClient chooseClient(Request request) throws JSONException {
        String coordType = LocationProvider.COORTYPE_WGS84;
        JSONObject params = request.getJSONParams();
        if (params != null) {
            coordType = params.optString(PARAM_COORDTYPE);
            if (TextUtils.isEmpty(coordType)) {
                coordType = params.optString(PARAM_COORTYPE, LocationProvider.COORTYPE_WGS84);
            }
        }

        ILocationClient client = null;
        if (mProvider != null) {
            client = mProvider.createLocationClient(Runtime.getInstance().getContext(), coordType);
        }

        if (client == null) {
            client = new DefaultLocationClient(Runtime.getInstance().getContext());
        }
        return client;
    }

    private void openLocation(final Request request) {
        final Activity activity = request.getNativeInterface().getActivity();
        mOpenRequest = request;

        if (openLocationView != null && openLocationView.getVisibility() == View.VISIBLE) {
            return;
        }

        if (activity == null) {
            return;
        }

        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        FrameLayout.LayoutParams lp =
                                new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT);
                        openLocationView =
                                mOpenRequest
                                        .getNativeInterface()
                                        .getActivity()
                                        .getLayoutInflater()
                                        .inflate(R.layout.open_location_layout, null);
                        openLocationMapLayout =
                                openLocationView.findViewById(R.id.fl_open_location_map_view);
                        MapFrameLayout mapFrameLayout = createMapView(activity);
                        openLocationMapLayout.addView(mapFrameLayout, lp);
                        openLocationRootView = mOpenRequest.getNativeInterface().getRootView();
                        openLocationRootView.addView(openLocationView, lp);

                        View moveToLocation =
                                openLocationView
                                        .findViewById(R.id.img_open_location_move_to_location);
                        View navigateToDestination =
                                openLocationView.findViewById(R.id.img_navigation_destination);
                        TextView tvName = openLocationView.findViewById(R.id.tv_address_name);
                        TextView tvAddress = openLocationView.findViewById(R.id.tv_address_detail);

                        View statusBar =
                                openLocationView.findViewById(R.id.open_location_status_bar);
                        statusBar.setLayoutParams(
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        getStatusBarHeight(activity)));
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            statusBar.setBackgroundColor(Color.argb(60, 0, 0, 0));
                        }

                        onOpenLocationBackPressedFeatureListener =
                                new RootView.OnBackPressedFeatureListener() {
                                    @Override
                                    public boolean onBackPress() {
                                        if (mMapProxy != null) {
                                            mMapProxy.onActivityDestroy();
                                            mMapProxy = null;
                                        }
                                        openLocationView.setVisibility(View.GONE);
                                        handler.postDelayed(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (openLocationRootView != null
                                                                && openLocationView != null) {
                                                            openLocationRootView
                                                                    .removeView(openLocationView);
                                                            openLocationRootView = null;
                                                            openLocationView = null;
                                                        }
                                                    }
                                                },
                                                300);
                                        mOpenRequest
                                                .getNativeInterface()
                                                .getRootView()
                                                .removeOnBackPressedFeatureListener(
                                                        onOpenLocationBackPressedFeatureListener);
                                        onOpenLocationBackPressedFeatureListener = null;
                                        mOpenRequest = null;
                                        return true;
                                    }
                                };
                        mOpenRequest
                                .getNativeInterface()
                                .getRootView()
                                .addOnBackPressedFeatureListener(
                                        onOpenLocationBackPressedFeatureListener);

                        double latitude = Double.NaN;
                        double longitude = Double.NaN;
                        String coordType = TYPE_WGS84;
                        double scale = 18d;
                        String name = "";
                        String address = "";
                        String rawParams = mOpenRequest.getRawParams();

                        ImageView goBack = openLocationView.findViewById(R.id.img_back);

                        goBack.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (mMapProxy != null) {
                                            mMapProxy.onActivityDestroy();
                                            mMapProxy = null;
                                        }
                                        openLocationView.setVisibility(View.GONE);
                                        handler.postDelayed(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (openLocationRootView != null
                                                                && openLocationView != null) {
                                                            openLocationRootView
                                                                    .removeView(openLocationView);
                                                            openLocationRootView = null;
                                                            openLocationView = null;
                                                        }
                                                    }
                                                },
                                                300);
                                        mOpenRequest
                                                .getNativeInterface()
                                                .getRootView()
                                                .removeOnBackPressedFeatureListener(
                                                        onOpenLocationBackPressedFeatureListener);
                                        onOpenLocationBackPressedFeatureListener = null;
                                        mOpenRequest = null;
                                    }
                                });

                        if (rawParams != null && !rawParams.isEmpty()) {
                            try {
                                JSONObject jsonParams = new JSONObject(rawParams);
                                for (Iterator keys = jsonParams.keys(); keys.hasNext(); ) {
                                    String key = ((String) keys.next()).intern();
                                    switch (key) {
                                        case LATITUDE:
                                            latitude = jsonParams.optDouble(LATITUDE);
                                            if (latitude < -90d || latitude > 90d) {
                                                latitude = Double.NaN;
                                            }
                                            break;
                                        case LONGITUDE:
                                            longitude = jsonParams.optDouble(LONGITUDE);
                                            if (longitude < -180d || longitude > 180d) {
                                                longitude = Double.NaN;
                                            }
                                            break;
                                        case COORD_TYPE:
                                            String tmp =
                                                    jsonParams.optString(COORD_TYPE, TYPE_WGS84);
                                            if (TYPE_GCJ02.equals(tmp) || TYPE_WGS84.equals(tmp)) {
                                                coordType = tmp;
                                            } else {
                                                Log.e(
                                                        TAG,
                                                        "openLocation: coordType is illegal, coordType:"
                                                                + coordType
                                                                + ", set default value wgs84");
                                                coordType = TYPE_WGS84;
                                            }
                                            break;
                                        case SCALE:
                                            scale = jsonParams.optDouble(SCALE);
                                            if (Double.isNaN(scale) || scale < 5d || scale > 18d) {
                                                Log.e(
                                                        TAG,
                                                        "openLocation: scale is illegal,"
                                                                + scale
                                                                +
                                                                " not between 5d and 18d, set default value 18d.");
                                                scale = 18d;
                                            }
                                            break;
                                        case NAME:
                                            name = jsonParams.optString(NAME);
                                            break;
                                        case ADDRESS:
                                            address = jsonParams.optString(ADDRESS);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                                    mOpenRequest
                                            .getCallback()
                                            .callback(
                                                    new Response(
                                                            ERROR_ILLEGAL_ARGUMENT,
                                                            "latitude or longitude is illegal."));
                                }
                                if (mMapProxy != null && !Double.isNaN(latitude)
                                        && !Double.isNaN(longitude)) {
                                    mMapProxy.setShowMyLocation(true, false);
                                    mMapProxy.updateLatLng(latitude, longitude, coordType);
                                    mMapProxy.setScale((float) scale);
                                    mMapProxy.removeGeolocationMarkers();

                                    List<MapMarker> markerList = new ArrayList<>();
                                    MapMarker marker = new MapMarker();
                                    marker.latitude = latitude;
                                    marker.longitude = longitude;
                                    marker.coordType = coordType;
                                    marker.geoMarkerType = MapMarker.GEOLOCATION_MARKER_TYPE_TARGET;
                                    markerList.add(marker);
                                    mMapProxy.setGeolocationMarkers(markerList);

                                    mOpenRequest
                                            .getCallback()
                                            .callback(new Response(Response.CODE_SUCCESS,
                                                    "open location success."));
                                }
                                tvName.setText(name);
                                tvAddress.setText(address);
                                final NavigationInfo navigationInfo =
                                        new NavigationInfo(latitude, longitude, scale, name,
                                                address);
                                navigateToDestination.setOnClickListener(
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                GeolocationProvider geolocationProvider =
                                                        ProviderManager.getDefault().getProvider(
                                                                GeolocationProvider.NAME);
                                                if (geolocationProvider != null) {
                                                    geolocationProvider.onNavigateButtonClick(
                                                            activity, navigationInfo, mOpenRequest);
                                                } else {
                                                    geolocationProvider =
                                                            new GeolocationProviderImpl();
                                                    geolocationProvider.onNavigateButtonClick(
                                                            activity, navigationInfo, mOpenRequest);
                                                }
                                            }
                                        });
                            } catch (JSONException e) {
                                Log.e(TAG, "error " + e);
                            }
                        }

                        moveToLocation.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (mMapProxy != null) {
                                            mMapProxy.moveToMyLocation();
                                        }
                                    }
                                });
                    }
                });
    }

    public MapFrameLayout createMapView(Activity activity) {
        MapProvider mapProvider = ProviderManager.getDefault().getProvider(MapProvider.NAME);
        if (mapProvider == null) {
            return getNoMapView(activity);
        }
        if (!(activity instanceof LauncherActivity)) {
            return null;
        }
        HybridManager manager = ((LauncherActivity) activity).getHybridView().getHybridManager();
        mMapProxy = mapProvider.createMapProxy(manager);
        if (mMapProxy == null) {
            return getNoMapView(activity);
        }

        MapFrameLayout layout = (MapFrameLayout) mMapProxy.getMapView();
        return layout;
    }

    private void chooseLocation(final Request request) {
        final Activity activity = request.getNativeInterface().getActivity();
        mChooseRequest = request;
        if (chooseLocationView != null && chooseLocationView.getVisibility() == View.VISIBLE) {
            return;
        }

        if (activity == null) {
            return;
        }

        ThreadUtils.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        FrameLayout.LayoutParams lp =
                                new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT);
                        chooseLocationView =
                                mChooseRequest
                                        .getNativeInterface()
                                        .getActivity()
                                        .getLayoutInflater()
                                        .inflate(R.layout.choose_location_layout, null);
                        FrameLayout mapView =
                                chooseLocationView.findViewById(R.id.fl_choose_location_map_view);
                        MapFrameLayout mapFrameLayout = createMapView(activity);
                        mapView.addView(mapFrameLayout, lp);
                        chooseLocationRootView = mChooseRequest.getNativeInterface().getRootView();
                        chooseLocationRootView.addView(chooseLocationView, lp);

                        curLocInfo = new LocationInfo();

                        View statusBar =
                                chooseLocationView.findViewById(R.id.choose_location_status_bar);
                        ImageView goBack = chooseLocationView.findViewById(R.id.img_back);
                        View chooseTitle =
                                chooseLocationView.findViewById(R.id.tv_choose_location_title);
                        ImageView search = chooseLocationView.findViewById(R.id.img_choose_search);
                        Button chooseDone = chooseLocationView.findViewById(R.id.bt_choose_done);
                        LinearLayout llMapViewList =
                                chooseLocationView.findViewById(R.id.ll_map_view_list);

                        EditText searchEdit = chooseLocationView.findViewById(R.id.et_search_input);
                        ImageView inputCancel =
                                chooseLocationView.findViewById(R.id.img_input_cancel);

                        View moveToLocation =
                                chooseLocationView
                                        .findViewById(R.id.img_choose_location_move_to_location);
                        ProgressBar locationLoading =
                                chooseLocationView.findViewById(R.id.img_choose_location_loading);

                        recyclerviewNearBy = chooseLocationView.findViewById(R.id.rv_list_poi);

                        statusBar.setLayoutParams(
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        getStatusBarHeight(activity)));
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            statusBar.setBackgroundColor(Color.argb(60, 0, 0, 0));
                        }

                        onChooseLocationBackPressedFeatureListener =
                                new RootView.OnBackPressedFeatureListener() {
                                    @Override
                                    public boolean onBackPress() {
                                        if (llMapViewList.getVisibility() == View.VISIBLE) {
                                            // 退出导航页面，删除View，并且销毁资源
                                            if (mMapProxy != null) {
                                                mMapProxy.onActivityDestroy();
                                                mMapProxy = null;
                                            }
                                            chooseLocationView.setVisibility(View.GONE);
                                            handler.postDelayed(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (chooseLocationRootView != null
                                                                    && chooseLocationView != null) {
                                                                chooseLocationRootView.removeView(
                                                                        chooseLocationView);
                                                                chooseLocationRootView = null;
                                                                chooseLocationView = null;
                                                            }
                                                        }
                                                    },
                                                    300);
                                            mChooseRequest
                                                    .getNativeInterface()
                                                    .getRootView()
                                                    .removeOnBackPressedFeatureListener(
                                                            onChooseLocationBackPressedFeatureListener);
                                            onChooseLocationBackPressedFeatureListener = null;
                                            mChooseRequest = null;
                                            return true;
                                        } else {
                                            // 隐藏非相关内容
                                            chooseTitle.setVisibility(View.VISIBLE);
                                            search.setVisibility(View.VISIBLE);
                                            chooseDone.setVisibility(View.VISIBLE);
                                            llMapViewList.setVisibility(View.VISIBLE);

                                            // 展示相关内容
                                            searchEdit.setText("");
                                            searchEdit.setVisibility(View.GONE);
                                            inputCancel.setVisibility(View.GONE);
                                            recyclerviewSearch.setVisibility(View.GONE);
                                            return true;
                                        }
                                    }
                                };
                        mChooseRequest
                                .getNativeInterface()
                                .getRootView()
                                .addOnBackPressedFeatureListener(
                                        onChooseLocationBackPressedFeatureListener);

                        double latitude = Double.NaN;
                        double longitude = Double.NaN;
                        String coordType = TYPE_WGS84;
                        String rawParams = mChooseRequest.getRawParams();

                        if (rawParams != null && !rawParams.isEmpty()) {
                            try {
                                JSONObject jsonParams = new JSONObject(rawParams);
                                for (Iterator keys = jsonParams.keys(); keys.hasNext(); ) {
                                    String key = ((String) keys.next()).intern();
                                    switch (key) {
                                        case LATITUDE:
                                            latitude = jsonParams.optDouble(LATITUDE);
                                            if (latitude < -90d || latitude > 90d) {
                                                latitude = Double.NaN;
                                            }
                                            break;
                                        case LONGITUDE:
                                            longitude = jsonParams.optDouble(LONGITUDE);
                                            if (longitude < -180d || longitude > 180d) {
                                                longitude = Double.NaN;
                                            }
                                            break;
                                        case COORD_TYPE:
                                            String tmp =
                                                    jsonParams.optString(COORD_TYPE, TYPE_WGS84);
                                            if (TYPE_GCJ02.equals(tmp) || TYPE_WGS84.equals(tmp)) {
                                                coordType = tmp;
                                            } else {
                                                Log.e(
                                                        TAG,
                                                        "chooseLocation: coordType is illegal, coordType:"
                                                                + coordType
                                                                + ", set default value wgs84");
                                                coordType = TYPE_WGS84;
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "error ", e);
                            }
                        }

                        recyclerviewNearBy.setLayoutManager(new GridLayoutManager(activity, 1));
                        recyclerviewNearBy.addItemDecoration(
                                new DividerItemDecoration(activity,
                                        DividerItemDecoration.VERTICAL));
                        datasNearBy = new ArrayList<>();
                        locationAdapterNearBy = new LocationAdapter(activity, datasNearBy);
                        locationAdapterNearBy.setClickListener(
                                new LocationAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClicked(int position, LocationInfo info) {
                                        if (mMapProxy != null) {
                                            curLocInfo.latitude = info.latitude;
                                            curLocInfo.longitude = info.longitude;
                                            curLocInfo.coordType = info.coordType;
                                            curLocInfo.name = info.name;
                                            curLocInfo.address = info.address;
                                            curLocInfo.city = info.city;
                                            mMapProxy.updateLatLng(info.latitude, info.longitude,
                                                    null);
                                            locationAdapterNearBy
                                                    .setSelectSearchItemIndex(position);
                                            locationAdapterNearBy.notifyDataSetChanged();
                                        }
                                    }
                                });
                        recyclerviewNearBy.setAdapter(locationAdapterNearBy);

                        recyclerviewSearch =
                                chooseLocationView.findViewById(R.id.rv_list_search_result);
                        recyclerviewSearch.setLayoutManager(new GridLayoutManager(activity, 1));
                        recyclerviewSearch.addItemDecoration(
                                new DividerItemDecoration(activity,
                                        DividerItemDecoration.VERTICAL));
                        datasSearch = new ArrayList<>();
                        locationAdapterSearch = new LocationSearchAdapter(activity, datasSearch);
                        locationAdapterSearch.setClickListener(
                                new LocationSearchAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClicked(int position, LocationInfo info) {
                                        // 关闭搜索view，跳转到地图view
                                        if (mMapProxy != null) {
                                            curLocInfo.latitude = info.latitude;
                                            curLocInfo.longitude = info.longitude;
                                            curLocInfo.coordType = info.coordType;
                                            curLocInfo.name = info.name;
                                            curLocInfo.address = info.address;
                                            curLocInfo.city = info.city;

                                            // 隐藏非相关内容
                                            chooseTitle.setVisibility(View.VISIBLE);
                                            search.setVisibility(View.VISIBLE);
                                            chooseDone.setVisibility(View.VISIBLE);
                                            llMapViewList.setVisibility(View.VISIBLE);

                                            // 展示相关内容
                                            searchEdit.setText("");
                                            searchEdit.setVisibility(View.GONE);
                                            inputCancel.setVisibility(View.GONE);
                                            recyclerviewSearch.setVisibility(View.GONE);

                                            datasSearch.clear();
                                            locationAdapterSearch.notifyDataSetChanged();

                                            mMapProxy.updateLatLng(info.latitude, info.longitude,
                                                    null);

                                            HybridLatLng latLngCur =
                                                    new HybridLatLng(info.latitude, info.longitude);
                                            recyclerviewNearBy.setVisibility(View.GONE);
                                            locationLoading.setVisibility(View.VISIBLE);
                                            mMapProxy.reverseGeoCodeResult(
                                                    latLngCur,
                                                    new MapProxy.OnLocGeoCoderResultListener() {
                                                        @Override
                                                        public void onGeoCoderResultListener(
                                                                List<LocationInfo> datas) {
                                                            if (datas != null && datas.size() > 0) {
                                                                curLocInfo.city = datas.get(0).city;
                                                                recyclerviewNearBy.setVisibility(
                                                                        View.VISIBLE);
                                                                locationLoading
                                                                        .setVisibility(View.GONE);
                                                                datasNearBy.clear();
                                                                datasNearBy.addAll(datas);
                                                                locationAdapterNearBy
                                                                        .setSelectSearchItemIndex(
                                                                                0);
                                                                locationAdapterNearBy
                                                                        .notifyDataSetChanged();
                                                                recyclerviewNearBy
                                                                        .scrollToPosition(0);
                                                            } else {
                                                                datasNearBy.clear();
                                                                locationAdapterNearBy
                                                                        .notifyDataSetChanged();
                                                                recyclerviewNearBy
                                                                        .setVisibility(View.GONE);
                                                                locationLoading
                                                                        .setVisibility(View.GONE);
                                                            }
                                                        }
                                                    });

                                            // 隐藏软键盘
                                            InputMethodManager imm =
                                                    (InputMethodManager)
                                                            activity.getSystemService(
                                                                    Context.INPUT_METHOD_SERVICE);
                                            View currentFocusView = activity.getCurrentFocus();
                                            if (imm != null && currentFocusView != null) {
                                                imm.hideSoftInputFromWindow(
                                                        currentFocusView.getWindowToken(), 0);
                                            }
                                        }
                                    }
                                });
                        recyclerviewSearch.setAdapter(locationAdapterSearch);

                        chooseDone.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // 返回选择的坐标，成功或者失败
                                        if (curLocInfo.isValid()) {
                                            try {
                                                JSONObject jsonObject = new JSONObject();
                                                jsonObject.put(NAME, curLocInfo.name);
                                                jsonObject.put(ADDRESS, curLocInfo.address);
                                                jsonObject.put(COORD_TYPE, curLocInfo.coordType);
                                                jsonObject.put(LATITUDE, curLocInfo.latitude);
                                                jsonObject.put(LONGITUDE, curLocInfo.longitude);
                                                mChooseRequest.getCallback()
                                                        .callback(new Response(jsonObject));
                                            } catch (JSONException e) {
                                                Log.e(TAG, "choose location failed" + e);
                                                mChooseRequest
                                                        .getCallback()
                                                        .callback(
                                                                new Response(
                                                                        ERROR_CHOOSE_LOCATION_INVALID,
                                                                        "choose location is invalid"));
                                            }
                                        } else {
                                            mChooseRequest
                                                    .getCallback()
                                                    .callback(
                                                            new Response(
                                                                    ERROR_CHOOSE_LOCATION_INVALID,
                                                                    "choose location is invalid"));
                                        }

                                        if (mMapProxy != null) {
                                            mMapProxy.onActivityDestroy();
                                            mMapProxy = null;
                                        }
                                        chooseLocationView.setVisibility(View.GONE);
                                        handler.postDelayed(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (chooseLocationRootView != null
                                                                && chooseLocationView != null) {
                                                            chooseLocationRootView
                                                                    .removeView(chooseLocationView);
                                                            chooseLocationRootView = null;
                                                            chooseLocationView = null;
                                                        }
                                                    }
                                                },
                                                300);
                                        mChooseRequest
                                                .getNativeInterface()
                                                .getRootView()
                                                .removeOnBackPressedFeatureListener(
                                                        onChooseLocationBackPressedFeatureListener);
                                        onChooseLocationBackPressedFeatureListener = null;
                                        mChooseRequest = null;
                                    }
                                });

                        search.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // 非搜索相关view
                                        chooseTitle.setVisibility(View.GONE);
                                        search.setVisibility(View.GONE);
                                        chooseDone.setVisibility(View.GONE);
                                        llMapViewList.setVisibility(View.GONE);

                                        // 搜索相关内容view
                                        searchEdit.setVisibility(View.VISIBLE);
                                        searchEdit.requestFocus();
                                        inputCancel.setVisibility(View.GONE);
                                        recyclerviewSearch.setVisibility(View.VISIBLE);
                                        if (searchEdit.isFocused()) {
                                            InputMethodManager imm =
                                                    (InputMethodManager)
                                                            activity.getSystemService(
                                                                    Context.INPUT_METHOD_SERVICE);
                                            if (null != imm) {
                                                imm.showSoftInput(searchEdit,
                                                        InputMethodManager.SHOW_IMPLICIT);
                                            }
                                        }
                                    }
                                });

                        goBack.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (llMapViewList.getVisibility() == View.VISIBLE) {
                                            // 退出导航页面，删除View，并且销毁资源
                                            if (mMapProxy != null) {
                                                mMapProxy.onActivityDestroy();
                                                mMapProxy = null;
                                            }
                                            chooseLocationView.setVisibility(View.GONE);
                                            handler.postDelayed(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (chooseLocationRootView != null
                                                                    && chooseLocationView != null) {
                                                                chooseLocationRootView.removeView(
                                                                        chooseLocationView);
                                                                chooseLocationRootView = null;
                                                                chooseLocationView = null;
                                                            }
                                                        }
                                                    },
                                                    300);
                                            mChooseRequest
                                                    .getNativeInterface()
                                                    .getRootView()
                                                    .removeOnBackPressedFeatureListener(
                                                            onChooseLocationBackPressedFeatureListener);
                                            onChooseLocationBackPressedFeatureListener = null;
                                            mChooseRequest = null;
                                        } else {
                                            // 展示相关内容
                                            chooseTitle.setVisibility(View.VISIBLE);
                                            search.setVisibility(View.VISIBLE);
                                            chooseDone.setVisibility(View.VISIBLE);
                                            llMapViewList.setVisibility(View.VISIBLE);

                                            // 隐藏非相关内容
                                            searchEdit.setText("");
                                            searchEdit.setVisibility(View.GONE);
                                            inputCancel.setVisibility(View.GONE);
                                            recyclerviewSearch.setVisibility(View.GONE);

                                            // 隐藏软键盘
                                            InputMethodManager imm =
                                                    (InputMethodManager)
                                                            activity.getSystemService(
                                                                    Context.INPUT_METHOD_SERVICE);
                                            View currentFocusView = activity.getCurrentFocus();
                                            if (imm != null && currentFocusView != null) {
                                                imm.hideSoftInputFromWindow(
                                                        currentFocusView.getWindowToken(), 0);
                                            }
                                        }
                                    }
                                });

                        searchEdit.addTextChangedListener(
                                new TextWatcher() {
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before,
                                                              int count) {
                                        if (s.length() > 0) {
                                            if (mMapProxy != null) {
                                                if (!TextUtils.isEmpty(curLocInfo.city)) {
                                                    mMapProxy.poiSearchResult(
                                                            s.toString(),
                                                            curLocInfo,
                                                            new MapProxy.OnLocPoiSearchResultListener() {
                                                                @Override
                                                                public void onPoiSearchResultListener(
                                                                        List<LocationInfo> datas) {
                                                                    if (datas != null
                                                                            && datas.size() > 0) {
                                                                        if (recyclerviewSearch
                                                                                .getVisibility()
                                                                                == View.VISIBLE) {
                                                                            datasSearch.clear();
                                                                            datasSearch
                                                                                    .addAll(datas);
                                                                            locationAdapterSearch
                                                                                    .setKeyWord(
                                                                                            s.toString());
                                                                            locationAdapterSearch
                                                                                    .notifyDataSetChanged();
                                                                            recyclerviewSearch
                                                                                    .scrollToPosition(
                                                                                            0);
                                                                        }
                                                                    } else {
                                                                        datasSearch.clear();
                                                                        locationAdapterSearch
                                                                                .notifyDataSetChanged();
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    Log.e(TAG,
                                                            "currentCity is null,can not search poi.");
                                                }
                                            }
                                            inputCancel.setVisibility(View.VISIBLE);
                                        } else {
                                            inputCancel.setVisibility(View.GONE);
                                            datasSearch.clear();
                                            locationAdapterSearch.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start,
                                                                  int count, int after) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                    }
                                });

                        inputCancel.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        searchEdit.setText("");
                                    }
                                });

                        if (mMapProxy != null) {
                            mMapProxy.setShowMyLocation(true, false);
                            if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                                mMapProxy.updateLatLng(latitude, longitude, coordType);
                            } else {
                                mMapProxy.moveToMyLocation();
                            }
                            mMapProxy.setScale(18f);
                            mMapProxy.setOnStatusChangeListener(
                                    new MapProxy.OnStatusChangeListener() {
                                        @Override
                                        public void onStatusChangeStart() {
                                        }

                                        @Override
                                        public void onStatusChange() {
                                        }

                                        @Override
                                        public void onStatusChangeFinish(HybridLatLng latLngCur) {
                                            // 每次移动后，重新搜索附近的位置
                                            if (latLngCur != null) {
                                                recyclerviewNearBy.setVisibility(View.GONE);
                                                locationLoading.setVisibility(View.VISIBLE);
                                                mMapProxy.reverseGeoCodeResult(
                                                        latLngCur,
                                                        new MapProxy.OnLocGeoCoderResultListener() {
                                                            @Override
                                                            public void onGeoCoderResultListener(
                                                                    List<LocationInfo> datas) {
                                                                if (datas != null
                                                                        && datas.size() > 0) {
                                                                    curLocInfo.latitude =
                                                                            datas.get(0).latitude;
                                                                    curLocInfo.longitude =
                                                                            datas.get(0).longitude;
                                                                    curLocInfo.coordType =
                                                                            datas.get(0).coordType;
                                                                    curLocInfo.name =
                                                                            datas.get(0).name;
                                                                    curLocInfo.address =
                                                                            datas.get(0).address;
                                                                    curLocInfo.city =
                                                                            datas.get(0).city;

                                                                    recyclerviewNearBy
                                                                            .setVisibility(
                                                                                    View.VISIBLE);
                                                                    locationLoading.setVisibility(
                                                                            View.GONE);
                                                                    datasNearBy.clear();
                                                                    datasNearBy.addAll(datas);
                                                                    locationAdapterNearBy
                                                                            .setSelectSearchItemIndex(
                                                                                    0);
                                                                    locationAdapterNearBy
                                                                            .notifyDataSetChanged();
                                                                    recyclerviewNearBy
                                                                            .scrollToPosition(0);
                                                                } else {
                                                                    datasNearBy.clear();
                                                                    locationAdapterNearBy
                                                                            .notifyDataSetChanged();
                                                                    recyclerviewNearBy
                                                                            .setVisibility(
                                                                                    View.GONE);
                                                                    locationLoading.setVisibility(
                                                                            View.GONE);
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });

                            moveToLocation.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (mMapProxy != null) {
                                                mMapProxy.moveToMyLocation();
                                            }
                                        }
                                    });

                            mMapProxy.setOnMapLoadedListener(
                                    new MapProxy.OnMapLoadedListener() {
                                        @Override
                                        public void onMapLoaded() {
                                            mMapProxy.removeGeolocationMarkers();
                                            Point p = mMapProxy.getCenterPoint();
                                            if (p != null) {
                                                List<MapMarker> markerList = new ArrayList<>();
                                                MapMarker marker = new MapMarker();
                                                marker.offsetX = p.x;
                                                marker.offsetY = p.y;
                                                marker.zIndex = 10;
                                                marker.geoMarkerType =
                                                        MapMarker.GEOLOCATION_MARKER_TYPE_CENTER;
                                                markerList.add(marker);
                                                mMapProxy.setGeolocationMarkers(markerList);
                                            }

                                            HybridLatLng latLngCur = mMapProxy.getCenterLocation();
                                            if (latLngCur != null) {
                                                recyclerviewNearBy.setVisibility(View.GONE);
                                                locationLoading.setVisibility(View.VISIBLE);
                                                mMapProxy.reverseGeoCodeResult(
                                                        latLngCur,
                                                        new MapProxy.OnLocGeoCoderResultListener() {
                                                            @Override
                                                            public void onGeoCoderResultListener(
                                                                    List<LocationInfo> datas) {
                                                                if (datas != null
                                                                        && datas.size() > 0) {
                                                                    curLocInfo.latitude =
                                                                            datas.get(0).latitude;
                                                                    curLocInfo.longitude =
                                                                            datas.get(0).longitude;
                                                                    curLocInfo.coordType =
                                                                            datas.get(0).coordType;
                                                                    curLocInfo.name =
                                                                            datas.get(0).name;
                                                                    curLocInfo.address =
                                                                            datas.get(0).address;
                                                                    curLocInfo.city =
                                                                            datas.get(0).city;

                                                                    recyclerviewNearBy
                                                                            .setVisibility(
                                                                                    View.VISIBLE);
                                                                    locationLoading.setVisibility(
                                                                            View.GONE);
                                                                    datasNearBy.clear();
                                                                    datasNearBy.addAll(datas);
                                                                    locationAdapterNearBy
                                                                            .setSelectSearchItemIndex(
                                                                                    0);
                                                                    locationAdapterNearBy
                                                                            .notifyDataSetChanged();
                                                                } else {
                                                                    datasNearBy.clear();
                                                                    locationAdapterNearBy
                                                                            .notifyDataSetChanged();
                                                                    recyclerviewNearBy
                                                                            .setVisibility(
                                                                                    View.GONE);
                                                                    locationLoading.setVisibility(
                                                                            View.GONE);
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    protected Response getLocationType(Request request) throws JSONException {
        List<String> providers =
                LocationUtils.getEnabledProviders(Runtime.getInstance().getContext());
        request.getCallback().callback(responseLocationType(providers));
        return Response.SUCCESS;
    }

    private Response subscribe(Request request) throws JSONException {
        ILocationClient client = chooseClient(request);
        if (client == null) {
            request.getCallback().callback(responseLocation(CODE_UNAVAILABLE, null));
            return Response.SUCCESS;
        }
        request.getNativeInterface().getResidentManager().postRegisterFeature(this);
        SubscribeCallbackContext subscribeCallbackContext =
                new SubscribeCallbackContext(request, ACTION_SUBSCRIBE, client, false,
                        isReserved(request));
        putCallbackContext(subscribeCallbackContext);
        return Response.SUCCESS;
    }

    private Response unsubscribe(Request request) {
        request.getNativeInterface().getResidentManager().postUnregisterFeature(this);
        removeCallbackContext(ACTION_SUBSCRIBE);
        return Response.SUCCESS;
    }

    private Response getSupportedCoordTypes(Request request) throws JSONException {
        Set<String> coordTypes = getSupportedCoordTypes();
        JSONArray jsonArray = new JSONArray();
        if (coordTypes.size() > 0) {
            for (String coordType : coordTypes) {
                jsonArray.put(coordType);
            }
        }
        return new Response(jsonArray);
    }

    private Set<String> getSupportedCoordTypes() {
        Set<String> result = new HashSet<>();
        if (mProvider != null) {
            result = mProvider.getSupportedCoordTypes();
        } else {
            result.add(LocationProvider.COORTYPE_WGS84);
        }
        return result;
    }

    protected Response responseLocationType(List<String> providers) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        if (providers.size() > 0) {
            for (String provider : providers) {
                jsonArray.put(provider);
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(RESULT_TYPES, jsonArray);
        return new Response(jsonObject);
    }

    private Response responseLocation(int code, HapLocation location) throws JSONException {
        if (code == CODE_TIMEOUT) {
            return new Response(Response.CODE_TIMEOUT, "timeout");
        } else if (code == CODE_CLOSE) {
            return new Response(ERROR_SERVICE_CLOSE, "location service is closed");
        } else if (code == CODE_UNAVAILABLE) {
            return new Response(
                    Response.CODE_SERVICE_UNAVAILABLE, "no network or location service closed");
        } else {
            if (location != null) {
                JSONObject js = new JSONObject();
                js.put(RESULT_LATITUDE, location.getLatitude());
                js.put(RESULT_LONGITUDE, location.getLongitude());
                js.put(RESULT_ACCURACY, location.getAccuracy());
                js.put(RESULT_TIME, location.getTime());
                return new Response(js);
            } else {
                return new Response(Response.CODE_GENERIC_ERROR, "no location");
            }
        }
    }

    private class SubscribeCallbackContext extends CallbackContext {
        private ILocationClient client;
        private boolean useCache;

        public SubscribeCallbackContext(
                Request request,
                String action,
                ILocationClient client,
                boolean useCache,
                boolean reserved) {
            super(Geolocation.this, action, request, reserved);
            this.client = client;
            this.useCache = useCache;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            client.subscribe(
                    useCache,
                    new LocationChangedListener() {
                        @Override
                        public void onLocationChanged(HapLocation location, int errorCode) {
                            runCallbackContext(getAction(), errorCode, location);
                        }
                    });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            client.unsubscribe();
        }

        @Override
        public void callback(int what, Object obj) {
            try {
                HapLocation location = (HapLocation) obj;
                Response response = responseLocation(what, location);
                mRequest.getCallback().callback(response);
            } catch (JSONException e) {
                Log.e(TAG, "Fail to callback location change", e);
            }
        }
    }
}
