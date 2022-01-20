/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.ConfigurationManager;
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
                Component.METHOD_GET_BOUNDING_CLIENT_RECT
        })
public class TimePicker extends Picker {

    protected static final String TYPE_TIME = "time";

    private static final String TIME_PICKER = "HH:mm";

    private TimePickerDialog.OnTimeSetListener mOnTimeSetListener;
    private Date mSelectedTime;
    private TimePickerDialog mDialog;

    public TimePicker(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mConfigurationListener = new OnConfigurationListener(this);
        ConfigurationManager.getInstance().addListener(mConfigurationListener);
    }

    @Override
    public void show() {
        if (isShowing()) {
            mDialog.dismiss();
        }

        Calendar c = Calendar.getInstance();
        if (mSelectedTime != null) {
            c.setTime(mSelectedTime);
        }
        int themeRes = getTheme();
        if (themeRes == 0 && DarkThemeUtil.isDarkMode(mContext)) {
            themeRes = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        }
        mDialog =
                new TimePickerDialog(
                        mContext,
                        themeRes,
                        mOnTimeSetListener,
                        c.get(Calendar.HOUR_OF_DAY),
                        c.get(Calendar.MINUTE),
                        true);
        mDialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelCallBack();
                    }
                });
        mDialog.show();
    }

    @Override
    protected boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textView = super.createViewImpl();
        textView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        show();
                    }
                });

        return textView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.SELECTED:
                String selectedTime = Attributes.getString(attribute);
                setSelectedTime(selectedTime);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    public void setSelectedTime(String selectedTime) {
        if (TextUtils.isEmpty(selectedTime)) {
            mSelectedTime = null;
            return;
        }

        try {
            mSelectedTime = (new SimpleDateFormat(TIME_PICKER)).parse(selectedTime);
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
            mOnTimeSetListener =
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(android.widget.TimePicker view, int hour,
                                              int minute) {
                            setSelectedTime(hour + ":" + minute);
                            Map<String, Object> params = new HashMap<>();
                            params.put("hour", hour);
                            params.put("minute", minute);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.CHANGE, TimePicker.this,
                                    params, null);
                        }
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
            mOnTimeSetListener = null;
            return true;
        } else if (Attributes.Event.CLICK.equals(event)) {
            return true;
        }

        return super.removeEvent(event);
    }
}
