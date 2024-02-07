/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.analyzer.model;

import android.os.Parcel;
import android.os.Parcelable;

public class LogData implements Parcelable {
    public @LogPackage.LogLevel
    int mLevel;
    public @LogPackage.LogType
    int mType;
    public String mContent;

    public LogData(@LogPackage.LogLevel int level, @LogPackage.LogType int type, String content) {
        mLevel = level;
        mType = type;
        mContent = content;
    }

    protected LogData(Parcel in) {
        mLevel = in.readInt();
        mType = in.readInt();
        mContent = in.readString();
    }

    public static final Creator<LogData> CREATOR = new Creator<LogData>() {
        @Override
        public LogData createFromParcel(Parcel in) {
            return new LogData(in);
        }

        @Override
        public LogData[] newArray(int size) {
            return new LogData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mLevel);
        dest.writeInt(mType);
        dest.writeString(mContent);
    }
}
