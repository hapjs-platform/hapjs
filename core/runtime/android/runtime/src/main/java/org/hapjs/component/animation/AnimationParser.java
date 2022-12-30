/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.ComponentBackgroundComposer;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.drawable.SizeBackgroundDrawable.Position;
import org.hapjs.runtime.HapEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 解析动画的工具类，待解析 JSONArray 关键帧序列如下：
 *
 * <p>[ {"backgroundColor":"#f76160","time":0}, {"backgroundColor":"#0000ff","time":100},
 * {"width":"100px","time":0}, {"width":"300px","time":100}, {"animationName":"Animation1"},
 * (哨兵帧，标志一个动画帧序列结束，带有动画名参数；注意：一个动画名对应的动画可以包含若干属性动画) {"height":"100px","time":0},
 * {"height":"350px","time":100}, {"animationName":"Animation2"} (哨兵帧，标志一个动画帧序列结束，带有动画名参数) ]
 */
public class AnimationParser {

    static final String TAG_ANIMATION_NAME = "animationName";
    static final String TAG_BACKGROUND_COLOR = "backgroundColor";
    static final String TAG_BACKGROUND_POSITION = "backgroundPosition";
    static final String TAG_OPACITY = "opacity";
    static final String TAG_WIDTH = "width";
    static final String TAG_HEIGHT = "height";
    static final String TAG_ROTATE = "rotate";
    static final String TAG_ROTATE_X = "rotateX";
    static final String TAG_ROTATE_Y = "rotateY";
    static final String TAG_SCALE_X = "scaleX";
    static final String TAG_SCALE_Y = "scaleY";
    static final String TAG_TRANSLATE_X = "translateX";
    static final String TAG_TRANSLATE_Y = "translateY";
    static final String TAG_TRANSLATE_Z = "translateZ";
    static final String PROPERTY_ALPHA = "alpha";
    static final String PROPERTY_BACKGROUND_COLOR = TAG_BACKGROUND_COLOR;
    static final String PROPERTY_BACKGROUND_POSITION = TAG_BACKGROUND_POSITION;
    static final String PROPERTY_WIDTH = TAG_WIDTH;
    static final String PROPERTY_HEIGHT = TAG_HEIGHT;
    static final String PROPERTY_ROTATION = "rotation";
    static final String PROPERTY_ROTATION_X = "rotationX";
    static final String PROPERTY_ROTATION_Y = "rotationY";
    static final String PROPERTY_SCALE_X = "scaleX";
    static final String PROPERTY_SCALE_Y = "scaleY";
    static final String PROPERTY_TRANSLATION_X = "translationX";
    static final String PROPERTY_TRANSLATION_Y = "translationY";
    static final String PROPERTY_PIVOT_X = "pivotX";
    static final String PROPERTY_PIVOT_Y = "pivotY";
    static final String PROPERTY_TRANSLATION_Z = "translationZ";
    private static final String TAG = "AnimationParser";
    private static final String TAG_TIME = "time";
    private static final String TAG_TRANSFORM = "transform";
    private static final String TAG_TIME_MILLISECOND = "ms";
    private static final String TAG_TIME_SECOND = "s";
    private static final String TAG_TRANSFORM_ORIGIN = "transform-origin";

    private AnimationParser() {
    }

    public static CSSAnimatorSet parse(
            HapEngine hapEngine,
            CSSAnimatorSet oldAnimatorSet,
            String keyframesStr,
            final Component component) {

        if (TextUtils.isEmpty(keyframesStr) || component == null) {
            return null;
        }
        final View target = component.getHostView();
        if (target == null) {
            return null;
        }

        try {
            JSONArray keyframes = new JSONArray(keyframesStr);
            if (keyframes.length() == 0) {
                return null;
            }

            final CSSAnimatorSet animatorSet =
                    CSSAnimatorSet.createNewAnimator(hapEngine, oldAnimatorSet, component);
            if (animatorSet == null) {
                return null;
            }
            List<Animator> animators = new ArrayList<>();

            // set ListenerBridge for every animator
            AnimatorListenerBridge.AnimatorEventListener animatorEventListener =
                    component.getOrCreateAnimatorEventListener();
            AnimatorListenerBridge listenerBridge =
                    new AnimatorListenerBridge(animatorEventListener);

            // 动画name属性集合，用于保存各属性动画帧
            Map<String, List<Keyframe>> keyframeMap = new ArrayMap<>(1);
            TimeInterpolator interpolator = animatorSet.getKeyFrameInterpolator();
            int len = keyframes.length();
            for (int i = 0; i < len; i++) {
                JSONObject keyframe = keyframes.getJSONObject(i);
                // 标志某个动画帧结束的哨兵帧
                String animationName = keyframe.optString(TAG_ANIMATION_NAME, "");
                boolean shouldCreateAnimators = !TextUtils.isEmpty(animationName);

                if (!shouldCreateAnimators) {

                    int time = keyframe.getInt(TAG_TIME);
                    float fraction = ((float) time) / 100;

                    double opacity =
                            Attributes.getDouble(keyframe.optString(TAG_OPACITY), Double.NaN);
                    if (!Double.isNaN(opacity)) {
                        getKeyframeList(PROPERTY_ALPHA, keyframeMap)
                                .add(
                                        keyframeWithInterpolator(
                                                Keyframe.ofFloat(fraction, (float) opacity),
                                                interpolator));
                    }

                    String backgroundColorStr = keyframe.optString(TAG_BACKGROUND_COLOR);
                    if (!TextUtils.isEmpty(backgroundColorStr)) {
                        getKeyframeList(PROPERTY_BACKGROUND_COLOR, keyframeMap)
                                .add(
                                        keyframeWithInterpolator(
                                                Keyframe.ofInt(
                                                        fraction, ColorUtil
                                                                .getColor(backgroundColorStr,
                                                                        Color.TRANSPARENT)),
                                                interpolator));
                    }

                    String backgroundPositionStr = keyframe.optString(TAG_BACKGROUND_POSITION);
                    if (!TextUtils.isEmpty(backgroundPositionStr)) {
                        ComponentBackgroundComposer backgroundComposer =
                                component.getOrCreateBackgroundComposer();
                        int[] currentBgRelativeWidthHeight =
                                backgroundComposer.getBgRelativeWidthHeight();
                        if (backgroundComposer.getBgRelativeWidthHeight() == null) {
                            backgroundComposer.setListenToBgPosition(true);
                            animatorSet.destroy();
                            return oldAnimatorSet;
                        }
                        Position bgPosition = Position.parse(backgroundPositionStr);
                        bgPosition.setRelativeSize(
                                currentBgRelativeWidthHeight[0], currentBgRelativeWidthHeight[1]);
                        bgPosition.calculatePx(hapEngine);
                        getKeyframeList(PROPERTY_BACKGROUND_POSITION, keyframeMap)
                                .add(
                                        keyframeWithInterpolator(
                                                Keyframe.ofObject(fraction, bgPosition),
                                                interpolator));
                    }

                    int width = Attributes.getInt(hapEngine, keyframe.optString(TAG_WIDTH), -1);
                    if (width >= 0) {
                        getKeyframeList(PROPERTY_WIDTH, keyframeMap)
                                .add(keyframeWithInterpolator(Keyframe.ofInt(fraction, width),
                                        interpolator));
                    }

                    int height = Attributes.getInt(hapEngine, keyframe.optString(TAG_HEIGHT), -1);
                    if (height >= 0) {
                        getKeyframeList(PROPERTY_HEIGHT, keyframeMap)
                                .add(keyframeWithInterpolator(Keyframe.ofInt(fraction, height),
                                        interpolator));
                    }

                    JSONObject transformObject = keyframe.optJSONObject(TAG_TRANSFORM);
                    if (transformObject == null) {
                        transformObject = Transform.toJsonObject(keyframe.opt(TAG_TRANSFORM));
                    }
                    if (transformObject != null) {
                        double rotate = getRotate(transformObject.optString(TAG_ROTATE));
                        if (!Double.isNaN(rotate)) {
                            getKeyframeList(PROPERTY_ROTATION, keyframeMap)
                                    .add(
                                            keyframeWithInterpolator(
                                                    Keyframe.ofFloat(fraction, (float) rotate),
                                                    interpolator));
                        }

                        double rotateX = getRotate(transformObject.optString(TAG_ROTATE_X));
                        if (!Double.isNaN(rotateX)) {
                            getKeyframeList(PROPERTY_ROTATION_X, keyframeMap)
                                    .add(
                                            keyframeWithInterpolator(
                                                    Keyframe.ofFloat(fraction, (float) rotateX),
                                                    interpolator));
                        }

                        double rotateY = getRotate(transformObject.optString(TAG_ROTATE_Y));
                        if (!Double.isNaN(rotateY)) {
                            getKeyframeList(PROPERTY_ROTATION_Y, keyframeMap)
                                    .add(
                                            keyframeWithInterpolator(
                                                    Keyframe.ofFloat(fraction, (float) rotateY),
                                                    interpolator));
                        }

                        double scaleX =
                                Attributes.getDouble(transformObject.optString(TAG_SCALE_X),
                                        Double.NaN);
                        if (!Double.isNaN(scaleX)) {
                            List<Keyframe> keyframeList =
                                    getKeyframeList(PROPERTY_SCALE_X, keyframeMap);
                            Keyframe interpolatorKeyframe =
                                    keyframeWithInterpolator(
                                            Keyframe.ofFloat(fraction, (float) scaleX),
                                            interpolator);
                            resolveDuplicate(hapEngine, interpolatorKeyframe, keyframeList);
                        }

                        double scaleY =
                                Attributes.getDouble(transformObject.optString(TAG_SCALE_Y),
                                        Double.NaN);
                        if (!Double.isNaN(scaleY)) {
                            List<Keyframe> keyframeList =
                                    getKeyframeList(PROPERTY_SCALE_Y, keyframeMap);
                            Keyframe interpolatorKeyframe =
                                    keyframeWithInterpolator(
                                            Keyframe.ofFloat(fraction, (float) scaleY),
                                            interpolator);
                            resolveDuplicate(hapEngine, interpolatorKeyframe, keyframeList);
                        }

                        String strX = transformObject.optString(TAG_TRANSLATE_X);
                        float translateX = FloatUtil.UNDEFINED;
                        if (!TextUtils.isEmpty(strX)) {
                            if (strX.endsWith(Attributes.Unit.PERCENT)) {
                                int componentWidth = component.getHostView().getWidth();
                                // 判断自身的宽高此时是否能取到，取不到直接返回旧值
                                if (isInvalidSize(componentWidth)) {
                                    if (oldAnimatorSet != null) {
                                        oldAnimatorSet.setIsReady(false);
                                        oldAnimatorSet.setIsPercent(true);
                                    }
                                    animatorSet.destroy();
                                    return oldAnimatorSet;
                                }
                                animatorSet.setIsPercent(true);
                                float ratio = Attributes.getPercent(strX, 0);
                                translateX = ratio * componentWidth;
                            } else {
                                translateX =
                                        Attributes.getFloat(hapEngine, strX, FloatUtil.UNDEFINED);
                            }
                        }
                        if (!FloatUtil.isUndefined(translateX)) {
                            getKeyframeList(PROPERTY_TRANSLATION_X, keyframeMap)
                                    .add(
                                            keyframeWithInterpolator(
                                                    Keyframe.ofFloat(fraction, translateX),
                                                    interpolator));
                        }

                        String strY = transformObject.optString(TAG_TRANSLATE_Y);
                        float translateY = FloatUtil.UNDEFINED;
                        if (!TextUtils.isEmpty(strY)) {
                            if (strY.endsWith(Attributes.Unit.PERCENT)) {
                                int componentHeight = component.getHostView().getHeight();
                                // 判断自身的宽高此时是否能取到，取不到直接返回旧值
                                if (isInvalidSize(componentHeight)) {
                                    if (oldAnimatorSet != null) {
                                        oldAnimatorSet.setIsReady(false);
                                        oldAnimatorSet.setIsPercent(true);
                                    }
                                    animatorSet.destroy();
                                    return oldAnimatorSet;
                                }
                                animatorSet.setIsPercent(true);
                                float ratio = Attributes.getPercent(strY, 0);
                                translateY = ratio * componentHeight;
                            } else {
                                translateY =
                                        Attributes.getFloat(hapEngine, strY, FloatUtil.UNDEFINED);
                            }
                        }
                        if (!FloatUtil.isUndefined(translateY)) {
                            getKeyframeList(PROPERTY_TRANSLATION_Y, keyframeMap)
                                    .add(
                                            keyframeWithInterpolator(
                                                    Keyframe.ofFloat(fraction, translateY),
                                                    interpolator));
                        }

                        String strZ = transformObject.optString(TAG_TRANSLATE_Z);
                        float translateZ = FloatUtil.UNDEFINED;
                        if (!TextUtils.isEmpty(strZ)) {
                            if (strZ.endsWith(Attributes.Unit.PERCENT)) {
                                animatorSet.setIsPercent(true);
                                float ratio = Attributes.getPercent(strZ, 0);
                                // 2dp thickness
                                translateZ = ratio * 2;
                            } else {
                                translateZ =
                                        Attributes.getFloat(hapEngine, strZ, FloatUtil.UNDEFINED);
                            }
                        }
                        if (!FloatUtil.isUndefined(translateZ)) {
                            getKeyframeList(PROPERTY_TRANSLATION_Z, keyframeMap)
                                    .add(
                                            keyframeWithInterpolator(
                                                    Keyframe.ofFloat(fraction, translateZ),
                                                    interpolator));
                        }
                    }

                    // support transform origin
                    String transformOriginStr =
                            keyframe.optString(Attributes.Style.TRANSFORM_ORIGIN);
                    if (TextUtils.isEmpty(transformOriginStr)) {
                        transformOriginStr = keyframe.optString(TAG_TRANSFORM_ORIGIN);
                    }
                    float pivotX = FloatUtil.UNDEFINED;
                    float pivotY = FloatUtil.UNDEFINED;
                    if (!TextUtils.isEmpty(transformOriginStr)) {
                        pivotX = Origin.parseOrigin(transformOriginStr, Origin.ORIGIN_X, target,
                                hapEngine);
                        pivotY = Origin.parseOrigin(transformOriginStr, Origin.ORIGIN_Y, target,
                                hapEngine);
                    }

                    if (!FloatUtil.isUndefined(pivotX)) {
                        getKeyframeList(PROPERTY_PIVOT_X, keyframeMap)
                                .add(keyframeWithInterpolator(Keyframe.ofFloat(fraction, pivotX),
                                        interpolator));
                    }

                    if (!FloatUtil.isUndefined(pivotY)) {
                        getKeyframeList(PROPERTY_PIVOT_Y, keyframeMap)
                                .add(keyframeWithInterpolator(Keyframe.ofFloat(fraction, pivotY),
                                        interpolator));
                    }
                }

                if (i == len - 1 || shouldCreateAnimators) {
                    List<Animator> list4OneName =
                            createAndListenAnimators(
                                    keyframeMap, animatorSet, component, target, listenerBridge,
                                    animationName);
                    if (!list4OneName.isEmpty()) {
                        animators.addAll(list4OneName);
                    }
                    // 创建完前一个动画必须清除数据，否则会混到第二个动画中
                    keyframeMap.clear();
                }
            }

            animatorSet.playTogether(animators);
            if (oldAnimatorSet != null) {
                oldAnimatorSet.destroy();
            }
            return animatorSet;
        } catch (JSONException e) {
            Log.e(TAG, "parse: " + e);
        }

        return null;
    }

    private static List<Keyframe> getKeyframeList(
            @NonNull String propertyName, @NonNull Map<String, List<Keyframe>> keyframeMap) {
        List<Keyframe> frameList = keyframeMap.get(propertyName);
        if (frameList == null) {
            frameList = new ArrayList<>();
            keyframeMap.put(propertyName, frameList);
        }
        return frameList;
    }

    private static Keyframe keyframeWithInterpolator(
            @NonNull Keyframe keyframe, TimeInterpolator interpolator) {
        keyframe.setInterpolator(interpolator);
        return keyframe;
    }

    private static List<Animator> createAndListenAnimators(
            @NonNull Map<String, List<Keyframe>> keyframeMap,
            @NonNull CSSAnimatorSet animatorSet,
            @NonNull Component component,
            @NonNull View target,
            @NonNull AnimatorListenerBridge listenerBridge,
            @NonNull String animationName) {

        List<Animator> animators = new ArrayList<>();
        if (keyframeMap.isEmpty()) {
            Log.w(TAG, "createAndSaveAnimators: return with keyframeMap.isEmpty");
            return animators;
        }

        for (Map.Entry<String, List<Keyframe>> propertyEntry : keyframeMap.entrySet()) {
            String property = propertyEntry.getKey();
            List<Keyframe> keyframeList = propertyEntry.getValue();
            if (keyframeList.size() < 2) {
                Log.w(
                        TAG,
                        "createAndSaveAnimators: break with keyframeList.size() < 2, property: "
                                + property);
                continue;
            }
            animatorSet.setDirty(true);

            PropertyValuesHolder holder =
                    PropertyValuesHolder
                            .ofKeyframe(property, keyframeList.toArray(new Keyframe[0]));

            switch (property) {
                case PROPERTY_BACKGROUND_COLOR:
                    ValueAnimator bgColorAnimator = ValueAnimator.ofPropertyValuesHolder(holder);
                    bgColorAnimator.setEvaluator(new ArgbEvaluator());
                    bgColorAnimator.addUpdateListener(
                            new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int color = (int) animation.getAnimatedValue();
                                    component.setBackgroundColor(color);
                                    component.applyBackground();
                                }
                            });
                    animators.add(bgColorAnimator);
                    break;

                case PROPERTY_BACKGROUND_POSITION:
                    ValueAnimator bgPositionAnimator = ValueAnimator.ofPropertyValuesHolder(holder);
                    bgPositionAnimator.setEvaluator(BgPositionEvaluator.getInstance());
                    bgPositionAnimator.addUpdateListener(
                            new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    Position bgPosition = (Position) animation.getAnimatedValue();
                                    component.setBackgroundPosition(bgPosition.getParseStr());
                                    component.applyBackground();
                                }
                            });
                    animators.add(bgPositionAnimator);
                    break;

                case PROPERTY_WIDTH:
                    ValueAnimator widthAnimator = ValueAnimator.ofPropertyValuesHolder(holder);
                    widthAnimator.addUpdateListener(
                            new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int width = (int) animation.getAnimatedValue();

                                    ViewGroup.LayoutParams lp = target.getLayoutParams();
                                    lp.width = width;
                                    target.setLayoutParams(lp);

                                    YogaNode node = YogaUtil.getYogaNode(target);
                                    if (node != null) {
                                        node.setWidth(width);
                                        component.setWidthDefined(true);
                                        if (!(target instanceof YogaLayout)) {
                                            node.dirty();
                                        }
                                        target.requestLayout();
                                    }
                                }
                            });
                    animators.add(widthAnimator);
                    break;

                case PROPERTY_HEIGHT:
                    ValueAnimator heightAnimator = ValueAnimator.ofPropertyValuesHolder(holder);
                    heightAnimator.addUpdateListener(
                            new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int height = (int) animation.getAnimatedValue();

                                    ViewGroup.LayoutParams lp = target.getLayoutParams();
                                    lp.height = height;
                                    target.setLayoutParams(lp);

                                    YogaNode node = YogaUtil.getYogaNode(target);
                                    if (node != null) {
                                        node.setHeight(height);
                                        component.setHeightDefined(true);
                                        if (!(target instanceof YogaLayout)) {
                                            node.dirty();
                                        }
                                        target.requestLayout();
                                    }
                                }
                            });
                    animators.add(heightAnimator);
                    break;

                case PROPERTY_ALPHA:
                case PROPERTY_ROTATION:
                case PROPERTY_ROTATION_X:
                case PROPERTY_ROTATION_Y:
                case PROPERTY_SCALE_X:
                case PROPERTY_SCALE_Y:
                case PROPERTY_TRANSLATION_X:
                case PROPERTY_TRANSLATION_Y:
                case PROPERTY_TRANSLATION_Z:
                case PROPERTY_PIVOT_X:
                case PROPERTY_PIVOT_Y:
                    ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, holder);
                    animators.add(animator);
                    break;

                default:
                    break;
            }
        }
        // 取当前动画name的第一个属性animator来添加监听，带动画名name
        AnimatorListenerAdapter listener = listenerBridge.createAnimatorListener(animationName);
        if (!animators.isEmpty()) {
            animators.get(0).addListener(listener);
        }
        return animators;
    }

    /**
     * BUG: Android targetSdkVersion >= P 时，重复 time 的相邻关键帧插值计算得到非法浮点参数值（如 NaN, -INFINITY, INFINITY）
     * 注：当前仅 scaleX/Y 动画会检查 float 参数 {@link View#setScaleX(float)} {@link View#setScaleY(float)}
     * 解决办法：关键帧序列已在前端按 time 顺序整理好，在添加到关键帧列表 keyframeList 时，time 若有重叠后者覆盖前者。
     *
     * @param keyframe     待添加进关键帧列表 keyframeList 的关键帧
     * @param keyframeList 待去除重复 time 动画时间偏移的关键帧列表
     */
    private static void resolveDuplicate(
            HapEngine hapEngine, Keyframe keyframe, List<Keyframe> keyframeList) {
        if (keyframe == null || keyframeList == null) {
            return;
        }
        if (keyframeList.isEmpty()) {
            keyframeList.add(keyframe);
            return;
        }
        boolean shouldResolve = true;
        if (hapEngine != null && hapEngine.getContext() != null) {
            final int targetSdkVersion =
                    hapEngine.getContext().getApplicationInfo().targetSdkVersion;
            shouldResolve = targetSdkVersion >= Build.VERSION_CODES.P;
        }
        if (shouldResolve) {
            int index = keyframeList.size() - 1;
            if (FloatUtil
                    .floatsEqual(keyframe.getFraction(), keyframeList.get(index).getFraction())) {
                keyframeList.remove(index);
            }
        }
        keyframeList.add(keyframe);
    }

    static double getRotate(String rotateStr) {
        if (TextUtils.isEmpty(rotateStr)) {
            return Double.NaN;
        }

        String temp = rotateStr.trim();
        if (temp.endsWith("deg")) {
            temp = temp.substring(0, temp.indexOf("deg"));
        }

        try {
            return Double.parseDouble(temp);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Double.NaN;
    }

    public static int getTime(String timeStr) {
        if (TextUtils.isEmpty(timeStr)) {
            return 0;
        }

        String temp = timeStr.trim();
        boolean isSecondUnit = false;
        try {
            if (temp.endsWith(TAG_TIME_MILLISECOND)) {
                temp = temp.substring(0, temp.indexOf(TAG_TIME_MILLISECOND));
            } else if (temp.endsWith(TAG_TIME_SECOND)) {
                temp = temp.substring(0, temp.indexOf(TAG_TIME_SECOND));
                isSecondUnit = true;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "getTime: ", e);
            return 0;
        }
        temp = temp.trim();
        if (TextUtils.isEmpty(temp)) {
            return 0;
        }
        try {
            float time = Float.parseFloat(temp);
            return isSecondUnit ? (int) (time * 1000) : (int) time;
        } catch (Exception e) {
            Log.e(TAG, "getTime: ", e);
        }
        return 0;
    }

    private static boolean isInvalidSize(int size) {
        return (size <= 0) || (size == Integer.MAX_VALUE);
    }
}
