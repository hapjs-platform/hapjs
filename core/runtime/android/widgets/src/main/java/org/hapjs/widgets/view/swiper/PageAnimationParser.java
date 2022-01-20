/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.animation.ArgbEvaluator;
import android.util.Log;
import android.view.View;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.component.view.ComponentHost;

public class PageAnimationParser {

    private static final String TAG = "PageAnimationParser";

    public static ViewPager.PageTransformer parse(
            SwiperView swiperView, SwiperAnimation swiperAnimation) {

        ArgbEvaluator argbEvaluator = new ArgbEvaluator();

        return new ViewPager.PageTransformer() {
            float translateX = 0;
            float translateY = 0;
            float alpha = 1;
            float rotate = 0;
            float rotateX = 0;
            float rotateY = 0;
            float scaleX = 1;
            float scaleY = 1;
            float initPosition = 0;
            int currColor = IntegerUtil.UNDEFINED;
            int colorStart = IntegerUtil.UNDEFINED;
            int colorEnd = IntegerUtil.UNDEFINED;

            @Override
            public void transformPage(View page, float position) {

                float ratio;
                float endPosition = 0;
                float previousPosition = 0;
                float nextPosition = 0;

                if (swiperView != null
                        && swiperView.getViewPager() != null
                        && swiperView.getViewPager().getClientWidth() != 0) {
                    if (swiperView.getViewPager().getAdapter() != null
                            && swiperView.getViewPager().getAdapter() instanceof LoopPagerAdapter) {
                        // 第一次进入swiper页面时, 需要让position的值等于initPosition
                        initPosition =
                                (page.getLeft()
                                        - swiperView.getViewPager().getClientWidth()
                                        * (swiperView.getViewPager().getCurrentItem() + 1))
                                        * 1.0f
                                        / swiperView.getViewPager().getClientWidth();
                        // loop为false, index不为0时，对 initPosition + 1 处理后值才能等于position
                        if (!((LoopPagerAdapter) swiperView.getViewPager().getAdapter()).isLoop()) {
                            initPosition = 1 + initPosition;
                        }
                    }
                    endPosition =
                            (float) swiperView.getViewPager().getPaddingLeft()
                                    / swiperView.getViewPager().getClientWidth();
                    previousPosition = endPosition + 1;
                    nextPosition = endPosition - 1;
                }

                if ((FloatUtil.floatsEqual(position - nextPosition, 0) || position > nextPosition)
                        && (FloatUtil.floatsEqual(position - previousPosition, 0)
                        || position < previousPosition)) {

                    // position等于0或1是考虑到从最后一个page滑到第一个page的情况
                    if (FloatUtil.floatsEqual(position - endPosition, 0)
                            || FloatUtil.floatsEqual(position - nextPosition, 0)
                            || FloatUtil.floatsEqual(position - previousPosition, 0)
                            || FloatUtil.floatsEqual(position - initPosition, 0)
                            || FloatUtil.floatsEqual(position, 0)
                            || FloatUtil.floatsEqual(position, 1)) {
                        // 动画结束后颜色恢复为动画开始前的值
                        if (page instanceof ComponentHost) {
                            currColor =
                                    (((ComponentHost) page).getComponent().getBackgroundColor());
                        }
                        resetValue();
                    } else if (position > endPosition) {
                        ratio = Math.abs(1 - Math.abs(position - endPosition));
                        calculateRealValue(ratio);
                    } else if (position < endPosition) {
                        ratio = Math.abs(position - endPosition);
                        calculateRealValue(ratio);
                    }
                    page.setPivotX(swiperAnimation.getPivotX());
                    page.setPivotY(swiperAnimation.getPivotY());
                    page.setAlpha(alpha);
                    page.setTranslationX(translateX);
                    page.setTranslationY(translateY);
                    page.setRotation(rotate);
                    page.setRotationX(rotateX);
                    page.setRotationY(rotateY);
                    page.setScaleX(scaleX);
                    page.setScaleY(scaleY);
                    if (currColor != IntegerUtil.UNDEFINED) {
                        page.setBackgroundColor(currColor);
                    }
                }
            }

            private void calculateRealValue(float ratio) {
                alpha =
                        getTransformValueByRatio(
                                swiperAnimation.getAlphaStart(), swiperAnimation.getAlphaEnd(),
                                ratio);
                translateX =
                        getTransformValueByRatio(
                                swiperAnimation.getTranslationXStart(),
                                swiperAnimation.getTranslationXEnd(),
                                ratio);
                translateY =
                        getTransformValueByRatio(
                                swiperAnimation.getTranslationYStart(),
                                swiperAnimation.getTranslationYEnd(),
                                ratio);
                rotate =
                        getTransformValueByRatio(
                                swiperAnimation.getRotateStart(), swiperAnimation.getRotateEnd(),
                                ratio);
                rotateX =
                        getTransformValueByRatio(
                                swiperAnimation.getRotateXStart(), swiperAnimation.getRotateXEnd(),
                                ratio);
                rotateY =
                        getTransformValueByRatio(
                                swiperAnimation.getRotateYStart(), swiperAnimation.getRotateYEnd(),
                                ratio);
                scaleX =
                        getTransformValueByRatio(
                                swiperAnimation.getScaleXStart(), swiperAnimation.getScaleXEnd(),
                                ratio);
                scaleY =
                        getTransformValueByRatio(
                                swiperAnimation.getScaleYStart(), swiperAnimation.getScaleYEnd(),
                                ratio);
                colorStart = swiperAnimation.getBackgroundColorStart();
                colorEnd = swiperAnimation.getBackgroundColorEnd();
                if (colorStart != IntegerUtil.UNDEFINED && colorEnd != IntegerUtil.UNDEFINED) {
                    try {
                        currColor = calculateBackgroundColorValue(colorStart, colorEnd, ratio);
                    } catch (NumberFormatException ex) {
                        Log.e(TAG, "color value transform exception: " + ex);
                    }
                } else {
                    currColor = IntegerUtil.UNDEFINED;
                }
            }

            private void resetValue() {
                translateX = 0;
                translateY = 0;
                alpha = 1;
                rotate = 0;
                rotateX = 0;
                rotateY = 0;
                scaleX = 1;
                scaleY = 1;
            }

            private float getTransformValueByRatio(
                    float startValue, float endValue, float transformRatio) {
                return startValue + (endValue - startValue) * transformRatio;
            }

            private int calculateBackgroundColorValue(int startValue, int endValue,
                                                      float colorRatio) {
                return (int) argbEvaluator.evaluate(colorRatio, startValue, endValue);
            }
        };
    }
}
