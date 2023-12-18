/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input;

import android.content.Context;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.FlexCheckBox;

@WidgetAnnotation(
        name = Edit.WIDGET_NAME,
        types = {@TypeAnnotation(name = CheckBox.TYPE_CHECKBOX)},
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class CheckBox extends Button {

    protected static final String TYPE_CHECKBOX = "checkbox";
    private static final String KEY_CHECK_EVENT_STATE = "check_event_state";
    private boolean isCheckEventRegistered = false;
    private String mName;
    private String mValue;

    public CheckBox(
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
        FlexCheckBox checkBox = new FlexCheckBox(mContext,isEnableTalkBack());
        checkBox.setComponent(this);
        initDefaultView(checkBox);
        initOnCheckedListener(checkBox);
        return checkBox;
    }

    private void initOnCheckedListener(FlexCheckBox checkBox) {
        if (checkBox == null) {
            return;
        }
        checkBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        changeAttrDomData(Attributes.Style.CHECKED, isChecked);
                        if (isCheckEventRegistered) {
                            Map<String, Object> params = new HashMap();
                            params.put("checked", isChecked);
                            params.put("name", mName);
                            params.put("value", mValue);
                            params.put("text", mValue);
                            Map<String, Object> attrs = new HashMap();
                            attrs.put("checked", isChecked);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.CHANGE, CheckBox.this,
                                    params, attrs);
                        }
                    }
                });
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.CHECKED:
                boolean checked = Attributes.getBoolean(attribute, false);
                setChecked(checked);
                return true;
            case Attributes.Style.NAME:
                mName = Attributes.getString(attribute, null);
                return true;
            case Attributes.Style.VALUE:
                mValue = Attributes.getString(attribute, null);
                if(isEnableTalkBack() && (mHost instanceof  FlexCheckBox)){
                    ((FlexCheckBox)mHost).setValue(mValue);
                }
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            isCheckEventRegistered = true;
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            isCheckEventRegistered = false;
            return true;
        }

        return super.removeEvent(event);
    }

    public void setChecked(boolean checked) {
        if (mHost == null) {
            return;
        }

        ((FlexCheckBox) mHost).setChecked(checked);
    }

    public void toggle() {
        if (mHost == null || !mHost.isEnabled()) {
            return;
        }

        ((FlexCheckBox) mHost).toggle();
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null || outState == null) {
            return;
        }
        outState.put(KEY_CHECK_EVENT_STATE, isCheckEventRegistered);
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }
        if (savedState.get(KEY_CHECK_EVENT_STATE) != null) {
            isCheckEventRegistered = (boolean) savedState.get(KEY_CHECK_EVENT_STATE);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            ((FlexCheckBox) mHost).setOnCheckedChangeListener(null);
        }
    }
}
