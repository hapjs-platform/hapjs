/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListAdapter;
import androidx.appcompat.app.AlertDialog;
import org.hapjs.common.utils.ColorUtil;

public class HybridDialogImpl implements HybridDialog {
    private AlertDialog.Builder mBuilder;
    private AlertDialog mDialog;
    private String[] mColorArray;

    HybridDialogImpl(Context context, int themeResId) {
        mBuilder = new AlertDialog.Builder(context, themeResId);
    }

    HybridDialogImpl(Context context, int themeResId, String[] colorArray) {
        mColorArray = colorArray;
        mBuilder = new AlertDialog.Builder(context, themeResId);
    }

    @Override
    public void setTitle(CharSequence title) {
        mBuilder.setTitle(title);
    }

    @Override
    public void setButton(
            int whichButton, CharSequence text, DialogInterface.OnClickListener listener) {
        switch (whichButton) {
            case DialogInterface.BUTTON_POSITIVE:
                mBuilder.setPositiveButton(text, listener);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mBuilder.setNegativeButton(text, listener);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mBuilder.setNeutralButton(text, listener);
                break;
            default:
                break;
        }
    }

    @Override
    public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        mBuilder.setOnCancelListener(listener);
    }

    @Override
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        mBuilder.setOnDismissListener(onDismissListener);
    }

    @Override
    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog.dismiss();
    }

    @Override
    public void show() {
        if (mDialog == null) {
            mDialog = mBuilder.create();
            mDialog.setOnShowListener(
                    new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            if (mColorArray != null && mColorArray.length > 0) {
                                if (mColorArray.length > 0) {
                                    setTextColor(mDialog, DialogInterface.BUTTON_POSITIVE,
                                            (mColorArray[0]));
                                }
                                if (mColorArray.length > 1) {
                                    setTextColor(mDialog, DialogInterface.BUTTON_NEGATIVE,
                                            (mColorArray[1]));
                                }
                                if (mColorArray.length > 2) {
                                    setTextColor(mDialog, DialogInterface.BUTTON_NEUTRAL,
                                            (mColorArray[2]));
                                }
                            }
                        }
                    });
        }
        mDialog.show();
    }

    @Override
    public void setView(View view) {
        mBuilder.setView(view);
    }

    private void setTextColor(AlertDialog dialog, int whichButton, String textColor) {
        if (!TextUtils.isEmpty(textColor)) {
            dialog.getButton(whichButton).setTextColor(ColorUtil.getColor(textColor));
        }
    }

    @Override
    public void setMessage(CharSequence message) {
        mBuilder.setMessage(message);
    }

    @Override
    public void setCancelable(boolean flag) {
        mBuilder.setCancelable(flag);
    }

    @Override
    public void setAdapter(ListAdapter adapter, DialogInterface.OnClickListener onClickListener) {
        mBuilder.setAdapter(adapter, onClickListener);
    }

    @Override
    public void setItems(int itemsId, DialogInterface.OnClickListener listener) {
        mBuilder.setItems(itemsId, listener);
    }

    @Override
    public Dialog createDialog() {
        return mBuilder.create();
    }

    @Override
    public void setOnKeyListener(DialogInterface.OnKeyListener onKeyListener) {
        mBuilder.setOnKeyListener(onKeyListener);
    }
}
