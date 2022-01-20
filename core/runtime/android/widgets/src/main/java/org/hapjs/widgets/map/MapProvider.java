/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map;

import org.hapjs.bridge.HybridManager;
import org.hapjs.component.bridge.RenderEventCallback;

public interface MapProvider {

    String NAME = "map";

    MapProxy createMapProxy(HybridManager manager);

    MapProxy createMapProxy(HybridManager manager, RenderEventCallback callback);

    String getPlatformKey(String mapName);

    void onMapCreate(String mapName);

    boolean isCompatibleWithVersionKitkat();
}
