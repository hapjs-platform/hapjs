/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.custom;

import android.content.Context;
import org.hapjs.widgets.view.refresh.Footer;
import org.hapjs.widgets.view.refresh.Header;

public interface WidgetProvider {

    String NAME = "widget-provider";

    Header createRefreshHeader(Context context);

    Footer createRefreshFooter(Context context);

    interface AttributeApplier {
        boolean apply(String name, Object attribute);
    }
}
