/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

public class HapConfiguration {

    private int mLastUiMode;

    private int mUiMode;

    private Locale mLocale;

    private int mOrientation;

    private int mLastOrientation;

    public HapConfiguration() {
        mLocale = Locale.getDefault();
        Context context = Runtime.getInstance().getContext();
        mUiMode =
                ((context.getResources().getConfiguration().uiMode)
                        & Configuration.UI_MODE_NIGHT_MASK);
        mLastUiMode = mUiMode;
        mOrientation = context.getResources().getConfiguration().orientation;
    }

    public int getUiMode() {
        return mUiMode;
    }

    public void setUiMode(int uiMode) {
        mUiMode = uiMode;
    }

    public Locale getLocale() {
        return mLocale;
    }

    public void setLocale(Locale locale) {
        mLocale = locale;
    }

    public int getLastUiMode() {
        return mLastUiMode;
    }

    public void setLastUiMode(int lastUiMode) {
        mLastUiMode = lastUiMode;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public int getLastOrientation() {
        return mLastOrientation;
    }

    public void setLastOrientation(int lastOrientation) {
        mLastOrientation = lastOrientation;
    }

    @Override
    public String toString() {
        return "Locale:"
                + mLocale
                + " UiMode:"
                + mUiMode
                + " LastUiMode:"
                + mLastUiMode
                + " mOrientation:"
                + mOrientation;
    }

    public HapConfiguration obtain() {
        HapConfiguration configuration = new HapConfiguration();
        configuration.mLocale = mLocale;
        configuration.mUiMode = mUiMode;
        configuration.mLastUiMode = mLastUiMode;
        configuration.mOrientation = mOrientation;
        configuration.mLastOrientation = mLastOrientation;
        return configuration;
    }
}
