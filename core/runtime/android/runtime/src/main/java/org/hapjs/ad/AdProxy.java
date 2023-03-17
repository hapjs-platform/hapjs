/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.ad;

import android.view.View;
import android.view.ViewGroup;

public abstract class AdProxy {
    protected static final String TAG = "AdProxy";

    /**
     * 初始化广告View
     *
     * @param type
     * @param adContainer
     * @return
     */
    public abstract View initAdView(String type, ViewGroup adContainer);

    /**
     * 加载广告数据
     *
     * @param unitId
     * @param type
     */
    public abstract void loadAd(String unitId, String type);

    /**
     * 绑定点击区域
     *
     * @param unitId
     * @param type
     * @param container
     */
    public abstract void bindClickView(String unitId, String type, NativeAdEntity container);


    /**
     * 释放广告资源
     */
    public abstract void destroy();
}
