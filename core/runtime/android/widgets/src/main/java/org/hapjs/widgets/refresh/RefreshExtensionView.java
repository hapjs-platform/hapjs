/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.refresh;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.widgets.view.refresh.IFooterView;
import org.hapjs.widgets.view.refresh.IHeaderView;
import org.hapjs.widgets.view.refresh.RefreshExtension;
import org.hapjs.widgets.view.refresh.RefreshMovement;
import org.hapjs.widgets.view.refresh.RefreshState;

public class RefreshExtensionView extends PercentFlexboxLayout
        implements IHeaderView<RefreshExtensionView>, IFooterView<RefreshExtensionView> {
    private OnMoveListener mMoveListener;
    private RefreshExtension mRefreshExtension;
    private List<Runnable> mPendings = new ArrayList<>();

    public RefreshExtensionView(Context context) {
        super(context);
    }

    public void setMoveListener(OnMoveListener moveListener) {
        mMoveListener = moveListener;
    }

    @NonNull
    @Override
    public RefreshExtensionView get() {
        return this;
    }

    @Override
    public void bindExpand(RefreshExtension extension) {
        mRefreshExtension = extension;
        for (Runnable runnable : mPendings) {
            runnable.run();
        }
        mPendings.clear();
    }

    @Override
    public void onMove(
            RefreshMovement movement, float moveY, float percent, boolean isDrag,
            boolean isRefreshing) {
        if (mMoveListener != null) {
            mMoveListener.onMove(moveY, percent, isDrag, isRefreshing);
        }
    }

    @Override
    public void onStateChanged(RefreshState state, int oldState, int currentState) {
    }

    @Override
    public boolean apply(String name, Object attribute) {
        return false;
    }

    public void setDragRate(float dragRate) {
        if (mRefreshExtension == null) {
            float finalDragRate = dragRate;
            mPendings.add(() -> setDragRate(finalDragRate));
            return;
        }
        dragRate = Math.abs(dragRate);
        mRefreshExtension.setDragRate(dragRate);
    }

    public void setTriggerRatio(float ratio) {
        if (mRefreshExtension == null) {
            float finalRatio = ratio;
            mPendings.add(() -> setTriggerRatio(finalRatio));
            return;
        }
        ratio = Math.abs(ratio);
        mRefreshExtension.setTriggerRefreshRatio(ratio);
    }

    public void setTriggerSize(int size) {
        if (mRefreshExtension == null) {
            int finalSize = size;
            mPendings.add(() -> setTriggerSize(finalSize));
            return;
        }
        size = Math.abs(size);
        mRefreshExtension.setTriggerRefreshHeight(size);
    }

    public void setMaxDragRatio(float ratio) {
        if (mRefreshExtension == null) {
            float finalRatio = ratio;
            mPendings.add(() -> setMaxDragRatio(finalRatio));
            return;
        }
        ratio = Math.abs(ratio);
        mRefreshExtension.setMaxDragRatio(ratio);
    }

    public void setMaxDragSize(int size) {
        if (mRefreshExtension == null) {
            int finalSize = size;
            mPendings.add(() -> setMaxDragSize(finalSize));
            return;
        }
        size = Math.abs(size);
        mRefreshExtension.setMaxDragHeight(size);
    }

    public void setRefreshDisplayRatio(float ratio) {
        if (mRefreshExtension == null) {
            float finalRatio = ratio;
            mPendings.add(() -> setRefreshDisplayRatio(finalRatio));
            return;
        }
        ratio = Math.abs(ratio);
        mRefreshExtension.setDisplayRatio(ratio);
    }

    public void setRefreshDisplaySize(int size) {
        if (mRefreshExtension == null) {
            int finalSize = size;
            mPendings.add(() -> setRefreshDisplaySize(finalSize));
            return;
        }
        size = Math.abs(size);
        mRefreshExtension.setDisplayHeight(size);
    }

    public void setStyle(int style) {
        if (mRefreshExtension == null) {
            mPendings.add(() -> setStyle(style));
            return;
        }
        if (style == RefreshExtension.STYLE_TRANSLATION
                || style == RefreshExtension.STYLE_FIXED_FRONT
                || style == RefreshExtension.STYLE_FIXED_BEHIND) {
            mRefreshExtension.setStyle(style);
        }
    }

    public void setTranslationWithContent(boolean translationWithContent) {
        if (mRefreshExtension == null) {
            mPendings.add(() -> setTranslationWithContent(translationWithContent));
            return;
        }
        mRefreshExtension.setTranslationWithContentWhenRefreshing(translationWithContent);
    }

    public void setAutoRefresh(boolean autoRefresh) {
        if (mRefreshExtension == null) {
            mPendings.add(() -> setAutoRefresh(autoRefresh));
            return;
        }
        mRefreshExtension.setAutoRefresh(autoRefresh);
    }

    public interface OnMoveListener {
        void onMove(float moveDistance, float percent, boolean isDrag, boolean isRefreshing);
    }
}
