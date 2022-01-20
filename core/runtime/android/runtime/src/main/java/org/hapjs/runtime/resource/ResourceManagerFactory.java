/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.Context;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.runtime.ResourceConfig;

public class ResourceManagerFactory {
    private static final ConcurrentHashMap<String, ResourceManager> sManagers =
            new ConcurrentHashMap();

    public static ResourceManager getResourceManager(Context context, String pkg) {
        ResourceManager manager = sManagers.get(pkg);
        if (manager == null) {
            manager = createResourceManager(context, pkg);
            ResourceManager oldManager = sManagers.putIfAbsent(pkg, manager);
            if (oldManager != null) {
                manager = oldManager;
            }
        }
        return manager;
    }

    private static ResourceManager createResourceManager(Context context, String pkg) {
        if (ResourceConfig.getInstance().isLoadFromLocal()) {
            return new LocalResourceManager(context, pkg);
        } else {
            return new RemoteResourceManager(pkg);
        }
    }
}
