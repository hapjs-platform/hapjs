/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.utils;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import java.lang.reflect.Field;
import org.hapjs.widgets.view.swiper.LoopPagerAdapter;
import org.hapjs.widgets.view.swiper.PagerAdapter;
import org.hapjs.widgets.view.swiper.ViewPager;

public class ScrollableViewUtil {
    private static final int SCROLL_UP = -1;
    private static final int SCROLL_DOWN = 1;

    public static boolean canPullDown(ViewGroup viewGroup) {
        if (viewGroup instanceof AbsListView) {
            return !isAbsListViewToTop((AbsListView) viewGroup);
        }

        if (viewGroup instanceof RecyclerView) {
            return !isRecyclerViewToTop((RecyclerView) viewGroup);
        }

        if (viewGroup instanceof NestedScrollView) {
            return !isNestedScrollViewToTop((NestedScrollView) viewGroup);
        }

        if (viewGroup instanceof ScrollView) {
            return !isScrollViewToTop((ScrollView) viewGroup);
        }

        if (viewGroup instanceof ViewPager) {
            return !isViewPagerToTop((ViewPager) viewGroup);
        }

        if (viewGroup instanceof WebView) {
            return !isWebViewToTop((WebView) viewGroup);
        }

        return viewGroup.canScrollVertically(SCROLL_DOWN);
    }

    public static boolean canPullUp(ViewGroup viewGroup) {
        if (viewGroup instanceof AbsListView) {
            return !isAbsListViewToBottom((AbsListView) viewGroup);
        }

        if (viewGroup instanceof RecyclerView) {
            return !isRecyclerViewToBottom((RecyclerView) viewGroup);
        }

        if (viewGroup instanceof NestedScrollView) {
            return !isNestedScrollViewToBottom((NestedScrollView) viewGroup);
        }

        if (viewGroup instanceof ScrollView) {
            return !isScrollViewToBottom((ScrollView) viewGroup);
        }

        if (viewGroup instanceof ViewPager) {
            return !isViewPagerToBottom((ViewPager) viewGroup);
        }

        if (viewGroup instanceof WebView) {
            return !isWebViewToBottom((WebView) viewGroup);
        }

        return !viewGroup.canScrollVertically(SCROLL_UP);
    }

    /**
     * 判断AbsListView是否处于顶部位置，不可下滑滚动
     *
     * @param listView
     * @return
     */
    public static boolean isAbsListViewToTop(AbsListView listView) {
        int firstChildTop = 0;

        if (listView.getChildCount() <= 0) {
            // 没有child时，允许触发下拉
            return true;
        }
        // 如果ListView的子控件数量不为0，获取第一个子控件的top
        firstChildTop = listView.getChildAt(0).getTop() - listView.getPaddingTop();
        return listView.getFirstVisiblePosition() == 0 && firstChildTop <= 0;
    }

    /**
     * 判断AbsListView是否处理底部文职，不可上滑滚动
     *
     * @param listView
     * @return
     */
    public static boolean isAbsListViewToBottom(AbsListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null || listView.getChildCount() <= 0) {
            // 没有child或者adapter时，允许触发上拉
            return true;
        }

        if (listView.getLastVisiblePosition() == adapter.getCount() - 1) {
            View lastChild = listView.getChildAt(listView.getChildCount() - 1);
            return lastChild.getBottom() <= listView.getMeasuredHeight();
        }
        return false;
    }

    /**
     * 判断RecyclerView是否处理顶部位置，不可下滑滚动
     *
     * @param recyclerView
     * @return
     */
    public static boolean isRecyclerViewToTop(RecyclerView recyclerView) {
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager == null || manager.getItemCount() <= 0) {
            return true;
        }

        int firstChildTop = 0;
        if (recyclerView.getChildCount() > 0) {
            // 处理item高度超过一屏幕时的情况
            View firstVisibleChild = recyclerView.getChildAt(0);
            if (firstVisibleChild != null
                    && firstVisibleChild.getMeasuredHeight() >= recyclerView.getMeasuredHeight()) {
                return !recyclerView.canScrollVertically(-1);
            }

            View firstChild = recyclerView.getChildAt(0);
            RecyclerView.LayoutParams layoutParams =
                    (RecyclerView.LayoutParams) firstChild.getLayoutParams();
            firstChildTop =
                    firstChild.getTop()
                            - layoutParams.topMargin
                            - getRecyclerViewItemTopInset(layoutParams)
                            - recyclerView.getPaddingTop();
        }
        if (manager instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
            return layoutManager.findFirstCompletelyVisibleItemPosition() < 1 && firstChildTop == 0;
        } else if (manager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;
            int[] out = layoutManager.findFirstCompletelyVisibleItemPositions(null);
            return out[0] < 1 && firstChildTop == 0;
        }
        return false;
    }

    /**
     * 判断RecyclerView是否处理最左边位置，不可向左滚动
     *
     * @param recyclerView
     * @return
     */
    public static boolean isRecyclerViewToLeft(RecyclerView recyclerView) {
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager == null || manager.getItemCount() <= 0) {
            return true;
        }

        int firstChildLeft = 0;
        if (recyclerView.getChildCount() > 0) {
            // 处理item高度超过一屏幕时的情况
            View firstVisibleChild = recyclerView.getChildAt(0);
            if (firstVisibleChild != null
                    && firstVisibleChild.getMeasuredWidth() >= recyclerView.getMeasuredHeight()) {
                return !recyclerView.canScrollHorizontally(-1);
            }

            View firstChild = recyclerView.getChildAt(0);
            RecyclerView.LayoutParams layoutParams =
                    (RecyclerView.LayoutParams) firstChild.getLayoutParams();
            firstChildLeft =
                    firstChild.getLeft()
                            - layoutParams.leftMargin
                            - getRecyclerViewItemLeftInset(layoutParams)
                            - recyclerView.getPaddingLeft();
        }
        if (manager instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
            return layoutManager.findFirstCompletelyVisibleItemPosition() < 1
                    && firstChildLeft == 0;
        } else if (manager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;
            int[] out = layoutManager.findFirstCompletelyVisibleItemPositions(null);
            return out[0] < 1 && firstChildLeft == 0;
        }
        return false;
    }

    /**
     * 判断RecyclerView是否处理最右边位置，不可向右滚动
     *
     * @param recyclerView
     * @return
     */
    public static boolean isRecyclerViewToRight(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager == null || manager.getItemCount() <= 0) {
                return false;
            }

            if (manager instanceof LinearLayoutManager) {
                // 处理item高度超过一屏幕时的情况
                View lastVisibleChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                if (lastVisibleChild != null
                        &&
                        lastVisibleChild.getMeasuredWidth() >= recyclerView.getMeasuredHeight()) {
                    return !recyclerView.canScrollHorizontally(1);
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
                return layoutManager.findLastCompletelyVisibleItemPosition()
                        == layoutManager.getItemCount() - 1;
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;

                int[] out = layoutManager.findLastCompletelyVisibleItemPositions(null);
                int lastPosition = layoutManager.getItemCount() - 1;
                for (int position : out) {
                    if (position == lastPosition) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断RecyclerView是否处于底部位置，不可上滑滚动
     *
     * @param recyclerView
     * @return
     */
    public static boolean isRecyclerViewToBottom(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager == null || manager.getItemCount() <= 0) {
                return false;
            }

            if (manager instanceof LinearLayoutManager) {
                // 处理item高度超过一屏幕时的情况
                View lastVisibleChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                if (lastVisibleChild != null
                        &&
                        lastVisibleChild.getMeasuredHeight() >= recyclerView.getMeasuredHeight()) {
                    return !recyclerView.canScrollVertically(1);
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
                return layoutManager.findLastCompletelyVisibleItemPosition()
                        == layoutManager.getItemCount() - 1;
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;

                int[] out = layoutManager.findLastCompletelyVisibleItemPositions(null);
                int lastPosition = layoutManager.getItemCount() - 1;
                for (int position : out) {
                    if (position == lastPosition) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int getRecyclerViewItemTopInset(RecyclerView.LayoutParams layoutParams) {
        try {
            Field field = RecyclerView.LayoutParams.class.getDeclaredField("mDecorInsets");
            field.setAccessible(true);
            Rect decorInsets = (Rect) field.get(layoutParams);
            return decorInsets.top;
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    private static int getRecyclerViewItemLeftInset(RecyclerView.LayoutParams layoutParams) {
        try {
            Field field = RecyclerView.LayoutParams.class.getDeclaredField("mDecorInsets");
            field.setAccessible(true);
            Rect decorInsets = (Rect) field.get(layoutParams);
            return decorInsets.left;
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    public static boolean isWebViewToTop(WebView webView) {
        return webView.getScrollY() <= 0;
    }

    public static boolean isWebViewToBottom(WebView webView) {
        return webView.getContentHeight() * webView.getScaleY()
                - (webView.getHeight() + webView.getScrollY())
                <= 0;
    }

    public static boolean isScrollViewToTop(ScrollView scrollView) {
        return scrollView.getScrollY() <= 0;
    }

    public static boolean isScrollViewToBottom(ScrollView scrollView) {
        if (scrollView.getChildCount() == 0) {
            return true;
        }
        int contentHeight =
                scrollView.getMeasuredHeight()
                        + scrollView.getScrollY()
                        - scrollView.getPaddingTop()
                        - scrollView.getPaddingBottom();
        return contentHeight >= scrollView.getChildAt(0).getHeight();
    }

    public static boolean isNestedScrollViewToTop(NestedScrollView scrollView) {
        return scrollView.getScrollY() <= 0;
    }

    public static boolean isNestedScrollViewToBottom(NestedScrollView scrollView) {
        if (scrollView.getChildCount() == 0) {
            return true;
        }
        int contentHeight =
                scrollView.getMeasuredHeight()
                        + scrollView.getScrollY()
                        - scrollView.getPaddingTop()
                        - scrollView.getPaddingBottom();
        return contentHeight >= scrollView.getChildAt(0).getHeight();
    }

    public static boolean isViewPagerToTop(ViewPager viewPager) {
        if (viewPager.isHorizontal()) {
            return true;
        }
        if (viewPager.getAdapter() == null) {
            return true;
        }

        PagerAdapter adapter = viewPager.getAdapter();
        if (!(adapter instanceof LoopPagerAdapter)) {
            return viewPager.getCurrentItem() == 0;
        }

        return viewPager.getCurrentItem() == 0 && !((LoopPagerAdapter) adapter).isLoop();
    }

    public static boolean isViewPagerToBottom(ViewPager viewPager) {
        if (viewPager.isHorizontal()) {
            return true;
        }
        if (viewPager.getAdapter() == null) {
            return true;
        }
        PagerAdapter adapter = viewPager.getAdapter();
        if (!(adapter instanceof LoopPagerAdapter)) {
            return viewPager.getCurrentItem() == (adapter.getCount() - 1);
        }

        return viewPager.getCurrentItem() == (((LoopPagerAdapter) adapter).getActualItemCount() - 1)
                && !((LoopPagerAdapter) adapter).isLoop();
    }
}
