/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.view.View;

public interface HybridView {
    HybridManager getHybridManager();

    View getWebView();

    void setHybridViewClient(HybridViewClient client);

    void setHybridChromeClient(HybridChromeClient client);

    void loadUrl(String url);

    HybridSettings getSettings();

    void destroy();

    void menuButtonPressPage(OnKeyUpListener onKeyUpListener);

    //    void reload();
    //    void clearCache(boolean includeDiskFiles);
    boolean canGoBack();

    void goBack();

    boolean needRunInBackground();

    void setOnVisibilityChangedListener(OnVisibilityChangedListener l);

    interface OnVisibilityChangedListener {
        void onVisibilityChanged(boolean visible);
    }

    interface OnKeyUpListener {
        void consume(boolean consumed);
    }
}
