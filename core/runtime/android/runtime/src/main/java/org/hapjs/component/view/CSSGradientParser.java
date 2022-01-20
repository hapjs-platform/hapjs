/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.view.drawable.LinearGradientDrawable;
import org.hapjs.runtime.HapEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CSSGradientParser {

    // type
    static final String TYPE_LINEAR_GRADIENT = "linearGradient";
    static final String TYPE_REPEATING_LINEAR_GRADIENT = "repeatingLinearGradient";
    // tag
    static final String TAG_TYPE = "type";
    static final String TAG_DIRECTIONS = "directions";
    static final String TAG_VALUES = "values";
    private static final String TAG = "CSSGradientParser";

    private CSSGradientParser() {
    }

    public static Drawable parseGradientDrawable(HapEngine hapEngine, String bgImageStr) {
        if (TextUtils.isEmpty(bgImageStr)) {
            return null;
        }
        Drawable drawable = null;
        try {
            JSONObject paramObject = new JSONObject(bgImageStr);
            if (paramObject == null || paramObject.length() == 0) {
                return null;
            }
            JSONArray paramArray = paramObject.getJSONArray(TAG_VALUES);
            if (paramArray == null || paramArray.length() == 0) {
                return null;
            }
            JSONObject bgImageObject;
            List<Drawable> bgImageList = new ArrayList<>();
            Drawable childDrawable = null;
            for (int index = 0; index < paramArray.length(); index++) {
                bgImageObject = paramArray.getJSONObject(index);
                String type = bgImageObject.getString(TAG_TYPE);
                switch (type) {
                    case TYPE_LINEAR_GRADIENT: {
                        childDrawable =
                                new LinearGradientDrawable(
                                        hapEngine,
                                        parseLinearGradientDirection(bgImageObject),
                                        parseGradientColorStop(bgImageObject));
                        break;
                    }
                    case TYPE_REPEATING_LINEAR_GRADIENT: {
                        childDrawable =
                                new LinearGradientDrawable(
                                        hapEngine,
                                        parseLinearGradientDirection(bgImageObject),
                                        parseGradientColorStop(bgImageObject))
                                        .setMode(Shader.TileMode.REPEAT);
                        break;
                    }
                    default:
                        break;
                }
                if (childDrawable != null) {
                    bgImageList.add(childDrawable);
                }
            }
            if (bgImageList.size() == 0 || bgImageList.size() == 1) {
                return childDrawable;
            }
            Drawable[] drawables = new Drawable[bgImageList.size()];
            for (int index = 0; index < bgImageList.size(); index++) {
                drawables[index] = bgImageList.get(index);
            }
            drawable = new LayerDrawable(drawables);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Error background json");
        }
        return drawable;
    }

    private static List<ColorStop> parseGradientColorStop(JSONObject paramObject)
            throws JSONException {
        JSONArray colorArray = paramObject.getJSONArray(TAG_VALUES);
        List<ColorStop> colorStopList = new ArrayList<>();
        if (colorArray != null && colorArray.length() >= 2) {
            for (int index = 0; index < colorArray.length(); index++) {
                String colorStopStr = colorArray.getString(index);
                if (TextUtils.isEmpty(colorStopStr)) {
                    continue;
                }
                colorStopStr = colorStopStr.trim();
                String[] colorStopStrSplit = colorStopStr.split(" ");
                ColorStop subColorStop = null;
                if (colorStopStrSplit.length == 1) {
                    subColorStop = new ColorStop();
                    subColorStop.mPosition = Float.toString(1f / (colorArray.length() - 1) * index);

                } else if (colorStopStrSplit.length == 2) {
                    if (colorStopStrSplit[1].endsWith("%") || colorStopStrSplit[1].endsWith("px")) {
                        subColorStop = new ColorStop();
                        subColorStop.mPosition = colorStopStrSplit[1].trim();
                        subColorStop.isDefaultPosition = false;
                    }
                }
                if (subColorStop != null) {
                    subColorStop.mColor = ColorUtil.getColor(colorStopStrSplit[0]);
                    colorStopList.add(subColorStop);
                }
            }
        }
        return colorStopList;
    }

    private static List<String> parseLinearGradientDirection(JSONObject object)
            throws JSONException {
        JSONArray directionArray = object.getJSONArray(TAG_DIRECTIONS);
        List<String> direcs = new ArrayList<>();
        if (directionArray != null) {
            for (int index = 0; index < directionArray.length(); index++) {
                direcs.add(directionArray.getString(index));
            }
        }
        return direcs;
    }

    public static class ColorStop {
        public int mColor;
        public String mPosition;
        public boolean isDefaultPosition = true;
    }
}
