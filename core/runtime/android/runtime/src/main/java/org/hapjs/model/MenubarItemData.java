/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

public class MenubarItemData {
    public static final int TOP_ITEM_LOCATION_TAG = 0;
    public static final int BOTTOM_ITEM_LOCATION_TAG = 1;
    public static final int NAME_TAG = 1;
    public static final int DARWABLE_ID_TAG = 2;
    public static final int LOCATION_TAG = 3;
    public static final int SHOW_POINT_VALUE_TAG = 4;

    private String name;
    private int drawableId;
    private boolean isShowPoint;
    private boolean isNeedUpdate;
    private String key;
    private int locationTag;

    /**
     * @param nameStr
     * @param id
     * @param tag
     * @param showPoint 是否显示红点提示，对于显示红点提示的需要设置key
     */
    public MenubarItemData(String nameStr, int id, int tag, boolean showPoint) {
        this.name = nameStr;
        this.drawableId = id;
        this.locationTag = tag;
        this.isShowPoint = showPoint;
    }

    public boolean isNeedUpdate() {
        return isNeedUpdate;
    }

    public void setNeedUpdate(boolean needUpdate) {
        isNeedUpdate = needUpdate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getTag() {
        return locationTag;
    }

    public void setTag(int tag) {
        this.locationTag = tag;
    }

    public boolean isShowPoint() {
        return isShowPoint;
    }

    public void setShowPoint(boolean showPoint) {
        isShowPoint = showPoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public void setDrawableId(int drawableId) {
        this.drawableId = drawableId;
    }
}
