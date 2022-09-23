/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.hapjs.analyzer.views.CollapsedLayout;
import org.hapjs.analyzer.views.SlideMonitoredRecyclerView;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.R;

import java.util.List;

public abstract class CollapsedPanel extends AbsPanel {
    private static final int PEEK_HEIGHT_DP = 350;
    protected BottomSheetBehavior<FrameLayout> mBottomSheetBehavior;
    private CollapsedLayout mCollapsedLayout;

    public CollapsedPanel(Context context, String name, int position) {
        super(context, name, position);
    }

    @Override
    protected final int layoutId() {
        return R.layout.layout_analyzer_collapsed;
    }

    protected abstract int panelLayoutId();

    @Override
    @CallSuper
    protected void onCreateFinish() {
        super.onCreateFinish();
        mCollapsedLayout = findViewById(R.id.collapsed_layout);
        FrameLayout panelLayout = findViewById(R.id.layout_analyzer_collapsed_content_stub);
        mBottomSheetBehavior = BottomSheetBehavior.from(panelLayout);
        mBottomSheetBehavior.setPeekHeight(DisplayUtil.dip2Pixel(getContext(), PEEK_HEIGHT_DP));
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });

        View panelView = LayoutInflater.from(getContext()).inflate(panelLayoutId(), panelLayout, false);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        panelLayout.addView(panelView, p);
    }

    @Override
    @CallSuper
    void onDismiss() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    @CallSuper
    void onShow() {
        super.onShow();
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void setControlView(View view) {
        if (mCollapsedLayout == null) {
            return;
        }
        if (view != null) {
            view.setOnClickListener(v -> {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        }
    }

    public void addDragShieldView(List<View> views) {
        if (mCollapsedLayout == null || views == null || views.isEmpty()) {
            return;
        }
        mCollapsedLayout.addDragShieldViews(views);
    }

    void setUpRecyclerView(SlideMonitoredRecyclerView recyclerView) {
        if (recyclerView == null || mBottomSheetBehavior == null) {
            return;
        }
        recyclerView.setOnSlideToBottomListener(() -> {
            if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }
}
