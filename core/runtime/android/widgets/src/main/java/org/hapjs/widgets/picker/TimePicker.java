/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.TextLayoutView;

@WidgetAnnotation(
        name = Picker.WIDGET_NAME,
        types = {@TypeAnnotation(name = TimePicker.TYPE_TIME)},
        methods = {
                Picker.METHOD_SHOW,
                Component.METHOD_ANIMATE,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class TimePicker extends Picker {

    public static final String TYPE_TIME = "time";

    private static final String TIME_PATTERN = "HH:mm";
    protected OnTimeSelectListener mOnTimeSelectListener;
    private Date mSelectedTime;

    protected Dialog mDialog;
    protected Calendar mCalendar;

    public TimePicker(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        setConfigurationListener();
    }

    @Override
    public void show() {
        if (isShowing()) {
            mDialog.dismiss();
        }
        configCalendar();

        mDialog = createDialog(mOnTimeSelectListener);
        mDialog.setOnCancelListener(dialog -> cancelCallBack());
        mDialog.show();
    }

    private void configCalendar() {
        mCalendar = Calendar.getInstance();
        if (mSelectedTime != null) {
            mCalendar.setTime(mSelectedTime);
        }
    }

    protected Dialog createDialog(OnTimeSelectListener onTimeSelectListener) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hour, minute) -> {
            if (onTimeSelectListener != null) {
                onTimeSelectListener.onTimeSelected(hour, minute);
            }
        };

        int themeRes = getTheme();
        if (themeRes == 0 && DarkThemeUtil.isDarkMode(mContext)) {
            themeRes = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        }
        return new TimePickerDialog(
                mContext,
                themeRes,
                onTimeSetListener,
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                true);
    }

    @Override
    protected boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textView = super.createViewImpl();
        textView.setOnClickListener(v -> show());

        return textView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        if (Attributes.Style.SELECTED.equals(key)) {
            String selectedTime = Attributes.getString(attribute);
            setSelectedTime(selectedTime);
            return true;
        }

        return super.setAttribute(key, attribute);
    }

    public void setSelectedTime(String selectedTime) {
        if (TextUtils.isEmpty(selectedTime)) {
            mSelectedTime = null;
            return;
        }

        try {
            mSelectedTime = new SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).parse(selectedTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event)) {
            return true;
        }
        if (Attributes.Event.CHANGE.equals(event)) {
            mOnTimeSelectListener = (hour, minute) -> {
                setSelectedTime(hour + ":" + minute);
                Map<String, Object> params = new HashMap<>();
                params.put("hour", hour);
                params.put("minute", minute);
                mCallback.onJsEventCallback(
                        getPageId(), mRef, Attributes.Event.CHANGE, TimePicker.this,
                        params, null);
            };
            return true;
        } else if (Attributes.Event.CLICK.equals(event)) {
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event)) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            mOnTimeSelectListener = null;
            return true;
        } else if (Attributes.Event.CLICK.equals(event)) {
            return true;
        }

        return super.removeEvent(event);
    }

    public interface OnTimeSelectListener {
        void onTimeSelected(int hour, int minute);
    }
}
