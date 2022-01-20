/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import org.hapjs.component.Component;
import org.hapjs.runtime.R;

public class UnsupportedView extends FrameLayout implements ComponentHost {

    private static final int TEXT_SIZE_SP = 13;
    private final TextPaint mPaint;
    private String mWidgetName;
    private Component mComponent;

    public UnsupportedView(@NonNull Context context) {
        super(context);
        mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        mPaint.setTextAlign(Paint.Align.CENTER);
        int textSize =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP,
                                TEXT_SIZE_SP,
                                context.getResources().getDisplayMetrics());
        mPaint.setTextSize(textSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!TextUtils.isEmpty(mWidgetName)) {
            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            float distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
            float baseline = getMeasuredHeight() / 2 + distance;
            canvas.drawText(
                    String.format(getContext().getString(R.string.unsupported_element_tip),
                            mWidgetName),
                    getMeasuredWidth() / 2,
                    baseline,
                    mPaint);
        }
    }

    public void setWidgetName(String widgetName) {
        mWidgetName = widgetName;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }
}
