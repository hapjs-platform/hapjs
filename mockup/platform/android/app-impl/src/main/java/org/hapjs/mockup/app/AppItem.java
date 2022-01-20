/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.app;

import java.io.File;
import org.hapjs.model.AppInfo;

public class AppItem {
    public static final int STATE_OK = 0;
    public static final int STATE_INSTALL_AVAILABLE = 1;
    public static final int STATE_UPDATE_AVAILABLE = 2;

    private String mName;
    private String mIconPath;
    private String mPackageName;
    private int mVersion;
    private int mState;
    private File mRpkFile;

    public AppItem(File rpkFile, AppInfo appInfo, int state) {
        this(
                rpkFile,
                appInfo.getName(),
                appInfo.getIcon(),
                appInfo.getPackage(),
                appInfo.getVersionCode(),
                state);
    }

    public AppItem(
            File rpkFile, String name, String iconPath, String packageName, int version,
            int state) {
        mRpkFile = rpkFile;
        mName = name;
        mIconPath = iconPath;
        mPackageName = packageName;
        mVersion = version;
        mState = state;
    }

    public String getName() {
        return mName;
    }

    public String getIconPath() {
        return mIconPath;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getVersion() {
        return mVersion;
    }

    public File getRpkFile() {
        return mRpkFile;
    }

    public int getState() {
        return mState;
    }
}
