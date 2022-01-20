/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.map.Map;
import org.hapjs.widgets.map.utils.MapUtils;
import org.json.JSONObject;

public class MapLabel {

    public static final String TEXT_ALIGN_LEFT = "left";
    public static final String TEXT_ALIGN_CENTER = "center";
    public static final String TEXT_ALIGN_RIGHT = "right";

    public static final int DEFAULT_COLOR = Color.parseColor("#000000");
    public static final String DEFAULT_FONT_SIZE = "30px";
    public static final String DEFAULT_LENGTH = "0px";
    public static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#00ffffff");
    public static final String DEFAULT_ANCHOR = "0";

    public String content;
    public int color;
    public int fontSize;
    public int borderRadius;
    public Rect padding;
    public int backgroundColor;
    public String textAlign;
    public int xOffset;
    public int yOffset;

    public static MapLabel parseMapLabel(HapEngine hapEngine, JSONObject object) {
        MapLabel mapLabel = new MapLabel();
        mapLabel.content = object.optString(Map.CONTENT, null);
        if (TextUtils.isEmpty(mapLabel.content)) {
            return null;
        }
        mapLabel.xOffset =
                Attributes.getInt(hapEngine,
                        object.optString(Map.ANCHOR_X, MapLabel.DEFAULT_ANCHOR));
        mapLabel.yOffset =
                Attributes.getInt(hapEngine,
                        object.optString(Map.ANCHOR_Y, MapLabel.DEFAULT_ANCHOR));
        mapLabel.color = MapUtils.getColorFromJSONObject(object, Map.COLOR, MapLabel.DEFAULT_COLOR);
        mapLabel.fontSize =
                Attributes.getInt(hapEngine,
                        object.optString(Map.FONT_SIZE, MapLabel.DEFAULT_FONT_SIZE));
        mapLabel.borderRadius =
                Attributes.getInt(hapEngine,
                        object.optString(Map.BORDER_RADIUS, MapLabel.DEFAULT_LENGTH));
        mapLabel.padding =
                Map.parsePadding(hapEngine,
                        object.optString(Map.PADDING, MapLabel.DEFAULT_LENGTH));
        mapLabel.backgroundColor =
                MapUtils.getColorFromJSONObject(
                        object, Map.BACKGROUND_COLOR, MapLabel.DEFAULT_BACKGROUND_COLOR);
        mapLabel.textAlign = object.optString(Map.TEXT_ALIGN, MapLabel.TEXT_ALIGN_CENTER);
        return mapLabel;
    }

    public View getView(Context context) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        TextView textView = new TextView(context);
        ViewGroup.LayoutParams l =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(l);
        textView.setText(content);
        textView.setTextColor(color);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        float[] outerR =
                new float[] {
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius,
                        borderRadius
                };
        ShapeDrawable drawable = new ShapeDrawable(new RoundRectShape(outerR, null, null));
        drawable.setPadding(new Rect(padding.left, padding.top, padding.right, padding.bottom));
        drawable.getPaint().setColor(backgroundColor);
        drawable.getPaint().setAntiAlias(true);
        drawable.getPaint().setStyle(Paint.Style.FILL);
        textView.setBackground(drawable);

        switch (textAlign) {
            case TEXT_ALIGN_LEFT:
                textView.setGravity(Gravity.START);
                break;
            case TEXT_ALIGN_CENTER:
                textView.setGravity(Gravity.CENTER);
                break;
            case TEXT_ALIGN_RIGHT:
                textView.setGravity(Gravity.END);
                break;
            default:
                textView.setGravity(Gravity.CENTER);
                break;
        }

        return textView;
    }
}
