/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.hapjs.debugger.app.impl.R;

public class AppLaunchTestActivity extends AppCompatActivity {
    private static final String TAG = "AppLaunchTestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        ViewGroup contentHolder = findViewById(R.id.content);
        View content = getLayoutInflater().inflate(R.layout.activity_launch_test_content, contentHolder);

        ViewPager2 pager = content.findViewById(R.id.pager);
        TabLayout tabLayout = content.findViewById(R.id.tab_layout);
        pager.setAdapter(new PagerAdapter(this));
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.launch_page_test_title : R.string.launch_by_deeplink_title);
        }
        );
        mediator.attach();
    }

    private class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? new LaunchPageFragment() : new LaunchByDeeplilnkFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public static class LaunchPageFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.launch_page_layout, container, false);

            TextView launchBtn = content.findViewById(R.id.btn_launch);
            EditText pkgEditText = content.findViewById(R.id.pkg);
            View clearPkgBtn = content.findViewById(R.id.clear_pkg);
            clearPkgBtn.setOnClickListener(v -> pkgEditText.setText(null));
            pkgEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 0) {
                        launchBtn.setEnabled(false);
                        clearPkgBtn.setVisibility(View.GONE);
                    } else {
                        launchBtn.setEnabled(true);
                        clearPkgBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
            pkgEditText.setOnFocusChangeListener((v, hasFocus) ->
                    clearPkgBtn.setVisibility(pkgEditText.length() > 0 && hasFocus ? View.VISIBLE : View.GONE));

            EditText pageEditText = content.findViewById(R.id.page);
            View clearPageBtn = content.findViewById(R.id.clear_page);
            clearPageBtn.setOnClickListener(v -> pageEditText.setText(null));
            pageEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 0) {
                        clearPageBtn.setVisibility(View.GONE);
                    } else {
                        clearPageBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
            pageEditText.setOnFocusChangeListener((v, hasFocus) ->
                    clearPageBtn.setVisibility(pageEditText.length() > 0 && hasFocus ? View.VISIBLE : View.GONE));

            launchBtn.setOnClickListener(v -> launchQuickApp(getActivity(),
                    pkgEditText.getText().toString(), pageEditText.getText().toString()));

            return content;
        }
    }

    public static class LaunchByDeeplilnkFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View content = inflater.inflate(R.layout.launch_by_deeplink_layout, container, false);

            TextView launchBtn = content.findViewById(R.id.btn_launch);
            EditText deeplinkEditText = content.findViewById(R.id.deeplink);
            View clearDeeplinkBtn = content.findViewById(R.id.clear_deeplink);
            clearDeeplinkBtn.setOnClickListener(v -> deeplinkEditText.setText(null));
            deeplinkEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 0) {
                        launchBtn.setEnabled(false);
                        clearDeeplinkBtn.setVisibility(View.GONE);
                    } else {
                        launchBtn.setEnabled(true);
                        clearDeeplinkBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
            deeplinkEditText.setOnFocusChangeListener((v, hasFocus) ->
                    clearDeeplinkBtn.setVisibility(deeplinkEditText.length() > 0 && hasFocus ? View.VISIBLE : View.GONE));

            launchBtn.setOnClickListener(v -> launchDeeplink(getActivity(), deeplinkEditText.getText().toString()));

            return content;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static void launchQuickApp(Activity activity, String packageName, String path) {
        if (TextUtils.isEmpty(packageName)) {
            Toast.makeText(activity, R.string.toast_no_package, Toast.LENGTH_LONG).show();
            return;
        }
        String url = "hap://app/" + packageName + path;
        launchDeeplink(activity, url);
    }

    private static void launchDeeplink(Activity activity, String deeplinkText) {
        if (TextUtils.isEmpty(deeplinkText)) {
            Toast.makeText(activity, R.string.toast_no_deeplink, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(deeplinkText));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchDeeplink: ", e);
            Toast.makeText(activity, R.string.toast_wrong_deeplink, Toast.LENGTH_LONG).show();
        }
    }
}
