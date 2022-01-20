/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video.manager;

import org.hapjs.widgets.video.IMediaPlayer;
import org.hapjs.widgets.video.PlayerProxy;

public interface PlayerManager {

    /**
     * player的实例数量不会被限制
     */
    int PLAYER_COUNT_UNLIMITED = 0;

    /**
     * 设置player的数量限制。
     *
     * @param limit limit > 0,or limit = {@link PlayerManager#PLAYER_COUNT_UNLIMITED}
     */
    void setPlayerCountLimit(int limit);

    <P extends IMediaPlayer> P obtainPlayer(PlayerProxy target);

    void startUsePlayer(PlayerProxy target);

    void release(IMediaPlayer player);

    <P extends IMediaPlayer> P createProxy(Class<P> type);
}
