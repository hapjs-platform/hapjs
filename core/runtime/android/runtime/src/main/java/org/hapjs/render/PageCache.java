/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.common.utils.lrucache.LruCache;

public class PageCache extends LruCache<String, Page> {
    public static final String TAG = "PageCache";
    private PageManager.PageChangedListener mPageChangedListener;

    public PageCache(PageManager.PageChangedListener pageChangedListener) {
        super();
        this.mPageChangedListener = pageChangedListener;
    }

    public PageCache(int capacity, PageManager.PageChangedListener pageChangedListener) {
        super(capacity);
        this.mPageChangedListener = pageChangedListener;
    }

    @Override
    public void onRemoved(String key, Page value) {
        if (value != null && mPageChangedListener != null) {
            ThreadUtils.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mPageChangedListener.onPageRemoved(-1, value);
                        }
                    });
        }
    }
}
