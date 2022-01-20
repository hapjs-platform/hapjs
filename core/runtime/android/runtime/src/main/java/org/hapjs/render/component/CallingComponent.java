/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.component;

import android.util.Log;
import org.hapjs.component.Component;
import org.hapjs.render.ComponentAction;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;

public class CallingComponent {

    public void applyComponentAction(ComponentAction componentAction, VDocument doc) {
        VElement element = doc.getElementById(componentAction.ref);
        if (element == null) {
            return;
        }

        Component component = element.getComponent();
        if (component == null) {
            Log.w("CallingComponent", "component may be recycled");
            if (element.getRecyclerItem() != null) {
                element.getRecyclerItem()
                        .invokeMethod(componentAction.method, componentAction.args);
            }
            return;
        }
        component.invokeMethod(componentAction.method, componentAction.args);
    }
}
