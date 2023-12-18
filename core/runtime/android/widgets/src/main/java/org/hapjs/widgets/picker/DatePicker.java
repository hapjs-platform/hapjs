/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.TextLayoutView;

@WidgetAnnotation(
        name = Picker.WIDGET_NAME,
        types = {@TypeAnnotation(name = DatePicker.TYPE_DATE)},
        methods = {
                Picker.METHOD_SHOW,
                Component.METHOD_ANIMATE,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class DatePicker extends Picker {

    public static final String TYPE_DATE = "date";

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String MIN_DATE = "1970-01-01";
    private static final String MAX_DATE = "2100-12-31";
    private static final String TAG = "DatePicker";
    private SimpleDateFormat mDateFormat;
    private OnDateSelectListener mOnDateSelectListener;
    private Date mSelectedDate;

    protected Dialog mDialog;
    protected Calendar mCalendar;
    protected long mMinDate;
    protected long mMaxDate;

    public DatePicker(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mDateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date minDate = mDateFormat.parse(MIN_DATE);
            if (minDate != null) {
                mMinDate = minDate.getTime();
            }

            Date maxDate = mDateFormat.parse(MAX_DATE);
            if (maxDate != null) {
                mMaxDate = maxDate.getTime();
            }
        } catch (ParseException e) {
            Log.e(TAG, "init error", e);
        }
    }

    @Override
    public void show() {
        if (isShowing()) {
            mDialog.dismiss();
        }
        // check value
        if (mMinDate > mMaxDate) {
            mCallback.onJsException(
                    new IllegalArgumentException(
                            "start date must be less than or equal to end date"));
            return;
        }
        configCalendar();

        mDialog = createDialog(mOnDateSelectListener);
        mDialog.setOnCancelListener(dialog -> cancelCallBack());
        mDialog.show();
    }

    @Override
    protected boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    private void configCalendar() {
        mCalendar = Calendar.getInstance();
        if (mSelectedDate != null) {
            mCalendar.setTime(mSelectedDate);
        }
    }

    protected Dialog createDialog(OnDateSelectListener onDateSelectListener) {
        DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, dayOfMonth) -> {
            if (onDateSelectListener != null) {
                onDateSelectListener.onDateSelected(year, month, dayOfMonth);
            }
        };
        DatePickerDialog dialog = new DatePickerDialog(mContext, getTheme(), onDateSetListener,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        android.widget.DatePicker datePicker = dialog.getDatePicker();
        datePicker.setMinDate(mMinDate);
        datePicker.setMaxDate(mMaxDate);

        return dialog;
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textView = super.createViewImpl();
        textView.setOnClickListener(v -> show());

        return textView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.START:
                String startDate = Attributes.getString(attribute, MIN_DATE);
                setStartDate(startDate);
                return true;
            case Attributes.Style.END:
                String endDate = Attributes.getString(attribute, MAX_DATE);
                setEndDate(endDate);
                return true;
            case Attributes.Style.SELECTED:
                String selectedDate = Attributes.getString(attribute);
                setSelectedDate(selectedDate);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    public void setStartDate(String startDate) {
        if (TextUtils.isEmpty(startDate)) {
            return;
        }

        try {
            Date minDate = mDateFormat.parse(startDate);
            if (minDate != null) {
                mMinDate = minDate.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setEndDate(String endDate) {
        if (TextUtils.isEmpty(endDate)) {
            return;
        }

        try {
            Date maxDate = mDateFormat.parse(endDate);
            if (maxDate != null) {
                mMaxDate = maxDate.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setSelectedDate(String selectedDate) {
        if (TextUtils.isEmpty(selectedDate)) {
            mSelectedDate = null;
            return;
        }

        try {
            Date selectedDateTime = mDateFormat.parse(selectedDate);
            if (selectedDateTime != null
                    && selectedDateTime.getTime() >= mMinDate
                    && selectedDateTime.getTime() <= mMaxDate) {
                mSelectedDate = selectedDateTime;
            }
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
            mOnDateSelectListener = (year, month, dayOfMonth) -> {
                setSelectedDate(year + "-" + (month + 1) + "-" + dayOfMonth);
                Map<String, Object> params = new HashMap<>();
                params.put("year", year);
                params.put("month", month);
                params.put("day", dayOfMonth);
                mCallback.onJsEventCallback(
                        getPageId(), mRef, Attributes.Event.CHANGE, DatePicker.this,
                        params, null);
            };

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
            mOnDateSelectListener = null;
        } else if (Attributes.Event.CLICK.equals(event)) {
            return true;
        }

        return super.removeEvent(event);
    }

    public interface OnDateSelectListener {
        void onDateSelected(int year, int month, int dayOfMonth);
    }
}
