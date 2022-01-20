/*
 * Copyright (c) 李玉江<1032694760@qq.com>
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.hapjs.widgets.view.picker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.Future;

public class WheelView extends View {

    private static final float ITEM_PADDING = 13f;
    private static final int CLICK = 1;
    private static final int FLING = 2;
    private static final int DRAG = 3;
    private static final int VELOCITY_FLING = 5;

    private static final int DIVIDER_COLOR = 0XFF4D4D4D;
    private static final int DIVIDER_ALPHA = 220;
    private static final float DIVIDER_THICK = 2f;

    private H mHandler;
    private GestureDetector mGestureDetector;
    private OnItemSelectListener mOnItemSelectListener;
    private Future mFuture;
    private Paint mPaintOuterText;
    private Paint mPaintCenterText;
    private Paint mPaintIndicator;
    private Paint mPaintShadow;
    private List<Item> mItems = new ArrayList<>();
    private int mMaxTextWidth;
    private int mMaxTextHeight;
    private int mTextSize;
    private float mItemHeight;
    private Typeface mTypeface = Typeface.DEFAULT;
    private int mTextColorOuter;
    private int mTextColorCenter;
    private DividerConfig mDividerConfig = new DividerConfig();
    private float mLineSpaceMultiplier;
    private int mTextPadding;
    private boolean mIsLoop = true;
    private float mFirstLineY;
    private float mSecondLineY;
    private float mTotalScrollY = 0;
    private int mInitPosition = -1;
    private int mSelectedIndex;
    private int mPreCurrentIndex;
    private int mVisibleItemCount;
    private int mMeasuredHeight;
    private int mMeasuredWidth;
    private int mRadius;
    private int mOffset = 0;
    private float mPreviousY = 0;
    private long mStartTime = 0;
    private int mWidthMeasureSpec;
    private int mGravity = Gravity.CENTER;
    private int mDrawCenterContentStart = 0;
    private int mDrawOutContentStart = 0;
    private float mCenterContentOffset = 0f;
    private boolean mUseWeight = true;
    private boolean mTextSizeAutoFit = true;

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        judgeLineSpace();
        initView(context);
    }

    private void initView(Context context) {
        float density = getResources().getDisplayMetrics().density;
        if (density < 1) {
            mCenterContentOffset = 2.4F;
        } else if (1 <= density && density < 2) {
            mCenterContentOffset = 3.6F;
        } else if (1 <= density && density < 2) {
            mCenterContentOffset = 4.5F;
        } else if (2 <= density && density < 3) {
            mCenterContentOffset = 6.0F;
        } else if (density >= 3) {
            mCenterContentOffset = density * 2.5F;
        }
        mHandler = new H(this);
        mGestureDetector =
                new GestureDetector(
                        context,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public final boolean onFling(
                                    MotionEvent e1, MotionEvent e2, float velocityX,
                                    float velocityY) {
                                cancelFuture();
                                InertiaTimerTask command =
                                        new InertiaTimerTask(WheelView.this, velocityY);
                                mFuture =
                                        Executors.scheduled()
                                                .scheduleWithFixedDelay(command, 0, VELOCITY_FLING,
                                                        TimeUnit.MILLISECONDS);
                                return true;
                            }
                        });
        mGestureDetector.setIsLongpressEnabled(false);
        initPaints();
    }

    private void initPaints() {
        mPaintOuterText = new Paint();
        mPaintOuterText.setAntiAlias(true);
        mPaintOuterText.setColor(mTextColorOuter);
        mPaintOuterText.setTypeface(mTypeface);
        mPaintOuterText.setTextSize(mTextSize);
        mPaintCenterText = new Paint();
        mPaintCenterText.setAntiAlias(true);
        mPaintCenterText.setColor(mTextColorCenter);
        mPaintCenterText.setTypeface(mTypeface);
        mPaintCenterText.setTextSize(mTextSize);
        mPaintIndicator = new Paint();
        mPaintIndicator.setAntiAlias(true);
        mPaintIndicator.setColor(mDividerConfig.mColor);
        mPaintIndicator.setStrokeWidth(mDividerConfig.mThick);
        mPaintIndicator.setAlpha(mDividerConfig.mAlpha);
        mPaintShadow = new Paint();
        mPaintShadow.setAntiAlias(true);
        mPaintShadow.setColor(mDividerConfig.mShadowColor);
        mPaintShadow.setAlpha(mDividerConfig.mShadowAlpha);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public final void setOffset(int offset) {
        if (offset < 1 || offset > 5) {
            throw new IllegalArgumentException("must between 1 and 5");
        }
        int count = offset * 2 + 1;
        if (offset % 2 == 0) {
            count += offset;
        } else {
            count += offset - 1;
        }
        if (count % 2 == 0) {
            throw new IllegalArgumentException("must be odd");
        }
        if (count != mVisibleItemCount) {
            mVisibleItemCount = count;
            requestLayout();
        }
    }

    public final int getSelectedIndex() {
        return mSelectedIndex;
    }

    public final void setSelectedIndex(int index) {
        if (mItems == null || mItems.isEmpty()) {
            return;
        }
        int size = mItems.size();
        if (index == 0 || (index > 0 && index < size && index != mSelectedIndex)) {
            mInitPosition = index;
            mTotalScrollY = 0;
            mOffset = 0;
            invalidate();
        }
    }

    public final void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        this.mOnItemSelectListener = onItemSelectListener;
    }

    public final void setDisplayedValues(List<?> items) {
        this.mItems.clear();
        if (items != null) {
            for (Object item : items) {
                if (item instanceof Item) {
                    this.mItems.add((Item) item);
                } else if (item instanceof CharSequence || item instanceof Number) {
                    this.mItems.add(new StringItem(item.toString()));
                } else {
                    throw new IllegalArgumentException("please implements " + Item.class.getName());
                }
            }
        }
        requestLayout();
    }

    public final void setDisplayedValues(List<?> items, int index) {
        setDisplayedValues(items);
        setSelectedIndex(index);
    }

    public void setTextColor(@ColorInt int colorNormal, @ColorInt int colorFocus) {
        this.mTextColorOuter = colorNormal;
        this.mTextColorCenter = colorFocus;
        mPaintOuterText.setColor(colorNormal);
        mPaintCenterText.setColor(colorFocus);
        invalidate();
    }

    public final void setTypeface(Typeface font) {
        mTypeface = font;
        mPaintOuterText.setTypeface(mTypeface);
        mPaintCenterText.setTypeface(mTypeface);
        requestLayout();
    }

    public final void setTextSize(float size) {
        if (size > 0.0F) {
            mTextSize = (int) (getContext().getResources().getDisplayMetrics().density * size);
            mPaintOuterText.setTextSize(mTextSize);
            mPaintCenterText.setTextSize(mTextSize);
            requestLayout();
        }
    }

    public void setDivider(DividerConfig config) {
        if (null == config) {
            mDividerConfig.setVisible(false);
            mDividerConfig.setShadowVisible(false);
            requestLayout();
            return;
        }
        this.mDividerConfig = config;
        mPaintIndicator.setColor(config.mColor);
        mPaintIndicator.setStrokeWidth(config.mThick);
        mPaintIndicator.setAlpha(config.mAlpha);
        mPaintShadow.setColor(config.mShadowColor);
        mPaintShadow.setAlpha(config.mShadowAlpha);
        requestLayout();
    }

    public final void setLineSpace(@FloatRange(from = 2, to = 4) float multiplier) {
        mLineSpaceMultiplier = multiplier;
        judgeLineSpace();
    }

    public void setTextPadding(int textPadding) {
        this.mTextPadding = toPx(getContext(), textPadding);
        requestLayout();
    }

    private void judgeLineSpace() {
        if (mLineSpaceMultiplier < 1.5f) {
            mLineSpaceMultiplier = 1.5f;
        } else if (mLineSpaceMultiplier > 4.0f) {
            mLineSpaceMultiplier = 4.0f;
        }
        requestLayout();
    }

    private void remeasure() {
        if (mItems == null) {
            return;
        }

        Rect rect = new Rect();
        for (int i = 0; i < mItems.size(); i++) {
            String s1 = contentText(mItems.get(i));
            mPaintCenterText.getTextBounds(s1, 0, s1.length(), rect);
            int textWidth = rect.width();
            if (textWidth > mMaxTextWidth) {
                mMaxTextWidth = textWidth;
            }
            mPaintCenterText.getTextBounds("汉字", 0, 2, rect);
            mMaxTextHeight = rect.height() + 2;
        }
        mItemHeight = mLineSpaceMultiplier * mMaxTextHeight;

        int halfCircumference = (int) (mItemHeight * (mVisibleItemCount - 1));
        mMeasuredHeight = (int) ((halfCircumference * 2) / Math.PI);
        mRadius = (int) (halfCircumference / Math.PI);
        ViewGroup.LayoutParams params = getLayoutParams();
        if (mUseWeight) {
            mMeasuredWidth = MeasureSpec.getSize(mWidthMeasureSpec);
        } else if (params != null && params.width > 0) {
            mMeasuredWidth = params.width;
        } else {
            mMeasuredWidth = mMaxTextWidth;
            if (mTextPadding < 0) {
                mTextPadding = toPx(getContext(), ITEM_PADDING);
            }
            mMeasuredWidth += mTextPadding * 2;
        }
        mFirstLineY = (mMeasuredHeight - mItemHeight) / 2.0F + mCenterContentOffset;
        mSecondLineY = (mMeasuredHeight + mItemHeight) / 2.0F + mCenterContentOffset;
        if (mInitPosition == -1) {
            if (mIsLoop && mItems.size() > 1) {
                mInitPosition = (mItems.size() + 1) / 2;
            } else {
                mInitPosition = 0;
            }
        }
        mPreCurrentIndex = mInitPosition;
        mSelectedIndex = 0;
    }

    private void smoothScroll(int actionType) {
        cancelFuture();
        if (actionType == FLING || actionType == DRAG) {
            mOffset = (int) ((mTotalScrollY % mItemHeight + mItemHeight) % mItemHeight);
            if ((float) mOffset > mItemHeight / 2.0F) {
                mOffset = (int) (mItemHeight - (float) mOffset);
            } else {
                mOffset = -mOffset;
            }
        }
        ScrollTimerTask command = new ScrollTimerTask(this, mOffset);
        mFuture =
                Executors.scheduled().scheduleWithFixedDelay(command, 0, 10, TimeUnit.MILLISECONDS);
    }

    private void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    private void selectedCallback() {
        if (mOnItemSelectListener == null) {
            return;
        }
        postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mOnItemSelectListener != null) {
                            mOnItemSelectListener.onSelected(WheelView.this, mSelectedIndex);
                        }
                    }
                },
                200L);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mItems == null || mItems.size() == 0) {
            return;
        }
        String[] visibleItemStrings = new String[mVisibleItemCount];
        int change = (int) (mTotalScrollY / mItemHeight);
        mPreCurrentIndex = mInitPosition + change % mItems.size();
        if (mIsLoop && mItems.size() > 1) {
            if (mPreCurrentIndex < 0) {
                mPreCurrentIndex = mItems.size() + mPreCurrentIndex;
            }
            if (mPreCurrentIndex > mItems.size() - 1) {
                mPreCurrentIndex = mPreCurrentIndex - mItems.size();
            }
        } else {
            if (mPreCurrentIndex < 0) {
                mPreCurrentIndex = 0;
            }
            if (mPreCurrentIndex > mItems.size() - 1) {
                mPreCurrentIndex = mItems.size() - 1;
            }
        }
        float itemHeightOffset = (mTotalScrollY % mItemHeight);
        int counter = 0;
        while (counter < mVisibleItemCount) {
            int index = mPreCurrentIndex - (mVisibleItemCount / 2 - counter);
            if (mIsLoop && mItems.size() > 1) {
                index = getLoopMappingIndex(index);
                visibleItemStrings[counter] = mItems.get(index).getName();
            } else if (index < 0) {
                visibleItemStrings[counter] = "";
            } else if (index > mItems.size() - 1) {
                visibleItemStrings[counter] = "";
            } else {
                visibleItemStrings[counter] = mItems.get(index).getName();
            }
            counter++;
        }
        if (mDividerConfig.mVisible) {
            float ratio = mDividerConfig.mRatio;
            canvas.drawLine(
                    mMeasuredWidth * ratio,
                    mFirstLineY,
                    mMeasuredWidth * (1 - ratio),
                    mFirstLineY,
                    mPaintIndicator);
            canvas.drawLine(
                    mMeasuredWidth * ratio,
                    mSecondLineY,
                    mMeasuredWidth * (1 - ratio),
                    mSecondLineY,
                    mPaintIndicator);
        }
        if (mDividerConfig.mShadowVisible) {
            mPaintShadow.setColor(mDividerConfig.mShadowColor);
            mPaintShadow.setAlpha(mDividerConfig.mShadowAlpha);
            canvas.drawRect(0.0F, mFirstLineY, mMeasuredWidth, mSecondLineY, mPaintShadow);
        }
        counter = 0;
        while (counter < mVisibleItemCount) {
            canvas.save();
            double radian = ((mItemHeight * counter - itemHeightOffset)) / mRadius;
            float angle = (float) (90D - (radian / Math.PI) * 180D);
            if (angle >= 90F || angle <= -90F) {
                canvas.restore();
            } else {
                String contentText;
                String tempStr = contentText(visibleItemStrings[counter]);
                contentText = tempStr;

                if (mTextSizeAutoFit) {
                    remeasureTextSize(contentText);
                    mGravity = Gravity.CENTER;
                } else {
                    mGravity = Gravity.LEFT;
                }
                measuredCenterContentStart(contentText);
                measuredOutContentStart(contentText);
                float translateY =
                        (float)
                                (mRadius - Math.cos(radian) * mRadius
                                        - (Math.sin(radian) * mMaxTextHeight) / 2D);
                canvas.translate(0.0F, translateY);
                canvas.scale(1.0F, (float) Math.sin(radian));
                if (translateY <= mFirstLineY && mMaxTextHeight + translateY >= mFirstLineY) {
                    canvas.save();
                    canvas.clipRect(0, 0, mMeasuredWidth, mFirstLineY - translateY);
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, mDrawOutContentStart, mMaxTextHeight,
                            mPaintOuterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, mFirstLineY - translateY, mMeasuredWidth,
                            (int) (mItemHeight));
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, mDrawCenterContentStart, mMaxTextHeight,
                            mPaintCenterText);
                    canvas.restore();
                } else if (translateY <= mSecondLineY
                        && mMaxTextHeight + translateY >= mSecondLineY) {
                    canvas.save();
                    canvas.clipRect(0, 0, mMeasuredWidth, mSecondLineY - translateY);
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, mDrawCenterContentStart, mMaxTextHeight,
                            mPaintCenterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, mSecondLineY - translateY, mMeasuredWidth,
                            (int) (mItemHeight));
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, mDrawOutContentStart, mMaxTextHeight,
                            mPaintOuterText);
                    canvas.restore();
                } else if (translateY >= mFirstLineY
                        && mMaxTextHeight + translateY <= mSecondLineY) {
                    canvas.clipRect(0, 0, mMeasuredWidth, mMaxTextHeight + mCenterContentOffset);
                    float y = mMaxTextHeight;
                    int i = 0;
                    for (Item item : mItems) {
                        if (item.getName().equals(tempStr)) {
                            mSelectedIndex = i;
                            break;
                        }
                        i++;
                    }
                    canvas.drawText(contentText, mDrawCenterContentStart, y, mPaintCenterText);
                } else {
                    canvas.save();
                    canvas.clipRect(0, 0, mMeasuredWidth, mItemHeight);
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, mDrawOutContentStart, mMaxTextHeight,
                            mPaintOuterText);
                    canvas.restore();
                }
                canvas.restore();
                mPaintCenterText.setTextSize(mTextSize);
            }
            counter++;
        }
    }

    private void remeasureTextSize(String contentText) {
        Rect rect = new Rect();
        mPaintCenterText.getTextBounds(contentText, 0, contentText.length(), rect);
        int width = rect.width();
        int size = mTextSize;
        while (width > mMeasuredWidth) {
            size--;
            mPaintCenterText.setTextSize(size);
            mPaintCenterText.getTextBounds(contentText, 0, contentText.length(), rect);
            width = rect.width();
        }
        mPaintOuterText.setTextSize(size);
    }

    private int getLoopMappingIndex(int index) {
        if (index < 0) {
            index = index + mItems.size();
            index = getLoopMappingIndex(index);
        } else if (index > mItems.size() - 1) {
            index = index - mItems.size();
            index = getLoopMappingIndex(index);
        }
        return index;
    }

    private String contentText(Object item) {
        if (item == null) {
            return "";
        } else if (item instanceof Item) {
            return ((Item) item).getName();
        } else if (item instanceof Integer) {
            return String.format(Locale.getDefault(), "%02d", (int) item);
        }
        return item.toString();
    }

    private void measuredCenterContentStart(String content) {
        Rect rect = new Rect();
        mPaintCenterText.getTextBounds(content, 0, content.length(), rect);
        switch (mGravity) {
            case Gravity.CENTER:
                mDrawCenterContentStart = (int) ((mMeasuredWidth - rect.width()) * 0.5);
                break;
            case Gravity.LEFT:
                mDrawCenterContentStart = toPx(getContext(), 8);
                break;
            case Gravity.RIGHT:
                mDrawCenterContentStart = mMeasuredWidth - rect.width();
                break;
            default:
                break;
        }
    }

    private void measuredOutContentStart(String content) {
        Rect rect = new Rect();
        mPaintOuterText.getTextBounds(content, 0, content.length(), rect);
        switch (mGravity) {
            case Gravity.CENTER:
                mDrawOutContentStart = (int) ((mMeasuredWidth - rect.width()) * 0.5);
                break;
            case Gravity.LEFT:
                mDrawOutContentStart = toPx(getContext(), 8);
                break;
            case Gravity.RIGHT:
                mDrawOutContentStart = mMeasuredWidth - rect.width();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mWidthMeasureSpec = widthMeasureSpec;
        remeasure();
        setMeasuredDimension(mMeasuredWidth, mMeasuredHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventConsumed = mGestureDetector.onTouchEvent(event);
        ViewParent parent = getParent();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartTime = System.currentTimeMillis();
                cancelFuture();
                mPreviousY = event.getRawY();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = mPreviousY - event.getRawY();
                mPreviousY = event.getRawY();
                mTotalScrollY = mTotalScrollY + dy;
                if (!mIsLoop || mItems.size() == 1) {
                    float top = -mInitPosition * mItemHeight;
                    float bottom = (mItems.size() - 1 - mInitPosition) * mItemHeight;
                    if (mTotalScrollY - mItemHeight * 0.25 < top) {
                        top = mTotalScrollY - dy;
                    } else if (mTotalScrollY + mItemHeight * 0.25 > bottom) {
                        bottom = mTotalScrollY - dy;
                    }
                    if (mTotalScrollY < top) {
                        mTotalScrollY = (int) top;
                    } else if (mTotalScrollY > bottom) {
                        mTotalScrollY = (int) bottom;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            default:
                if (!eventConsumed) {
                    float y = event.getY();
                    double l = Math.acos((mRadius - y) / mRadius) * mRadius;
                    int circlePosition = (int) ((l + mItemHeight / 2) / mItemHeight);
                    float extraOffset = (mTotalScrollY % mItemHeight + mItemHeight) % mItemHeight;
                    mOffset = (int) ((circlePosition - mVisibleItemCount / 2) * mItemHeight
                            - extraOffset);
                    if ((System.currentTimeMillis() - mStartTime) > 120) {
                        smoothScroll(DRAG);
                    } else {
                        smoothScroll(CLICK);
                    }
                }
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        invalidate();
        return true;
    }

    protected int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    private int toPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        int pxValue = (int) (dpValue * scale + 0.5f);
        return pxValue;
    }

    public interface Item extends java.io.Serializable {
        String getName();
    }

    public interface OnItemSelectListener {
        void onSelected(WheelView wheelView, int index);
    }

    private static class H extends Handler {
        static final int WHAT_INVALIDATE = 1000;
        static final int WHAT_SCROLL = 2000;
        static final int WHAT_ITEM_SELECTED = 3000;
        final WheelView mWheelView;

        H(WheelView view) {
            this.mWheelView = view;
        }

        @Override
        public final void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_INVALIDATE:
                    mWheelView.invalidate();
                    break;
                case WHAT_SCROLL:
                    mWheelView.smoothScroll(WheelView.FLING);
                    break;
                case WHAT_ITEM_SELECTED:
                    mWheelView.selectedCallback();
                    break;
                default:
                    break;
            }
        }
    }

    private static class ScrollTimerTask extends TimerTask {
        final WheelView mWheelView;
        int mRealTotalOffset = Integer.MAX_VALUE;
        int mRealOffset = 0;
        int mOffset;

        ScrollTimerTask(WheelView view, int offset) {
            this.mWheelView = view;
            this.mOffset = offset;
        }

        @Override
        public void run() {
            if (mRealTotalOffset == Integer.MAX_VALUE) {
                mRealTotalOffset = mOffset;
            }
            mRealOffset = (int) ((float) mRealTotalOffset * 0.1F);
            if (mRealOffset == 0) {
                if (mRealTotalOffset < 0) {
                    mRealOffset = -1;
                } else {
                    mRealOffset = 1;
                }
            }
            if (Math.abs(mRealTotalOffset) <= 1) {
                mWheelView.cancelFuture();
                mWheelView.mHandler.sendEmptyMessage(H.WHAT_ITEM_SELECTED);
            } else {
                mWheelView.mTotalScrollY = mWheelView.mTotalScrollY + mRealOffset;
                if (!mWheelView.mIsLoop || mWheelView.mItems.size() == 1) {
                    float itemHeight = mWheelView.mItemHeight;
                    float top = (float) (-mWheelView.mInitPosition) * itemHeight;
                    float bottom =
                            (float) (mWheelView.getCount() - 1 - mWheelView.mInitPosition)
                                    * itemHeight;
                    if (mWheelView.mTotalScrollY <= top || mWheelView.mTotalScrollY >= bottom) {
                        mWheelView.mTotalScrollY = mWheelView.mTotalScrollY - mRealOffset;
                        mWheelView.cancelFuture();
                        mWheelView.mHandler.sendEmptyMessage(H.WHAT_ITEM_SELECTED);
                        return;
                    }
                }
                mWheelView.mHandler.sendEmptyMessage(H.WHAT_INVALIDATE);
                mRealTotalOffset = mRealTotalOffset - mRealOffset;
            }
        }
    }

    private static class InertiaTimerTask extends TimerTask {
        final float mVelocityY;
        final WheelView mWheelView;
        float a = Integer.MAX_VALUE;

        InertiaTimerTask(WheelView view, float velocityY) {
            this.mWheelView = view;
            this.mVelocityY = velocityY;
        }

        @Override
        public final void run() {
            if (a == Integer.MAX_VALUE) {
                if (Math.abs(mVelocityY) > 2000F) {
                    if (mVelocityY > 0.0F) {
                        a = 2000F;
                    } else {
                        a = -2000F;
                    }
                } else {
                    a = mVelocityY;
                }
            }
            if (Math.abs(a) >= 0.0F && Math.abs(a) <= 20F) {
                mWheelView.cancelFuture();
                mWheelView.mHandler.sendEmptyMessage(H.WHAT_SCROLL);
                return;
            }
            int i = (int) ((a * 10F) / 1000F);
            mWheelView.mTotalScrollY = mWheelView.mTotalScrollY - i;
            if (!mWheelView.mIsLoop || mWheelView.mItems.size() == 1) {
                float itemHeight = mWheelView.mItemHeight;
                float top = (-mWheelView.mInitPosition) * itemHeight;
                float bottom = (mWheelView.getCount() - 1 - mWheelView.mInitPosition) * itemHeight;
                if (mWheelView.mTotalScrollY - itemHeight * 0.25 < top) {
                    top = mWheelView.mTotalScrollY + i;
                } else if (mWheelView.mTotalScrollY + itemHeight * 0.25 > bottom) {
                    bottom = mWheelView.mTotalScrollY + i;
                }
                if (mWheelView.mTotalScrollY <= top) {
                    a = 40F;
                    mWheelView.mTotalScrollY = (int) top;
                } else if (mWheelView.mTotalScrollY >= bottom) {
                    mWheelView.mTotalScrollY = (int) bottom;
                    a = -40F;
                }
            }
            if (a < 0.0F) {
                a = a + 20F;
            } else {
                a = a - 20F;
            }
            mWheelView.mHandler.sendEmptyMessage(H.WHAT_INVALIDATE);
        }
    }

    public static class DividerConfig {
        protected boolean mVisible = true;
        protected boolean mShadowVisible = false;
        protected int mColor = DIVIDER_COLOR;
        protected int mShadowColor = 0XFFBBBBBB;
        protected int mShadowAlpha = 100;
        protected int mAlpha = DIVIDER_ALPHA;
        protected float mRatio = 0f;
        protected float mThick = DIVIDER_THICK;

        public DividerConfig() {
            super();
        }

        public DividerConfig setVisible(boolean visible) {
            this.mVisible = visible;
            return this;
        }

        public DividerConfig setShadowVisible(boolean shadowVisible) {
            this.mShadowVisible = shadowVisible;
            if (shadowVisible && mColor == DIVIDER_COLOR) {
                mColor = mShadowColor;
                mAlpha = 255;
            }
            return this;
        }

        public DividerConfig setShadowColor(@ColorInt int color) {
            mShadowVisible = true;
            mShadowColor = color;
            return this;
        }

        public DividerConfig setColor(@ColorInt int color) {
            this.mColor = color;
            return this;
        }

        public DividerConfig setAlpha(@IntRange(from = 1, to = 255) int alpha) {
            this.mAlpha = alpha;
            return this;
        }

        public DividerConfig setRatio(@FloatRange(from = 0, to = 1) float ratio) {
            this.mRatio = ratio;
            return this;
        }
    }

    private static class StringItem implements Item {
        private String name;

        private StringItem(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
