/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class CheckableAlertDialog extends Dialog implements Checkable {
    private static final String TAG = "CheckableAlertDialog";

    private TextView mTitle;
    private TextView mMessage;
    private CheckBox mCheckBox;
    private View mCheckboxPanel;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private Button mNeutralButton;
    private View mButtonGroup;

    public CheckableAlertDialog(Context context) {
        super(context, R.style.HapTheme_Dialog);

        initializeViews();
    }

    private void initializeViews() {
        super.setContentView(R.layout.alert_dialog);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.getAttributes().gravity = Gravity.BOTTOM;
        } else {
            Log.w(TAG, "initializeViews: window is null");
        }
        mTitle = (TextView) findViewById(R.id.alertTitle);
        mMessage = (TextView) findViewById(R.id.message);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
        mCheckboxPanel = findViewById(R.id.checkboxPanel);
        mPositiveButton = (Button) findViewById(android.R.id.button1);
        mNegativeButton = (Button) findViewById(android.R.id.button2);
        mNeutralButton = (Button) findViewById(android.R.id.button3);
        mButtonGroup = findViewById(R.id.buttonGroup);
        DarkThemeUtil.disableForceDark(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup customPanel = (ViewGroup) findViewById(android.R.id.custom);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(layoutResID, customPanel, true);
        findViewById(R.id.customPanel).setVisibility(View.VISIBLE);
    }

    @Override
    public void setContentView(View view) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mTitle.setText(title);
        findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getContext().getString(titleId));
    }

    public void setMessage(CharSequence message) {
        mMessage.setText(message);
        findViewById(R.id.contentPanel).setVisibility(View.VISIBLE);
    }

    public void setMessage(int messageId) {
        setMessage(getContext().getString(messageId));
    }

    /**
     * Set the check box to display.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public void setCheckBox(boolean isChecked, CharSequence msg) {
        mCheckboxPanel.setVisibility(View.VISIBLE);
        mCheckBox.setChecked(isChecked);
        mCheckBox.setText(msg);
        // If checkbox is visible, clear padding top of button group
        clearPaddingTopOfButtonGroup();
    }

    /**
     * Set the check box to display.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public void setCheckBox(boolean isChecked, int msgId) {
        setCheckBox(isChecked, getContext().getString(msgId));
    }

    /**
     * Get the checked status of check box.
     *
     * @return Checking status.
     */
    public boolean isChecked() {
        return mCheckboxPanel.getVisibility() == View.VISIBLE && mCheckBox.isChecked();
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param whichButton Which button to set the listener on, can be one of {@link
     *                    DialogInterface#BUTTON_POSITIVE}, {@link DialogInterface#BUTTON_NEGATIVE}, or {@link
     *                    DialogInterface#BUTTON_NEUTRAL}
     * @param text        The text to display in button.
     * @param listener    The {@link OnClickListener} to use.
     */
    public void setButton(int whichButton, CharSequence text, OnClickListener listener) {
        switch (whichButton) {
            case DialogInterface.BUTTON_POSITIVE:
                setupButton(mPositiveButton, whichButton, text, listener);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                setupButton(mNegativeButton, whichButton, text, listener);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                setupButton(mNeutralButton, whichButton, text, listener);
                break;
            default:
                // ignore
                break;
        }
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param whichButton Which button to set the listener on, can be one of {@link
     *                    DialogInterface#BUTTON_POSITIVE}, {@link DialogInterface#BUTTON_NEGATIVE}, or {@link
     *                    DialogInterface#BUTTON_NEUTRAL}
     * @param textId      The resource id of the text to display in the button
     * @param listener    The {@link OnClickListener} to use.
     */
    public void setButton(int whichButton, int textId, OnClickListener listener) {
        setButton(whichButton, getContext().getString(textId), listener);
    }

    private void setupButton(
            Button button, int whichButton, CharSequence text, OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setText(text);
        setupClickListener(button, whichButton, listener);
    }

    protected void setupClickListener(Button button, int whichButton, OnClickListener listener) {
        button.setOnClickListener(new OnClickListenerWrapper(listener, whichButton));
    }

    private void clearPaddingTopOfButtonGroup() {
        mButtonGroup.setPadding(
                mButtonGroup.getPaddingLeft(),
                0,
                mButtonGroup.getPaddingRight(),
                mButtonGroup.getPaddingBottom());
    }

    private class OnClickListenerWrapper implements View.OnClickListener {
        private OnClickListener listener;
        private int whichButton;

        public OnClickListenerWrapper(OnClickListener listener, int whichButton) {
            this.listener = listener;
            this.whichButton = whichButton;
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onClick(CheckableAlertDialog.this, whichButton);
            }
            dismiss();
        }
    }
}
