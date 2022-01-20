/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.os.Build;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.text.Text;
import org.hapjs.widgets.view.text.TextLayoutView;

@WidgetAnnotation(name = Option.WIDGET_NAME)
public class Option extends Text {

    protected static final String WIDGET_NAME = "option";

    // attribute
    private static final String SELECTED = "selected";

    private boolean mSelected;
    private String mValue;
    private boolean forceDarkAllowed = true;

    public Option(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textView = new TextLayoutView(mContext);
        textView.setComponent(this);
        return textView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case SELECTED:
                boolean selected = Attributes.getBoolean(attribute, false);
                setSelected(selected);
                return true;
            case Attributes.Style.VALUE:
                String value = Attributes.getString(attribute);
                setValue(value);
                return true;
            case Attributes.Style.FORCE_DARK:
                forceDarkAllowed = Attributes.getBoolean(attribute, true);
                return super.setAttribute(key, attribute);
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public void setSelected(boolean selected) {
        if (selected == mSelected) {
            return;
        }
        mSelected = selected;
        Select select = (Select) mParent;
        select.onSelectionChange(this, selected);
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value;
    }

    @Override
    public void setFontSize(int fontSize) {
        super.setFontSize(fontSize);
        mLayoutBuilder.setTextSpacingExtra(0);
    }

    public void setSelfForceDarkAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mHost.setForceDarkAllowed(forceDarkAllowed);
        }
    }
}
