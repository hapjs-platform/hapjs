/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view;


import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.widgets.view.swiper.PagerAdapter;
import org.hapjs.widgets.view.swiper.ViewPager;
import org.hapjs.widgets.view.text.PageTextView;

import java.util.ArrayList;


public class BookPagerView extends FrameLayout implements ComponentHost {
    BookPagerAdapter pagerAdapter;
    ViewPager viewPager;
    private Component mComponent;
    private String text;
    private boolean pageChanged = false;
    private BookEventListener mBookEventListener;

    public BookPagerView(@NonNull Context context) {
        super(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        viewPager = new ViewPager(getContext());
        pagerAdapter = new BookPagerAdapter(getContext());
        viewPager.setAdapter(pagerAdapter);
        //不进行页数的限制，不然页码无法计算出来
        viewPager.setOffscreenPageLimit(10000);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (pageChanged) {
                    //避免一直回调
                    pageChanged = false;
                    if (mBookEventListener != null) {
                        mBookEventListener.onPageChanged(position + 1, pagerAdapter.getCount());
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_SETTLING) {
                    pageChanged = true;
                }
            }
        });
        addView(viewPager, lp);
    }

    public void setOriginText(String text) {
        if (!TextUtils.isEmpty(this.text) && text.equals(this.text)) {
            return;
        }
        this.text = text;
        post(() -> {
            int[] location = new int[2];
            getLocationOnScreen(location);
            //确保组件能刚好撑满一屏
            getComponent().setHeight(String.valueOf(DisplayUtil.getScreenHeight(getContext()) - location[1] - dp2px(15)));
            pagerAdapter.createPage(text, 0);
        });
    }

    public int dp2px(float dpValue) {
        return (int) (0.5f + dpValue * Resources.getSystem().getDisplayMetrics().density);
    }

    public void addOriginText(String text, int pageIndex) {
        if (TextUtils.isEmpty(text) || pageIndex < 0) {
            return;
        }
        pagerAdapter.createPage(text, pageIndex);
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    private class BookPagerAdapter extends PagerAdapter {

        private Context context;

        private ArrayList<PageTextView> views;

        public BookPagerAdapter(Context context) {
            this.context = context;
            views = new ArrayList<>();
        }

        public void createPage(String text, int index) {
            PageTextView pageTextView = new PageTextView(context);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            pageTextView.setLayoutParams(lp);
            pageTextView.setPageSplitCallback(new PageTextView.PageSplitCallback() {
                @Override
                public void nextContent(String text) {
                    pagerAdapter.createPage(text, index + 1);
                }

                @Override
                public void onSplitPageEnd() {
                    if (mBookEventListener != null) {
                        mBookEventListener.onPageSplitEnd(pagerAdapter.getCount());
                    }
                }
            });
            pageTextView.setOriginText(text, getComponent().getHeight() - getPaddingBottom() - getPaddingTop(), getWidth());
            views.add(index, pageTextView);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return views.size();
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object, int position) {
            return object == view;
        }

        @Override
        public int getItemPosition(Object object) {
            if (views.contains(object)) {
                return views.indexOf(object);
            } else {
                return POSITION_NONE;
            }
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            PageTextView pageTextView = views.get(position);
            container.addView(pageTextView);
            pageTextView.showContent();
            return views.get(position);
        }
    }

    public void setBookEventListener(BookEventListener mBookEventListener) {
        this.mBookEventListener = mBookEventListener;
    }

    public interface BookEventListener {
        void onPageSplitEnd(int totalPage);

        void onPageChanged(int curPage, int totalPage);
    }

}
