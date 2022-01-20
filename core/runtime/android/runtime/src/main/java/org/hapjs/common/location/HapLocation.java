/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

public class HapLocation {
    private double mLatitude;
    private double mLongitude;
    private float mAccuracy;
    private long mTime;

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public float getAccuracy() {
        return mAccuracy;
    }

    public void setAccuracy(float accuracy) {
        this.mAccuracy = accuracy;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }
}
