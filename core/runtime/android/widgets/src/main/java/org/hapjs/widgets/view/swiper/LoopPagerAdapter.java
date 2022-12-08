/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.system.utils.TalkBackUtils;
import org.hapjs.widgets.R;

public class LoopPagerAdapter extends BaseLoopPagerAdapter {

    /**
     * 设置loop=true时的item总数 动态设置loop=true时，如果MAX_COUNT太大，ViewPager中的populate方法时间开销也会很大，因此取值
     * 5000，以保证MAX_COUNT足够大的同时也不会太耗时,粗略测试不同MAX_COUNT下populate耗时为： 1000 10ms左右 5000 15ms左右 10000 25ms左右
     * 100000 50ms左右
     */
    public static final int MAX_COUNT = 5000;

    private LoopViewPager mViewPager;

    private Container mContainer;
    private List<RecyclerDataItem> mRecyclerDataItems = new ArrayList<>();
    private Map<Component, RecyclerDataItem> mCreatedComponents = new ArrayMap<>();
    private Container.RecyclerItem mContainerDataItem;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean mIsEnableTalkBack;
    private Runnable mNotify =
            new Runnable() {
                @Override
                public void run() {
                    if (mViewPager != null) {
                        resetPages();
                        // no need to recover current index here,it is recovered in getItemPosition
                        notifyDataSetChanged();
                    }
                }
            };

    public LoopPagerAdapter(LoopViewPager viewPager) {
        mViewPager = viewPager;
        mIsEnableTalkBack = TalkBackUtils.isEnableTalkBack((null != viewPager ? viewPager.getContext() : null), false);
    }

    public void setData(Container container, Container.RecyclerItem data) {
        mContainerDataItem = data;
        mContainer = container;
        resetPages();
        notifyDataSetChanged();
    }

    public void notifyItemInserted() {
        // 避免频繁刷新
        mMainHandler.removeCallbacks(mNotify);
        mMainHandler.postDelayed(mNotify, 32);
    }

    public void notifyItemRemoved() {
        mMainHandler.removeCallbacks(mNotify);
        mMainHandler.postDelayed(mNotify, 32);
    }

    private void resetPages() {
        for (RecyclerDataItem recyclerDataItem : mRecyclerDataItems) {
            recyclerDataItem.removeAllTwinComponent();
        }
        mRecyclerDataItems.clear();
        if (mContainerDataItem != null) {
            for (RecyclerDataItem item : mContainerDataItem.getChildren()) {

                if (((Component.RecyclerItem) item).isFixOrFloating()) {
                    Log.w(
                            "Swiper",
                            "fix or floating child of Swiper is not support"); // Component#setPositionMode
                }
                mRecyclerDataItems.add(item);
            }
        }
    }

    @Override
    public Object instantiateActualItem(ViewGroup container, int position) {
        RecyclerDataItem item = mRecyclerDataItems.get(position);

        Component component = item.getBoundComponent();
        if (component != null && isLoop()) {
            item.addTwinComponent(component);
        }

        component = item.createRecycleComponent(mContainer);
        item.dispatchBindComponent(component);
        mContainer.addChild(component); // it will be destroy when mContainer destroy
        container.addView(component.getHostView());
        if (mIsEnableTalkBack) {
            View tmpHostView = component.getHostView();
            if (null != tmpHostView) {
                CharSequence contentDesp = tmpHostView.getContentDescription();
                if (TextUtils.isEmpty(contentDesp)) {
                    tmpHostView.setContentDescription(tmpHostView.getResources().getString(R.string.talkback_notitle_defaultstr));
                }
            }
        }
        mCreatedComponents.put(component, item);

        return component;
    }

    @Override
    public void destroyActualItem(ViewGroup container, int position, Object object) {
        Component component = (Component) object;

        RecyclerDataItem recyclerDataItem = mCreatedComponents.get(component);
        if (recyclerDataItem != null) {
            if (Objects.equals(recyclerDataItem.getBoundComponent(), component)) {
                recyclerDataItem.dispatchUnbindComponent();
            }
            if (recyclerDataItem.isTwinComponent(component)) {
                recyclerDataItem.removeTwinComponent(component);
            }
            mCreatedComponents.remove(component);
        }

        container.removeView(component.getHostView());
        mContainer.removeChild(component);
        component.destroy(); // view already detached
    }

    @Override
    public boolean isActualViewFromObject(View view, Object object, int position) {
        return view == ((Component) object).getHostView();
    }

    @Override
    public int getActualItemCount() {
        return mRecyclerDataItems == null ? 0 : mRecyclerDataItems.size();
    }

    @Override
    public int getActualItemPosition(Object object) {
        Component component = (Component) object;
        int size = mRecyclerDataItems.size();
        for (int i = 0; i < size; i++) {
            if (mRecyclerDataItems.get(i).getRef() == component.getRef()) {
                // current position may have changed, need to re-populate
                return i;
            }
        }
        // item is removed
        return POSITION_NONE;
    }

    public Container.RecyclerItem getContainerDataItem() {
        return mContainerDataItem;
    }
}
