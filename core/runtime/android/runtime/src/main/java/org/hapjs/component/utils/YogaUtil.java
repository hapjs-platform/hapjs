/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.utils;

import android.view.View;
import com.facebook.yoga.YogaNode;
import org.hapjs.component.view.YogaLayout;

public class YogaUtil {

    private YogaUtil() {
    }

    public static YogaNode getYogaNode(View view) {
        if (view == null) {
            return null;
        }

        if (view.getParent() instanceof YogaLayout) {
            YogaLayout parent = (YogaLayout) view.getParent();
            YogaNode node = parent.getYogaNodeForView(view);
            return node;
        }

        return null;
    }

    public static YogaNode getParentNode(View view) {
        YogaNode node = getYogaNode(view);
        if (node == null) {
            return null;
        }
        return node.getParent();
    }

    public static YogaNode getRootNode(YogaNode node) {
        YogaNode rootNode = node;
        while (rootNode.getParent() != null) {
            rootNode = rootNode.getParent();
        }
        return rootNode;
    }
}
