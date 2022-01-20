/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ListAdapter;

public interface HybridDialog {

    void setTitle(CharSequence title);

    void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener);

    void setMessage(CharSequence message);

    void setCancelable(boolean flag);

    void setOnCancelListener(DialogInterface.OnCancelListener listener);

    void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener);

    void setAdapter(ListAdapter adapter, DialogInterface.OnClickListener onClickListener);

    void setItems(int itemsId, DialogInterface.OnClickListener listener);

    void dismiss();

    void show();

    void setView(View view);

    Dialog createDialog();

    void setOnKeyListener(DialogInterface.OnKeyListener onKeyListener);
}
