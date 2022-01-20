/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.utils.EasterEggUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EasterEggUtils.addEasterEgg(toolbar, new EasterEggUtils.EasterEggListener() {
            @Override
            public void onTrigger(View view) {
                PreferenceUtils.setCardModeAdded(getApplicationContext(), true);
                Toast.makeText(getApplicationContext(), R.string.toast_card_mode_added, Toast.LENGTH_SHORT).show();
            }
        });

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
        private ListPreference reloadPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

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

            reloadPreference = ((ListPreference) findPreference("setting_item_reload"));
            final String reloadAppStrategyText = getResources().getString(R.string.setting_item_reload_strategy_app);
            final String reloadPageStrategyText = getResources().getString(R.string.setting_item_reload_strategy_page);
            reloadPreference.setSummary(PreferenceUtils.shouldReloadPackage(getActivity())
                    ? reloadPageStrategyText
                    : reloadAppStrategyText);
            reloadPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (getResources().getString(R.string.setting_item_reload_app_value).equals(newValue)) {
                        PreferenceUtils.setReloadPackage(getActivity(), false);
                        reloadPreference.setSummary(reloadAppStrategyText);
                    } else if (getResources().getString(R.string.setting_item_reload_page_value).equals(newValue)) {
                        PreferenceUtils.setReloadPackage(getActivity(), true);
                        reloadPreference.setSummary(reloadPageStrategyText);
                    }
                    return true;
                }
            });

            CheckBoxPreference checkBoxPreference = ((CheckBoxPreference) findPreference("setting_item_wait_connect"));
            checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PreferenceUtils.setWaitDevTools(getActivity(), ((Boolean) newValue));
                    return true;
                }
            });

            CheckBoxPreference webDebugPreference = ((CheckBoxPreference) findPreference("setting_item_web_debug"));
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

        private String getSummary(String summary) {
            if (TextUtils.isEmpty(summary)) {
                return getString(R.string.setting_item_unset_summary);
            } else {
                return summary;
            }
        }
    }
}
