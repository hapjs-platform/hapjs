/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import android.os.Parcel;
import android.os.Parcelable;

public class PreviewInfo implements Parcelable {
    public static final Creator<PreviewInfo> CREATOR =
            new Creator<PreviewInfo>() {
                @Override
                public PreviewInfo createFromParcel(Parcel in) {
                    return new PreviewInfo(in);
                }

                @Override
                public PreviewInfo[] newArray(int size) {
                    return new PreviewInfo[size];
                }
            };
    private String mId;
    private String mName;
    private String mIconUrl;
    private int mType;
    private int mOrientation;

    public PreviewInfo() {
    }

    protected PreviewInfo(Parcel in) {
        mId = in.readString();
        mName = in.readString();
        mIconUrl = in.readString();
        mType = in.readInt();
        mOrientation = in.readInt();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getIconUrl() {
        return mIconUrl;
    }

    public void setIconUrl(String iconUrl) {
        mIconUrl = iconUrl;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    @Override
    public String toString() {
        return "PreviewInfo(mId="
                + mId
                + ", mName="
                + mName
                + ", mIconUrl="
                + mIconUrl
                + ", mType="
                + mType
                + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeString(mIconUrl);
        dest.writeInt(mType);
        dest.writeInt(mOrientation);
    }
}
