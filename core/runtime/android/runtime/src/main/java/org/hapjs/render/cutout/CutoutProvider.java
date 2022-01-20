/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.cutout;

import android.content.Context;
import android.graphics.Rect;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

/**
 * Android8,8.1异形屏适配接口，由各家自己实现调用内部私有api。 请在Application中添加到{@link org.hapjs.runtime.ProviderManager}中
 */
public interface CutoutProvider {

    String NAME = "cutout";

    /**
     * 是否为异形屏。目前暂时只适配刘海屏和水滴屏幕
     *
     * @param window
     * @return
     */
    boolean isCutoutScreen(@NonNull Context context, @NonNull Window window);

    /**
     * 获取异形区域的显示位置，位置参考相对于竖屏，左上角为原点。如果该接口无法获取到异形区的信息，将调用 {@link #getCutoutHeight(Context,
     * Window)}接口获取异形区的高度
     *
     * @return
     */
    @Nullable
    List<Rect> getCutoutDisplay(@NonNull Context context, @NonNull Window window);

    /**
     * 获取异形区域的高度(刘海，水滴)。如果私有接口无法获取到异形区域的完整位置，那么将调用此接口 获取高度。参考位置相对于竖屏。
     *
     * @return
     */
    int getCutoutHeight(@NonNull Context context, @NonNull Window window);
}
