/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.NumberPicker;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.TextLayoutView;
import org.json.JSONArray;
import org.json.JSONException;

@WidgetAnnotation(
        name = Picker.WIDGET_NAME,
        types = {@TypeAnnotation(name = TextPicker.TYPE_TEXT)},
        methods = {
                Picker.METHOD_SHOW,
                Component.METHOD_ANIMATE,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT
        })
public class TextPicker extends Picker {

    protected static final String TYPE_TEXT = "text";

    private Dialog mDialog;
    private NumberPicker mTextPicker;
    private String[] mRange;
    private int mSelectedIndex;
    private NumberPicker.OnValueChangeListener mOnValueChangeListener;

    public TextPicker(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    public void show() {
        if (mTextPicker == null || mRange == null || mRange.length == 0) {
            return;
        }
        mTextPicker.setValue(mSelectedIndex);
        if (mDialog == null) {
            mDialog =
                    new AlertDialog.Builder(mContext, getTheme())
                            .setView(mTextPicker)
                            .setPositiveButton(
                                    android.R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (mOnValueChangeListener != null && mRange != null
                                                    && mRange.length > 0) {
                                                mSelectedIndex = Math.max(0,
                                                        Math.min(mSelectedIndex,
                                                                mRange.length - 1));
                                                Map<String, Object> params = new HashMap<>();
                                                params.put("newSelected", mSelectedIndex);
                                                params.put("newValue", mRange[mSelectedIndex]);
                                                Map<String, Object> attributes = new HashMap<>();
                                                attributes.put("value", mSelectedIndex);
                                                mCallback.onJsEventCallback(
                                                        getPageId(),
                                                        mRef,
                                                        Attributes.Event.CHANGE,
                                                        TextPicker.this,
                                                        params,
                                                        attributes);
                                            }
                                        }
                                    })
                            .setNegativeButton(
                                    android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                            .setOnCancelListener(
                                    new DialogInterface.OnCancelListener() {

                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            cancelCallBack();
                                        }
                                    })
                            .create();
        }
        mDialog.show();
    }

    @Override
    protected TextLayoutView createViewImpl() {
        TextLayoutView textView = super.createViewImpl();
        mTextPicker = new NumberPicker(mContext);
        textView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        show();
                    }
                });
        mTextPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        return textView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.RANGE:
                String[] range = getRange(attribute);
                setRange(range);
                return true;
            case Attributes.Style.SELECTED:
                int selectedIndex = Attributes.getInt(mHapEngine, attribute, 0);
                setSelectedIndex(selectedIndex);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    public void setRange(String[] range) {
        mRange = range;
        if (mTextPicker == null) {
            return;
        }
        if (mRange != null && mRange.length > 0) {
            int minValue = mTextPicker.getMinValue();
            int oldMaxValue = mTextPicker.getMaxValue();
            int oldSpan = oldMaxValue - minValue + 1;
            int newMaxValue = mRange.length - 1;
            int newSpan = newMaxValue - minValue + 1;
            // To avoid ArrayIndexOutBoundsException,
            // when the size of the range gets larger,we first set the display values,
            // otherwise,we set the max value.
            if (newSpan > oldSpan) {
                mTextPicker.setDisplayedValues(mRange);
                mTextPicker.setMaxValue(mRange.length - 1);
            } else {
                mTextPicker.setMaxValue(mRange.length - 1);
                mTextPicker.setDisplayedValues(mRange);
            }
            mTextPicker.setMinValue(0);
            mSelectedIndex = mSelectedIndex < mRange.length ? mSelectedIndex : 0;
        } else {
            mTextPicker.setDisplayedValues(null);
            mTextPicker.setMinValue(0);
            mTextPicker.setMaxValue(0);
            mSelectedIndex = 0;
        }
        mTextPicker.setWrapSelectorWheel(true);
    }

    public void setSelectedIndex(int selectedIndex) {
        mSelectedIndex = Math.max(0, selectedIndex);
        if (mTextPicker != null) {
            mTextPicker.setValue(mSelectedIndex);
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (Attributes.Event.CHANGE.equals(event)) {
            if (mOnValueChangeListener == null) {
                mOnValueChangeListener =
                        new NumberPicker.OnValueChangeListener() {
                            @Override
                            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                                mSelectedIndex = newVal;
                            }
                        };
            }
            mTextPicker.setOnValueChangedListener(mOnValueChangeListener);
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
            mOnValueChangeListener = null;
            return true;
        } else if (Attributes.Event.CLICK.equals(event)) {
            return true;
        }

        return super.removeEvent(event);
    }

    private String[] getRange(Object rangeObject) {
        if (!(rangeObject instanceof JSONArray)) {
            return null;
        }
        JSONArray jsonArray = (JSONArray) rangeObject;
        String[] rangeArray = new String[((JSONArray) rangeObject).length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String item = jsonArray.getString(i);
                rangeArray[i] = item;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return rangeArray;
    }
}
