/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.slideview;

import android.text.TextUtils;
import android.util.Log;

import org.hapjs.common.json.JSONObject;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.json.JSONException;

import java.util.Iterator;

public class SecondaryConfirmInfo {

    private static final String TAG = "SecondaryConfirmInfo";

    public static final int UNDEFINE = Integer.MIN_VALUE;

    private static final String BUTTONS_SECONDARY_CONFIRM_TEXT = "text";
    private static final String BUTTONS_SECONDARY_CONFIRM_TEXT_SIZE = "textSize";
    private static final String BUTTONS_SECONDARY_CONFIRM_TEXT_COLOR = "textColor";

    public String text;
    public int textSize;
    public int textColor;

    private SecondaryConfirmInfo() {
        this.text = null;
        this.textSize = UNDEFINE;
        this.textColor = UNDEFINE;
    }

    public static SecondaryConfirmInfo parseSecondaryConfirmJson(Component component, String secondaryConfirmJson) {
        SecondaryConfirmInfo secondaryConfirmInfo = new SecondaryConfirmInfo();
        try {
            JSONObject jsonObject = new JSONObject(secondaryConfirmJson);
            for (Iterator<String> keys = jsonObject.keys(); keys.hasNext(); ) {
                String key = ((String) keys.next()).intern();
                switch (key) {
                    case BUTTONS_SECONDARY_CONFIRM_TEXT:
                        String textStr = jsonObject.optString(key);
                        secondaryConfirmInfo.text = textStr;
                        break;
                    case BUTTONS_SECONDARY_CONFIRM_TEXT_SIZE:
                        String textSizeStr = jsonObject.optString(key);
                        secondaryConfirmInfo.textSize = Attributes.getFontSize(component.getHapEngine(), component.getPage(), textSizeStr, UNDEFINE, component);
                        break;
                    case BUTTONS_SECONDARY_CONFIRM_TEXT_COLOR:
                        String textColorStr = jsonObject.optString(key);
                        secondaryConfirmInfo.textColor = ColorUtil.getColor(textColorStr, UNDEFINE);
                        break;
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "parseSecondaryConfirmJson: JSONException=" + e);
            e.printStackTrace();
        }
        return secondaryConfirmInfo;
    }

    @Override
    public String toString() {
        return String.format("[text:%s, textSize:%s, textColor:%s]",
                text == null ? "undefine" : text,
                textSize == UNDEFINE ? "undefine" : textSize,
                textColor == UNDEFINE ? "undefine" : ColorUtil.getColorStr(textColor));
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(this.text);
    }
}