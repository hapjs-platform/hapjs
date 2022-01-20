/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import org.hapjs.render.RootView;

public class DummyActivity extends Activity {

    private final Activity mBase;
    private Context mThemeContext;
    private LayoutInflater inflater;

    public DummyActivity(Activity base) {
        super();
        this.mBase = base;
        attachBaseContext(base);
        mThemeContext =
                CardView.getThemeContext(base,
                        org.hapjs.runtime.R.style.Theme_AppCompat_Light_NoActionBar);
    }

    @Override
    public ClassLoader getClassLoader() {
        return RootView.class.getClassLoader();
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getBaseContext()).cloneInContext(mThemeContext);
            }
            return inflater;
        }
        return getBaseContext().getSystemService(name);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        mBase.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public WindowManager getWindowManager() {
        return (WindowManager) mBase.getSystemService(WINDOW_SERVICE);
    }

    @Override
    public Window getWindow() {
        return mBase.getWindow();
    }

    @Override
    public boolean isFinishing() {
        return mBase.isFinishing();
    }

    @Override
    public boolean isDestroyed() {
        return mBase.isDestroyed();
    }

    @Override
    public Resources.Theme getTheme() {
        return mThemeContext.getTheme();
    }
}
