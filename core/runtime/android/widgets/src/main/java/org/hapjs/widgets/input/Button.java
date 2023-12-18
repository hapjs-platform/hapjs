/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;
import org.hapjs.widgets.view.text.FlexButton;

@WidgetAnnotation(
        name = Edit.WIDGET_NAME,
        types = {@TypeAnnotation(name = Button.TYPE_BUTTON)},
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Button extends Edit {

    protected static final String TYPE_BUTTON = "button";

    protected static final String DEFAULT_WIDTH = "128px";
    protected static final String DEFAULT_HEIGHT = "70px";

    public Button(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextView createViewImpl() {
        final FlexButton button = new FlexButton(mContext);
        button.setComponent(this);
        initDefaultView(button);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.btn_default_bg_selector);
        return button;
    }

    @Override
    protected void initDefaultView(TextView view) {
        Page page = initFontLevel();
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE, this));
        view.setTextColor(ColorUtil.getColor(DEFAULT_COLOR));

        int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH, this);
        view.setMinWidth(minWidth);
        view.setMinimumWidth(minWidth);

        int minHeight = Attributes.getInt(mHapEngine, DEFAULT_HEIGHT, this);
        view.setMinHeight(minHeight);
        view.setMinimumHeight(minHeight);
    }

    @Override
    public void setBackgroundColor(String colorStr) {
        super.setBackgroundColor(colorStr);

        if (mHost == null) {
            return;
        }

        mHost.setBackground(getOrCreateCSSBackground());
    }

    @Override
    protected int getDefaultVerticalGravity() {
        return Gravity.CENTER_VERTICAL;
    }
}
