/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

public class ScreenOrientation {
    public static final String ORIENTATION_PORTRAIT_PRIMARY = "portraitprimary";
    public static final String ORIENTATION_PORTRAIT_SECONDARY = "portraitsecondary";
    public static final String ORIENTATION_LANDSCAPE_PRIMARY = "landscapeprimary";
    public static final String ORIENTATION_LANDSCAPE_SECONDARY = "landscapesecondary";

    private String mOrientation = ORIENTATION_PORTRAIT_PRIMARY;
    private float mAngel = 0f;

    public String getOrientation() {
        return mOrientation;
    }

    public void setOrientation(String orientation) {
        mOrientation = orientation;
    }

    public float getAngel() {
        return mAngel;
    }

    public void setAngel(float angel) {
        mAngel = angel;
    }
}
