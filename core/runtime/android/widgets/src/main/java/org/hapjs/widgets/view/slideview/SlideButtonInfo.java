/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.slideview;

import android.net.Uri;
import android.util.Log;

import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SlideButtonInfo {

    private static final String TAG = "SlideButtonInfo";

    public static final int UNDEFINE = Integer.MIN_VALUE;

    private static final String BUTTONS_ID = "id";
    private static final String BUTTONS_BUTTON_WIDTH = "buttonWidth";
    private static final String BUTTONS_ICON = "icon";
    private static final String BUTTONS_ICON_WIDTH = "iconWidth";
    private static final String BUTTONS_ICON_HEIGHT = "iconHeight";
    private static final String BUTTONS_ICON_BACKGROUND_COLOR = "iconBackgroundColor";
    private static final String BUTTONS_TEXT = "text";
    private static final String BUTTONS_TEXT_SIZE = "textSize";
    private static final String BUTTONS_TEXT_COLOR = "textColor";
    private static final String BUTTONS_BACKGROUND_COLOR = "backgroundColor";
    private static final String BUTTONS_BACKGROUND_TYPE = "backgroundType";
    private static final String BUTTONS_SECONDARY_CONFIRM = "secondaryConfirm";

    private static Set<String> mButtonsIdSet;

    public String id;
    public int buttonWidth;
    public Uri icon;
    public String text;
    public int iconWidth;
    public int iconHeight;
    public int iconBackgroundColor;
    public int textSize;
    public int textColor;
    public int backgroundColor;
    public String backgroundType;
    public SecondaryConfirmInfo secondaryConfirmInfo;

    private SlideButtonInfo() {
        this.id = null;
        this.buttonWidth = UNDEFINE;
        this.icon = null;
        this.text = null;
        this.iconWidth = UNDEFINE;
        this.iconHeight = UNDEFINE;
        this.iconBackgroundColor = UNDEFINE;
        this.textSize = UNDEFINE;
        this.textColor = UNDEFINE;
        this.backgroundColor = UNDEFINE;
        this.backgroundType = null;
        this.secondaryConfirmInfo = null;
    }

    public static List<SlideButtonInfo> parseButtonJson(Component component, String buttonsJson) {
        List<SlideButtonInfo> buttonsDataList = new ArrayList<>();
        try {
            mButtonsIdSet = null;
            JSONArray jsonArray = new JSONArray(buttonsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SlideButtonInfo holder = new SlideButtonInfo();
                for (Iterator<String> keys = jsonObject.keys(); keys.hasNext(); ) {
                    String key = keys.next().intern();
                    switch (key) {
                        case BUTTONS_ID:
                            String indexStr = jsonObject.optString(key);
                            holder.id = indexStr.trim();
                            break;
                        case BUTTONS_BUTTON_WIDTH:
                            String buttonWidthStr = jsonObject.optString(key);
                            holder.buttonWidth = Attributes.getInt(component.getHapEngine(), buttonWidthStr, UNDEFINE, component);
                            break;
                        case BUTTONS_ICON:
                            String iconStr = jsonObject.optString(key);
                            holder.icon = component.tryParseUri(iconStr.trim());
                            break;
                        case BUTTONS_ICON_WIDTH:
                            String iconWidthStr = jsonObject.optString(key);
                            holder.iconWidth = Attributes.getInt(component.getHapEngine(), iconWidthStr, UNDEFINE, component);
                            break;
                        case BUTTONS_ICON_HEIGHT:
                            String iconHeightStr = jsonObject.optString(key);
                            holder.iconHeight = Attributes.getInt(component.getHapEngine(), iconHeightStr, UNDEFINE, component);
                            break;
                        case BUTTONS_ICON_BACKGROUND_COLOR:
                            String iconBackgroundColorStr = jsonObject.optString(key);
                            holder.iconBackgroundColor = ColorUtil.getColor(iconBackgroundColorStr, UNDEFINE);
                            break;
                        case BUTTONS_TEXT:
                            String textStr = jsonObject.optString(key);
                            holder.text = textStr;
                            break;
                        case BUTTONS_TEXT_SIZE:
                            String textSizeStr = jsonObject.optString(key);
                            holder.textSize = Attributes.getFontSize(component.getHapEngine(), component.getPage(), textSizeStr, UNDEFINE, component);
                            break;
                        case BUTTONS_TEXT_COLOR:
                            String textColorStr = jsonObject.optString(key);
                            holder.textColor = ColorUtil.getColor(textColorStr, UNDEFINE);
                            break;
                        case BUTTONS_BACKGROUND_COLOR:
                            String backgroundColorStr = jsonObject.optString(key);
                            holder.backgroundColor = ColorUtil.getColor(backgroundColorStr, UNDEFINE);
                            break;
                        case BUTTONS_BACKGROUND_TYPE:
                            String backgroundTypeStr = jsonObject.optString(key);
                            holder.backgroundType = backgroundTypeStr.trim().toLowerCase();
                            break;
                        case BUTTONS_SECONDARY_CONFIRM:
                            String secondaryConfirmStr = jsonObject.optString(key);
                            holder.secondaryConfirmInfo = SecondaryConfirmInfo.parseSecondaryConfirmJson(component, secondaryConfirmStr);
                            break;
                    }
                }
                if (!checkButtonsIdLegal(holder.id)) {
                    throw new IllegalArgumentException("button's id can not be null and should be unique. button:" + holder.toString());
                }
                buttonsDataList.add(holder);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseButtonJson: JSONException=" + e);
            e.printStackTrace();
        }
        return buttonsDataList;
    }

    private static boolean checkButtonsIdLegal(String id) {
        if (id == null) {
            return false;
        }
        if (mButtonsIdSet == null) {
            mButtonsIdSet = new HashSet<>();
        } else {
            if (mButtonsIdSet.contains(id)) {
                return false;
            }
        }
        mButtonsIdSet.add(id);
        return true;
    }

    @Override
    public String toString() {
        return String.format("[index:%s, buttonWidth:%s, icon:%s, iconWidth:%s, iconHeight:%s, iconBackgroundColor:%s, text:%s, textSize:%s, textColor:%s, backgroundColor:%s, backgroundType:%s, secondaryConfirmDataHolder:%s]",
                id,
                buttonWidth == UNDEFINE ? "undefine" : buttonWidth,
                icon == null ? "undefine" : icon,
                iconWidth == UNDEFINE ? "undefine" : iconWidth,
                iconHeight == UNDEFINE ? "undefine" : iconHeight,
                iconBackgroundColor == UNDEFINE ? "undefine" : ColorUtil.getColorStr(iconBackgroundColor),
                text == null ? "undefine" : text,
                textSize == UNDEFINE ? "undefine" : textSize,
                textColor == UNDEFINE ? "undefine" : ColorUtil.getColorStr(textColor),
                backgroundColor == UNDEFINE ? "undefine" : ColorUtil.getColorStr(backgroundColor),
                backgroundType == null ? "undefine" : backgroundType,
                secondaryConfirmInfo == null ? "undefine" : secondaryConfirmInfo);
    }
}