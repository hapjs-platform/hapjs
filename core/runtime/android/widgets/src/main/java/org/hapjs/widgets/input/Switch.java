/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.FlexSwitch;

@WidgetAnnotation(
        name = Switch.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Switch extends Button {

    protected static final String WIDGET_NAME = "switch";

    private static final String KEY_CHECKED = "checked";
    private static final String KEY_CHECK_EVENT_STATE = "check_event_state";

    private static final String THUMB_COLOR = "thumbColor";
    private static final String TRACK_COLOR = "trackColor";

    private static final int DEFAULT_SWITCH_COLOR = 0xff009385;

    private boolean isCheckEventRegistered = false;

    private int mThumbColor = DEFAULT_SWITCH_COLOR;
    private int mTrackColor = DEFAULT_SWITCH_COLOR;

    public Switch(
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
        FlexSwitch flexSwitch = new FlexSwitch(mContext);
        flexSwitch.setComponent(this);
        initDefaultView(flexSwitch);
        initOnCheckedListener(flexSwitch);
        return flexSwitch;
    }

    private void initOnCheckedListener(FlexSwitch flexSwitch) {
        if (flexSwitch == null) {
            return;
        }
        flexSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        changeAttrDomData(Attributes.Style.CHECKED, isChecked);
                        if (isCheckEventRegistered) {
                            Map<String, Object> params = new HashMap();
                            params.put("checked", isChecked);
                            Map<String, Object> attributes = new HashMap();
                            attributes.put("checked", isChecked);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.CHANGE, Switch.this, params,
                                    attributes);
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
            case THUMB_COLOR:
                String thumbColorStr =
                        Attributes.getString(attribute, ColorUtil.getColorStr(mThumbColor));
                setThumbColor(thumbColorStr);
                return true;
            case TRACK_COLOR:
                String trackColorStr =
                        Attributes.getString(attribute, ColorUtil.getColorStr(mTrackColor));
                setTrackColor(trackColorStr);
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
        ((android.widget.Switch) mHost).setChecked(checked);
    }

    private void setThumbColor(String thumbColorStr) {
        if (TextUtils.isEmpty(thumbColorStr) || mHost == null) {
            return;
        }
        mThumbColor = ColorUtil.getColor(thumbColorStr, mThumbColor);
        ((android.widget.Switch) mHost)
                .getThumbDrawable()
                .setColorFilter(new PorterDuffColorFilter(mThumbColor, PorterDuff.Mode.SRC_IN));
    }

    private void setTrackColor(String trackColorStr) {
        if (TextUtils.isEmpty(trackColorStr) || mHost == null) {
            return;
        }
        mTrackColor = ColorUtil.getColor(trackColorStr, mTrackColor);
        // 系统会默认添加30%（0.3 * ff = 4D）的透明度
        ((android.widget.Switch) mHost)
                .getTrackDrawable()
                .setColorFilter(new PorterDuffColorFilter(mTrackColor, PorterDuff.Mode.SRC_IN));
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null) {
            return;
        }
        outState.put(KEY_CHECK_EVENT_STATE, isCheckEventRegistered);
        outState.put(KEY_CHECKED, ((android.widget.Switch) mHost).isChecked());
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }

        if (savedState.get(KEY_CHECKED) != null) {
            setChecked((boolean) savedState.get(KEY_CHECKED));
        }
        if (savedState.get(KEY_CHECK_EVENT_STATE) != null) {
            isCheckEventRegistered = (boolean) savedState.get(KEY_CHECK_EVENT_STATE);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            ((FlexSwitch) mHost).setOnCheckedChangeListener(null);
        }
    }
}
