/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import java.util.List;

public class DefaultLocationClient extends AbstractLocationClient {

    private LocationManager mLocationManager;
    private LocationListenerImpl mRealListener = new LocationListenerImpl();
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    public DefaultLocationClient(Context context) {
        super(context);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    protected HapLocation getCacheLocation() {
        Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation =
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long currentTime = System.currentTimeMillis();
        long gpsTime = gpsLocation == null ? 0 : gpsLocation.getTime();
        long networkTime = networkLocation == null ? 0 : networkLocation.getTime();

        Location result = gpsTime >= networkTime ? gpsLocation : networkLocation;
        if (result != null
                && currentTime > result.getTime()
                && currentTime - result.getTime() < CACHE_EXPIRE) {
            return convertLocation(result);
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    protected void start() {
        mMainHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        for (String provider : LocationUtils.getEnabledProviders(mContext)) {
                            mLocationManager
                                    .requestLocationUpdates(provider, 200, 0, mRealListener);
                        }
                    }
                });
    }

    @SuppressLint("MissingPermission")
    protected void stop() {
        mMainHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        mLocationManager.removeUpdates(mRealListener);
                    }
                });
    }

    @Override
    public void subscribe(boolean useCache, LocationChangedListener listener) {
        if (LocationUtils.isLocationServiceClosed(mContext) && listener != null) {
            listener.onLocationChanged(null, LocationChangedListener.CODE_CLOSE);
            return;
        }

        List<String> providers = LocationUtils.getEnabledProviders(mContext);
        if (providers.isEmpty() && listener != null) {
            listener.onLocationChanged(null, LocationChangedListener.CODE_UNAVAILABLE);
            return;
        }
        super.subscribe(useCache, listener);
    }

    private HapLocation convertLocation(Location location) {
        HapLocation hapLocation = new HapLocation();
        hapLocation.setLatitude(location.getLatitude());
        hapLocation.setLongitude(location.getLongitude());
        hapLocation.setAccuracy(location.getAccuracy());
        hapLocation.setTime(location.getTime());
        return hapLocation;
    }

    private class LocationListenerImpl implements android.location.LocationListener {
        private static final int EXPIRE_TIME = 5 * 1000;
        private Location currentBestLocation;

        LocationListenerImpl() {
        }

        @Override
        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, currentBestLocation)) {
                currentBestLocation = location;
                if (mListener != null) {
                    mListener.onLocationChanged(
                            convertLocation(location),
                            LocationChangedListener.CODE_RESULT_RECEIVED);
                }
            }
        }

        private boolean isBetterLocation(Location location, Location currentBestLocation) {
            if (currentBestLocation == null) {
                return true;
            }

            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyNewer = timeDelta > EXPIRE_TIME;
            boolean isSignificantlyOlder = timeDelta < -EXPIRE_TIME;
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

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
