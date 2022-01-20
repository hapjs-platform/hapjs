/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.refresh;

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import org.hapjs.widgets.custom.WidgetProvider;

public abstract class RefreshExtension<V extends View & WidgetProvider.AttributeApplier>
        extends RefreshMovement {
    /**
     * 默认手指拖动的速度
     */
    public static final float DEFAULT_DRAG_RATE = 0.5f;

    /**
     * 默认触发更新的系数，触发更新的高度=header/footer的高度* ratio
     */
    public static final float DEFAULT_TRIGGER_REFRESH_RATIO = 0.7f;

    /**
     * 默认在更新状态下header/footer的显示高度，默认与trigger的高度一致。
     */
    public static final float DEFAULT_DISPLAY_RATIO = 0.7f;

    /**
     * 默认header/footer能够下拉的最大高度系数，默认为header/footer的高度
     */
    public static final float DEFAULT_MAX_DRAG_RATIO = 1f;

    /**
     * header或footer显示在content的顶部上面或者底部下面，content会随着header或者footer移动
     */
    public static final int STYLE_TRANSLATION = 0;

    /**
     * header或footer浮在content的上面，content不会随着header或者footer移动
     */
    public static final int STYLE_FIXED_FRONT = 1;

    /**
     * header或footer浮在content的下面，content不会随着header或者footer移动
     */
    public static final int STYLE_FIXED_BEHIND = 2;
    /**
     * 默认的header/footer显示风格，跟随content移动
     */
    private static final int STYLE_DEFAULT = STYLE_TRANSLATION;
    /**
     * header/footer的显示风格
     */
    private int mStyle = STYLE_DEFAULT;
    /**
     * 拖动时header/footer的移动速度，该速度=View在拖动时的移动距离 / 手势移动的距离。
     */
    private float mDragRate = DEFAULT_DRAG_RATE;
    /**
     * header/footer移动的最大高度比例。该比例=View的移动距离 / View本身的高度 只有在mMaxDragHeight小于等于0生效
     */
    private float mMaxDragRatio = DEFAULT_MAX_DRAG_RATIO;
    /**
     * header/footer移动的最大高度，大于0时生效，否则mMaxDragRatio生效
     */
    private int mMaxDragHeight = 0;
    /**
     * header/footer触发刷新的高度比例。该比例=View的移动距离 / View本身的高度 只有在mTriggerRefreshHeight小于等于0时生效
     */
    private float mTriggerRefreshRatio = DEFAULT_TRIGGER_REFRESH_RATIO;
    /**
     * header/footer移动的最大高度，大于0时生效，否则mTriggerRefreshRatio生效
     */
    private int mTriggerRefreshHeight = 0;
    /**
     * header/footer在刷新的时候显示高度
     */
    private float mDisplayRatio = DEFAULT_DISPLAY_RATIO;
    /**
     * header/footer在刷新的时候显示高度
     */
    private int mDisplayHeight = 0;
    /**
     * 刷新是header/footer是否随着content一起移动 只有style为{@link #STYLE_TRANSLATION}时才会生效
     */
    private boolean mTranslationWithContentWhenRefreshing = false;
    /**
     * 是否自动触发刷新，即当滑动到顶部或者底部时自动弹出header/footer。
     */
    private boolean mAutoRefresh = false;
    private OnStyleChangedListener mStyleChangedListener;

    public RefreshExtension(@NonNull V view) {
        super(view);
    }

    public void setStyleChangedListener(OnStyleChangedListener styleChangedListener) {
        mStyleChangedListener = styleChangedListener;
    }

    public int getStyle() {
        return mStyle;
    }

    public void setStyle(@RefreshStyle int style) {
        int oldStyle = mStyle;
        boolean changed = style != oldStyle;
        mStyle = style;
        if (changed && mStyleChangedListener != null) {
            mStyleChangedListener.onStyleChanged(this, oldStyle, style);
        }
    }

    public float getDragRate() {
        return mDragRate;
    }

    public void setDragRate(float dragRate) {
        mDragRate = dragRate;
    }

    public float getTriggerRefreshRatio() {
        return mTriggerRefreshRatio;
    }

    public void setTriggerRefreshRatio(float triggerRefreshRatio) {
        mTriggerRefreshRatio = triggerRefreshRatio;
    }

    public float getMaxDragRatio() {
        return mMaxDragRatio;
    }

    public void setMaxDragRatio(float maxDragRatio) {
        mMaxDragRatio = maxDragRatio;
    }

    public int getMaxDragHeight() {
        return mMaxDragHeight;
    }

    public void setMaxDragHeight(int maxDragHeight) {
        mMaxDragHeight = maxDragHeight;
    }

    public int getTriggerRefreshHeight() {
        return mTriggerRefreshHeight;
    }

    public void setTriggerRefreshHeight(int triggerRefreshHeight) {
        mTriggerRefreshHeight = triggerRefreshHeight;
    }

    public void setDisplayRatio(float displayRatio) {
        mDisplayRatio = displayRatio;
    }

    public void setDisplayHeight(int displayHeight) {
        mDisplayHeight = displayHeight;
    }

    public void setTranslationWithContentWhenRefreshing(
            boolean translationWithContentWhenRefreshing) {
        mTranslationWithContentWhenRefreshing = translationWithContentWhenRefreshing;
    }

    public boolean canTranslationWithContentWhenRefreshing() {
        return mTranslationWithContentWhenRefreshing;
    }

    public boolean isAutoRefresh() {
        return mAutoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        mAutoRefresh = autoRefresh;
    }

    /**
     * 触发更新条件的大小
     *
     * @return
     */
    public int getMeasuredTriggerRefreshSize() {
        // mTriggerRefreshHeight的优先级比mTriggerRefreshRatio高
        if (mTriggerRefreshHeight > 0) {
            return mTriggerRefreshHeight;
        }
        return (int) (getMeasureHeight() * mTriggerRefreshRatio);
    }

    /**
     * header/footer在刷新状态时的显示大小
     *
     * @return
     */
    public int getMeasuredDisplaySize() {
        // mDisplayHeight的优先级比mDisplayRatio高
        if (mDisplayHeight > 0) {
            return mDisplayHeight;
        }

        return (int) (getMeasureHeight() * mDisplayRatio);
    }

    /**
     * header/footer能够拖动的最大高度
     *
     * @return
     */
    public int getMeasuredDragMaxSize() {
        // mMaxDragHeight的优先级比mMaxDragRatio高
        if (mMaxDragHeight > 0) {
            return mMaxDragHeight;
        }
        return (int) (getMeasureHeight() * mMaxDragRatio);
    }

    /**
     * 计算手指拖动时的阻尼滑动距离
     *
     * @param distance
     * @return
     */
    public abstract float calculateStickyMoving(float distance);

    public abstract void onStateChanged(RefreshState state, int oldState, int currentState);

    public boolean isTouchInExtensionView(MotionEvent event) {
        View view = getView();
        float x = event.getX() - view.getScrollX();
        float y = event.getY() - view.getScrollY();
        return x >= 0 && x <= view.getWidth() && y >= 0 && y < view.getHeight();
    }

    @IntDef({STYLE_TRANSLATION, STYLE_FIXED_FRONT, STYLE_FIXED_BEHIND})
    public @interface RefreshStyle {
    }

    public interface OnStyleChangedListener {
        void onStyleChanged(RefreshExtension extension, int oldStyle, int newStyle);
    }
}
