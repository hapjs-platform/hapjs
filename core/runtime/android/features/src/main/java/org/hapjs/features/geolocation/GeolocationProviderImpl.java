/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.features.Geolocation;
import org.hapjs.features.geolocation.util.PackageUtils;
import org.hapjs.widgets.map.CoordType;

public class GeolocationProviderImpl implements GeolocationProvider {

    public static final String PACKAGE_NAME_BAIDUMAP = "com.baidu.BaiduMap";
    public static final String PACKAGE_NAME_AMAP = "com.autonavi.minimap";
    public static final String PACKAGE_NAME_QQMAP = "com.tencent.map";
    public static final String BAIDUMAP_NAVI_URI = "baidumap://map/navi?";
    public static final String BAIDUMAP_ROUTE_URI = "baidumap://map/direction?";
    public static final String AMAP_NAVI_URI = "androidamap://navi?";
    public static final String AMAP_ROUTE_URI = "amapuri://route/plan/?";
    public static final String QQMAP_NAVI_URI = "qqmap://map/routeplan?";
    protected static final String COORD_TYPE = "coordType";
    protected static final String TYPE_WGS84 = CoordType.WGS84;
    protected static final String TYPE_GCJ02 = CoordType.GCJ02;
    private static final String TAG = "GeolocationProviderImpl";

    @Override
    public void onNavigateButtonClick(
            Activity activity, NavigationInfo navigationInfo, Request request) {
        List<String> availableMapList = new ArrayList<>();

        if (PackageUtils.isPackageInstalled(activity, PACKAGE_NAME_BAIDUMAP)) {
            availableMapList.add(
                    activity.getResources().getString(org.hapjs.features.R.string.baidu_map));
        }

        if (PackageUtils.isPackageInstalled(activity, PACKAGE_NAME_AMAP)) {
            availableMapList
                    .add(activity.getResources().getString(org.hapjs.features.R.string.a_map));
        }

        if (isQQMapNaviAvailable()
                && PackageUtils.isPackageInstalled(activity, PACKAGE_NAME_QQMAP)) {
            availableMapList
                    .add(activity.getResources().getString(org.hapjs.features.R.string.qq_map));
        }

        if (availableMapList.size() > 0) {
            showMapAppDialog(activity, availableMapList, navigationInfo, request);
        } else {
            showNoMapAppDialog(activity, request);
        }
    }

    private String getQQMapKey() {
        // qq地图需要申请相应的key才支持拉起导航
        return null;
    }

    private boolean isQQMapNaviAvailable() {
        return !TextUtils.isEmpty(getQQMapKey());
    }

    private void showMapAppDialog(
            Activity activity,
            java.util.List<String> availableMapList,
            NavigationInfo navigationInfo,
            Request request) {
        final String[] items = availableMapList.toArray(new String[availableMapList.size()]);
        CustomBottomDialog customBottomDialog = CustomBottomDialog.newInstance(items);
        customBottomDialog.setOnItemClickListener(
                new CustomBottomDialog.ClickBottomItemListener() {
                    @Override
                    public void onItemClick(int position) {
                        String chooseMap = items[position];
                        navigateStart(activity, chooseMap, navigationInfo, request);
                    }
                });
        customBottomDialog.showDialog(activity.getFragmentManager());
    }

    private void showNoMapAppDialog(Activity activity, Request request) {
        String[] items = {
                activity.getResources().getString(org.hapjs.features.R.string.install_baidu_map_tip)
        };
        CustomBottomDialog customBottomDialog = CustomBottomDialog.newInstance(items);
        customBottomDialog.setOnItemClickListener(
                new CustomBottomDialog.ClickBottomItemListener() {
                    @Override
                    public void onItemClick(int position) {
                        // 如果没有地图应用跳转到应用市场安装百度地图。
                        Intent intent;
                        Uri uri = Uri.parse("market://details?id=" + PACKAGE_NAME_BAIDUMAP);
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        activity.startActivity(intent);
                    }
                });
        request
                .getCallback()
                .callback(new Response(Geolocation.ERROR_MAP_APP_NOT_INSTALL,
                        "no available map app."));
        customBottomDialog.showDialog(activity.getFragmentManager());
    }

    private void navigateStart(
            Activity activity, String appName, NavigationInfo navigetionInfo, Request request) {
        if (appName
                .equals(activity.getResources().getString(org.hapjs.features.R.string.baidu_map))) {
            naviByBaiduMap(activity, navigetionInfo, request);
            request
                    .getCallback()
                    .callback(
                            new Response(Response.CODE_SUCCESS, "send open baidu map app intent."));
        } else if (appName.equals(
                activity.getResources().getString(org.hapjs.features.R.string.a_map))) {
            naviByAMap(activity, navigetionInfo, request);
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_SUCCESS, "send open A map app intent."));
        } else if (appName.equals(
                activity.getResources().getString(org.hapjs.features.R.string.qq_map))) {
            naviByQQMap(activity, navigetionInfo, request);
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_SUCCESS, "send open qq map app intent."));
        } else {
            request
                    .getCallback()
                    .callback(new Response(Geolocation.ERROR_OPEN_MAP_APP_FAIL,
                            "fail to open map app."));
        }
    }

    private void naviByQQMap(Activity activity, NavigationInfo navigetionInfo, Request request) {
        Uri uri =
                Uri.parse(
                        QQMAP_NAVI_URI
                                + "type="
                                + "drive"
                                + "&fromcoord="
                                + "CurrentLocation"
                                + "&to="
                                + navigetionInfo.name
                                + "&tocoord="
                                + navigetionInfo.latitude
                                + ","
                                + navigetionInfo.longitude
                                + "&referer="
                                + getQQMapKey());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }

    private void naviByAMap(Activity activity, NavigationInfo navigetionInfo, Request request) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        // 将功能Scheme以URI的方式传入data
        Uri uri =
                Uri.parse(
                        AMAP_ROUTE_URI
                                + "sourceApplication="
                                + "com.quick.app"
                                + "&dlat="
                                + navigetionInfo.latitude
                                + "&dlon="
                                + navigetionInfo.longitude
                                + "&dname="
                                + navigetionInfo.name
                                + "&dev="
                                + 0
                                + "&t="
                                + 0);
        intent.setData(uri);
        intent.setPackage(PACKAGE_NAME_AMAP);
        activity.startActivity(intent);
    }

    private void naviByBaiduMap(Activity activity, NavigationInfo navigetionInfo, Request request) {
        Intent intent = new Intent();
        intent.setData(
                Uri.parse(
                        BAIDUMAP_ROUTE_URI
                                + "destination="
                                + "name:"
                                + navigetionInfo.name
                                + "|latlng:"
                                + navigetionInfo.latitude
                                + ","
                                + navigetionInfo.longitude
                                + "|addr:"
                                + navigetionInfo.address
                                + "&coord_type="
                                + TYPE_GCJ02
                                + "&mode="
                                + "driving"
                                + "&src="
                                + "com.quick.app"));
        activity.startActivity(intent);
    }
}
