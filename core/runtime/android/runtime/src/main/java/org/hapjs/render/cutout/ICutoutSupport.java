/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.cutout;

import android.content.Context;
import android.view.View;
import android.view.Window;

public interface ICutoutSupport extends CutoutProvider {

    String FIT_CUTOUT_PORTRAIT = "portrait";
    String FIT_CUTOUT_LANDSCAPE = "landscape";
    String FIT_CUTOUT_NONE = "none";

    void fit(Context context, Window window, View cutoutView, boolean portrait, String fitCutout);
}
