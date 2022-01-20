/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.component.Component;
import org.hapjs.component.animation.Transform;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.Swiper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SwiperAnimationParser {

    private static final String TAG = "SwiperAnimationParser";
    private static final String TAG_OPACITY = "opacity";
    private static final String TAG_ROTATE = "rotate";
    private static final String TAG_ROTATE_X = "rotateX";
    private static final String TAG_ROTATE_Y = "rotateY";
    private static final String TAG_SCALE_X = "scaleX";
    private static final String TAG_SCALE_Y = "scaleY";
    private static final String TAG_TRANSLATE_X = "translateX";
    private static final String TAG_TRANSLATE_Y = "translateY";
    private static final String TAG_TRANSFORM = "transform";
    private static final String TAG_ANIMATION_NAME = "animationName";
    private static final String TAG_BACKGROUND_COLOR = "backgroundColor";
    private static final String TAG_TIME = "time";

    public SwiperAnimation parse(
            HapEngine hapEngine,
            SwiperAnimation swiperAnimation,
            String keyframesStr,
            Map<String, Float> ratioAndFractionMap,
            final Component component) {
        float ratioWidth;
        float ratioHeight;
        if (TextUtils.isEmpty(keyframesStr) || component == null) {
            return null;
        }
        if (component instanceof Swiper && swiperAnimation != null) {
            try {
                JSONArray keyframes = new JSONArray(keyframesStr);
                if (keyframes.length() == 0) {
                    return null;
                }
                int len = keyframes.length();
                for (int i = 0; i < len; i++) {
                    JSONObject keyframe = keyframes.getJSONObject(i);
                    // 标志某个动画帧结束的哨兵帧
                    String animationName = keyframe.optString(TAG_ANIMATION_NAME, "");
                    if (TextUtils.isEmpty(animationName)) {
                        int time = keyframe.getInt(TAG_TIME);
                        float fraction = ((float) time) / 100;
                        float opacity =
                                Attributes.getFloat(hapEngine, keyframe.optString(TAG_OPACITY),
                                        Float.NaN);
                        if (!Double.isNaN(opacity)) {
                            if (fraction == 0) {
                                swiperAnimation.setAlphaStart(opacity);
                            } else if (fraction == 1) {
                                swiperAnimation.setAlphaEnd(opacity);
                            }
                        }
                        String backgroundColorStr = keyframe.optString(TAG_BACKGROUND_COLOR);
                        int color = IntegerUtil.UNDEFINED;
                        if (!TextUtils.isEmpty(backgroundColorStr)) {
                            color = ColorUtil.getColor(backgroundColorStr, Color.TRANSPARENT);
                        }
                        if (!IntegerUtil.isUndefined(color)) {
                            if (fraction == 0) {
                                swiperAnimation.setBackgroundColorStart(color);
                            } else if (fraction == 1) {
                                swiperAnimation.setBackgroundColorEnd(color);
                            }
                        }

                        JSONObject transformObject = keyframe.optJSONObject(TAG_TRANSFORM);
                        if (transformObject == null) {
                            transformObject = Transform.toJsonObject(keyframe.opt(TAG_TRANSFORM));
                        }
                        if (transformObject != null) {
                            float rotate = getRotate(transformObject.optString(TAG_ROTATE));
                            if (!Float.isNaN(rotate)) {
                                if (fraction == 0) {
                                    swiperAnimation.setRotateStart(rotate);
                                } else if (fraction == 1) {
                                    swiperAnimation.setRotateEnd(rotate);
                                }
                            }

                            float rotateX = getRotate(transformObject.optString(TAG_ROTATE_X));
                            if (!Float.isNaN(rotateX)) {
                                if (fraction == 0) {
                                    swiperAnimation.setRotateXStart(rotateX);
                                } else if (fraction == 1) {
                                    swiperAnimation.setRotateXEnd(rotateX);
                                }
                            }

                            float rotateY = getRotate(transformObject.optString(TAG_ROTATE_Y));
                            if (!Float.isNaN(rotateY)) {
                                if (fraction == 0) {
                                    swiperAnimation.setRotateYStart(rotateY);
                                } else if (fraction == 1) {
                                    swiperAnimation.setRotateYEnd(rotateY);
                                }
                            }

                            double scaleX =
                                    Attributes.getDouble(transformObject.optString(TAG_SCALE_X),
                                            Double.NaN);
                            if (!Double.isNaN(scaleX)) {
                                if (fraction == 0) {
                                    swiperAnimation.setScaleXStart((float) scaleX);
                                } else if (fraction == 1) {
                                    swiperAnimation.setScaleXEnd((float) scaleX);
                                }
                            }

                            double scaleY =
                                    Attributes.getDouble(transformObject.optString(TAG_SCALE_Y),
                                            Double.NaN);
                            if (!Double.isNaN(scaleY)) {
                                if (fraction == 0) {
                                    swiperAnimation.setScaleYStart((float) scaleY);
                                } else if (fraction == 1) {
                                    swiperAnimation.setScaleYEnd((float) scaleY);
                                }
                            }

                            String strX = transformObject.optString(TAG_TRANSLATE_X);
                            float translateX = FloatUtil.UNDEFINED;
                            if (!TextUtils.isEmpty(strX)) {
                                if (strX.endsWith(Attributes.Unit.PERCENT)) {
                                    ratioWidth = Attributes.getPercent(strX, 0);
                                    int componentWidth = component.getWidth();
                                    if (componentWidth == 0) {
                                        if (fraction == 0) {
                                            ratioAndFractionMap
                                                    .put(Swiper.getWidthRatioStart(), ratioWidth);
                                            ratioAndFractionMap
                                                    .put(Swiper.getWidthFractionStart(), fraction);
                                        } else {
                                            ratioAndFractionMap
                                                    .put(Swiper.getWidthRatioEnd(), ratioWidth);
                                            ratioAndFractionMap
                                                    .put(Swiper.getWidthFractionEnd(), fraction);
                                        }
                                    } else {
                                        translateX = ratioWidth * componentWidth;
                                    }
                                } else {
                                    translateX = Attributes
                                            .getFloat(hapEngine, strX, FloatUtil.UNDEFINED);
                                }
                            }
                            if (!FloatUtil.isUndefined(translateX)) {
                                if (fraction == 0) {
                                    swiperAnimation.setTranslationXStart(translateX);
                                } else if (fraction == 1) {
                                    swiperAnimation.setTranslationXEnd(translateX);
                                }
                            }

                            String strY = transformObject.optString(TAG_TRANSLATE_Y);
                            float translateY = FloatUtil.UNDEFINED;
                            if (!TextUtils.isEmpty(strY)) {
                                if (strY.endsWith(Attributes.Unit.PERCENT)) {
                                    ratioHeight = Attributes.getPercent(strY, 0);
                                    int componentHeight = component.getHeight();
                                    if (componentHeight == 0) {
                                        if (fraction == 0) {
                                            ratioAndFractionMap
                                                    .put(Swiper.getHeightRatioStart(), ratioHeight);
                                            ratioAndFractionMap
                                                    .put(Swiper.getHeightFractionStart(), fraction);
                                        } else {
                                            ratioAndFractionMap
                                                    .put(Swiper.getHeightRatioEnd(), ratioHeight);
                                            ratioAndFractionMap
                                                    .put(Swiper.getHeightFractionEnd(), fraction);
                                        }
                                    } else {
                                        translateY = ratioHeight * componentHeight;
                                    }

                                } else {
                                    translateY = Attributes
                                            .getFloat(hapEngine, strY, FloatUtil.UNDEFINED);
                                }
                            }
                            if (!FloatUtil.isUndefined(translateY)) {
                                if (fraction == 0) {
                                    swiperAnimation.setTranslationYStart(translateY);
                                } else if (fraction == 1) {
                                    swiperAnimation.setTranslationYEnd(translateY);
                                }
                            }
                        }
                    }
                }
                return swiperAnimation;
            } catch (JSONException e) {
                Log.e(TAG, "parse: " + e);
            }
        }

        return null;
    }

    private float getRotate(String rotateStr) {
        if (TextUtils.isEmpty(rotateStr)) {
            return Float.NaN;
        }

        String temp = rotateStr.trim();
        if (temp.endsWith("deg")) {
            temp = temp.substring(0, temp.indexOf("deg"));
        }

        try {
            return Float.parseFloat(temp);
        } catch (NumberFormatException e) {
            Log.d(TAG, "rotate value transform exception :" + e);
        }

        return Float.NaN;
    }
}
