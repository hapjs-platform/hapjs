/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.Nullable;
import org.hapjs.features.R;

public class CustomBottomDialog extends DialogFragment {
    public static final String TAG = "CustomBottomDialog";

    private String[] mItems;
    private ClickBottomItemListener mListener;

    public static CustomBottomDialog newInstance(String[] items) {
        CustomBottomDialog dialog = new CustomBottomDialog();
        Bundle args = new Bundle();
        args.putStringArray("items", items);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItems = getArguments().getStringArray("items");
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.bottom_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = (ListView) view.findViewById(R.id.lv_list_dialog);
        listView.setAdapter(
                new ArrayAdapter(getActivity(), R.layout.choose_navi_map_layout, R.id.tv_navi_map,
                        mItems));

        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                                            long id) {
                        dismiss();
                        if (mListener != null) {
                            mListener.onItemClick(position);
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            params.dimAmount = 0.5f;
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);
    }

    public void showDialog(FragmentManager fragmentManager) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag(getTag());
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        try {
            // Create and show the dialog.
            show(ft, getTag());
        } catch (Exception e) {
            Log.e(TAG, "show dialog failed. exception:", e);
        }
    }

    public void setOnItemClickListener(ClickBottomItemListener listener) {
        mListener = listener;
    }

    public interface ClickBottomItemListener {
        void onItemClick(int position);
    }
}
