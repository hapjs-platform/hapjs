/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class ViewUtils {

    /**
     * Set a view 's LayoutParams fit its parent. Such as the inner view of {@link
     * org.hapjs.render.vdom.DocComponent} & {@link org.hapjs.component.Scroller}. The view' height
     * will be WRAP_CONTENT, if parent's view is WRAP_CONTENT, else the height is MATCH_PARENT. The
     * child view' width is always MATCH_PARENT currently.
     *
     * @param lp     the view's LayoutParams
     * @param parent the view's parent view
     */
    public static void fitParentLayoutParams(ViewGroup.LayoutParams lp, View parent) {
        if (lp == null || parent == null || parent.getLayoutParams() == null) {
            return;
        }

        lp.height =
                parent.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : ViewGroup.LayoutParams.MATCH_PARENT;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public static void fitMultiModeLayoutParams(Context context, ViewGroup.LayoutParams lp) {
        if (FoldingUtils.isMultiWindowMode()) {
            lp.width = DisplayUtil.getScreenWidth(context);
        }
    }
}
