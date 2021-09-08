/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class LocationClient {

    private static final int INTERVAL_TIME = 1000;
    private static final int INTERVAL_DISTANCE = 5;

    private Context mContext;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private LocationListener mLocationListener;

    private LocationListenerImpl mNetworkLocationListener = new LocationListenerImpl();
    private LocationListenerImpl mGPSLocationListener = new LocationListenerImpl();

    public LocationClient(Context context) {
        mContext = context.getApplicationContext();
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static boolean isLocationServiceOn(Context context) {
        LocationManager manager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }

        long significantTimeInterval = 5 * 1000;
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > significantTimeInterval;
        boolean isSignificantlyOlder = timeDelta < -significantTimeInterval;
        boolean isOlder = timeDelta < 0;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 100;

        boolean isFromSameProvider =
                TextUtils.equals(location.getProvider(), currentBestLocation.getProvider());

        if (isMoreAccurate) {
            return true;
        } else if (!isOlder && !isLessAccurate) {
            return true;
        } else if (!isOlder && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    public void registerLocationListener(LocationListener listener) {
        if (listener != null) {
            mLocationListener = listener;
        }
    }

    @SuppressWarnings("MissingPermission")
    public void unRegisterLocationListener(LocationListener listener) {
        if (listener != null) {
            stop();
        }
    }

    @SuppressWarnings("MissingPermission")
    public void stop() {
        mLocationManager.removeUpdates(mNetworkLocationListener);
        mLocationManager.removeUpdates(mGPSLocationListener);
    }

    @SuppressWarnings("MissingPermission")
    public void start() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat
                .checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        syncCurrentLocation();

        stop();

        if (hasGPSModule(mContext)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_TIME, INTERVAL_DISTANCE, mNetworkLocationListener);
        }

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL_TIME, INTERVAL_DISTANCE, mGPSLocationListener);
    }

    @SuppressWarnings("MissingPermission")
    public void syncCurrentLocation() {
        Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation =
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long gpsTime = (gpsLocation == null ? 0 : gpsLocation.getTime());
        long networkTime = (networkLocation == null ? 0 : networkLocation.getTime());
        if (gpsTime > networkTime) {
            updateLocation(gpsLocation);
        } else {
            updateLocation(networkLocation);
        }
    }

    private void updateLocation(Location location) {
        if (location != null && isBetterLocation(location, mCurrentLocation)) {
            mCurrentLocation = location;
            if (mLocationListener != null) {
                mLocationListener.onLocationChanged(
                        mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude(),
                        mCurrentLocation.getAccuracy());
            }
        }
    }

    private class LocationListenerImpl implements android.location.LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @SuppressWarnings("MissingPermission")
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            updateLocation(mLocationManager.getLastKnownLocation(provider));
        }

        @SuppressWarnings("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            updateLocation(mLocationManager.getLastKnownLocation(provider));
        }

        @SuppressWarnings("MissingPermission")
        @Override
        public void onProviderDisabled(String provider) {
            updateLocation(mLocationManager.getLastKnownLocation(provider));
        }
    }

    public boolean hasGPSModule(Context context) {
        final LocationManager mgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        if ( mgr == null ) {
            return false;
        }
        final List<String> providers = mgr.getAllProviders();
        if ( providers == null ) {
            return false;
        }
        return providers.contains(LocationManager.GPS_PROVIDER);
    }
}
