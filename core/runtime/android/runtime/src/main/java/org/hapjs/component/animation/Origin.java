/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.text.TextUtils;
import android.view.View;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;

public class Origin {

    public static final int ORIGIN_X = 0;
    public static final int ORIGIN_Y = 1;

    public static float parseOrigin(String origin, int type, View target, HapEngine hapEngine) {
        if (TextUtils.isEmpty(origin) || target == null || hapEngine == null) {
            return FloatUtil.UNDEFINED;
        }
        float originValue = FloatUtil.UNDEFINED;
        String[] originArray = origin.split(" ");
        int originIndex = type;
        if (originArray.length > originIndex && !TextUtils.isEmpty(originArray[originIndex])) {
            String originStr = originArray[originIndex];
            if (originStr.endsWith("%")) {
                // TODO: 19-8-22 transform-origin 支持百分比
                // 根据文档, 目前不支持百分比, 后续支持的话再开放.
                // 如需支持百分比, 应保证在 View 布局完成之后再调此方法, 否则 View 的宽高为 0 ,
                // 无法计算出正确的 origin

            /*String temp = originStr.substring(0, originStr.indexOf("%"));
            float percent = Float.parseFloat(temp) / 100;
            originValue = target.getWidth() * percent;*/
            } else {
                originValue = Attributes.getFloat(hapEngine, originStr);
            }
        }
        return originValue;
    }
}
