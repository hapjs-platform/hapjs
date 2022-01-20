/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.appearance;

import android.graphics.Rect;
import android.view.View;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;

public class AppearanceHelper {

    public static final int NONE = -1;
    public static final int APPEAR = 0;
    public static final int DISAPPEAR = 1;
    public static boolean mWatchEnabled = true;
    private Rect mVisibleRect = new Rect();
    private Component mAwareChild;
    private boolean mVisible;

    /**
     * @param awareChild child to notify when appearance changed.
     */
    public AppearanceHelper(Component awareChild) {
        mAwareChild = awareChild;
    }

    public boolean isWatchAppearance() {
        return mAwareChild != null && mAwareChild.isWatchAppearance() && mWatchEnabled;
    }

    public boolean isWatchAppearance(int event) {
        return mAwareChild != null && mAwareChild.isWatchAppearance(event) && mWatchEnabled;
    }

    public Component getAwareChild() {
        return mAwareChild;
    }

    public void setAwareChild(Component component) {
        mAwareChild = component;
    }

    public boolean isViewVisible() {
        if (mAwareChild == null) {
            return false;
        }
        View view = mAwareChild.getHostView();
        return view != null && view.isAttachedToWindow() && view.getLocalVisibleRect(mVisibleRect);
    }

    public void updateAppearanceEvent() {
        updateAppearanceEvent(isViewVisible());
    }

    public void updateAppearanceEvent(boolean visible) {
        if (visible == mVisible || mAwareChild == null) {
            return;
        }
        mVisible = !mVisible;
        if (mVisible && isWatchAppearance(APPEAR)) {
            mAwareChild.notifyAppearStateChange(Attributes.Event.APPEAR);
        } else if (!mVisible && isWatchAppearance(DISAPPEAR)) {
            mAwareChild.notifyAppearStateChange(Attributes.Event.DISAPPEAR);
        }
    }

    public void reset() {
        mAwareChild = null;
        mVisible = false;
    }
}
