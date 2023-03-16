/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.model;

import android.net.Uri;

public class TabBarInfo {
    public static final String KEY_TABBAR_PAGE_PATH = "pagePath";
    public static final String KEY_TABBAR_PAGEPARAMS = "pageParams";
    public static final String KEY_TABBAR_ICONPATH = "iconPath";
    public static final String KEY_TABBAR_SELECTEDICONPATH = "selectedIconPath";
    public static final String KEY_TABBAR_TEXT = "text";
    public static final String KEY_TABBAR_INDEX = "index";

    public String getTabBarPagePath() {
        return mTabBarPagePath;
    }

    public void setTabBarPagePath(String mTabBarPagePath) {
        this.mTabBarPagePath = mTabBarPagePath;
    }

    public String getTabBarIconPath() {
        return mTabBarIconPath;
    }

    public void setTabBarIconPath(String mTabBarIconPath) {
        this.mTabBarIconPath = mTabBarIconPath;
    }

    public String getTabBarSelectedIconPath() {
        return mTabBarSelectedIconPath;
    }

    public void setTabBarSelectedIconPath(String mTabBarSelectedIconPath) {
        this.mTabBarSelectedIconPath = mTabBarSelectedIconPath;
    }

    public String getTabBarText() {
        return mTabBarText;
    }

    public void setTabBarText(String mTabBarText) {
        this.mTabBarText = mTabBarText;
    }


    public Uri getTabBarIconUri() {
        return mTabBarIconUri;
    }

    public void setTabBarIconUri(Uri mTabBarIconUri) {
        this.mTabBarIconUri = mTabBarIconUri;
    }

    public Uri getTabBarSelectedIconUri() {
        return mTabBarSelectedIconUri;
    }

    public void setTabBarSelectedIconUri(Uri mTabBarSelectedIconUri) {
        this.mTabBarSelectedIconUri = mTabBarSelectedIconUri;
    }

    public String getTabBarPageParams() {
        return mTabBarPageParams;
    }

    public void setTabBarPageParams(String mTabBarPageParams) {
        this.mTabBarPageParams = mTabBarPageParams;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean mIsSelected) {
        this.mIsSelected = mIsSelected;
    }

    public String mTabBarPagePath;
    public String mTabBarPageParams;
    public String mTabBarIconPath;
    public String mTabBarSelectedIconPath;
    public String mTabBarText;
    public Uri mTabBarIconUri;
    public Uri mTabBarSelectedIconUri;
    public boolean mIsSelected;
}
