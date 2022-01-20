/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video.manager;

public interface PlayerManagerProvider {

    String NAME = "PlayerManagerProvider";

    PlayerManager getPlayerManager();
}
