/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.ExtensionMetaData;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;

public abstract class ModuleExtension extends AbstractExtension {
    @Override
    public ExtensionMetaData getMetaData() {
        return ModuleBridge.getModuleMap().get(getName());
    }

    public abstract void attach(RootView rootView, PageManager pageManager, AppInfo appInfo);
}
