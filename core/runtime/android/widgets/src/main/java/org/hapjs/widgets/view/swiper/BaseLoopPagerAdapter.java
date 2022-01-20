/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.view.View;
import android.view.ViewGroup;

public abstract class BaseLoopPagerAdapter extends PagerAdapter {

    private boolean mLoop = false;

    public boolean isLoop() {
        return mLoop;
    }

    public void setLoop(boolean loop) {
        if (mLoop == loop) {
            return;
        }
        mLoop = loop;
        notifyDataSetChanged();
    }

    @Override
    public final int getCount() {
        int itemCount = getActualItemCount();
        if (isLoop()) {
            return itemCount > 1 ? itemCount + 2 : itemCount;
        }
        return itemCount;
    }

    @Override
    public final Object instantiateItem(ViewGroup container, int position) {
        return instantiateActualItem(container, convertToRealPosition(position));
    }

    @Override
    public final void destroyItem(ViewGroup container, int position, Object object) {
        destroyActualItem(container, convertToRealPosition(position), object);
    }

    @Override
    public final boolean isViewFromObject(View view, Object object, int position) {
        return isActualViewFromObject(view, object, convertToRealPosition(position));
    }

    @Override
    public final int getItemPosition(Object object) {
        int position = getActualItemPosition(object);
        if (position >= 0) {
            return convertToLoopPosition(position);
        }
        return position;
    }

    public int convertToRealPosition(int position) {
        if (!mLoop) {
            return position;
        }

        int actualItemCount = getActualItemCount();

        if (position == 0) {
            return actualItemCount - 1;
        } else if (position == actualItemCount + 1) {
            return 0;
        } else {
            return position - 1;
        }
    }

    public int convertToLoopPosition(int position) {
        if (!mLoop) {
            return position;
        }

        // 向右+1
        position++;
        return position;
    }

    public abstract Object instantiateActualItem(ViewGroup container, int position);

    public abstract void destroyActualItem(ViewGroup container, int position, Object object);

    public abstract boolean isActualViewFromObject(View view, Object object, int position);

    public abstract int getActualItemPosition(Object object);

    public abstract int getActualItemCount();
}
