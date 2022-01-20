/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.hapjs.mockup.app.AppManager;
import org.hapjs.mockup.app.impl.R;

/**
 * A fragment representing a list of Items.
 */
public class AppItemFragment extends Fragment {
    private RecyclerView mRecycler;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRecycler =
                (RecyclerView) inflater.inflate(R.layout.fragment_appitem_list, container, false);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        return mRecycler;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (PermissionChecker.checkSelfPermission(
                getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            bindData();
        } else {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0
                && permissions.length == 1
                && Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[0])) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bindData();
            } else {
                Toast.makeText(getActivity(), R.string.toast_permission_fail, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void bindData() {
        mRecycler.setAdapter(new AppItemAdapter(AppManager.getAllApps(getActivity())));
    }
}
