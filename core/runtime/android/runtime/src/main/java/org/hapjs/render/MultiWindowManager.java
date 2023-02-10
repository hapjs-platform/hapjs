/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.content.Context;
import android.text.TextUtils;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FoldingUtils;
import org.hapjs.render.vdom.DocAnimator;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;

public class MultiWindowManager {
    public static final String TAG = "MultiWindowManager";

    public static final String SHOPPING_MODE = "multiWindowShoppingMode";
    public static final String NAVIGATION_MODE = "multiWindowNavigationMode";

    private static String sMultiWindowModeType = SHOPPING_MODE;

    public static final int MULTI_WINDOW_DIVIDER_WIDTH = 6; // px

    public static boolean shouldApplyMultiWindowMode(Context context) {
        SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        // 折叠屏机型，设置了平行世界模式，且处于展开状态
        return sysOpProvider.isFoldableDevice(context)
                && FoldingUtils.isMultiWindowMode()
                && !isFoldState(context);
    }

    // 是否处于折叠状态
    private static boolean isFoldState(Context context) {
        SysOpProvider sysOpProvider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
        if (!sysOpProvider.isFoldableDevice(context)) {
            return false;
        }
        return sysOpProvider.isFoldStatusByDisplay(context);
    }

    public static void setMultiWindowModeType(String modeType) {
        if (TextUtils.equals(modeType, SHOPPING_MODE)
                || TextUtils.equals(modeType, NAVIGATION_MODE)) {
            sMultiWindowModeType = modeType;
        }
    }

    public static String getMultiWindowModeType() {
        return sMultiWindowModeType;
    }

    public static boolean isShoppingMode() {
        return SHOPPING_MODE.equals(sMultiWindowModeType);
    }

    public static boolean isNavigationMode() {
        return NAVIGATION_MODE.equals(sMultiWindowModeType);
    }

    public static float getFirstPageTranslationX(Context context) {
        return (DisplayUtil.getScreenWidth(context) + MULTI_WINDOW_DIVIDER_WIDTH) >> 1;
    }

    public static float getRightPageTranslationX(Context context) {
        return DisplayUtil.getScreenWidth(context) + MULTI_WINDOW_DIVIDER_WIDTH;
    }

    public static class MultiWindowPageChangeExtraInfo {

        private final boolean mIsMultiWindowMode;
        private final String mMultiWindowModeType;

        private Context mContext;

        private int mOldIndex;
        private int mNewIndex;
        private Page mOldPage;
        private Page mCurrentPage;
        private VDocument mLeftDoc;
        private VDocument mRightDoc;

        private PageManager mPageManager;

        public MultiWindowPageChangeExtraInfo(Context context, int oldIndex, int newIndex, Page oldPage, Page currPage,
                                              VDocument leftDoc,
                                              VDocument rightDoc, PageManager pageManager) {
            mContext = context;

            mIsMultiWindowMode = MultiWindowManager.shouldApplyMultiWindowMode(mContext);
            mMultiWindowModeType = MultiWindowManager.getMultiWindowModeType();

            mOldIndex = oldIndex;
            mNewIndex = newIndex;
            mOldPage = oldPage;
            mCurrentPage = currPage;
            mLeftDoc = leftDoc;
            mRightDoc = rightDoc;
            mPageManager = pageManager;
        }

        public boolean isOpenFirstPage() {
            return isOpen() && mNewIndex == 0;
        }

        public boolean isOpenSecondPage() {
            return isOpen() && mNewIndex == 1;
        }

        public boolean isCloseFirstPage() {
            return !isOpen() && mNewIndex == 0;
        }

        public boolean isOpen() {
            return mNewIndex > mOldIndex;
        }

        public boolean isReplace() {
            return mNewIndex == mOldIndex;
        }

        public boolean isReplaceLeftPage() {
            if (mPageManager != null) {
                if (mPageManager.getPageCount() <= 1) {
                    return false;
                }
                return mPageManager.getMultiWindowLeftPageId() == mOldPage.getPageId();
            }
            return false;
        }

        public boolean isReplaceRightPage() {
            if (mPageManager != null) {
                Page rightPage = mPageManager.getCurrPage();
                if (rightPage != null) {
                    return rightPage.getPageId() == mOldPage.getPageId();
                }
            }
            return false;
        }

        private boolean isReplaceFirstPage() {
            if (mPageManager != null && mPageManager.getPageCount() == 1) {
                return true;
            }
            return false;
        }

        public float getTranslationXWithNoAnim(Context context) {
            float translationX;
            if (isReplace()) {
                if (isReplaceFirstPage()) {
                    translationX = MultiWindowManager.getFirstPageTranslationX(context);
                } else {
                    if (isReplaceLeftPage()) {
                        translationX = 0;
                    } else {
                        translationX = MultiWindowManager.getRightPageTranslationX(context);
                    }
                }
            } else {
                translationX = MultiWindowManager.getFirstPageTranslationX(context);
            }
            return translationX;
        }

        public VDocument getDetachTargetDoc() {
            VDocument detachTargetDocument;

            if (isReplace()) {
                if (isReplaceLeftPage()) {
                    detachTargetDocument = mLeftDoc;
                } else {
                    detachTargetDocument = mRightDoc;
                }
                return detachTargetDocument;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage() || isOpenSecondPage()) {
                            detachTargetDocument = null;
                        } else {
                            detachTargetDocument = mLeftDoc;
                        }
                    } else {
                        detachTargetDocument = mRightDoc;
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage() || isOpenSecondPage()) {
                            detachTargetDocument = null;
                        } else {
                            detachTargetDocument = mRightDoc;
                        }
                    } else {
                        detachTargetDocument = mRightDoc;
                    }
                    break;
                default:
                    detachTargetDocument = mRightDoc;
                    break;
            }

            return detachTargetDocument;
        }

        public Page getDetachTargetPage() {
            Page detachTargetPage;

            if (isReplace()) {
                detachTargetPage = mOldPage;
                return detachTargetPage;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage() || isOpenSecondPage()) {
                            detachTargetPage = null;
                        } else {
                            detachTargetPage = mPageManager.getPrePage(mOldPage);
                        }
                    } else {
                        detachTargetPage = mOldPage;
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage() || isOpenSecondPage()) {
                            detachTargetPage = null;
                        } else {
                            detachTargetPage = mOldPage;
                        }
                    } else {
                        detachTargetPage = mOldPage;
                    }
                    break;
                default:
                    detachTargetPage = mOldPage;
                    break;
            }

            return detachTargetPage;
        }

        public int getDetachAnimType() {
            int detachAnimType;

            if (isReplace()) {
                detachAnimType = DocAnimator.TYPE_UNDEFINED;
                return detachAnimType;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        detachAnimType = DocAnimator.TYPE_MULTI_WINDOW_SHOPPING_MODE_DETACH_OPEN_EXIT;
                    } else {
                        if (isCloseFirstPage()) {
                            detachAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_DETACH_CLOSE_EXIT;
                        } else {
                            detachAnimType = DocAnimator.TYPE_MULTI_WINDOW_SHOPPING_MODE_DETACH_CLOSE_EXIT;
                        }
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        detachAnimType = DocAnimator.TYPE_MULTI_WINDOW_NAVIGATION_MODE_DETACH_OPEN_EXIT;
                    } else {
                        if (isCloseFirstPage()) {
                            detachAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_DETACH_CLOSE_EXIT;
                        } else {
                            detachAnimType = DocAnimator.TYPE_MULTI_WINDOW_NAVIGATION_MODE_DETACH_CLOSE_EXIT;
                        }
                    }
                    break;
                default:
                    detachAnimType = DocAnimator.TYPE_UNDEFINED;
                    break;
            }

            return detachAnimType;
        }

        public VDocument getMoveTargetDoc() {
            VDocument moveTargetDocument;

            if (isReplace()) {
                return null;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        moveTargetDocument = mRightDoc;
                        mLeftDoc = moveTargetDocument;
                        mRightDoc = null;
                    } else {
                        moveTargetDocument = mLeftDoc;
                        mLeftDoc = null;
                        mRightDoc = moveTargetDocument;
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        if (isOpenSecondPage()) {
                            moveTargetDocument = mRightDoc;
                            mLeftDoc = moveTargetDocument;
                            mRightDoc = null;
                        } else {
                            moveTargetDocument = null;
                        }
                    } else {
                        if (isCloseFirstPage()) {
                            moveTargetDocument = mLeftDoc;
                            mLeftDoc = null;
                            mRightDoc = moveTargetDocument;
                        } else {
                            moveTargetDocument = null;
                        }
                    }
                    break;
                default:
                    moveTargetDocument = null;
                    break;
            }

            return moveTargetDocument;
        }

        public VDocument getMovedLeftDoc() {
            return mLeftDoc;
        }

        public VDocument getMovedRightDoc() {
            return mRightDoc;
        }

        public Page getMoveTargetPage() {
            Page moveTargetPage;

            if (isReplace()) {
                return null;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage()) {
                            moveTargetPage = null;
                        } else {
                            moveTargetPage = mOldPage;
                        }
                    } else {
                        moveTargetPage = mCurrentPage;
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        if (isOpenSecondPage()) {
                            moveTargetPage = mOldPage;
                        } else {
                            moveTargetPage = null;
                        }
                    } else {
                        if (isCloseFirstPage()) {
                            moveTargetPage = mCurrentPage;
                        } else {
                            moveTargetPage = null;
                        }
                    }
                    break;
                default:
                    moveTargetPage = null;
                    break;
            }

            return moveTargetPage;
        }

        public int getMoveAnimType() {
            int moveAnimType;

            if (isReplace()) {
                return DocAnimator.TYPE_UNDEFINED;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        if (isOpenSecondPage()) {
                            moveAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_MOVE_OPEN_TO_LEFT;
                        } else {
                            moveAnimType = DocAnimator.TYPE_MULTI_WINDOW_SHOPPING_MODE_MOVE_OPEN_TO_LEFT;
                        }
                    } else {
                        if (isCloseFirstPage()) {
                            moveAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_MOVE_CLOSE_TO_MIDDLE;
                        } else {
                            moveAnimType = DocAnimator.TYPE_MULTI_WINDOW_SHOPPING_MODE_MOVE_CLOSE_TO_RIGHT;
                        }
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        if (isOpenSecondPage()) {
                            moveAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_MOVE_OPEN_TO_LEFT;
                        } else {
                            moveAnimType = DocAnimator.TYPE_UNDEFINED;
                        }
                    } else {
                        if (isCloseFirstPage()) {
                            moveAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_MOVE_CLOSE_TO_MIDDLE;
                        } else {
                            moveAnimType = DocAnimator.TYPE_UNDEFINED;
                        }
                    }
                    break;
                default:
                    moveAnimType = DocAnimator.TYPE_UNDEFINED;
                    break;
            }

            return moveAnimType;
        }

        public Page getAttachTargetPage() {
            Page attachTargetPage;

            if (isReplace()) {
                return mCurrentPage;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        attachTargetPage = mCurrentPage;
                    } else {
                        if (isCloseFirstPage()) {
                            if (mCurrentPage.shouldRefresh()) {
                                attachTargetPage = mCurrentPage;
                            } else {
                                attachTargetPage = null;
                            }
                        } else {
                            attachTargetPage = mPageManager.getPrePage(mCurrentPage);
                        }
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        attachTargetPage = mCurrentPage;
                    } else {
                        if (isCloseFirstPage()) {
                            if (mCurrentPage.shouldRefresh()) {
                                attachTargetPage = mCurrentPage;
                            } else {
                                attachTargetPage = null;
                            }
                        } else {
                            attachTargetPage = mCurrentPage;
                        }
                    }
                    break;
                default:
                    attachTargetPage = mCurrentPage;
                    break;
            }

            return attachTargetPage;
        }

        public int getAttachAnimType() {
            int attachAnimType;

            if (isReplace()) {
                return DocAnimator.TYPE_UNDEFINED;
            }

            switch (mMultiWindowModeType) {
                case SHOPPING_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage()) {
                            attachAnimType = DocAnimator.TYPE_UNDEFINED;
                        } else if (isOpenSecondPage()) {
                            attachAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_ATTACH_OPEN_ENTER;
                        } else {
                            attachAnimType = DocAnimator.TYPE_MULTI_WINDOW_SHOPPING_MODE_ATTACH_OPEN_ENTER;
                        }
                    } else {
                        if (isCloseFirstPage() && mCurrentPage.shouldRefresh()) {
                            attachAnimType = DocAnimator.TYPE_UNDEFINED;
                        } else {
                            attachAnimType = DocAnimator.TYPE_MULTI_WINDOW_SHOPPING_MODE_ATTACH_CLOSE_ENTER;
                        }
                    }
                    break;
                case NAVIGATION_MODE:
                    if (isOpen()) {
                        if (isOpenFirstPage()) {
                            attachAnimType = DocAnimator.TYPE_UNDEFINED;
                        } else if (isOpenSecondPage()) {
                            attachAnimType = DocAnimator.TYPE_MULTI_WINDOW_COMMON_ATTACH_OPEN_ENTER;
                        } else {
                            attachAnimType = DocAnimator.TYPE_MULTI_WINDOW_NAVIGATION_MODE_ATTACH_OPEN_ENTER;
                        }
                    } else {
                        if (isCloseFirstPage() && mCurrentPage.shouldRefresh()) {
                            attachAnimType = DocAnimator.TYPE_UNDEFINED;
                        } else {
                            attachAnimType = DocAnimator.TYPE_MULTI_WINDOW_NAVIGATION_MODE_ATTACH_CLOSE_ENTER;
                        }
                    }
                    break;
                default:
                    attachAnimType = DocAnimator.TYPE_UNDEFINED;
                    break;
            }

            return attachAnimType;
        }

        public boolean isMultiWindowMode() {
            return mIsMultiWindowMode;
        }

        public boolean isShoppingMode() {
            return mIsMultiWindowMode && SHOPPING_MODE.equals(mMultiWindowModeType);
        }

        public boolean isNavigationMode() {
            return mIsMultiWindowMode && NAVIGATION_MODE.equals(mMultiWindowModeType);
        }

    }

}
