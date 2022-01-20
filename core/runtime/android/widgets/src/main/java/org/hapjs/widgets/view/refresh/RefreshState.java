/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

public class RefreshState {
    public static final int PULL_DOWN_IDLE = 1; // 静止状态
    public static final int PULL_DOWN_TO_REFRESH = 1 << 1; // 下拉状态，还没有刷新
    public static final int PULL_DOWN_REFRESHING = 1 << 2; // 正在刷新
    public static final int PULL_DOWN_TO_RELEASE = 1 << 3; // 刷新完成，正在回到原来位置

    public static final int PULL_UP_IDLE = 1 << 16;
    public static final int PULL_UP_TO_REFRESH = 1 << 17;
    public static final int PULL_UP_REFRESHING = 1 << 18;
    public static final int PULL_UP_TO_RELEASE = 1 << 19;

    private static final int HEADER_MASK = 0xFFFF;
    private static final int FOOTER_MASK = 0xFFFF0000;

    // 低16位表示header状态，高16位表示footer状态
    private int mState = PULL_DOWN_IDLE | PULL_UP_IDLE;

    private boolean mExtendedDisplay = false;

    public static boolean isHeaderState(int state) {
        return (state & HEADER_MASK) != 0;
    }

    public static boolean isFooterState(int state) {
        return (state & FOOTER_MASK) != 0;
    }

    public void setState(int state) {
        // 设置下拉的状态
        if ((state & HEADER_MASK) != 0) {
            // 清除下拉的状态
            mState &= ~HEADER_MASK;
            // 更新下拉的状态
            mState |= state;
        }

        // 设置上拉的状态
        if ((state & FOOTER_MASK) != 0) {
            // 清除上拉的状态
            mState &= ~FOOTER_MASK;
            // 更新上拉的状态
            mState |= state;
        }
    }

    public int getHeaderState() {
        return mState & HEADER_MASK;
    }

    public int getFooterState() {
        return mState & FOOTER_MASK;
    }

    public boolean isPullDownIDLE() {
        return (mState & PULL_DOWN_IDLE) != 0;
    }

    public boolean isPullDownToRefresh() {
        return (mState & PULL_DOWN_TO_REFRESH) != 0;
    }

    public boolean isPullDownRefreshing() {
        return (mState & PULL_DOWN_REFRESHING) != 0;
    }

    public boolean isPullDownToRelease() {
        return (mState & PULL_DOWN_TO_RELEASE) != 0;
    }

    public boolean isPullUpIDLE() {
        return (mState & PULL_UP_IDLE) != 0;
    }

    public boolean isPullUpToRefresh() {
        return (mState & PULL_UP_TO_REFRESH) != 0;
    }

    public boolean isPullUpRefreshing() {
        return (mState & PULL_UP_REFRESHING) != 0;
    }

    public boolean isPullUpToRelease() {
        return (mState & PULL_UP_TO_RELEASE) != 0;
    }

    public void setExtendedDisplay(boolean extendedDisplay) {
        mExtendedDisplay = extendedDisplay;
    }
}
