/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class TextPicker extends Picker {

    public static final String TYPE_TEXT = "text";

    private NumberPicker mTextPicker;

    protected Dialog mDialog;
    protected String[] mRange;
    protected int mSelectedIndex;
    protected int mCandidateIndex;
    protected boolean mChangeCallbackEnabled = false;

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
        if (isShowing()) {
            mDialog.dismiss();
        }

        if (shouldShowDialog()) {
            return;
        }

        mDialog = createDialog();
        mDialog.show();
    }

    protected boolean shouldShowDialog() {
        return mRange == null || mRange.length == 0;
    }

    protected Dialog createDialog() {
        View textPicker = createPickerView();
        return new AlertDialog.Builder(mContext, getTheme())
                .setView(textPicker)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (mChangeCallbackEnabled && mRange != null && mRange.length > 0) {
                        changeCallback();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .setOnCancelListener(dialog -> cancelCallBack())
                .create();
    }

    protected View createPickerView() {
        mTextPicker = new NumberPicker(mContext);
        mTextPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mTextPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (mChangeCallbackEnabled) {
                mCandidateIndex = newVal;
            }
        });
        setRange(mRange);
        setSelectedIndex(mSelectedIndex);
        return mTextPicker;
    }

    protected void changeCallback() {
        mSelectedIndex = Math.max(0, Math.min(mCandidateIndex, mRange.length - 1));
        Map<String, Object> params = new HashMap<>();
        params.put("newSelected", mSelectedIndex);
        params.put("newValue", mRange[mSelectedIndex]);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", mSelectedIndex);
        mCallback.onJsEventCallback(getPageId(), mRef, Attributes.Event.CHANGE,
                TextPicker.this, params, attributes);
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
        switch (key) {
            case Attributes.Style.RANGE:
                setRange(getRange(attribute));
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
        mCandidateIndex = mSelectedIndex;
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
            mChangeCallbackEnabled = true;
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
            mChangeCallbackEnabled = false;
            return true;
        } else if (Attributes.Event.CLICK.equals(event)) {
            return true;
        }

        return super.removeEvent(event);
    }

    protected String[] getRange(Object rangeObject) {
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
