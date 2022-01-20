/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import org.hapjs.runtime.ProviderManager;
import org.hapjs.widgets.video.manager.PlayerManager;
import org.hapjs.widgets.video.manager.PlayerManagerProvider;

public class PlayerInstanceManager {

    private static volatile PlayerInstanceManager INSTANCE;
    private static Object LOCK = new Object();
    private PlayerManager mPlayerManager;

    private PlayerInstanceManager() {
        PlayerManager playerManager = null;

        PlayerManagerProvider provider =
                ProviderManager.getDefault().getProvider(PlayerManagerProvider.NAME);
        if (provider != null) {
            playerManager = provider.getPlayerManager();
        }

        if (playerManager == null) {
            playerManager = DefaultPlayerManager.get();
        }
        mPlayerManager = playerManager;
    }

    public static PlayerInstanceManager getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new PlayerInstanceManager();
                }
            }
        }
        return INSTANCE;
    }

    public void setPlayerCountLimit(int limit) {
        mPlayerManager.setPlayerCountLimit(limit);
    }

    <P extends IMediaPlayer> P obtainPlayer(PlayerProxy target) {
        return mPlayerManager.obtainPlayer(target);
    }

    void startUsePlayer(PlayerProxy target) {
        mPlayerManager.startUsePlayer(target);
    }

    void release(IMediaPlayer player) {
        mPlayerManager.release(player);
    }

    public IMediaPlayer createMediaPlayer() {
        return new PlayerProxy();
    }

    public <P extends IMediaPlayer> P createMediaPlayer(Class<P> type) {
        return mPlayerManager.createProxy(type);
    }
}
