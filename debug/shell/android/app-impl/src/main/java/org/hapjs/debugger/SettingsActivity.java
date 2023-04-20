/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.utils.PreferenceUtils;
import org.hapjs.debugger.widget.CustomEditTextPreference;
import org.hapjs.debugger.widget.CustomListPreference;
import org.hapjs.debugger.widget.EditTextPreferenceFragment;
import org.hapjs.debugger.widget.SettingViewBehavior;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private SettingViewBehavior<View> mBehavior;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_settings);
        mRootView = findViewById(R.id.root_view);
        showSettingsFragment();
        mBehavior = SettingViewBehavior.from(findViewById(R.id.setting));
        mBehavior.addCallback(new SettingViewBehavior.Callback() {
            @Override
            public void onStateChanged(@NonNull View view, int state) {
                if (state == SettingViewBehavior.STATE_EXPANDED) {
                    mRootView.setBackgroundResource(R.color.floatingBg);
                } else if (state == SettingViewBehavior.STATE_HIDDEN) {
                    mRootView.setBackgroundColor(Color.TRANSPARENT);
                    finish();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });
        runOnUiThread(() -> mBehavior.setState(SettingViewBehavior.STATE_EXPANDED));
        Log.d(TAG, "behavior state = " + mBehavior.getState());
    }

    protected void showSettingsFragment() {
        getFragmentManager().beginTransaction()
                .replace(R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        mRootView.setBackgroundColor(Color.TRANSPARENT);
        overridePendingTransition(0, R.anim.bottom_out);
    }

    public static class SettingsFragment extends PreferenceFragment {
        private static final String DEBUG_PARAMS = "__DEBUG_PARAMS__";
        private static final String DIALOG_FRAGMENT_TAG = "DIALOG_FRAGMENT_TAG";
        private EditTextPreference serverPreference;
        private CustomListPreference reloadPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
            String server = PreferenceUtils.getServer(getActivity());
            String summary = getSummary(server);
            serverPreference = (EditTextPreference) findPreference(getString(R.string.setting_item_server_key));
            serverPreference.setText(server);
            serverPreference.setSummary(summary);
            serverPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newSeverValue = (String) newValue;
                    PreferenceUtils.setServer(getActivity(), newSeverValue);
                    PreferenceUtils.setUniversalScan(getActivity(), false);
                    serverPreference.setSummary(getSummary(newSeverValue));
                    return true;
                }
            });

            reloadPreference = ((CustomListPreference) findPreference(getString(R.string.setting_item_reload_key)));
            reloadPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                if (getResources().getString(R.string.setting_item_reload_app_value).equals(newValue)) {
                    PreferenceUtils.setReloadPackage(getActivity(), false);
                } else if (getResources().getString(R.string.setting_item_reload_page_value).equals(newValue)) {
                    PreferenceUtils.setReloadPackage(getActivity(), true);
                }
                return true;
            });

            SwitchPreference waitConnectPreference = ((SwitchPreference) findPreference((getString(R.string.setting_item_wait_connect_key))));
            waitConnectPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtils.setWaitDevTools(getActivity(), ((Boolean) newValue));
                return true;
            });

            SwitchPreference webDebugPreference = ((SwitchPreference) findPreference(getString(R.string.setting_item_web_debug_key)));
            webDebugPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtils.setWebDebugEnabled(getActivity(), (Boolean) newValue);
                return true;
            });

            final EditTextPreference paramsPreference = (EditTextPreference) findPreference(getString(R.string.setting_item_params_key));
            String params = PreferenceUtils.getLaunchParams(getActivity());
            paramsPreference.setSummary(getSummary(params));
            paramsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue != null) {
                    if (Uri.parse(newValue.toString()).getQueryParameter(DEBUG_PARAMS) != null) {
                        Toast.makeText(getActivity(), R.string.toast_params_illegal, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                String newParamsValue = (String) newValue;
                PreferenceUtils.setLaunchParams(getActivity(), newParamsValue);
                paramsPreference.setSummary(getSummary(newParamsValue));
                return true;
            });
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof CustomEditTextPreference) {
                if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
                    return;
                }
                Fragment f = EditTextPreferenceFragment.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                getFragmentManager().beginTransaction()
                        .hide(this)
                        .add(R.id.content, f, DIALOG_FRAGMENT_TAG)
                        .show(f)
                        .addToBackStack(null)
                        .commit();
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals(getString(R.string.setting_item_instruction_key))) {
                Intent intent = new Intent(getActivity(), WebActivity.class);
                intent.putExtra(WebActivity.KEY_URL, "https://doc.quickapp.cn/ide/machine-debug.html");
                intent.putExtra(WebActivity.KEY_TITLE, preference.getTitle());
                getActivity().startActivity(intent);
                return true;
            } else if (preference.getKey().equals(getString(R.string.setting_item_faq_key))) {
                Intent intent = new Intent(getActivity(), WebActivity.class);
                intent.putExtra(WebActivity.KEY_URL, "https://faq.quickapp.cn/");
                intent.putExtra(WebActivity.KEY_TITLE, preference.getTitle());
                getActivity().startActivity(intent);
                return true;
            } else if (preference.getKey().equals(getString(R.string.setting_item_doc_key))) {
                Intent intent = new Intent(getActivity(), WebActivity.class);
                intent.putExtra(WebActivity.KEY_URL, "https://doc.quickapp.cn/");
                intent.putExtra(WebActivity.KEY_TITLE, preference.getTitle());
                getActivity().startActivity(intent);
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        private String getSummary(String summary) {
            if (TextUtils.isEmpty(summary)) {
                return getString(R.string.setting_item_unset_summary);
            } else {
                return summary;
            }
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            RecyclerView list = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            list.setVerticalScrollBarEnabled(false);
            return list;
        }
    }
}
