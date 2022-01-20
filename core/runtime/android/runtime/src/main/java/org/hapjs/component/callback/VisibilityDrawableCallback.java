/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.callback;

import org.hapjs.component.view.drawable.SizeBackgroundDrawable.Position;

public interface VisibilityDrawableCallback {
    /**
     * Called when the drawable's visibility changes.
     *
     * @param visible whether or not the drawable is visible
     */
    void onVisibilityChange(boolean visible);

    /**
     * Called when the drawable gets drawn.
     */
    boolean onDraw(String currentDrawUrl);

    /**
     * Called when the drawable position has been calculated
     *
     * @param position the calculated position
     */
    void onPositionCalculated(Position position);
}
