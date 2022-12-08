/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.tab;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.AbstractScrollable;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.HScrollable;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ScrollView;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.TabContentView;

@WidgetAnnotation(
        name = TabContent.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class TabContent extends AbstractScrollable<TabContentView>
        implements SwipeObserver, HScrollable {

    protected static final String WIDGET_NAME = "tab-content";

    // attribute
    private static final String SCROLLABLE = "scrollable";

    private TabPageAdapter mAdapter;
    private int mDomIndex;
    private int mPageIndex = -1;

    public TabContent(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TabContentView createViewImpl() {
        TabContentView tabContentView = new TabContentView(mContext);
        tabContentView.setComponent(this);
        mAdapter = new TabPageAdapter();
        tabContentView.setAdapter(mAdapter);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        tabContentView.setLayoutParams(params);
        tabContentView.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageScrolled(
                            final int position, float positionOffset, int positionOffsetPixels) {
                        if (positionOffset == 0f && mPageIndex != position) {
                            mPageIndex = position;
                            processAppearanceEvent();
                            setClipChildrenInner();
                        }
                    }
                });
        return tabContentView;
    }

    @Override
    public void setFlex(float flex) {
        if (mHost == null) {
            return;
        }

        if (mHost.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) mHost.getLayoutParams()).weight = flex;
        }
    }

    @Override
    public void addView(View childView, int index) {
        mAdapter.addPage(childView, index);
        mAdapter
                .getPage(index)
                .addScrollViewListener(
                        new ScrollView.ScrollViewListener() {
                            @Override
                            public void onScrollChanged(ScrollView scrollView, int x, int y,
                                                        int oldx, int oldy) {
                                processAppearanceEvent();
                            }
                        });

        if (mDomIndex == mAdapter.getCount() - 1) {
            mHost.setCurrentItem(mDomIndex, false);
        }
    }

    @Override
    public void removeChild(Component child) {
        int index = removeChildInternal(child);
        if (index >= 0 && mAdapter != null) {
            mAdapter.removePageAt(index);
        }
    }

    @Override
    public void removeView(View child) {
        // replace with removePageAt.
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case SCROLLABLE:
                setScrollable(Attributes.getBoolean(attribute, true));
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
        super.setClipChildren(clipChildren);
        setClipChildrenInner();
    }

    private void setClipChildrenInner() {
        if (mPageIndex > -1) {
            ViewGroup parent = mAdapter.getPage(mPageIndex);
            setClipChildrenInternal(parent, mClipChildren);
        }
    }

    void setDomIndex(int index) {
        mDomIndex = index;
        setCurrentItem(mDomIndex);
    }

    void setCurrentItem(int position) {
        if (mHost == null) {
            return;
        }

        mHost.setCurrentItem(position, false);
    }

    void addOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        if (mHost == null) {
            return;
        }
        mHost.addOnPageChangeListener(listener);
    }

    private void setScrollable(boolean scrollable) {
        if (mHost == null) {
            return;
        }
        mHost.setScrollable(scrollable);
    }

    private class TabPageAdapter extends PagerAdapter {
        private static final String TAG = "TabPageAdapter";
        private List<ViewGroup> views;

        public TabPageAdapter() {
            views = new ArrayList<>();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ViewGroup view = views.get(position);
            if (view.getParent() != null) {
                container.removeView(view);
            }
            container.addView(view);
            setClipChildrenInternal(view, mClipChildren);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (container != null && object instanceof View) {
                container.removeView((View) object);
            }
        }

        @Override
        public int getCount() {
            return views == null ? 0 : views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            int position = views.indexOf(object);
            return position == -1 ? POSITION_NONE : position;
        }

        void addPage(View view, int index) {
            TabContentScrollView scrollView = new TabContentScrollView(mContext);

            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            }

            if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            scrollView.addView(view, lp);
            views.add(index, scrollView);
            int lastIndex = mHost.getCurrentItem();
            notifyDataSetChanged();
            // fix the bug that current index changed when insert item to current position;
            mHost.setCurrentItem(lastIndex, false);
        }

        void removePageAt(int index) {
            if (views == null || mHost == null) {
                return;
            }
            int pageLength = views.size();
            if (index < 0 || index > pageLength - 1) {
                Log.w(TAG, "removePageAt: wrong index = " + index + " total pageLength= "
                        + pageLength);
                return;
            }
            views.remove(index);
            notifyDataSetChanged();
        }

        TabContentScrollView getPage(int index) {
            return (TabContentScrollView) views.get(index);
        }
    }
}
