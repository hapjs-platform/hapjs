/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.hapjs.runtime.Runtime;

public class MapCallout {

    public static final String DISPLAY_BY_CLICK = "byclick";
    public static final String DISPLAY_ALWAYS = "always";

    public static final String TEXT_ALIGN_LEFT = "left";
    public static final String TEXT_ALIGN_CENTER = "center";
    public static final String TEXT_ALIGN_RIGHT = "right";

    public static final int DEFAULT_COLOR = Color.parseColor("#000000");
    public static final String DEFAULT_FONT_SIZE = "30px";
    public static final String DEFAULT_LENGTH = "0px";
    public static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#ffffff");

    public String content;
    public int color;
    public int fontSize;
    public int borderRadius;
    public Rect padding;
    public int backgroundColor;
    public String display;
    public String textAlign;
    public boolean isConvertHtml;

    public View getView(Context context) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        TextView textView = new TextView(context);
        ViewGroup.LayoutParams l =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(l);
        if (isConvertHtml) {
            Spanned result;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(content);
            }
            textView.setText(result);
        } else {
            textView.setText(content);
        }
        textView.setTextColor(color);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);

        ShapeDrawable drawable = new ShapeDrawable(new BubbleRoundRectShape(borderRadius));
        drawable.setPadding(
                new Rect(
                        padding.left,
                        padding.top,
                        padding.right,
                        (int) (padding.bottom + BubbleRoundRectShape.TRIANGLE_HEIGHT)));
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

    static class BubbleRoundRectShape extends Shape {

        public static final float TRIANGLE_WIDTH;
        public static final float TRIANGLE_HEIGHT;

        static {
            // 定义callout气泡下方的倒三角尺寸（10dp * 10dp）
            float density =
                    Runtime.getInstance().getContext().getResources().getDisplayMetrics().density;
            TRIANGLE_WIDTH = 10 * density + 0.5f;
            TRIANGLE_HEIGHT = 10 * density + 0.5f;
        }

        private float mRadii;
        private Path mPath;

        public BubbleRoundRectShape(float radii) {

            mRadii = radii;
            mPath = new Path();
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            canvas.drawPath(mPath, paint);
        }

        @Override
        protected void onResize(float w, float h) {

            super.onResize(w, h);

            mPath.reset();
            float triangleWidth = TRIANGLE_WIDTH;
            float triangleHeight = TRIANGLE_HEIGHT;
            float dia = mRadii * 2;

            if (dia + triangleHeight > h) {
                dia = h - triangleHeight;
            }
            if (dia + triangleWidth > w) {
                dia = w - triangleWidth;
            }

            mPath.moveTo(w / 2, h);
            mPath.lineTo(w / 2 - triangleWidth / 2, h - triangleHeight);
            RectF leftBottom = new RectF(0, h - triangleHeight - dia, dia, h - triangleHeight);
            mPath.arcTo(leftBottom, 90, 90);
            RectF leftTop = new RectF(0, 0, dia, dia);
            mPath.arcTo(leftTop, 180, 90);
            RectF rightTop = new RectF(w - dia, 0, w, dia);
            mPath.arcTo(rightTop, -90, 90);
            RectF rightBottom = new RectF(w - dia, h - triangleHeight - dia, w, h - triangleHeight);
            mPath.arcTo(rightBottom, 0, 90);
            mPath.lineTo(w / 2 + triangleWidth / 2, h - triangleHeight);
            mPath.close();
        }

        @Override
        public BubbleRoundRectShape clone() throws CloneNotSupportedException {
            final BubbleRoundRectShape shape = (BubbleRoundRectShape) super.clone();
            shape.mRadii = mRadii;
            shape.mPath = new Path(mPath);
            return shape;
        }
    }
}
