/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

import android.os.Parcel;
import android.os.Parcelable;

public class AppInfo implements Parcelable {
    public static final Creator<AppInfo> CREATOR = new Creator<AppInfo>() {
        @Override
        public AppInfo createFromParcel(Parcel source) {
            return new AppInfo(source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readInt(),
                    source.readInt());
        }

        @Override
        public AppInfo[] newArray(int size) {
            return new AppInfo[0];
        }
    };
    private String mName;
    private String mPackage;
    private String mVersionName;
    private int mMinPlatformVersion;
    private int mVersionCode;

    public AppInfo(String pkg, String name, String versionName, int versionCode,
                   int minPlatformVersion) {
        mPackage = pkg;
        mName = name;
        mVersionName = versionName;
        mVersionCode = versionCode;
        mMinPlatformVersion = minPlatformVersion;
    }

    public String getPackage() {
        return mPackage;
    }

    public String getName() {
        return mName;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public int getMinPlatformVersion() {
        return mMinPlatformVersion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackage);
        dest.writeString(mName);
        dest.writeString(mVersionName);
        dest.writeInt(mVersionCode);
        dest.writeInt(mMinPlatformVersion);
    }
}
