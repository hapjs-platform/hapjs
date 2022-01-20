/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import org.hapjs.runtime.CheckableAlertDialog;
import org.hapjs.runtime.ExceptionActivity;
import org.hapjs.runtime.R;

public class ExceptionDialogBuilder {
    private static final String TAG = "ExceptionDialogBuilder";

    private Context mContext;

    private String mAppName;

    private Exception mException;

    public ExceptionDialogBuilder(Context context) {
        mContext = context;
    }

    public ExceptionDialogBuilder setAppName(String appName) {
        mAppName = appName;
        return this;
    }

    public ExceptionDialogBuilder setException(Exception exception) {
        mException = exception;
        return this;
    }

    public Dialog show() {
        Dialog dialog = create();
        dialog.show();
        return dialog;
    }

    public Dialog create() {
        CheckableAlertDialog dialog = new CheckableAlertDialog(mContext);
        if (!TextUtils.isEmpty(mAppName)) {
            dialog.setTitle(mContext.getString(R.string.dlg_page_error_title, mAppName));
        }
        if (mException != null) {
            dialog.setMessage(getExceptionMessage());
        }
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                R.string.dlg_btn_quit,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Activity) mContext).finish();
                    }
                });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, R.string.dlg_btn_continue, null);
        dialog.setOnShowListener(
                new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Window window = ((Dialog) dialog).getWindow();
                        if (window != null) {
                            TextView messageText = window.findViewById(R.id.message);
                            messageText.setMovementMethod(LinkMovementMethod.getInstance());
                        } else {
                            Log.e(TAG, "onShow: window is null");
                        }
                    }
                });
        return dialog;
    }

    private CharSequence getExceptionMessage() {
        CharSequence messageText = mContext.getString(R.string.dlg_page_error_message);
        SpannableString messageString = new SpannableString(messageText);
        int linkColor = mContext.getResources().getColor(R.color.link_text_light);
        ExceptionURLSpan span = new ExceptionURLSpan(mException, linkColor);
        messageString.setSpan(span, 0, messageString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return messageString;
    }

    @SuppressLint("ParcelCreator")
    private static class ExceptionURLSpan extends URLSpan {
        private Exception exception;
        private int linkColor;

        public ExceptionURLSpan(Exception e, int linkColor) {
            super("");
            this.exception = e;
            this.linkColor = linkColor;
        }

        @Override
        public void onClick(View widget) {
            try {
                ExceptionActivity.startExceptionActivity(widget.getContext(), exception);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "card or inset view doesn't support open exception activity", e);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            int orgLinkColor = ds.linkColor;
            ds.linkColor = linkColor;
            super.updateDrawState(ds);
            ds.linkColor = orgLinkColor;
        }
    }
}
