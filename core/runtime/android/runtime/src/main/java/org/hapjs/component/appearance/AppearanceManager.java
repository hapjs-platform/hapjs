/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.appearance;

import java.util.HashMap;
import java.util.Map;
import org.hapjs.component.Component;

public class AppearanceManager {

    protected HashMap<Component, AppearanceHelper> mAppearanceHelpers = new HashMap<>();

    public void checkAppearanceEvent() {
        for (Map.Entry<Component, AppearanceHelper> item : mAppearanceHelpers.entrySet()) {
            AppearanceHelper helper = item.getValue();
            if (!helper.isWatchAppearance()) {
                continue;
            }
            helper.updateAppearanceEvent();
        }
    }

    public void checkAppearanceOneEvent(Component component) {
        if (null != component) {
            AppearanceHelper helper = mAppearanceHelpers.get(component);
            if (null == helper || !helper.isWatchAppearance()) {
                return;
            }
            helper.updateAppearanceEvent();
        }
    }

    public void bindAppearanceEvent(Component component) {
        if (mAppearanceHelpers.containsKey(component)) {
            // do nothing
            return;
        }
        mAppearanceHelpers.put(component, new AppearanceHelper(component));
    }

    public void unbindAppearanceEvent(Component component) {
        AppearanceHelper helper = mAppearanceHelpers.get(component);
        if (helper == null) {
            return;
        }

        // if helper of component has no watch event,then remove it.
        if (!helper.isWatchAppearance()) {
            mAppearanceHelpers.remove(component);
        }
    }
}
