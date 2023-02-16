/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.ad;

import android.content.Context;

import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.Div;

import java.util.Map;


@WidgetAnnotation(
        name = AdClickArea.WIDGET_NAME
)
public class AdClickArea extends Div {

    protected static final String WIDGET_NAME = "ad-clickable-area";

    public AdClickArea(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback,
                       Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }
}
