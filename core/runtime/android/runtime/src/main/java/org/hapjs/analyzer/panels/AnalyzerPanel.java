/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.view.View;

import androidx.annotation.IntDef;

public interface AnalyzerPanel {

    int UNDEFINED = -1;
    int TOP = 0;
    int BOTTOM = 2;

    @IntDef({UNDEFINED, TOP, BOTTOM})
    @interface POSITION {
    }

    String getName();

    View getPanelView();

    int collapseLayoutId();

    void show();

    void show(boolean animation);

    void dismiss();

    void dismiss(boolean animation);

    boolean isShowing();

    interface Callback {
        void onPanelShow();

        void onPanelHidden();
    }
}
