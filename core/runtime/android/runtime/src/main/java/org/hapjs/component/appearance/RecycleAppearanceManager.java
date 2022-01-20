/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.appearance;

import java.util.HashSet;
import org.hapjs.component.Component;
import org.hapjs.component.Container;

public class RecycleAppearanceManager extends AppearanceManager {

    private HashSet<AppearanceHelper> mRecycledHelper = new HashSet<>();

    @Override
    public void bindAppearanceEvent(Component component) {
        if (mAppearanceHelpers.containsKey(component)) {
            // do nothing
            return;
        }
        if (!mRecycledHelper.isEmpty()) {
            AppearanceHelper recycledHelper = mRecycledHelper.iterator().next();
            recycledHelper.reset();
            recycledHelper.setAwareChild(component);
            mAppearanceHelpers.put(component, recycledHelper);
            mRecycledHelper.remove(recycledHelper);
        } else {
            mAppearanceHelpers.put(component, new AppearanceHelper(component));
        }
    }

    public void recycleHelper(Component component) {
        if (mAppearanceHelpers.containsKey(component)) {
            AppearanceHelper recycledHelper = mAppearanceHelpers.remove(component);
            mRecycledHelper.add(recycledHelper);
        }
        if (component instanceof Container<?>) {
            java.util.List<Component> children = ((Container<?>) component).getChildren();
            for (Component child : children) {
                recycleHelper(child);
            }
        }
    }
}
