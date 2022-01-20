/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class LocationUtils {

    private static final String TAG = "LocationUtils";

    public static List<String> getEnabledProviders(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> list = new ArrayList<>(2);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && isNetworkConnected(context)) {
            list.add(LocationManager.NETWORK_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            list.add(LocationManager.GPS_PROVIDER);
        }
        return list;
    }

    public static boolean isLocationServiceClosed(Context context) {
        try {
            int locationMode =
                    Settings.Secure
                            .getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return locationMode == Settings.Secure.LOCATION_MODE_OFF;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "local service closed error", e);
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
