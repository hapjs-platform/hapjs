/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.readerdiv;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;

import org.hapjs.component.Component;
import org.hapjs.component.view.PercentFrameLayout;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.R;
import org.hapjs.widgets.ReaderDiv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hapjs.widgets.ReaderDiv.READERDIV_LINE_SPACE_HIGH;
import static org.hapjs.widgets.ReaderDiv.READERDIV_LINE_SPACE_LOW;
import static org.hapjs.widgets.ReaderDiv.READER_PAGE_MODE_VERTICAL;
import static org.hapjs.widgets.ReaderDiv.READER_VIEW_MODE_AD;
import static org.hapjs.widgets.ReaderDiv.READER_VIEW_MODE_TEXT;
import static org.hapjs.widgets.view.readerdiv.ReaderPageView.DEFAULT_CHECK_DISTANCE;
import static org.hapjs.widgets.view.readerdiv.ReaderPageView.SHADER_WIDTH;

/**
 * 负责总体管理三个页面的隐藏显示重绘
 */
public class ReaderLayoutView extends PercentFrameLayout implements ReaderPageView.PageCallback {
    private String mCurChapter = "";
    private String mNextChapter = "";
    private String mPreChapter = "";
    //pre  mid  next
    //是否允许翻页和move上下章的由三章内容是否存在决定，每次翻章则更新章节指向
    private List<ReaderPageData> mPrePageDatas = new ArrayList<>();
    private List<ReaderPageData> mCurPageDatas = new ArrayList<>();
    private List<ReaderPageData> mNextPageDatas = new ArrayList<>();
    private int mFontSize = 69;
    private int mFontColor = Color.BLACK;
    private int mLineHeight = 124;
    private int mPageColor = Color.WHITE;
    private String mLineHeightType = READERDIV_LINE_SPACE_LOW;
    private int DEFAULT_FONT_SIZE = 69;
    private int DEFAULT_LINE_HEIGHT = 124;
    private float DEFAULT_LR_PADDING = 43.2f;
    //    private String mSplitStr = "\",\"";
    private String mSplitStr;
    private int mIndentSize = 2;
    private final String TAG = "ReaderLayoutView";

    private final static int LINE_HEIGHT_NORMAL = 50;
    private final static int LINE_HEIGHT_MIN = 30;
    private final static int LINE_HEIGHT_MAX = 70;
    private final static int DEFAULT_10_PADDING = 10;

    private YogaFlexDirection mFlexDirection = YogaFlexDirection.ROW;
    private YogaJustify mJustifyContent = YogaJustify.FLEX_START;
    private YogaAlign mAlignItems = YogaAlign.STRETCH;
    //只有小说
    private int mCurrentIndex = -1;
    private int mLastPageIndex = 0;

    public int getTotalCurrentIndex() {
        return mTotalCurrentIndex;
    }

    //小说和广告
    private int mTotalCurrentIndex = -1;
    private ReaderPageData mCurReaderPageData = null;
    //当前章节内容和标题
    private String mPageContent = "";
    private String mPageTitle = "";
    private String mPrePageTitle = "";
    private String mNextPageTitle = "";
    private ReaderPageCallback mReaderPageCallback = null;
    private boolean mIsDebug = false;
    /**
     * @param isForward 前进还是后退         ---需要还原后退过程中某个是广告还是文本
     */
    private HashMap<Integer, String> mReaderPageTypes = new HashMap<>();
    private int mCurMaxIndex = -1;
    private int mCurMinIndex = -1;
    public static final String HORIZON_READER_TAG = "  horizon_reader_tag  ";
    public static final String VERTICAL_READER_TAG = "  vertical_reader_tag  ";
    public static String READER_LOG_TAG = HORIZON_READER_TAG;
    private int mTextColor = Color.BLACK;
    private int mPageLineCount = 0;
    private int mLastLoadIndex = -1;
    /**
     * for component
     */
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;
    private IGesture mGesture;
    private boolean mIsAttachToWindow;
    private String mReaderMoveMode = ReaderDiv.READER_PAGE_MODE_HORIZON;
    private boolean mNoNeedPreloadPage;

    private static final int GO_NEXT_SUCCESS = 1;
    private static final int GO_NEXT_FAILURE = -1;

    public ReaderCallback getReaderCallback() {
        return mReaderCallback;
    }

    public void setReaderCallback(ReaderCallback readerCallback) {
        this.mReaderCallback = readerCallback;
    }

    public void setReaderStartCallback(ReaderChapterStartCallback readerChapterCallback) {
        this.mReaderStartCallback = readerChapterCallback;
    }

    public void setReaderEndCallback(ReaderChapterEndCallback readerChapterCallback) {
        this.mReaderEndCallback = readerChapterCallback;
    }

    public String getNextPageTitle() {
        return mNextPageTitle;
    }

    private ReaderCallback mReaderCallback;

    private ReaderChapterStartCallback mReaderStartCallback;
    private ReaderChapterEndCallback mReaderEndCallback;

    public ReaderLayoutView(Context context, int color, String spliteStr, String readerMoveMode) {
        super(context);
        mPageColor = color;
        mSplitStr = spliteStr;
        mReaderMoveMode = readerMoveMode;
        initView();
    }

    private ReaderPageView mPrePageView = null;
    private ReaderPageView mMidPageView = null;
    private ReaderPageView mNextPageView = null;

    public ReaderLayoutView(Context context, AttributeSet attrs) {
        super(context);
        initView();
    }


    public void setChildComponent() {
        mPrePageView.setComponent(mComponent);
        mMidPageView.setComponent(mComponent);
        mNextPageView.setComponent(mComponent);
    }

    public void initView() {
        if (READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            READER_LOG_TAG = VERTICAL_READER_TAG;
        } else {
            READER_LOG_TAG = HORIZON_READER_TAG;
        }
        removeAllViews();
        mPrePageView = new ReaderPageView(getContext(), mPageColor, mReaderMoveMode);
        mPrePageView.setId(R.id.reader_pre_page);
        mPrePageView.getYogaNode().setFlexDirection(mFlexDirection);
        mPrePageView.getYogaNode().setJustifyContent(mJustifyContent);
        mPrePageView.getYogaNode().setAlignItems(mAlignItems);
        mPrePageView.setLineHeight(mLineHeight);
        mPrePageView.setFontSize(mFontSize);
        mPrePageView.setFontColor(mFontColor);
        mPrePageView.setBgColor(mPageColor);
        mPrePageView.setMaxPageLineCount(mPageLineCount);
        LayoutParams frameLp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addView(mPrePageView, 0, frameLp);

        mMidPageView = new ReaderPageView(getContext(), mPageColor, mReaderMoveMode);
        mMidPageView.setId(R.id.reader_mid_page);
        mMidPageView.setLineHeight(mLineHeight);
        mMidPageView.setFontSize(mFontSize);
        mMidPageView.setFontColor(mFontColor);
        mMidPageView.setBgColor(mPageColor);
        mMidPageView.setMaxPageLineCount(mPageLineCount);
        mMidPageView.getYogaNode().setFlexDirection(mFlexDirection);
        mMidPageView.getYogaNode().setJustifyContent(mJustifyContent);
        mMidPageView.getYogaNode().setAlignItems(mAlignItems);
        LayoutParams frameLpmid = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addView(mMidPageView, 1, frameLpmid);

        mNextPageView = new ReaderPageView(getContext(), mPageColor, mReaderMoveMode);
        mNextPageView.setId(R.id.reader_next_page);
        mNextPageView.setLineHeight(mLineHeight);
        mNextPageView.setFontSize(mFontSize);
        mNextPageView.setFontColor(mFontColor);
        mNextPageView.setBgColor(mPageColor);
        mNextPageView.setMaxPageLineCount(mPageLineCount);
        mNextPageView.getYogaNode().setFlexDirection(mFlexDirection);
        mNextPageView.getYogaNode().setJustifyContent(mJustifyContent);
        mNextPageView.getYogaNode().setAlignItems(mAlignItems);
        LayoutParams frameLpnext = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addView(mNextPageView, 2, frameLpnext);

        mPrePageView.setPageCallback(this);
        mMidPageView.setPageCallback(this);
        mNextPageView.setPageCallback(this);
        mCurrentIndex = -1;
    }

    public int getLineHeight() {
        return mLineHeight;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachToWindow = true;
        reCalculatePage(null, true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachToWindow = false;
        mIsNeddVerLayout = true;
        mLastTriggerView = null;
        mLastLoadIndex = -1;
        mLastMoveType = null;
    }

    @Override
    public boolean isCurrentPage(ReaderPageView readerPageView) {
        ReaderPageView tmpReaderPageView = getCurReaderPageView();
        if (null != readerPageView && readerPageView == tmpReaderPageView) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isNextPage(ReaderPageView readerPageView) {
        ReaderPageView nextPageView = getPageViewByIndex((mCurrentIndex + 1) % 3);
        if (null != readerPageView && readerPageView == nextPageView) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPrePage(ReaderPageView readerPageView) {
        ReaderPageView prePageView = getPageViewByIndex(mCurrentIndex == 0 ? 2 : (mCurrentIndex - 1) % 3);
        if (null != readerPageView && readerPageView == prePageView) {
            return true;
        }
        return false;
    }


    @Override
    public void goNextPage(boolean isForword) {
        switchNextPage(true, isForword);
    }

    public int goNextVerticalPage(boolean isForword) {
        return switchNextPage(false, isForword);
    }

    public ReaderPageView getCurReaderPageView() {
        switch (mCurrentIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    public ReaderPageView getPageViewByIndex(int index) {
        switch (index) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    public int getPageViewIndex(View view) {
        int index = -1;
        if (view == mPrePageView) {
            index = 0;
        } else if (view == mMidPageView) {
            index = 1;
        } else if (view == mNextPageView) {
            index = 2;
        }
        return index;
    }

    public ReaderPageView getNextPageView() {
        int nextIndex = mCurrentIndex + 1;
        if (nextIndex > 2) {
            nextIndex = 0;
        }
        switch (nextIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    public ReaderPageView getNextPageView(int curTouchIndex) {
        int nextIndex = curTouchIndex + 1;
        if (nextIndex > 2) {
            nextIndex = 0;
        }
        switch (nextIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    private boolean isNextChapterValid(boolean isForward) {
        boolean nextChapterValid = false;
        if (isForward) {
            nextChapterValid = mNextPageDatas.size() > 0;
        } else {
            nextChapterValid = mPrePageDatas.size() > 0;
        }
        return nextChapterValid;
    }

    private boolean isCanMoveHorizonChapter(boolean isForward) {
        boolean isCanMove = false;
        return isCanMove;
    }

    private boolean isCanMoveNextChapter(boolean isForward) {
        boolean isCanMove = false;
        return isCanMove;
    }

    //逆序章节 后退边界
    private boolean mInPreChapter = false;
    //章节边界 前进边界
    private boolean mInForWardChapter = false;
    //逆序上章节阅读
    private boolean mIsReverRead = false;

    public int switchNextPage(boolean isHorizonMode, boolean isForward) {
        int successCode = GO_NEXT_FAILURE;
        //横向翻页下一章节预加载
        if (mIsReverRead) {
            isForward = !isForward;
        }
        if (!(mIsReverRead || mInPreChapter) && mTotalCurrentIndex <= 0 && !isForward) {
            Log.w(TAG, READER_LOG_TAG + "switchNextPage is not valid mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " isForward : " + isForward);
            return successCode;
        }
        if (mCurPageDatas.size() <= 0) {
            Log.e(TAG, READER_LOG_TAG + "switchNextPage mCurPageDatas size 0.");
            return successCode;
        }
        int pageIndex = 0;
        if (null == mCurReaderPageData) {
            pageIndex = 0;
        } else {
            pageIndex = mCurReaderPageData.pageIndex;
        }
        //末尾前一个是广告情况不需要拦截
        if (isForward && pageIndex == (mCurPageDatas.size() - 1)
                && !READER_VIEW_MODE_AD.equals(mReaderPageTypes.get(mTotalCurrentIndex))) {
            Log.w(TAG, READER_LOG_TAG + "switchNextPage is not valid pageIndex : " + pageIndex
                    + " mCurPageDatas.size() : " + mCurPageDatas.size());
            return successCode;
        }
        successCode = GO_NEXT_SUCCESS;
        int preTotalIndex = mTotalCurrentIndex;
        String textMode = "";
        int lastLoadIndex = mCurrentIndex;
        if (mCurrentIndex == -1) {
            lastLoadIndex = 0;
        }
        if (mIsReverRead) {
            if (isForward) {
                mCurrentIndex--;
                mTotalCurrentIndex++;
                if (mCurrentIndex < 0) {
                    mCurrentIndex = 2;
                }
                mCurMaxIndex = mTotalCurrentIndex;
            } else {
                mCurrentIndex++;
                mTotalCurrentIndex--;
                textMode = mReaderPageTypes.get(mTotalCurrentIndex);
                if (mCurrentIndex > 2) {
                    mCurrentIndex = 0;
                }
                mCurMinIndex = mTotalCurrentIndex;
            }
        } else {
            if (isForward) {
                mCurrentIndex++;
                mTotalCurrentIndex++;
                if (mCurrentIndex > 2) {
                    mCurrentIndex = 0;
                }
                mCurMaxIndex = mTotalCurrentIndex;
            } else {
                mCurrentIndex--;
                mTotalCurrentIndex--;
                textMode = mReaderPageTypes.get(mTotalCurrentIndex);
                if (mCurrentIndex < 0) {
                    mCurrentIndex = 2;
                }
                mCurMinIndex = mTotalCurrentIndex;
            }
        }
        mLastLoadIndex = mCurrentIndex;
        if (mIsReverRead) {
            mLastMoveType = isForward ? MOVE_DOWN_TYPE : MOVE_UP_TYPE;
        } else {
            mLastMoveType = isForward ? MOVE_UP_TYPE : MOVE_DOWN_TYPE;
        }
        switch (mCurrentIndex) {
            case 0:
                initCurrentPage(isHorizonMode, mInPreChapter, mCurrentIndex, preTotalIndex, isForward, textMode, mPrePageView, lastLoadIndex);
                break;
            case 1:
                initCurrentPage(isHorizonMode, mInPreChapter, mCurrentIndex, preTotalIndex, isForward, textMode, mMidPageView, lastLoadIndex);
                break;
            case 2:
                initCurrentPage(isHorizonMode, mInPreChapter, mCurrentIndex, preTotalIndex, isForward, textMode, mNextPageView, lastLoadIndex);
                break;
            default:
                break;
        }
        return successCode;
    }

    private int getPageIndex(boolean isForward, boolean isPreAd, boolean isNextAd, ReaderPageView readerPageView) {
        int pageIndex = -1;
        if (null == readerPageView) {
            Log.w(TAG, "getPageIndex readerPageView is null.");
        }
        if (null == mCurReaderPageData) {
            pageIndex = 0;
            mLastPageIndex = 0;
        } else {
            pageIndex = mCurReaderPageData.pageIndex;
        }
        if (readerPageView.mIsTextMode) {
            if (null != mCurReaderPageData) {
                if (isForward) {
                    if (isPreAd && mTotalCurrentIndex == (mCurMinIndex + 1) && mTotalCurrentIndex > 0) {
                        pageIndex = mCurReaderPageData.pageIndex;
                    } else {
                        if (mCurReaderPageData.pageIndex >= (mCurPageDatas.size() - 1)) {
                            pageIndex = mCurPageDatas.size() - 1;
                        } else {
                            pageIndex = mCurReaderPageData.pageIndex + 1;
                        }
                    }

                } else {
                    if (isNextAd && (mTotalCurrentIndex == mCurMaxIndex - 1)) {
                        pageIndex = mCurReaderPageData.pageIndex;
                    } else {
                        if (mCurReaderPageData.pageIndex <= 0) {
                            Log.w(TAG, READER_LOG_TAG + " getPageIndex mCurReaderPageData.pageIndex : " + mCurReaderPageData.pageIndex);
                            pageIndex = 0;
                        } else {
                            pageIndex = mCurReaderPageData.pageIndex - 1;
                        }
                    }
                }
            }
        } else {
            if (!isForward) {
                if (!isNextAd) {
                    if (mCurReaderPageData.pageIndex <= 0) {
                        Log.w(TAG, READER_LOG_TAG + " getPageIndex forward false  error,mCurReaderPageData.pageIndex : " + (null != mCurReaderPageData ? mCurReaderPageData.pageIndex : " null mCurReaderPageData"));
                        pageIndex = 0;
                    } else {
                        pageIndex = mCurReaderPageData.pageIndex - 1;
                    }
                }
            }

        }
        return pageIndex;
    }

    private void initCurrentPage(boolean isHorizonMode, boolean isPreChapter, int currentIndex, int preTotalIndex, boolean isForward, String textMode, ReaderPageView readerPageView, int lastLoadIndex) {
        if (null == readerPageView) {
            Log.w(TAG, READER_LOG_TAG + "initCurrentPage readerPageView is null.");
            return;
        }
        if (currentIndex < 0 || currentIndex > 2) {
            Log.w(TAG, READER_LOG_TAG + "initCurrentPage currentIndex is not valid  currentIndex : " + currentIndex);
            return;
        }
        if (!isForward) {
            if (!TextUtils.isEmpty(textMode)) {
                readerPageView.mIsTextMode = !textMode.equals(READER_VIEW_MODE_AD);
            } else {
                Log.w(TAG, READER_LOG_TAG + "initCurrentPage 0 error textMode : " + textMode);
            }
        } else {
            mReaderPageTypes.put(mTotalCurrentIndex, readerPageView.mIsTextMode ? READER_VIEW_MODE_TEXT : READER_VIEW_MODE_AD);
        }
        //默认第一个是text，不能是ad
        boolean isPreAd = mTotalCurrentIndex > 1 ? READER_VIEW_MODE_AD.equals(mReaderPageTypes.get(mTotalCurrentIndex - 1)) : false;
        boolean isNextAd = false;
        if (!isForward) {
            isNextAd = READER_VIEW_MODE_AD.equals(mReaderPageTypes.get(mTotalCurrentIndex + 1));
        }
        //广告pageIndex保留和上一个text一致
        int pageIndex = -1;
        if (isPreChapter) {
            pageIndex = mCurPageDatas.size() - 1;
        } else {
            pageIndex = getPageIndex(isForward, isPreAd, isNextAd, readerPageView);
        }
        if (isForward && pageIndex <= mLastPageIndex) {
            Log.w(TAG, READER_LOG_TAG + " initCurrentPage isForward true error pageIndex : " + pageIndex
                    + " mLastPageIndex : " + mLastPageIndex);
        }
        if (!isForward && pageIndex >= mLastPageIndex) {
            Log.w(TAG, READER_LOG_TAG + " initCurrentPage isForward false error pageIndex : " + pageIndex
                    + " mLastPageIndex : " + mLastPageIndex);
        }
        if (Math.abs(pageIndex - mLastPageIndex) > 1) {
            Log.w(TAG, READER_LOG_TAG + " mCurrentIndex : " + mCurrentIndex
                    + " isForward : " + isForward
                    + " textMode : " + (readerPageView.mIsTextMode ? READER_VIEW_MODE_TEXT : READER_VIEW_MODE_AD)
                    + "  pageIndex : " + pageIndex
                    + "  mLastPageIndex : " + mLastPageIndex
                    + " isPreAd : " + isPreAd
                    + " isNextAd : " + isNextAd
                    + " mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " mCurMaxIndex : " + mCurMaxIndex
                    + " mCurMinIndex : " + mCurMinIndex
            );
        }
        Log.w(TAG, READER_LOG_TAG + " pageindexlog initCurrentPage"
                + " mIsReverRead : " + mIsReverRead
                + " pageIndex ： " + pageIndex
                + " mCurrentIndex : " + mCurrentIndex
                + " mLastPageIndex  : " + mLastPageIndex
                + " mTotalCurrentIndex " + mTotalCurrentIndex
                + " textMode : " + (readerPageView.mIsTextMode ? READER_VIEW_MODE_TEXT : READER_VIEW_MODE_AD)
                + " isForward : " + isForward
                + " mInForWardChapter : " + mInForWardChapter
                + " mInPreChapter : " + mInPreChapter
                + " mIsNeddVerLayout : " + mIsNeddVerLayout
        );
        mLastPageIndex = pageIndex;
        //默认预加载下一页---下一页根据历史确定
        if (mIsReverRead) {
            if (currentIndex == 0) {
                prepareNextPage(isHorizonMode, isPreAd, !readerPageView.mIsTextMode, readerPageView, pageIndex, isForward, !isForward ? mMidPageView : mNextPageView);
            } else if (currentIndex == 1) {
                prepareNextPage(isHorizonMode, isPreAd, !readerPageView.mIsTextMode, readerPageView, pageIndex, isForward, !isForward ? mNextPageView : mPrePageView);
            } else if (currentIndex == 2) {
                prepareNextPage(isHorizonMode, isPreAd, !readerPageView.mIsTextMode, readerPageView, pageIndex, isForward, !isForward ? mPrePageView : mMidPageView);
            }
        } else {
            if (currentIndex == 0) {
                prepareNextPage(isHorizonMode, isPreAd, !readerPageView.mIsTextMode, readerPageView, pageIndex, isForward, isForward ? mMidPageView : mNextPageView);
            } else if (currentIndex == 1) {
                prepareNextPage(isHorizonMode, isPreAd, !readerPageView.mIsTextMode, readerPageView, pageIndex, isForward, isForward ? mNextPageView : mPrePageView);
            } else if (currentIndex == 2) {
                prepareNextPage(isHorizonMode, isPreAd, !readerPageView.mIsTextMode, readerPageView, pageIndex, isForward, isForward ? mPrePageView : mMidPageView);
            }
        }
        if (null != mReaderCallback) {
            //预加载广告
            mReaderCallback.onPageChange(isForward, preTotalIndex, mTotalCurrentIndex, pageIndex, mCurPageDatas.size(), mCurrentIndex, isPreAd);
        }
        if (readerPageView.mIsTextMode) {
            mCurReaderPageData = mCurPageDatas.get(pageIndex);
            Log.w(TAG, READER_LOG_TAG + " initCurrentPage textmode true pageIndex : " + pageIndex
                    + " readerPageView : " + readerPageView);
            readerPageView.setFontSize(mFontSize);
            readerPageView.setFontColor(mFontColor);
            readerPageView.setPageIndex(pageIndex);
            ReaderPageView curTouchReaderPageView = getPageViewByIndex(lastLoadIndex);
            curTouchReaderPageView.setTotalPageIndex(mTotalCurrentIndex);
            if (mPrePageView != curTouchReaderPageView) {
                mPrePageView.setTotalPageIndex(-1);
            }
            if (mMidPageView != curTouchReaderPageView) {
                mMidPageView.setTotalPageIndex(-1);
            }
            if (mNextPageView != curTouchReaderPageView) {
                mNextPageView.setTotalPageIndex(-1);
            }
            readerPageView.setBgColor(mPageColor);
            readerPageView.setLineHeight(mLineHeight);
            readerPageView.setMaxPageLineCount(mPageLineCount);
            readerPageView.setReaderPageData(mCurReaderPageData.pageLineDatas);
        } else {
            readerPageView.setPageIndex(pageIndex);
            readerPageView.setLineHeight(mLineHeight);
            ReaderPageView curTouchReaderPageView = getPageViewByIndex(lastLoadIndex);
            curTouchReaderPageView.setTotalPageIndex(mTotalCurrentIndex);
            if (mPrePageView != curTouchReaderPageView) {
                mPrePageView.setTotalPageIndex(-1);
            }
            if (mMidPageView != curTouchReaderPageView) {
                mMidPageView.setTotalPageIndex(-1);
            }
            if (mNextPageView != curTouchReaderPageView) {
                mNextPageView.setTotalPageIndex(-1);
            }
            Log.w(TAG, READER_LOG_TAG + " initCurrentPage textmode false pageIndex : " + pageIndex
                    + " readerPageView : " + readerPageView);
            if (null != mReaderCallback) {
                //当前页广告view加载
                ReaderPageView preReaderPageView = null;
                if (currentIndex == 0) {
                    preReaderPageView = mNextPageView;
                } else if (currentIndex == 1) {
                    preReaderPageView = mPrePageView;
                } else {
                    preReaderPageView = mMidPageView;
                }
                mReaderCallback.refreshPageView(preReaderPageView, readerPageView, readerPageView.mIsTextMode ? READER_VIEW_MODE_TEXT : READER_VIEW_MODE_AD);
            }
        }
        if (isHorizonMode) {
            if (currentIndex == 0) {
                mMidPageView.setVisibility(INVISIBLE);
                mMidPageView.bringToFront();
                mPrePageView.bringToFront();
                mPrePageView.setVisibility(VISIBLE);
                mMidPageView.setVisibility(VISIBLE);
                mNextPageView.setVisibility(VISIBLE);
            } else if (currentIndex == 1) {
                mNextPageView.setVisibility(INVISIBLE);
                mNextPageView.bringToFront();
                mMidPageView.bringToFront();
                mPrePageView.setVisibility(VISIBLE);
                mMidPageView.setVisibility(VISIBLE);
                mNextPageView.setVisibility(VISIBLE);
            } else {
                mPrePageView.setVisibility(INVISIBLE);
                mPrePageView.bringToFront();
                mNextPageView.bringToFront();
                mPrePageView.setVisibility(VISIBLE);
                mMidPageView.setVisibility(VISIBLE);
                mNextPageView.setVisibility(VISIBLE);
            }
        }

    }

    private void prepareNextPage(boolean isHorizonMode, boolean isPreAd, boolean isCurrentAd, ReaderPageView curPageView, int curPageIndex, boolean isForward, ReaderPageView nextPageView) {
        if (null == nextPageView) {
            Log.w(TAG, READER_LOG_TAG + "prepareNextPage nextPageView is null.");
        }
        ReaderPageData readerPageData = null;
        boolean isNextTitle = false;
        int nextPageIndex = -1;
        if (isForward && curPageIndex + 1 < (mCurPageDatas.size())) {
            readerPageData = mCurPageDatas.get(curPageIndex + 1);
            nextPageIndex = curPageIndex + 1;
        } else if (!isForward && curPageIndex > 0) {
            //当前是广告时候的下一个加载index确定
            if (isCurrentAd) {
                readerPageData = mCurPageDatas.get(curPageIndex);
                nextPageIndex = curPageIndex;
            } else {
                readerPageData = mCurPageDatas.get(curPageIndex - 1);
                nextPageIndex = curPageIndex - 1;
            }
        } else if ((!isForward && curPageIndex == 0 && null != curPageView && !curPageView.mIsTextMode)) {
            readerPageData = mCurPageDatas.get(0);
            nextPageIndex = 0;
        } else {
            Log.w(TAG, READER_LOG_TAG + " prepareNextPage curPageIndex : " + curPageIndex
                    + " isForward : " + isForward
                    + " allSize : " + mCurPageDatas.size()
                    + " isHorizonMode : " + isHorizonMode);
        }
        Log.w(TAG, READER_LOG_TAG + " pageindexlog prepareNextPage curPageIndex : " + curPageIndex
                + " nextPageIndex : " + nextPageIndex
                + " nextPageView Index : " + (null != nextPageView ? nextPageView.getPageIndex() : " null nextPageView")
                + " isPreAd : " + isPreAd
                + " isCurrentAd : " + isCurrentAd
                + " isForward : " + isForward
                + " allSize : " + mCurPageDatas.size());
        if (null != readerPageData) {
            //默认下一页是文本
            nextPageView.setPageIndex(nextPageIndex);
            nextPageView.setTextMode(true);
            nextPageView.setFontSize(mFontSize);
            nextPageView.setFontColor(mFontColor);
            nextPageView.setBgColor(mPageColor);
            nextPageView.setLineHeight(mLineHeight);
            nextPageView.setMaxPageLineCount(mPageLineCount);
            nextPageView.setReaderPageData(readerPageData.pageLineDatas);
        } else {
            Log.w(TAG, READER_LOG_TAG + " prepareNextPage readerPageData is null.");
        }
    }

    /**
     * 设置当前章节内容
     *
     * @param chapterContent
     */
    public void setChapterContent(String chapterContent) {

    }

    /**
     * 将章节内容分页
     */
    public void initPageDatas(String title, String content, ReaderPageCallback readerPageCallback, boolean isForwardPage) {
        if (null != readerPageCallback) {
            clearChapter();
        }
        mIsReverRead = !isForwardPage;
        mIsInLayoutView = true;
        Log.w(TAG, READER_LOG_TAG + " initPageDatas mIsReverRead : " + mIsReverRead);
        mPageContent = content;
        mPageTitle = title;
        if (READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            setIsNeedVerLayout(true);
        }
        setNoNeedPreloadPage(true, true);
        initChapterdata(content);
        reCalculatePage(readerPageCallback, false);
        post(new Runnable() {
            @Override
            public void run() {
                mIsInLayoutView = false;
                Log.w(TAG, READER_LOG_TAG + " initPageDatas mIsInLayoutView : " + mIsInLayoutView);
            }
        });
    }

    public void setNoNeedPreloadPage(boolean noNeedPreloadPage, boolean initTextMode) {
        mNoNeedPreloadPage = noNeedPreloadPage;
        final ReaderPageView tmpcurPageView = getPageViewByIndex(mCurrentIndex);
        final ReaderPageView tmpprePageView = getPrePageView();
        final ReaderPageView tmpnextPageView = getNextPageView();
        if (null != tmpcurPageView) {
            tmpcurPageView.setNoNeedPreloadPage(noNeedPreloadPage);
            if (initTextMode) {
                tmpcurPageView.setTextMode(true);
            }
        }
        if (null != tmpprePageView) {
            tmpprePageView.setNoNeedPreloadPage(noNeedPreloadPage);
            if (initTextMode) {
                tmpprePageView.setTextMode(true);
            }
        }
        if (null != tmpnextPageView) {
            tmpnextPageView.setNoNeedPreloadPage(noNeedPreloadPage);
            if (initTextMode) {
                tmpnextPageView.setTextMode(true);
            }
        }
        Log.w(TAG, READER_LOG_TAG + " setNoNeedPreloadPage noNeedPreloadPage : " + noNeedPreloadPage);
    }

    public void clearChapter() {
        mPrePageTitle = "";
        mNextPageTitle = "";
        mPreChapter = "";
        mNextChapter = "";
        mPrePageDatas.clear();
        mNextPageDatas.clear();
        mInPreChapter = false;
        mInForWardChapter = false;
        mIsReverRead = false;
    }

    public void preSetChapter(boolean isForward, String title, String content, ReaderPageCallback readerPageCallback) {
        if (!mIsAttachToWindow || TextUtils.isEmpty(content)) {
            Log.w(TAG, READER_LOG_TAG + " preSetChapter mIsAttachToWindow : " + mIsAttachToWindow
                    + " content empty : " + TextUtils.isEmpty(content)
                    + " isForward : " + isForward);
            return;
        }
        if (!isForward) {
            mPreChapter = content;
            mPrePageTitle = title;
        } else {
            mNextChapter = content;
            mNextPageTitle = title;
        }
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            Log.e(TAG, READER_LOG_TAG + " preSetChapter width : " + width
                    + " height : " + height);
            post(new Runnable() {
                @Override
                public void run() {
                    int realwidth = getWidth();
                    int realheight = getHeight();
                    if (!isForward) {
                        calculateChapter(mPrePageDatas, mPrePageTitle, mPreChapter, realwidth, realheight);
                    } else {
                        calculateChapter(mNextPageDatas, mNextPageTitle, mNextChapter, realwidth, realheight);
                    }

                }
            });
        } else {
            if (!isForward) {
                calculateChapter(mPrePageDatas, mPrePageTitle, mPreChapter, width, height);
            } else {
                calculateChapter(mNextPageDatas, mNextPageTitle, mNextChapter, width, height);
            }
        }
        if (null != readerPageCallback) {
            readerPageCallback.onPageContent(null);
        }
    }

    public void setFontSize(int fontSize) {
        mFontSize = fontSize;
        setLineHeight(false, mLineHeightType);
        ReaderText readerText = null;
        if (READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            setIsNeedVerLayout(true);
        }
        if (null != mPrePageView) {
            readerText = mPrePageView.getReaderText();
            if (null != readerText) {
                readerText.setTextSize(fontSize);
                readerText.setLineHeight(mLineHeight);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontSize mPrePageView readerText is null.");
            }
        }
        if (null != mMidPageView) {
            readerText = mMidPageView.getReaderText();
            if (null != readerText) {
                readerText.setTextSize(fontSize);
                readerText.setLineHeight(mLineHeight);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "  setFontSize mMidPageView readerText is null.");
            }
        }
        if (null != mNextPageView) {
            readerText = mNextPageView.getReaderText();
            if (null != readerText) {
                readerText.setTextSize(fontSize);
                readerText.setLineHeight(mLineHeight);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontSize mNextPageView readerText is null.");
            }
        }
    }

    public void setFontColor(int fontColor) {
        mFontColor = fontColor;
        ReaderText readerText = null;
        TextView titleTextView = null;
        if (null != mPrePageView) {
            readerText = mPrePageView.getReaderText();
            titleTextView = mPrePageView.getReaderTitleText();
            if (null != readerText) {
                readerText.setTextColor(fontColor);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontColor mPrePageView readerText is null.");
            }
            if (null != titleTextView) {
                titleTextView.setTextColor(fontColor);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontColor mPrePageView titleTextView is null.");
            }
        }
        if (null != mMidPageView) {
            readerText = mMidPageView.getReaderText();
            titleTextView = mMidPageView.getReaderTitleText();
            if (null != readerText) {
                readerText.setTextColor(fontColor);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontColor mMidPageView readerText is null.");
            }
            if (null != titleTextView) {
                titleTextView.setTextColor(fontColor);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontColor mMidPageView titleTextView is null.");
            }
        }
        if (null != mNextPageView) {
            readerText = mNextPageView.getReaderText();
            titleTextView = mNextPageView.getReaderTitleText();
            if (null != readerText) {
                readerText.setTextColor(fontColor);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontColor mNextPageView readerText is null.");
            }
            if (null != titleTextView) {
                titleTextView.setTextColor(fontColor);
            } else {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + " setFontColor mNextPageView titleTextView is null.");
            }
        }
    }

    public boolean reCalculatePage(ReaderPageCallback readerPageCallback, boolean isNeedResetLoadPage) {
        boolean isSuccess = false;
        if (!mIsAttachToWindow || TextUtils.isEmpty(mPageContent)) {
            Log.w(TAG, READER_LOG_TAG + " reCalculatePage mIsAttachToWindow : " + mIsAttachToWindow
                    + " mPageContent empty : " + TextUtils.isEmpty(mPageContent));
            return isSuccess;
        }
        if (isNeedResetLoadPage) {
            setNoNeedPreloadPage(false, true);
        }
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            Log.e(TAG, READER_LOG_TAG + " calculateChapter width : " + width
                    + " height : " + height);
            post(new Runnable() {
                @Override
                public void run() {
                    int realwidth = getWidth();
                    int realheight = getHeight();
                    initPageView(realwidth, realheight, readerPageCallback);
                }
            });
        } else {
            isSuccess = true;
            initPageView(width, height, readerPageCallback);
        }
        return isSuccess;
    }

    private void initPageView(int width, int height, ReaderPageCallback readerPageCallback) {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, READER_LOG_TAG + " initPageView width : " + width
                    + " height : " + height);
            return;
        }
        calculateChapter(mCurPageDatas, mPageTitle, mPageContent, width, height);
        calculateChapter(mPrePageDatas, mPrePageTitle, mPreChapter, width, height);
        calculateChapter(mNextPageDatas, mNextPageTitle, mNextChapter, width, height);
        reversePageData();
        mCurReaderPageData = null;
        mCurrentIndex = -1;
        Log.w(TAG, READER_LOG_TAG + "initPageView mNoNeedPreloadPage : " + mNoNeedPreloadPage
                + " mCurrentIndex : " + mCurrentIndex
                + " mReaderMoveMode : " + mReaderMoveMode);
        mTotalCurrentIndex = -1;
        mCurMaxIndex = -1;
        mCurMinIndex = -1;
        mReaderPageTypes.clear();
        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            switchNextPage(false, mIsReverRead ? false : true);
        } else {
            switchNextPage(true, mIsReverRead ? false : true);
        }
        if (null != readerPageCallback) {
            readerPageCallback.onPageContent(getPageContents(true, 0, 0, 0));
        }
    }

    private void reversePageData() {
        if (mIsReverRead && mCurPageDatas.size() > 0) {
            Collections.reverse(mCurPageDatas);
            ReaderPageData readerPageData = null;
            int allSize = mCurPageDatas.size();
            for (int i = 0; i < allSize; i++) {
                readerPageData = mCurPageDatas.get(i);
                readerPageData.pageIndex = i;
            }
        }
    }

    private boolean mIsResetLayout = false;

    @Override
    public void setResetLayout(boolean isReset) {
        mIsResetLayout = isReset;
    }

    @Override
    public int getCurrentIndex(ReaderPageView readerPageView, boolean isForward, String moveType, String lastAnimationMoveType) {
        int successCode = GO_NEXT_FAILURE;
        if (null == readerPageView) {
            Log.w(TAG, READER_LOG_TAG + " getCurrentIndex readerPageView is null.");
            return successCode;
        }
        if (TOUCH_MOVE_TYPE.equals(moveType)) {
            successCode = goNextVerticalPage(isForward);
        } else if (TOUCH_FLING_TYPE.equals(moveType)) {
            successCode = goNextVerticalPage(isForward);
        }
        return successCode;
    }

    public boolean isNoLoadAd() {
        return mIsNoLoadAd;
    }

    private boolean mIsNoLoadAd;
    //属性动画，平移
    private AnimatorSet mAnimatorSet = null;
    private boolean mIsAnimationRun = false;
    private boolean mIsInLayoutView = false;
    private int mLastAnimationValue = 0;
    private final int UP_MOVE_ORITATION = 1;
    private final int DOWN_MOVE_ORITATION = -1;
    private String mLastAnimationType = "";

    @Override
    public void startVerticalAnimation(final boolean isUpMove, Context context, final ReaderPageView view, int duration, int fromY, int distance, int pageChangeType) {

        if (null == view || duration <= 0 || distance == 0) {
            Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation view is null or duration : " + duration
                    + " distance : " + distance);
            return;
        }
        mLastAnimationType = isUpMove ? MOVE_UP_TYPE : MOVE_DOWN_TYPE;
        ReaderPageView readerPageView = view;
        if (null == readerPageView) {
            Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation readerPageView is null or duration : " + duration
                    + " distance : " + distance
                    + " mLastPageIndex : " + mLastPageIndex);
            return;
        }
        int realDistance = distance;
        //移动距离distance 校验计算
        if (mIsAnimationRun && null != mAnimatorSet) {
            Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation cancel animation mIsAnimationRun true.");
            mAnimatorSet.cancel();
        }
        final int tmpDuration = duration;
        final long tmpStartTime = System.currentTimeMillis();
        final int tmpRealDistance = realDistance;
        mIsAnimationRun = true;
        ValueAnimator animator = ValueAnimator.ofInt(fromY, tmpRealDistance);//创建一个值从0到400的动画
        final int startTop = view.getTop();
        final int startBottom = getTop();
        // 默认顺序  next  pre  mid
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int curValue = (int) animation.getAnimatedValue();
                boolean isCanMove = isCanMoveVertical(isUpMove ? ReaderPageView.PageCallback.MOVE_UP_TYPE : ReaderPageView.PageCallback.MOVE_DOWN_TYPE, view, false, 0);
                if (!isCanMove) {
                    if (mIsAnimationRun && null != mAnimatorSet) {
                        Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation onAnimationUpdate cancel animation mIsAnimationRun true.");
                        mAnimatorSet.cancel();
                    }
                    Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation onAnimationUpdate relayout isCanMove : " + isCanMove);
                    view.layout(view.getLeft(), 0, view.getRight(), view.getHeight());
                    //next  0  -height
                    ReaderPageView prePageView = getPrePageView(view);
                    prePageView.layout(prePageView.getLeft(), -prePageView.getHeight(),
                            prePageView.getRight(), 0);
                    //mid  0 height
                    ReaderPageView nextPageView = getNextPageView(view);
                    nextPageView.layout(nextPageView.getLeft(), nextPageView.getHeight(),
                            nextPageView.getRight(), 2 * nextPageView.getHeight());
                    return;
                }
                boolean isAbortAnimation = preCheckAnimation(isUpMove, isUpMove ? UP_MOVE_ORITATION : DOWN_MOVE_ORITATION, view);
                if (isAbortAnimation) {
                    Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation  onAnimationUpdate need cancel, " +
                            " animation : " + animation.getAnimatedValue()
                            + " startTop : " + startTop
                            + " getTop() : " + (null != view ? view.getTop() : " null view ")
                            + " curValue : " + curValue
                            + " mLastAnimationValue : " + mLastAnimationValue
                            + " tmpRealDistance : " + tmpRealDistance
                            + " isAbortAnimation : " + isAbortAnimation
                            + " view : " + view

                    );
                    startNextVerticalAnimation(isUpMove, isUpMove ? UP_MOVE_ORITATION : DOWN_MOVE_ORITATION, view, tmpStartTime, tmpDuration, tmpRealDistance, mLastAnimationValue);
                    return;
                }
                preLoadNextPage(view, isUpMove, TOUCH_FLING_TYPE, "");

                mLastAnimationValue = curValue;
                view.layout(view.getLeft(), startTop + (int) curValue, view.getRight(), startTop + (int) curValue + view.getHeight());
                //next  0  -height
                ReaderPageView prePageView = getPrePageView(view);
                prePageView.layout(prePageView.getLeft(), -prePageView.getHeight() + startTop + (int) curValue,
                        prePageView.getRight(), startTop + (int) curValue);
                //mid  0 height
                ReaderPageView nextPageView = getNextPageView(view);
                nextPageView.layout(nextPageView.getLeft(), startTop + (int) curValue + nextPageView.getHeight(),
                        nextPageView.getRight(), 2 * nextPageView.getHeight() + startTop + (int) curValue);
                Log.w(TAG, "startVerticalAnimation   onAnimationUpdate  nextPageView  : " + nextPageView +
                        "animation : " + animation.getAnimatedValue()
                        + " nextPageView getTop() : " + (nextPageView.getTop() - nextPageView.getHeight()
                        + " nextPageView.getTranslationY() : " + nextPageView.getTranslationY())
                );
            }
        });
        mAnimatorSet = new AnimatorSet();
        double totalTime = tmpDuration;
        mAnimatorSet.setDuration((int) totalTime);
        mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        mAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation onAnimationStart   " +
                        " isUpMove : " + isUpMove
                        + " duration : " + duration
                        + " distance : " + distance
                        + " startTop : " + startTop
                        + " view : " + view
                );
            }


            @Override
            public void onAnimationEnd(Animator animation) {
                mIsNoLoadAd = false;
                mIsAnimationRun = false;
                if (null != view) {
                    view.getY();
                }
                Log.w(TAG, READER_LOG_TAG + "startVerticalAnimation onAnimationEnd   " +
                        " isUpMove : " + isUpMove
                        + " duration : " + duration
                        + " distance : " + distance
                        + " startTop : " + startTop
                        + " endTop : " + (null != view ? view.getTop() : " null view")
                        + " remainDistance : " + (null != view ? (distance - (view.getTop() - startTop)) : " null view")
                        + " view : " + view
                );
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mAnimatorSet.play(animator);
        mAnimatorSet.start();
    }

    private ReaderPageView mLastTriggerView = null;

    /**
     * index刷新
     *
     * @param isUpMove
     * @param scrollOrientation
     * @param animationView
     * @return
     */
    private boolean preCheckAnimation(boolean isUpMove, int scrollOrientation, ReaderPageView animationView) {
        boolean isAbortAnimation = false;
        if (null == animationView) {
            Log.w(TAG, READER_LOG_TAG + "preCheckAnimation animationView is null.");
            return isAbortAnimation;
        }
        if (scrollOrientation == UP_MOVE_ORITATION && (animationView.getTop() <= (-animationView.getHeight() + DEFAULT_CHECK_DISTANCE))) {
            isAbortAnimation = true;
        } else if (scrollOrientation == DOWN_MOVE_ORITATION && (animationView.getTop() >= (animationView.getHeight() - DEFAULT_CHECK_DISTANCE))) {
            isAbortAnimation = true;
        }
        if (!isAbortAnimation) {
            return isAbortAnimation;
        }
        Log.w(TAG, READER_LOG_TAG + "preCheckAnimation for next animation mCurrentIndex : " + mCurrentIndex
                + " isUpMove : " + isUpMove
                + " animationView : " + animationView
        );
        return isAbortAnimation;
    }

    private boolean mIsLogLoadNext = false;
    private String mLastMoveType;

    public void preLoadNextPage(ReaderPageView curReaderPageView, boolean isForward, String touchmoveType, String lastAnimationMoveType) {
        if (null != curReaderPageView && curReaderPageView.isNoNeedPreloadPage()) {
            Log.w(TAG, READER_LOG_TAG + "preLoadNextPage pagecontent change no need PreloadPage.");
            return;
        }
        String moveType = isForward ? MOVE_UP_TYPE : MOVE_DOWN_TYPE;
        if (curReaderPageView.getTotalPageIndex() == -1 || (curReaderPageView.getTotalPageIndex() != -1 && !moveType.equals(mLastMoveType))) {
            boolean isGoNextPage = false;
            if (isForward && curReaderPageView.getTop() <= 0) {
                isGoNextPage = true;
            } else if (!isForward && curReaderPageView.getTop() >= 0) {
                isGoNextPage = true;
            }
            if (isGoNextPage) {
                mIsLogLoadNext = true;
                Log.w(TAG, READER_LOG_TAG + "preLoadNextPage   mCurrentIndex : " + mCurrentIndex
                        + " isForward : " + isForward
                        + " touchmoveType : " + touchmoveType
                        + " moveType : " + moveType
                        + " mLastMoveType : " + mLastMoveType
                        + " getTotalPageIndex : " + curReaderPageView.getTotalPageIndex()
                        + " curReaderPageView : " + getPageViewIndex(curReaderPageView)
                        + " isGoNextPage : " + isGoNextPage
                        + " curReaderPageView.getTop() : " + curReaderPageView.getTop()
                );
                int successCode = getCurrentIndex(curReaderPageView, isForward, touchmoveType, lastAnimationMoveType);
                if (successCode == GO_NEXT_FAILURE) {
                    Log.w(TAG, READER_LOG_TAG + " preLoadNextPage  GO_NEXT_FAILURE isForward : " + isForward
                            + " touchmoveType : " + touchmoveType
                            + " moveType : " + moveType
                            + " mLastMoveType : " + mLastMoveType
                            + " lastAnimationMoveType : " + lastAnimationMoveType
                            + " curReaderPageView : " + getCurPageViewIndex(curReaderPageView));
                }
            }
        } else {
            if (mIsLogLoadNext) {
                mIsLogLoadNext = false;
                Log.w(TAG, READER_LOG_TAG + "preLoadNextPage  else  mCurrentIndex : " + mCurrentIndex
                        + " isForward : " + isForward
                        + " touchmoveType : " + touchmoveType
                        + " moveType : " + moveType
                        + " mLastMoveType : " + mLastMoveType
                        + " getTotalPageIndex : " + curReaderPageView.getTotalPageIndex()
                        + " curReaderPageView : " + getPageViewIndex(curReaderPageView)
                        + " curReaderPageView.getTop() : " + curReaderPageView.getTop()
                );
            }
        }
    }

    private int getCurPageViewIndex(ReaderPageView readerPageView) {
        int recyclerIndex = -1;
        if (readerPageView == mPrePageView) {
            recyclerIndex = 0;
        } else if (readerPageView == mMidPageView) {
            recyclerIndex = 1;
        } else if (readerPageView == mNextPageView) {
            recyclerIndex = 2;
        } else {
            Log.w(TAG, READER_LOG_TAG + " getCurPageViewIndex readerPageView is not valid, readerPageView null : " + (readerPageView == null ? true : false));
        }
        return recyclerIndex;
    }

    private void startNextVerticalAnimation(boolean isUpMove, int scrollOrientation, ReaderPageView animationView, long startTime, int duration, int distance, int currentValue) {
        int userTime = (int) (System.currentTimeMillis() - startTime);
        int remainTime = duration - userTime;
        int remainDistance = distance - currentValue;
        Log.w(TAG, READER_LOG_TAG + "startNextVerticalAnimation for next animation mCurrentIndex : " + mCurrentIndex
                + " isUpMove : " + isUpMove
                + " startTime : " + startTime
                + " remainTime : " + remainTime
                + " duration : " + duration
                + " distance : " + distance
                + " userTime : " + userTime
                + " currentValue : " + currentValue
                + " animationView : " + animationView
        );
        //开始下个动画移动
        if (scrollOrientation == UP_MOVE_ORITATION) {
            ReaderPageView readerVPageView = getNextPageView(animationView);
            startVerticalAnimation(isUpMove, getContext(), readerVPageView, remainTime, 0, remainDistance, -1);
        } else if (scrollOrientation == DOWN_MOVE_ORITATION) {
            ReaderPageView readerVPageView = getPrePageView(animationView);
            startVerticalAnimation(isUpMove, getContext(), readerVPageView, remainTime, 0, remainDistance, -1);
        }
    }

    @Override
    public String clearFlingAnimation() {
        String lastAnimationType = "";
        if (mIsAnimationRun && null != mAnimatorSet) {
            lastAnimationType = mLastAnimationType;
            mAnimatorSet.cancel();
            Log.w(TAG, READER_LOG_TAG + "clearFlingAnimation mIsAnimationRun true.");
        }
        return lastAnimationType;
    }

    @Override
    public boolean isCanMove(int moveType, boolean triggerCallback, int touchDistance) {
        boolean isCanMove = false;
        if (moveType == MOVE_LEFT_TYPE) {
            if (mIsReverRead) {
                isCanMove = (mLastPageIndex > 0 && mTotalCurrentIndex > 0) ||
                        (mLastPageIndex == 0 && (null != getCurReaderPageView() && ((!getCurReaderPageView().isTextMode()) || (getCurReaderPageView().isTextMode() && getCurReaderPageView().getLeft() < 0))));
            } else {
                //前进
                isCanMove = (mLastPageIndex + 1 < mCurPageDatas.size());
            }
            if (triggerCallback && !isCanMove && null != mReaderEndCallback && touchDistance < 0 && Math.abs(touchDistance) > 50) {
                mReaderEndCallback.onChapterEnd(mReaderMoveMode);
            }
            if (!isCanMove) {
                isCanMove = isCanMoveHorizonChapter(true);
            }
        } else if (moveType == MOVE_RIGHT_TYPE) {
            //后退
            if (mIsReverRead) {
                isCanMove = (mLastPageIndex + 1 < mCurPageDatas.size());
            } else {
                isCanMove = (mLastPageIndex > 0 && mTotalCurrentIndex > 0) ||
                        (mLastPageIndex == 0 && (null != getCurReaderPageView() && ((!getCurReaderPageView().isTextMode()) || (getCurReaderPageView().isTextMode() && getCurReaderPageView().getLeft() < 0))));
            }
            if (triggerCallback && !isCanMove && null != mReaderStartCallback && touchDistance > 0 && Math.abs(touchDistance) > 50) {
                mReaderStartCallback.onChapterStart(mReaderMoveMode);
            }
            if (!isCanMove) {
                isCanMove = isCanMoveHorizonChapter(false);
            }
        }
        if (!isCanMove) {
            Log.w(TAG, READER_LOG_TAG + " isCanMove mLastPageIndex : " + mLastPageIndex
                    + " mCurPageDatas.size() : " + mCurPageDatas.size()
                    + " mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " textMode : " + (null != getCurReaderPageView() ? getCurReaderPageView().isTextMode() : " null getCurReaderPageView")
                    + " current left : " + (null != getCurReaderPageView() ? getCurReaderPageView().getLeft() : " null getCurReaderPageView")
                    + " isCanMove : " + isCanMove
                    + " mIsReverRead : " + mIsReverRead);
        }
        return isCanMove;
    }

    @Override
    public boolean isCanMoveVertical(String moveType, ReaderPageView readerPageView, boolean triggerCallback, int touchDistance) {
        //同一个屏幕触摸的可能是上下两部分问题
        boolean isCanMove = false;
        if (null == readerPageView) {
            Log.w(TAG, READER_LOG_TAG + " isCanMoveVertical readerPageView is null, mLastPageIndex : " + mLastPageIndex
                    + " mCurPageDatas.size() : " + mCurPageDatas.size()
                    + " mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " isCanMove : " + isCanMove);
            return isCanMove;
        }
        if (mIsInLayoutView) {
            Log.w(TAG, READER_LOG_TAG + " isCanMoveVertical  mIsInLayoutView false  mLastPageIndex : " + mLastPageIndex
                    + " mCurPageDatas.size() : " + mCurPageDatas.size()
                    + " mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " textMode : " + (null != readerPageView ? readerPageView.isTextMode() : " null readerPageView")
                    + " isCanMove : " + isCanMove
                    + "  readerPageView.getTop()  : " + readerPageView.getTop()
                    + " moveType   : " + moveType);
            return isCanMove;
        }
        int pageIndex = -1;
        if (MOVE_UP_TYPE.equals(moveType)) {
            if (mIsReverRead) {
                int top = readerPageView.getTop();
                pageIndex = readerPageView.getPageIndex();
                boolean isTextMode = readerPageView.isTextMode();
                if (isTextMode && pageIndex == 0 && top >= 0) {
                    isCanMove = false;
                } else {
                    isCanMove = true;
                }
                if (!isCanMove && triggerCallback && null != mReaderEndCallback && touchDistance < 0 && Math.abs(touchDistance) > 50) {
                    mReaderEndCallback.onChapterEnd(mReaderMoveMode);
                }
            } else {
                //以当前触摸的page为判断依据确定是否可以上下滚动
                int top = readerPageView.getTop();
                pageIndex = readerPageView.getPageIndex();
                boolean isTextMode = readerPageView.isTextMode();
                if (mCurPageDatas.size() > 1 && pageIndex == (mCurPageDatas.size() - 1) && top <= 0) {
                    //最后一个
                    isCanMove = false;
                    if (triggerCallback && null != mReaderEndCallback && touchDistance < 0 && Math.abs(touchDistance) > 50) {
                        mReaderEndCallback.onChapterEnd(mReaderMoveMode);
                    }
                } else if (mCurPageDatas.size() == 1) {
                    isCanMove = false;
                    if (triggerCallback && null != mReaderEndCallback && touchDistance < 0 && Math.abs(touchDistance) > 50) {
                        mReaderEndCallback.onChapterEnd(mReaderMoveMode);
                    }
                } else {
                    isCanMove = true;
                }
            }
            if (!isCanMove) {
                isCanMove = isCanMoveNextChapter(true);
            }
        } else if (MOVE_DOWN_TYPE.equals(moveType)) {
            int top = readerPageView.getTop();
            pageIndex = readerPageView.getPageIndex();
            boolean isTextMode = readerPageView.isTextMode();
            if (mIsReverRead) {
                //反向滑动场景
                if (mCurPageDatas.size() > 1 && pageIndex == (mCurPageDatas.size() - 1) && top <= 0) {
                    //最后一个
                    isCanMove = false;
                } else if (mCurPageDatas.size() == 1) {
                    isCanMove = false;
                } else {
                    isCanMove = true;
                }
            } else {
                if (isTextMode && pageIndex == 0 && top >= 0) {
                    isCanMove = false;
                } else {
                    isCanMove = true;
                }
            }
            if (!isCanMove && triggerCallback && null != mReaderStartCallback && touchDistance > 0 && Math.abs(touchDistance) > 50) {
                mReaderStartCallback.onChapterStart(mReaderMoveMode);
            }
            if (!isCanMove) {
                isCanMove = isCanMoveNextChapter(false);
            }
        }
        if (!isCanMove) {
            Log.w(TAG, READER_LOG_TAG + " isCanMoveVertical mLastPageIndex : " + mLastPageIndex
                    + " mCurPageDatas.size() : " + mCurPageDatas.size()
                    + " mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " textMode : " + (null != readerPageView ? readerPageView.isTextMode() : " null readerPageView")
                    + " isCanMove : " + isCanMove
                    + "  readerPageView.getTop()  : " + readerPageView.getTop()
                    + " move pageIndex : " + pageIndex
                    + " moveType  : " + moveType
                    + " mIsReverRead : " + mIsReverRead
                    + " mInForWardChapter : " + mInForWardChapter
                    + " mInPreChapter : " + mInPreChapter);
        }
        return isCanMove;
    }

    @Override
    public String getChapterTitle() {
        return mPageTitle;
    }

    @Override
    public boolean isReverseRead() {
        return mIsReverRead;
    }

    @Override
    public int getCurPageSize() {
        return mCurPageDatas.size();
    }


    private boolean mIsNeddVerLayout = true;

    public void setIsNeedVerLayout(boolean isNeedVerLayout) {
        mIsNeddVerLayout = isNeedVerLayout;
        Log.w(TAG, READER_LOG_TAG + " setIsNeedVerLayout isNeedVerLayout : " + isNeedVerLayout);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        if (ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            if (!mIsNeddVerLayout) {
                Log.w(TAG, READER_LOG_TAG + "onLayout mIsNeedLayout : " + mIsNeddVerLayout);
                return;
            }
            Log.w(TAG, READER_LOG_TAG + "onLayout mIsNeedLayout : " + mIsNeddVerLayout
                    + " mCurrentIndex : " + mCurrentIndex);
            int childCount = getChildCount();
            View childView = null;
            for (int i = 0; i < childCount; i++) {
                childView = getChildAt(i);
                if (childView instanceof ReaderPageView) {
                    childView.setTranslationX(0);
                    childView.setTranslationY(0);
                    if (isPrePage((ReaderPageView) childView)) {
                        childView.layout(0, -getHeight(), getWidth(), 0);
                    } else if (isCurrentPage((ReaderPageView) childView)) {
                        childView.layout(0, 0, getWidth(), getHeight());
                    } else if (isNextPage((ReaderPageView) childView)) {
                        childView.layout(0, getHeight(), getWidth(), getHeight() * 2);
                    }
                } else {
                    super.onLayout(changed, left, top, right, bottom);
                }
            }
        } else {
            ReaderPageView readerPageView = getCurReaderPageView();
            if (null != readerPageView && readerPageView.isPageViewMove()) {
                Log.w(TAG, ReaderLayoutView.READER_LOG_TAG + "onLayout isPageViewMove, no need onlayout.");
                return;
            }
            Log.w(TAG, READER_LOG_TAG + "onLayout  "
                    + " mCurrentIndex : " + mCurrentIndex
                    + " mTotalCurrentIndex : " + mTotalCurrentIndex
                    + " mLastPageIndex : " + mLastPageIndex);
            int childCount = getChildCount();
            View childView = null;
            //中间状态
            for (int i = 0; i < childCount; i++) {
                childView = getChildAt(i);
                if (childView instanceof ReaderPageView) {
                    childView.setTranslationX(0);
                    childView.setTranslationY(0);
                    if (isPrePage((ReaderPageView) childView)) {
                        if (mTotalCurrentIndex == 0 || mLastPageIndex == 0) {
                            childView.layout(0, 0, getWidth(), getHeight());
                        } else {
                            childView.layout(-getWidth(), 0, 0, getHeight());
                        }
                    } else if (isCurrentPage((ReaderPageView) childView)) {
                        childView.layout(0, 0, getWidth(), getHeight());
                    } else if (isNextPage((ReaderPageView) childView)) {
                        childView.layout(0, 0, getWidth(), getHeight());
                    }
                    mIsResetLayout = false;
                } else {
                    super.onLayout(changed, left, top, right, bottom);
                }
            }
        }

    }

    public boolean refreshReaderMoveMode(String readerMoveMode) {
        boolean isSuccess = false;
        if (!TextUtils.isEmpty(mReaderMoveMode)
                && mReaderMoveMode.equals(readerMoveMode)) {
            Log.w(TAG, READER_LOG_TAG + " refreshReaderMoveMode same readerMode : " + readerMoveMode);
            return isSuccess;
        }
        ReaderPageView curPageView = getPageViewByIndex(mCurrentIndex);
        ReaderPageView prePageView = getPrePageView();
        ReaderPageView nextPageView = getNextPageView();
        if (null == curPageView || null == prePageView || null == nextPageView) {
            Log.w(TAG, READER_LOG_TAG + " refreshReaderMoveMode ReaderPageView is not valid.");
            return isSuccess;
        }
        isSuccess = true;
        mReaderMoveMode = readerMoveMode;
        if (READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
            READER_LOG_TAG = VERTICAL_READER_TAG;
        } else {
            READER_LOG_TAG = HORIZON_READER_TAG;
        }
        curPageView.refreshReaderMode(mReaderMoveMode);
        prePageView.refreshReaderMode(mReaderMoveMode);
        nextPageView.refreshReaderMode(mReaderMoveMode);
        if (READER_PAGE_MODE_VERTICAL.equals(readerMoveMode)) {
            mInForWardChapter = false;
            mInPreChapter = false;
            if (mIsReverRead) {
                Log.w(TAG, READER_LOG_TAG + " refreshReaderMoveMode READER_PAGE_MODE_VERTICAL mIsReverRead.");
                //逆序情况刷新阅读情况需要重刷章节内容，并切换到章节首页
                mIsReverRead = false;
                if (READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode)) {
                    setIsNeedVerLayout(true);
                }
                reCalculatePage(null, true);
                return true;
            }
            mIsReverRead = false;
        }
        boolean isHorizonMode = !READER_PAGE_MODE_VERTICAL.equals(mReaderMoveMode);
        //横向阅读是当前页  竖向阅读是下一页
        if (isHorizonMode) {
            requestLayout();
            post(new Runnable() {
                @Override
                public void run() {
                    goNextPage(false);
                }
            });
        } else {
            int childCount = getChildCount();
            View childView = null;
            for (int i = 0; i < childCount; i++) {
                childView = getChildAt(i);
                if (childView instanceof ReaderPageView) {
                    childView.setTranslationX(0);
                    childView.setTranslationY(0);
                    if (childView == prePageView) {
                        childView.layout(0, -getHeight(), getWidth(), 0);
                    } else if (childView == curPageView) {
                        childView.layout(0, 0, getWidth(), getHeight());
                    } else if (childView == nextPageView) {
                        childView.layout(0, getHeight(), getWidth(), getHeight() * 2);
                    }
                }
            }
        }
        return isSuccess;

    }

    private void initChapterdata(String content) {
        mCurChapter = content;
    }

    public void setSplitString(String spliteStr) {
        mSplitStr = spliteStr;
    }

    public void calculateChapter(List<ReaderPageData> rdPageDatas, String title, String chapterContent, int width, int height) {
        try {
            if (width <= 0 || height <= 0 || TextUtils.isEmpty(chapterContent)) {
                Log.e(TAG, READER_LOG_TAG + " calculateChapter width : " + width
                        + " height : " + height
                        + " chapterContent is empty : " + TextUtils.isEmpty(chapterContent)
                        + " pre : " + (rdPageDatas == mPrePageDatas)
                        + " next : " + (rdPageDatas == mNextPageDatas));
                return;
            }
            // 标题高度占两行
            rdPageDatas.clear();
            float realWidth = width - 2 * SHADER_WIDTH;
            float realHeight = height - 2 * SHADER_WIDTH + (mLineHeight - mFontSize > 0 ? (mLineHeight - mFontSize) : 0) - DEFAULT_10_PADDING;
            if (mLineHeight - mFontSize <= 0) {
                Log.w(TAG, READER_LOG_TAG + " calculateChapter lineheight error mLineHeight : " + mLineHeight
                        + " mFontSize : " + mFontSize);
            }
            int oneLineCount = (int) (realWidth / mFontSize);
            int pageLineCount = (int) (realHeight / mLineHeight);
            if (oneLineCount <= 2 || pageLineCount <= 2 || realWidth <= 0 || realHeight <= 0) {
                Log.w(TAG, READER_LOG_TAG + " calculateChapter oneLineCount : " + oneLineCount
                        + " pageLineCount : " + pageLineCount
                        + " realWidth : " + realWidth
                        + " realHeight : " + realHeight);
                return;
            }
            Log.w(TAG, READER_LOG_TAG + " calculateChapter mFontSize : " + mFontSize
                    + "mLineHeight : " + mLineHeight
                    + "oneLineCount : " + oneLineCount
                    + " pageLineCount : " + pageLineCount
                    + " realWidth : " + realWidth
                    + " realHeight : " + realHeight);

            mPageLineCount = pageLineCount;
            if (TextUtils.isEmpty(chapterContent)) {
                Log.w(TAG, "calculateChapter chapterContent is empty.");
                return;
            }
            if (TextUtils.isEmpty(mSplitStr)) {
                Log.w(TAG, "calculateChapter mSplitStr is empty.");
                return;
            }
            //章节分段
            String[] tmpSections = chapterContent.split(mSplitStr);
            String section = "";
            List<String> lines = new ArrayList<>();
            String lineStr = "";
            int begin = 0;
            boolean isSectionHeader = true;
            //段落分行,并需要每个段落尾部主动添加换行
            for (int i = 0; i < tmpSections.length; i++) {
                section = tmpSections[i].replace("\\s*", "");
                isSectionHeader = true;
                int sectionLength = section.length();
                begin = 0;
                while (begin <= sectionLength) {
                    if (isSectionHeader) {
                        int deleteCount = 0;
                        isSectionHeader = false;
                        if ((oneLineCount - mIndentSize) > sectionLength) {
                            //当前段落只有一行
                            lineStr = getResources().getString(R.string.header_2_space) + section + "\n";
                            if (!TextUtils.isEmpty(lineStr) && lineStr.replace("\\s*", "").equals("\n")) {
                                Log.w(TAG, READER_LOG_TAG + " reach break point.");
                            } else {
                                lines.add(lineStr);
                            }
                            if (mIsDebug) {
                                Log.w(TAG, READER_LOG_TAG + "  calculateChapter    "
                                        + " lineStr : " + lineStr);
                            }
                            break;
                        } else {
                            if ((oneLineCount - mIndentSize) == sectionLength) {
                                lineStr = getResources().getString(R.string.header_2_space) + section.substring(begin, oneLineCount - mIndentSize) + "\n";
                            } else {
                                lineStr = getResources().getString(R.string.header_2_space) + section.substring(begin, oneLineCount - mIndentSize);
                            }
                            lines.add(lineStr);
                        }
                        begin = begin + (oneLineCount - mIndentSize) + deleteCount;
                    } else {
                        int deleteCount = 0;
                        if ((begin + oneLineCount) > sectionLength) {
                            //当前段落遍历完成
                            lineStr = section.substring(begin) + "\n";
                            if (!TextUtils.isEmpty(lineStr) && lineStr.replace("\\s*", "").equals("\n")) {
                                Log.w(TAG, READER_LOG_TAG + " reach break point.");
                            } else {
                                lines.add(lineStr);
                            }
                            if (mIsDebug) {
                                Log.w(TAG, READER_LOG_TAG + "  calculateChapter    "
                                        + " lineStr : " + lineStr);
                            }
                            break;
                        } else {
                            if ((begin + oneLineCount) == sectionLength) {
                                lineStr = section.substring(begin, begin + oneLineCount) + "\n";
                            } else {
                                lineStr = section.substring(begin, begin + oneLineCount);
                            }
                            lines.add(lineStr);
                        }
                        begin = begin + oneLineCount + deleteCount;
                    }
                    if (mIsDebug) {
                        Log.w(TAG, READER_LOG_TAG + "  calculateChapter    "
                                + " lineStr : " + lineStr);
                    }
                }
            }
            //行汇成页面
            int lineSize = lines.size();
            if (TextUtils.isEmpty(title)) {
                Log.w(TAG, READER_LOG_TAG + "  calculateChapter    "
                        + " title is empty."
                        + " pre : " + (rdPageDatas == mPrePageDatas)
                        + " next : " + (rdPageDatas == mNextPageDatas));
            }
            int pageSize = (lineSize - (pageLineCount - 2)) / pageLineCount + ((lineSize - (pageLineCount - 2)) % pageLineCount == 0 ? 0 : 1) + 1;
            int beginLine = 0;
            int firstaPageLineCount = pageLineCount - 2;
            ReaderPageData pageData = null;
            for (int i = 0; i < pageSize; i++) {
                if (i == 0) {
                    pageLineCount = firstaPageLineCount;
                } else {
                    pageLineCount = mPageLineCount;
                }
                if (beginLine + pageLineCount <= lineSize) {
                    pageData = new ReaderPageData();
                    pageData.pageIndex = i;
                    pageData.pageLineDatas.addAll(lines.subList(beginLine, beginLine + pageLineCount));
                    pageData.pageData = getDrawPageString(pageData.pageLineDatas);
                    rdPageDatas.add(pageData);
                    beginLine = beginLine + pageLineCount;
                } else {
                    pageData = new ReaderPageData();
                    pageData.pageIndex = i;
                    pageData.pageLineDatas.addAll(lines.subList(beginLine, lineSize));
                    pageData.pageData = getDrawPageString(pageData.pageLineDatas);
                    rdPageDatas.add(pageData);
                }

            }
            Log.w(TAG, READER_LOG_TAG + "  calculateChapter    "
                    + " width : " + width
                    + " height : " + height
                    + " realWidth : " + realWidth
                    + " realHeight : " + realHeight
                    + " oneLineCount : " + oneLineCount
                    + " pageLineCount : " + pageLineCount);
        } catch (Exception e) {
            Log.e(TAG, READER_LOG_TAG + " calculateChapter error : " + e.getMessage()
                    + " mSplitStr : " + mSplitStr
                    + " chapterContent : " + chapterContent);
        }
    }

    public void getPageContent(boolean isAll, int pageIndex, int startline, int endline, ReaderPageCallback readerPageCallback) {
        mReaderPageCallback = readerPageCallback;
        if (mCurPageDatas.size() == 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (null != readerPageCallback) {
                        readerPageCallback.onPageContent(getPageContents(isAll, pageIndex, startline, endline));
                    } else {
                        Log.w(TAG, "getPageContent readerPageCallback is null.");
                    }
                }
            });
        } else {
            if (null != readerPageCallback) {
                readerPageCallback.onPageContent(getPageContents(isAll, pageIndex, startline, endline));
            } else {
                Log.w(TAG, "getPageContent else readerPageCallback is null.");
            }
        }
    }

    private List<String> getPageContents(boolean isAll, int pageIndex, int startline, int endline) {
        if (pageIndex == -1 && mLastPageIndex != -1) {
            pageIndex = mLastPageIndex;
            Log.w(TAG, "getPageContents mLastPageIndex  : " + mLastPageIndex);
        }
        if (pageIndex < 0 || (pageIndex + 1) >= mCurPageDatas.size()) {
            Log.w(TAG, "getPageContents pageIndex is invalid, pageIndex: " + pageIndex
                    + " allpages : " + mCurPageDatas.size());
            return null;
        }
        List<String> datas = new ArrayList<>();
        ReaderPageData readerPageData = mCurPageDatas.get(pageIndex);
        if (null == readerPageData) {
            Log.w(TAG, "getPageContents readerPageData is null, pageIndex: " + pageIndex);
            return datas;
        }
        List<String> readerDatas = readerPageData.pageLineDatas;
        if (isAll) {
            return readerDatas;
        }
        List<String> tmpDatas = null;
        if (null != readerDatas && startline >= 0 && startline < endline &&
                endline < readerDatas.size()) {
            tmpDatas = readerDatas.subList(startline, endline);
        } else {
            Log.w(TAG, "getPageContents startline endline invalid, "
                    + " startline : " + startline
                    + " endline : " + endline
                    + "  readerDatas size : " + (null != readerDatas ? readerDatas.size() : " null readerDatas"));
        }
        if (null != tmpDatas) {
            datas.addAll(tmpDatas);
        }
        return tmpDatas;
    }

    public void setPageColor(int color) {
        mPageColor = color;
        setPageColor(true, mPageColor, 0, 0, null);
    }

    public void setPageColor(boolean isAll, int color, int startLine, int endLine, ReaderPageColorCallback readerPageColorCallback) {
        if (isAll) {
            mPageColor = color;
        }
        if (null == getCurPageView()) {
            post(new Runnable() {
                @Override
                public void run() {
                    boolean success = setPageColor(isAll, color, startLine, endLine);
                    if (null != readerPageColorCallback) {
                        readerPageColorCallback.onPageColor(success);
                    } else {
                        Log.w(TAG, "setPageColor readerPageColorCallback is null.");
                    }
                }
            });
        } else {
            boolean success = setPageColor(isAll, color, startLine, endLine);
            if (null != readerPageColorCallback) {
                readerPageColorCallback.onPageColor(success);
            } else {
                Log.w(TAG, "setPageColor else readerPageColorCallback is null.");
            }
        }
    }

    /**
     * 页面color是所有阅读页面，对应行背景color是当前页面
     *
     * @param isAll
     * @param color
     * @param startLine
     * @param endLine
     * @return
     */
    private boolean setPageColor(boolean isAll, int color, int startLine, int endLine) {
        boolean isSuccess = false;
        ReaderPageView readerPageView = getCurPageView();
        if (null == readerPageView) {
            Log.w(TAG, "setPageColor readerPageView is null. ");
            return isSuccess;
        }

        boolean isTextMode = readerPageView.isTextMode();
        ReaderText readerText = null;
        if (isTextMode) {
            readerText = readerPageView.getReaderText();
            if (null != readerText) {
                if (isAll) {
                    isSuccess = true;
                    readerText.setBgColor(color);
                    if (null != mPrePageView) {
                        mPrePageView.setBgColor(mPageColor);
                    }
                    if (null != mMidPageView) {
                        mMidPageView.setBgColor(mPageColor);
                    }
                    if (null != mNextPageView) {
                        mNextPageView.setBgColor(mPageColor);
                    }
                } else {
                    isSuccess = readerText.setLineBgColor(color, startLine, endLine);
                }
            } else {
                Log.w(TAG, "setPageColor readerText is null.");
            }
        } else {
            isSuccess = true;
            if (null != mPrePageView) {
                mPrePageView.setBgColor(mPageColor);
            }
            if (null != mMidPageView) {
                mMidPageView.setBgColor(mPageColor);
            }
            if (null != mNextPageView) {
                mNextPageView.setBgColor(mPageColor);
            }
            Log.w(TAG, "setPageColor invalid isTextMode : " + isTextMode);
        }
        return isSuccess;

    }

    public String getDrawPageString(List<String> pagelineDatas) {
        if (null == pagelineDatas) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        int size = pagelineDatas.size();
        for (int i = 0; i < size; i++) {
            stringBuilder.append(pagelineDatas.get(i));
        }
        String pageData = stringBuilder.toString();
        return pageData;
    }

    /**
     * 设置段落分隔符
     *
     * @param splite
     */
    public void setSectionChars(String splite) {

    }

    /**
     * 字体样式相关设置
     *
     * @param fontSize
     */
    public void setTextSize(int fontSize) {

    }

    public void setTextColor(int color) {
        mTextColor = color;
    }


    /**
     * 设置文字左右间距
     *
     * @param fontSize
     */
    public void setTextSpage(int fontSize) {

    }

    /**
     * 设置行高 low  normal high
     *
     * @param
     */
    public boolean setLineHeight(boolean isChecked, String type) {
        boolean isSuccess = false;
        if (isChecked && null != mLineHeightType && mLineHeightType.equals(type)) {
            Log.w(TAG, "setLineHeight isChecked true, same type : " + type);
            return isSuccess;
        }
        isSuccess = true;
        mLineHeightType = type;
        if (READERDIV_LINE_SPACE_LOW.equals(type)) {
            mLineHeight = 2 * mFontSize;
        } else if (READERDIV_LINE_SPACE_HIGH.equals(type)) {
            mLineHeight = 3 * mFontSize;
        } else {
            mLineHeight = 2 * mFontSize + mFontSize / 2;
        }
        return isSuccess;
    }

    /**
     * 设置行间距
     *
     * @param linespace
     */
    public void setLineSpace(int linespace) {

    }

    public void setIndentSize(int indent) {

    }


    @Override
    public boolean onClickCallback(MotionEvent event) {
        boolean isConsume = false;
        if (null == event) {
            Log.w(TAG, READER_LOG_TAG + "onClickCallback event is null.");
            return isConsume;
        }
        if (mGesture instanceof GestureDelegate) {
            ReaderPageView readerPageView = getCurReaderPageView();
            if (null != readerPageView && !readerPageView.isTextMoveAction()) {
                isConsume = ((GestureDelegate) mGesture).fireClickEvent(event, true);
            } else {
                Log.w(TAG, READER_LOG_TAG + " onClickCallback  fireClickEvent not trigger  isTextMoveAction true.");
            }
        } else {
            Log.w(TAG, READER_LOG_TAG + "onClickCallback this not GestureDelegate.");
        }
        if (!isConsume) {
            Log.w(TAG, READER_LOG_TAG + " onClickCallback isConsume is false.");
        }
        return isConsume;
    }

    public ReaderPageView getCurPageView() {
        return getCurReaderPageView();
    }

    @Override
    public ReaderPageView getPrePageView() {
        int nextIndex = mCurrentIndex - 1;
        if (nextIndex < 0) {
            nextIndex = 2;
        }
        switch (nextIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    public ReaderPageView getPrePageView(ReaderPageView curPageView) {
        int curPageIndex = -1;
        if (curPageView == mPrePageView) {
            curPageIndex = 0;
        } else if (curPageView == mMidPageView) {
            curPageIndex = 1;
        } else if (curPageView == mNextPageView) {
            curPageIndex = 2;
        }
        if (curPageIndex == -1 || null == curPageView) {
            Log.w(TAG, READER_LOG_TAG + " getPrePageView curPageIndex -1 or curPageView null, curPageIndex :  " + curPageIndex);
            return null;
        }
        int nextIndex = curPageIndex - 1;
        if (nextIndex < 0) {
            nextIndex = 2;
        }
        switch (nextIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    public ReaderPageView getNextPageView(ReaderPageView curPageView) {
        int curPageIndex = -1;
        if (curPageView == mPrePageView) {
            curPageIndex = 0;
        } else if (curPageView == mMidPageView) {
            curPageIndex = 1;
        } else if (curPageView == mNextPageView) {
            curPageIndex = 2;
        }
        if (curPageIndex == -1 || null == curPageView) {
            Log.w(TAG, READER_LOG_TAG + " getNextPageView curPageIndex -1 or curPageView null, curPageIndex :  " + curPageIndex);
            return null;
        }
        int nextIndex = curPageIndex + 1;
        if (nextIndex > 2) {
            nextIndex = 0;
        }
        switch (nextIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }


    @Override
    public ReaderPageView getPrePageView(int curTouchIndex) {
        int nextIndex = curTouchIndex - 1;
        if (nextIndex < 0) {
            nextIndex = 2;
        }
        switch (nextIndex) {
            case 0:
                return mPrePageView;
            case 1:
                return mMidPageView;
            case 2:
                return mNextPageView;
            default:
                break;
        }
        return null;
    }

    public interface ReaderCallback {
        //startIndex 上一页的inedx  currentIndex 当前页index  chapterIndex当前页的小说index  chapterAllIndex 所有小说页   recyclerIndex 循环页号 0 1 2
        void onPageChange(boolean isForward, int preIndex, int curIndex, int chapterIndex, int chapterAllIndex, int recyclerIndex, boolean ispreAd);

        void refreshPageView(ViewGroup prePageView, ViewGroup curPageView, String readerMode);
    }

    public interface ReaderChapterEndCallback {
        void onChapterEnd(String readerMode);
    }

    public interface ReaderChapterStartCallback {
        void onChapterStart(String readerMode);
    }

    public interface ReaderPageCallback {
        void onPageContent(List<String> content);
    }

    public interface ReaderPageColorCallback {
        void onPageColor(boolean isSuccess);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
        setChildComponent();
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }
}
