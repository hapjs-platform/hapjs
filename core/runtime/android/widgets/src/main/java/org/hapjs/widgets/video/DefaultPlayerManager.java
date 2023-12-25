/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hapjs.runtime.Runtime;
import org.hapjs.widgets.video.manager.PlayerManager;

public class DefaultPlayerManager implements PlayerManager {

    /**
     * player的数据默认被限制在3个以内，当超过3个时，会将最早的player释放掉
     */
    private static final int DEFAULT_PLAYER_COUNT_LIMIT = 3;
    private static final Object LOCK = new Object();
    private static volatile DefaultPlayerManager INSTANCE;
    private Context mContext;
    private int mPlayerCountLimit = DEFAULT_PLAYER_COUNT_LIMIT;
    private List<PlayerHolder> mInstances;
    private static final String TAG = "DefaultPlayerManager";

    private DefaultPlayerManager() {
        mInstances = new ArrayList<>(DEFAULT_PLAYER_COUNT_LIMIT);
        mContext = Runtime.getInstance().getContext().getApplicationContext();
    }

    public static DefaultPlayerManager get() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new DefaultPlayerManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 设置player的数量限制。
     *
     * @param limit limit > 0,or limit = {@link DefaultPlayerManager#PLAYER_COUNT_UNLIMITED}
     */
    @Override
    public void setPlayerCountLimit(int limit) {
        if (limit == PLAYER_COUNT_UNLIMITED) {
            mPlayerCountLimit = limit;
        } else if (limit > 0) {
            mPlayerCountLimit = limit;
            trimToSize(mPlayerCountLimit);
        }
    }

    @NonNull
    @Override
    public <P extends IMediaPlayer> P obtainPlayer(PlayerProxy target) {
        if (!hasLimit()) {
            PlayerHolder holder = createInternal(target);
            return (P) holder.mPlayer;
        }

        trimToSize(mPlayerCountLimit);
        PlayerHolder holder = findAvaiableIdlePlayer(target);
        if (holder != null) {
            // 有可用的空闲player
            holder.mTarget.unbind();
            holder.mTarget = target;
            // move the holder to last
            mInstances.remove(holder);
            mInstances.add(holder);
        } else {
            // 如果找不到空闲的player，将最早的player移除掉
            trimToSize(mPlayerCountLimit - 1);
            holder = createInternal(target);
        }
        return (P) holder.mPlayer;
    }

    @Override
    public void startUsePlayer(PlayerProxy target) {
        int size = mInstances.size();
        for (int index = 0; index < size; index++) {
            PlayerHolder holder = mInstances.get(index);
            if (Objects.equals(holder.mTarget, target) && index != size - 1) {
                mInstances.remove(index);
                mInstances.add(holder);
                break;
            }
        }
    }

    private boolean hasLimit() {
        return mPlayerCountLimit != PLAYER_COUNT_UNLIMITED;
    }

    private PlayerHolder findAvaiableIdlePlayer(PlayerProxy target) {
        int size = mInstances.size();
        for (int i = 0; i < size; i++) {
            PlayerHolder holder = mInstances.get(i);
            Player player = holder.mPlayer;
            if (null != player && null != target) {
                String mark = player.getMark();
                String targetMark = target.getMark();
                if (!TextUtils.isEmpty(targetMark) && !targetMark.equals(mark)) {
                    Log.w(TAG, "findAvaiableIdlePlayer targetMark mark not same.");
                    continue;
                }
            }
            int targetState = player.getTargetState();
            int currentState = player.getCurrentState();
            if (targetState == currentState
                    && (currentState == IMediaPlayer.STATE_IDLE
                    || currentState == IMediaPlayer.STATE_ERROR
                    || currentState == IMediaPlayer.STATE_STOP)) {

                return holder;
            }
        }
        return null;
    }

    private PlayerHolder createInternal(PlayerProxy target) {
        Player player = new ExoPlayer(mContext, (null != target ? target.getMark() : null));
        PlayerHolder holder = new PlayerHolder(player, target);
        mInstances.add(holder);
        return holder;
    }

    @Override
    public void release(IMediaPlayer player) {
        if (player == null) {
            return;
        }

        int size = mInstances.size();
        for (int i = 0; i < size; i++) {
            PlayerHolder holder = mInstances.get(i);
            if (holder.mPlayer == player) {
                mInstances.remove(i);
                holder.mTarget.unbind();
                holder.mTarget = null;
                holder.mPlayer.release();
                holder.mPlayer = null;
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends IMediaPlayer> P createProxy(Class<P> type) {
        return (P) new PlayerProxy<>();
    }

    private void trimToSize(int size) {
        if (size > mPlayerCountLimit) {
            size = mPlayerCountLimit;
        } else if (size < 0) {
            size = 0;
        }

        int totalSize = mInstances.size();
        if (totalSize > size) {
            int length = totalSize - size;
            for (int i = 0; i < length; i++) {
                PlayerHolder holder = mInstances.remove(0);
                holder.mTarget.unbind();
                holder.mPlayer.release();
                holder.mPlayer = null;
            }
        }
    }

    private static class PlayerHolder {
        Player mPlayer;
        PlayerProxy mTarget;

        PlayerHolder(Player player, PlayerProxy proxy) {
            mPlayer = player;
            mTarget = proxy;
        }
    }
}
