/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.readerdiv;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.VelocityTrackerCompat;
import androidx.core.widget.ScrollerCompat;

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;

import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.widgets.R;
import org.hapjs.widgets.ReaderDiv;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static androidx.customview.widget.ViewDragHelper.INVALID_POINTER;
import static org.hapjs.widgets.view.readerdiv.ReaderLayoutView.READER_LOG_TAG;
import static org.hapjs.widgets.view.readerdiv.ReaderPageView.PageCallback.TOUCH_MOVE_TYPE;

/**
 * 小说页面
 * <p>
 * 1、章节标题  需要自定义
 * 2、小说原文
 */
public class ReaderPageView extends PercentFlexboxLayout {
    private final String TAG = "ReaderPageView";
    private PageCallback mPageCallback = null;
    private List<String> mPageData = new ArrayList<>();
    private int mLineY;
    private CharSequence mText = null;
    private int mLineHeight = 0;
    private int mFontSize = 30;
    private int mMaxLines = 0;
    public boolean mIsTextMode = true;
    private List<String> mPageText = null;
    private final static int DEFAULT_TITLE_SIZE_ADD = 6;
    /**
     * 文本中有 \r\n  则 绘制时候是会自动换行
     */
    private String mDefaultSpliteTag = "\r\n";
    private String mSpliteTag = "\",\"";


    public ReaderText getReaderText() {
        return mReaderText;
    }

    public TextView getReaderTitleText() {
        return mReaderTitleTv;
    }

    private ReaderText mReaderText = null;
    private View mReaderLayoutView = null;
    private View mReaderPageLayout = null;
    private View mReaderTitleLayout = null;
    private TextView mReaderTitleTv = null;
    /**
     * 翻页手势和动画处理
     */
    //手势效果  统一由父容器下发控制子view移动和动画
    private ScrollerCompat mScroller;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    //计算滑动速度，决定惯性滑动
    private VelocityTracker mVelocityTracker;
    private int mLastTouchX = -1;
    private int mLastTouchY = -1;
    private int mScrollPointerId = INVALID_POINTER;
    private boolean mEnableHorizonScroll = true;
    private boolean mEnableVerticalScroll = false;
    private boolean mIsIntercept = false;
    private boolean mIsTextMoveAction = false;
    //是否是
    private int mIsDeltaActive = 0;
    private final int DELTA_DEFAULT = 0;
    private final int DELTA_ACTIVE = 1;
    private final int DELTA_NEGATIVE = -1;

    private final int[] mNestedOffsets = new int[2];
    private boolean mIsFirstScroll = true;
    private ReaderPageView mCurScrollView = null;
    private final int MAX_PAGE_VELOCITY = 50;
    private final int MIN_PAGE_VELOCITY = -50;
    public final int GO_NEXT_PAGE = 1;
    public final int GO_PRE_PAGE = 2;
    public final int NO_GO_PAGE = 0;
    private boolean mIsPageViewMove = false;
    private MotionEvent mLastDownMotionEvent = null;
    private boolean mIsAnimated = false;
    private int mTextColor = Color.BLACK;
    private int mBgColor = Color.WHITE;
    private String mReaderMoveMode = ReaderDiv.READER_PAGE_MODE_HORIZON;
    public static final int DEFAULT_CHECK_DISTANCE = 120;
    private boolean mIsShowLog;

    public void setPageIndex(int mPageIndex) {
        this.mPageIndex = mPageIndex;
    }

    public int getPageIndex() {
        return mPageIndex;
    }

    private int mPageIndex = -1;

    public boolean isNextTitle() {
        return mNextTitle;
    }

    public void setNextTitle(boolean mNextTitle) {
        this.mNextTitle = mNextTitle;
    }

    private boolean mNextTitle;

    public int getTotalPageIndex() {
        return mTotalPageIndex;
    }

    public void setTotalPageIndex(int totalPageIndex) {
        this.mTotalPageIndex = totalPageIndex;
    }

    private int mTotalPageIndex = -1;

    public boolean isTextMode() {
        return mIsTextMode;
    }

    public void setTextMode(boolean mIsTextMode) {
        this.mIsTextMode = mIsTextMode;
        if (mIsTextMode) {
            removeAllViews();
            if (null == mReaderLayoutView || null == mReaderText || null == mReaderTitleTv) {
                mReaderLayoutView = initReaderPageLayout();
                mReaderPageLayout = mReaderLayoutView.findViewById(R.id.reader_layout_view);
                mReaderTitleLayout = mReaderLayoutView.findViewById(R.id.reader_title_layout);
                mReaderTitleTv = mReaderLayoutView.findViewById(R.id.reader_title_tv);
                mReaderText = mReaderLayoutView.findViewById(R.id.reader_content_tv);
            }
            mReaderLayoutView.setBackgroundColor(mBgColor);
            if (null != mReaderTitleLayout) {
                mReaderTitleLayout.setBackgroundColor(mBgColor);
            }
            mReaderText.initReaderText(mTextColor, mBgColor);
            mReaderText.setPageCallback(mPageCallback);
            ViewGroup.LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
            YogaNode yogaNode = getYogaNode();
            if (null != yogaNode) {
                yogaNode.setFlexDirection(YogaFlexDirection.ROW);
                yogaNode.setJustifyContent(YogaJustify.FLEX_START);
                yogaNode.setAlignItems(YogaAlign.STRETCH);
            } else {
                Log.w(TAG, READER_LOG_TAG + " setTextMode yogaNode is null.");
            }
            addView(mReaderLayoutView, params);
            if (null != mReaderLayoutView && mReaderLayoutView.getVisibility() == View.GONE) {
                mReaderLayoutView.setVisibility(View.VISIBLE);
            }
            if (null != mReaderText && mReaderText.getVisibility() == View.GONE) {
                mReaderText.setVisibility(View.VISIBLE);
            }
        } else {
            if (null != mReaderLayoutView && mReaderLayoutView.getVisibility() == View.VISIBLE) {
                mReaderLayoutView.setVisibility(View.GONE);
            }
            if (null != mReaderText && mReaderText.getVisibility() == View.VISIBLE) {
                mReaderText.setVisibility(View.GONE);
                mReaderText.setPageCallback(null);
            }
        }
    }

    private View initReaderPageLayout() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.reader_page_container, null);
        return view;
    }

    private void initChapterTitle(TextView textView) {
        if (null == textView || null == mPageCallback) {
            Log.w(TAG, READER_LOG_TAG + " initChapterTitle textView or mPageCallback is null.");
            return;
        }
        String pageTitle = mPageCallback.getChapterTitle();
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFontSize + DEFAULT_TITLE_SIZE_ADD);
        textView.setText(pageTitle);
    }

    public void setBgColor(int color) {
        mBgColor = color;
        setBackgroundColor(mBgColor);
        if (mIsTextMode) {
            if (null != mReaderLayoutView) {
                mReaderLayoutView.setBackgroundColor(mBgColor);
            }
            if (null != mReaderTitleLayout && mReaderTitleLayout.getVisibility() == VISIBLE) {
                mReaderTitleLayout.setBackgroundColor(mBgColor);
            }
            if (null != mReaderText) {
                mReaderText.setBgColor(mBgColor);
            }

        } else {
            Log.w(TAG, "setBgColor mReaderText : " + mReaderText
                    + " mReaderText is null : " + (null == mReaderText));
        }
    }

    public void setPageCallback(PageCallback pageCallback) {
        this.mPageCallback = pageCallback;
        if (null != mReaderText) {
            mReaderText.setPageCallback(pageCallback);
        } else {
            Log.w(TAG, "setPageCallback mReaderText is null.");
        }
    }

    public ReaderPageView(@NonNull Context context, int bgColor, String readerMoveMode) {
        super(context);
        mBgColor = bgColor;
        mReaderMoveMode = readerMoveMode;
        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            mEnableVerticalScroll = true;
            mEnableHorizonScroll = false;
        } else {
            mEnableVerticalScroll = false;
            mEnableHorizonScroll = true;
        }
        initView();
    }

    public void refreshReaderMode(String readerMode) {
        mReaderMoveMode = readerMode;
        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            mEnableVerticalScroll = true;
            mEnableHorizonScroll = false;
        } else {
            mEnableVerticalScroll = false;
            mEnableHorizonScroll = true;
        }
    }

    public ReaderPageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context);
        initView();
    }

    public ReaderPageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context);
        initView();
    }

    public void setFontSize(int size) {
        mFontSize = size;
        if (null != mReaderText) {
            mReaderText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFontSize);
        }
    }

    public void setFontColor(int color) {
        mTextColor = color;
        if (null != mReaderText) {
            mReaderText.setTextColor(mTextColor);
        }
    }

    public void setLineHeight(int lineHeight) {
        mLineHeight = lineHeight;
        Log.w(TAG, READER_LOG_TAG + "setLineHeight mLineHeight : " + mLineHeight);
        if (null != mReaderTitleLayout) {
            if (null != mPageCallback && ((mPageIndex == 0 && !mPageCallback.isReverseRead()) ||
                    (mPageCallback.isReverseRead() && mPageCallback.getCurPageSize() > 0
                            && mPageIndex == (mPageCallback.getCurPageSize() - 1))) && mIsTextMode) {
                mReaderTitleLayout.setBackgroundColor(mBgColor);
                mReaderTitleLayout.setVisibility(VISIBLE);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, mLineHeight * 2);
                mReaderTitleLayout.setLayoutParams(layoutParams);
                if (null != mReaderTitleTv) {
                    initChapterTitle(mReaderTitleTv);
                } else {
                    Log.w(TAG, READER_LOG_TAG + "setLineHeight mReaderTitleTv is null. ");
                }
            } else {
                if (mReaderTitleLayout.getVisibility() != View.GONE) {
                    mReaderTitleLayout.setVisibility(GONE);
                }
            }
        } else {
            Log.w(TAG, READER_LOG_TAG + "setLineHeight mReaderTitleLayout is null.");
        }
        if (null != mReaderText) {
            mReaderText.setLineHeight(lineHeight);
        }

    }

    public void setMaxPageLineCount(int lineCount) {
        if (null != mReaderText) {
            mReaderText.setMaxLines(lineCount);
        }
    }

    public void setReaderPageData(List<String> pageText) {
        if (null == pageText) {
            return;
        }
        mPageText = pageText;
        if (null != mPageText) {
            mReaderText.setReaderPageData(mPageText);
        }
    }

    public static final int SHADER_WIDTH = 40;
    private static final int DEFAULT_END_COLOR = Color.parseColor("#00000000");
    private static final int DEFAULT_START_COLOR = Color.parseColor("#4C000000");

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (getRight() == 0 || getRight() == getWidth()) {
            setBackgroundColor(mBgColor);
            if (null != mReaderLayoutView) {
                mReaderLayoutView.setBackgroundColor(mBgColor);
            }
            if (null != mReaderTitleLayout && mReaderLayoutView.getVisibility() == View.VISIBLE
                    && mIsTextMode) {
                mReaderTitleLayout.setBackgroundColor(mBgColor);
            }
            return;
        }
        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            Log.w(TAG, READER_LOG_TAG + "dispatchDraw mReaderMoveMode READER_PAGE_MODE_VERTICAL.");
            return;
        }
        setBackgroundColor(getResources().getColor(R.color.near_transparent_color));
        if (null != mReaderLayoutView) {
            mReaderLayoutView.setBackgroundColor(getResources().getColor(R.color.near_transparent_color));
        }
        canvas.save();
        RectF bgrectF = new RectF(0, 0, SHADER_WIDTH, getHeight());
        Paint bgpaint = new Paint();
        bgpaint.setColor(mBgColor);
        canvas.drawRect(bgrectF, bgpaint);
        canvas.restore();

        canvas.save();
        RectF bgToprectF = new RectF(0, 0, getWidth(), SHADER_WIDTH);
        canvas.drawRect(bgToprectF, bgpaint);
        canvas.restore();

        canvas.save();
        RectF bgBottomrectF = new RectF(0, getHeight() - SHADER_WIDTH, getWidth(), getHeight());
        canvas.drawRect(bgBottomrectF, bgpaint);
        canvas.restore();

        canvas.save();
        RectF rectF = new RectF(getWidth() - SHADER_WIDTH, 0, getWidth(), getHeight());
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        LinearGradient linearGradient = new LinearGradient(getWidth() - SHADER_WIDTH, 0,
                getWidth(), 0, DEFAULT_START_COLOR, DEFAULT_END_COLOR, Shader.TileMode.CLAMP);
        paint.setShader(linearGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rectF, paint);
        canvas.restore();
    }

    public void initView() {
        setBackgroundColor(mBgColor);
        removeAllViews();
        if (null == mReaderLayoutView || null == mReaderText || null == mReaderTitleTv) {
            mReaderLayoutView = initReaderPageLayout();
            mReaderPageLayout = mReaderLayoutView.findViewById(R.id.reader_layout_view);
            mReaderTitleLayout = mReaderLayoutView.findViewById(R.id.reader_title_layout);
            mReaderTitleTv = mReaderLayoutView.findViewById(R.id.reader_title_tv);
            mReaderText = mReaderLayoutView.findViewById(R.id.reader_content_tv);
        }
        mReaderLayoutView.setBackgroundColor(mBgColor);
        if (null != mReaderTitleLayout && mIsTextMode) {
            mReaderTitleLayout.setBackgroundColor(mBgColor);
        }
        mReaderText.setBgColor(mBgColor);
        mReaderText.initDefaultReaderText();
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        addView(mReaderLayoutView, params);
        initScroller();
        initVelocity();
    }

    public int getLineHeight() {
        return mLineHeight;
    }


    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText(mText);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLastDownMotionEvent = null;
    }

    public interface PageCallback {
        boolean onClickCallback(MotionEvent event);

        ReaderPageView getPrePageView();

        ReaderPageView getCurPageView();

        boolean isCurrentPage(ReaderPageView readerPageView);

        boolean isNextPage(ReaderPageView readerPageView);

        boolean isPrePage(ReaderPageView readerPageView);

        void goNextPage(boolean isForword);

        void setResetLayout(boolean isReset);

        //for vertical move
        int getCurrentIndex(ReaderPageView readerPageView, boolean isForward, String moveType, String lastAnimationMoveType);

        ReaderPageView getPrePageView(ReaderPageView readerPageView);

        ReaderPageView getNextPageView(ReaderPageView readerPageView);

        ReaderPageView getNextPageView(int curTouchIndex);

        ReaderPageView getPrePageView(int curTouchIndex);

        void preLoadNextPage(ReaderPageView curReaderPageView, boolean isForward, String moveType, String lastAnimationMoveType);

        void startVerticalAnimation(boolean isUpMove, Context context, final ReaderPageView view, int duration, int fromY, int toY, int pageChangeType);

        void setNoNeedPreloadPage(boolean noNeedPreloadPage, boolean inittextMode);

        String clearFlingAnimation();

        ReaderPageView getNextPageView();

        int MOVE_LEFT_TYPE = 1;
        int MOVE_RIGHT_TYPE = 2;
        String MOVE_UP_TYPE = "MOVE_UP_TYPE";
        String MOVE_DOWN_TYPE = "MOVE_DOWN_TYPE";

        //竖向移动 move过程的手势
        String TOUCH_MOVE_TYPE = "TOUCH_MOVE_TYPE";
        //竖向抬手后的fling
        String TOUCH_FLING_TYPE = "TOUCH_FLING_TYPE";

        boolean isCanMove(int moveType, boolean isTriggerCallback, int touchDistance);

        boolean isCanMoveVertical(String moveType, ReaderPageView readerPageView, boolean isTriggerCallback, int touchDistance);

        String getChapterTitle();

        String getNextPageTitle();

        boolean isReverseRead();

        int getCurPageSize();
    }

    private void initScroller() {
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public boolean isTextMoveAction() {
        return mIsTextMoveAction;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            return onInterceptVerticalTouchEvent(event);
        } else {
            return onInterceptHorizontalTouchEvent(event);
        }
    }

    public boolean onInterceptHorizontalTouchEvent(MotionEvent event) {
        if (null == mPageCallback || !mPageCallback.isCurrentPage(ReaderPageView.this)) {
            mIsIntercept = false;
            Log.d(TAG, READER_LOG_TAG + " isCurrentPage false , this : " + ReaderPageView.this
                    + " currentpage : " + (null != mPageCallback ? mPageCallback.getCurPageView() : " null mPageCallback")
                    + " action :  " + event.getAction()
                    + " mIsTextMode : " + mIsTextMode);
            return super.onInterceptTouchEvent(event);
        }
        if (mIsAnimated) {
            Log.d(TAG, READER_LOG_TAG + " event  this : " + ReaderPageView.this
                    + " mIsAnimated : " + mIsAnimated);
            mIsIntercept = false;
            return super.onInterceptTouchEvent(event);
        }
        int ea = event.getAction();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (ea) {
            case MotionEvent.ACTION_DOWN:
                mIsIntercept = false;
                mIsTextMoveAction = false;
                mLastTouchX = (int) event.getX();
                mLastTouchY = (int) event.getY();
                if (mNoNeedPreloadPage && null != mPageCallback) {
                    mPageCallback.setNoNeedPreloadPage(false, false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getX() - mLastTouchX;
                int dy = (int) event.getY() - mLastTouchY;
                if (!mIsIntercept) {
                    Log.d(TAG, READER_LOG_TAG + " onInterceptTouchEvent  : "
                            + "O donw 1 up 2 move   eventtype : " + event.getAction()
                            + " ReaderPageView.this : " + ReaderPageView.this
                            + " dx : " + dx
                    );
                    //左滑 dx < 0  右滑 dx > 0  (dy > 0 && dy > mTouchSlop)
                    if ((dx > 0 && dx > mTouchSlop) && (null != mPageCallback && mPageCallback.isCanMove(PageCallback.MOVE_RIGHT_TYPE, true, dx))) {
                        bringToFront();
                        mIsIntercept = true;
                    } else if ((dx < 0 && Math.abs(dx) > mTouchSlop) && (null != mPageCallback && mPageCallback.isCanMove(PageCallback.MOVE_LEFT_TYPE, true, dx))) {
                        mIsIntercept = true;
                    }
                    mIsTextMoveAction = (Math.abs(dx) > mTouchSlop) && !mIsIntercept && mIsTextMode;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsIntercept = false;
                if (mIsShowLog) {
                    Log.w(TAG, READER_LOG_TAG + "  onInterceptTouchEvent   ACTION_UP ACTION_CANCEL  mIsIntercept false");
                }
                mLastTouchX = 0;
                mLastTouchY = 0;
                break;
            default:
                mIsIntercept = false;
                if (mIsShowLog) {
                    Log.w(TAG, READER_LOG_TAG + "  onInterceptTouchEvent  default  mIsIntercept false");
                }
                mLastTouchX = 0;
                mLastTouchY = 0;
                break;

        }
        return mIsIntercept;
    }

    public boolean onInterceptVerticalTouchEvent(MotionEvent event) {
        int ea = event.getAction();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (ea) {
            case MotionEvent.ACTION_DOWN:
                if (mIsShowLog) {
                    Log.d(TAG, READER_LOG_TAG + "onInterceptVerticalTouchEvent  ACTION_DOWN   "
                    );
                }
                mIsIntercept = false;
                mIsTextMoveAction = false;
                mLastTouchX = (int) event.getX();
                mLastTouchY = (int) event.getY();
                if (null != mPageCallback) {
                    mIsVerticalAnimationType = mPageCallback.clearFlingAnimation();
                }
                if (mNoNeedPreloadPage && null != mPageCallback) {
                    mPageCallback.setNoNeedPreloadPage(false, false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int dy = (int) event.getY() - mLastTouchY;
                if (!mIsIntercept) {
                    if ((dy > 0 && dy > mTouchSlop) && (null != mPageCallback && mPageCallback.isCanMoveVertical(PageCallback.MOVE_DOWN_TYPE, this, true, dy))) {
                        mIsIntercept = true;
                    } else if ((dy < 0 && Math.abs(dy) > mTouchSlop) && (null != mPageCallback && mPageCallback.isCanMoveVertical(PageCallback.MOVE_UP_TYPE, this, true, dy))) {
                        mIsIntercept = true;
                    }
                    mIsTextMoveAction = (Math.abs(dy) > mTouchSlop) && !mIsIntercept && mIsTextMode;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsIntercept = false;
                if (mIsShowLog) {
                    Log.w(TAG, READER_LOG_TAG + "  onInterceptTouchEvent   ACTION_UP ACTION_CANCEL  mIsIntercept false");
                }
                mLastTouchX = 0;
                mLastTouchY = 0;
                break;
            default:
                mIsIntercept = false;
                if (mIsShowLog) {
                    Log.w(TAG, READER_LOG_TAG + "  onInterceptTouchEvent  default  mIsIntercept false");
                }
                mLastTouchX = 0;
                mLastTouchY = 0;
                break;

        }
        return mIsIntercept;
    }

    public boolean isPageViewMove() {
        return mIsPageViewMove;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            return resolveVerticalTouch(event);
        } else {
            return resolveHorizontalTouch(event);
        }
    }

    public boolean resolveHorizontalTouch(MotionEvent event) {
        if (null == mPageCallback || !mPageCallback.isCurrentPage(
                ReaderPageView.this)
        ) {
            Log.w(TAG, READER_LOG_TAG + "onTouchEvent event  this : " + ReaderPageView.this
                    + " action : " + event.getAction()
                    + " not current pageview : " + (mPageCallback.getCurPageView()));
            return super.onTouchEvent(event);
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLastDownMotionEvent = event;
        }
        int action = event.getAction();
        final boolean canScrollHorizontally = mEnableHorizonScroll;
        final boolean canScrollVertically = mEnableVerticalScroll;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        final MotionEvent vtev = MotionEvent.obtain(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);
        boolean eventAddedToVelocityTracker = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                mLastTouchX = (int) event.getX();
                mLastTouchY = (int) event.getY();
                if (mNoNeedPreloadPage && null != mPageCallback) {
                    mPageCallback.setNoNeedPreloadPage(false, false);
                }
                if (mIsShowLog) {
                    Log.w(TAG, READER_LOG_TAG + " onTouchEvent ACTION_DOWN mIsPageViewMove true   isDownConsume : " +
                            true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                mIsPageViewMove = true;
                float deltaX = event.getX() - mLastTouchX;
                float deltaY = event.getY() - mLastTouchY;
                boolean startScroll = false;
                boolean isCanMove = false;
                if (canScrollHorizontally && Math.abs(deltaX) > mTouchSlop) {
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                        isCanMove = (null != mPageCallback && mPageCallback.isCanMove(PageCallback.MOVE_RIGHT_TYPE, true, (int) deltaX));
                    } else {
                        isCanMove = (null != mPageCallback && mPageCallback.isCanMove(PageCallback.MOVE_LEFT_TYPE, true, (int) deltaX));
                        deltaX += mTouchSlop;
                    }
                    startScroll = true;
                }
                if (isCanMove && mNoNeedPreloadPage) {
                    isCanMove = false;
                    Log.w(TAG, READER_LOG_TAG + " onTouchEvent move isCanMove true mNoNeedPreloadPage true  ");
                }
                if (!isCanMove) {
                    mIsPageViewMove = false;
                    Log.w(TAG, READER_LOG_TAG + " onTouchEvent move isCanMove false.");
                    return false;
                }
                boolean isMoveEdge = false;
                if (deltaX > 0) {
                    if (null != mCurScrollView) {
                        if (mCurScrollView.getLeft() >= 0) {
                            isMoveEdge = true;
                        }
                    } else {
                        if (!mIsFirstScroll && getLeft() >= 0) {
                            isMoveEdge = true;
                            layout(0, 0, getWidth(), getHeight());
                        }
                    }
                }
                if (isMoveEdge) {
                    mIsPageViewMove = false;
                    Log.w(TAG, READER_LOG_TAG + " onTouchEvent move isMoveEdge true."
                            + " mCurScrollView left : " + (null != mCurScrollView ? mCurScrollView.getLeft() : " null mCurScrollView")
                            + " left : " + getLeft());
                    return false;
                }
                if (canScrollHorizontally) {
                    if (deltaX > 0) {
                        mIsDeltaActive = DELTA_ACTIVE;
                    } else {
                        mIsDeltaActive = DELTA_NEGATIVE;
                    }
                }
                if (startScroll) {
                    if (canScrollHorizontally && deltaX != 0) {
                        if (mIsFirstScroll) {
                            if (deltaX > 0) {
                                if (null != mPageCallback) {
                                    ReaderPageView readerPageView = mPageCallback.getPrePageView();
                                    if (null != readerPageView) {
                                        mIsFirstScroll = false;
                                        readerPageView.bringToFront();
                                        readerPageView.layout(-(getWidth() - (int) event.getX()), readerPageView.getTop(),
                                                -(getWidth() - (int) event.getX()) + readerPageView.getWidth(), readerPageView.getBottom()
                                        );
                                        mCurScrollView = readerPageView;
                                        mCurScrollView.invalidate();
                                    }
                                }
                            } else {
                                offsetLeftAndRight((int) deltaX);
                                invalidate();
                            }
                            mIsFirstScroll = false;
                        } else {
                            if (null != mCurScrollView) {
                                mCurScrollView.layout(-(getWidth() - (int) event.getX()), mCurScrollView.getTop(),
                                        (int) event.getX(), mCurScrollView.getBottom()
                                );
                                mCurScrollView.invalidate();
                            } else {
                                offsetLeftAndRight((int) deltaX);
                                invalidate();
                            }
                        }
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                float xvel = canScrollHorizontally ? -mVelocityTracker.getXVelocity() : 0;
                float yvel = canScrollVertically ? -mVelocityTracker.getYVelocity() : 0;
                float xvelOld = canScrollHorizontally ?
                        -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                final float yvelOld = canScrollVertically ?
                        -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
                boolean isReAdjustFold = false;
                //适配计算速度值有问题
                if (canScrollHorizontally) {
                    if (mIsDeltaActive != DELTA_DEFAULT) {
                        if (mIsDeltaActive == DELTA_ACTIVE && xvel < 0) {
                            isReAdjustFold = true;
                            xvel = -xvel;
                        } else if ((mIsDeltaActive == DELTA_NEGATIVE && xvel > 0)) {
                            isReAdjustFold = true;
                            xvel = -xvel;
                        }
                    }
                }
                if (isReAdjustFold) {
                    if (mIsShowLog) {
                        Log.w(TAG, READER_LOG_TAG + " onTouchEvent isReAdjustFold ACTION_UP "
                                + " xvel : " + xvel
                                + " yvel : " + yvel
                                + " xvelOld : " + xvelOld
                                + " yvelOld : " + yvelOld
                                + " mIsDeltaActive : " + mIsDeltaActive

                        );
                    }

                }
                mIsDeltaActive = 0;
                //当前坐标位置  加速度
                //松手瞬间改变视图容器位置，否则无法显示所有动画
                int left = -1;
                if (null != mCurScrollView) {
                    left = mCurScrollView.getLeft();
                }
                if (!mNoNeedPreloadPage) {
                    if (null != mCurScrollView) {
                        if (xvel < MIN_PAGE_VELOCITY) {
                            int width = getWidth();
                            int curLeft = mCurScrollView.getLeft();
                            float curX = mCurScrollView.getX();
                            float curTranslationX = mCurScrollView.getTranslationX();
                            Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 1 NO_GO_PAGE "
                                    + " width : " + width
                                    + " curLeft : " + curLeft
                                    + " curTranslationX : " + curTranslationX
                                    + " curX : " + curX);
                            initValueAnimation(getContext(), mCurScrollView, 0, -left - getWidth(), NO_GO_PAGE);
                        } else if (xvel > MAX_PAGE_VELOCITY) {
                            int width = getWidth();
                            int curLeft = mCurScrollView.getLeft();
                            float curX = mCurScrollView.getX();
                            float curTranslationX = mCurScrollView.getTranslationX();
                            Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 2 GO_PRE_PAGE  "
                                    + " width : " + width
                                    + " curLeft : " + curLeft
                                    + " curTranslationX : " + curTranslationX
                                    + " curX : " + curX);
                            initValueAnimation(getContext(), mCurScrollView, 0, -left, GO_PRE_PAGE);
                        } else {
                            int curScrollRight = mCurScrollView.getRight();
                            if (curScrollRight > mCurScrollView.getWidth() / 2) {
                                //右侧动画  mCurScrollView.getLeft()
                                int width = getWidth();
                                int curLeft = mCurScrollView.getLeft();
                                float curX = mCurScrollView.getX();
                                float curTranslationX = mCurScrollView.getTranslationX();
                                Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 3 GO_PRE_PAGE "
                                        + " width : " + width
                                        + " curLeft : " + curLeft
                                        + " curTranslationX : " + curTranslationX
                                        + " curX : " + curX);
                                initValueAnimation(getContext(), mCurScrollView, 0, -left, GO_PRE_PAGE);
                            } else {
                                //左侧动画  ,mCurScrollView.getLeft()
                                int width = getWidth();
                                int curLeft = mCurScrollView.getLeft();
                                float curX = mCurScrollView.getX();
                                float curTranslationX = mCurScrollView.getTranslationX();
                                Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 4 NO_GO_PAGE "
                                        + " width : " + width
                                        + " curLeft : " + curLeft
                                        + " curTranslationX : " + curTranslationX
                                        + " curX : " + curX);
                                initValueAnimation(getContext(), mCurScrollView, 0, -left - getWidth(), NO_GO_PAGE);
                            }
                        }
                    } else {
                        if (xvel < MIN_PAGE_VELOCITY) {
                            //当前view的移动动画  左侧移动动画  ReaderPageView.this.getLeft()
                            int width = getWidth();
                            int curLeft = getLeft();
                            float curX = getX();
                            float curTranslationX = getTranslationX();
                            Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 5 GO_NEXT_PAGE "
                                    + " width : " + width
                                    + " curLeft : " + curLeft
                                    + " curTranslationX : " + curTranslationX
                                    + " curX : " + curX);
                            initValueAnimation(getContext(), ReaderPageView.this, 0, -getLeft() - getWidth(), GO_NEXT_PAGE);
                        } else if (xvel > MAX_PAGE_VELOCITY) {
                            int width = getWidth();
                            int curLeft = getLeft();
                            float curX = getX();
                            float curTranslationX = getTranslationX();
                            Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 6 NO_GO_PAGE "
                                    + " width : " + width
                                    + " curLeft : " + curLeft
                                    + " curTranslationX : " + curTranslationX
                                    + " curX : " + curX);
                            initValueAnimation(getContext(), ReaderPageView.this, 0, -getLeft(), NO_GO_PAGE);
                        } else {
                            //当前页的动画
                            int curScrollRight = ReaderPageView.this.getRight();
                            if (curScrollRight > ReaderPageView.this.getWidth() / 3) {
                                int width = getWidth();
                                int curLeft = getLeft();
                                float curX = getX();
                                float curTranslationX = getTranslationX();
                                Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 7 NO_GO_PAGE "
                                        + " width : " + width
                                        + " curLeft : " + curLeft
                                        + " curTranslationX : " + curTranslationX
                                        + " curX : " + curX);
                                initValueAnimation(getContext(), ReaderPageView.this, 0, -getLeft(), NO_GO_PAGE);
                            } else {
                                int width = getWidth();
                                int curLeft = getLeft();
                                float curX = getX();
                                float curTranslationX = getTranslationX();
                                Log.w(TAG, READER_LOG_TAG + " ACTION_UP_TR 8 GO_NEXT_PAGE "
                                        + " width : " + width
                                        + " curLeft : " + curLeft
                                        + " curTranslationX : " + curTranslationX
                                        + " curX : " + curX);
                                //左侧动画  ReaderPageView.this.getLeft()
                                initValueAnimation(getContext(), ReaderPageView.this, 0, -getWidth() + getLeft(), GO_NEXT_PAGE);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, READER_LOG_TAG + " resolveHorizontalTouch ACTION_UP ACTION_CANCEL no initValueAnimation ,"
                            + " mIsPageViewMove : " + mIsPageViewMove
                            + " mNoNeedPreloadPage : " + mNoNeedPreloadPage);
                }
                mIsFirstScroll = true;
                mCurScrollView = null;
                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                }
                break;
            default:
                break;
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return super.onTouchEvent(event);
    }


    private boolean mIsCanVerticalMove = false;

    public boolean isNoNeedPreloadPage() {
        return mNoNeedPreloadPage;
    }

    public void setNoNeedPreloadPage(boolean mNoNeedPreloadPage) {
        this.mNoNeedPreloadPage = mNoNeedPreloadPage;
    }

    private boolean mNoNeedPreloadPage = false;
    private String mIsVerticalAnimationType = "";

    private boolean resolveVerticalTouch(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLastDownMotionEvent = event;
        }
        int action = event.getAction();
        final boolean canScrollHorizontally = mEnableHorizonScroll;
        final boolean canScrollVertically = mEnableVerticalScroll;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        final int actionIndex = event.getActionIndex();
        final MotionEvent vtev = MotionEvent.obtain(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        //需要移动距离
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);
        boolean eventAddedToVelocityTracker = false;
        boolean isAnimation = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                mLastTouchX = (int) event.getX();
                mLastTouchY = (int) event.getY();
                if (null != mPageCallback) {
                    mIsVerticalAnimationType = mPageCallback.clearFlingAnimation();
                }
                if (mNoNeedPreloadPage && null != mPageCallback) {
                    mPageCallback.setNoNeedPreloadPage(false, false);
                }
                if (mIsShowLog) {
                    Log.w(TAG, READER_LOG_TAG + " onTouchEvent ACTION_DOWN mIsPageViewMove true   isDownConsume : " +
                            true);
                }
                return true;
//            case MotionEvent.ACTION_POINTER_DOWN:
//                mActivePointerId = event.getPointerId(actionIndex);
//                break;
            case MotionEvent.ACTION_MOVE:
                float curEventY = event.getY();
                if (curEventY < 0) {
                    if (mIsShowLog) {
                        Log.w(TAG, READER_LOG_TAG + " resolveVerticalTouch ACTION_MOVE curEventY is not valid.");
                    }
                    return false;
                }
                float deltaY = curEventY - mLastTouchY;
                boolean isCanMove = false;
                boolean startScroll = false;
                if (canScrollVertically && Math.abs(deltaY) > mTouchSlop) {
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                        isCanMove = (null != mPageCallback && mPageCallback.isCanMoveVertical(PageCallback.MOVE_DOWN_TYPE, this, true, (int) deltaY));
                    } else {
                        isCanMove = (null != mPageCallback && mPageCallback.isCanMoveVertical(PageCallback.MOVE_UP_TYPE, this, true, (int) deltaY));
                        deltaY += mTouchSlop;
                    }
                    startScroll = true;
                }
                if (isCanMove && mNoNeedPreloadPage) {
                    isCanMove = false;
                    if (mIsShowLog) {
                        Log.w(TAG, READER_LOG_TAG + " onTouchEvent move isCanMove true mNoNeedPreloadPage true  ");
                    }
                }
                mIsCanVerticalMove = isCanMove;
                if (!isCanMove) {
                    Log.w(TAG, READER_LOG_TAG + " onTouchEvent move isCanMove false, deltaY : " + Math.abs(deltaY)
                            + " mTouchSlop : " + mTouchSlop);
                    return false;
                }
                ViewParent viewParent = getParent();
                if (viewParent instanceof ReaderLayoutView && !mNoNeedPreloadPage) {
                    ((ReaderLayoutView) viewParent).setIsNeedVerLayout(false);
                }
                if (startScroll) {
                    if (canScrollVertically && deltaY != 0) {
                        mPageCallback.preLoadNextPage(this, deltaY < 0, TOUCH_MOVE_TYPE, mIsVerticalAnimationType);
                        if (deltaY > 0) {
                            mIsDeltaActive = DELTA_ACTIVE;
                        } else {
                            mIsDeltaActive = DELTA_NEGATIVE;
                        }
                        offsetTopAndBottom((int) deltaY);
                        //next  0  -height
                        ReaderPageView prePageView = mPageCallback.getPrePageView(this);
                        prePageView.layout(prePageView.getLeft(), -prePageView.getHeight() + getTop(),
                                prePageView.getRight(), getTop());
                        //mid  0 height
                        ReaderPageView nextPageView = mPageCallback.getNextPageView(this);
                        nextPageView.layout(nextPageView.getLeft(), getTop() + nextPageView.getHeight(),
                                nextPageView.getRight(), 2 * nextPageView.getHeight() + getTop());

                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                float xvel = mVelocityTracker.getXVelocity();
                float yvel = mVelocityTracker.getYVelocity();
                float xvelOld = canScrollHorizontally ?
                        -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                final float yvelOld = canScrollVertically ?
                        -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
                boolean isReAdjustFold = false;
                //适配计算速度值有问题
                if (canScrollVertically) {
                    if (mIsDeltaActive != DELTA_DEFAULT) {
                        if (mIsDeltaActive == DELTA_ACTIVE && yvel < 0) {
                            isReAdjustFold = true;
                            yvel = -yvel;
                        } else if ((mIsDeltaActive == DELTA_NEGATIVE && yvel > 0)) {
                            isReAdjustFold = true;
                            yvel = -yvel;
                        }
                    }
                }
                if (isReAdjustFold) {
                    if (mIsShowLog) {
                        Log.w(TAG, READER_LOG_TAG + " onTouchEvent isReAdjustFold ACTION_UP "
                                + " xvel : " + xvel
                                + " yvel : " + yvel
                                + " xvelOld : " + xvelOld
                                + " yvelOld : " + yvelOld
                                + " mIsDeltaActive : " + mIsDeltaActive
                        );
                    }
                }
                mIsDeltaActive = 0;
                if (Math.abs(yvel) <= 5) {
                    boolean isConsumeClick = preCheckClick(mLastTouchY, (int) event.getY());
                    Log.w(TAG, READER_LOG_TAG + " resolveVerticalTouch ACTION_UP ACTION_CANCEL preCheckClick true.");
                } else {
                    if (yvel != 0 && mIsCanVerticalMove && !mNoNeedPreloadPage) {
                        mPageCallback.clearFlingAnimation();
//                        if (mIsFoldableDevice) {
//                            yvel = yvel * 3;
//                        }
                        initFlingValue((int) yvel);
                        mPageCallback.startVerticalAnimation(yvel < 0, getContext(), this, mDuration, 0, mSplineDistance, -1);
                    } else {
                        Log.w(TAG, READER_LOG_TAG + " resolveVerticalTouch ACTION_UP ACTION_CANCEL no startVerticalAnimation ," +
                                "  yvel  " + yvel
                                + " mIsCanVerticalMove : " + mIsCanVerticalMove
                                + " mNoNeedPreloadPage : " + mNoNeedPreloadPage);
                    }
                }
                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                }
                break;
            default:
                break;
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return super.onTouchEvent(event);
    }

    private boolean preCheckClick(int startY, int endY) {
        boolean isConsume = false;
        if (Math.abs(endY - startY) <= mTouchSlop) {
            if (null != mPageCallback) {
                isConsume = mPageCallback.onClickCallback(mLastDownMotionEvent);
            }
            Log.w(TAG, READER_LOG_TAG + "preCheckClick   isConsume : " + isConsume);
        }
        return isConsume;
    }


    /**
     * 根据速度获取距离和时间  OverScroller fling 竖向平滑滑动
     */
    // Fling friction
    private float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)

    private float mPhysicalCoeff;
    // Animation duration, in milliseconds
    private int mDuration;
    // Distance to travel along spline animation
    private int mSplineDistance;
    // Current velocity
    private float mCurrVelocity;
    // Animation starting time, in system milliseconds
    private long mStartTime;

    private void initVelocity() {
        final float ppi = getContext().getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning
    }

    private void initFlingValue(int velocity) {
        mCurrVelocity = velocity;
        mDuration = 0;
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        double totalDistance = 0.0;
        if (velocity != 0) {
            mDuration = getSplineFlingDuration(velocity);
            totalDistance = getSplineFlingDistance(velocity);
        }
        if (totalDistance < 100) {
            totalDistance = 100;
            mDuration = 200;
            Log.w(TAG, READER_LOG_TAG + " initFlingValue adjust totalDistance.");
        }

        mSplineDistance = (int) (totalDistance * Math.signum(velocity));
    }

    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }

    /* Returns the duration, expressed in milliseconds */
    private int getSplineFlingDuration(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }

    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    //属性动画，平移
    public void initValueAnimation(Context context, View view, float fromX, float toX, int pageChangeType) {
        if (Math.abs(toX - fromX) <= mTouchSlop) {
            boolean isConsume = false;
            if (null != mPageCallback) {
                isConsume = mPageCallback.onClickCallback(mLastDownMotionEvent);
            }
            Log.w(TAG, READER_LOG_TAG + "initValueAnimation   isConsume : " + isConsume);
            if (isConsume) {
                mIsPageViewMove = false;
                return;
            }
        }
        if (mIsAnimated) {
            Log.w(TAG, READER_LOG_TAG + " initValueAnimation   fromX : " + fromX
                    + " toX : " + toX
                    + " mIsAnimated : " + mIsAnimated);
            return;
        }
        if (mIsShowLog) {
            Log.w(TAG, READER_LOG_TAG + " initValueAnimation   fromX : " + fromX
                    + " toX : " + toX);
        }
        if (null == view || null == context) {
            mIsPageViewMove = false;
            Log.e(TAG, READER_LOG_TAG + "initValueAnimation error view or context is null.");
            return;
        }
        mIsAnimated = true;
        ObjectAnimator translationX = ObjectAnimator.ofFloat(view, "translationX", fromX, toX);
        AnimatorSet animatorSet = new AnimatorSet();
        double totalTime = 300;
        double realTime = totalTime * Math.abs(toX - fromX) / (double) getWidth();
        animatorSet.setDuration((int) realTime);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (null != view) {
                    view.getY();
                }
                mIsAnimated = false;
                mIsPageViewMove = false;
                int width = view.getWidth();
                int curLeft = view.getLeft();
                float curTranslationX = view.getTranslationX();
                float curX = view.getX();
                Log.w(TAG, READER_LOG_TAG + " onAnimationEnd start   "
                        + " width : " + width
                        + " curLeft : " + curLeft
                        + " curTranslationX : " + curTranslationX
                        + " curX : " + curX
                        + " pageChangeType : " + pageChangeType);
                if (null != mPageCallback) {
                    mPageCallback.setResetLayout(true);
                    if (pageChangeType == GO_NEXT_PAGE) {
                        mPageCallback.goNextPage(true);
                    } else if (pageChangeType == GO_PRE_PAGE) {
                        mPageCallback.goNextPage(false);
                    } else {
                        //恢复移动状态前
                        if (null != mPageCallback && null != view) {
                            view.setTranslationX(0);
                            view.setTranslationY(0);
                            if (mPageCallback.isPrePage((ReaderPageView) view)) {
                                view.layout(-getWidth(), 0, 0, getHeight());
                            } else if (mPageCallback.isCurrentPage((ReaderPageView) view)) {
                                view.layout(0, 0, getWidth(), getHeight());
                            } else if (mPageCallback.isNextPage((ReaderPageView) view)) {
                                view.layout(0, 0, getWidth(), getHeight());
                            }
                        } else {
                            Log.w(TAG, READER_LOG_TAG + " onAnimationEnd view or mPageCallback is null.");
                        }
                    }
                    width = view.getWidth();
                    curLeft = view.getLeft();
                    curTranslationX = view.getTranslationX();
                    curX = view.getX();
                    Log.w(TAG, READER_LOG_TAG + " onAnimationEnd end   "
                            + " width : " + width
                            + " curLeft : " + curLeft
                            + " curTranslationX : " + curTranslationX
                            + " curX : " + curX);
                }
                if (view == mCurScrollView) {
                    mCurScrollView = null;
                }

                //重绘将阴影去掉
                if (null != view) {
                    view.invalidate();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.play(translationX);
        animatorSet.start();
    }
}
