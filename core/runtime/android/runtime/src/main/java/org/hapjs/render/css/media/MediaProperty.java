/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

public abstract class MediaProperty {

    // min max
    static final int HEIGHT = 0;
    // min max
    static final int WIDTH = 1;
    // min max
    static final int DEVICE_HEIGHT = 2;
    // min max
    static final int DEVICE_WIDTH = 3;
    // min max
    static final int RESOLUTION = 4; // 设备的分辨率，支持dpi dppx 或者dpcm单位
    // min max
    static final int ASPECT_RATIO = 5; // 设备中的页面可见区域宽度与高度的比率。 如：aspect-ratio:1/1
    static final int ORIENTATION = 6; // 可选值：orientation: portrait、orientation:landscape

    static final int ORIENTATION_PORTRAIT = 7; // 可选值：orientation: portrait、orientation:landscape
    static final int ORIENTATION_LANDSCAPE = 8; // 可选值：orientation: portrait、orientation:landscape

    static final int PREFERS_COLOR_SCHEME =
            9; // 设备主题模式prefers-color-scheme，可选值 light 、 dark 、
    // no-preference（表示用户未指定操作系统主题。其作为布尔值时以false输出）

    static final int INVALID_PROPERTY = 10; // 无效的属性

    private CompareOperator op;
    private Object value;

    CompareOperator getCompareOperator() {
        return op;
    }

    void setCompareOperator(CompareOperator op) {
        this.op = op;
    }

    public boolean getResult() {
        if (getValue() == null || op == null) {
            return false;
        }
        return op.compare(getValue());
    }

    abstract void updateMediaPropertyInfo(MediaPropertyInfo info);

    Object getValue() {
        return value;
    }

    void setValue(Object value) {
        this.value = value;
    }

    abstract int getType();
}
