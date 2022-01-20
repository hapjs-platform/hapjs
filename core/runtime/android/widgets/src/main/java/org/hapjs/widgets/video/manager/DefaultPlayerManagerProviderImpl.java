/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video.manager;

import org.hapjs.widgets.video.DefaultPlayerManager;

public class DefaultPlayerManagerProviderImpl implements PlayerManagerProvider {

    @Override
    public PlayerManager getPlayerManager() {
        return DefaultPlayerManager.get();
    }
}
