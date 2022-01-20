/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.view.View;
import androidx.annotation.NonNull;
import org.hapjs.widgets.custom.WidgetProvider;

public interface IExtensionView<T extends View> extends WidgetProvider.AttributeApplier {

    @NonNull
    T get();

    void bindExpand(RefreshExtension extension);

    void onMove(
            RefreshMovement movement, float moveY, float percent, boolean isDrag,
            boolean isRefreshing);

    void onStateChanged(RefreshState state, int oldState, int currentState);
}
