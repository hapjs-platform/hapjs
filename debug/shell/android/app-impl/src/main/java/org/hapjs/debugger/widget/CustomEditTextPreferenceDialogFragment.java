package org.hapjs.debugger.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreferenceDialogFragment;

import org.hapjs.debugger.app.impl.R;


public class CustomEditTextPreferenceDialogFragment extends EditTextPreferenceDialogFragment {

    public static CustomEditTextPreferenceDialogFragment newInstance(String key) {
        Bundle args = new Bundle();
        CustomEditTextPreferenceDialogFragment fragment = new CustomEditTextPreferenceDialogFragment();
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        TextView title = view.findViewById(android.R.id.title);
        if (title != null) {
            title.setText(getPreference().getTitle());
            title.setClickable(true);
            title.setOnClickListener(v -> {
                CustomEditTextPreferenceDialogFragment.this.onClick(null, DialogInterface.BUTTON_NEGATIVE);
                getDialog().dismiss();
            });
        }
        Button saveBtn = view.findViewById(R.id.save_btn);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                CustomEditTextPreferenceDialogFragment.this.onClick(null, DialogInterface.BUTTON_POSITIVE);
                getDialog().dismiss();
            });
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        Dialog dlg = new Dialog(context, R.style.preference_dialog_style);
        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dlg.setContentView(contentView, lp);
        }
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dlg;
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
