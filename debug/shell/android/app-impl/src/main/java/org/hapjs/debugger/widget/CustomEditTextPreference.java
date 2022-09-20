/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.preference.AndroidResources;
import androidx.preference.EditTextPreference;

import org.hapjs.debugger.app.impl.R;

public class CustomEditTextPreference extends EditTextPreference {

    private CharSequence mTextHint;

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EditTextPreference, defStyleAttr, defStyleRes);
        mTextHint = getString(a, R.styleable.EditTextPreference_hint, R.styleable.EditTextPreference_android_hint);
        a.recycle();
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, getAttr(context, androidx.preference.R.attr.editTextPreferenceStyle,
                AndroidResources.ANDROID_R_EDITTEXT_PREFERENCE_STYLE));
    }

    public CustomEditTextPreference(Context context) {
        this(context, null);
    }

    public CharSequence getTextHint() {
        return mTextHint;
    }

    public void setTextHint(CharSequence hint) {
        mTextHint = hint;
    }

    static int getAttr(@NonNull Context context, int attr, int fallbackAttr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return attr;
        }
        return fallbackAttr;
    }

    static String getString(@NonNull TypedArray a, @StyleableRes int index,
                            @StyleableRes int fallbackIndex) {
        String val = a.getString(index);
        if (val == null) {
            val = a.getString(fallbackIndex);
        }
        return val;
    }
}
