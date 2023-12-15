/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.slideview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;

import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.render.Page;
import org.hapjs.widgets.R;

import java.util.List;

public class SlideViewLayout extends ViewGroup implements ComponentHost, GestureHost {

    private static final String TAG = "SlideViewLayout";

    protected static final int STATE_CLOSE = 0;
    protected static final int STATE_CLOSING = 1;
    protected static final int STATE_OPEN = 2;
    protected static final int STATE_OPENING = 3;
    protected static final int STATE_DRAGGING = 4;

    private static final int MIN_FLING_VELOCITY = 300; // px
    // 滑动距离超过这个值，就阻止父控件拦截touchEvent
    private static final int MIN_DIST_REQUEST_DISALLOW_PARENT = 24; // px

    private static final float MAX_SECONDARY_LAYOUT_PROPORTION = 2f / 3;
    private static final float MAX_SLIDING_PROPORTION = 4f / 5;

    public static final int UNDEFINE = Integer.MIN_VALUE;
    private static final int MAX_BUTTON_NUM = 3;
    private static final int DIVIDER_WIDTH = 1; // px

    private static final String BACKGROUND_TYPE_FILL = "fill";
    private static final String BACKGROUND_TYPE_ICON = "icon";

    private static final int TAG_KEY_SEC_CONF = "secondary_confirm_text".hashCode();

    private static final int DEFAULT_ICON_WIDTH = 75;
    private static final int DEFAULT_ICON_HEIGHT = 75;
    private static final int DEFAULT_BACKGROUND_COLOR = ColorUtil.getColor("#f2f2f2");
    private static final int TYPE_ICON_DEFAULT_BACKGROUND_COLOR = ColorUtil.getColor("#ffffff");
    private static final int DEFAULT_TEXT_COLOR = ColorUtil.getColor("#000000");
    private static final int DEFAULT_TEXT_SIZE = 15;

    public static final String SLIDE_EDGE_LEFT = "left";
    public static final String SLIDE_EDGE_RIGHT = "right";

    public static final int DRAG_EDGE_LEFT = 0x1;
    public static final int DRAG_EDGE_RIGHT = 0x1 << 1;

    public static final String LAYER_ABOVE = "above";
    public static final String LAYER_SAME = "same";

    // mainLayout覆盖在secondaryLayout之上
    public static final int LAYER_MODE_ABOVE = 0;
    // mainLayout与secondaryLayout处于同一层
    public static final int LAYER_MODE_SAME = 1;

    private Component mComponent;
    private IGesture mGesture;

    private YogaLayout mMainLayout;
    private FrameLayout mSecondaryLayout;

    private boolean mIsRectInit = false;

    // 分别用于记录mainLayout处于关闭、打开、最大可滑动状态时的位置值
    private Rect mRectMainClose = new Rect();
    private Rect mRectMainOpen = new Rect();
    private Rect mRectMainMaxSlide = new Rect();

    // 分别用于记录secondaryLayout处于关闭、打开、最大可滑动状态时的位置值
    private Rect mRectSecClose = new Rect();
    private Rect mRectSecOpen = new Rect();
    private Rect mRectSecMaxSlide = new Rect();

    private boolean mIsOpenBeforeInit = false;
    private volatile boolean mIsScrolling = false;
    private volatile boolean mEnableSlide = true;
    private boolean mIsSecondaryConfirmShow = true;
    private boolean mIsSecConfExpandAnimPlaying = false;

    private boolean mIsOpened = false;
    private int mState = STATE_CLOSE;
    private int mLayerMode = LAYER_MODE_SAME;

    private int mLastMainLeft = 0;
    private int mLastMainTop = 0;

    private int mEdge = DRAG_EDGE_RIGHT;

    private boolean mIsFlexibleScrolling = false;

    private float mDragDist = 0;
    private float mPrevX = -1;
    private float mPrevY = -1;

    private ViewDragHelper mDragHelper;
    private GestureDetectorCompat mGestureDetector;

    private SlideListener mSwipeListener;
    private ButtonsClickListener mButtonsClickListener;

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    public interface SlideListener {
        void onClosed(SlideViewLayout view);

        void onOpened(SlideViewLayout view);

        void onSlide(SlideViewLayout view, float slideOffset);
    }

    public interface ButtonsClickListener {
        void onButtonClick(SlideViewLayout view, String id, boolean isSecondaryConfirm);
    }

    public SlideViewLayout(Context context) {
        super(context);

        mMainLayout = new YogaLayout(context);
        ViewGroup.MarginLayoutParams mainLayoutLp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mMainLayout, mainLayoutLp);

        mSecondaryLayout = new FrameLayout(context);
        ViewGroup.MarginLayoutParams secondaryLayoutLp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mSecondaryLayout, 0, secondaryLayoutLp);

        ViewDragHelper.Callback callback = new SlideViewDragHelperCallback();
        mDragHelper = ViewDragHelper.create(this, 1.0f, callback);
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_ALL);

        GestureDetector.OnGestureListener onGestureListener = new SlideViewGestureListener();
        mGestureDetector = new GestureDetectorCompat(context, onGestureListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        result |= mGestureDetector.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        mDragHelper.processTouchEvent(event);
        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnableSlide()) {
            return super.onInterceptTouchEvent(ev);
        }

        mDragHelper.processTouchEvent(ev);
        mGestureDetector.onTouchEvent(ev);
        accumulateDragDist(ev);

        boolean couldBecomeClick = couldBecomeClick(ev);
        boolean settling = mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING;
        boolean idleAfterScrolled = mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE
                && mIsScrolling;

        mPrevX = ev.getX();
        mPrevY = ev.getY();

        return !couldBecomeClick && (settling || idleAfterScrolled);
    }

    public YogaLayout getMainLayout() {
        return mMainLayout;
    }

    public FrameLayout getSecondaryLayout() {
        return mSecondaryLayout;
    }

    private void fillSecondaryLayout(List<SlideButtonInfo> buttonInfoList, Component component, Page page) {
        if (buttonInfoList == null || buttonInfoList.isEmpty()) {
            return;
        }
        if (mSecondaryLayout == null) {
            return;
        }
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (layoutInflater == null) {
            Log.e(TAG, "fillSecondaryLayout: layoutInflater is null, can not inflate secondary layout.");
            return;
        }
        mSecondaryLayout.removeAllViews();
        LinearLayout buttonsAreaLayout = new LinearLayout(getContext());
        buttonsAreaLayout.setOrientation(LinearLayout.HORIZONTAL);
        mSecondaryLayout.addView(buttonsAreaLayout, new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT));
        int buttonsCount = Math.min(buttonInfoList.size(), MAX_BUTTON_NUM);
        for (int i = 0; i < buttonsCount; i++) {
            SlideButtonInfo buttonInfo = buttonInfoList.get(i);
            View buttonLayout = layoutInflater.inflate(R.layout.slide_view_item_button, this, false);
            SimpleDraweeView icon = buttonLayout.findViewById(R.id.button_icon);
            TextView text = buttonLayout.findViewById(R.id.button_text);
            boolean isBackgroundTypeIcon = TextUtils.equals(buttonInfo.backgroundType, BACKGROUND_TYPE_ICON);
            int defaultBackgroundColor = isBackgroundTypeIcon ? TYPE_ICON_DEFAULT_BACKGROUND_COLOR : DEFAULT_BACKGROUND_COLOR;
            buttonLayout.setBackgroundColor(buttonInfo.backgroundColor != UNDEFINE ? buttonInfo.backgroundColor : defaultBackgroundColor);
            if (buttonInfo.icon == null) {
                icon.setVisibility(GONE);
            } else {
                icon.setVisibility(VISIBLE);
                icon.setImageURI(buttonInfo.icon);
                if (isBackgroundTypeIcon) {
                    RoundingParams roundingParams = RoundingParams.asCircle();
                    icon.setBackgroundColor(buttonInfo.iconBackgroundColor != UNDEFINE ? buttonInfo.iconBackgroundColor : DEFAULT_BACKGROUND_COLOR);
                    roundingParams.setOverlayColor(buttonInfo.backgroundColor != UNDEFINE ? buttonInfo.backgroundColor : defaultBackgroundColor);
                    icon.getHierarchy().setRoundingParams(roundingParams);
                    icon.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
                }
                ViewGroup.LayoutParams lp = icon.getLayoutParams();
                lp.width = buttonInfo.iconWidth != UNDEFINE ? buttonInfo.iconWidth : DEFAULT_ICON_WIDTH;
                lp.height = buttonInfo.iconHeight != UNDEFINE ? buttonInfo.iconHeight : DEFAULT_ICON_HEIGHT;
                icon.setLayoutParams(lp);
            }

            if (TextUtils.isEmpty(buttonInfo.text) || isBackgroundTypeIcon) {
                text.setVisibility(GONE);
            } else {
                text.setVisibility(VISIBLE);
                text.setText(buttonInfo.text);
                if (null != page && page.isTextSizeAdjustAuto()) {
                    String fontSizeStr = DEFAULT_TEXT_SIZE + Attributes.Unit.DP;
                    int fontsize = Attributes.getInt(mComponent.getHapEngine(), fontSizeStr, component);
                    int defaultFontSize = Attributes.getFontSize(mComponent.getHapEngine(), mComponent.getPage(), fontsize, fontsize, component);
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonInfo.textSize != UNDEFINE ? buttonInfo.textSize : defaultFontSize);
                } else {
                    String fontSizeStr = DEFAULT_TEXT_SIZE + Attributes.Unit.DP;
                    int fontsize = Attributes.getInt(mComponent.getHapEngine(), fontSizeStr, component);
                    int defaultFontSize = Attributes.getFontSize(mComponent.getHapEngine(), mComponent.getPage(), fontsize, fontsize, component);
                    text.setTextSize(buttonInfo.textSize != UNDEFINE ? buttonInfo.textSize : defaultFontSize);
                }
                text.setTextColor(buttonInfo.textColor != UNDEFINE ? buttonInfo.textColor : DEFAULT_TEXT_COLOR);
            }
            buttonLayout.setOnClickListener(v -> {
                if (mButtonsClickListener != null) {
                    mButtonsClickListener.onButtonClick(SlideViewLayout.this, buttonInfo.id, false);
                }
                if (v.getTag(TAG_KEY_SEC_CONF) != null) {
                    showSecondaryConfirm(v);
                }
            });

            int layoutWidth = buttonInfo.buttonWidth != UNDEFINE ? buttonInfo.buttonWidth : LinearLayout.LayoutParams.WRAP_CONTENT;
            MarginLayoutParams lp = new MarginLayoutParams(layoutWidth, LinearLayout.LayoutParams.MATCH_PARENT);
            if (i != buttonsCount - 1) {
                lp.rightMargin = DIVIDER_WIDTH;
            }
            buttonsAreaLayout.addView(buttonLayout, lp);

            if (buttonInfo.secondaryConfirmInfo != null && buttonInfo.secondaryConfirmInfo.isValid()) {
                SecondaryConfirmInfo secConfHolder = buttonInfo.secondaryConfirmInfo;
                TextView secConfText = new TextView(getContext());
                secConfText.setGravity(Gravity.CENTER);
                secConfText.setVisibility(INVISIBLE);
                secConfText.setText(secConfHolder.text);
                if (null != page && page.isTextSizeAdjustAuto()) {
                    int fontsize = DisplayUtil.dp2px(getContext(), DEFAULT_TEXT_SIZE);
                    int defaultFontSize = Attributes.getFontSize(mComponent.getHapEngine(), mComponent.getPage(), fontsize, fontsize, component);
                    secConfText.setTextSize(TypedValue.COMPLEX_UNIT_PX, secConfHolder.textSize != UNDEFINE ? secConfHolder.textSize : defaultFontSize);
                } else {
                    int defaultFontSize = Attributes.getFontSize(mComponent.getHapEngine(), mComponent.getPage(), DEFAULT_TEXT_SIZE, DEFAULT_TEXT_SIZE, component);
                    secConfText.setTextSize(secConfHolder.textSize != UNDEFINE ? secConfHolder.textSize : defaultFontSize);
                }
                int holderTextColor = buttonInfo.textColor != UNDEFINE ? buttonInfo.textColor : DEFAULT_TEXT_COLOR;
                secConfText.setTextColor(secConfHolder.textColor != UNDEFINE ? secConfHolder.textColor : holderTextColor);
                secConfText.setBackgroundColor(buttonInfo.backgroundColor != UNDEFINE ? buttonInfo.backgroundColor : DEFAULT_BACKGROUND_COLOR);
                buttonLayout.setTag(TAG_KEY_SEC_CONF, secConfText);
                secConfText.setOnClickListener(v -> {
                    if (mButtonsClickListener != null) {
                        mButtonsClickListener.onButtonClick(SlideViewLayout.this, buttonInfo.id, true);
                    }
                });
                mSecondaryLayout.addView(secConfText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;
        int desiredHeight = 0;

        if (mMainLayout != null) {
            measureChild(mMainLayout, widthMeasureSpec, heightMeasureSpec);

            final int mainLayoutWidth = mMainLayout.getMeasuredWidth();
            final int mainLayoutHeight = mMainLayout.getMeasuredHeight();

            desiredWidth = mainLayoutWidth;
            desiredHeight = mainLayoutHeight;
        }

        if (mSecondaryLayout != null && mSecondaryLayout.getChildCount() > 0) {
            ViewGroup.LayoutParams secondaryLayoutLp = mSecondaryLayout.getLayoutParams();

            measureChild(mSecondaryLayout, widthMeasureSpec, heightMeasureSpec);

            LinearLayout buttonsAreaLayout = (LinearLayout) mSecondaryLayout.getChildAt(0);
            int buttonsAreaLayoutMeasuredWidth = buttonsAreaLayout.getMeasuredWidth();

            int buttonLayoutTotalWidth = 0;
            int dividerTotalWidth = (buttonsAreaLayout.getChildCount() - 1) * DIVIDER_WIDTH;
            for (int i = 0; i < buttonsAreaLayout.getChildCount(); i++) {
                View buttonLayout = buttonsAreaLayout.getChildAt(i);
                buttonLayoutTotalWidth += buttonLayout.getMeasuredWidth();
            }

            int buttonsAreaLayoutMaxWidth = (int) (desiredWidth * MAX_SECONDARY_LAYOUT_PROPORTION);

            if (buttonLayoutTotalWidth > buttonsAreaLayoutMaxWidth) {
                for (int i = 0; i < buttonsAreaLayout.getChildCount(); i++) {
                    View buttonLayout = buttonsAreaLayout.getChildAt(i);
                    float scale = (float) buttonLayout.getMeasuredWidth() / buttonLayoutTotalWidth;
                    ViewGroup.LayoutParams buttonLp = buttonLayout.getLayoutParams();
                    if (buttonLp != null) {
                        buttonLp.width = (int) (buttonsAreaLayoutMaxWidth * scale);
                        buttonLayout.setLayoutParams(buttonLp);
                    }
                }

            }

            secondaryLayoutLp.width = Math.min(buttonsAreaLayoutMaxWidth + dividerTotalWidth, buttonsAreaLayoutMeasuredWidth);
            secondaryLayoutLp.height = desiredHeight;
            mSecondaryLayout.setLayoutParams(secondaryLayoutLp);
        }

        desiredWidth += getPaddingLeft() + getPaddingRight();
        desiredHeight += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(desiredWidth, desiredHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int minLeft = getPaddingLeft();
        final int maxRight = Math.max(r - getPaddingRight() - l, 0);
        final int minTop = getPaddingTop();
        final int maxBottom = Math.max(b - getPaddingBottom() - t, 0);

        for (int index = 0; index < getChildCount(); index++) {
            final View child = getChildAt(index);

            int measuredChildHeight = child.getMeasuredHeight();
            int measuredChildWidth = child.getMeasuredWidth();

            int left, right, top, bottom;
            left = right = top = bottom = 0;

            switch (mEdge) {
                case DRAG_EDGE_LEFT:
                    left = Math.min(getPaddingLeft(), maxRight);
                    top = Math.min(getPaddingTop(), maxBottom);
                    right = Math.min(measuredChildWidth + getPaddingLeft(), maxRight);
                    bottom = Math.min(measuredChildHeight + getPaddingTop(), maxBottom);
                    break;

                case DRAG_EDGE_RIGHT:
                    left = Math.max(r - measuredChildWidth - getPaddingRight() - l, minLeft);
                    top = Math.min(getPaddingTop(), maxBottom);
                    right = Math.max(r - getPaddingRight() - l, minLeft);
                    bottom = Math.min(measuredChildHeight + getPaddingTop(), maxBottom);
                    break;

            }

            child.layout(left, top, right, bottom);
        }

        if (mLayerMode == LAYER_MODE_SAME) {
            switch (mEdge) {
                case DRAG_EDGE_LEFT:
                    mSecondaryLayout.offsetLeftAndRight(-mSecondaryLayout.getWidth());
                    break;

                case DRAG_EDGE_RIGHT:
                    mSecondaryLayout.offsetLeftAndRight(mSecondaryLayout.getWidth());
                    break;
            }
        }

        initRects();

        if (mIsOpenBeforeInit) {
            open(false);
        } else {
            close(false);
        }

        mLastMainLeft = mMainLayout.getLeft();
        mLastMainTop = mMainLayout.getTop();
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void open(boolean animation) {
        mIsOpenBeforeInit = true;

        if (animation) {
            if (mState == STATE_OPENING) {
                return;
            }
            mState = STATE_OPENING;
            // 会触发SlideListener回调
            mDragHelper.smoothSlideViewTo(mMainLayout, mRectMainOpen.left, mRectMainOpen.top);
        } else {
            mState = STATE_OPEN;
            mIsOpened = true;
            mDragHelper.abort();

            // 不会触发SlideListener回调
            mMainLayout.layout(
                    mRectMainOpen.left,
                    mRectMainOpen.top,
                    mRectMainOpen.right,
                    mRectMainOpen.bottom
            );

            mSecondaryLayout.layout(
                    mRectSecOpen.left,
                    mRectSecOpen.top,
                    mRectSecOpen.right,
                    mRectSecOpen.bottom
            );
        }

        ViewCompat.postInvalidateOnAnimation(SlideViewLayout.this);
    }

    public void close(boolean animation) {
        mIsOpenBeforeInit = false;

        if (animation) {
            if (mState == STATE_CLOSING) {
                return;
            }
            mState = STATE_CLOSING;
            // 会触发SlideListener回调
            mDragHelper.smoothSlideViewTo(mMainLayout, mRectMainClose.left, mRectMainClose.top);
        } else {
            mState = STATE_CLOSE;
            mIsOpened = false;
            mDragHelper.abort();

            // 不会触发SlideListener回调
            mMainLayout.layout(
                    mRectMainClose.left,
                    mRectMainClose.top,
                    mRectMainClose.right,
                    mRectMainClose.bottom
            );

            mSecondaryLayout.layout(
                    mRectSecClose.left,
                    mRectSecClose.top,
                    mRectSecClose.right,
                    mRectSecClose.bottom
            );
        }

        ViewCompat.postInvalidateOnAnimation(SlideViewLayout.this);
    }

    public void showSecondaryConfirm(View buttonLayout) {
        if (mIsSecConfExpandAnimPlaying) {
            return;
        }

        TextView secConfText = ((TextView) buttonLayout.getTag(TAG_KEY_SEC_CONF));

        secConfText.setLeft(buttonLayout.getLeft());
        secConfText.setRight(buttonLayout.getRight());

        ValueAnimator secConfExpandLeftAnim = ObjectAnimator.ofInt(secConfText, "left", buttonLayout.getLeft(), 0);
        ValueAnimator secConfExpandRightAnim = ObjectAnimator.ofInt(secConfText, "right", buttonLayout.getRight(), mRectSecOpen.width());
        AnimatorSet secConfExpandAnimSet = new AnimatorSet();
        secConfExpandAnimSet.playTogether(secConfExpandLeftAnim, secConfExpandRightAnim);
        secConfExpandAnimSet.setDuration(100);
        secConfExpandAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsSecondaryConfirmShow = true;
                mIsSecConfExpandAnimPlaying = false;
            }
        });
        secConfText.setVisibility(VISIBLE);
        mIsSecondaryConfirmShow = true;
        secConfExpandAnimSet.start();
    }

    public void hideSecondaryConfirm() {
        View buttonsLayout = mSecondaryLayout.getChildAt(0);

        buttonsLayout.setVisibility(VISIBLE);
        for (int i = 1; i < mSecondaryLayout.getChildCount(); i++) {
            View secConfText = mSecondaryLayout.getChildAt(i);
            secConfText.setVisibility(INVISIBLE);
        }

        mIsSecondaryConfirmShow = false;
    }

    public void setEdge(String edge) {
        switch (edge) {
            case SLIDE_EDGE_LEFT:
                mEdge = DRAG_EDGE_LEFT;
                break;
            case SLIDE_EDGE_RIGHT:
                mEdge = DRAG_EDGE_RIGHT;
                break;
            default:
                Log.e(TAG, String.format("setSlideEdge: %s is invalid.", edge));
                break;
        }
    }

    public int getEdge() {
        return mEdge;
    }

    public void setSwipeListener(SlideListener listener) {
        mSwipeListener = listener;
    }

    public void setButtonsClickListener(ButtonsClickListener listener) {
        mButtonsClickListener = listener;
    }

    public void setEnableSlide(boolean enableSlide) {
        mEnableSlide = enableSlide;
    }

    public boolean isEnableSlide() {
        boolean isButtonsAreaLayoutEmpty = mSecondaryLayout == null || mSecondaryLayout.getChildCount() == 0;
        return mEnableSlide && !isButtonsAreaLayoutEmpty;
    }

    public void setIsOpen(boolean isOpen) {
        if (isOpen) {
            open(false);
        } else {
            close(false);
        }
    }

    public void setLayer(String layer) {
        switch (layer) {
            case LAYER_ABOVE:
                mLayerMode = LAYER_MODE_ABOVE;
                break;
            case LAYER_SAME:
                mLayerMode = LAYER_MODE_SAME;
                break;
            default:
                Log.e(TAG, String.format("setLayer: %s is invalid.", layer));
                break;
        }
    }

    public void setButtons(List<SlideButtonInfo> buttonInfoList, Component component, Page page) {
        fillSecondaryLayout(buttonInfoList, component, page);
    }


    public boolean isOpened() {
        return (mState == STATE_OPEN);
    }

    public boolean isClosed() {
        return (mState == STATE_CLOSE);
    }

    private int getMainOpenLeft() {
        switch (mEdge) {
            case DRAG_EDGE_LEFT:
                return mRectMainClose.left + mSecondaryLayout.getWidth();

            case DRAG_EDGE_RIGHT:
                return mRectMainClose.left - mSecondaryLayout.getWidth();

            default:
                Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                return 0;
        }
    }

    private int getSecOpenLeft() {
        if (mLayerMode == LAYER_MODE_ABOVE) {
            return mRectSecClose.left;
        }

        switch (mEdge) {
            case DRAG_EDGE_LEFT:
                return mRectSecClose.left + mSecondaryLayout.getWidth();

            case DRAG_EDGE_RIGHT:
                return mRectSecClose.left - mSecondaryLayout.getWidth();

            default:
                Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                return 0;
        }
    }

    private int getMainMaxSlideLeft() {
        switch (mEdge) {
            case DRAG_EDGE_LEFT:
                return mRectMainClose.left + getHorizontalMaxSlideOffset();

            case DRAG_EDGE_RIGHT:
                return mRectMainClose.left - getHorizontalMaxSlideOffset();

            default:
                Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                return 0;
        }
    }

    private int getSecMaxSlideLeft() {
        switch (mEdge) {
            case DRAG_EDGE_LEFT:
                return mRectSecOpen.left;

            case DRAG_EDGE_RIGHT:
                return mRectSecOpen.right - getHorizontalMaxSlideOffset();

            default:
                Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                return 0;
        }
    }

    private int getSecMaxSlideRight() {
        switch (mEdge) {
            case DRAG_EDGE_LEFT:
                return mRectSecOpen.left + getHorizontalMaxSlideOffset();

            case DRAG_EDGE_RIGHT:
                return mRectSecOpen.right;

            default:
                Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                return 0;
        }
    }

    private int getHorizontalMaxSlideOffset() {
        return (int) (mMainLayout.getWidth() * MAX_SLIDING_PROPORTION);
    }

    private void initRects() {
        if (mIsRectInit) {
            return;
        }

        mRectMainClose.set(
                mMainLayout.getLeft(),
                mMainLayout.getTop(),
                mMainLayout.getRight(),
                mMainLayout.getBottom()
        );

        mRectSecClose.set(
                mSecondaryLayout.getLeft(),
                mSecondaryLayout.getTop(),
                mSecondaryLayout.getRight(),
                mSecondaryLayout.getBottom()
        );

        mRectMainOpen.set(
                getMainOpenLeft(),
                mRectMainClose.top,
                getMainOpenLeft() + mMainLayout.getWidth(),
                mRectMainClose.top + mMainLayout.getHeight()
        );

        mRectSecOpen.set(
                getSecOpenLeft(),
                mRectSecClose.top,
                getSecOpenLeft() + mSecondaryLayout.getWidth(),
                mRectSecClose.top + mSecondaryLayout.getHeight()
        );

        mRectMainMaxSlide.set(
                getMainMaxSlideLeft(),
                mRectMainClose.top,
                getMainMaxSlideLeft() + mMainLayout.getWidth(),
                mRectMainClose.top + mMainLayout.getHeight()
        );

        mRectSecMaxSlide.set(
                getSecMaxSlideLeft(),
                mRectSecOpen.top,
                getSecMaxSlideRight(),
                mRectSecOpen.bottom
        );

        mIsRectInit = true;
    }

    private boolean couldBecomeClick(MotionEvent ev) {
        return isInMainLayout(ev) && !shouldInitiateADrag();
    }

    private boolean isInMainLayout(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();

        boolean withinVertical = mMainLayout.getTop() <= y && y <= mMainLayout.getBottom();
        boolean withinHorizontal = mMainLayout.getLeft() <= x && x <= mMainLayout.getRight();

        return withinVertical && withinHorizontal;
    }

    private boolean shouldInitiateADrag() {
        float minDistToInitiateDrag = mDragHelper.getTouchSlop();
        return mDragDist >= minDistToInitiateDrag;
    }

    private void accumulateDragDist(MotionEvent ev) {
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mDragDist = 0;
            return;
        }

        boolean dragHorizontally = getEdge() == DRAG_EDGE_LEFT ||
                getEdge() == DRAG_EDGE_RIGHT;

        float dragged;
        if (dragHorizontally) {
            dragged = Math.abs(ev.getX() - mPrevX);
        } else {
            dragged = Math.abs(ev.getY() - mPrevY);
        }

        mDragDist += dragged;
    }

    private class SlideViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean hasDisallowed = false;

        @Override
        public boolean onDown(MotionEvent e) {
            mIsScrolling = false;
            hasDisallowed = false;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mIsScrolling = true;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mIsScrolling = true;

            if (getParent() != null) {
                boolean shouldDisallow;

                if (!hasDisallowed) {
                    shouldDisallow = getDistToClosestEdge() >= MIN_DIST_REQUEST_DISALLOW_PARENT || mIsFlexibleScrolling;
                    if (shouldDisallow) {
                        hasDisallowed = true;
                    }
                } else {
                    shouldDisallow = true;
                }

                getParent().requestDisallowInterceptTouchEvent(shouldDisallow);
            }

            return false;
        }
    }

    private int getDistToClosestEdge() {
        switch (mEdge) {
            case DRAG_EDGE_LEFT:
                final int pivotRight = mRectMainClose.left + mSecondaryLayout.getWidth();

                return Math.min(
                        Math.min(mMainLayout.getLeft() - mRectMainClose.left, mSecondaryLayout.getWidth()),
                        Math.max(0, pivotRight - mMainLayout.getLeft())
                );

            case DRAG_EDGE_RIGHT:
                final int pivotLeft = mRectMainClose.right - mSecondaryLayout.getWidth();

                return Math.min(
                        Math.max(0, mMainLayout.getRight() - pivotLeft),
                        Math.min(mRectMainClose.right - mMainLayout.getRight(), mSecondaryLayout.getWidth())
                );

            default:
                Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                return 0;
        }
    }

    private int getHalfwayPivotHorizontal() {
        if (mEdge == DRAG_EDGE_LEFT) {
            return mRectMainClose.left + mSecondaryLayout.getWidth() / 2;
        } else {
            return mRectMainClose.right - mSecondaryLayout.getWidth() / 2;
        }
    }

    private class SlideViewDragHelperCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {

            if (!isEnableSlide()) {
                return false;
            }

            mDragHelper.captureChildView(mMainLayout, pointerId);
            return false;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int validSlideLeft;
            switch (mEdge) {
                case DRAG_EDGE_LEFT:
                    validSlideLeft = Math.min(Math.max(left, mRectMainClose.left), mRectMainMaxSlide.left);
                    mIsFlexibleScrolling = validSlideLeft > mRectMainOpen.left;
                    return validSlideLeft;

                case DRAG_EDGE_RIGHT:
                    validSlideLeft = Math.max(Math.min(left, mRectMainClose.left), mRectMainMaxSlide.left);
                    mIsFlexibleScrolling = validSlideLeft < mRectMainOpen.left;
                    return validSlideLeft;

                default:
                    return child.getLeft();
            }
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            switch (mEdge) {
                case DRAG_EDGE_LEFT:
                case DRAG_EDGE_RIGHT:
                    return getHorizontalMaxSlideOffset();
                default:
                    return super.getViewHorizontalDragRange(child);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final boolean velRightExceeded = xvel >= MIN_FLING_VELOCITY;
            final boolean velLeftExceeded = xvel <= -MIN_FLING_VELOCITY;

            final int pivotHorizontal = getHalfwayPivotHorizontal();

            switch (mEdge) {
                case DRAG_EDGE_LEFT:
                    if (velRightExceeded) {
                        open(true);
                    } else if (velLeftExceeded) {
                        close(true);
                    } else {
                        if (mMainLayout.getLeft() < pivotHorizontal) {
                            close(true);
                        } else {
                            open(true);
                        }
                    }
                    break;

                case DRAG_EDGE_RIGHT:
                    if (velRightExceeded) {
                        close(true);
                    } else if (velLeftExceeded) {
                        open(true);
                    } else {
                        if (mMainLayout.getRight() < pivotHorizontal) {
                            open(true);
                        } else {
                            close(true);
                        }
                    }
                    break;
            }
        }

        @Override
        public boolean onEdgeLock(int edgeFlags) {
            return !isEnableSlide();
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            if (!isEnableSlide()) {
                return;
            }

            boolean edgeStartLeft = (mEdge == DRAG_EDGE_RIGHT)
                    && edgeFlags == ViewDragHelper.EDGE_LEFT;

            boolean edgeStartRight = (mEdge == DRAG_EDGE_LEFT)
                    && edgeFlags == ViewDragHelper.EDGE_RIGHT;

            if (edgeStartLeft || edgeStartRight) {
                mDragHelper.captureChildView(mMainLayout, pointerId);
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            if (mLayerMode == LAYER_MODE_SAME || mIsFlexibleScrolling) {
                int l, t, r, b;
                switch (mEdge) {
                    case DRAG_EDGE_LEFT:
                        l = left - mRectSecOpen.width();
                        t = mRectSecOpen.top;
                        r = left;
                        b = mRectSecOpen.bottom;
                        break;
                    case DRAG_EDGE_RIGHT:
                        l = left + mRectMainOpen.width();
                        t = mRectSecOpen.top;
                        r = left + mRectMainOpen.width() + mRectSecOpen.width();
                        b = mRectSecOpen.bottom;
                        break;
                    default:
                        Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                        l = t = r = b = 0;
                        break;
                }
                mSecondaryLayout.layout(l, t, r, b);
            }

            boolean isMoved = (mMainLayout.getLeft() != mLastMainLeft) || (mMainLayout.getTop() != mLastMainTop);
            if (isMoved) {
                if (mMainLayout.getLeft() == mRectMainClose.left && mMainLayout.getTop() == mRectMainClose.top
                        && mIsOpened) {
                    if (mSwipeListener != null) {
                        mSwipeListener.onClosed(SlideViewLayout.this);
                    }
                    if (mIsSecondaryConfirmShow) {
                        hideSecondaryConfirm();
                    }
                } else if (mMainLayout.getLeft() == mRectMainOpen.left && mMainLayout.getTop() == mRectMainOpen.top
                        && !mIsOpened && mState != STATE_DRAGGING) {
                    if (mSwipeListener != null) {
                        mSwipeListener.onOpened(SlideViewLayout.this);
                    }
                }
                if (mSwipeListener != null) {
                    mSwipeListener.onSlide(SlideViewLayout.this, getSlideOffset());
                }
            }

            mLastMainLeft = mMainLayout.getLeft();
            mLastMainTop = mMainLayout.getTop();
            ViewCompat.postInvalidateOnAnimation(SlideViewLayout.this);
        }

        private float getSlideOffset() {
            switch (mEdge) {
                case DRAG_EDGE_LEFT:
                    return (float) (mMainLayout.getLeft() - mRectMainClose.left) / mSecondaryLayout.getWidth();

                case DRAG_EDGE_RIGHT:
                    return (float) (mRectMainClose.left - mMainLayout.getLeft()) / mSecondaryLayout.getWidth();

                default:
                    Log.e(TAG, String.format("getSecOpenLeft: mEdge: %s is invalid.", mEdge));
                    return 0;
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);

            switch (state) {
                case ViewDragHelper.STATE_DRAGGING:
                    mState = STATE_DRAGGING;
                    break;

                case ViewDragHelper.STATE_IDLE:
                    if (mEdge == DRAG_EDGE_LEFT || mEdge == DRAG_EDGE_RIGHT) {
                        if (mMainLayout.getLeft() == mRectMainClose.left) {
                            mState = STATE_CLOSE;
                            mIsOpened = false;
                        } else {
                            mState = STATE_OPEN;
                            mIsOpened = true;
                        }
                    }
                    break;
            }
        }
    }

}
