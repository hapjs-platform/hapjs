/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.SwitchPreference;

import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.utils.PreferenceUtils;
import org.hapjs.debugger.widget.CustomEditTextPreferenceDialogFragment;
import org.hapjs.debugger.widget.CustomListPreference;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction()
                .replace(R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragment {
        private EditTextPreference serverPreference;
        private CustomListPreference reloadPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
            String server = PreferenceUtils.getServer(getActivity());
            String summary = getSummary(server);
            serverPreference = (EditTextPreference) findPreference("setting_item_server");
            serverPreference.setText(server);
            serverPreference.setSummary(summary);
            serverPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String server = (String) newValue;
                    PreferenceUtils.setServer(getActivity(), server);
                    PreferenceUtils.setUniversalScan(getActivity(), false);
                    serverPreference.setSummary(getSummary(server));
                    return true;
                }
            });

            reloadPreference = ((CustomListPreference) findPreference("setting_item_reload"));
            reloadPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (getResources().getString(R.string.setting_item_reload_app_value).equals(newValue)) {
                        PreferenceUtils.setReloadPackage(getActivity(), false);
                    } else if (getResources().getString(R.string.setting_item_reload_page_value).equals(newValue)) {
                        PreferenceUtils.setReloadPackage(getActivity(), true);
                    }
                    return true;
                }
            });

            SwitchPreference checkBoxPreference = ((SwitchPreference) findPreference("setting_item_wait_connect"));
            checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PreferenceUtils.setWaitDevTools(getActivity(), ((Boolean) newValue));
                    return true;
                }
            });

            SwitchPreference webDebugPreference = ((SwitchPreference) findPreference("setting_item_web_debug"));
            webDebugPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PreferenceUtils.setWebDebugEnabled(getActivity(), (Boolean) newValue);
                    return true;
                }
            });

            final EditTextPreference paramsPreference = (EditTextPreference) findPreference("setting_item_params");
            String params = PreferenceUtils.getLaunchParams(getActivity());
            paramsPreference.setSummary(getSummary(params));
            paramsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null) {
                        final String debugParams = "__DEBUG_PARAMS__";
                        if (Uri.parse(newValue.toString()).getQueryParameter(debugParams) != null) {
                            Toast.makeText(getActivity(), R.string.toast_params_illegal, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }

                    String params = (String) newValue;
                    PreferenceUtils.setLaunchParams(getActivity(), params);
                    paramsPreference.setSummary(getSummary(params));
                    return true;
                }
            });
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof EditTextPreference) {
                if (getFragmentManager().findFragmentByTag("DIALOG_FRAGMENT_TAG") != null) {
                    return;
                }
                DialogFragment f = CustomEditTextPreferenceDialogFragment.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                f.show(getFragmentManager(), "DIALOG_FRAGMENT_TAG");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        private String getSummary(String summary) {
            if (TextUtils.isEmpty(summary)) {
                return getString(R.string.setting_item_unset_summary);
            } else {
                return summary;
            }
        }
    }
}
