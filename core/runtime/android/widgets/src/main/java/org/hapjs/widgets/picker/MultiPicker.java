/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.hapjs.widgets.view.picker.WheelView;
import org.hapjs.widgets.view.text.TextLayoutView;
import org.json.JSONArray;
import org.json.JSONException;

@WidgetAnnotation(
        name = Picker.WIDGET_NAME,
        types = {@TypeAnnotation(name = MultiPicker.TYPE_MULTI_TEXT)},
        methods = {
                Picker.METHOD_SHOW,
                Component.METHOD_ANIMATE,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT
        })
public class MultiPicker extends Picker {

    protected static final String TYPE_MULTI_TEXT = "multi-text";

    // event
    private static final String COLUMNCHANGE = "columnchange";

    private static final float LINE_SPACE_MULTIPLIER = 2.0F;
    private static final int PADDING = 0;
    private static final int SIZE = 18;
    private static final int COLOR_FOCUS = 0XFF000000;
    private static final int COLOR_NORMAL = 0XFFBBBBBB;
    private static final int ITEM_OFF_SET = 3;

    private Dialog mDialog;
    private LinearLayout mLinearLayout;
    private List<WheelView> mNumberPickers = new ArrayList<>();
    private List<List<String>> mRange;
    private int[] mSelectedIndex;
    private boolean hasChangeListener = false;
    private boolean hasColumnChangeListener = false;

    private WheelView.DividerConfig dividerConfig = new WheelView.DividerConfig();

    public MultiPicker(
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
        mLinearLayout.removeAllViews();
        ViewGroup parent =
                mLinearLayout.getParent() == null ? null : (ViewGroup) mLinearLayout.getParent();
        if (parent != null) {
            parent.removeView(mLinearLayout);
        }

        if (mRange != null && mRange.size() > 0 && mNumberPickers.size() > 0) {
            Iterator<WheelView> iterator = mNumberPickers.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                WheelView numberPicker = iterator.next();
                if (numberPicker != null) {
                    if (index >= mRange.size()) {
                        iterator.remove();
                    } else {
                        ViewGroup.LayoutParams params = numberPicker.getLayoutParams();
                        if (params == null) {
                            params = new LinearLayout.LayoutParams(0,
                                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
                        }
                        mLinearLayout.addView(numberPicker, params);
                        if (index < mSelectedIndex.length) {
                            numberPicker.setSelectedIndex(mSelectedIndex[index]);
                        } else {
                            numberPicker.setSelectedIndex(0);
                        }
                    }
                }
                index++;
            }
        }

        int themeRes = getTheme();
        if (themeRes == 0 && DarkThemeUtil.isDarkMode(mContext)) {
            themeRes = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        }
        mDialog =
                new AlertDialog.Builder(mContext, themeRes)
                        .setView(mLinearLayout)
                        .setPositiveButton(
                                android.R.string.ok,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        changeCallBack();
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

        mLinearLayout = new LinearLayout(mContext);
        mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        mLinearLayout.setGravity(Gravity.CENTER);

        return textView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.RANGE:
                setRange(getRanges(attribute));
                return true;
            case Attributes.Style.SELECTED:
                setSelectedIndex(getSelectedIndex(attribute));
                return true;
            case Attributes.Style.VALUE:
                super.setAttribute(key, getValue(attribute));
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    private void setRange(List<List<String>> range) {
        mRange = range;
        if (mRange != null && mRange.size() > 0) {
            int mCols = mRange.size();
            if (mSelectedIndex == null) {
                mSelectedIndex = new int[mCols];
            } else if (mSelectedIndex.length != mCols) {
                mSelectedIndex = Arrays.copyOf(mSelectedIndex, mCols);
            }
            WheelView numberPicker = null;
            for (int i = 0; i < mCols; i++) {
                if (i < mNumberPickers.size()) {
                    numberPicker = mNumberPickers.get(i);
                    numberPicker.setTag(i);
                }
                if (numberPicker == null) {
                    numberPicker = createNumberPicker(i);
                    mNumberPickers.add(i, numberPicker);
                }
                List<String> columns = mRange.get(i);

                if (columns != null && columns.size() > 0) {
                    numberPicker.setDisplayedValues(columns); // 设置显示数据

                    mSelectedIndex[i] = mSelectedIndex[i] < columns.size() ? mSelectedIndex[i] : 0;
                    numberPicker.setSelectedIndex(mSelectedIndex[i]);
                } else {
                    numberPicker.setDisplayedValues(null);
                    numberPicker.setSelectedIndex(0);
                    mSelectedIndex[i] = 0;
                }
                numberPicker = null;
            }
        } else {
            if (mNumberPickers.size() > 0) {
                for (int i = 0; i < mNumberPickers.size(); i++) {
                    WheelView numberPicker = mNumberPickers.get(i);
                    if (numberPicker != null) {
                        numberPicker.setDisplayedValues(null);
                        numberPicker.setSelectedIndex(0);
                        if (mSelectedIndex != null && i < mSelectedIndex.length) {
                            mSelectedIndex[i] = 0;
                        }
                    }
                }
            }
        }
    }

    private WheelView createNumberPicker(int tag) {
        WheelView wheelView = new WheelView(mContext);

        wheelView.setTag(tag);
        wheelView.setLineSpace(LINE_SPACE_MULTIPLIER);
        wheelView.setTextPadding(PADDING);
        wheelView.setTextSize(SIZE);
        wheelView.setTypeface(Typeface.DEFAULT);
        wheelView.setTextColor(COLOR_NORMAL, COLOR_FOCUS);
        wheelView.setDivider(dividerConfig);
        wheelView.setOffset(ITEM_OFF_SET);

        wheelView.setOnItemSelectListener(
                new WheelView.OnItemSelectListener() {

                    @Override
                    public void onSelected(WheelView wheelView, int index) {
                        columnChangeCallBack(wheelView, index);
                    }
                });

        return wheelView;
    }

    private void columnChangeCallBack(WheelView picker, int newVal) {
        if (mRange != null && mRange.size() > 0) {
            int tag = (int) picker.getTag();
            if (tag >= 0 && tag < mRange.size()) {
                List<String> columns = mRange.get(tag);
                if (columns != null && columns.size() > 0) {
                    mSelectedIndex[tag] = Math.max(0, Math.min(newVal, columns.size() - 1));
                    picker.setSelectedIndex(mSelectedIndex[tag]);

                    if (hasColumnChangeListener) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("column", tag);
                        params.put("newSelected", mSelectedIndex[tag]);
                        params.put("newValue", columns.get(mSelectedIndex[tag]));

                        mCallback.onJsEventCallback(
                                getPageId(), mRef, COLUMNCHANGE, MultiPicker.this, params, null);
                    }
                }
            }
        }
    }

    private void changeCallBack() {
        if (hasChangeListener && mRange != null && mRange.size() > 0) {
            Map<String, Object> params = new HashMap<>();
            List<String> valueArray = new ArrayList<>();
            List<Integer> selectArray = new ArrayList<>();
            int index;
            for (int i = 0; i < mRange.size(); i++) {
                List<String> columns = mRange.get(i);
                if (columns != null && columns.size() > 0) {
                    index = Math.max(0, Math.min(mSelectedIndex[i], columns.size() - 1));
                    mSelectedIndex[i] = index;
                    valueArray.add(columns.get(index));
                    selectArray.add(index);
                }
            }
            params.put("newValue", valueArray);
            params.put("newSelected", selectArray);

            mCallback.onJsEventCallback(
                    getPageId(), mRef, Attributes.Event.CHANGE, MultiPicker.this, params, null);
        }
    }

    private void setSelectedIndex(int[] selectedIndex) {
        if (selectedIndex == null || selectedIndex.length <= 0) {
            return;
        }

        if (mRange != null && mRange.size() > 0 && selectedIndex.length != mRange.size()) {
            return;
        }

        mSelectedIndex = selectedIndex;

        if (mNumberPickers.size() > 0) {
            for (int i = 0; i < mNumberPickers.size(); i++) {
                WheelView numberPicker = mNumberPickers.get(i);
                if (numberPicker != null && i < mSelectedIndex.length) {
                    if (i < mSelectedIndex.length) {
                        numberPicker.setSelectedIndex(mSelectedIndex[i]);
                    } else {
                        numberPicker.setSelectedIndex(0);
                    }
                }
            }
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case Attributes.Event.CHANGE:
                hasChangeListener = true;
                return true;
            case COLUMNCHANGE:
                hasColumnChangeListener = true;
                return true;
            case Attributes.Event.CLICK:
                return true;
            default:
                break;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case Attributes.Event.CHANGE:
                hasChangeListener = false;
                return true;
            case COLUMNCHANGE:
                hasColumnChangeListener = false;
                return true;
            case Attributes.Event.CLICK:
                return true;
            default:
                break;
        }

        return super.removeEvent(event);
    }

    private List<List<String>> getRanges(Object rangeObject) {
        if (rangeObject instanceof String) {
            try {
                rangeObject = new JSONArray((String) rangeObject);
            } catch (JSONException e) {
                Log.e(TAG, "get ranges error", e);
            }
        }
        if (!(rangeObject instanceof JSONArray)) {
            return null;
        }
        JSONArray jsonArray = (JSONArray) rangeObject;

        List<List<String>> ranges = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object item = jsonArray.get(i);
                ranges.add(getSingleRange(item));
            } catch (Exception e) {
                return null;
            }
        }

        return ranges;
    }

    private List<String> getSingleRange(Object rangeObject) {
        if (rangeObject instanceof String) {
            try {
                rangeObject = new JSONArray((String) rangeObject);
            } catch (JSONException e) {
                Log.e(TAG, "get single ranges error", e);
            }
        }
        if (!(rangeObject instanceof JSONArray)) {
            return null;
        }
        JSONArray jsonArray = (JSONArray) rangeObject;
        List<String> rangeArray = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String item = jsonArray.getString(i);
                rangeArray.add(item);
            } catch (JSONException e) {
                return null;
            }
        }
        return rangeArray;
    }

    private int[] getSelectedIndex(Object selectedIndexObject) {
        if (selectedIndexObject instanceof String) {
            try {
                selectedIndexObject = new JSONArray((String) selectedIndexObject);
            } catch (JSONException e) {
                Log.e(TAG, "get selected index error", e);
            }
        }
        if (!(selectedIndexObject instanceof JSONArray)) {
            return null;
        }
        JSONArray jsonArray = (JSONArray) selectedIndexObject;
        int[] selectArray = new int[((JSONArray) selectedIndexObject).length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                int index = jsonArray.getInt(i);
                if (index < 0) {
                    return null;
                }
                selectArray[i] = index;
            } catch (JSONException e) {
                return null;
            }
        }
        return selectArray;
    }

    private String getValue(Object valueObject) {
        if (valueObject instanceof String) {
            try {
                valueObject = new JSONArray((String) valueObject);
            } catch (JSONException e) {
                return (String) valueObject;
            }
        }
        if (!(valueObject instanceof JSONArray)) {
            return "";
        }
        JSONArray jsonArray = (JSONArray) valueObject;

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                stringBuilder.append(jsonArray.getString(i)).append(" ");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return stringBuilder.toString().trim();
    }
}
