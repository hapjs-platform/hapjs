/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;

import org.hapjs.analyzer.Analyzer;
import org.hapjs.analyzer.AnalyzerContext;
import org.hapjs.analyzer.AnalyzerStatisticsManager;
import org.hapjs.analyzer.monitors.abs.Monitor;

public abstract class AbsPanel extends RelativeLayout implements AnalyzerPanel {

    public static final int NONE = 0;
    public static final int ANIMATION_BOTTOM_TO_TOP = 1;
    public static final int ANIMATION_TOP_TO_BOTTOM = 2;
    private static final int TRANSITION_DURATION = 200;

    private @POSITION int mPosition;
    protected Context mContext;
    private String mName;
    private Animation mOpenAnimation;
    private Animation mCloseAnimation;


    @IntDef({NONE, ANIMATION_BOTTOM_TO_TOP, ANIMATION_TOP_TO_BOTTOM})
    @interface AnimationType {

    }

    public AbsPanel(Context context, String name, @POSITION int position) {
        super(context);
        mContext = context;
        mName = name;
        mPosition = position;
        LayoutInflater.from(context).inflate(layoutId(), this);
        onCreateFinish();
    }

    @Override
    public final String getName() {
        return mName;
    }

    protected abstract int layoutId();

    @Override
    public int collapseLayoutId() {
        return 0;
    }

    protected void onCreateFinish() {

    }

    @Override
    public View getPanelView() {
        return this;
    }

    @Override
    public final void show() {
        if (isShowing()) {
            return;
        }
        showInternal();
        onShow();
    }

    @Override
    public void show(boolean animation) {
        if (isShowing()) {
            return;
        }
        @AnimationType int animType = getAnimationType(true);
        if (animType == NONE) {
            show();
        } else {
            showInternal();
            showAnimation(animType);
        }
    }

    private void showInternal() {
        PanelDisplay panelDisplay = getAnalyzerContext().getPanelDisplay();
        if (panelDisplay != null) {
            attachToDisplay(panelDisplay);
        }
    }

    private void attachToDisplay(PanelDisplay display) {
        switch (mPosition) {
            case UNDEFINED:
                break;
            case TOP:
                display.addTopPanel(this);
                break;
            case BOTTOM:
                display.addBottomPanel(this);
                break;
            default:
                break;
        }
        display.onPanelShow(this);
    }

    private void detachFromDisplay() {
        PanelDisplay panelDisplay = getAnalyzerContext().getPanelDisplay();
        if (panelDisplay == null) {
            return;
        }
        panelDisplay.dismissConsolePanel();
        detachFromDisplay(panelDisplay);
    }

    private void detachFromDisplay(PanelDisplay display) {
        display.onPanelDismiss(this);
        display.removePanel(this);
    }

    @AnimationType
    protected int getAnimationType(boolean isShow) {
        @AnimationType int animType = NONE;
        if (mPosition == TOP) {
            animType = isShow ? ANIMATION_TOP_TO_BOTTOM : ANIMATION_BOTTOM_TO_TOP;
        } else if (mPosition == BOTTOM) {
            animType = isShow ? ANIMATION_BOTTOM_TO_TOP : ANIMATION_TOP_TO_BOTTOM;
        }
        return animType;
    }

    @Override
    public final void dismiss() {
        if (!isShowing()) {
            return;
        }

        PanelDisplay panelDisplay = getAnalyzerContext().getPanelDisplay();
        if (panelDisplay == null) {
            return;
        }
        onDismiss();
        detachFromDisplay(panelDisplay);
    }

    @Override
    public void dismiss(boolean animation) {
        if (!isShowing()) {
            return;
        }

        @AnimationType int animType = getAnimationType(false);

        if (animType == NONE) {
            dismiss();
            return;
        }
        dismissAnimation(animType);
    }

    @Override
    public boolean isShowing() {
        PanelDisplay panelDisplay = getAnalyzerContext().getPanelDisplay();
        if (panelDisplay != null) {
            return panelDisplay.isPanelShowing(this);
        }
        return false;
    }

    @CallSuper
    void onShow(){
        AnalyzerStatisticsManager.getInstance().recordPanelShow(AbsPanel.this);
    }

    abstract void onDismiss();

    public void onDestroy() {
        mCloseAnimation = null;
        mOpenAnimation = null;
    }

    public AnalyzerContext getAnalyzerContext() {
        return Analyzer.get().getAnalyzerContext();
    }

    @SuppressWarnings("unchecked")
    public <T extends Monitor> T getMonitor(String name) {
        return (T) getAnalyzerContext().getMonitor(name);
    }

    protected void showAnimation(@AnimationType int animType) {
        if (mCloseAnimation != null) {
            mCloseAnimation.cancel();
            mCloseAnimation = null;
        }
        mOpenAnimation = createAnimation(animType, true);
        mOpenAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mOpenAnimation = null;
                onShow();
                onShowAnimationFinished();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        startAnimation(mOpenAnimation);
    }

    protected void onShowAnimationFinished() {
        // do nothing
    }

    protected void onDismissAnimationFinished() {
        // do nothing
    }

    protected void dismissAnimation(@AnimationType int animType) {
        if (mOpenAnimation != null) {
            mOpenAnimation.cancel();
            mOpenAnimation = null;
        }
        mCloseAnimation = createAnimation(animType, false);
        mCloseAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                onDismiss();
                detachFromDisplay();
                onDismissAnimationFinished();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        startAnimation(mCloseAnimation);
    }

    private Animation createAnimation(@AnimationType int type, boolean isEnter) {
        float fromX = 0;
        float fromY = 0;
        float toX = 0;
        float toY = 0;
        switch (type) {
            case ANIMATION_BOTTOM_TO_TOP:
                fromX = 0;
                toX = 0;
                fromY = isEnter ? 1 : 0;
                toY = isEnter ? 0 : 1;
                break;
            case ANIMATION_TOP_TO_BOTTOM:
                fromX = 0;
                toX = 0;
                fromY = isEnter ? -1 : 0;
                toY = isEnter ? 0 : -1;
                break;
            default:
        }
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, fromX, Animation.RELATIVE_TO_SELF, toX,
                Animation.RELATIVE_TO_SELF, fromY, Animation.RELATIVE_TO_SELF, toY);
        animation.setDuration(TRANSITION_DURATION);
        return animation;
    }
}
