/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.utils;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.widgets.map.Map;
import org.hapjs.widgets.map.model.HybridLatLng;
import org.hapjs.widgets.map.model.MapCallout;
import org.hapjs.widgets.map.model.MapLabel;
import org.json.JSONException;
import org.json.JSONObject;

public class MapUtils {
    public static void checkIfPolylinesPointsIsLegal(List<HybridLatLng> points)
            throws IllegalArgumentException {
        if (points == null) {
            throw new IllegalArgumentException("points list can not be null");
        } else if (points.size() < 2) {
            throw new IllegalArgumentException("points count can not less than 2");
        } else if (points.contains((Object) null)) {
            throw new IllegalArgumentException("points list can not contains null");
        }
    }

    public static void checkIfPolygonsPointsIsLegal(List<HybridLatLng> points)
            throws IllegalArgumentException {
        if (points == null) {
            throw new IllegalArgumentException("points list can not be null");
        } else if (points.size() <= 2) {
            throw new IllegalArgumentException("points count can not less than three");
        } else if (points.contains((Object) null)) {
            throw new IllegalArgumentException("points list can not contains null");
        } else {
            Set<String> set = new HashSet<String>();
            for (HybridLatLng latLng : points) {
                if (latLng == null) {
                    continue;
                }
                set.add(latLng.getGenerateSymbol());
            }
            if (set.size() <= 2) {
                throw new IllegalArgumentException(
                        "points contain same item, which leads to points count less than three");
            }
        }
    }

    public static void checkLabelString(String label)
            throws IllegalArgumentException, JSONException {
        JSONObject object = new JSONObject(label);
        String content = object.optString(Map.CONTENT, null);
        if (TextUtils.isEmpty(content)) {
            throw new IllegalArgumentException("label must have its content.");
        }

        String textAlign = object.optString(Map.TEXT_ALIGN, MapLabel.TEXT_ALIGN_CENTER);
        if (!MapLabel.TEXT_ALIGN_LEFT.equals(textAlign)
                && !MapLabel.TEXT_ALIGN_CENTER.equals(textAlign)
                && !MapLabel.TEXT_ALIGN_RIGHT.equals(textAlign)) {
            throw new IllegalArgumentException(
                    "textAlign attr of label only supports [left, center, right].");
        }
    }

    public static void checkCalloutString(String callout)
            throws IllegalArgumentException, JSONException {
        JSONObject object = new JSONObject(callout);
        String content = object.optString(Map.CONTENT, null);
        if (TextUtils.isEmpty(content)) {
            throw new IllegalArgumentException("callout must have its content.");
        }
        String display = object.optString(Map.DISPLAY, MapCallout.DISPLAY_BY_CLICK);
        if (!MapCallout.DISPLAY_ALWAYS.equals(display)
                && !MapCallout.DISPLAY_BY_CLICK.equals((display))) {
            throw new IllegalArgumentException(
                    "display attr of callout only supports [byclick, always].");
        }
        String textAlign = object.optString(Map.TEXT_ALIGN, MapCallout.TEXT_ALIGN_CENTER);
        if (!MapCallout.TEXT_ALIGN_LEFT.equals(textAlign)
                && !MapCallout.TEXT_ALIGN_CENTER.equals(textAlign)
                && !MapCallout.TEXT_ALIGN_RIGHT.equals(textAlign)) {
            throw new IllegalArgumentException(
                    "textAlign attr of callout only supports [left, center, right].");
        }
    }

    public static int getColorFromJSONObject(
            JSONObject colorObj, @NonNull String colorKey, int defaultColor)
            throws IllegalArgumentException {
        int color = defaultColor;
        if (colorObj != null) {
            String colorValue = colorObj.optString(colorKey);
            if (!TextUtils.isEmpty(colorValue)) {
                color = ColorUtil.getColor(colorValue, defaultColor);
            }
        }
        return color;
    }
}
