/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Rect;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.view.Display;
import android.view.WindowManager;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.permission.HapPermissionManager;
import org.hapjs.bridge.permission.PermissionCallback;
import org.hapjs.common.utils.DeviceInfoUtil;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.common.utils.PackageUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.render.DecorLayout;
import org.hapjs.render.RootView;
import org.hapjs.render.cutout.CutoutSupportFactory;
import org.hapjs.render.cutout.ICutoutSupport;
import org.hapjs.render.vdom.VDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implement FeatureExtension for getting Device information
 *
 * <table>
 * <tr>
 * <th>Action name</th> <th>Action description</th> <th>Invocation mode</th>
 * <th>Request param name</th> <th>Request param description</th>
 * <th>Response param name</th> <th>Response param description</th>
 * </tr>
 * <tr>
 * <td>getInfo</td> <td>Get device information </td> <td>SYNC</td>
 * <td>N/A</td> <td>N/A</td>
 * <td>json object</td> <td>this object contains all keys including 'model', 'romVersion', 'language',
 * 'region', 'deviceId', all these keys will be followed by a string as its value respectively</td>
 * </tr>
 * </table>
 *
 * @hide
 */
@FeatureExtensionAnnotation(
        name = Device.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = Device.ACTION_GET_INFO, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Device.ACTION_GET_ADVERTISING_ID,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Device.ACTION_GET_USER_ID, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Device.ACTION_GET_DEVICE_ID, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Device.ACTION_GET_OAID, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Device.ACTION_GET_ID, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Device.ACTION_GET_SERIAL,
                        mode = FeatureExtension.Mode.ASYNC,
                        permissions = {Manifest.permission.READ_PHONE_STATE}),
                @ActionAnnotation(name = Device.ACTION_GET_CPU_INFO, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Device.ACTION_GET_TOTAL_STORAGE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Device.ACTION_GET_AVAILABLE_STORAGE,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Device.ATTR_PLATFORM_INFO,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Device.ATTR_PLATFORM_INFO_ALIAS),
                @ActionAnnotation(
                        name = Device.ATTR_HOST,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Device.ATTR_HOST_ALIAS),
                @ActionAnnotation(
                        name = Device.ATTR_ALLOW_TRACK_OAID,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Device.ATTR_ALLOW_TRACK_OAID_ALIAS)
        })
public class Device extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.device";
    protected static final String ACTION_GET_INFO = "getInfo";
    protected static final String ACTION_GET_ID = "getId";
    protected static final String ACTION_GET_ADVERTISING_ID = "getAdvertisingId";
    protected static final String ACTION_GET_DEVICE_ID = "getDeviceId";
    protected static final String ACTION_GET_USER_ID = "getUserId";
    protected static final String ACTION_GET_SERIAL = "getSerial";
    protected static final String ACTION_GET_CPU_INFO = "getCpuInfo";
    protected static final String ACTION_GET_TOTAL_STORAGE = "getTotalStorage";
    protected static final String ACTION_GET_AVAILABLE_STORAGE = "getAvailableStorage";
    protected static final String ACTION_GET_OAID = "getOAID";
    protected static final String PARAM_TYPE = "type";
    protected static final String TYPE_DEVICE = "device";
    protected static final String TYPE_MAC = "mac";
    protected static final String TYPE_USER = "user";
    protected static final String TYPE_ADVERTISING = "advertising";
    protected static final String TYPE_OAID = "oaid";
    protected static final String RESULT_BRAND = "brand";
    protected static final String RESULT_MANUFACTURER = "manufacturer";
    protected static final String RESULT_MODEL = "model";
    protected static final String RESULT_PRODUCT = "product";
    protected static final String RESULT_OS_TYPE = "osType";
    protected static final String RESULT_OS_VERSION_NAME = "osVersionName";
    protected static final String RESULT_OS_VERSION_CODE = "osVersionCode";
    protected static final String RESULT_VENDOR_OS_NAME = "vendorOsName";
    protected static final String RESULT_VENDOR_OS_VERSION = "vendorOsVersion";
    protected static final String RESULT_PLATFORM_VERSION_NAME = "platformVersionName";
    protected static final String RESULT_PLATFORM_VERSION_CODE = "platformVersionCode";
    protected static final String RESULT_LANGUAGE = "language";
    protected static final String RESULT_REGION = "region";
    protected static final String RESULT_SCREEN_DENSITY = "screenDensity";
    protected static final String RESULT_SCREEN_WIDTH = "screenWidth";
    protected static final String RESULT_SCREEN_HEIGHT = "screenHeight";
    protected static final String RESULT_WINDOW_WIDTH = "windowWidth";
    protected static final String RESULT_WINDOW_HEIGHT = "windowHeight";
    protected static final String RESULT_DEVICE_TYPE = "deviceType";
    protected static final String RESULT_STATUS_BAR_HEIGHT = "statusBarHeight";
    protected static final String RESULT_CUTOUT = "cutout";
    protected static final String RESULT_SCREEN_REFRESH_RATE = "screenRefreshRate";

    protected static final String RESULT_ADVERTISING_ID = "advertisingId";
    protected static final String RESULT_USER_ID = "userId";
    protected static final String RESULT_SERIAL = "serial";
    protected static final String RESULT_CPU_INFO = "cpuInfo";
    protected static final String RESULT_DEVICE_ID = "deviceId";
    protected static final String RESULT_TOTAL_STORAGE = "totalStorage";
    protected static final String RESULT_AVAILABLE_STORAGE = "availableStorage";
    protected static final String RESULT_DEVICE = "device";
    protected static final String RESULT_MAC = "mac";
    protected static final String RESULT_USER = "user";
    protected static final String RESULT_ADVERTISING = "advertising";
    protected static final String RESULT_VERSION_NAME = "versionName";
    protected static final String RESULT_VERSION_CODE = "versionCode";
    protected static final String RESULT_PACKAGE = "package";
    protected static final String CPU_INFO_FILE = "/proc/cpuinfo";
    // attr
    protected static final String ATTR_PLATFORM_INFO_ALIAS = "platform";
    protected static final String ATTR_PLATFORM_INFO = "__getPlatform";
    protected static final String ATTR_HOST_ALIAS = "host";
    protected static final String ATTR_HOST = "__getHost";
    protected static final String ATTR_ALLOW_TRACK_OAID_ALIAS = "allowTrackOAID";
    protected static final String ATTR_ALLOW_TRACK_OAID = "__getAllowTrackOAID";
    private static final String TAG = "Device";

    @Override
    public Response invokeInner(Request request) throws JSONException {
        String action = request.getAction();
        Response response = null;
        if (ACTION_GET_ID.equals(action)) {
            response = getId(request);
        } else if (ACTION_GET_ADVERTISING_ID.equals(action)) {
            response = getAdvertisingId(request);
        } else if (ACTION_GET_DEVICE_ID.equals(action)) {
            response = getDeviceId(request);
        } else if (ACTION_GET_USER_ID.equals(action)) {
            response = getUserId(request);
        } else if (ACTION_GET_SERIAL.equals(action)) {
            response = getSerial(request);
        } else if (ACTION_GET_CPU_INFO.equals(action)) {
            response = getCpuInfo(request);
        } else if (ACTION_GET_TOTAL_STORAGE.equals(action)) {
            response = getTotalStorage(request);
        } else if (ACTION_GET_AVAILABLE_STORAGE.equals(action)) {
            response = getAvailableStorage(request);
        } else if (ACTION_GET_OAID.equals(action)) {
            response = getOAID(request);
        } else if (ATTR_PLATFORM_INFO.equals(action)) {
            return getPlatformAttr(request);
        } else if (ATTR_HOST.equals(action)) {
            return getHostAttr(request);
        } else if (ATTR_ALLOW_TRACK_OAID.equals(action)) {
            return getAllowTrackOAIDAttr(request);
        } else {
            response = getInfo(request);
        }
        if (response != null) {
            request.getCallback().callback(response);
        }
        return Response.SUCCESS;
    }

    private Response getInfo(Request request) {
        try {
            JSONObject info = getInfoJSONObject(request);
            return new Response(info);
        } catch (JSONException e) {
            return getExceptionResponse(ACTION_GET_INFO, e);
        } catch (SecurityException e) {
            return getExceptionResponse(ACTION_GET_INFO, e);
        }
    }

    protected JSONObject getInfoJSONObject(Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        JSONObject info = new JSONObject();
        info.put(RESULT_BRAND, Build.BRAND);
        info.put(RESULT_MANUFACTURER, Build.MANUFACTURER);
        info.put(RESULT_MODEL, Build.MODEL);
        info.put(RESULT_PRODUCT, Build.PRODUCT);
        info.put(RESULT_OS_TYPE, "android");
        info.put(RESULT_OS_VERSION_NAME, Build.VERSION.RELEASE);
        info.put(RESULT_OS_VERSION_CODE, Build.VERSION.SDK_INT);
        String[] vendorOsInfo = getVendorOsInfo();
        info.put(RESULT_VENDOR_OS_NAME, vendorOsInfo[0]);
        info.put(RESULT_VENDOR_OS_VERSION, vendorOsInfo[1]);
        info.put(RESULT_PLATFORM_VERSION_NAME, org.hapjs.runtime.BuildConfig.platformVersionName);
        info.put(RESULT_PLATFORM_VERSION_CODE, org.hapjs.runtime.BuildConfig.platformVersion);
        info.put(RESULT_LANGUAGE, Locale.getDefault().getLanguage());
        info.put(RESULT_REGION, Locale.getDefault().getCountry());
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        info.put(RESULT_SCREEN_DENSITY, dm.density);
        info.put(RESULT_SCREEN_WIDTH, dm.widthPixels);
        info.put(RESULT_SCREEN_HEIGHT, dm.heightPixels);
        int windowWidth = dm.widthPixels;
        int windowHeight = dm.heightPixels;
        int statusBarHeight = 0;
        RootView rootView = request.getNativeInterface().getRootView();
        if (rootView != null && rootView.getDocument() != null) {
            VDocument vDocument = rootView.getDocument();
            DecorLayout mDecorLayout = (DecorLayout) vDocument.getComponent().getInnerView();
            if (mDecorLayout != null) {
                statusBarHeight = mDecorLayout.getStatusBarHeight();
                windowWidth = mDecorLayout.getMeasuredWidth();
                windowHeight =
                        mDecorLayout.getMeasuredHeight() - mDecorLayout.getContentInsets().top;
            }
        }
        info.put(RESULT_STATUS_BAR_HEIGHT, statusBarHeight);
        info.put(RESULT_WINDOW_WIDTH, windowWidth);
        info.put(RESULT_WINDOW_HEIGHT, windowHeight);
        info.put(RESULT_CUTOUT, getCutoutInfo(request));
        info.put(RESULT_DEVICE_TYPE, BuildConfig.FLAVOR);
        Display display = activity.getWindowManager().getDefaultDisplay();
        info.put(RESULT_SCREEN_REFRESH_RATE, display.getRefreshRate());
        return info;
    }

    private JSONArray getCutoutInfo(Request request) {
        JSONArray jsonArray = new JSONArray();
        Activity activity = request.getNativeInterface().getActivity();
        if (activity == null) {
            return jsonArray;
        }
        try {
            ICutoutSupport cutoutSupport = CutoutSupportFactory.createCutoutSupport();
            boolean isCutoutScreen =
                    cutoutSupport
                            .isCutoutScreen(activity.getApplicationContext(), activity.getWindow());
            if (isCutoutScreen) {
                List<Rect> cutoutDisplay =
                        cutoutSupport.getCutoutDisplay(activity.getApplicationContext(),
                                activity.getWindow());
                if (cutoutDisplay != null && cutoutDisplay.size() > 0) {
                    for (Rect rect : cutoutDisplay) {
                        try {
                            JSONObject cutout = new JSONObject();
                            cutout.put("left", rect.left);
                            cutout.put("top", rect.top);
                            cutout.put("right", rect.right);
                            cutout.put("bottom", rect.bottom);
                            jsonArray.put(cutout);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return jsonArray;
    }

    private Response getId(Request request) throws JSONException {
        String rawParams = request.getRawParams();
        if (rawParams == null || rawParams.isEmpty()) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "no type");
        }

        JSONObject params = new JSONObject(rawParams);
        JSONArray typeObject = params.optJSONArray(PARAM_TYPE);
        if (typeObject == null || typeObject.length() == 0) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT, "no type");
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermission(request, new String[] {Manifest.permission.READ_PHONE_STATE});
        } else {
            getId(request, true);
        }
        return null;
    }

    protected void getId(Request request, boolean isAndroidQOrAbove) throws JSONException {
        String rawParams = request.getRawParams();
        JSONObject params = new JSONObject(rawParams);
        JSONArray typeObject = params.optJSONArray(PARAM_TYPE);
        JSONObject result = new JSONObject();
        Context context = request.getNativeInterface().getActivity();
        int n = typeObject.length();
        for (int i = 0; i < n; ++i) {
            String type = typeObject.getString(i);
            if (TYPE_DEVICE.equals(type)) {
                if (isAndroidQOrAbove) {
                    result.put(RESULT_DEVICE, getOAID(context));
                } else {
                    result.put(RESULT_DEVICE, getDeviceId(context));
                }
            } else if (TYPE_MAC.equals(type)) {
                result.put(RESULT_MAC, getMacAddress(context));
            } else if (TYPE_USER.equals(type)) {
                result.put(RESULT_USER, getAndroidId(context));
            } else if (TYPE_ADVERTISING.equals(type)) {
                result.put(RESULT_ADVERTISING, getAdvertisingId(context));
            } else {
                Log.e(FEATURE_NAME, "unexcept type:" + type);
            }
        }
        request.getCallback().callback(new Response(result));
    }

    private String getMacAddress(Context context) {
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        return wInfo.getMacAddress();
    }

    private String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String getAdvertisingId(Context context) {
        return DeviceInfoUtil.getAdvertisingId(context);
    }

    protected Response getAdvertisingId(Request request) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        String adId = getAdvertisingId(context);
        if (TextUtils.isEmpty(adId)) {
            return new Response(Response.CODE_GENERIC_ERROR, "getAdvertisingId fail");
        }

        JSONObject result = new JSONObject();
        result.put(RESULT_ADVERTISING_ID, adId);
        return new Response(result);
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    protected String getDeviceId(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }

    private Response getDeviceId(Request request) throws JSONException {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermission(request, new String[] {Manifest.permission.READ_PHONE_STATE});
        } else {
            getDeviceId(request, true);
        }
        return null;
    }

    protected void getDeviceId(Request request, boolean isAndroidQOrAbove) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        JSONObject result = new JSONObject();
        if (isAndroidQOrAbove) {
            result.put(RESULT_DEVICE_ID, getOAID(context));
        } else {
            result.put(RESULT_DEVICE_ID, getDeviceId(context));
        }
        request.getCallback().callback(new Response(result));
    }

    private Response getOAID(Request request) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        JSONObject result = new JSONObject();
        result.put(TYPE_OAID, getOAID(context));
        return new Response(result);
    }

    protected String getOAID(Context context) {
        // TODO add get oaid on Android Q
        return "";
    }

    private Response getUserId(Request request) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        JSONObject result = new JSONObject();
        result.put(RESULT_USER_ID, getAndroidId(context));
        return new Response(result);
    }

    private Response getSerial(Request request) throws JSONException {
        String serial;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Starting with Android API level 29(Q),third part App not allowed to get SN.
            return new Response(Response.CODE_GENERIC_ERROR, "getSerial fail，not allowed to get SN starting with Android Q");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                serial = Build.getSerial();
            } catch (SecurityException e) {
                return getExceptionResponse(ACTION_GET_SERIAL, e);
            }
        } else {
            serial = Build.SERIAL;
        }
        JSONObject result = new JSONObject();
        result.put(RESULT_SERIAL, serial);
        return new Response(result);
    }

    private Response getCpuInfo(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_CPU_INFO, FileUtils.readFileAsString(CPU_INFO_FILE));
        } catch (IOException e) {
            return getExceptionResponse(request, e);
        }
        return new Response(result);
    }

    private Response getTotalStorage(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        result.put(RESULT_TOTAL_STORAGE, stat.getTotalBytes());
        return new Response(result);
    }

    private Response getAvailableStorage(Request request) throws JSONException {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        JSONObject result = new JSONObject();
        result.put(RESULT_AVAILABLE_STORAGE, stat.getAvailableBytes());
        return new Response(result);
    }

    private Response getPlatformAttr(Request request) {
        JSONObject info = new JSONObject();
        try {
            info.put(RESULT_VERSION_NAME, org.hapjs.runtime.BuildConfig.platformVersionName);
            info.put(RESULT_VERSION_CODE, org.hapjs.runtime.BuildConfig.platformVersion);
            return new Response(info);
        } catch (JSONException e) {
            return getExceptionResponse(ACTION_GET_INFO, e);
        } catch (SecurityException e) {
            return getExceptionResponse(ACTION_GET_INFO, e);
        }
    }

    protected void requestPermission(final Request request, String[] permissions) {
        HapPermissionManager.getDefault()
                .requestPermissions(
                        request.getView().getHybridManager(),
                        permissions,
                        new PermissionCallback() {
                            @Override
                            public void onPermissionAccept() {
                                String action = request.getAction();
                                try {
                                    if (ACTION_GET_ID.equals(action)) {
                                        getId(request, false);
                                    } else if (ACTION_GET_DEVICE_ID.equals(action)) {
                                        getDeviceId(request, false);
                                    } else {
                                        Log.e(FEATURE_NAME, "unexcept action:" + action);
                                        request.getCallback()
                                                .callback(new Response(Response.NO_ACTION));
                                    }
                                } catch (Exception e) {
                                    Log.e(FEATURE_NAME, "getId fail!", e);
                                    request.getCallback().callback(getExceptionResponse(action, e));
                                }
                            }

                            @Override
                            public void onPermissionReject(int reason, boolean dontDisturb) {
                                request.getCallback().callback(Response.getUserDeniedResponse(dontDisturb));
                            }
                        });
    }

    private Response getHostAttr(Request request) {
        Activity activity = request.getNativeInterface().getActivity();
        String hostPkg = activity.getPackageName();
        JSONObject info = getPkgInfo(activity, hostPkg);
        getMorePackageInfo(request, info);
        return new Response(info);
    }

    protected Response getAllowTrackOAIDAttr(Request request) {
        return new Response(true);
    }

    private JSONObject getPkgInfo(Context context, String pkg) {
        JSONObject info = new JSONObject();
        try {
            info.put(RESULT_PACKAGE, pkg);
            PackageInfo pi = PackageUtils.getPackageInfo(context, pkg, 0);
            if (pi != null) {
                info.put(RESULT_VERSION_NAME, pi.versionName);
                info.put(RESULT_VERSION_CODE, pi.versionCode);
            } else {
                info.put(RESULT_VERSION_NAME, null);
                info.put(RESULT_VERSION_CODE, null);
            }
        } catch (JSONException e) {
            Log.e(TAG, "getPkgInfo: JSONException", e);
        }
        return info;
    }

    private void getMorePackageInfo(Request request, JSONObject packageInfo) {
        if (null == packageInfo || null == request) {
            Log.e(TAG, "getMorePackageInfo packageInfo or request is null.");
            return;
        }
        ApplicationContext applicationContext = request.getApplicationContext();
        if (null == applicationContext) {
            Log.e(TAG, "getMorePackageInfo applicationContext is null.");
            return;
        }
        AppInfo appInfo = applicationContext.getAppInfo();
        org.hapjs.model.PackageInfo packageInfoObject = null;
        if (null != appInfo) {
            packageInfoObject = appInfo.getPackageInfo();
        }
        if (null == packageInfoObject) {
            Log.e(TAG, "getMorePackageInfo packageInfoObject is null.");
            return;
        }
        try {
            packageInfo.put(
                    org.hapjs.model.PackageInfo.KEY_TOOLKIT_VERSION,
                    packageInfoObject.getToolkitVersion());
            packageInfo.put(
                    org.hapjs.model.PackageInfo.KEY_PACKAGE_TIMESTAMP,
                    packageInfoObject.getTimeStamp());
        } catch (JSONException e) {
            Log.e(TAG, "getMorePackageInfo: JSONException", e);
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    public String[] getVendorOsInfo() {
        return new String[] {"", ""};
    }
}
