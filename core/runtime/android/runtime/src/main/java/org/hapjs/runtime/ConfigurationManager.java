/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hapjs.bridge.ApplicationContext;

public class ConfigurationManager {

    private static final String TAG = "ConfigurationManager";
    private ApplicationContext mApplicationContext;
    private HapConfiguration mCurrentConfiguration;
    private CopyOnWriteArrayList<ConfigurationListener> mConfigurationListener;

    private ConfigurationManager() {
        mCurrentConfiguration = new HapConfiguration();
        mConfigurationListener = new CopyOnWriteArrayList<>();
    }

    public static ConfigurationManager getInstance() {
        return Holder.INSTANCE;
    }

    public void init(ApplicationContext applicationContext) {
        mApplicationContext = applicationContext;
        mConfigurationListener.clear();
        updateLocale(
                obtainLocale(applicationContext.getContext().getResources().getConfiguration()));
        updateThemeMode(applicationContext.getContext().getResources().getConfiguration());
        updateOrientation(applicationContext.getContext().getResources().getConfiguration());
        updateScreenSize(applicationContext.getContext().getResources().getConfiguration());
    }

    public void addListener(ConfigurationListener listener) {
        if (isConfigAllowed()) {
            return;
        }
        mConfigurationListener.add(listener);
    }

    public void removeListener(ConfigurationListener listener) {
        mConfigurationListener.remove(listener);
    }

    public Locale getCurrentLocale() {
        return getCurrent().getLocale();
    }

    public HapConfiguration getCurrent() {
        return mCurrentConfiguration;
    }

    public void update(Context context, Configuration configuration) {
        if (isConfigAllowed()) {
            return;
        }
        updateContextConfiguration(context, configuration);
        updateLocale(obtainLocale(configuration));
        updateThemeMode(configuration);
        updateOrientation(configuration);
        updateScreenSize(configuration);

        for (ConfigurationListener l : mConfigurationListener) {
            l.onConfigurationChanged(mCurrentConfiguration);
        }

        mCurrentConfiguration.setLastUiMode(mCurrentConfiguration.getUiMode());
        mCurrentConfiguration.setLastOrientation(configuration.orientation);
    }

    private void updateThemeMode(Configuration configuration) {
        int uiMode = (configuration.uiMode) & Configuration.UI_MODE_NIGHT_MASK;
        mCurrentConfiguration.setUiMode(uiMode);
    }

    private void updateOrientation(Configuration configuration) {
        mCurrentConfiguration.setOrientation(configuration.orientation);
    }

    private void updateScreenSize(Configuration configuration) {
        mCurrentConfiguration.setScreenSize(configuration.screenWidthDp);
    }

    public void reset(Context context) {
        if (isConfigAllowed()) {
            return;
        }
        Configuration systemConfig = Resources.getSystem().getConfiguration();
        updateContextConfiguration(context, systemConfig);
        updateLocale(obtainLocale(systemConfig));
        mConfigurationListener.clear();
        mApplicationContext = null;
    }

    private void updateContextConfiguration(Context context, Configuration configuration) {
        Configuration newConfiguration = new Configuration(configuration);
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            newConfiguration.uiMode = Configuration.UI_MODE_NIGHT_YES;
        } else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            newConfiguration.uiMode = Configuration.UI_MODE_NIGHT_NO;
        }
        context
                .getResources()
                .updateConfiguration(newConfiguration, context.getResources().getDisplayMetrics());
        if (context instanceof Activity) {
            context
                    .getApplicationContext()
                    .getResources()
                    .updateConfiguration(newConfiguration,
                            context.getResources().getDisplayMetrics());
        }
    }

    private void updateLocale(Locale newLocale) {
        Locale.setDefault(newLocale);
        mCurrentConfiguration.setLocale(newLocale);
    }

    private boolean isConfigAllowed() {
        return mApplicationContext != null // has initialized or not
                && HapEngine.getInstance(mApplicationContext.getPackage()).isCardMode();
    }

    public Locale obtainLocale(Configuration configuration) {
        Locale locale;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            locale = configuration.getLocales().get(0);
        } else {
            locale = configuration.locale;
        }
        return locale;
    }

    public boolean contains(ConfigurationListener listener) {
        return mConfigurationListener.contains(listener);
    }

    public interface ConfigurationListener {
        void onConfigurationChanged(HapConfiguration newConfig);
    }

    private static class Holder {
        static final ConfigurationManager INSTANCE = new ConfigurationManager();
    }
}
