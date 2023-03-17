/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.adcustom;

import android.content.Context;

import org.hapjs.bridge.HybridView;

public interface AdCustomDataProvider {
    String NAME = "AdCustom";

    // 组件销毁时调用，按需自行适配
    void onComponentDestroy(String adUnitId, int componentRef);

    // 广告加载时调用，具体逻辑自行适配
    void loadCustomAd(Context context, String adUnitId, int ref, AdCustom adCustom);

    // 保证调用该类的其他具体方法前会先调用该方法，可根据业务需求，选择性使用
    void init(HybridView hybridView);

    // 低内存时调用，按需自行适配
    void onLowMemory();
}
