/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.widget;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import org.hapjs.debugger.app.impl.R;

public class EditTextPreferenceFragment extends Fragment {
    protected static final String ARG_KEY = "key";

    private static final String SAVE_STATE_TITLE = "EditTextPreferenceFragment.title";
    private static final String SAVE_STATE_LAYOUT = "EditTextPreferenceFragment.layout";
    private static final String SAVE_STATE_TEXT = "EditTextPreferenceFragment.text";
    private static final String SAVE_STATE_HINT = "EditTextPreferenceFragment.hint";
    private CustomEditTextPreference mPreference;
    private CharSequence mDialogTitle;
    private @LayoutRes
    int mDialogLayoutRes;

    private EditText mEditText;
    private CharSequence mText;
    private CharSequence mHint;

    public static EditTextPreferenceFragment newInstance(String key) {
        Bundle args = new Bundle();
        EditTextPreferenceFragment fragment = new EditTextPreferenceFragment();
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Fragment rawFragment = getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException("Target fragment must implement TargetFragment interface");
        }

        final DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) rawFragment;

        final String key = getArguments().getString(ARG_KEY);
        if (savedInstanceState == null) {
            mPreference = (CustomEditTextPreference) fragment.findPreference(key);
            mDialogTitle = mPreference.getTitle();
            mDialogLayoutRes = mPreference.getDialogLayoutResource();
            mText = mPreference.getText();
            mHint = mPreference.getTextHint();
        } else {
            mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE);
            mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0);
            mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT);
            mHint = savedInstanceState.getCharSequence(SAVE_STATE_HINT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle);
        outState.putInt(SAVE_STATE_LAYOUT, mDialogLayoutRes);
        outState.putCharSequence(SAVE_STATE_TEXT, mText);
        outState.putCharSequence(SAVE_STATE_HINT, mHint);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final int resId = mDialogLayoutRes;
        if (resId == 0) {
            return null;
        }
        return inflater.inflate(resId, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (view != null) {
            TextView title = view.findViewById(android.R.id.title);
            title.setText(mPreference.getTitle());
            title.setClickable(true);
            title.setOnClickListener(v -> {
                if (hideKeyboard(view)) {
                    view.postDelayed(() -> back(), 100);
                } else {
                    back();
                }
            });

            Button saveBtn = view.findViewById(R.id.save_btn);
            if (saveBtn != null) {
                saveBtn.setOnClickListener(v -> {
                    String value = mEditText.getText().toString();
                    if (mPreference.callChangeListener(value)) {
                        mPreference.setText(value);
                    }
                    if (hideKeyboard(view)) {
                        view.postDelayed(() -> back(), 100);
                    } else {
                        back();
                    }
                });
            }
            mEditText = view.findViewById(android.R.id.edit);
            mEditText.requestFocus();
            mEditText.setText(mText);
            mEditText.setSelection(mEditText.getText().length());
            if (mHint != null) {
                mEditText.setHint(mHint);
            }
            InputMethodManager inputManager =
                    (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(mEditText, 0);
        }
    }

    private void back() {
        getFragmentManager().popBackStack();
    }

    private boolean hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isSoftShowing()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
            return true;
        }
        return false;
    }

    private boolean isSoftShowing() {
        //获取当前屏幕内容的高度
        Window window = getActivity().getWindow();
        int screenHeight = window.getDecorView().getHeight();
        //获取View可见区域的bottom
        Rect rect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        return screenHeight * 2 / 3 > rect.bottom;
    }
}
