/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.Page;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.text.TypefaceBuilder;
import org.hapjs.widgets.view.SelectView;
import org.hapjs.widgets.view.text.TextDecoration;

@WidgetAnnotation(
        name = Select.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Select extends Container<SelectView> {

    protected static final String WIDGET_NAME = "select";

    private static final String TAG = "select";
    private static final String DEFAULT_COLOR = "#8a000000";
    private static final String DEFAULT_FONT_SIZE = "30px";

    private SelectAdapter mAdapter;
    private Option mSelectedItem;
    private boolean mChangeEventSet;

    private int mColor = ColorUtil.getColor(DEFAULT_COLOR);
    private float mFontSize = Attributes.getFontSize(mHapEngine, getPage(), DEFAULT_FONT_SIZE);
    private int mFontStyle = Typeface.NORMAL;
    private int mFontWeight = Typeface.NORMAL;
    private int mTextDecoration = TextDecoration.NONE;
    private boolean forceDarkAllowed = true;

    public Select(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mAdapter = new SelectAdapter();
    }

    private void initSelect() {
        Page page = super.initFontLevel();
        mFontSize = Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE, this);
    }

    @Override
    protected SelectView createViewImpl() {
        SelectView selectView = new SelectView(mContext);
        selectView.setBackgroundResource(R.drawable.select_background);
        selectView.setComponent(this);
        selectView.setAdapter(mAdapter);
        selectView.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id) {
                        mSelectedItem = (Option) mChildren.get(position);

                        if (!mChangeEventSet) {
                            return;
                        }
                        Map<String, Object> params = new HashMap<>();
                        String value = mSelectedItem.getValue();
                        if (!TextUtils.isEmpty(value)) {
                            params.put("newValue", value);
                        } else {
                            Option option = mAdapter.getItem(position);
                            if (option != null) {
                                params.put("newValue", option.getText());
                            } else {
                                Log.e(TAG, "onItemSelected: option is null");
                                params.put("newValue", "");
                            }
                        }
                        mCallback.onJsEventCallback(
                                getPageId(), mRef, Attributes.Event.CHANGE, Select.this, params,
                                null);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        return selectView;
    }

    @Override
    public void addChild(Component child, int index) {
        if (!(child instanceof Option)) {
            Log.w(TAG, "select only accept option as its child!");
            return;
        }
        super.addChild(child, index);

        if (GrayModeManager.getInstance().shouldApplyGrayMode()) {
            GrayModeManager.getInstance().applyGrayMode(child.getHostView(), true);
        }

        if (forceDarkAllowed) {
            ((Option) child).setSelfForceDarkAllowed();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                child.getHostView().setForceDarkAllowed(false);
            }
        }
        mAdapter.notifyDataSetChanged();
        setSelectedItem();

        // When we tested the option event on chrome, we found that the touch event set on the option
        // was invalid. If the touch event on the option component is enabled in a fast application,
        // it will result in a conflict with its own item selection event.
        // Based on this, the decision is consistent with chrome,
        // and the touch event of the option component is shielded.
        freezeAllEvents(child);
    }

    @Override
    public void addView(View childView, int index) {
        // empty implement.
    }

    @Override
    public void removeChild(Component child) {
        super.removeChild(child);
        mAdapter.notifyDataSetChanged();
        setSelectedItem();
    }

    @Override
    public void removeView(View child) {
        // empty implement.
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute, DEFAULT_COLOR);
                setColor(colorStr);
                return true;
            case Attributes.Style.FONT_SIZE:
                int defaultFontSize = Attributes.getFontSize(mHapEngine, getPage(), DEFAULT_FONT_SIZE, this);
                int fontSize = Attributes.getFontSize(mHapEngine, getPage(), attribute, defaultFontSize, this);
                setFontSize(fontSize);
                return true;
            case Attributes.Style.FONT_STYLE:
                String fontStyleStr = Attributes.getString(attribute, "normal");
                setFontStyle(fontStyleStr);
                return true;
            case Attributes.Style.FONT_WEIGHT:
                String fontWeightStr = Attributes.getString(attribute, "normal");
                setFontWeight(fontWeightStr);
                return true;
            case Attributes.Style.TEXT_DECORATION:
                String textDecorationStr = Attributes.getString(attribute, "none");
                setTextDecoration(textDecorationStr);
                return true;
            case Attributes.Style.FORCE_DARK:
                forceDarkAllowed = Attributes.getBoolean(attribute, true);
                for (Component child : getChildren()) {
                    if (child instanceof Option) {
                        if (forceDarkAllowed) {
                            ((Option) child).setSelfForceDarkAllowed();
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                child.getHostView().setForceDarkAllowed(false);
                            }
                        }
                    }
                }
                return super.setAttribute(key, attribute);
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public void setColor(String colorStr) {
        mColor = ColorUtil.getColor(colorStr);
    }

    public void setFontSize(int fontSize) {
        mFontSize = fontSize;
    }

    public void setFontStyle(String fontStyleStr) {
        int fontStyle = Typeface.NORMAL;
        if ("italic".equals(fontStyleStr)) {
            fontStyle = Typeface.ITALIC;
        }

        mFontStyle = fontStyle;
    }

    public void setFontWeight(String fontWeightStr) {
        int weight = TypefaceBuilder.parseFontWeight(fontWeightStr);

        mFontWeight = weight;
    }

    public void setTextDecoration(String textDecorationStr) {
        int textDecoration = TextDecoration.NONE;
        if ("underline".equals(textDecorationStr)) {
            textDecoration = TextDecoration.UNDERLINE;
        } else if ("line-through".equals(textDecorationStr)) {
            textDecoration = TextDecoration.LINE_THROUGH;
        }

        mTextDecoration = textDecoration;
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            mChangeEventSet = true;
            return true;
        }
        if (Attributes.Event.CLICK.equals(event)) {
            // not supported.
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
            mChangeEventSet = false;
            return true;
        }

        return super.removeEvent(event);
    }

    public void onSelectionChange(Option option, boolean selected) {
        if (selected) {
            mSelectedItem = option;
        } else {
            if (mSelectedItem == option) {
                mSelectedItem = mAdapter.getItem(0);
            }
        }
        setSelectedItem();
    }

    @Override
    public void applyCache() {
        super.applyCache();
        setSelectedItem();
    }

    private void setSelectedItem() {
        if (mHost != null && mSelectedItem != null && mSelectedItem != mHost.getSelectedItem()) {
            int selection = mChildren.indexOf(mSelectedItem);
            if (selection > -1) {
                mHost.setSelection(selection);
            }
        }
    }

    private void freezeAllEvents(Component component) {
        component.freezeEvent(Attributes.Event.TOUCH_START);
        component.freezeEvent(Attributes.Event.TOUCH_MOVE);
        component.freezeEvent(Attributes.Event.TOUCH_END);
        component.freezeEvent(Attributes.Event.TOUCH_CANCEL);
        component.freezeEvent(Attributes.Event.TOUCH_CLICK);
        component.freezeEvent(Attributes.Event.TOUCH_LONG_PRESS);
    }

    private class SelectAdapter extends BaseAdapter implements SpinnerAdapter {

        private LayoutInflater mInflater;

        public SelectAdapter() {
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mChildren.size();
        }

        @Override
        public Option getItem(int position) {
            if (position < 0 || position >= mChildren.size()) {
                return null;
            }
            return (Option) mChildren.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final AppCompatTextView textView;
            if (convertView == null) {
                textView =
                        (AppCompatTextView) mInflater.inflate(R.layout.select_text, parent, false);
            } else {
                textView = (AppCompatTextView) convertView;
            }

            Option option = getItem(position);
            String text = (option == null ? "" : option.getText());

            if (!TextUtils.isEmpty(text)) {
                SpannableString s = new SpannableString(text);
                if (mTextDecoration == TextDecoration.UNDERLINE) {
                    s.setSpan(new UnderlineSpan(), 0, text.length(),
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                } else if (mTextDecoration == TextDecoration.LINE_THROUGH) {
                    s.setSpan(new StrikethroughSpan(), 0, text.length(),
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                textView.setText(s);
            }

            textView.setTextColor(mColor);
            ViewCompat.setBackgroundTintList(mHost, ColorStateList.valueOf(mColor));

            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFontSize);
            textView.setTypeface(textView.getTypeface(), getTextStyle());

            textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            ViewGroup.LayoutParams lp = textView.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;

            return textView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            Option option = getItem(position);
            if (option.getHostView() == null) {
                option.createView();
            }

            // If the view changes, you need to refreeze all events.
            freezeAllEvents(option);

            View host = option.getHostView();
            // 修复 Android 4.4 上强转失败导致闪退的问题(http://jira.hapjs.org/browse/ISSUE-558)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                AbsListView.LayoutParams lp =
                        new AbsListView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                host.setLayoutParams(lp);
            }
            return host;
        }

        private int getTextStyle() {
            if (mFontStyle != Typeface.NORMAL && mFontWeight != Typeface.NORMAL) {
                return Typeface.BOLD_ITALIC;
            } else if (mFontStyle != Typeface.NORMAL) {
                return mFontStyle;
            } else {
                return mFontWeight;
            }
        }
    }
}
